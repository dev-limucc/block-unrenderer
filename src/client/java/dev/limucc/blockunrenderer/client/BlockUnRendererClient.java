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
import net.minecraft.resources.Identifier;

public class BlockUnRendererClient implements ClientModInitializer {

    /** Our own controls section: Options → Controls → Block UN-renderer. */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("block_unrenderer", "main"));

    /** Toggle/hold hiding. Unbound by default. */
    public static KeyMapping TOGGLE_KEY;
    /** Open the Block Manager GUI directly. Unbound by default. */
    public static KeyMapping OPEN_KEY;

    private static boolean toggledOn = false;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        HideState.rebuildFromConfig();

        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY));

        OPEN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.open", InputConstants.UNKNOWN.getValue(), CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ModConfig cfg = ConfigManager.get();

            if (cfg.triggerMode == ModConfig.TriggerMode.HOLD) {
                // isDown() is false while the key is unbound, so this is a safe no-op until bound.
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

        BlockUnRenderer.LOGGER.info(
                "Block UN-renderer client ready. Bind keys under Options → Controls → Block UN-renderer.");
    }
}
