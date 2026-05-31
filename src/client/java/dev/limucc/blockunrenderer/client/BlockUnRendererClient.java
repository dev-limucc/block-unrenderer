package dev.limucc.blockunrenderer.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.blockunrenderer.BlockUnRenderer;
import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BlockUnRendererClient implements ClientModInitializer {

    /** Default key: X (toggle/hold the hiding). Rebindable in Controls. */
    public static KeyMapping TOGGLE_KEY;

    /** Default key: N — add/remove the block you're looking at from the hidden list. */
    public static KeyMapping TARGET_KEY;

    private static boolean toggledOn = false;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        HideState.rebuildFromConfig();

        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.toggle",
                        InputConstants.KEY_X, KeyMapping.Category.GAMEPLAY));

        TARGET_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.block_unrenderer.target",
                        InputConstants.KEY_N, KeyMapping.Category.GAMEPLAY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ModConfig cfg = ConfigManager.get();

            // ── Hide trigger (hold or toggle) ──────────────────────────────────
            if (cfg.triggerMode == ModConfig.TriggerMode.HOLD) {
                HideState.setActive(TOGGLE_KEY.isDown());
            } else {
                while (TOGGLE_KEY.consumeClick()) {
                    toggledOn = !toggledOn;
                    HideState.setActive(toggledOn);
                    overlay(mc, "Block UN-renderer: " + (toggledOn ? "§aHIDDEN" : "§cVISIBLE"));
                }
            }

            // ── Add/remove the block under the crosshair ───────────────────────
            while (TARGET_KEY.consumeClick()) {
                toggleLookedAtBlock(mc);
            }
        });

        BlockUnRenderer.LOGGER.info("Block UN-renderer client ready. Toggle: X, Target block: N");
    }

    /** Adds (or removes) the block the player is looking at to the hidden list. */
    private void toggleLookedAtBlock(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            overlay(mc, "§eLook at a block first");
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return;
        String idStr = id.toString();

        ModConfig cfg = ConfigManager.get();
        if (cfg.hiddenBlocks.remove(idStr)) {
            overlay(mc, "§cShown: §f" + idStr);
        } else {
            cfg.hiddenBlocks.add(idStr);
            overlay(mc, "§aHidden: §f" + idStr);
        }
        ConfigManager.save();
        HideState.rebuildFromConfig();
    }

    private static void overlay(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal(msg));
        }
    }
}
