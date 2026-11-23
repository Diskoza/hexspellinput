package dev.diskozawr.hexspellinput.client.placement;

import at.petrak.hexcasting.api.casting.math.HexCoord;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class PatternPlacementFinder {
    public static final int SCREEN_MARGIN = 12;
    public static final int MAX_SEARCH_RADIUS = 128;

    private PatternPlacementFinder() {
    }

    public static Optional<HexCoord> find(
        List<HexCoord> relativePositions,
        Set<HexCoord> usedSpots,
        Predicate<HexCoord> allowedPosition
    ) {
        for (int radius = 0; radius <= MAX_SEARCH_RADIUS; radius++) {
            for (HexCoord candidate : ring(radius)) {
                boolean valid = true;
                for (HexCoord relative : relativePositions) {
                    HexCoord absolute = relative.plus(candidate);
                    if (usedSpots.contains(absolute) || !allowedPosition.test(absolute)) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    static List<HexCoord> ring(int radius) {
        if (radius == 0) {
            return List.of(HexCoord.getOrigin());
        }
        java.util.ArrayList<HexCoord> result = new java.util.ArrayList<>(radius * 6);
        for (int q = -radius; q <= radius; q++) {
            int rMin = Math.max(-radius, -q - radius);
            int rMax = Math.min(radius, -q + radius);
            for (int r = rMin; r <= rMax; r++) {
                HexCoord coord = new HexCoord(q, r);
                if (coord.distanceTo(HexCoord.getOrigin()) == radius) {
                    result.add(coord);
                }
            }
        }
        result.sort(java.util.Comparator.comparingInt(HexCoord::getQ).thenComparingInt(HexCoord::getR));
        return result;
    }
}
