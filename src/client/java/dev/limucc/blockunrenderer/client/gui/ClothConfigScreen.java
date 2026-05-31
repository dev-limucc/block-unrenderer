package dev.limucc.blockunrenderer.client.gui;

import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.render.HideState;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/**
 * Cloth Config screen — the ModMenu entry point. Exposes the filter mode, trigger mode,
 * lighting, "show underneath", and the block-ID list. For picking blocks visually
 * (search + icons) bind the "Open Block Manager" key, which opens {@link BlockManagerScreen}.
 */
public final class ClothConfigScreen {

    private ClothConfigScreen() {}

    public static Screen create(Screen parent) {
        ModConfig cfg = ConfigManager.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Block UN-renderer"))
                .setSavingRunnable(() -> {
                    ConfigManager.save();
                    HideState.rebuildFromConfig();
                });

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(eb.startEnumSelector(
                        Component.literal("Filter mode"), ModConfig.FilterMode.class, cfg.filterMode)
                .setDefaultValue(ModConfig.FilterMode.HIDE_LISTED)
                .setEnumNameProvider(v -> Component.literal(switch ((ModConfig.FilterMode) v) {
                    case HIDE_LISTED -> "Hide listed blocks";
                    case SHOW_ONLY_LISTED -> "Show ONLY listed blocks";
                }))
                .setTooltip(Component.literal("Hide listed = blacklist. Show only listed = whitelist."))
                .setSaveConsumer(v -> cfg.filterMode = v)
                .build());

        general.addEntry(eb.startEnumSelector(
                        Component.literal("Trigger mode"), ModConfig.TriggerMode.class, cfg.triggerMode)
                .setDefaultValue(ModConfig.TriggerMode.TOGGLE)
                .setTooltip(Component.literal("HOLD: active only while the key is held. TOGGLE: press to flip."))
                .setSaveConsumer(v -> cfg.triggerMode = v)
                .build());

        general.addEntry(eb.startEnumSelector(
                        Component.literal("Lighting"), ModConfig.LightMode.class, cfg.lightMode)
                .setDefaultValue(ModConfig.LightMode.FULLBRIGHT)
                .setTooltip(Component.literal("Light exposed areas while hiding. (Iris shaders control their own lighting.)"))
                .setSaveConsumer(v -> cfg.lightMode = v)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        Component.literal("Show blocks underneath"), cfg.showBlocksUnderneath)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Keep blocks behind/under hidden ones rendered (no holes into the void)."))
                .setSaveConsumer(v -> cfg.showBlocksUnderneath = v)
                .build());

        general.addEntry(eb.startStrList(
                        Component.literal("Blocks"), new ArrayList<>(cfg.hiddenBlocks))
                .setTooltip(Component.literal("Block IDs, e.g. minecraft:stone, minecraft:chest. "
                        + "Tip: bind \"Open Block Manager\" for a searchable picker with icons."))
                .setSaveConsumer(v -> cfg.hiddenBlocks = new ArrayList<>(v))
                .build());

        return builder.build();
    }
}
