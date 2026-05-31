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

    /**
     * How the block list is interpreted:
     *  HIDE_LISTED      — hide the listed blocks, show everything else (blacklist, default).
     *  SHOW_ONLY_LISTED — show ONLY the listed blocks, hide everything else (whitelist).
     */
    public FilterMode filterMode = FilterMode.HIDE_LISTED;

    /**
     * The blocks the filter acts on, e.g. "minecraft:stone", "minecraft:chest".
     * (JSON key kept as "hiddenBlocks" for backward compatibility with 1.0 configs.)
     */
    public List<String> hiddenBlocks = new ArrayList<>();

    /**
     * When true (default): blocks under/behind a hidden block still render, so you
     * see the surface underneath instead of a hole into the void.
     */
    public boolean showBlocksUnderneath = true;

    /**
     * Lighting while hiding:
     *  OFF         — no change.
     *  FULLBRIGHT  — bright, neutral white light (clean visibility).
     *  NIGHT_VISION— classic night-vision look (slightly tinted).
     * Both work in vanilla/Sodium; under Iris shaders the shader controls lighting.
     */
    public LightMode lightMode = LightMode.FULLBRIGHT;

    public enum TriggerMode { HOLD, TOGGLE }

    public enum LightMode { OFF, FULLBRIGHT, NIGHT_VISION }

    public enum FilterMode { HIDE_LISTED, SHOW_ONLY_LISTED }
}
