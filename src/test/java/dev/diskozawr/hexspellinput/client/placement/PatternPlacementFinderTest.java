package dev.diskozawr.hexspellinput.client.placement;

import at.petrak.hexcasting.api.casting.math.HexCoord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PatternPlacementFinderTest {
    private static final List<HexCoord> LINE =
        List.of(new HexCoord(0, 0), new HexCoord(1, 0));

    @Test
    void usesOriginOnEmptyBoard() {
        assertEquals(HexCoord.getOrigin(),
            PatternPlacementFinder.find(LINE, Set.of(), ignored -> true).orElseThrow());
    }

    @Test
    void movesAwayFromOccupiedCenter() {
        HexCoord found = PatternPlacementFinder.find(
            LINE, Set.of(HexCoord.getOrigin()), ignored -> true
        ).orElseThrow();
        assertNotEquals(HexCoord.getOrigin(), found);
    }

    @Test
    void avoidsSeveralOccupiedAreas() {
        Set<HexCoord> used = Set.of(
            new HexCoord(0, 0), new HexCoord(1, 0),
            new HexCoord(-1, 0), new HexCoord(-2, 0)
        );
        HexCoord found = PatternPlacementFinder.find(LINE, used, ignored -> true).orElseThrow();
        for (HexCoord relative : LINE) {
            assertFalse(used.contains(relative.plus(found)));
        }
    }

    @Test
    void reportsNoSpace() {
        assertTrue(PatternPlacementFinder.find(LINE, Set.of(), ignored -> false).isEmpty());
    }

    @Test
    void respectsReservedAreaPredicate() {
        HexCoord found = PatternPlacementFinder.find(
            LINE,
            Set.of(),
            coordinate -> coordinate.getQ() < 0
        ).orElseThrow();
        for (HexCoord relative : LINE) {
            assertTrue(relative.plus(found).getQ() < 0);
        }
    }
}
