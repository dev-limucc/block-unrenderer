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
}
