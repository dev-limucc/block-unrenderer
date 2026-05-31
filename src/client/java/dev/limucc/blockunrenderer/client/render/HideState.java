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
 * Central, ultra-cheap runtime state for which blocks are currently hidden.
 *
 * Performance design:
 *  - The hidden set is a HashSet<Block> resolved ONCE from config (not per-frame
 *    string parsing). Lookups during meshing / block-entity rendering are O(1).
 *  - The hot-path checks ({@link #shouldHide}) test the `active` boolean FIRST,
 *    so when the mod is off there is essentially zero overhead.
 *  - Toggling visibility triggers a single chunk re-mesh (allChanged) — a one-time
 *    cost, not a per-frame one. Block entities update instantly with no rebuild.
 */
public final class HideState {

    private HideState() {}

    /** Whether hiding is currently in effect (driven by the keybind). */
    private static volatile boolean active = false;

    /** Resolved set of blocks to hide. Rebuilt from config on load/change. */
    private static volatile Set<Block> hidden = new HashSet<>();

    // ── Hot path (called from mixins) ─────────────────────────────────────────

    /** True if hiding is on AND this block is in the hidden set. */
    public static boolean shouldHide(Block block) {
        // boolean check first → near-zero cost when the mod is off
        return active && hidden.contains(block);
    }

    public static boolean isActive() {
        return active;
    }

    // ── State changes (called from the keybind handler) ───────────────────────

    /** Sets the active state; only triggers a chunk re-mesh when it actually changes. */
    public static void setActive(boolean value) {
        if (value == active) return;
        active = value;
        triggerChunkReload();
    }

    /** Re-resolves the hidden-block set from the current config (block IDs → Block). */
    public static void rebuildFromConfig() {
        Set<Block> next = new HashSet<>();
        for (String id : ConfigManager.get().hiddenBlocks) {
            if (id == null || id.isBlank()) continue;
            Identifier rl = Identifier.tryParse(id.trim());
            if (rl == null) {
                BlockUnRenderer.LOGGER.warn("Invalid block id in config: '{}'", id);
                continue;
            }
            BuiltInRegistries.BLOCK.getOptional(rl).ifPresentOrElse(
                    next::add,
                    () -> BlockUnRenderer.LOGGER.warn("Unknown block id in config: '{}'", id));
        }
        hidden = next;
        // If currently active, re-mesh so the new set takes effect immediately
        if (active) triggerChunkReload();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Forces a re-mesh of all loaded chunks so hidden/shown blocks update at once. */
    private static void triggerChunkReload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null && mc.level != null) {
            mc.execute(() -> mc.levelRenderer.allChanged());
        }
    }
}
