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
 * Central runtime state for hidden blocks.
 * Every hot-path check tests `active` first → ~0 cost when the mod is off.
 */
public final class HideState {

    private HideState() {}

    private static volatile boolean active = false;
    private static volatile Set<Block> hidden = new HashSet<>();

    // ── Hot path (mixins) ─────────────────────────────────────────────────────

    public static boolean shouldHide(Block block) {
        return active && hidden.contains(block);
    }

    public static boolean shouldShowUnder(Block block) {
        return active && ConfigManager.get().showBlocksUnderneath && hidden.contains(block);
    }

    /** Let skylight pass through this hidden block (when lighting isn't OFF). */
    public static boolean shouldPassLight(Block block) {
        return active && ConfigManager.get().lightMode != ModConfig.LightMode.OFF && hidden.contains(block);
    }

    /** True if a lighting boost should be applied now (read by LightmapMixin). */
    public static boolean isLightActive() {
        return active && ConfigManager.get().lightMode != ModConfig.LightMode.OFF && !hidden.isEmpty();
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
        Set<Block> next = new HashSet<>();
        for (String id : ConfigManager.get().hiddenBlocks) {
            if (id == null || id.isBlank()) continue;
            Identifier rl = Identifier.tryParse(id.trim());
            if (rl == null) continue;
            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(next::add);
        }
        hidden = next;
        if (active) triggerChunkReload();
    }

    private static void triggerChunkReload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null && mc.level != null) {
            mc.execute(() -> mc.levelRenderer.allChanged());
        }
    }
}
