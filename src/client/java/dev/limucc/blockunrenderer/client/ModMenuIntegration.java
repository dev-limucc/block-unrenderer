package dev.limucc.blockunrenderer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.render.HideState;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
                    HideState.rebuildFromConfig();
                });

        ConfigEntryBuilder e = builder.entryBuilder();

        // ── Info ────────────────────────────────────────────────────────────
        ConfigCategory info = builder.getOrCreateCategory(Component.literal("Info"));
        info.addEntry(e.startTextDescription(Component.literal("§lBlock UN-renderer")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("Hides listed blocks & block entities client-side.")).build());
        info.addEntry(e.startTextDescription(Component.literal(" ")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("§eKey X§r — toggle/hold hiding (mode in Settings).")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("Rebind in Options → Controls → Gameplay.")).build());
        info.addEntry(e.startTextDescription(Component.literal(" ")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("§7Works with & without Sodium. No cost when off.")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("§7Under Iris shaders, the shader controls lighting —")).build());
        info.addEntry(e.startTextDescription(
                Component.literal("§7use the shader's own brightness if areas look dark.")).build());

        // ── Settings ────────────────────────────────────────────────────────
        ConfigCategory settings = builder.getOrCreateCategory(Component.literal("Settings"));

        settings.addEntry(e
                .startEnumSelector(Component.literal("Trigger Mode"), ModConfig.TriggerMode.class, cfg.triggerMode)
                .setDefaultValue(ModConfig.TriggerMode.TOGGLE)
                .setTooltip(Component.literal(
                        "HOLD — hidden only while the key is held.\n" +
                        "TOGGLE — press the key to switch on/off."))
                .setSaveConsumer(v -> cfg.triggerMode = v)
                .build());

        settings.addEntry(e
                .startBooleanToggle(Component.literal("Show Blocks Underneath"), cfg.showBlocksUnderneath)
                .setDefaultValue(true)
                .setTooltip(Component.literal(
                        "ON — blocks under/behind a hidden block still render (no holes).\n" +
                        "OFF — full see-through into the void."))
                .setSaveConsumer(v -> cfg.showBlocksUnderneath = v)
                .build());

        settings.addEntry(e
                .startBooleanToggle(Component.literal("Fix Lighting"), cfg.fixLighting)
                .setDefaultValue(true)
                .setTooltip(Component.literal(
                        "ON — hidden blocks stop blocking light + a brightness boost so\n" +
                        "exposed areas are clearly visible (vanilla & Sodium).\n" +
                        "Under Iris shaders, use the shader's brightness instead."))
                .setSaveConsumer(v -> cfg.fixLighting = v)
                .build());

        // Searchable block picker — type to filter, pick a block, Save to add it.
        List<Block> selectable = BuiltInRegistries.BLOCK.stream()
                .filter(b -> b.asItem() != Items.AIR)
                .sorted(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()))
                .collect(Collectors.toList());

        settings.addEntry(e
                .startDropdownMenu(Component.literal("Add Block (search)"),
                        DropdownMenuBuilder.TopCellElementBuilder.ofBlockObject(Blocks.AIR),
                        DropdownMenuBuilder.CellCreatorBuilder.ofBlockObject())
                .setSelections(selectable)
                .setDefaultValue(Blocks.AIR)
                .setTooltip(Component.literal(
                        "Type to search, pick a block, then press Save —\n" +
                        "it gets added to the Hidden Block IDs list below.\n" +
                        "Shows block icons; reopen the screen to see the updated list."))
                .setSaveConsumer(block -> {
                    if (block != null && block != Blocks.AIR) {
                        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                        if (!cfg.hiddenBlocks.contains(id)) cfg.hiddenBlocks.add(id);
                    }
                })
                .build());

        settings.addEntry(e
                .startStrList(Component.literal("Hidden Block IDs"), cfg.hiddenBlocks)
                .setDefaultValue(java.util.List.of())
                .setTooltip(Component.literal(
                        "One block ID per line, e.g. minecraft:stone, minecraft:chest.\n" +
                        "Use the + button to add a row and type the ID.\n" +
                        "Works for normal blocks and block entities."))
                .setExpanded(true)
                .setInsertButtonEnabled(true)
                .setSaveConsumer(v -> cfg.hiddenBlocks = v)
                .build());

        return builder.build();
    }
}
