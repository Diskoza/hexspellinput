package dev.diskozawr.hexspellinput.client;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
import dev.diskozawr.hexspellinput.HexSpellInputMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = HexSpellInputMod.MOD_ID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ScrollPatternTooltip {
    private ScrollPatternTooltip() {
    }

    @SubscribeEvent
    public static void addPatternSignature(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemScroll)) {
            return;
        }

        CompoundTag stackTag = stack.getTag();
        if (stackTag == null || !stackTag.contains(ItemScroll.TAG_PATTERN, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag patternTag = stackTag.getCompound(ItemScroll.TAG_PATTERN);
        if (!HexPattern.isPattern(patternTag)) {
            return;
        }

        try {
            String signature = displaySignature(HexPattern.fromNBT(patternTag));
            event.getToolTip().add(
                Component.translatable("hexspellinput.tooltip.scroll_signature", signature)
                    .withStyle(ChatFormatting.AQUA)
            );
        } catch (RuntimeException ignored) {
        }
    }

    static String displaySignature(HexPattern pattern) {
        String signature = pattern.anglesSignature();
        return signature.isEmpty() ? "-" : signature;
    }
}
