package dev.limucc.blockunrenderer.client.compat;

import net.minecraft.client.Minecraft;

/** MC 26.2+: chunk re-mesh lives on the new LevelExtractor. */
public final class ChunkReload {

    private ChunkReload() {}

    public static void allChanged(Minecraft mc) {
        if (mc.levelExtractor != null && mc.level != null) {
            mc.levelExtractor.allChanged();
        }
    }
}
