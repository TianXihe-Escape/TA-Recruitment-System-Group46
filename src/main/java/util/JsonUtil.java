package util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser and pretty printer using only JDK classes.
 *
 * This utility avoids any third-party JSON dependency by supporting just the
 * subset of JSON behavior the project needs for reading and writing its local
 * data files. The tradeoff is that parsing and formatting rules are implemented
 * manually rather than delegated to a dedicated JSON library.
 */
public final class JsonUtil {
    /**
     * Utility class; callers use the static parse/format helpers only.
     */
    private JsonUtil() {
    }

    /**
     * Parses one JSON document into plain JDK collections and scalar values.
     *
     * Returned values follow a simple shape:
     * - objects become {@code Map<String, Object>}
     * - arrays become {@code List<Object>}
     * - strings, booleans, null, and numbers stay as scalar values
     */
    public static Object parse(String json) {
        return new Parser(json).parse();
    }

    /**
     * Converts a parsed value tree back into indented JSON text.
     *
     * The output is deterministic and human-readable so saved repository files
     * remain easy to inspect and diff in version control.
     */
    public static String toPrettyJson(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value, 0);
        return builder.toString();
    }

    /**
     * Dispatches one value to the matching JSON writer branch.
     *
     * The method accepts only the project-supported JSON-compatible types and
     * rejects any unexpected Java object with an explicit exception.
     */
    private static void writeValue(StringBuilder builder, Object value, int indent) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String stringValue) {
            builder.append('"').append(escape(stringValue)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        if (value instanceof List<?> listValue) {
            writeList(builder, listValue, indent);
            return;
        }
        if (value instanceof Map<?, ?> mapValue) {
            writeMap(builder, mapValue, indent);
            return;
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    /**
     * Writes a JSON array using multi-line pretty formatting.
     *
     * Empty arrays are kept compact, while non-empty arrays place each value on
     * its own indented line for easier reading in saved data files.
     */
    private static void writeList(StringBuilder builder, List<?> values, int indent) {
        if (values.isEmpty()) {
            builder.append("[ ]");
            return;
        }
        builder.append("[\n");
        for (int index = 0; index < values.size(); index++) {
            indent(builder, indent + 2);
            writeValue(builder, values.get(index), indent + 2);
            if (index < values.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        indent(builder, indent);
        builder.append(']');
    }

    /**
     * Writes a JSON object while preserving iteration order from the supplied map.
     *
     * LinkedHashMap is used by the parser so keys round-trip in a stable order,
     * which helps reduce noisy diffs in generated JSON files.
     */
    private static void writeMap(StringBuilder builder, Map<?, ?> values, int indent) {
        if (values.isEmpty()) {
            builder.append("{ }");
            return;
        }
        builder.append("{\n");
        int index = 0;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            indent(builder, indent + 2);
            builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(" : ");
            writeValue(builder, entry.getValue(), indent + 2);
            if (index < values.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
            index++;
        }
        indent(builder, indent);
        builder.append('}');
    }

    /**
     * Appends a fixed number of leading spaces for pretty-print indentation.
     */
    private static void indent(StringBuilder builder, int indent) {
        builder.append(" ".repeat(Math.max(0, indent)));
    }

    /**
     * Escapes control characters and special JSON string characters.
     *
     * This ensures written strings can be parsed back safely even when they
     * contain quotes, backslashes, tabs, or non-printable control characters.
     */
    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }

    /**
     * Small recursive-descent parser for the subset of JSON used by this project.
     *
     * The parser walks the input text with a mutable index and delegates each
     * syntactic structure to a specialized parse method.
     */
    private static final class Parser {
        /**
         * Raw JSON text being parsed.
         */
        private final String json;
        /**
         * Current cursor position inside the JSON text.
         */
        private int index;

        private Parser(String json) {
            this.json = json == null ? "" : json;
        }

        /**
         * Parses the full document and rejects trailing non-whitespace content.
         */
        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != json.length()) {
                throw new IllegalArgumentException("Unexpected trailing JSON content at index " + index);
            }
            return value;
        }

        /**
         * Parses the next JSON value based on the current leading character.
         *
         * This is the main dispatch point for object, array, string, literal,
         * and numeric parsing.
         */
        private Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON.");
            }
            char current = json.charAt(index);
            return switch (current) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (current == '-' || Character.isDigit(current)) {
                        yield parseNumber();
                    }
                    throw new IllegalArgumentException("Unexpected token at index " + index);
                }
            };
        }

        /**
         * Parses one JSON object into a LinkedHashMap.
         *
         * LinkedHashMap is chosen so key order is preserved in the same order as
         * it appeared in the source JSON text.
         */
        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> map = new LinkedHashMap<>();
            if (peek('}')) {
                expect('}');
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    expect(',');
                    continue;
                }
                expect('}');
                return map;
            }
        }

        /**
         * Parses one JSON array into a mutable ArrayList.
         */
        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> list = new ArrayList<>();
            if (peek(']')) {
                expect(']');
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    expect(',');
                    continue;
                }
                expect(']');
                return list;
            }
        }

        /**
         * Parses a quoted JSON string, including supported escape sequences.
         */
        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < json.length()) {
                char current = json.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("Unterminated escape sequence.");
                    }
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            if (index + 4 > json.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape.");
                            }
                            String hex = json.substring(index, index + 4);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("Unsupported escape sequence: \\" + escaped);
                    }
                } else {
                    builder.append(current);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        /**
         * Parses an integer or decimal number.
         *
         * Whole numbers are returned as {@code Long} and decimal numbers as
         * {@code Double}, which is sufficient for the repository data used here.
         */
        private Object parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            if (index < json.length() && json.charAt(index) == '.') {
                index++;
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            String number = json.substring(start, index);
            if (number.contains(".")) {
                return Double.parseDouble(number);
            }
            return Long.parseLong(number);
        }

        /**
         * Parses one exact literal token such as true, false, or null.
         */
        private Object parseLiteral(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw new IllegalArgumentException("Unexpected token at index " + index);
            }
            index += literal.length();
            return value;
        }

        /**
         * Checks whether the current character matches the expected one.
         */
        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        /**
         * Consumes one required structural character or throws a parse error.
         */
        private void expect(char expected) {
            if (!peek(expected)) {
                throw new IllegalArgumentException("Expected '" + expected + "' at index " + index);
            }
            index++;
        }

        /**
         * Advances past any JSON-insignificant whitespace.
         */
        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }
    }
}
