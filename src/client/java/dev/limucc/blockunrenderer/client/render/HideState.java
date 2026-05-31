package dev.limucc.blockunrenderer.client.render;

import dev.limucc.blockunrenderer.client.config.ConfigManager;
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

    public static boolean shouldFixLight(Block block) {
        return active && ConfigManager.get().fixLighting && hidden.contains(block);
    }

    /** Global fullbright while hiding with Fix Lighting on (read by LightTextureMixin). */
    public static boolean isFullbright() {
        return active && ConfigManager.get().fixLighting && !hidden.isEmpty();
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
