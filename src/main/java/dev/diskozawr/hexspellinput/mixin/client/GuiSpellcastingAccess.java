package dev.diskozawr.hexspellinput.mixin.client;

import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(value = GuiSpellcasting.class, remap = false)
public interface GuiSpellcastingAccess {
    @Invoker("drawStart")
    boolean hexspellinput$drawStart(double x, double y);

    @Invoker("drawMove")
    boolean hexspellinput$drawMove(double x, double y);

    @Invoker("drawEnd")
    boolean hexspellinput$drawEnd();

    @Accessor("usedSpots")
    Set<HexCoord> hexspellinput$usedSpots();
}
