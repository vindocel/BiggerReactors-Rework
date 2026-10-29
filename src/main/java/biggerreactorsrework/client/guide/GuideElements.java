package biggerreactorsrework.client.guide;

import biggerreactorsrework.client.deps.RecipeViewers;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** The building blocks of the guide pages, printed in ink on the cream pages of the reactor guide. */
final class GuideElements {
    /** Green inks coordinated with the cover. */
    static final int BODY = 0xFF343C30;
    static final int HEADING = 0xFF3D641F;
    static final int MUTED = 0xFF687357;
    static final int TIP = 0xFF476F28;
    static final int WARNING = 0xFFB0402A;
    static final int RULE = 0x603D641F;
    static final int TINT = 0x223D641F;
    /** Dotted underline of an item reference. */
    static final int REFERENCE = 0xA03D641F;
    static final int LINE = 10;

    private GuideElements() {
    }

    /** Pixel arrow shared by crafting and process diagrams; independent of font glyphs. */
    static void arrow(GuiGraphics g, int x, int y) {
        g.fill(x, y + 3, x + 10, y + 5, HEADING);
        for (int step = 0; step < 4; step++) {
            g.fill(x + 7 + step, y + step, x + 8 + step, y + 8 - step, HEADING);
        }
    }

    /** Section title with a thin rule under it. */
    record Heading(Component text) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 15;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            screen.fittedText(g, text, x, y + 2, width, HEADING);
            g.fill(x, y + 12, x + width, y + 13, RULE);
        }
    }

    /** First block of a chapter: its icon and title. */
    record ChapterTitle(Supplier<ItemStack> icon, Component title) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 24;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            g.renderItem(icon.get(), x, y + 1);
            screen.fittedText(g, title, x + 20, y + 5, width - 20, HEADING);
            g.fill(x, y + 20, x + width, y + 21, RULE);
            g.fill(x, y + 22, x + width, y + 23, RULE);
        }
    }

    /**
     * Wrapped paragraph; the pagination may split it into {@link Lines} across pages. It is inked (and its item
     * references marked) when created, so the chapter's first mentions follow the reading order.
     */
    static final class Text implements GuideElement {
        private final Component text;
        private int cachedWidth = -1;
        private Lines lines = new Lines(List.of());

        Text(Component text) {
            this.text = GuideReferences.ink(text);
        }

        List<FormattedCharSequence> lines(GuideScreen screen, int width) {
            if (width != cachedWidth) {
                cachedWidth = width;
                lines = new Lines(screen.font().split(text, width));
            }
            return lines.lines();
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return lines(screen, width).size() * LINE;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            lines(screen, width);
            lines.render(screen, g, x, y, width, mouseX, mouseY);
        }
    }

    /**
     * Some lines of a paragraph (a paragraph split between two pages). Item references are underlined with dots;
     * hovering one tints it and shows the item.
     */
    static final class Lines implements GuideElement {
        private final List<FormattedCharSequence> lines;
        private List<List<GuideReferences.Span>> spans;

        Lines(List<FormattedCharSequence> lines) {
            this.lines = lines;
        }

        List<FormattedCharSequence> lines() {
            return lines;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return lines.size() * LINE;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            if (spans == null) {
                spans = new ArrayList<>(lines.size());
                for (FormattedCharSequence line : lines) {
                    spans.add(GuideReferences.spans(screen.font(), line));
                }
            }
            for (int i = 0; i < lines.size(); i++) {
                int ly = y + i * LINE;
                for (GuideReferences.Span span : spans.get(i)) {
                    if (mouseX >= x + span.start() && mouseX < x + span.end() && mouseY >= ly - 1 && mouseY < ly + 10) {
                        g.fill(x + span.start() - 1, ly - 1, x + span.end() + 1, ly + 10, TINT);
                        screen.hoverReference(span.id());
                    }
                }
                g.drawString(screen.font(), lines.get(i), x, ly, BODY, false);
                for (GuideReferences.Span span : spans.get(i)) {
                    for (int px = span.start(); px < span.end() - 1; px += 2) {
                        g.fill(x + px, ly + 9, x + px + 1, ly + 10, REFERENCE);
                    }
                }
            }
        }
    }

    /** Boxed note with a colored bar and a title (tip or warning). */
    static final class Note implements GuideElement {
        private final Component title;
        private final Text body;
        private final int accent;
        private final boolean attention;

        Note(Component title, Component body, int accent) {
            this(title, body, accent, false);
        }

        Note(Component title, Component body, int accent, boolean attention) {
            this.title = title;
            this.body = new Text(body);
            this.accent = accent;
            this.attention = attention;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return 15 + body.height(screen, width - 10) + 4;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            int h = height(screen, width);
            g.fill(x, y, x + width, y + h, attention ? 0x38D4AC45 : 0x1E000000 | (accent & 0x00FFFFFF));
            g.fill(x, y, x + 2, y + h, accent);
            if (attention) {
                // Added 2026-10-29: a native pixel warning symbol, independent of font glyphs and external assets.
                for (int row = 0; row < 12; row++) {
                    int half = (row + 1) / 2;
                    g.fill(x + 12 - half, y + 2 + row, x + 13 + half, y + 3 + row, accent);
                    if (row >= 2 && row < 11) {
                        int inner = (row - 1) / 2;
                        g.fill(x + 12 - inner, y + 2 + row, x + 13 + inner, y + 3 + row, 0xFFE2C365);
                    }
                }
                g.fill(x + 11, y + 6, x + 13, y + 10, BODY);
                g.fill(x + 11, y + 11, x + 13, y + 12, BODY);
            }
            if (attention) screen.fittedText(g, title, x + 23, y + 4, width - 27, accent);
            else g.drawString(screen.font(), title, x + 6, y + 4, accent, false);
            body.render(screen, g, x + 6, y + 15, width - 10, mouseX, mouseY);
        }
    }

    /**
     * A short box (a mod integration, for example) with a button that opens its full text below it, on this page and
     * the next ones; clicking again closes it. The full text is added by the chapter while the box is open.
     */
    static final class Expandable implements GuideElement {
        private static final int BUTTON_H = 13;
        private final String key;
        private final Component title;
        private final Text summary;

        Expandable(String key, Component title, Component summary) {
            this.key = key;
            this.title = title;
            this.summary = new Text(summary);
        }

        private Component label() {
            return Component.translatable(GuideScreen.expanded(key) ? "guide.biggerreactorsrework.expand.less"
                    : "guide.biggerreactorsrework.expand.more");
        }

        private int buttonWidth(GuideScreen screen, int width) {
            return Math.min(width - 10, screen.font().width(label()) + 12);
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return 15 + summary.height(screen, width - 10) + 3 + BUTTON_H + 4;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            int h = height(screen, width);
            g.fill(x, y, x + width, y + h, TINT);
            g.fill(x, y, x + 2, y + h, HEADING);
            g.drawString(screen.font(), title, x + 6, y + 4, HEADING, false);
            summary.render(screen, g, x + 6, y + 15, width - 10, mouseX, mouseY);
            screen.textButton(g, x + 6, y + h - BUTTON_H - 4, buttonWidth(screen, width), BUTTON_H, label(), mouseX, mouseY);
        }

        @Override
        public boolean click(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int button) {
            int by = y + height(screen, width) - BUTTON_H - 4;
            if (mouseX < x + 6 || mouseX >= x + 6 + buttonWidth(screen, width) || mouseY < by || mouseY >= by + BUTTON_H) {
                return false;
            }
            screen.toggleExpanded(key);
            return true;
        }
    }

    /** An item with its name and a description beside it. */
    static final class ItemNote implements GuideElement {
        private final Supplier<ItemStack> stack;
        private final Text description;

        ItemNote(Supplier<ItemStack> stack, Component description) {
            this.stack = stack;
            this.description = new Text(description);
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return Math.max(20, 11 + description.height(screen, width - 22)) + 2;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            ItemStack item = stack.get();
            screen.itemSlot(g, item, x, y, mouseX, mouseY);
            screen.fittedText(g, item.getHoverName(), x + 22, y + 1, width - 22, HEADING);
            description.render(screen, g, x + 22, y + 12, width - 22, mouseX, mouseY);
        }
    }

    /** A row of items, then fluids (wrapping), with their tooltips. */
    static final class ItemRow implements GuideElement {
        private final Supplier<List<ItemStack>> items;
        private final List<Supplier<? extends Fluid>> fluids;

        ItemRow(Supplier<List<ItemStack>> items, List<Supplier<? extends Fluid>> fluids) {
            this.items = items;
            this.fluids = fluids;
        }

        private int perRow(int width) {
            return Math.max(1, (width + 2) / 20);
        }

        @Override
        public int height(GuideScreen screen, int width) {
            int count = items.get().size() + fluids.size();
            return ((count + perRow(width) - 1) / perRow(width)) * 20;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            List<ItemStack> list = items.get();
            int per = perRow(width);
            for (int i = 0; i < list.size() + fluids.size(); i++) {
                int sx = x + (i % per) * 20;
                int sy = y + (i / per) * 20;
                if (i < list.size()) {
                    screen.itemSlot(g, list.get(i), sx, sy, mouseX, mouseY);
                } else {
                    screen.fluidSlot(g, fluids.get(i - list.size()).get(), sx, sy, mouseX, mouseY);
                }
            }
        }
    }

    /** A crafting table recipe (shaped or shapeless) read from the recipe manager, tag ingredients cycling. */
    record Crafting(ResourceLocation id) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 80;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            Level level = Minecraft.getInstance().level;
            RecipeHolder<?> holder = level == null ? null : level.getRecipeManager().byKey(id).orElse(null);
            if (holder == null) {
                screen.fixedText(g, Component.translatable("guide.biggerreactorsrework.recipe_missing"), x, y + 4, width, MUTED);
                return;
            }
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            Component name = result.getCount() > 1
                    ? Component.literal(result.getCount() + "× ").append(result.getHoverName()) : result.getHoverName();
            screen.fixedText(g, name, x, y + 1, width, HEADING);
            int gridY = y + 11;
            List<Ingredient> ingredients = recipe.getIngredients();
            int recipeW = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int index = row * recipeW + col;
                    ItemStack shown = ItemStack.EMPTY;
                    if (col < recipeW && index < ingredients.size()) {
                        shown = cycle(ingredients.get(index));
                    }
                    screen.itemSlot(g, shown, x + col * 18, gridY + row * 18, mouseX, mouseY);
                }
            }
            int arrowX = x + 3 * 18 + 6;
            arrow(g, arrowX + 2, gridY + 23);
            screen.itemSlot(g, result, arrowX + 16, gridY + 18, mouseX, mouseY);
            Component kind = Component.translatable(recipe instanceof ShapedRecipe
                    ? "guide.biggerreactorsrework.recipe_shaped" : "guide.biggerreactorsrework.recipe_shapeless");
            screen.fixedText(g, kind, x, y + 69, width, MUTED);
        }

        private static ItemStack cycle(Ingredient ingredient) {
            ItemStack[] options = ingredient.getItems();
            if (options.length == 0) {
                return ItemStack.EMPTY;
            }
            return options[(int) ((Util.getMillis() / 1000) % options.length)];
        }
    }

    /** Opens machine recipe categories in EMI / JEI; hidden when no viewer is installed. */
    static final class ViewerButton implements GuideElement {
        private final Component label;
        private final ResourceLocation[] categories;

        ViewerButton(Component label, ResourceLocation... categories) {
            this.label = label;
            this.categories = categories;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return RecipeViewers.available() ? 16 : 0;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            if (RecipeViewers.available()) {
                screen.textButton(g, x, y, buttonWidth(screen, width), 15, label, mouseX, mouseY);
            }
        }

        private int buttonWidth(GuideScreen screen, int width) {
            return Math.min(width, screen.font().width(label) + 16);
        }

        @Override
        public boolean click(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int button) {
            if (!RecipeViewers.available() || mouseX < x || mouseX >= x + buttonWidth(screen, width) || mouseY < y || mouseY >= y + 15) {
                return false;
            }
            screen.playClick();
            RecipeViewers.show(categories);
            return true;
        }
    }

    /**
     * Table with a header row; the cells are recomputed every frame (they follow the server configuration). A table
     * longer than the page continues on the next pages, the header repeated on each.
     */
    static final class Table implements GuideElement {
        static final int ROW = 11;
        private final List<Component> headers;
        private final Supplier<List<List<String>>> rows;
        private final float[] columns;
        private final List<List<Component>> headerTips;
        private final java.util.function.Function<List<String>,GuideReferences.Target> rowReference;
        /** When set, every cell keeps its size and slides inside its column, and may point to what it names. */
        private final java.util.function.BiFunction<List<String>,Integer,GuideReferences.Target> cellReference;

        /** {@code columns}: relative width of each column. */
        Table(List<Component> headers, float[] columns, Supplier<List<List<String>>> rows) {
            this(headers,columns,rows,java.util.Collections.nCopies(headers.size(),List.of()));
        }
        Table(List<Component> headers,float[] columns,Supplier<List<List<String>>> rows,List<List<Component>> headerTips) {
            this(headers,columns,rows,headerTips,row->null);
        }
        Table(List<Component> headers,float[] columns,Supplier<List<List<String>>> rows,List<List<Component>> headerTips,
              java.util.function.Function<List<String>,GuideReferences.Target> rowReference) {
            this(headers,columns,rows,headerTips,rowReference,null);
        }
        private Table(List<Component> headers,float[] columns,Supplier<List<List<String>>> rows,List<List<Component>> headerTips,
              java.util.function.Function<List<String>,GuideReferences.Target> rowReference,
              java.util.function.BiFunction<List<String>,Integer,GuideReferences.Target> cellReference) {
            this.headers = headers;
            this.columns = columns;
            this.rows = rows;
            this.headerTips = headerTips;
            this.rowReference = rowReference;
            this.cellReference = cellReference;
        }

        /** Names in every column slide instead of shrinking; hovering one shows what {@code reference} finds for it. */
        static Table named(List<Component> headers,float[] columns,Supplier<List<List<String>>> rows,
                           java.util.function.BiFunction<List<String>,Integer,GuideReferences.Target> reference) {
            return new Table(headers,columns,rows,java.util.Collections.nCopies(headers.size(),List.of()),row->null,reference);
        }

        int rowCount() {
            return rows.get().size();
        }

        /** Rows {@code from} (inclusive) to {@code to} (exclusive) under the same header. */
        Table slice(int from, int to) {
            return new Table(headers, columns, () -> {
                List<List<String>> all = rows.get();
                return all.subList(Math.min(from, all.size()), Math.min(to, all.size()));
            },headerTips,rowReference,cellReference);
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return (rows.get().size() + 1) * ROW + 3;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            int[] starts = new int[columns.length + 1];
            float total = 0;
            for (float c : columns) {
                total += c;
            }
            float acc = 0;
            for (int i = 0; i < columns.length; i++) {
                starts[i] = x + Math.round(acc / total * width);
                acc += columns[i];
            }
            starts[columns.length] = x + width;
            g.fill(x, y, x + width, y + ROW, 0x3378AC32);
            for (int c = 0; c < headers.size(); c++) {
                screen.fixedText(g, headers.get(c), starts[c] + 2, y + 2, starts[c + 1] - starts[c] - 3, HEADING);
                if(mouseX>=starts[c]&&mouseX<starts[c+1]&&mouseY>=y&&mouseY<y+ROW&&!headerTips.get(c).isEmpty())screen.textTooltip(headerTips.get(c));
            }
            List<List<String>> list = rows.get();
            for (int r = 0; r < list.size(); r++) {
                int ry = y + (r + 1) * ROW;
                if (r % 2 == 1) {
                    g.fill(x, ry, x + width, ry + ROW, TINT);
                }
                List<String> cells = list.get(r);
                for (int c = 0; c < cells.size() && c < columns.length; c++) {
                    if(c==0)screen.fittedText(g,Component.literal(cells.get(c)),starts[c]+2,ry+2,starts[c+1]-starts[c]-3,HEADING);
                    else if(cellReference!=null)screen.fittedText(g,Component.literal(cells.get(c)),starts[c]+2,ry+2,starts[c+1]-starts[c]-3,BODY);
                    else screen.fixedText(g,Component.literal(cells.get(c)),starts[c]+2,ry+2,starts[c+1]-starts[c]-3,BODY);
                    if(mouseX>=starts[c]&&mouseX<starts[c+1]&&mouseY>=ry&&mouseY<ry+ROW){
                        var target=cellReference!=null?cellReference.apply(cells,c):c==0?rowReference.apply(cells):null;
                        if(target!=null)screen.materialTooltip(target);
                        else screen.textTooltip(List.of(Component.literal(cells.get(c))));
                    }
                }
            }
            g.fill(x, y + (list.size() + 1) * ROW, x + width, y + (list.size() + 1) * ROW + 1, RULE);
        }
    }

    /** Empty space. */
    record Spacer(int size) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return size;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
        }
    }
}
