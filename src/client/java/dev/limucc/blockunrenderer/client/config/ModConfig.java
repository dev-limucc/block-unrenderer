package dev.limucc.blockunrenderer.client.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for Block UN-renderer.
 * One ID list covers regular blocks, block entities, and liquids (minecraft:water, minecraft:lava, …).
 */
public class ModConfig {

    /** HOLD = hidden only while key held; TOGGLE = press to flip on/off. */
    public TriggerMode triggerMode = TriggerMode.TOGGLE;

    /**
     * How the list is interpreted:
     *  HIDE_LISTED      — hide the listed entries, show everything else (blacklist, default).
     *  SHOW_ONLY_LISTED — show ONLY the listed entries, hide everything else (whitelist).
     */
    public FilterMode filterMode = FilterMode.HIDE_LISTED;

    /**
     * The blocks/liquids the filter acts on, e.g. "minecraft:stone", "minecraft:chest", "minecraft:water".
     * (JSON key kept as "hiddenBlocks" for backward compatibility with older configs.)
     */
    public List<String> hiddenBlocks = new ArrayList<>();

    /** When true, hide ALL entities (mobs, items, etc.) while hiding is active. */
    public boolean hideEntities = false;

    /** When true, hide ALL liquids (water, lava, modded) while hiding is active. */
    public boolean hideLiquids = false;

    /**
     * When true (default): blocks under/behind a hidden block still render, so you
     * see the surface underneath instead of a hole into the void.
     */
    public boolean showBlocksUnderneath = true;

    /**
     * Lighting while hiding:
     *  OFF        — no change.
     *  FULLBRIGHT — bright, neutral white light (clean visibility) — default.
     * Works in vanilla/Sodium; under Iris shaders the shader controls lighting.
     */
    public LightMode lightMode = LightMode.FULLBRIGHT;

    public enum TriggerMode { HOLD, TOGGLE }

    public enum LightMode { OFF, FULLBRIGHT }

    public enum FilterMode { HIDE_LISTED, SHOW_ONLY_LISTED }
}
