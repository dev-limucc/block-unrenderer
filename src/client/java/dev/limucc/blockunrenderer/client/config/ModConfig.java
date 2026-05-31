package dev.limucc.blockunrenderer.client.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for Block UN-renderer.
 *
 * One list of block IDs covers BOTH regular blocks (stone, etc.) and block
 * entities (chests, furnaces, etc.) — the mod hides whichever applies.
 */
public class ModConfig {

    /** HOLD = hidden only while key held; TOGGLE = press to flip on/off. */
    public TriggerMode triggerMode = TriggerMode.TOGGLE;

    /**
     * Block IDs to hide, e.g. "minecraft:stone", "minecraft:chest".
     * Works for normal blocks and block entities alike.
     */
    public List<String> hiddenBlocks = new ArrayList<>();

    public enum TriggerMode {
        HOLD,
        TOGGLE
    }
}
