package dev.limucc.blockunrenderer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.blockunrenderer.BlockUnRenderer;
import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.gui.BlockManagerScreen;
import dev.limucc.blockunrenderer.client.render.HideState;
import dev.limucc.blockunrenderer.net.OptInPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

        // ── Server-side opt-in (Modrinth Content Rules section 3) ──────────────────
        // The see-through feature is only permitted in singleplayer (your own world), or in
        // multiplayer when the server explicitly opts in by sending the handshake. Until then
        // HideState.allowed stays false and nothing is hidden.
        ClientPlayNetworking.registerGlobalReceiver(OptInPayload.TYPE, (payload, context) ->
                context.client().execute(() -> HideState.setAllowed(true)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.hasSingleplayerServer()) {
                HideState.setAllowed(true);   // your own world — allowed
            }
            // multiplayer: remains disabled until the server sends an opt-in handshake
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                HideState.setAllowed(false));

        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY));

        OPEN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.open", InputConstants.UNKNOWN.getValue(), CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ModConfig cfg = ConfigManager.get();

            if (cfg.triggerMode == ModConfig.TriggerMode.HOLD) {
                // isDown() is false while the key is unbound, so this is a safe no-op until bound.
                // The HideState gate keeps this inert when the server has not opted in.
                HideState.setActive(TOGGLE_KEY.isDown());
            } else {
                while (TOGGLE_KEY.consumeClick()) {
                    if (!HideState.isAllowed()) {
                        if (mc.player != null) {
                            mc.player.sendOverlayMessage(Component.literal(
                                    "§cBlock UN-renderer is disabled here — the server has not opted in."));
                        }
                        continue;
                    }
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
