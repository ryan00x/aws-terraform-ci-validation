package io.github.hectorvent.floci.services.s3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

public class S3SelectEvaluator {

    private static final Logger LOG = Logger.getLogger(S3SelectEvaluator.class);

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "SELECT\\s+(.+?)\\s+FROM\\s+S3OBJECT(?:\\s+(\\w+))?\\s*(?:WHERE\\s+(.+?))?\\s*(?:LIMIT\\s+(\\d+))?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static String evaluateCsv(String content, String expression, boolean useHeaders) {
        Matcher matcher = SELECT_PATTERN.matcher(expression.trim());
        if (!matcher.find()) {
            LOG.debugv("SQL Pattern did not match: {0}", expression);
            return content;
        }

        String projection = matcher.group(1).trim();
        String alias = matcher.group(2);
        String whereClause = matcher.group(3);
        String limitStr = matcher.group(4);

        String[] lines = content.split("\\r?\\n");
        if (lines.length == 0) return "";

        List<String> headerList = new ArrayList<>();
        int dataStart = 0;
        if (useHeaders) {
            headerList = Arrays.asList(lines[0].split(","));
            dataStart = 1;
        }

        List<String[]> rows = new ArrayList<>();
        for (int i = dataStart; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            rows.add(lines[i].split(","));
        }

        // Filter
        if (whereClause != null) {
            rows = filterRows(rows, headerList, alias, whereClause);
        }

        // Limit
        if (limitStr != null) {
            int limit = Integer.parseInt(limitStr);
            if (rows.size() > limit) {
                rows = rows.subList(0, limit);
            }
        }

        // Project
        return projectRows(rows, headerList, projection);
    }

    private static List<String[]> filterRows(List<String[]> rows, List<String> headers, String alias, String where) {
        String processedWhere = where;
        if (alias != null) {
            processedWhere = processedWhere.replaceAll("(?i)" + Pattern.quote(alias) + "\\.", "");
        }
        final String finalWhere = processedWhere;
        
        return rows.stream().filter(row -> {
            // Simple evaluation: col > val or col = val
            // Try to find a comparison
            Pattern compPattern = Pattern.compile("(\\w+)\\s*(>|=|<)\\s*(\\d+|'[^']*')", Pattern.CASE_INSENSITIVE);
            Matcher m = compPattern.matcher(finalWhere);
            if (m.find()) {
                String colName = m.group(1);
                String op = m.group(2);
                String valStr = m.group(3);
                
                String cellValue = getCellValue(row, headers, colName);
                if (cellValue == null) return false;

                if (valStr.startsWith("'")) {
                    String val = valStr.substring(1, valStr.length() - 1);
                    return op.equals("=") && cellValue.equalsIgnoreCase(val);
                } else {
                    try {
                        double cellNum = Double.parseDouble(cellValue);
                        double valNum = Double.parseDouble(valStr);
                        return switch (op) {
                            case ">" -> cellNum > valNum;
                            case "<" -> cellNum < valNum;
                            case "=" -> cellNum == valNum;
                            default -> false;
                        };
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    private static String projectRows(List<String[]> rows, List<String> headers, String projection) {
        if (projection.equals("*")) {
            return rows.stream()
                    .map(row -> String.join(",", row))
                    .collect(Collectors.joining("\n")) + (rows.isEmpty() ? "" : "\n");
        }

        String[] cols = projection.split(",");
        return rows.stream().map(row -> {
            List<String> projected = new ArrayList<>();
            for (String col : cols) {
                projected.add(getCellValue(row, headers, col.trim()));
            }
            return String.join(",", projected);
        }).collect(Collectors.joining("\n")) + (rows.isEmpty() ? "" : "\n");
    }

    private static String getCellValue(String[] row, List<String> headers, String colName) {
        if (colName.startsWith("_")) {
            try {
                int idx = Integer.parseInt(colName.substring(1)) - 1;
                return idx < row.length ? row[idx] : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).equalsIgnoreCase(colName)) {
                return i < row.length ? row[i] : null;
            }
        }
        return null;
    }
}
