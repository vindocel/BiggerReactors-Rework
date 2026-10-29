package biggerreactorsrework.client.guide;

import biggerreactorsrework.calculator.CalculationResult;
import biggerreactorsrework.calculator.CalculatorCatalog;
import biggerreactorsrework.calculator.CalculatorRequest;
import biggerreactorsrework.calculator.Route;
import biggerreactorsrework.client.calculator.CalculatorClient;
import biggerreactorsrework.client.calculator.CalculatorLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static biggerreactorsrework.client.guide.CalculatorReport.t;

/**
 * The calculator on an open spread. Left page: the tabs New calculation, Recent and Saved, with the form or a list;
 * in the layout view, the parts legend and the ports. Right page: progress, the result with its sections, or the
 * 3D layout of a machine or of the whole plant. All state lives in {@link CalculatorSession}.
 */
final class CalculatorPages {
    private static final int TAB_H = 14, ROW_H = 24, FIELD_H = 14, BUTTON_H = 14;

    private CalculatorPages() { }

    /** Placeholder in the chapter: the book replaces it by the two pages. */
    static final class Spread implements GuideElement {
        Left left(int height) { return new Left(height); }
        Right right(int height) { return new Right(height); }
        public int height(GuideScreen s, int w) { return 0; }
        public void render(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my) { }
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ form helpers

    /** Whole number typed by the player, null with a reason key otherwise. Group separators of the game language are accepted when consistent. */
    record Parsed(long value, @Nullable String error) { }

    static Parsed parse(String text) {
        String compact = text.replace(" ", "").replace(" ", "").replace(" ", "").replace("_", "").replace("'", "");
        if (compact.isEmpty()) return new Parsed(0, "target.empty");
        char group = java.text.DecimalFormatSymbols.getInstance(CalculatorReport.locale()).getGroupingSeparator();
        String digits = compact;
        if (compact.indexOf(group) >= 0) {
            if (!compact.matches("\\d{1,3}(" + java.util.regex.Pattern.quote(String.valueOf(group)) + "\\d{3})+")) return new Parsed(0, "target.invalid");
            digits = compact.replace(String.valueOf(group), "");
        }
        if (!digits.matches("\\d+")) return new Parsed(0, "target.invalid");
        if (digits.length() > 10) return new Parsed(0, "target.range");
        long value = Long.parseLong(digits);
        if (value < 1 || value > CalculatorRequest.MAX_TARGET) return new Parsed(0, "target.range");
        return new Parsed(value, null);
    }

    private static EditBox field(GuideScreen s) {
        if (CalculatorSession.target == null) {
            var box = new EditBox(s.font(), 0, 0, 10, FIELD_H, t("target"));
            box.setMaxLength(20);
            box.setBordered(false);
            box.setTextColor(0xFFE0E0E0);
            box.setResponder(text -> CalculatorSession.formChanged());
            CalculatorSession.target = box;
        }
        return CalculatorSession.target;
    }

    /** Selector kinds: moderator, coolant, coil, fuel. */
    private static final int MODERATOR = 0, COOLANT = 1, COIL = 2, FUEL = 3;
    private static final String[] KIND_LABELS = {"moderator", "coolant", "coil", "fuel"};
    private static final int ROW_STEP = 20, SELECTOR_H = 18, DROP_ROW = 16;
    /** Paper tones of the book, so selectors and their lists read as part of the page. */
    private static final int PAPER_LIGHT = 0xFFF1ECDC, PAPER_EDGE = 0xFFA7A192, PAPER_LOCKED = 0xFFD0CAB6;

    private static int[] kinds() {
        return CalculatorSession.liquid ? new int[]{FUEL, COOLANT, COIL} : new int[]{FUEL, MODERATOR};
    }

    /** Top of the first selector row for a form starting at {@code top}. */
    private static int rowsTop(int top) {
        return top + 10 + FIELD_H + 12 + BUTTON_H + 3;
    }

    private static Component name(int kind, String id) {
        return switch (kind) {
            case COOLANT -> CalculatorReport.fluidName(id);
            case FUEL -> CalculatorReport.itemName(id);
            default -> CalculatorReport.blockName(id);
        };
    }

    private static List<String> options(CalculatorCatalog catalog, int kind) {
        List<String> ids = switch (kind) {
            case MODERATOR -> new ArrayList<>(catalog.moderators());
            case COOLANT -> catalog.coolants().stream().map(CalculatorCatalog.Coolant::id).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            case COIL -> new ArrayList<>(catalog.coils());
            default -> new ArrayList<>(catalog.fuels());
        };
        ids.sort(Comparator.comparing(id -> name(kind, id).getString()));
        return ids;
    }

    @Nullable
    private static String selected(int kind) {
        return switch (kind) {
            case MODERATOR -> CalculatorSession.moderator;
            case COOLANT -> CalculatorSession.coolant;
            case COIL -> CalculatorSession.coil;
            default -> CalculatorSession.fuel;
        };
    }

    private static void select(int kind, @Nullable String id) {
        switch (kind) {
            case MODERATOR -> CalculatorSession.moderator = id;
            case COOLANT -> CalculatorSession.coolant = id;
            case COIL -> CalculatorSession.coil = id;
            default -> CalculatorSession.fuel = id;
        }
        CalculatorSession.formChanged();
    }

    @Nullable
    private static CalculatorRequest request() {
        var catalog = CalculatorClient.catalog();
        if (catalog == null || CalculatorSession.target == null) return null;
        var parsed = parse(CalculatorSession.target.getValue());
        String fuel = CalculatorSession.fuel;
        if (parsed.error() != null || fuel == null || !catalog.fuels().contains(fuel)) return null;
        if (!CalculatorSession.liquid) {
            if (CalculatorSession.moderator == null || !catalog.moderators().contains(CalculatorSession.moderator)) return null;
            return new CalculatorRequest(parsed.value(), fuel, false, CalculatorSession.moderator, "", catalog.fingerprint());
        }
        if (CalculatorSession.coolant == null || CalculatorSession.coil == null || catalog.coolantRoute(CalculatorSession.coolant) == null
                || !catalog.coils().contains(CalculatorSession.coil)) return null;
        return new CalculatorRequest(parsed.value(), fuel, true, CalculatorSession.coolant, CalculatorSession.coil, catalog.fingerprint());
    }

    private static void calculate(GuideScreen s) {
        var request = request();
        if (request == null) return;
        CalculatorSession.target.setFocused(false);
        CalculatorSession.shown = null;
        CalculatorSession.layout = false;
        CalculatorClient.start(request);
        s.playClick();
    }

    // ------------------------------------------------------------------ left page

    static final class Left implements GuideElement {
        private final int height;
        private int formScroll;
        private final GuideElements.Note experimental = new GuideElements.Note(t("experimental.title"), t("experimental.text"), 0xFF826015, true);

        /** Modified 2026-10-29: the notice belongs to the input form, not the chapter's permanent layout. */
        private int noticeSpace(GuideScreen s, int w) {
            return CalculatorSession.tab == CalculatorSession.Tab.NEW && !CalculatorSession.layout
                    ? experimental.height(s, w) + GuideBook.GAP : 0;
        }

        Left(int height) { this.height = height; }

        public int height(GuideScreen s, int w) { return height; }

        public void render(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my) {
            CalculatorClient.requestCatalog();
            if (CalculatorSession.layout && CalculatorSession.shown != null) {
                legend(s, g, x, y, w, mx, my);
                return;
            }
            int bottom = y + height;
            int notice = noticeSpace(s, w);
            if (notice > 0) {
                experimental.render(s, g, x, y, w, mx, my);
                y += notice;
            }
            int tw = (w - 4) / 3;
            var tabs = CalculatorSession.Tab.values();
            for (int i = 0; i < 3; i++) {
                int tx = x + i * (tw + 2);
                boolean active = CalculatorSession.tab == tabs[i];
                boolean hover = !active && over(mx, my, tx, y, tw, TAB_H);
                biggerreactorsrework.client.GuiArt.buttonBackground(g, tx, y, tw, TAB_H, hover, active, false, biggerreactorsrework.client.GuiFrame.REACTOR);
                s.fittedText(g, t("tab." + tabs[i].name().toLowerCase(java.util.Locale.ROOT)), tx + 3, y + 3, tw - 6, 0xFFEEEEEE, true);
            }
            int top = y + TAB_H + 4;
            switch (CalculatorSession.tab) {
                case NEW -> form(s, g, x, top, w, bottom - top, mx, my);
                case RECENT -> list(s, g, x, top, w, bottom - top, mx, my, CalculatorLibrary.get().recents(), "recent.empty");
                case SAVED -> list(s, g, x, top, w, bottom - top, mx, my, CalculatorLibrary.get().saved(), "saved.empty");
            }
        }

        private int[] fieldBox(int x, int top, int w) {
            return new int[]{x, top + 10, w, FIELD_H};
        }

        private void form(GuideScreen s, GuiGraphics g, int x, int top, int w, int h, int mx, int my) {
            int buttonY = top + h - BUTTON_H;
            int viewport = Math.max(1, buttonY - top - 4);
            formScroll = Math.clamp(formScroll, 0, maxFormScroll(viewport));
            g.enableScissor(x, top, x + w, top + viewport);
            boolean inside = over(mx, my, x, top, w, viewport);
            formFields(s, g, x, top - formScroll, w, inside ? mx : -1, inside ? my : -1);
            g.disableScissor();
            if (maxFormScroll(viewport) > 0) {
                int thumb = Math.max(6, viewport * viewport / (viewport + maxFormScroll(viewport)));
                int offset = (viewport - thumb) * formScroll / maxFormScroll(viewport);
                g.fill(x + w + 1, top, x + w + 3, top + viewport, GuideElements.TINT);
                g.fill(x + w + 1, top + offset, x + w + 3, top + offset + thumb, GuideElements.RULE);
            }
            boolean locked = CalculatorClient.running();
            s.textButton(g, x, buttonY, w, BUTTON_H, locked ? t("cancel") : t("calculate"), mx, my, locked || request() != null);
            renderDropdown(s, g, x, top - TAB_H - 4, w, mx, my);
        }

        private int maxFormScroll(int viewport) {
            int content = rowsTop(0) + kinds().length * ROW_STEP;
            return Math.max(0, content - viewport);
        }

        private void formFields(GuideScreen s, GuiGraphics g, int x, int top, int w, int mx, int my) {
            var catalog = CalculatorClient.catalog();
            boolean locked = CalculatorClient.running();
            s.fittedText(g, t("target"), x, top, w, GuideElements.MUTED);
            var box = fieldBox(x, top, w);
            g.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], 0xFF1B1E22);
            biggerreactorsrework.client.GuiArt.ring(g, box[0], box[1], box[2], box[3], field(s).isFocused() ? 0xFF36CFEE : 0xFF4A5560);
            var field = field(s);
            field.setX(box[0] + 4);
            field.setY(box[1] + 3);
            field.setWidth(box[2] - 8);
            field.setEditable(!locked);
            field.render(g, mx, my, 0);
            var parsed = parse(field.getValue());
            Component reading = parsed.error() == null ? t("target.reading", CalculatorReport.whole(parsed.value()))
                    : parsed.error().equals("target.range") ? t("target.range", CalculatorReport.whole(CalculatorRequest.MAX_TARGET)) : t(parsed.error());
            s.fittedText(g, reading, x, box[1] + FIELD_H + 2, w, parsed.error() == null ? GuideElements.TIP : field.getValue().isEmpty() ? GuideElements.MUTED : CalculatorReport.ERROR);
            int modeY = box[1] + FIELD_H + 12;
            int half = (w - 2) / 2;
            for (int i = 0; i < 2; i++) {
                boolean active = CalculatorSession.liquid == (i == 1);
                int bx = x + i * (half + 2);
                biggerreactorsrework.client.GuiArt.buttonBackground(g, bx, modeY, half, BUTTON_H, !active && !locked && over(mx, my, bx, modeY, half, BUTTON_H), active, locked,
                        biggerreactorsrework.client.GuiFrame.REACTOR);
                s.fittedText(g, t(i == 0 ? "mode.solid" : "mode.liquid"), bx + 3, modeY + 3, half - 6, 0xFFEEEEEE, true);
            }
            int rowY = rowsTop(top);
            if (catalog == null) {
                s.fittedText(g, t("loading"), x, rowY, w, GuideElements.MUTED);
            } else {
                var kinds = kinds();
                for (int i = 0; i < kinds.length; i++) selector(s, g, x, rowY + i * ROW_STEP, w, mx, my, kinds[i], catalog, locked);
            }
        }

        /** Open list of a selector: {x, y, width, visible rows, kind}; null when closed or not applicable. */
        @Nullable
        private int[] dropdown(GuideScreen s, int x, int y, int w) {
            int kind = CalculatorSession.dropdown;
            var catalog = CalculatorClient.catalog();
            if (kind < 0 || catalog == null || CalculatorClient.running() || CalculatorSession.tab != CalculatorSession.Tab.NEW || CalculatorSession.layout) return null;
            int[] kinds = kinds();
            int index = -1;
            for (int i = 0; i < kinds.length; i++) if (kinds[i] == kind) index = i;
            int count = options(catalog, kind).size();
            if (index < 0 || count == 0) return null;
            int top = y + TAB_H + 4, rowTop = rowsTop(top - formScroll) + index * ROW_STEP;
            int bottom = y + height - noticeSpace(s, w) - BUTTON_H - 4;
            if (rowTop < top || rowTop + SELECTOR_H > bottom) return null;
            // The list spans the whole page so long names stay readable.
            int lx = x, lw = w;
            int below = (bottom - rowTop - SELECTOR_H) / DROP_ROW, above = (rowTop - top) / DROP_ROW;
            if (below >= Math.min(count, 4) || below >= above) {
                int rows = Math.min(count, below);
                return new int[]{lx, rowTop + SELECTOR_H, lw, rows, kind};
            }
            int rows = Math.min(count, above);
            return new int[]{lx, rowTop - rows * DROP_ROW, lw, rows, kind};
        }

        private void renderDropdown(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my) {
            var list = dropdown(s, x, y, w);
            if (list == null) return;
            var options = options(CalculatorClient.catalog(), list[4]);
            CalculatorSession.dropScroll = Math.max(0, Math.min(CalculatorSession.dropScroll, options.size() - list[3]));
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            int lx = list[0], ly = list[1], lw = list[2], lh = list[3] * DROP_ROW;
            g.fill(lx - 1, ly - 1, lx + lw + 1, ly + lh + 1, PAPER_EDGE);
            g.fill(lx, ly, lx + lw, ly + lh, PAPER_LIGHT);
            String current = selected(list[4]);
            for (int i = 0; i < list[3]; i++) {
                int index = i + CalculatorSession.dropScroll;
                String id = options.get(index);
                int ry = ly + i * DROP_ROW;
                boolean hover = over(mx, my, lx, ry, lw, DROP_ROW);
                if (hover) g.fill(lx, ry, lx + lw, ry + DROP_ROW, GuideElements.TINT);
                if (i > 0) g.fill(lx + 2, ry, lx + lw - 2, ry + 1, GuideElements.RULE & 0x40FFFFFF);
                icon(s, g, list[4], id, lx + 1, ry);
                s.fittedText(g, name(list[4], id), lx + 19, ry + 4, lw - 23, id.equals(current) ? GuideElements.HEADING : GuideElements.BODY);
            }
            if (options.size() > list[3]) {
                int barH = Math.max(6, lh * list[3] / options.size());
                int barY = ly + (lh - barH) * CalculatorSession.dropScroll / Math.max(1, options.size() - list[3]);
                g.fill(lx + lw - 2, barY, lx + lw, barY + barH, GuideElements.RULE);
            }
            g.pose().popPose();
        }

        /** Item or fluid of an option, 16 px, at (x, y + 0). */
        private static void icon(GuideScreen s, GuiGraphics g, int kind, String id, int x, int y) {
            var key = ResourceLocation.tryParse(id);
            if (key == null) return;
            if (kind == COOLANT) {
                if (BuiltInRegistries.FLUID.containsKey(key)) s.fluidIcon(g, BuiltInRegistries.FLUID.get(key), x, y);
            } else if (kind == FUEL && BuiltInRegistries.ITEM.containsKey(key)) {
                g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(key)), x, y);
            } else if (BuiltInRegistries.BLOCK.containsKey(key)) {
                g.renderItem(new ItemStack(BuiltInRegistries.BLOCK.get(key)), x, y);
            }
        }

        static void status(GuideScreen s, GuiGraphics g, int x, int y, int w) {
            if (!CalculatorClient.running()) return;
            String progress = CalculatorClient.total() > 0 ? CalculatorClient.done() + "/" + CalculatorClient.total() : "";
            Component text = switch (CalculatorClient.phase()) {
                case SEARCHING -> t("phase.searching", progress);
                case VALIDATING -> t("phase.validating", progress);
                default -> t("phase.preparing");
            };
            s.fittedText(g, text, x, y, w, GuideElements.TIP);
        }

        /** Label on the left, the selector button on the rest of the row. */
        private static int labelWidth(int w) {
            return w * 2 / 5;
        }

        private void selector(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my, int kind, CalculatorCatalog catalog, boolean locked) {
            int lw = labelWidth(w), bx = x + lw, bw = w - lw;
            s.fittedText(g, t(KIND_LABELS[kind]), x, y + 5, lw - 3, GuideElements.MUTED);
            boolean hover = !locked && over(mx, my, bx, y, bw, SELECTOR_H);
            boolean open = CalculatorSession.dropdown == kind;
            g.fill(bx, y, bx + bw, y + SELECTOR_H, open || hover ? GuideElements.HEADING : PAPER_EDGE);
            g.fill(bx + 1, y + 1, bx + bw - 1, y + SELECTOR_H - 1, locked ? PAPER_LOCKED : PAPER_LIGHT);
            String id = selected(kind);
            if (options(catalog, kind).isEmpty()) {
                s.fittedText(g, t("none_available"), bx + 4, y + 5, bw - 8, GuideElements.MUTED);
                return;
            }
            if (id == null) {
                s.fittedText(g, t("choose"), bx + 4, y + 5, bw - 14, GuideElements.MUTED, true);
                g.drawString(s.font(), "\u25BE", bx + bw - 9, y + 5, GuideElements.MUTED, false);
                return;
            }
            icon(s, g, kind, id, bx + 1, y + 1);
            var name = name(kind, id);
            s.fittedText(g, name, bx + 19, y + 5, bw - 30, GuideElements.BODY);
            g.drawString(s.font(), "\u25BE", bx + bw - 9, y + 5, GuideElements.MUTED, false);
            if (hover && CalculatorSession.dropdown < 0) s.textTooltip(List.of(name));
        }

        private void list(GuideScreen s, GuiGraphics g, int x, int top, int w, int h, int mx, int my, List<CalculationResult> entries, String empty) {
            if (CalculatorLibrary.get().readOnly()) {
                s.fittedText(g, t("library.read_only"), x, top + h - 9, w, CalculatorReport.ERROR);
                h -= 11;
            }
            if (entries.isEmpty()) {
                s.fittedText(g, t(empty), x, top, w, GuideElements.MUTED);
                return;
            }
            int visible = Math.max(1, h / ROW_H);
            CalculatorSession.listScroll = Math.max(0, Math.min(CalculatorSession.listScroll, entries.size() - visible));
            g.enableScissor(x - 2, top, x + w + 2, top + h);
            for (int i = 0; i < Math.min(visible, entries.size() - CalculatorSession.listScroll); i++) {
                var r = entries.get(i + CalculatorSession.listScroll);
                int ry = top + i * ROW_H;
                boolean selected = CalculatorSession.shown != null && CalculatorSession.shown.id().equals(r.id());
                if (selected || over(mx, my, x, ry, w, ROW_H - 2)) g.fill(x - 2, ry, x + w + 2, ry + ROW_H - 2, selected ? 0x403D641F : GuideElements.TINT);
                s.fittedText(g, t("entry", CalculatorReport.whole(r.request().target()), CalculatorReport.material(r)), x, ry + 2, w, GuideElements.HEADING);
                s.fittedText(g, t("entry.predicted", CalculatorReport.whole(r.value(CalculationResult.POWER)), CalculatorReport.date(r.createdAt())), x, ry + 12, w,
                        r.status() == biggerreactorsrework.calculator.CalculationStatus.TARGET_MET ? GuideElements.MUTED : CalculatorReport.ERROR);
            }
            g.disableScissor();
        }

        /** Layout view: back button, the warning, the parts of the selected machine with hover highlight, and the ports. */
        private void legend(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my) {
            s.textButton(g, x, y, w, BUTTON_H, t("back"), mx, my);
            int top = y + BUTTON_H + 4, bottom = y + height;
            var rows = legendRows(s, w);
            int total = rows.stream().mapToInt(row -> row.height).sum();
            CalculatorSession.legendScroll = Math.max(0, Math.min(CalculatorSession.legendScroll, total - (bottom - top)));
            g.enableScissor(x - 2, top, x + w + 2, bottom);
            int ry = top - CalculatorSession.legendScroll;
            for (var row : rows) {
                if (ry + row.height > top && ry < bottom) row.draw(s, g, x, ry, w, over(mx, my, x, Math.max(top, ry), w, row.height) && my < bottom ? mx : -1, my);
                ry += row.height;
            }
            g.disableScissor();
        }

        @Override
        public void prepare(GuideScreen s, int x, int y, int w, int mx, int my) {
            if (!CalculatorSession.layout || CalculatorSession.shown == null) return;
            int top = y + BUTTON_H + 4, bottom = y + height;
            if (mx < x || mx >= x + w || my < top || my >= bottom) return;
            int ry = top - CalculatorSession.legendScroll;
            for (var row : legendRows(s, w)) {
                if (my >= ry && my < ry + row.height && row.group != null) GuideMultiblock.highlight(previewId(CalculatorSession.shown, CalculatorSession.machine), row.group);
                ry += row.height;
            }
        }

        public boolean click(GuideScreen s, double mx, double my, int x, int y, int w, int button) {
            if (CalculatorSession.layout && CalculatorSession.shown != null) {
                if (over(mx, my, x, y, w, BUTTON_H)) {
                    CalculatorSession.layout = false;
                    s.playClick();
                    return true;
                }
                return false;
            }
            int notice = noticeSpace(s, w);
            if (my < y + notice) return false;
            y += notice;
            int tw = (w - 4) / 3;
            if (over(mx, my, x, y, w, TAB_H)) {
                int i = Math.min(2, (int) ((mx - x) / (tw + 2)));
                CalculatorSession.tab = CalculatorSession.Tab.values()[i];
                CalculatorSession.listScroll = 0;
                s.playClick();
                return true;
            }
            int top = y + TAB_H + 4;
            if (CalculatorSession.tab != CalculatorSession.Tab.NEW) {
                var entries = CalculatorSession.tab == CalculatorSession.Tab.RECENT ? CalculatorLibrary.get().recents() : CalculatorLibrary.get().saved();
                int index = (int) ((my - top) / ROW_H) + CalculatorSession.listScroll;
                if (my >= top && index >= 0 && index < entries.size()) {
                    CalculatorSession.show(entries.get(index), false);
                    s.playClick();
                    return true;
                }
                return false;
            }
            boolean locked = CalculatorClient.running();
            var list = dropdown(s, x, y, w);
            if (list != null) {
                CalculatorSession.dropdown = -1;
                if (over(mx, my, list[0], list[1], list[2], list[3] * DROP_ROW)) {
                    var options = options(CalculatorClient.catalog(), list[4]);
                    int index = (int) ((my - list[1]) / DROP_ROW) + CalculatorSession.dropScroll;
                    if (index >= 0 && index < options.size()) select(list[4], options.get(index));
                    s.playClick();
                    return true;
                }
            }
            int buttonY = y + height - notice - BUTTON_H;
            if (over(mx, my, x, buttonY, w, BUTTON_H)) {
                if (locked) { CalculatorClient.abandon(); s.playClick(); }
                else calculate(s);
                return true;
            }
            if (my < top || my >= buttonY - 4) return false;
            top -= formScroll;
            var box = fieldBox(x, top, w);
            if (!locked && over(mx, my, box[0], box[1], box[2], box[3])) {
                var field = field(s);
                field.setFocused(true);
                field.mouseClicked(mx, my, button);
                return true;
            }
            int modeY = box[1] + FIELD_H + 12, half = (w - 2) / 2;
            if (!locked && over(mx, my, x, modeY, w, BUTTON_H)) {
                boolean liquid = mx >= x + half + 2;
                if (liquid != CalculatorSession.liquid) {
                    CalculatorSession.liquid = liquid;
                    CalculatorSession.formChanged();
                    s.playClick();
                }
                return true;
            }
            int rowY = rowsTop(top);
            int[] kinds = kinds();
            for (int i = 0; i < kinds.length; i++) {
                int by = rowY + i * ROW_STEP;
                if (!locked && over(mx, my, x + labelWidth(w), by, w - labelWidth(w), SELECTOR_H)) {
                    if (list == null || list[4] != kinds[i]) {
                        CalculatorSession.dropdown = kinds[i];
                        var options = options(CalculatorClient.catalog(), kinds[i]);
                        CalculatorSession.dropScroll = Math.max(0, options.indexOf(selected(kinds[i])) - 2);
                    }
                    s.playClick();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean scroll(GuideScreen s, double mx, double my, int x, int y, int w, int h, double amount) {
            if (CalculatorSession.layout && CalculatorSession.shown != null) {
                CalculatorSession.legendScroll = Math.max(0, CalculatorSession.legendScroll - (int) Math.signum(amount) * 10);
                return true;
            }
            if (CalculatorSession.tab != CalculatorSession.Tab.NEW) {
                CalculatorSession.listScroll = Math.max(0, CalculatorSession.listScroll - (int) Math.signum(amount));
                return true;
            }
            int notice = noticeSpace(s, w);
            if (my < y + notice) return true;
            y += notice;
            var list = dropdown(s, x, y, w);
            if (list != null && over(mx, my, list[0], list[1], list[2], list[3] * DROP_ROW)) {
                int max = Math.max(0, options(CalculatorClient.catalog(), list[4]).size() - list[3]);
                CalculatorSession.dropScroll = Math.max(0, Math.min(max, CalculatorSession.dropScroll - (int) Math.signum(amount)));
                return true;
            }
            int top = y + TAB_H + 4, viewport = Math.max(1, height - notice - TAB_H - 4 - BUTTON_H - 4);
            if (maxFormScroll(viewport) > 0 && my >= top && my < top + viewport) {
                CalculatorSession.dropdown = -1;
                formScroll = Math.clamp(formScroll - (int) Math.signum(amount) * ROW_STEP, 0, maxFormScroll(viewport));
                unfocus();
                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(GuideScreen s, int keyCode, int scanCode, int modifiers) {
            if (CalculatorSession.dropdown >= 0 && keyCode == GLFW.GLFW_KEY_ESCAPE) {
                CalculatorSession.dropdown = -1;
                return true;
            }
            var field = CalculatorSession.target;
            if (field == null || !field.isFocused() || CalculatorSession.tab != CalculatorSession.Tab.NEW || CalculatorSession.layout) return false;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_TAB) {
                field.setFocused(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                calculate(s);
                return true;
            }
            field.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        @Override
        public boolean charTyped(GuideScreen s, char codePoint, int modifiers) {
            var field = CalculatorSession.target;
            if (field == null || !field.isFocused()) return false;
            field.charTyped(codePoint, modifiers);
            return true;
        }

        @Override
        public void unfocus() {
            if (CalculatorSession.target != null) CalculatorSession.target.setFocused(false);
        }
    }

    // ------------------------------------------------------------------ legend rows

    private abstract static class Row {
        final int height;
        @Nullable final String group;

        Row(int height, @Nullable String group) {
            this.height = height;
            this.group = group;
        }

        abstract void draw(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my);
    }

    private static List<Row> textRows(GuideScreen s, int w, Component text, int color) {
        var rows = new ArrayList<Row>();
        for (FormattedCharSequence line : s.font().split(text, w)) rows.add(new Row(GuideElements.LINE, null) {
            void draw(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my) { g.drawString(s.font(), line, x, y, color, false); }
        });
        return rows;
    }

    private static List<Row> legendRows(GuideScreen s, int w) {
        var r = CalculatorSession.shown;
        var rows = new ArrayList<Row>();
        var machines = r.plant().machines();
        int index = CalculatorSession.machine;
        var counts = new java.util.LinkedHashMap<String, long[]>();
        var items = new java.util.HashMap<String, ItemStack>();
        for (int m = 0; m < machines.size(); m++) {
            if (index >= 0 && m != index) continue;
            var machine = machines.get(m).machine();
            machine.blocks().forEach((pos, state) -> {
                String group = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                var lower = group.endsWith("_casing") || group.endsWith("casing_block");
                if (lower) {
                    int bounds = (pos.getX() == 0 || pos.getX() == machine.width() - 1 ? 1 : 0) + (pos.getY() == 0 || pos.getY() == machine.height() - 1 ? 1 : 0)
                            + (pos.getZ() == 0 || pos.getZ() == machine.depth() - 1 ? 1 : 0);
                    group = bounds >= 2 ? "frame" : "walls";
                }
                items.putIfAbsent(group, new ItemStack(state.getBlock()));
                counts.computeIfAbsent(group, k -> new long[1])[0]++;
            });
        }
        for (var line : CalculatorReport.lines(r, CalculatorSession.View.CONNECTIONS).subList(0, 2))
            rows.addAll(textRows(s, w, line.text(), line.color()));
        rows.addAll(textRows(s, w, t("layout.parts"), GuideElements.HEADING));
        counts.forEach((group, count) -> rows.add(new Row(ROW_H - 4, group) {
            void draw(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my) {
                var stack = items.get(group);
                s.itemSlot(g, stack, x, y, mx, my);
                Component name = group.equals("frame") ? Component.translatable("guide.biggerreactorsrework.viewer.frame", count[0])
                        : group.equals("walls") ? Component.translatable("guide.biggerreactorsrework.viewer.walls", count[0])
                        : Component.literal(count[0] + "× ").append(stack.getHoverName());
                s.fixedText(g, name, x + 22, y + 5, w - 22, GuideElements.BODY);
            }
        }));
        rows.addAll(textRows(s, w, t("layout.ports"), GuideElements.HEADING));
        for (int m = 0; m < machines.size(); m++) {
            if (index >= 0 && m != index) continue;
            var placed = machines.get(m);
            for (var port : placed.machine().ports()) rows.addAll(textRows(s, w, CalculatorReport.portLine(port, placed.offset()), GuideElements.MUTED));
        }
        return rows;
    }

    static String previewId(CalculationResult r, int machine) {
        return "calculator:" + r.id() + ":" + machine;
    }

    static GuideMultiblock.Spread preview(CalculationResult r, int machine) {
        return CalculatorSession.PREVIEWS.computeIfAbsent(previewId(r, machine), id -> {
            var machines = r.plant().machines();
            var selected = machine < 0 ? machines : List.of(machines.get(machine));
            return GuideMultiblock.spread(id, selected, Component.empty());
        });
    }

    // ------------------------------------------------------------------ right page

    static final class Right implements GuideElement {
        private final int height;
        private boolean draggingViewer;

        Right(int height) { this.height = height; }

        public int height(GuideScreen s, int w) { return height; }

        private static final CalculatorSession.View[] VIEWS = CalculatorSession.View.values();
        private static final String[] VIEW_ICONS = {"guide_book", "reactor_terminal", "heat_exchanger_terminal", "turbine_terminal", "reactor_coolant_port"};

        /** Picks up a result that just arrived for the form's calculation. */
        private static void collect() {
            var received = CalculatorClient.result();
            if (received != null && received != CalculatorSession.lastReceived) {
                CalculatorSession.lastReceived = received;
                if (received.status().hasDesign()) CalculatorSession.show(received, true);
            }
        }

        public void render(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my) {
            collect();
            var r = CalculatorSession.shown;
            if (CalculatorClient.running()) {
                s.fittedText(g, t("running"), x, y, w, GuideElements.HEADING);
                Left.status(s, g, x, y + 14, w);
                var lines = s.font().split(t("running.text"), w);
                for (int i = 0; i < lines.size(); i++) g.drawString(s.font(), lines.get(i), x, y + 28 + i * GuideElements.LINE, GuideElements.BODY, false);
                return;
            }
            if (r == null) {
                var failure = CalculatorClient.failure();
                Component text = failure != null ? CalculatorReport.status(failure) : t("idle");
                var lines = s.font().split(text, w);
                for (int i = 0; i < lines.size(); i++)
                    g.drawString(s.font(), lines.get(i), x, y + i * GuideElements.LINE, failure != null ? CalculatorReport.ERROR : GuideElements.MUTED, false);
                return;
            }
            if (CalculatorSession.layout) {
                layout(s, g, x, y, w, mx, my, r);
                return;
            }
            for (int i = 0; i < VIEWS.length; i++) {
                int bx = x + i * 20;
                boolean available = available(r, VIEWS[i]);
                boolean active = CalculatorSession.view == VIEWS[i];
                biggerreactorsrework.client.GuiArt.buttonBackground(g, bx, y, 18, 18, available && over(mx, my, bx, y, 18, 18), active, !available,
                        biggerreactorsrework.client.GuiFrame.REACTOR);
                g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(GuideIds.id(VIEW_ICONS[i]))), bx + 1, y + 1);
                if (over(mx, my, bx, y, 18, 18)) s.textTooltip(List.of(t("view." + VIEWS[i].name().toLowerCase(java.util.Locale.ROOT))));
            }
            int top = y + 22, bottom = y + height - BUTTON_H - 4;
            var lines = new ArrayList<FormattedCharSequence>();
            var colors = new ArrayList<Integer>();
            for (var line : CalculatorReport.lines(r, CalculatorSession.view)) {
                var split = s.font().split(line.text(), w - 4);
                for (var part : split) {
                    lines.add(part);
                    colors.add(line.color());
                }
                if (line.heading()) {
                    lines.add(FormattedCharSequence.EMPTY);
                    colors.add(0);
                }
            }
            int visible = (bottom - top) / GuideElements.LINE;
            CalculatorSession.scroll = Math.max(0, Math.min(CalculatorSession.scroll, lines.size() - visible));
            for (int i = 0; i < Math.min(visible, lines.size() - CalculatorSession.scroll); i++)
                g.drawString(s.font(), lines.get(i + CalculatorSession.scroll), x, top + i * GuideElements.LINE, colors.get(i + CalculatorSession.scroll), false);
            if (lines.size() > visible) {
                int barH = Math.max(8, (bottom - top) * visible / lines.size());
                int barY = top + (bottom - top - barH) * CalculatorSession.scroll / Math.max(1, lines.size() - visible);
                g.fill(x + w - 2, top, x + w, bottom, GuideElements.TINT);
                g.fill(x + w - 2, barY, x + w, barY + barH, GuideElements.RULE);
            }
            int half = (w - 2) / 2, buttonY = y + height - BUTTON_H;
            var library = CalculatorLibrary.get();
            boolean saved = library.isSaved(r.id()), full = !saved && library.savedFull();
            s.textButton(g, x, buttonY, half, BUTTON_H, t(saved ? "remove" : full ? "saved_full" : "save"), mx, my, !full);
            if (full && over(mx, my, x, buttonY, half, BUTTON_H)) s.textTooltip(List.of(t("saved_full.tooltip", CalculatorLibrary.SAVED_LIMIT)));
            s.textButton(g, x + half + 2, buttonY, w - half - 2, BUTTON_H, t("view_layout"), mx, my, r.plant() != null);
        }

        private static boolean available(CalculationResult r, CalculatorSession.View view) {
            return switch (view) {
                case EXCHANGER -> r.route() == Route.ADVANCED;
                case TURBINE -> r.route() != Route.SOLID;
                default -> true;
            };
        }

        private void layout(GuideScreen s, GuiGraphics g, int x, int y, int w, int mx, int my, CalculationResult r) {
            var machines = r.plant().machines();
            int count = machines.size() + (machines.size() > 1 ? 1 : 0);
            int bw = (w - 2 * (count - 1)) / count;
            for (int i = 0; i < count; i++) {
                int machine = machines.size() > 1 ? i - 1 : 0;
                int bx = x + i * (bw + 2);
                Component label = machine < 0 ? t("layout.plant") : CalculatorReport.machineName(machines.get(machine).machine().kind());
                boolean active = CalculatorSession.machine == machine;
                biggerreactorsrework.client.GuiArt.buttonBackground(g, bx, y, bw, BUTTON_H, !active && over(mx, my, bx, y, bw, BUTTON_H), active, false,
                        biggerreactorsrework.client.GuiFrame.REACTOR);
                s.fittedText(g, label, bx + 2, y + 3, bw - 4, 0xFFEEEEEE, true);
            }
            if (machines.size() == 1) CalculatorSession.machine = 0;
            var spread = preview(r, CalculatorSession.machine);
            spread.viewer.render(s, g, x, y + BUTTON_H + 4, w, my >= y + BUTTON_H + 4 ? mx : -1, my);
        }

        public boolean click(GuideScreen s, double mx, double my, int x, int y, int w, int button) {
            var r = CalculatorSession.shown;
            if (r == null || CalculatorClient.running()) return false;
            if (CalculatorSession.layout) {
                var machines = r.plant().machines();
                int count = machines.size() + (machines.size() > 1 ? 1 : 0);
                int bw = (w - 2 * (count - 1)) / count;
                if (over(mx, my, x, y, w, BUTTON_H)) {
                    int i = Math.min(count - 1, (int) ((mx - x) / (bw + 2)));
                    CalculatorSession.machine = machines.size() > 1 ? i - 1 : 0;
                    CalculatorSession.legendScroll = 0;
                    s.playClick();
                    return true;
                }
                draggingViewer = preview(r, CalculatorSession.machine).viewer.click(s, mx, my, x, y + BUTTON_H + 4, w, button);
                return draggingViewer;
            }
            if (over(mx, my, x, y, VIEWS.length * 20, 18)) {
                int i = (int) ((mx - x) / 20);
                if (i < VIEWS.length && available(r, VIEWS[i])) {
                    CalculatorSession.view = VIEWS[i];
                    CalculatorSession.scroll = 0;
                    s.playClick();
                }
                return true;
            }
            int half = (w - 2) / 2, buttonY = y + height - BUTTON_H;
            if (over(mx, my, x, buttonY, half, BUTTON_H)) {
                var library = CalculatorLibrary.get();
                if (library.isSaved(r.id())) library.removeSaved(r.id());
                else library.save(r);
                s.playClick();
                return true;
            }
            if (over(mx, my, x + half + 2, buttonY, w - half - 2, BUTTON_H) && r.plant() != null) {
                CalculatorSession.layout = true;
                CalculatorSession.machine = -1;
                CalculatorSession.legendScroll = 0;
                s.playClick();
                return true;
            }
            return false;
        }

        @Override
        public boolean drag(GuideScreen s, double dx, double dy) {
            var r = CalculatorSession.shown;
            return draggingViewer && r != null && CalculatorSession.layout && preview(r, CalculatorSession.machine).viewer.drag(s, dx, dy);
        }

        @Override
        public void release() {
            var r = CalculatorSession.shown;
            if (draggingViewer && r != null && r.plant() != null) preview(r, CalculatorSession.machine).viewer.release();
            draggingViewer = false;
        }

        @Override
        public boolean scroll(GuideScreen s, double mx, double my, int x, int y, int w, int h, double amount) {
            var r = CalculatorSession.shown;
            if (r == null || CalculatorClient.running()) return false;
            if (CalculatorSession.layout) return preview(r, CalculatorSession.machine).viewer.scroll(s, mx, my, x, y + BUTTON_H + 4, w, h, amount);
            CalculatorSession.scroll = Math.max(0, CalculatorSession.scroll - (int) Math.signum(amount) * 2);
            return true;
        }
    }
}
