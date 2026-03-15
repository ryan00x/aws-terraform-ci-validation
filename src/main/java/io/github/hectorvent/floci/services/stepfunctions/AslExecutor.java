package io.github.hectorvent.floci.services.stepfunctions;

import io.github.hectorvent.floci.services.lambda.LambdaExecutorService;
import io.github.hectorvent.floci.services.lambda.LambdaFunctionStore;
import io.github.hectorvent.floci.services.lambda.model.InvocationType;
import io.github.hectorvent.floci.services.lambda.model.InvokeResult;
import io.github.hectorvent.floci.services.lambda.model.LambdaFunction;
import io.github.hectorvent.floci.services.stepfunctions.model.Execution;
import io.github.hectorvent.floci.services.stepfunctions.model.HistoryEvent;
import io.github.hectorvent.floci.services.stepfunctions.model.StateMachine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

@ApplicationScoped
public class AslExecutor {

    private static final Logger LOG = Logger.getLogger(AslExecutor.class);
    private static final int MAX_WAIT_SECONDS = 30;

    private final LambdaExecutorService lambdaExecutor;
    private final LambdaFunctionStore functionStore;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sfn-executor");
        t.setDaemon(true);
        return t;
    });

    @Inject
    public AslExecutor(LambdaExecutorService lambdaExecutor, LambdaFunctionStore functionStore,
                       ObjectMapper objectMapper) {
        this.lambdaExecutor = lambdaExecutor;
        this.functionStore = functionStore;
        this.objectMapper = objectMapper;
    }

    /**
     * Launches execution asynchronously. Calls onUpdate when execution status changes.
     */
    public void executeAsync(StateMachine sm, Execution exec, List<HistoryEvent> history,
                             BiConsumer<Execution, List<HistoryEvent>> onUpdate) {
        executor.submit(() -> {
            try {
                AtomicLong eventId = new AtomicLong(history.size());
                JsonNode definition = objectMapper.readTree(sm.getDefinition());
                JsonNode states = definition.path("States");
                String startAt = definition.path("StartAt").asText();
                JsonNode currentInput = parseInput(exec.getInput());

                String currentStateName = startAt;
                while (currentStateName != null) {
                    JsonNode stateDef = states.path(currentStateName);
                    if (stateDef.isMissingNode()) {
                        throw new RuntimeException("State not found: " + currentStateName);
                    }

                    String type = stateDef.path("Type").asText();
                    addEvent(history, eventId, stateEnteredEventType(type), null,
                            Map.of("name", currentStateName, "input", currentInput.toString()));

                    try {
                        StateResult stateResult = executeState(currentStateName, type, stateDef, currentInput, history, eventId, sm);
                        addEvent(history, eventId, stateExitedEventType(type), eventId.get() - 1,
                                Map.of("name", currentStateName, "output", stateResult.output().toString()));

                        currentInput = stateResult.output();
                        currentStateName = stateResult.nextState();

                        if ("Succeed".equals(type) || stateDef.path("End").asBoolean(false)) {
                            currentStateName = null;
                        }
                    } catch (FailStateException e) {
                        exec.setStatus("FAILED");
                        exec.setStopDate(System.currentTimeMillis() / 1000L);
                        addEvent(history, eventId, "ExecutionFailed", null,
                                Map.of("error", e.error != null ? e.error : "States.Runtime",
                                       "cause", e.cause != null ? e.cause : ""));
                        onUpdate.accept(exec, history);
                        return;
                    } catch (Exception e) {
                        exec.setStatus("FAILED");
                        exec.setStopDate(System.currentTimeMillis() / 1000L);
                        addEvent(history, eventId, "ExecutionFailed", null,
                                Map.of("error", "States.Runtime", "cause", e.getMessage() != null ? e.getMessage() : "Unknown error"));
                        onUpdate.accept(exec, history);
                        return;
                    }
                }

                exec.setStatus("SUCCEEDED");
                exec.setOutput(currentInput.toString());
                exec.setStopDate(System.currentTimeMillis() / 1000L);
                addEvent(history, eventId, "ExecutionSucceeded", null,
                        Map.of("output", currentInput.toString()));
                onUpdate.accept(exec, history);

            } catch (Exception e) {
                LOG.warnv("ASL execution failed for {0}: {1}", exec.getExecutionArn(), e.getMessage());
                exec.setStatus("FAILED");
                exec.setStopDate(System.currentTimeMillis() / 1000L);
                onUpdate.accept(exec, history);
            }
        });
    }

    private StateResult executeState(String name, String type, JsonNode stateDef, JsonNode input,
                                     List<HistoryEvent> history, AtomicLong eventId, StateMachine sm) throws Exception {
        return switch (type) {
            case "Pass" -> executePassState(stateDef, input);
            case "Task" -> executeTaskState(name, stateDef, input, history, eventId, sm);
            case "Choice" -> executeChoiceState(stateDef, input);
            case "Wait" -> executeWaitState(stateDef, input);
            case "Succeed" -> new StateResult(applyOutputPath(stateDef, input, input), null);
            case "Fail" -> throw new FailStateException(
                    stateDef.path("Error").asText(null), stateDef.path("Cause").asText(null));
            case "Parallel" -> executeParallelState(name, stateDef, input, sm);
            case "Map" -> executeMapState(name, stateDef, input, sm);
            default -> new StateResult(input, stateDef.path("Next").asText(null));
        };
    }

    private StateResult executePassState(JsonNode stateDef, JsonNode input) throws Exception {
        JsonNode effectiveInput = applyInputPath(stateDef, input);

        JsonNode result;
        if (stateDef.has("Result")) {
            result = stateDef.get("Result");
        } else {
            result = effectiveInput;
        }

        JsonNode output = mergeResult(stateDef, input, result);
        output = applyOutputPath(stateDef, input, output);
        return new StateResult(output, stateDef.path("Next").asText(null));
    }

    private StateResult executeTaskState(String stateName, JsonNode stateDef, JsonNode input,
                                         List<HistoryEvent> history, AtomicLong eventId, StateMachine sm) throws Exception {
        JsonNode effectiveInput = applyInputPath(stateDef, input);
        if (stateDef.has("Parameters")) {
            effectiveInput = resolveParameters(stateDef.get("Parameters"), effectiveInput);
        }

        String resource = stateDef.path("Resource").asText();
        JsonNode taskResult = invokeResource(resource, effectiveInput, stateDef, sm);

        JsonNode output = mergeResult(stateDef, input, taskResult);
        output = applyOutputPath(stateDef, input, output);

        String next = stateDef.path("Next").asText(null);
        return new StateResult(output, next);
    }

    private JsonNode invokeResource(String resource, JsonNode input, JsonNode stateDef, StateMachine sm) throws Exception {
        // Support Lambda resources: direct ARN or optimized integration
        String functionName = null;
        JsonNode lambdaPayload = input;

        if (resource.contains(":lambda:") && resource.contains(":function:")) {
            // Direct Lambda ARN: arn:aws:lambda:region:account:function:name
            functionName = resource.substring(resource.lastIndexOf(':') + 1);
        } else if (resource.equals("arn:aws:states:::lambda:invoke") ||
                   resource.equals("arn:aws:states:::lambda:invoke.waitForTaskToken")) {
            // Optimized Lambda integration — function name comes from Parameters
            if (stateDef.has("Parameters")) {
                JsonNode params = stateDef.get("Parameters");
                String fnRef = params.path("FunctionName").asText(null);
                if (fnRef == null) fnRef = params.path("FunctionName.$").asText(null);
                if (fnRef != null) {
                    functionName = fnRef.contains(":") ? fnRef.substring(fnRef.lastIndexOf(':') + 1) : fnRef;
                }
                // Payload comes from Payload or Payload.$
                if (params.has("Payload.$")) {
                    lambdaPayload = resolvePath(params.path("Payload.$").asText(), input);
                } else if (params.has("Payload")) {
                    lambdaPayload = params.get("Payload");
                }
            }
        }

        if (functionName != null) {
            // Extract region from the state machine ARN: arn:aws:states:REGION:...
            String region = extractRegionFromArn(sm.getStateMachineArn());
            LambdaFunction fn = functionStore.get(region, functionName).orElse(null);
            if (fn == null) {
                throw new RuntimeException("Lambda function not found: " + functionName);
            }

            String payloadStr = objectMapper.writeValueAsString(lambdaPayload);
            InvokeResult result = lambdaExecutor.invoke(fn, payloadStr.getBytes(), InvocationType.RequestResponse);

            if (result.getFunctionError() != null) {
                throw new FailStateException("Lambda.AWSLambdaException", result.getFunctionError());
            }

            byte[] responseBytes = result.getPayload();
            if (responseBytes != null && responseBytes.length > 0) {
                return objectMapper.readTree(responseBytes);
            }
            return NullNode.getInstance();
        }

        // Unsupported resource: return input as-is (stub)
        LOG.warnv("Unsupported Task resource (stub passthrough): {0}", resource);
        return input;
    }

    private StateResult executeChoiceState(JsonNode stateDef, JsonNode input) throws Exception {
        JsonNode choices = stateDef.path("Choices");
        for (JsonNode choice : choices) {
            if (evaluateCondition(choice, input)) {
                return new StateResult(input, choice.path("Next").asText());
            }
        }
        // Default branch
        String defaultState = stateDef.path("Default").asText(null);
        if (defaultState != null) {
            return new StateResult(input, defaultState);
        }
        throw new FailStateException("States.NoChoiceMatched", "No choice rule matched and no default state");
    }

    private boolean evaluateCondition(JsonNode rule, JsonNode input) throws Exception {
        // Logical operators
        if (rule.has("And")) {
            for (JsonNode sub : rule.get("And")) {
                if (!evaluateCondition(sub, input)) return false;
            }
            return true;
        }
        if (rule.has("Or")) {
            for (JsonNode sub : rule.get("Or")) {
                if (evaluateCondition(sub, input)) return true;
            }
            return false;
        }
        if (rule.has("Not")) {
            return !evaluateCondition(rule.get("Not"), input);
        }

        String variable = rule.path("Variable").asText();
        JsonNode value = resolvePath(variable, input);

        if (rule.has("StringEquals")) {
            return value.asText().equals(rule.get("StringEquals").asText());
        }
        if (rule.has("StringEqualsPath")) {
            return value.asText().equals(resolvePath(rule.get("StringEqualsPath").asText(), input).asText());
        }
        if (rule.has("StringMatches")) {
            return value.asText().matches(globToRegex(rule.get("StringMatches").asText()));
        }
        if (rule.has("NumericEquals")) {
            return value.asDouble() == rule.get("NumericEquals").asDouble();
        }
        if (rule.has("NumericEqualsPath")) {
            return value.asDouble() == resolvePath(rule.get("NumericEqualsPath").asText(), input).asDouble();
        }
        if (rule.has("NumericLessThan")) {
            return value.asDouble() < rule.get("NumericLessThan").asDouble();
        }
        if (rule.has("NumericLessThanPath")) {
            return value.asDouble() < resolvePath(rule.get("NumericLessThanPath").asText(), input).asDouble();
        }
        if (rule.has("NumericGreaterThan")) {
            return value.asDouble() > rule.get("NumericGreaterThan").asDouble();
        }
        if (rule.has("NumericGreaterThanPath")) {
            return value.asDouble() > resolvePath(rule.get("NumericGreaterThanPath").asText(), input).asDouble();
        }
        if (rule.has("NumericLessThanEquals")) {
            return value.asDouble() <= rule.get("NumericLessThanEquals").asDouble();
        }
        if (rule.has("NumericGreaterThanEquals")) {
            return value.asDouble() >= rule.get("NumericGreaterThanEquals").asDouble();
        }
        if (rule.has("BooleanEquals")) {
            return value.asBoolean() == rule.get("BooleanEquals").asBoolean();
        }
        if (rule.has("BooleanEqualsPath")) {
            return value.asBoolean() == resolvePath(rule.get("BooleanEqualsPath").asText(), input).asBoolean();
        }
        if (rule.has("IsNull")) {
            boolean expectNull = rule.get("IsNull").asBoolean();
            return value.isNull() == expectNull;
        }
        if (rule.has("IsPresent")) {
            boolean expectPresent = rule.get("IsPresent").asBoolean();
            return !value.isMissingNode() == expectPresent;
        }
        if (rule.has("IsString")) {
            return value.isTextual() == rule.get("IsString").asBoolean();
        }
        if (rule.has("IsNumeric")) {
            return value.isNumber() == rule.get("IsNumeric").asBoolean();
        }
        if (rule.has("IsBoolean")) {
            return value.isBoolean() == rule.get("IsBoolean").asBoolean();
        }

        return false;
    }

    private StateResult executeWaitState(JsonNode stateDef, JsonNode input) throws InterruptedException {
        int seconds = 0;
        if (stateDef.has("Seconds")) {
            seconds = Math.min(stateDef.get("Seconds").asInt(), MAX_WAIT_SECONDS);
        } else if (stateDef.has("SecondsPath")) {
            JsonNode val = resolvePath(stateDef.get("SecondsPath").asText(), input);
            seconds = Math.min(val.asInt(), MAX_WAIT_SECONDS);
        }
        // Timestamp and TimestampPath: wait until that time or now, whichever is sooner
        if (seconds > 0) {
            TimeUnit.SECONDS.sleep(seconds);
        }
        return new StateResult(input, stateDef.path("Next").asText(null));
    }

    private StateResult executeParallelState(String name, JsonNode stateDef, JsonNode input, StateMachine sm) throws Exception {
        JsonNode branches = stateDef.path("Branches");
        List<Future<JsonNode>> futures = new ArrayList<>();

        for (JsonNode branch : branches) {
            String startAt = branch.path("StartAt").asText();
            JsonNode branchStates = branch.path("States");
            JsonNode capturedInput = input;

            futures.add(executor.submit(() -> executeBranch(startAt, branchStates, capturedInput, sm)));
        }

        ArrayNode results = objectMapper.createArrayNode();
        for (Future<JsonNode> future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }

        JsonNode output = mergeResult(stateDef, input, results);
        output = applyOutputPath(stateDef, input, output);
        return new StateResult(output, stateDef.path("Next").asText(null));
    }

    private StateResult executeMapState(String name, JsonNode stateDef, JsonNode input, StateMachine sm) throws Exception {
        JsonNode itemsPath = stateDef.path("ItemsPath");
        JsonNode items = itemsPath.isMissingNode() ? input : resolvePath(itemsPath.asText("$"), input);

        if (!items.isArray()) {
            throw new FailStateException("States.Runtime", "ItemsPath must reference an array");
        }

        JsonNode iterator = stateDef.path("Iterator");
        String startAt = iterator.path("StartAt").asText();
        JsonNode iteratorStates = iterator.path("States");

        ArrayNode results = objectMapper.createArrayNode();
        for (JsonNode item : items) {
            results.add(executeBranch(startAt, iteratorStates, item, sm));
        }

        JsonNode output = mergeResult(stateDef, input, results);
        output = applyOutputPath(stateDef, input, output);
        return new StateResult(output, stateDef.path("Next").asText(null));
    }

    private JsonNode executeBranch(String startAt, JsonNode states, JsonNode input, StateMachine sm) throws Exception {
        List<HistoryEvent> ignored = new ArrayList<>();
        AtomicLong eventId = new AtomicLong(0);
        JsonNode currentInput = input;
        String currentState = startAt;

        while (currentState != null) {
            JsonNode stateDef = states.path(currentState);
            if (stateDef.isMissingNode()) {
                throw new RuntimeException("State not found: " + currentState);
            }
            String type = stateDef.path("Type").asText();
            StateResult result = executeState(currentState, type, stateDef, currentInput, ignored, eventId, sm);
            currentInput = result.output();
            currentState = result.nextState();
            if ("Succeed".equals(type) || stateDef.path("End").asBoolean(false)) {
                currentState = null;
            }
        }
        return currentInput;
    }

    // ──────────────────────────── Path resolution ────────────────────────────

    private JsonNode applyInputPath(JsonNode stateDef, JsonNode input) {
        if (!stateDef.has("InputPath")) {
            return input;
        }
        String path = stateDef.get("InputPath").asText();
        if (path == null || path.equals("null")) {
            return NullNode.getInstance();
        }
        return resolvePath(path, input);
    }

    private JsonNode mergeResult(JsonNode stateDef, JsonNode input, JsonNode result) throws Exception {
        if (!stateDef.has("ResultPath")) {
            return result;
        }
        String resultPath = stateDef.get("ResultPath").asText();
        if (resultPath == null || resultPath.equals("null")) {
            return input;
        }
        if ("$".equals(resultPath)) {
            return result;
        }
        // Merge result into input at the given path
        if (!input.isObject()) {
            return result;
        }
        ObjectNode merged = input.deepCopy();
        setPath(merged, resultPath, result);
        return merged;
    }

    private JsonNode applyOutputPath(JsonNode stateDef, JsonNode input, JsonNode output) {
        if (!stateDef.has("OutputPath")) {
            return output;
        }
        String path = stateDef.get("OutputPath").asText();
        if (path == null || path.equals("null")) {
            return NullNode.getInstance();
        }
        return resolvePath(path, output);
    }

    private JsonNode resolveParameters(JsonNode parameters, JsonNode input) throws Exception {
        if (parameters.isObject()) {
            ObjectNode resolved = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = parameters.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode val = entry.getValue();
                if (key.endsWith(".$")) {
                    String realKey = key.substring(0, key.length() - 2);
                    resolved.set(realKey, resolvePath(val.asText(), input));
                } else {
                    resolved.set(key, val);
                }
            }
            return resolved;
        }
        return parameters;
    }

    JsonNode resolvePath(String path, JsonNode root) {
        if (path == null || "$".equals(path)) {
            return root;
        }
        if (!path.startsWith("$.")) {
            return NullNode.getInstance();
        }
        String[] parts = path.substring(2).split("\\.");
        JsonNode current = root;
        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return NullNode.getInstance();
            }
            // Handle array index notation like field[0]
            if (part.contains("[")) {
                int bracketOpen = part.indexOf('[');
                int bracketClose = part.indexOf(']');
                String fieldName = part.substring(0, bracketOpen);
                int index = Integer.parseInt(part.substring(bracketOpen + 1, bracketClose));
                current = current.path(fieldName).path(index);
            } else {
                current = current.path(part);
            }
        }
        return current.isMissingNode() ? NullNode.getInstance() : current;
    }

    private void setPath(ObjectNode root, String path, JsonNode value) {
        if (!path.startsWith("$.") && !"$".equals(path)) {
            return;
        }
        if ("$".equals(path)) {
            return;
        }
        String[] parts = path.substring(2).split("\\.");
        ObjectNode current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode next = current.path(parts[i]);
            if (!next.isObject()) {
                ObjectNode newNode = objectMapper.createObjectNode();
                current.set(parts[i], newNode);
                current = newNode;
            } else {
                current = (ObjectNode) next;
            }
        }
        current.set(parts[parts.length - 1], value);
    }

    private String globToRegex(String glob) {
        return "\\Q" + glob.replace("*", "\\E.*\\Q") + "\\E";
    }

    // ──────────────────────────── History helpers ────────────────────────────

    private void addEvent(List<HistoryEvent> history, AtomicLong counter, String type,
                          Long prevId, Map<String, Object> details) {
        HistoryEvent event = new HistoryEvent();
        event.setId(counter.incrementAndGet());
        event.setType(type);
        event.setPreviousEventId(prevId);
        event.setDetails(details);
        history.add(event);
    }

    private String stateEnteredEventType(String stateType) {
        return stateType + "StateEntered";
    }

    private String stateExitedEventType(String stateType) {
        return stateType + "StateExited";
    }

    private JsonNode parseInput(String input) {
        if (input == null || input.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(input);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private String extractRegionFromArn(String arn) {
        String[] parts = arn.split(":");
        return parts.length > 3 ? parts[3] : "us-east-1";
    }

    record StateResult(JsonNode output, String nextState) {}

    static class FailStateException extends RuntimeException {
        final String error;
        final String cause;

        FailStateException(String error, String cause) {
            super(error + ": " + cause);
            this.error = error;
            this.cause = cause;
        }
    }
}
