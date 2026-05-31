package dev.limucc.blockunrenderer.client.gui;

import dev.limucc.blockunrenderer.client.config.ConfigManager;
import dev.limucc.blockunrenderer.client.config.ModConfig;
import dev.limucc.blockunrenderer.client.render.HideState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Block manager: search box, scrollable block list with icons, click-to-add/remove,
 * toggle buttons, and an Info page. Built on vanilla widgets so all text is visible.
 */
public class BlockManagerScreen extends Screen {

    private record Entry(Block block, ItemStack stack, String idStr, String path, String namespace) {}

    private static List<Entry> ALL;

    private static final String[] INFO = {
            "§lBlock UN-renderer",
            "Hide any block or block entity on your client.",
            "",
            "§eHow to use",
            "• Type in the search box, then click a block to §aADD§r it.",
            "• With an empty search, the list shows your listed blocks —",
            "   click one to §cREMOVE§r it.",
            "• Bind keys in §eOptions → Controls → Block UN-renderer§r",
            "   (unbound by default) to toggle hiding & open this screen.",
            "",
            "§eToggles (top buttons)",
            "• §fFilter§r — HIDE listed (blacklist) or SHOW only listed (whitelist).",
            "• §fMode§r — HOLD: hide only while key held; TOGGLE: press to flip.",
            "• §fUnderneath§r — keep blocks under hidden ones rendered (no holes).",
            "• §fLight§r — cycle OFF / FULLBRIGHT / NIGHT to light exposed areas.",
            "",
            "§7Works with & without Sodium. No cost when hiding is off.",
            "§7Under Iris shaders, lighting is the shader's — use its brightness.",
            "§7For singleplayer / servers you own. Not for unfair PvP."
    };

    private final Screen parent;
    private EditBox search;
    private boolean showInfo = false;

    private final List<Entry> filtered = new ArrayList<>();
    private int scroll = 0;

    private int panelLeft, panelRight, listTop, listBottom;
    private static final int ROW_H = 20;

    public BlockManagerScreen(Screen parent) {
        super(Component.literal("Block UN-renderer"));
        this.parent = parent;
    }

    private static List<Entry> all() {
        if (ALL == null) {
            List<Entry> list = new ArrayList<>();
            for (Block b : BuiltInRegistries.BLOCK) {
                if (b.asItem() == Items.AIR) continue;
                Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                if (id == null) continue;
                list.add(new Entry(b, new ItemStack(b), id.toString(), id.getPath(), id.getNamespace()));
            }
            list.sort((a, c) -> a.path.compareTo(c.path));
            ALL = list;
        }
        return ALL;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.panelLeft  = cx - 155;
        this.panelRight = cx + 155;
        this.listTop    = 98;
        this.listBottom = this.height - 36;

        int by = 28, bw = 74, gap = 4;
        this.addRenderableWidget(Button.builder(filterLabel(), b -> {
            ModConfig c = ConfigManager.get();
            c.filterMode = (c.filterMode == ModConfig.FilterMode.HIDE_LISTED)
                    ? ModConfig.FilterMode.SHOW_ONLY_LISTED : ModConfig.FilterMode.HIDE_LISTED;
            ConfigManager.save();
            HideState.rebuildFromConfig();
            b.setMessage(filterLabel());
        }).bounds(panelLeft, by, bw, 20).build());

        this.addRenderableWidget(Button.builder(modeLabel(), b -> {
            ModConfig c = ConfigManager.get();
            c.triggerMode = (c.triggerMode == ModConfig.TriggerMode.TOGGLE)
                    ? ModConfig.TriggerMode.HOLD : ModConfig.TriggerMode.TOGGLE;
            ConfigManager.save();
            b.setMessage(modeLabel());
        }).bounds(panelLeft + (bw + gap), by, bw, 20).build());

        this.addRenderableWidget(Button.builder(underLabel(), b -> {
            ModConfig c = ConfigManager.get();
            c.showBlocksUnderneath = !c.showBlocksUnderneath;
            ConfigManager.save();
            HideState.rebuildFromConfig();
            b.setMessage(underLabel());
        }).bounds(panelLeft + 2 * (bw + gap), by, bw, 20).build());

        this.addRenderableWidget(Button.builder(lightLabel(), b -> {
            ModConfig c = ConfigManager.get();
            c.lightMode = switch (c.lightMode) {
                case OFF -> ModConfig.LightMode.FULLBRIGHT;
                case FULLBRIGHT -> ModConfig.LightMode.NIGHT_VISION;
                case NIGHT_VISION -> ModConfig.LightMode.OFF;
            };
            ConfigManager.save();
            HideState.rebuildFromConfig();
            b.setMessage(lightLabel());
        }).bounds(panelLeft + 3 * (bw + gap), by, bw, 20).build());

        this.search = new EditBox(this.font, panelLeft, 58, 310, 18, Component.literal("Search"));
        this.search.setHint(Component.literal("Search blocks by name…"));
        this.search.setMaxLength(100);
        this.search.setResponder(s -> refresh());
        this.search.setVisible(!showInfo);
        this.addRenderableWidget(this.search);
        if (!showInfo) this.setInitialFocus(this.search);

        // Info / Back toggle (bottom-left)
        this.addRenderableWidget(Button.builder(Component.literal(showInfo ? "Back" : "Info"), b -> {
            showInfo = !showInfo;
            b.setMessage(Component.literal(showInfo ? "Back" : "Info"));
            this.search.setVisible(!showInfo);
        }).bounds(panelLeft, this.height - 28, 60, 20).build());

        // Done (center)
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(cx - 50, this.height - 28, 100, 20).build());

        refresh();
    }

    private Component filterLabel() {
        return Component.literal("Filter: "
                + (ConfigManager.get().filterMode == ModConfig.FilterMode.HIDE_LISTED ? "HIDE" : "SHOW"));
    }
    private Component modeLabel()  { return Component.literal("Mode: " + ConfigManager.get().triggerMode.name()); }
    private Component underLabel() { return Component.literal("Underneath: " + onOff(ConfigManager.get().showBlocksUnderneath)); }
    private Component lightLabel() {
        String s = switch (ConfigManager.get().lightMode) {
            case OFF -> "OFF";
            case FULLBRIGHT -> "FULL";
            case NIGHT_VISION -> "NIGHT";
        };
        return Component.literal("Light: " + s);
    }
    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private void refresh() {
        filtered.clear();
        String q = (search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT));
        if (q.isEmpty()) {
            for (Entry e : all()) if (isHidden(e)) filtered.add(e);
        } else {
            int cap = 400;
            for (Entry e : all()) {
                if (e.path.contains(q) || e.idStr.contains(q)) {
                    filtered.add(e);
                    if (filtered.size() >= cap) break;
                }
            }
        }
        scroll = 0;
    }

    private boolean isHidden(Entry e) { return ConfigManager.get().hiddenBlocks.contains(e.idStr); }

    private void toggle(Entry e) {
        ModConfig cfg = ConfigManager.get();
        if (!cfg.hiddenBlocks.remove(e.idStr)) cfg.hiddenBlocks.add(e.idStr);
        ConfigManager.save();
        HideState.rebuildFromConfig();
        if (search.getValue().trim().isEmpty()) refresh();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);

        int tw = this.font.width(this.title);
        g.text(this.font, this.title, this.width / 2 - tw / 2, 8, 0xFFFFFFFF);

        if (showInfo) {
            int y = 56;
            for (String line : INFO) {
                g.text(this.font, line, panelLeft, y, 0xFFFFFFFF);
                y += 11;
            }
            return;
        }

        String hint = search.getValue().trim().isEmpty()
                ? "Showing hidden blocks — type above to search & add more"
                : filtered.size() + " match(es) — click a row to add/remove";
        g.text(this.font, hint, panelLeft, 80, 0xFFA0A0A0);

        g.fill(panelLeft - 2, listTop - 2, panelRight + 2, listBottom + 2, 0x90000000);

        g.enableScissor(panelLeft - 2, listTop, panelRight + 2, listBottom);
        int first = Math.max(0, scroll / ROW_H);
        int visible = (listBottom - listTop) / ROW_H + 2;
        for (int i = first; i < Math.min(filtered.size(), first + visible); i++) {
            Entry e = filtered.get(i);
            int y = listTop + i * ROW_H - scroll;
            boolean hovered = mouseX >= panelLeft && mouseX <= panelRight
                    && mouseY >= y && mouseY < y + ROW_H && mouseY >= listTop && mouseY < listBottom;
            boolean hidden = isHidden(e);

            if (hidden)       g.fill(panelLeft, y, panelRight, y + ROW_H, 0x3055FF55);
            else if (hovered) g.fill(panelLeft, y, panelRight, y + ROW_H, 0x22FFFFFF);

            g.item(e.stack, panelLeft + 2, y + 2);
            String name = e.namespace.equals("minecraft") ? e.path : e.namespace + ":" + e.path;
            g.text(this.font, name, panelLeft + 24, y + 6, 0xFFFFFFFF);

            String action = hidden ? "[- remove]" : "[+ add]";
            int color = hidden ? 0xFFFF6060 : 0xFF60FF60;
            g.text(this.font, action, panelRight - this.font.width(action) - 4, y + 6, color);
        }
        g.disableScissor();

        int total = filtered.size() * ROW_H;
        int view = listBottom - listTop;
        if (total > view) {
            int barH = Math.max(15, view * view / total);
            int barY = listTop + scroll * (view - barH) / (total - view);
            g.fill(panelRight + 2, barY, panelRight + 5, barY + barH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (showInfo) return false;
        double mx = event.x(), my = event.y();
        if (event.button() == 0 && mx >= panelLeft && mx <= panelRight && my >= listTop && my < listBottom) {
            int idx = (int) ((my - listTop + scroll) / ROW_H);
            if (idx >= 0 && idx < filtered.size()) { toggle(filtered.get(idx)); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!showInfo && mx >= panelLeft - 2 && mx <= panelRight + 5 && my >= listTop && my < listBottom) {
            int total = filtered.size() * ROW_H;
            int view = listBottom - listTop;
            int max = Math.max(0, total - view);
            scroll = Math.max(0, Math.min(max, scroll - (int) (scrollY * ROW_H)));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        HideState.rebuildFromConfig();
        this.minecraft.setScreen(this.parent);
    }
}
