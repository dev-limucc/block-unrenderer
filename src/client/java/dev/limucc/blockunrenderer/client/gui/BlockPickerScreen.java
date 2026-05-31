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
 * Custom block-picker screen for MC 26.1.2: a search bar, a scrollable list of
 * blocks with 3D item icons, and click-to-add / click-to-remove. Replaces the
 * Cloth string list (invisible text, no icons).
 *
 * Uses the 26.1 render-state-extraction API: we override extractRenderState
 * (not render) and draw with GuiGraphicsExtractor; mouse events use MouseButtonEvent.
 */
public class BlockPickerScreen extends Screen {

    private record Entry(Block block, ItemStack stack, String idStr, String path, String namespace) {}

    /** All blocks that have an item icon, cached once (registry is fixed at runtime). */
    private static List<Entry> ALL;

    private final Screen parent;
    private EditBox search;

    private final List<Entry> filtered = new ArrayList<>();
    private int scroll = 0;

    private int panelLeft, panelRight, listTop, listBottom;
    private static final int ROW_H = 20;

    public BlockPickerScreen(Screen parent) {
        super(Component.literal("Block UN-renderer — Pick Blocks"));
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
        this.panelLeft  = cx - 150;
        this.panelRight = cx + 150;
        this.listTop    = 64;
        this.listBottom = this.height - 40;

        this.search = new EditBox(this.font, panelLeft, 36, 300, 18, Component.literal("Search"));
        this.search.setHint(Component.literal("Search blocks…"));
        this.search.setMaxLength(100);
        this.search.setResponder(s -> refresh());
        this.addRenderableWidget(this.search);
        this.setInitialFocus(this.search);

        this.addRenderableWidget(Button.builder(modeLabel(), b -> {
            ModConfig cfg = ConfigManager.get();
            cfg.triggerMode = (cfg.triggerMode == ModConfig.TriggerMode.TOGGLE)
                    ? ModConfig.TriggerMode.HOLD : ModConfig.TriggerMode.TOGGLE;
            ConfigManager.save();
            b.setMessage(modeLabel());
        }).bounds(panelLeft, 12, 145, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(cx - 75, this.height - 28, 150, 20).build());

        refresh();
    }

    private Component modeLabel() {
        return Component.literal("Mode: " + ConfigManager.get().triggerMode.name());
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

    private boolean isHidden(Entry e) {
        return ConfigManager.get().hiddenBlocks.contains(e.idStr);
    }

    private void toggle(Entry e) {
        ModConfig cfg = ConfigManager.get();
        if (!cfg.hiddenBlocks.remove(e.idStr)) cfg.hiddenBlocks.add(e.idStr);
        ConfigManager.save();
        HideState.rebuildFromConfig();
        if (search.getValue().trim().isEmpty()) refresh();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a); // background + widgets

        // Title (centered manually)
        int tw = this.font.width(this.title);
        g.text(this.font, this.title, this.width / 2 - tw / 2, 4, 0xFFFFFFFF);

        // Hint line
        String hint = search.getValue().trim().isEmpty()
                ? "Showing hidden blocks — type to search all blocks"
                : filtered.size() + " result(s) — click a row to add/remove";
        g.text(this.font, hint, panelLeft, 58, 0xFFA0A0A0);

        // List background
        g.fill(panelLeft - 2, listTop - 2, panelRight + 2, listBottom + 2, 0x80000000);

        // Rows (clipped)
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
            else if (hovered) g.fill(panelLeft, y, panelRight, y + ROW_H, 0x20FFFFFF);

            g.item(e.stack, panelLeft + 2, y + 2);

            String name = e.namespace.equals("minecraft") ? e.path : e.namespace + ":" + e.path;
            g.text(this.font, name, panelLeft + 24, y + 6, 0xFFFFFFFF);

            String action = hidden ? "[- remove]" : "[+ add]";
            int color = hidden ? 0xFFFF6060 : 0xFF60FF60;
            int aw = this.font.width(action);
            g.text(this.font, action, panelRight - aw - 4, y + 6, color);
        }
        g.disableScissor();

        // Scrollbar
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
        double mx = event.x(), my = event.y();
        if (event.button() == 0 && mx >= panelLeft && mx <= panelRight && my >= listTop && my < listBottom) {
            int idx = (int) ((my - listTop + scroll) / ROW_H);
            if (idx >= 0 && idx < filtered.size()) {
                toggle(filtered.get(idx));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (mx >= panelLeft - 2 && mx <= panelRight + 5 && my >= listTop && my < listBottom) {
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
