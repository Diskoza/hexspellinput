package dev.diskozawr.hexspellinput.mixin.client;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import dev.diskozawr.hexspellinput.HexSpellInputMod;
import dev.diskozawr.hexspellinput.client.SpellDrawingController;
import dev.diskozawr.hexspellinput.client.parser.PatternSequenceParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiSpellcasting.class, remap = false)
public abstract class GuiSpellcastingMixin extends Screen {
    @Unique private static String hexspellinput$lastInput = "";
    @Unique private static final int PANEL_WIDTH = 150;
    @Unique private static final int PANEL_HEIGHT = 58;
    @Unique private EditBox hexspellinput$input;
    @Unique private Button hexspellinput$drawButton;
    @Unique private Button hexspellinput$stopButton;
    @Unique private SpellDrawingController hexspellinput$controller;
    @Unique private SpellDrawingController.Status hexspellinput$status =
        SpellDrawingController.Status.normal(Component.translatable("hexspellinput.status.ready"));
    @Unique private Rect2i hexspellinput$panelArea;

    protected GuiSpellcastingMixin(Component title) {
        super(title);
    }

    @Inject(method = "m_7856_", at = @At("TAIL"), remap = false)
    private void hexspellinput$init(CallbackInfo ci) {
        int panelWidth = Math.max(110, Math.min(PANEL_WIDTH, this.width - 12));
        int x = Math.max(8, this.width - panelWidth - 8);
        int y = 6;
        hexspellinput$panelArea = new Rect2i(x - 3, y - 3, panelWidth + 6, PANEL_HEIGHT);

        hexspellinput$input = new EditBox(this.font, x, y, panelWidth, 18,
            Component.translatable("hexspellinput.input.hint")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == 257 || keyCode == 335) {
                    hexspellinput$startDrawing();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        hexspellinput$input.setMaxLength(PatternSequenceParser.MAX_INPUT_LENGTH);
        hexspellinput$input.setHint(Component.translatable("hexspellinput.input.hint"));
        hexspellinput$input.setValue(hexspellinput$lastInput);
        hexspellinput$input.setResponder(value -> hexspellinput$lastInput = value);
        this.addRenderableWidget(hexspellinput$input);

        int buttonY = y + 21;
        int gap = 4;
        int buttonWidth = (panelWidth - gap) / 2;
        hexspellinput$drawButton = this.addRenderableWidget(Button.builder(
            Component.translatable("hexspellinput.button.draw"),
            button -> hexspellinput$startDrawing()
        ).bounds(x, buttonY, buttonWidth, 18).build());
        hexspellinput$stopButton = this.addRenderableWidget(Button.builder(
            Component.translatable("hexspellinput.button.stop"),
            button -> hexspellinput$controller.stop()
        ).bounds(x + buttonWidth + gap, buttonY, buttonWidth, 18).build());

        hexspellinput$controller = new SpellDrawingController(
            (GuiSpellcasting) (Object) this,
            () -> hexspellinput$panelArea,
            status -> hexspellinput$status = status
        );
        hexspellinput$stopButton.active = false;
    }

    @Unique
    private void hexspellinput$startDrawing() {
        if (hexspellinput$controller == null || hexspellinput$controller.isActive()) {
            return;
        }
        try {
            PatternSequenceParser.ParseResult result =
                PatternSequenceParser.parse(hexspellinput$input.getValue());
            hexspellinput$controller.start(result.patterns());
            hexspellinput$drawButton.active = false;
            hexspellinput$stopButton.active = true;
            hexspellinput$input.setFocused(false);
        } catch (PatternSequenceParser.ParseException exception) {
            HexSpellInputMod.LOGGER.warn(
                "Pattern parsing failed at token {}: {}",
                exception.patternNumber(),
                exception.getCause() == null ? exception.translationKey() : exception.getCause().getMessage()
            );
            hexspellinput$status = SpellDrawingController.Status.error(
                Component.translatable(exception.translationKey(), exception.arguments())
            );
        }
    }

    @Inject(method = "m_86600_", at = @At("TAIL"), remap = false)
    private void hexspellinput$tick(CallbackInfo ci) {
        if (hexspellinput$controller != null) {
            hexspellinput$controller.tick();
            boolean active = hexspellinput$controller.isActive();
            hexspellinput$drawButton.active = !active;
            hexspellinput$stopButton.active = active;
            hexspellinput$input.setEditable(!active);
        }
    }

    @Inject(method = "recvServerUpdate", at = @At("TAIL"), remap = false)
    private void hexspellinput$serverUpdate(ExecutionClientView info, int index, CallbackInfo ci) {
        if (hexspellinput$controller != null) {
            hexspellinput$controller.onServerUpdate();
        }
    }

    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true, remap = false)
    private void hexspellinput$mouseClicked(
        double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir
    ) {
        if (hexspellinput$controller == null) {
            return;
        }
        boolean inPanel = hexspellinput$panelArea != null
            && hexspellinput$panelArea.contains((int) mouseX, (int) mouseY);
        if (inPanel) {
            super.mouseClicked(mouseX, mouseY, button);
            cir.setReturnValue(true);
        } else if (hexspellinput$controller.isActive()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "m_7379_", at = @At("HEAD"), remap = false)
    private void hexspellinput$onClose(CallbackInfo ci) {
        if (hexspellinput$controller != null && hexspellinput$controller.isDrawing()) {
            hexspellinput$controller.cancelForEscape();
        }
    }

    @Inject(method = "m_88315_", at = @At("HEAD"), remap = false)
    private void hexspellinput$renderPanelBackground(
        GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci
    ) {
        if (hexspellinput$panelArea != null) {
            int x = hexspellinput$panelArea.getX();
            int y = hexspellinput$panelArea.getY();
            graphics.fill(
                x, y,
                x + hexspellinput$panelArea.getWidth(),
                y + hexspellinput$panelArea.getHeight(),
                0xb0101010
            );
        }
    }

    @Inject(method = "m_88315_", at = @At("TAIL"), remap = false)
    private void hexspellinput$renderStatus(
        GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci
    ) {
        if (hexspellinput$panelArea != null && hexspellinput$status != null) {
            int availableWidth = hexspellinput$panelArea.getWidth() - 8;
            int textWidth = this.font.width(hexspellinput$status.text());
            float scale = textWidth <= availableWidth
                ? 1.0f
                : Math.max(0.55f, availableWidth / (float) textWidth);
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.drawString(
                this.font,
                hexspellinput$status.text(),
                Math.round((hexspellinput$panelArea.getX() + 4) / scale),
                Math.round((hexspellinput$panelArea.getY() + 45) / scale),
                hexspellinput$status.color(),
                false
            );
            graphics.pose().popPose();
        }
    }
}
