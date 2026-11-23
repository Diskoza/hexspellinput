package dev.diskozawr.hexspellinput.client;

import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import dev.diskozawr.hexspellinput.HexSpellInputMod;
import dev.diskozawr.hexspellinput.mixin.client.GuiSpellcastingAccess;
import dev.diskozawr.hexspellinput.client.parser.PatternSequenceParser.ParsedPattern;
import dev.diskozawr.hexspellinput.client.placement.PatternPlacementFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SpellDrawingController {
    public static final int SERVER_RESPONSE_TIMEOUT_TICKS = 100;

    private final GuiSpellcasting screen;
    private final GuiSpellcastingAccess access;
    private final Supplier<Rect2i> reservedArea;
    private final Consumer<Status> statusSink;
    private final Queue<ParsedPattern> queue = new ArrayDeque<>();

    private State state = State.IDLE;
    private ParsedPattern current;
    private List<HexCoord> absolutePositions = List.of();
    private int nextPosition;
    private int waitTicks;
    private int total;
    private int completed;

    public SpellDrawingController(
        GuiSpellcasting screen,
        Supplier<Rect2i> reservedArea,
        Consumer<Status> statusSink
    ) {
        this.screen = screen;
        this.access = (GuiSpellcastingAccess) (Object) screen;
        this.reservedArea = reservedArea;
        this.statusSink = statusSink;
    }

    public void start(List<ParsedPattern> patterns) {
        stopSilently();
        queue.addAll(patterns);
        total = patterns.size();
        completed = 0;
        state = State.READY;
        statusSink.accept(Status.normal(Component.translatable("hexspellinput.status.checked", total)));
        HexSpellInputMod.LOGGER.info("Started automatic drawing queue with {} patterns", total);
    }

    public void tick() {
        if (state == State.IDLE) {
            return;
        }
        if (Minecraft.getInstance().screen != screen) {
            stopSilently();
            return;
        }
        if (state == State.WAITING) {
            if (++waitTicks >= SERVER_RESPONSE_TIMEOUT_TICKS) {
                HexSpellInputMod.LOGGER.warn("Timed out waiting for Hex Casting server response");
                fail("hexspellinput.error.timeout");
            }
            return;
        }
        if (state == State.READY) {
            beginNextPattern();
            return;
        }
        drawNextPoint();
    }

    private void beginNextPattern() {
        current = queue.poll();
        if (current == null) {
            finish();
            return;
        }

        List<HexCoord> relative = current.pattern().positions();
        Optional<HexCoord> origin = PatternPlacementFinder.find(
            relative,
            access.hexspellinput$usedSpots(),
            this::isVisibleAndOutsidePanel
        );
        if (origin.isEmpty()) {
            fail("hexspellinput.error.no_space");
            return;
        }

        absolutePositions = new ArrayList<>(relative.size());
        for (HexCoord coordinate : relative) {
            absolutePositions.add(coordinate.plus(origin.get()));
        }
        if (absolutePositions.size() < 2) {
            fail("hexspellinput.error.no_space");
            return;
        }

        Vec2 first = screen.coordToPx(absolutePositions.get(0));
        access.hexspellinput$drawStart(first.x, first.y);
        nextPosition = 1;
        state = State.DRAWING;
        statusSink.accept(Status.normal(Component.translatable(
            "hexspellinput.status.drawing", completed + 1, total
        )));
    }

    private void drawNextPoint() {
        if (nextPosition < absolutePositions.size()) {
            HexCoord previousCoordinate = absolutePositions.get(nextPosition - 1);
            HexCoord nextCoordinate = absolutePositions.get(nextPosition++);
            Vec2 anchor = screen.coordToPx(previousCoordinate);
            Vec2 exactPoint = screen.coordToPx(nextCoordinate);

            float deltaX = exactPoint.x - anchor.x;
            float deltaY = exactPoint.y - anchor.y;
            float distance = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
            double threshold = Mth.clamp(HexConfig.client().gridSnapThreshold(), 0.5, 1.0);
            float requiredDistance =
                (float) (screen.hexSize() * Math.sqrt(2.0 * threshold) + 0.05);
            float multiplier = distance == 0.0f ? 1.0f : requiredDistance / distance;
            Vec2 dragPoint = new Vec2(
                anchor.x + deltaX * multiplier,
                anchor.y + deltaY * multiplier
            );
            access.hexspellinput$drawMove(dragPoint.x, dragPoint.y);
            return;
        }
        access.hexspellinput$drawEnd();
        completed++;
        state = State.WAITING;
        waitTicks = 0;
        statusSink.accept(Status.normal(Component.translatable("hexspellinput.status.waiting")));
    }

    public void onServerUpdate() {
        if (state != State.WAITING) {
            return;
        }
        if (Minecraft.getInstance().screen != screen) {
            stopSilently();
            return;
        }
        state = queue.isEmpty() ? State.IDLE : State.READY;
        if (state == State.IDLE) {
            finish();
        }
    }

    public void stop() {
        boolean resetPartialDrawing = state == State.DRAWING;
        stopSilently();
        if (resetPartialDrawing) {
            screen.onClose();
        }
        statusSink.accept(Status.normal(Component.translatable("hexspellinput.status.stopped")));
        HexSpellInputMod.LOGGER.info("Automatic drawing queue stopped");
    }

    public void cancelForEscape() {
        stopSilently();
        statusSink.accept(Status.normal(Component.translatable("hexspellinput.status.stopped")));
        HexSpellInputMod.LOGGER.info("Automatic drawing queue cancelled by Escape");
    }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public boolean isDrawing() {
        return state == State.DRAWING;
    }

    private boolean isVisibleAndOutsidePanel(HexCoord coordinate) {
        Vec2 pixel = screen.coordToPx(coordinate);
        if (pixel.x < PatternPlacementFinder.SCREEN_MARGIN
            || pixel.y < PatternPlacementFinder.SCREEN_MARGIN
            || pixel.x > screen.width - PatternPlacementFinder.SCREEN_MARGIN
            || pixel.y > screen.height - PatternPlacementFinder.SCREEN_MARGIN) {
            return false;
        }
        Rect2i reserved = reservedArea.get();
        return reserved == null || !reserved.contains((int) pixel.x, (int) pixel.y);
    }

    private void finish() {
        int drawn = completed;
        stopSilently();
        statusSink.accept(Status.success(Component.translatable("hexspellinput.status.complete", drawn)));
        HexSpellInputMod.LOGGER.info("Completed automatic drawing queue with {} patterns", drawn);
    }

    private void fail(String translationKey) {
        boolean resetPartialDrawing = state == State.DRAWING;
        stopSilently();
        if (resetPartialDrawing) {
            screen.onClose();
        }
        statusSink.accept(Status.error(Component.translatable(translationKey)));
    }

    private void stopSilently() {
        queue.clear();
        state = State.IDLE;
        current = null;
        absolutePositions = List.of();
        nextPosition = 0;
        waitTicks = 0;
        total = 0;
        completed = 0;
    }

    private enum State {
        IDLE,
        READY,
        DRAWING,
        WAITING
    }

    public record Status(Component text, int color) {
        public static Status normal(Component text) {
            return new Status(text, 0xffdddddd);
        }

        public static Status error(Component text) {
            return new Status(text, 0xffff5555);
        }

        public static Status success(Component text) {
            return new Status(text, 0xff55ff55);
        }
    }
}
