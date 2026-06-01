package dev.limucc.blockunrenderer.client.render;

import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.Set;

/**
 * Central runtime state for the filter.
 *
 * Performance contract (this is a hot path — every method is called per-block during chunk
 * meshing and lighting):
 *  • When the mod is OFF, every check returns after a single volatile read of {@code active}.
 *  • When ON, we avoid per-call allocation and per-call String work:
 *      - the map-mod thread check is cached per-thread (computed once), not recomputed per block;
 *      - the filter sets are plain HashSets read without copying.
 *  • The mixins use MixinExtras {@code @ModifyReturnValue}, so no CallbackInfo is allocated.
 */
public final class HideState {

    private HideState() {}

    private static volatile boolean active = false;
    private static volatile Set<Block> listed = new HashSet<>();
    private static volatile Set<Fluid> listedFluids = new HashSet<>();
    private static volatile boolean hasFluids = false;           // skip fluid mixin work when no fluids listed
    private static volatile boolean listEmpty = true;            // for SHOW_ONLY fast-path / light gating
    private static volatile ModConfig.FilterMode mode = ModConfig.FilterMode.HIDE_LISTED;

    // Mirror of config flags, refreshed in rebuildFromConfig() so the hot path never calls
    // ConfigManager.get() (which would be a map lookup per block).
    private static volatile boolean cfgShowUnder = true;
    private static volatile boolean cfgHideEntities = false;
    private static volatile boolean cfgHideLiquids = false;
    private static volatile boolean cfgLightFullbright = true;

    // ── Map-mod guard ─────────────────────────────────────────────────────────
    // Xaero's Minimap / World Map sample world block states directly (on their own worker
    // threads, and sometimes the render thread). While they sample, we report the *vanilla*
    // world so the map isn't distorted.
    //   • An explicit re-entrant counter is pushed/popped by the @Pseudo soft-mixin.
    //   • A thread-name check is the zero-dependency fallback. We cache it per-thread (the name
    //     never changes for a given thread) so we don't run String.contains on every block.

    private static final ThreadLocal<int[]> MAP_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<Boolean> MAP_THREAD = ThreadLocal.withInitial(() -> {
        String n = Thread.currentThread().getName();
        return n != null && (n.contains("Xaero") || n.contains("xaero"));
    });

    public static void pushMapSampling() { MAP_DEPTH.get()[0]++; }
    public static void popMapSampling()  { int[] c = MAP_DEPTH.get(); if (c[0] > 0) c[0]--; }

    public static boolean isMapSampling() {
        return MAP_THREAD.get() || MAP_DEPTH.get()[0] > 0;
    }

    // ── Hot path (mixins) ─────────────────────────────────────────────────────
    // Each begins with the `active` volatile read; when off the JIT collapses this to a
    // predictable-branch no-op with zero allocation.

    public static boolean shouldHide(Block block) {
        if (!active) return false;
        if (isMapSampling()) return false;
        return targeted(block);
    }

    /** Hide a liquid (water/lava/modded). Matched by Fluid (covers waterlogged + flowing). */
    public static boolean shouldHideFluid(Fluid fluid) {
        if (!active) return false;
        if (isMapSampling()) return false;
        // "Hide liquids" toggle hides every fluid regardless of the list.
        if (cfgHideLiquids) return true;
        if (!hasFluids && mode == ModConfig.FilterMode.HIDE_LISTED) return false;
        boolean inList = listedFluids.contains(fluid);
        return (mode == ModConfig.FilterMode.SHOW_ONLY_LISTED) ? !inList : inList;
    }

    /** Hide all entities while active (independent of the block list). */
    public static boolean shouldHideEntities() {
        return active && cfgHideEntities && !isMapSampling();
    }

    public static boolean shouldShowUnder(Block block) {
        if (!active || !cfgShowUnder) return false;
        if (isMapSampling()) return false;
        return targeted(block);
    }

    /** Let skylight pass through this hidden block (when FULLBRIGHT is on). */
    public static boolean shouldPassLight(Block block) {
        if (!active || !cfgLightFullbright) return false;
        if (isMapSampling()) return false;
        return targeted(block);
    }

    /** True if a lighting boost should be applied now (read by the luminance mixin). */
    public static boolean isLightActive() {
        if (!active || !cfgLightFullbright) return false;
        // In SHOW_ONLY mode "the list" being empty still hides the whole world, so light is relevant.
        return !listEmpty || mode == ModConfig.FilterMode.SHOW_ONLY_LISTED;
    }

    private static boolean targeted(Block block) {
        boolean inList = listed.contains(block);
        return (mode == ModConfig.FilterMode.SHOW_ONLY_LISTED) ? !inList : inList;
    }

    public static boolean isFullbright() { return cfgLightFullbright; }

    public static boolean isActive() { return active; }

    // ── State changes ─────────────────────────────────────────────────────────

    public static void setActive(boolean value) {
        if (value == active) return;
        active = value;
        triggerChunkReload();
    }

    public static void rebuildFromConfig() {
        ModConfig cfg = ConfigManager.get();
        mode = cfg.filterMode;
        cfgShowUnder = cfg.showBlocksUnderneath;
        cfgHideEntities = cfg.hideEntities;
        cfgHideLiquids = cfg.hideLiquids;
        cfgLightFullbright = cfg.lightMode == ModConfig.LightMode.FULLBRIGHT;

        Set<Block> nextBlocks = new HashSet<>();
        Set<Fluid> nextFluids = new HashSet<>();
        for (String id : cfg.hiddenBlocks) {
            if (id == null || id.isBlank()) continue;
            Identifier rl = Identifier.tryParse(id.trim());
            if (rl == null) continue;
            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(nextBlocks::add);
            // A liquid is listed by its block ID (minecraft:water/lava); map to its Fluid too.
            BuiltInRegistries.FLUID.getOptional(rl).ifPresent(nextFluids::add);
        }
        listed = nextBlocks;
        listedFluids = nextFluids;
        hasFluids = !nextFluids.isEmpty();
        listEmpty = nextBlocks.isEmpty() && nextFluids.isEmpty();
        if (active) triggerChunkReload();
    }

    private static void triggerChunkReload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null && mc.level != null) {
            mc.execute(() -> mc.levelRenderer.allChanged());
        }
    }
}
