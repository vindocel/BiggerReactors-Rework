package biggerreactorsrework.client.guide;


import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Item references in the guide text: the first time a chapter names an item that helps to
 * explain it, the word is underlined and hovering it shows the item's icon and name. The references are editorial:
 * {@link #POLICY} lists, per paragraph, the items worth showing. Words are matched against the item's name in the
 * current language and the aliases in {@code guide.biggerreactorsrework.alias.<namespace>.<path>} ("|"-separated).
 */
final class GuideReferences {
    /** Marks a referenced word through its style's insertion, which survives line wrapping. */
    private static final String PREFIX = "biggerreactorsrework:guide_ref/";
    private static final String KEY_PREFIX = "guide.biggerreactorsrework.";

    private static final Map<String, List<String>> POLICY = Map.of();

    /** Items already referenced in the chapter being built: only the first mention is marked. */
    private static final Set<ResourceLocation> SEEN = new HashSet<>();

    /** What a reference shows: an item, or a fluid that has no item of its own. */
    record Target(ItemStack stack, Fluid fluid, Component name) {
    }

    /** A referenced word on one line, in pixels from the start of the line. */
    record Span(int start, int end, ResourceLocation id) {
    }

    private record Alias(String text, ResourceLocation id) {
    }

    private GuideReferences() {
    }

    /** Every chapter marks its own first mentions. */
    static void startChapter() {
        SEEN.clear();
    }

    /**
     * Translated highlights use readable green inks on paper, retaining red for warnings.
     * The first mention of each referenced item is marked.
     */
    static Component ink(Component text) {
        String raw = text.getString();
        StringBuilder plain = new StringBuilder(raw.length());
        List<Style> styles = new ArrayList<>(raw.length());
        Style style = Style.EMPTY;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(++i));
                ChatFormatting format = ChatFormatting.getByCode(switch (code) {
                    case 'b', 'd', '5', 'e' -> '2';
                    case 'a' -> '2';
                    case 'c' -> '4';
                    default -> code;
                });
                if (format != null) {
                    style = format == ChatFormatting.RESET ? Style.EMPTY : style.applyLegacyFormat(format);
                    if (code == 'b' || code == 'd' || code == '5' || code == 'e' || code == 'a') {
                        style = style.withColor(GuideElements.HEADING);
                    }
                }
                continue;
            }
            plain.append(c);
            styles.add(style);
        }
        if (text.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(KEY_PREFIX)) {
            mark(plain.toString(), styles, POLICY.getOrDefault(translatable.getKey().substring(KEY_PREFIX.length()), List.of()));
        }
        MutableComponent out = Component.empty();
        int start = 0;
        for (int i = 1; i <= plain.length(); i++) {
            if (i == plain.length() || !styles.get(i).equals(styles.get(start))) {
                out.append(Component.literal(plain.substring(start, i)).withStyle(styles.get(start)));
                start = i;
            }
        }
        return out;
    }

    /** Marks the first whole-word mention of each referenced item not yet seen in the chapter, longest name first. */
    private static void mark(String plain, List<Style> styles, List<String> allowed) {
        if (allowed.isEmpty()) {
            return;
        }
        List<Alias> aliases = new ArrayList<>();
        for (String entry : allowed) {
            ResourceLocation id = entry.contains(":") ? ResourceLocation.parse(entry) : GuideIds.id(entry);
            Target target = target(id);
            if (target == null) {
                continue;
            }
            aliases.add(new Alias(fold(target.name().getString()), id));
            String key = "guide.biggerreactorsrework.alias." + id.getNamespace() + "." + id.getPath();
            if (Language.getInstance().has(key)) {
                for (String alias : Language.getInstance().getOrDefault(key).split("\\|")) {
                    if (!alias.isBlank()) {
                        aliases.add(new Alias(fold(alias.strip()), id));
                    }
                }
            }
        }
        aliases.sort(Comparator.comparingInt((Alias a) -> a.text().length()).reversed());
        String folded = fold(plain);
        for (int i = 0; i < folded.length(); i++) {
            if (i > 0 && Character.isLetterOrDigit(folded.charAt(i - 1))) {
                continue;
            }
            for (Alias alias : aliases) {
                int end = i + alias.text().length();
                if (alias.text().isEmpty() || !folded.startsWith(alias.text(), i)
                        || end < folded.length() && Character.isLetterOrDigit(folded.charAt(end))) {
                    continue;
                }
                // A name already seen still takes its words, so a shorter name inside it is not marked.
                if (SEEN.add(alias.id())) {
                    for (int c = i; c < end; c++) {
                        styles.set(c, styles.get(c).withInsertion(PREFIX + alias.id()));
                    }
                }
                i = end - 1;
                break;
            }
        }
    }

    /** Lower case without accents, one character for one, so positions match the original text. */
    private static String fold(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            String decomposed = Normalizer.normalize(String.valueOf(text.charAt(i)), Normalizer.Form.NFD);
            out.append(Character.toLowerCase(decomposed.charAt(0)));
        }
        return out.toString();
    }

    /** The item (or fluid) behind an id, or null when nothing is registered under it. */
    static Target target(ResourceLocation id) {
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            return new Target(stack, null, stack.getHoverName());
        }
        if (BuiltInRegistries.FLUID.containsKey(id)) {
            Fluid fluid = BuiltInRegistries.FLUID.get(id);
            return new Target(ItemStack.EMPTY, fluid, fluid.getFluidType().getDescription());
        }
        return null;
    }

    /** The referenced words of one wrapped line, with their position in pixels. */
    static List<Span> spans(Font font, FormattedCharSequence line) {
        List<Span> out = new ArrayList<>();
        int[] x = {0};
        int[] start = {0};
        String[] current = {null};
        line.accept((index, style, codePoint) -> {
            String insertion = style.getInsertion();
            String id = insertion != null && insertion.startsWith(PREFIX) ? insertion.substring(PREFIX.length()) : null;
            if (!Objects.equals(id, current[0])) {
                if (current[0] != null) {
                    out.add(new Span(start[0], x[0], ResourceLocation.parse(current[0])));
                }
                current[0] = id;
                start[0] = x[0];
            }
            x[0] += font.width(FormattedCharSequence.codepoint(codePoint, style));
            return true;
        });
        if (current[0] != null) {
            out.add(new Span(start[0], x[0], ResourceLocation.parse(current[0])));
        }
        return out;
    }
}
