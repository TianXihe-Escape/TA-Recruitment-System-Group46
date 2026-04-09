package util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser and pretty printer using only JDK classes.
 */
public final class JsonUtil {
    private JsonUtil() {
    }

    public static Object parse(String json) {
        return new Parser(json).parse();
    }

    public static String toPrettyJson(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value, 0);
        return builder.toString();
    }

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

    private static void indent(StringBuilder builder, int indent) {
        builder.append(" ".repeat(Math.max(0, indent)));
    }

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

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json == null ? "" : json;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != json.length()) {
                throw new IllegalArgumentException("Unexpected trailing JSON content at index " + index);
            }
            return value;
        }

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

        private Object parseLiteral(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw new IllegalArgumentException("Unexpected token at index " + index);
            }
            index += literal.length();
            return value;
        }

        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        private void expect(char expected) {
            if (!peek(expected)) {
                throw new IllegalArgumentException("Expected '" + expected + "' at index " + index);
            }
            index++;
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }
    }
}
