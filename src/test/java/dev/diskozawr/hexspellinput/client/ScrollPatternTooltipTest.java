package dev.diskozawr.hexspellinput.client;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollPatternTooltipTest {
    @Test
    void displaysAngleSignature() {
        assertEquals("qqq", ScrollPatternTooltip.displaySignature(
            HexPattern.fromAngles("qqq", HexDir.NORTH_EAST)
        ));
    }

    @Test
    void displaysStraightLineAsDash() {
        assertEquals("-", ScrollPatternTooltip.displaySignature(
            HexPattern.fromAngles("", HexDir.SOUTH_WEST)
        ));
    }
}
