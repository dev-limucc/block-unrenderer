package dev.limucc.blockunrenderer.client.render;

import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

/**
 * Central runtime state for the block filter.
 * Every hot-path check tests `active` first → ~0 cost when the mod is off.
 */
public final class HideState {

    private HideState() {}

    private static volatile boolean active = false;
    private static volatile Set<Block> listed = new HashSet<>();
    private static volatile ModConfig.FilterMode mode = ModConfig.FilterMode.HIDE_LISTED;

    // ── Map-mod guard ─────────────────────────────────────────────────────────
    // Xaero's Minimap / World Map sample world block states directly (on their own
    // worker threads, and sometimes the render thread). While they sample, we must
    // report the *vanilla* world so the map isn't distorted. A re-entrant counter is
    // pushed/popped around Xaero's sampling by the @Pseudo soft-mixins; the thread-name
    // check is a zero-dependency fallback that also covers Xaero's background workers.

    private static final ThreadLocal<int[]> MAP_SAMPLING = ThreadLocal.withInitial(() -> new int[1]);

    public static void pushMapSampling() { MAP_SAMPLING.get()[0]++; }
    public static void popMapSampling()  { int[] c = MAP_SAMPLING.get(); if (c[0] > 0) c[0]--; }

    public static boolean isMapSampling() {
        if (MAP_SAMPLING.get()[0] > 0) return true;
        String n = Thread.currentThread().getName();
        // Extend here for other map mods (e.g. "JM-", "voxelmap") if needed.
        return n != null && (n.contains("Xaero") || n.contains("xaero"));
    }

    /** Is this block a target of the current filter? (mode-aware, no guard) */
    private static boolean targeted(Block block) {
        boolean inList = listed.contains(block);
        return (mode == ModConfig.FilterMode.SHOW_ONLY_LISTED) ? !inList : inList;
    }

    // ── Hot path (mixins) ─────────────────────────────────────────────────────

    public static boolean shouldHide(Block block) {
        return active && !isMapSampling() && targeted(block);
    }

    public static boolean shouldShowUnder(Block block) {
        return active && !isMapSampling()
                && ConfigManager.get().showBlocksUnderneath && targeted(block);
    }

    /** Let skylight pass through this hidden block (when lighting isn't OFF). */
    public static boolean shouldPassLight(Block block) {
        return active && !isMapSampling()
                && ConfigManager.get().lightMode != ModConfig.LightMode.OFF && targeted(block);
    }

    /** True if a lighting boost should be applied now (read by LightmapMixin / luminance). */
    public static boolean isLightActive() {
        return active && ConfigManager.get().lightMode != ModConfig.LightMode.OFF && !listed.isEmpty();
    }

    public static ModConfig.LightMode lightMode() {
        return ConfigManager.get().lightMode;
    }

    public static boolean isActive() { return active; }

    // ── State changes ─────────────────────────────────────────────────────────

    public static void setActive(boolean value) {
        if (value == active) return;
        active = value;
        triggerChunkReload();
    }

    public static void rebuildFromConfig() {
        mode = ConfigManager.get().filterMode;
        Set<Block> next = new HashSet<>();
        for (String id : ConfigManager.get().hiddenBlocks) {
            if (id == null || id.isBlank()) continue;
            Identifier rl = Identifier.tryParse(id.trim());
            if (rl == null) continue;
            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(next::add);
        }
        listed = next;
        if (active) triggerChunkReload();
    }

    private static void triggerChunkReload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null && mc.level != null) {
            mc.execute(() -> mc.levelRenderer.allChanged());
        }
    }
}
