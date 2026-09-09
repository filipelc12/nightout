package com.nightout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser JSON minimalista, sem dependencias externas.
 * Suporta objetos, arrays, strings, numeros, booleanos e null.
 * Objetos viram Map&lt;String,Object&gt;, arrays viram List&lt;Object&gt;.
 */
public final class Json {

    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    public static Object parse(String text) {
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        return (Map<String, Object>) parse(text);
    }

    private Object parseValue() {
        char c = peek();
        return switch (c) {
            case '{' -> parseObjectInternal();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObjectInternal() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char c = next();
            if (c == '}') break;
            if (c != ',') throw new IllegalStateException("Esperado ',' ou '}' na posicao " + pos);
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();
            char c = next();
            if (c == ']') break;
            if (c != ',') throw new IllegalStateException("Esperado ',' ou ']' na posicao " + pos);
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') break;
            if (c == '\\') {
                char esc = next();
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new IllegalStateException("Escape invalido: \\" + esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean parseBoolean() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new IllegalStateException("Valor booleano invalido na posicao " + pos);
    }

    private Object parseNull() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new IllegalStateException("Valor invalido na posicao " + pos);
    }

    private Double parseNumber() {
        int start = pos;
        while (pos < src.length() && "-+.eE0123456789".indexOf(src.charAt(pos)) >= 0) {
            pos++;
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        return src.charAt(pos);
    }

    private char next() {
        return src.charAt(pos++);
    }

    private void expect(char c) {
        if (src.charAt(pos) != c) {
            throw new IllegalStateException("Esperado '" + c + "' na posicao " + pos);
        }
        pos++;
    }
}
