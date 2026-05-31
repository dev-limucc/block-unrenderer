package dev.limucc.blockunrenderer.client.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for Block UN-renderer.
 * One block-ID list covers both regular blocks and block entities.
 */
public class ModConfig {

    /** HOLD = hidden only while key held; TOGGLE = press to flip on/off. */
    public TriggerMode triggerMode = TriggerMode.TOGGLE;

    /** Block IDs to hide, e.g. "minecraft:stone", "minecraft:chest". */
    public List<String> hiddenBlocks = new ArrayList<>();

    /**
     * When true (default): blocks behind/under a hidden block still render, so you
     * see the surface underneath instead of a hole into the void.
     * When false: full see-through (the old "floorless" behaviour).
     */
    public boolean showBlocksUnderneath = true;

    /**
     * When true (default): hidden blocks stop blocking light and a brightness boost
     * is applied while hiding, so exposed areas are clearly visible.
     * Note: under Iris shaders, the shader controls lighting — use its brightness.
     */
    public boolean fixLighting = true;

    public enum TriggerMode { HOLD, TOGGLE }
}
