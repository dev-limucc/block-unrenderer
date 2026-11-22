package dev.limucc.blockunrenderer.client.compat;

import net.minecraft.client.Minecraft;

/** MC 26.1: chunk re-mesh is LevelRenderer.allChanged(). */
public final class ChunkReload {

    private ChunkReload() {}

    public static void allChanged(Minecraft mc) {
        if (mc.levelRenderer != null && mc.level != null) {
            mc.levelRenderer.allChanged();
        }
    }
}
