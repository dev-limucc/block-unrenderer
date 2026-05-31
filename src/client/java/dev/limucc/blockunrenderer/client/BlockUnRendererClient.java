package dev.limucc.blockunrenderer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.blockunrenderer.BlockUnRenderer;
import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.gui.BlockManagerScreen;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public class BlockUnRendererClient implements ClientModInitializer {

    /** X — toggle/hold hiding. */
    public static KeyMapping TOGGLE_KEY;
    /** O — open the Block Manager GUI directly (no ModMenu needed). */
    public static KeyMapping OPEN_KEY;

    private static boolean toggledOn = false;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        HideState.rebuildFromConfig();

        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.toggle",
                        InputConstants.KEY_X, KeyMapping.Category.GAMEPLAY));

        OPEN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.open",
                        InputConstants.KEY_O, KeyMapping.Category.GAMEPLAY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ModConfig cfg = ConfigManager.get();

            if (cfg.triggerMode == ModConfig.TriggerMode.HOLD) {
                HideState.setActive(TOGGLE_KEY.isDown());
            } else {
                while (TOGGLE_KEY.consumeClick()) {
                    toggledOn = !toggledOn;
                    HideState.setActive(toggledOn);
                    if (mc.player != null) {
                        mc.player.sendOverlayMessage(Component.literal(
                                "Block UN-renderer: " + (toggledOn ? "§aHIDDEN" : "§cVISIBLE")));
                    }
                }
            }

            // Open the GUI with the dedicated key
            while (OPEN_KEY.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new BlockManagerScreen(null));
                }
            }
        });

        BlockUnRenderer.LOGGER.info("Block UN-renderer client ready. Toggle: X, Open GUI: O");
    }
}
