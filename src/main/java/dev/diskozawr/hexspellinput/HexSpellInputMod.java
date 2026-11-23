package dev.diskozawr.hexspellinput;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(HexSpellInputMod.MOD_ID)
public final class HexSpellInputMod {
    public static final String MOD_ID = "hexspellinput";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HexSpellInputMod() {
        String hexVersion = ModList.get().getModContainerById("hexcasting")
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("not detected");
        LOGGER.info("Hex Spell Input started; Hex Casting version: {}", hexVersion);
    }
}
