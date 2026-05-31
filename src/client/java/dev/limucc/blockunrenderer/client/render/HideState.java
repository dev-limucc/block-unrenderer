package dev.limucc.blockunrenderer.client.render;

import dev.limucc.blockunrenderer.BlockUnRenderer;
import dev.limucc.blockunrenderer.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

/**
 * Central runtime state for hidden blocks.
 *
 * Performance: every hot-path check tests the `active` boolean FIRST, so when the
 * mod is off the mixins return immediately — effectively zero overhead.
 */
public final class HideState {

    private HideState() {}

    private static volatile boolean active = false;
    private static volatile Set<Block> hidden = new HashSet<>();

    /** Saved gamma so we can restore it when hiding turns off. */
    private static Double savedGamma = null;

    // ── Hot path (mixins) ─────────────────────────────────────────────────────

    /** Is this block currently hidden? (active checked first → ~0 cost when off.) */
    public static boolean shouldHide(Block block) {
        return active && hidden.contains(block);
    }

    /** Should neighbours of this hidden block keep rendering (no holes)? */
    public static boolean shouldShowUnder(Block block) {
        return active && ConfigManager.get().showBlocksUnderneath && hidden.contains(block);
    }

    /** Should this hidden block stop blocking light? */
    public static boolean shouldFixLight(Block block) {
        return active && ConfigManager.get().fixLighting && hidden.contains(block);
    }

    public static boolean isActive() { return active; }

    // ── State changes (keybind / config) ──────────────────────────────────────

    public static void setActive(boolean value) {
        if (value == active) return;
        active = value;
        applyBrightness();
        triggerChunkReload();
    }

    public static void rebuildFromConfig() {
        Set<Block> next = new HashSet<>();
        for (String id : ConfigManager.get().hiddenBlocks) {
            if (id == null || id.isBlank()) continue;
            Identifier rl = Identifier.tryParse(id.trim());
            if (rl == null) continue;
            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(next::add);
        }
        hidden = next;
        if (active) {
            applyBrightness();
            triggerChunkReload();
        }
    }

    // ── Brightness boost (vanilla / Sodium; shaders use their own) ─────────────

    private static void applyBrightness() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        boolean wantBright = active && ConfigManager.get().fixLighting;
        try {
            if (wantBright) {
                if (savedGamma == null) savedGamma = mc.options.gamma().get();
                mc.options.gamma().set(1.0); // vanilla "Bright" — brightens dark exposed areas
            } else if (savedGamma != null) {
                mc.options.gamma().set(savedGamma);
                savedGamma = null;
            }
        } catch (Throwable t) {
            BlockUnRenderer.LOGGER.warn("Could not adjust brightness", t);
        }
    }

    private static void triggerChunkReload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null && mc.level != null) {
            mc.execute(() -> mc.levelRenderer.allChanged());
        }
    }
}
