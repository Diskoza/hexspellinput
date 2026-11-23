package dev.diskozawr.hexspellinput.client.parser;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class PatternSequenceParser {
    public static final int MAX_INPUT_LENGTH = 32_768;
    public static final int MAX_PATTERN_COUNT = 256;
    public static final int MAX_PATTERN_LENGTH = 2_048;

    private static final Pattern SEPARATORS = Pattern.compile("[\\s,;|]+");
    private static final Pattern VALID_ANGLES = Pattern.compile("[qwedsa]*");
    private static final Map<String, HexDir> DIRECTIONS = Map.of(
        "ne", HexDir.NORTH_EAST,
        "e", HexDir.EAST,
        "se", HexDir.SOUTH_EAST,
        "sw", HexDir.SOUTH_WEST,
        "w", HexDir.WEST,
        "nw", HexDir.NORTH_WEST
    );

    private PatternSequenceParser() {
    }

    public static ParseResult parse(String input) throws ParseException {
        if (input == null || input.trim().isEmpty()) {
            throw new ParseException("hexspellinput.error.empty", 0);
        }
        if (input.length() > MAX_INPUT_LENGTH) {
            throw new ParseException("hexspellinput.error.input_too_long", 0);
        }

        String[] rawTokens = SEPARATORS.split(input.trim());
        if (rawTokens.length == 0) {
            throw new ParseException("hexspellinput.error.empty", 0);
        }
        if (rawTokens.length > MAX_PATTERN_COUNT) {
            throw new ParseException("hexspellinput.error.too_many", 0);
        }

        List<ParsedPattern> parsed = new ArrayList<>(rawTokens.length);
        for (int i = 0; i < rawTokens.length; i++) {
            String original = rawTokens[i];
            String token = original.toLowerCase(Locale.ROOT);
            int number = i + 1;
            if (token.length() > MAX_PATTERN_LENGTH) {
                throw new ParseException("hexspellinput.error.pattern_too_long", number, number);
            }

            HexDir direction = HexDir.NORTH_EAST;
            String signature = token;
            int colon = token.indexOf(':');
            if (colon >= 0) {
                String prefix = token.substring(0, colon);
                direction = DIRECTIONS.get(prefix);
                if (direction == null) {
                    throw new ParseException("hexspellinput.error.unknown_direction", number, number, prefix);
                }
                signature = token.substring(colon + 1);
            }
            if ("-".equals(signature)) {
                signature = "";
            }

            for (int c = 0; c < signature.length(); c++) {
                String character = String.valueOf(signature.charAt(c));
                if (!VALID_ANGLES.matcher(character).matches()) {
                    throw new ParseException("hexspellinput.error.invalid_character", number, number, character);
                }
            }

            try {
                parsed.add(new ParsedPattern(HexPattern.fromAngles(signature, direction), original, number));
            } catch (IllegalStateException ex) {
                throw new ParseException("hexspellinput.error.intersection", number, ex, number);
            } catch (IllegalArgumentException ex) {
                throw new ParseException("hexspellinput.error.invalid_pattern", number, ex, number, safeMessage(ex));
            }
        }
        return new ParseResult(List.copyOf(parsed));
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    public record ParsedPattern(HexPattern pattern, String sourceToken, int number) {
    }

    public record ParseResult(List<ParsedPattern> patterns) {
    }

    public static final class ParseException extends Exception {
        private final String translationKey;
        private final int patternNumber;
        private final Object[] arguments;

        public ParseException(String translationKey, int patternNumber, Object... arguments) {
            super(translationKey);
            this.translationKey = translationKey;
            this.patternNumber = patternNumber;
            this.arguments = arguments;
        }

        public ParseException(String translationKey, int patternNumber, Throwable cause, Object... arguments) {
            super(translationKey, cause);
            this.translationKey = translationKey;
            this.patternNumber = patternNumber;
            this.arguments = arguments;
        }

        public String translationKey() {
            return translationKey;
        }

        public int patternNumber() {
            return patternNumber;
        }

        public Object[] arguments() {
            return arguments.clone();
        }
    }
}
