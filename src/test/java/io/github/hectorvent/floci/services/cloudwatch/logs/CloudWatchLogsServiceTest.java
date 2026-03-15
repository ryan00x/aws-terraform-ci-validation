package io.github.hectorvent.floci.services.cloudwatch.logs;

import io.github.hectorvent.floci.core.common.AwsException;
import io.github.hectorvent.floci.core.common.RegionResolver;
import io.github.hectorvent.floci.core.storage.InMemoryStorage;
import io.github.hectorvent.floci.services.cloudwatch.logs.model.LogEvent;
import io.github.hectorvent.floci.services.cloudwatch.logs.model.LogGroup;
import io.github.hectorvent.floci.services.cloudwatch.logs.model.LogStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CloudWatchLogsServiceTest {

    private static final String REGION = "us-east-1";

    private CloudWatchLogsService service;

    @BeforeEach
    void setUp() {
        service = new CloudWatchLogsService(
                new InMemoryStorage<>(),
                new InMemoryStorage<>(),
                new InMemoryStorage<>(),
                10000,
                new RegionResolver("us-east-1", "000000000000")
        );
    }

    // ──────────────────────────── Log Groups ────────────────────────────

    @Test
    void createLogGroup() {
        service.createLogGroup("/app/logs", null, null, REGION);

        List<LogGroup> groups = service.describeLogGroups(null, REGION);
        assertEquals(1, groups.size());
        assertEquals("/app/logs", groups.getFirst().getLogGroupName());
    }

    @Test
    void createLogGroupDuplicateThrows() {
        service.createLogGroup("/app/logs", null, null, REGION);
        assertThrows(AwsException.class, () ->
                service.createLogGroup("/app/logs", null, null, REGION));
    }

    @Test
    void createLogGroupBlankNameThrows() {
        assertThrows(AwsException.class, () ->
                service.createLogGroup("", null, null, REGION));
    }

    @Test
    void deleteLogGroup() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.deleteLogGroup("/app/logs", REGION);

        assertTrue(service.describeLogGroups(null, REGION).isEmpty());
    }

    @Test
    void deleteLogGroupNotFoundThrows() {
        assertThrows(AwsException.class, () ->
                service.deleteLogGroup("/missing", REGION));
    }

    @Test
    void describeLogGroupsWithPrefix() {
        service.createLogGroup("/app/alpha", null, null, REGION);
        service.createLogGroup("/app/beta", null, null, REGION);
        service.createLogGroup("/other/logs", null, null, REGION);

        List<LogGroup> result = service.describeLogGroups("/app", REGION);
        assertEquals(2, result.size());
    }

    @Test
    void putAndDeleteRetentionPolicy() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.putRetentionPolicy("/app/logs", 30, REGION);

        LogGroup group = service.describeLogGroups("/app/logs", REGION).getFirst();
        assertEquals(30, group.getRetentionInDays());

        service.deleteRetentionPolicy("/app/logs", REGION);
        group = service.describeLogGroups("/app/logs", REGION).getFirst();
        assertNull(group.getRetentionInDays());
    }

    @Test
    void tagAndUntagLogGroup() {
        service.createLogGroup("/app/logs", null, Map.of("env", "prod"), REGION);
        service.tagLogGroup("/app/logs", Map.of("team", "platform"), REGION);

        Map<String, String> tags = service.listTagsLogGroup("/app/logs", REGION);
        assertEquals("prod", tags.get("env"));
        assertEquals("platform", tags.get("team"));

        service.untagLogGroup("/app/logs", List.of("env"), REGION);
        tags = service.listTagsLogGroup("/app/logs", REGION);
        assertFalse(tags.containsKey("env"));
    }

    // ──────────────────────────── Log Streams ────────────────────────────

    @Test
    void createLogStream() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);

        List<LogStream> streams = service.describeLogStreams("/app/logs", null, REGION);
        assertEquals(1, streams.size());
        assertEquals("stream-1", streams.getFirst().getLogStreamName());
    }

    @Test
    void createLogStreamForNonExistentGroupThrows() {
        assertThrows(AwsException.class, () ->
                service.createLogStream("/missing", "stream-1", REGION));
    }

    @Test
    void createLogStreamDuplicateThrows() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);
        assertThrows(AwsException.class, () ->
                service.createLogStream("/app/logs", "stream-1", REGION));
    }

    @Test
    void deleteLogStream() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);
        service.deleteLogStream("/app/logs", "stream-1", REGION);

        assertTrue(service.describeLogStreams("/app/logs", null, REGION).isEmpty());
    }

    @Test
    void deleteLogGroupCascadesStreamsAndEvents() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);
        service.putLogEvents("/app/logs", "stream-1",
                List.of(Map.of("timestamp", System.currentTimeMillis(), "message", "hello")), REGION);

        service.deleteLogGroup("/app/logs", REGION);
        assertTrue(service.describeLogGroups(null, REGION).isEmpty());
    }

    // ──────────────────────────── Log Events ────────────────────────────

    @Test
    void putAndGetLogEvents() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);

        long now = System.currentTimeMillis();
        service.putLogEvents("/app/logs", "stream-1", List.of(
                Map.of("timestamp", now, "message", "first"),
                Map.of("timestamp", now + 1, "message", "second")
        ), REGION);

        CloudWatchLogsService.LogEventsResult result = service.getLogEvents(
                "/app/logs", "stream-1", null, null, 100, true, REGION);
        assertEquals(2, result.events().size());
        assertEquals("first", result.events().get(0).getMessage());
        assertEquals("second", result.events().get(1).getMessage());
    }

    @Test
    void getLogEventsWithTimeRange() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);

        long base = System.currentTimeMillis();
        service.putLogEvents("/app/logs", "stream-1", List.of(
                Map.of("timestamp", base, "message", "old"),
                Map.of("timestamp", base + 10000, "message", "new")
        ), REGION);

        CloudWatchLogsService.LogEventsResult result = service.getLogEvents(
                "/app/logs", "stream-1", base + 5000, null, 100, true, REGION);
        assertEquals(1, result.events().size());
        assertEquals("new", result.events().getFirst().getMessage());
    }

    @Test
    void filterLogEvents() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);

        long now = System.currentTimeMillis();
        service.putLogEvents("/app/logs", "stream-1", List.of(
                Map.of("timestamp", now, "message", "ERROR: something failed"),
                Map.of("timestamp", now + 1, "message", "INFO: all good"),
                Map.of("timestamp", now + 2, "message", "ERROR: another failure")
        ), REGION);

        CloudWatchLogsService.FilteredLogEventsResult result = service.filterLogEvents(
                "/app/logs", null, null, null, "ERROR", 100, REGION);
        assertEquals(2, result.events().size());
        assertTrue(result.events().stream().allMatch(e -> e.getMessage().contains("ERROR")));
    }

    @Test
    void filterLogEventsNoPattern() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);

        long now = System.currentTimeMillis();
        service.putLogEvents("/app/logs", "stream-1", List.of(
                Map.of("timestamp", now, "message", "msg1"),
                Map.of("timestamp", now + 1, "message", "msg2")
        ), REGION);

        CloudWatchLogsService.FilteredLogEventsResult result = service.filterLogEvents(
                "/app/logs", null, null, null, null, 100, REGION);
        assertEquals(2, result.events().size());
    }

    @Test
    void putLogEventsUpdatesStreamMetadata() {
        service.createLogGroup("/app/logs", null, null, REGION);
        service.createLogStream("/app/logs", "stream-1", REGION);

        long now = System.currentTimeMillis();
        service.putLogEvents("/app/logs", "stream-1",
                List.of(Map.of("timestamp", now, "message", "test")), REGION);

        List<LogStream> streams = service.describeLogStreams("/app/logs", null, REGION);
        LogStream stream = streams.getFirst();
        assertEquals(now, stream.getFirstEventTimestamp());
        assertEquals(now, stream.getLastEventTimestamp());
        assertNotNull(stream.getLastIngestionTime());
    }

    @Test
    void maxEventsPerQueryIsRespected() {
        CloudWatchLogsService limitedService = new CloudWatchLogsService(
                new InMemoryStorage<>(),
                new InMemoryStorage<>(),
                new InMemoryStorage<>(),
                2,
                new RegionResolver("us-east-1", "000000000000")
        );

        limitedService.createLogGroup("/app/logs", null, null, REGION);
        limitedService.createLogStream("/app/logs", "stream-1", REGION);

        long now = System.currentTimeMillis();
        limitedService.putLogEvents("/app/logs", "stream-1", List.of(
                Map.of("timestamp", now, "message", "a"),
                Map.of("timestamp", now + 1, "message", "b"),
                Map.of("timestamp", now + 2, "message", "c")
        ), REGION);

        CloudWatchLogsService.LogEventsResult result = limitedService.getLogEvents(
                "/app/logs", "stream-1", null, null, 100, true, REGION);
        assertEquals(2, result.events().size());
    }
}