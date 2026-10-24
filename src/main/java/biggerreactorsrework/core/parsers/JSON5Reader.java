package biggerreactorsrework.core.parsers;

import biggerreactorsrework.core.util.NonnullDefault;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JSON5 reader producing plain Java values: {@link Map} (insertion ordered), {@link List}, {@link String},
 * {@link Boolean}, {@link Long}/{@link Double} and {@code null}.
 * <p>
 * Supports comments, unquoted identifier keys, single or double quoted strings, trailing commas,
 * hexadecimal integers, leading/trailing decimal points, explicit signs, {@code Infinity} and {@code NaN}.
 */
@NonnullDefault
public final class JSON5Reader {

    public static final class ParseException extends Exception {
        public ParseException(String message, int position) {
            super(message + " at character " + position);
        }
    }

    private final String text;
    private int pos = 0;

    private JSON5Reader(String text) {
        this.text = text;
    }

    @Nullable
    public static Object parse(String text) throws ParseException {
        final var reader = new JSON5Reader(text);
        reader.skipWhitespaceAndComments();
        final var value = reader.readValue();
        reader.skipWhitespaceAndComments();
        if (reader.pos != text.length()) {
            throw new ParseException("Unexpected trailing content", reader.pos);
        }
        return value;
    }

    private boolean atEnd() {
        return pos >= text.length();
    }

    private char peek() throws ParseException {
        if (atEnd()) {
            throw new ParseException("Unexpected end of input", pos);
        }
        return text.charAt(pos);
    }

    private void skipWhitespaceAndComments() throws ParseException {
        while (!atEnd()) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '﻿') {
                pos++;
            } else if (c == '/' && pos + 1 < text.length() && text.charAt(pos + 1) == '/') {
                while (!atEnd() && text.charAt(pos) != '\n') {
                    pos++;
                }
            } else if (c == '/' && pos + 1 < text.length() && text.charAt(pos + 1) == '*') {
                final int end = text.indexOf("*/", pos + 2);
                if (end < 0) {
                    throw new ParseException("Unterminated comment", pos);
                }
                pos = end + 2;
            } else {
                return;
            }
        }
    }

    @Nullable
    private Object readValue() throws ParseException {
        char c = peek();
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
            case '\'':
                return readString();
            default:
                break;
        }
        if (c == '-' || c == '+' || c == '.' || Character.isDigit(c)) {
            return readNumber();
        }
        final var word = readIdentifier();
        return switch (word) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            case "null" -> null;
            case "Infinity" -> Double.POSITIVE_INFINITY;
            case "NaN" -> Double.NaN;
            default -> throw new ParseException("Unexpected token '" + word + "'", pos);
        };
    }

    private Map<String, Object> readObject() throws ParseException {
        final var map = new LinkedHashMap<String, Object>();
        pos++; // {
        while (true) {
            skipWhitespaceAndComments();
            if (peek() == '}') {
                pos++;
                return map;
            }
            final String key;
            char c = peek();
            if (c == '"' || c == '\'') {
                key = readString();
            } else {
                key = readIdentifier();
                if (key.isEmpty()) {
                    throw new ParseException("Expected object key", pos);
                }
            }
            skipWhitespaceAndComments();
            if (peek() != ':') {
                throw new ParseException("Expected ':'", pos);
            }
            pos++;
            skipWhitespaceAndComments();
            map.put(key, readValue());
            skipWhitespaceAndComments();
            c = peek();
            if (c == ',') {
                pos++;
            } else if (c != '}') {
                throw new ParseException("Expected ',' or '}'", pos);
            }
        }
    }

    private List<Object> readArray() throws ParseException {
        final var list = new ArrayList<Object>();
        pos++; // [
        while (true) {
            skipWhitespaceAndComments();
            if (peek() == ']') {
                pos++;
                return list;
            }
            list.add(readValue());
            skipWhitespaceAndComments();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c != ']') {
                throw new ParseException("Expected ',' or ']'", pos);
            }
        }
    }

    private String readString() throws ParseException {
        final char quote = text.charAt(pos++);
        final var builder = new StringBuilder();
        while (true) {
            char c = peek();
            pos++;
            if (c == quote) {
                return builder.toString();
            }
            if (c == '\n') {
                throw new ParseException("Unterminated string", pos);
            }
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            char e = peek();
            pos++;
            switch (e) {
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'v' -> builder.append('\u000B');
                case '0' -> builder.append('\0');
                case '\n' -> {
                    // line continuation
                }
                case '\r' -> {
                    if (!atEnd() && text.charAt(pos) == '\n') {
                        pos++;
                    }
                }
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw new ParseException("Truncated unicode escape", pos);
                    }
                    try {
                        builder.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw new ParseException("Invalid unicode escape", pos);
                    }
                    pos += 4;
                }
                default -> builder.append(e);
            }
        }
    }

    private String readIdentifier() {
        final int start = pos;
        while (!atEnd()) {
            char c = text.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$') {
                pos++;
            } else {
                break;
            }
        }
        return text.substring(start, pos);
    }

    private Object readNumber() throws ParseException {
        final int start = pos;
        boolean negative = false;
        char c = peek();
        if (c == '+' || c == '-') {
            negative = c == '-';
            pos++;
        }
        if (text.startsWith("Infinity", pos)) {
            pos += "Infinity".length();
            return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        if (text.startsWith("NaN", pos)) {
            pos += "NaN".length();
            return Double.NaN;
        }
        if (text.startsWith("0x", pos) || text.startsWith("0X", pos)) {
            pos += 2;
            final int hexStart = pos;
            while (!atEnd() && Character.digit(text.charAt(pos), 16) >= 0) {
                pos++;
            }
            try {
                final long value = Long.parseUnsignedLong(text.substring(hexStart, pos), 16);
                return negative ? -value : value;
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid hexadecimal number", start);
            }
        }
        boolean decimal = false;
        while (!atEnd()) {
            c = text.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E') {
                decimal = true;
                pos++;
            } else if ((c == '+' || c == '-') && (text.charAt(pos - 1) == 'e' || text.charAt(pos - 1) == 'E')) {
                pos++;
            } else {
                break;
            }
        }
        final var number = text.substring(start, pos);
        try {
            if (!decimal) {
                return Long.parseLong(number.startsWith("+") ? number.substring(1) : number);
            }
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            if (!decimal) {
                try {
                    return Double.parseDouble(number);
                } catch (NumberFormatException ignored) {
                }
            }
            throw new ParseException("Invalid number '" + number + "'", start);
        }
    }
}
