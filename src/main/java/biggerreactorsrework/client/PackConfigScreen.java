package biggerreactorsrework.client;

// Modified 2026-11-01: native Minecraft 26.1.2 API.
import biggerreactorsrework.core.config.PackConfiguration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Added 2026-11-01: searchable native configuration editor; server rules stay read-only inside worlds. */
public final class PackConfigScreen extends Screen {
    private final Screen parent;
    private final List<Button> rows = new ArrayList<>();
    private EditBox search;
    private int page;
    private int pageSize;
    private List<PackConfiguration.Option> filtered = List.of();
    private Button previous, next;
    private String query = "";
    public PackConfigScreen(Screen parent) { super(text("title")); this.parent = parent; }
    private static Component text(String key, Object... args) { return Component.translatable("config.biggerreactorsrework." + key, args); }
    private static Component optionName(PackConfiguration.Option option) { return Component.translatableWithFallback("config.biggerreactorsrework.option." + option.key(), option.key()); }
    @Override protected void init() {
        rows.clear();
        int w = Math.min(480, width - 24), x = (width - w) / 2;
        pageSize = Math.max(1, (height - 130) / 24);
        search = addRenderableWidget(new EditBox(font, x, 43, w, 20, text("search")));
        search.setHint(text("search")); search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; refresh(); });
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; refresh(); }).bounds(x, height - 49, 40, 20).build(ConfigButton::new));
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; refresh(); }).bounds(x + w - 40, height - 49, 40, 20).build(ConfigButton::new));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 70, height - 25, 140, 20).build(ConfigButton::new));
        refresh();
    }
    private void refresh() {
        rows.forEach(this::removeWidget); rows.clear();
        String find = query.toLowerCase(Locale.ROOT);
        filtered = PackConfiguration.options().stream().filter(o -> (o.registration().configType + " " + o.key() + " " + optionName(o).getString()).toLowerCase(Locale.ROOT).contains(find)).toList();
        page = Math.max(0, Math.min(page, Math.max(0, (filtered.size() - 1) / pageSize)));
        int w = Math.min(480, width - 24), x = (width - w) / 2;
        for (int i = page * pageSize; i < Math.min(filtered.size(), (page + 1) * pageSize); i++) {
            var option = filtered.get(i);
            var button = Button.builder(Component.literal(option.registration().configType.name() + " / ").append(optionName(option)).append(" = " + option.current()),
                    b -> minecraft.setScreen(new Editor(this, option))).bounds(x, 72 + (i % pageSize) * 24, w, 20)
                    .tooltip(Tooltip.create(Component.literal(option.node().generateComment()))).build(ConfigButton::new);
            button.active = PackConfiguration.editable(option);
            rows.add(addRenderableWidget(button));
        }
        previous.active = page > 0; next.active = (page + 1) * pageSize < filtered.size();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        g.centeredText(font, text("scope"), width / 2, 27, 0xFFBBBBBB);
        g.centeredText(font, Component.literal((page + 1) + " / " + Math.max(1, (filtered.size() + pageSize - 1) / pageSize)), width / 2, height - 43, 0xFFFFFFFF);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }

    private static final class ConfigButton extends Button {
        ConfigButton(Builder builder) { super(builder); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float dt) {
            GuiArt.buttonBackground(g, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), false, !active, GuiFrame.REACTOR);
            extractDefaultLabel(g.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
        }
    }

    private static final class Editor extends Screen {
        private final PackConfigScreen parent;
        private final PackConfiguration.Option option;
        private EditBox input;
        private Component error = Component.empty();
        private Button save;
        private String draft;
        Editor(PackConfigScreen parent, PackConfiguration.Option option) { super(optionName(option)); this.parent = parent; this.option = option; this.draft = option.current(); }
        @Override protected void init() {
            int w = Math.min(480, width - 24), x = (width - w) / 2;
            input = addRenderableWidget(new EditBox(font, x, 75, w, 20, title)); input.setMaxLength(32768); input.setValue(draft);
            save = addRenderableWidget(Button.builder(text("save"), b -> {
                try { PackConfiguration.save(option, input.getValue()); onClose(); }
                catch (java.io.IOException | RuntimeException failure) { error = text("error", failure.getMessage()); }
            }).bounds(width / 2 - 145, height - 29, 140, 20).build(ConfigButton::new));
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 5, height - 29, 140, 20).build(ConfigButton::new));
            input.setResponder(value -> { draft = value; try { option.checked(value); save.active = PackConfiguration.editable(option); error = Component.empty(); }
                catch (IllegalArgumentException failure) { save.active = false; error = text("invalid"); } });
            setInitialFocus(input);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
            super.extractRenderState(g, mx, my, dt);
            int w = Math.min(480, width - 24), x = (width - w) / 2;
            g.centeredText(font, title, width / 2, 17, 0xFFFFFFFF);
            g.centeredText(font, text("restart"), width / 2, 39, 0xFFBBBBBB);
            int y = 108;
            for (var line : font.split(Component.literal(option.node().generateComment()), w)) { if(y > height - 68) break; g.text(font, line, x, y, 0xFFCCCCCC); y += 11; }
            if (!error.getString().isEmpty()) g.centeredText(font, error, width / 2, height - 51, 0xFFFF8888);
        }
        @Override public void onClose() { minecraft.setScreen(parent); }
    }
}
