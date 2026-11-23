package dev.diskozawr.hexspellinput.client.parser;

import at.petrak.hexcasting.api.casting.math.HexDir;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatternSequenceParserTest {
    @Test
    void parsesOnePattern() throws Exception {
        var result = PatternSequenceParser.parse("aqaa");
        assertEquals(1, result.patterns().size());
        assertEquals("aqaa", result.patterns().get(0).sourceToken());
        assertEquals(1, result.patterns().get(0).number());
    }

    @Test
    void acceptsAllSeparators() throws Exception {
        var result = PatternSequenceParser.parse("aqaa, qqq;\newd| w");
        assertEquals(4, result.patterns().size());
    }

    @Test
    void acceptsUppercase() throws Exception {
        assertEquals("aqaa", PatternSequenceParser.parse("AQAA")
            .patterns().get(0).pattern().anglesSignature());
    }

    @Test
    void parsesDirection() throws Exception {
        assertEquals(HexDir.SOUTH_WEST, PatternSequenceParser.parse("sw:aqaa")
            .patterns().get(0).pattern().getStartDir());
    }

    @Test
    void rejectsEmptyInput() {
        var exception = assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse(" \n , ; | "));
        assertEquals("hexspellinput.error.empty", exception.translationKey());
    }

    @Test
    void rejectsUnknownPrefix() {
        var exception = assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse("north:aqaa"));
        assertEquals("hexspellinput.error.unknown_direction", exception.translationKey());
    }

    @Test
    void rejectsInvalidCharacter() {
        var exception = assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse("aqr"));
        assertEquals("hexspellinput.error.invalid_character", exception.translationKey());
        assertEquals(1, exception.patternNumber());
    }

    @Test
    void rejectsSelfIntersection() {
        var exception = assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse("qqqqqq"));
        assertEquals("hexspellinput.error.intersection", exception.translationKey());
    }

    @Test
    void enforcesLimits() {
        assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse("w".repeat(PatternSequenceParser.MAX_INPUT_LENGTH + 1)));

        String tooMany = String.join(" ", java.util.Collections.nCopies(
            PatternSequenceParser.MAX_PATTERN_COUNT + 1, "-"
        ));
        assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse(tooMany));

        assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse("w".repeat(PatternSequenceParser.MAX_PATTERN_LENGTH + 1)));
    }

    @Test
    void oneBadTokenPreventsAnyResult() {
        var exception = assertThrows(PatternSequenceParser.ParseException.class,
            () -> PatternSequenceParser.parse("aqaa qqq r"));
        assertEquals(3, exception.patternNumber());
    }
}
