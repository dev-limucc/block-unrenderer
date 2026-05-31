package dev.limucc.blockunrenderer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.render.HideState;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::build;
    }

    private Screen build(Screen parent) {
        ModConfig cfg = ConfigManager.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Block UN-renderer"))
                .setSavingRunnable(() -> {
                    ConfigManager.save();
                    // Re-resolve the block set and re-mesh if needed
                    HideState.rebuildFromConfig();
                });

        ConfigEntryBuilder e = builder.entryBuilder();

        // ── Info tab ──────────────────────────────────────────────────────────
        ConfigCategory info = builder.getOrCreateCategory(Component.literal("Info"));
        info.addEntry(e.startTextDescription(Component.literal("§lBlock UN-renderer")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("Hides listed blocks & block entities client-side.")).build());
        info.addEntry(e.startTextDescription(Component.literal(" ")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("§eKey: X§r — toggle/hold (set the mode in Settings).")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("Rebind in Options → Controls → Gameplay.")).build());
        info.addEntry(e.startTextDescription(Component.literal(" ")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("§7Works with and without Sodium. Hiding is instant;")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("§7there is no per-frame cost when the mod is off.")).build());

        // ── Settings tab ──────────────────────────────────────────────────────
        ConfigCategory settings = builder.getOrCreateCategory(Component.literal("Settings"));

        settings.addEntry(e
                .startEnumSelector(Component.literal("Trigger Mode"), ModConfig.TriggerMode.class, cfg.triggerMode)
                .setDefaultValue(ModConfig.TriggerMode.TOGGLE)
                .setTooltip(Component.literal(
                        "HOLD — blocks hidden only while the key is held.\n" +
                        "TOGGLE — press the key to switch hidden on/off."))
                .setSaveConsumer(v -> cfg.triggerMode = v)
                .build());

        settings.addEntry(e
                .startStrList(Component.literal("Hidden Block IDs"), cfg.hiddenBlocks)
                .setDefaultValue(java.util.List.of())
                .setTooltip(Component.literal(
                        "One Minecraft block ID per line, e.g.:\n" +
                        "  minecraft:stone\n" +
                        "  minecraft:chest\n" +
                        "  minecraft:deepslate\n" +
                        "Works for normal blocks and block entities (chests, furnaces, etc.)."))
                .setExpanded(true)
                .setInsertButtonEnabled(true)
                .setSaveConsumer(v -> cfg.hiddenBlocks = v)
                .build());

        return builder.build();
    }
}
