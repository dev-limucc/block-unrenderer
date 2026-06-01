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
import net.minecraft.world.level.block.LiquidBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Block manager: search box, scrollable list (blocks, block entities AND liquids) with icons,
 * click-to-add/remove, toggle buttons, and an Info page. Built on vanilla widgets so all text
 * is visible.
 */
public class BlockManagerScreen extends Screen {

    private record Entry(Block block, ItemStack stack, String idStr, String path, String namespace) {}

    private static List<Entry> ALL;

    private static final String[] INFO = {
            "§lBlock UN-renderer",
            "Hide blocks, block entities, liquids & entities on your client.",
            "",
            "§eHow to use",
            "• Type in the search box, then click a row to §aADD§r it.",
            "• With an empty search, the list shows your listed entries —",
            "   click one to §cREMOVE§r it.",
            "• Liquids (water, lava) are in the list — search \"water\"/\"lava\".",
            "• Bind keys in §eOptions → Controls → Block UN-renderer§r",
            "   (unbound by default) to toggle hiding & open this screen.",
            "",
            "§eFilter mode",
            "• §fFilter§r — HIDE listed (blacklist) or SHOW only listed (whitelist).",
            "• §fTrigger§r — HOLD: hide only while key held; TOGGLE: press to flip.",
            "",
            "§eHide also §8(green = ON/hidden)",
            "• §fEntities§r — hide all mobs / items / projectiles.",
            "• §fLiquids§r — hide ALL water / lava at once.",
            "• §fUnderneath§r — keep blocks under hidden ones rendered (no holes).",
            "• §fLight§r — OFF or FULLBRIGHT to light exposed areas.",
            "",
            "§7Works with & without Sodium. No cost when hiding is off.",
            "§7Doesn't distort Xaero's minimap / world map.",
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
                Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                if (id == null) continue;
                ItemStack icon;
                if (b instanceof LiquidBlock) {
                    // Liquids have no block item — show a bucket so they're recognisable.
                    icon = switch (id.getPath()) {
                        case "water" -> new ItemStack(Items.WATER_BUCKET);
                        case "lava"  -> new ItemStack(Items.LAVA_BUCKET);
                        default       -> new ItemStack(Items.BUCKET);
                    };
                } else {
                    if (b.asItem() == Items.AIR) continue;
                    icon = new ItemStack(b);
                }
                list.add(new Entry(b, icon, id.toString(), id.getPath(), id.getNamespace()));
            }
            list.sort((a, c) -> a.path.compareTo(c.path));
            ALL = list;
        }
        return ALL;
    }

    // Y positions of the two section header labels, drawn in extractRenderState().
    private int filterHeaderY, hideHeaderY;

    private int hintY;   // y of the "Showing listed entries…" line (set in init, drawn in render)

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.panelLeft  = cx - 155;
        this.panelRight = cx + 155;

        int bw = 153, gap = 4;             // two columns span the 310-wide panel
        int col0 = panelLeft, col1 = panelLeft + bw + gap;

        // ── "Filter mode" group: two wide mode buttons ───────────────────────
        this.filterHeaderY = 24;
        int mr = 34;                       // 34 → ends 54
        this.addRenderableWidget(Button.builder(filterLabel(), b -> {
            ModConfig c = ConfigManager.get();
            c.filterMode = (c.filterMode == ModConfig.FilterMode.HIDE_LISTED)
                    ? ModConfig.FilterMode.SHOW_ONLY_LISTED : ModConfig.FilterMode.HIDE_LISTED;
            ConfigManager.save();
            HideState.rebuildFromConfig();
            b.setMessage(filterLabel());
        }).bounds(col0, mr, bw, 20).build());

        this.addRenderableWidget(Button.builder(modeLabel(), b -> {
            ModConfig c = ConfigManager.get();
            c.triggerMode = (c.triggerMode == ModConfig.TriggerMode.TOGGLE)
                    ? ModConfig.TriggerMode.HOLD : ModConfig.TriggerMode.TOGGLE;
            ConfigManager.save();
            b.setMessage(modeLabel());
        }).bounds(col1, mr, bw, 20).build());

        // ── "Hide also" group: 2×2 grid of ON/OFF toggles ───────────────────
        this.hideHeaderY = 60;
        int tr1 = 72;                      // 72 → ends 92
        int tr2 = 96;                      // 96 → ends 116

        this.addRenderableWidget(Button.builder(entitiesLabel(), b -> {
            ModConfig c = ConfigManager.get(); c.hideEntities = !c.hideEntities;
            ConfigManager.save(); b.setMessage(entitiesLabel());
        }).bounds(col0, tr1, bw, 20).build());

        this.addRenderableWidget(Button.builder(liquidsLabel(), b -> {
            ModConfig c = ConfigManager.get(); c.hideLiquids = !c.hideLiquids;
            ConfigManager.save(); HideState.rebuildFromConfig(); b.setMessage(liquidsLabel());
        }).bounds(col1, tr1, bw, 20).build());

        this.addRenderableWidget(Button.builder(underLabel(), b -> {
            ModConfig c = ConfigManager.get(); c.showBlocksUnderneath = !c.showBlocksUnderneath;
            ConfigManager.save(); HideState.rebuildFromConfig(); b.setMessage(underLabel());
        }).bounds(col0, tr2, bw, 20).build());

        this.addRenderableWidget(Button.builder(lightLabel(), b -> {
            ModConfig c = ConfigManager.get();
            c.lightMode = (c.lightMode == ModConfig.LightMode.OFF)
                    ? ModConfig.LightMode.FULLBRIGHT : ModConfig.LightMode.OFF;
            ConfigManager.save(); HideState.rebuildFromConfig(); b.setMessage(lightLabel());
        }).bounds(col1, tr2, bw, 20).build());

        // Hint + search + list, each clearly below the toggle grid (ends at 116).
        this.hintY     = 122;
        int searchY    = 134;              // 134 → ends 152
        this.listTop   = 158;
        this.listBottom = this.height - 36;

        // ── Search ───────────────────────────────────────────────────────────
        this.search = new EditBox(this.font, panelLeft, searchY, 310, 18, Component.literal("Search"));
        this.search.setHint(Component.literal("Search blocks & liquids by name…"));
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

    // Toggle label: name + green ON / gray OFF for instant readability.
    private static Component onOffLabel(String name, boolean on) {
        return Component.literal(name + ": " + (on ? "§aON" : "§7OFF"));
    }

    private Component filterLabel() {
        boolean hide = ConfigManager.get().filterMode == ModConfig.FilterMode.HIDE_LISTED;
        return Component.literal(hide ? "Filter: §cHIDE listed" : "Filter: §bSHOW only listed");
    }
    private Component modeLabel() {
        boolean isToggle = ConfigManager.get().triggerMode == ModConfig.TriggerMode.TOGGLE;
        return Component.literal(isToggle ? "Trigger: §eTOGGLE" : "Trigger: §eHOLD");
    }
    private Component entitiesLabel()  { return onOffLabel("Entities", ConfigManager.get().hideEntities); }
    private Component liquidsLabel()   { return onOffLabel("Liquids",  ConfigManager.get().hideLiquids); }
    private Component underLabel() {
        // Underneath ON = keep neighbours rendered (green = "good/safe" default).
        return Component.literal("Underneath: "
                + (ConfigManager.get().showBlocksUnderneath ? "§aON" : "§7OFF"));
    }
    private Component lightLabel() {
        return Component.literal(ConfigManager.get().lightMode == ModConfig.LightMode.OFF
                ? "Light: §7OFF" : "Light: §eFULLBRIGHT");
    }

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

        // Section headers above each button group.
        g.text(this.font, Component.literal("§7Filter mode"), panelLeft, filterHeaderY, 0xFFB0B0B0);
        g.text(this.font, Component.literal("§7Hide also  §8(green = hidden)"), panelLeft, hideHeaderY, 0xFFB0B0B0);

        String hint = search.getValue().trim().isEmpty()
                ? "Showing listed entries — type below to search & add more"
                : filtered.size() + " match(es) — click a row to add/remove";
        g.text(this.font, hint, panelLeft, hintY, 0xFFA0A0A0);

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
