    /**
     * Moderator and coil properties drawn as bars. Each bar is scaled to the largest value of that property and
     * colored by how the value ranks among every registered entry: green near the top, red near the bottom.
     */
    public static final class Stats {
        private Stats() {
        }

        public static final int WIDTH = 168;
        public static final int ROW = 11;
        private static final int LABEL_X = 22;
        private static final int MARK_X = 77;
        private static final int BAR_X = 86;
        private static final int BAR_WIDTH = 48;
        private static final int BAR_HEIGHT = 5;
        private static final int BAR_FRAME = 0xFF373737;
        private static final int BAR_TRACK = 0xFF5A5A5A;
        private static final int LOW = 0xFFD04838;
        private static final int MIDDLE = 0xFFE0B030;
        private static final int HIGH = 0xFF48B048;
        private static final int TRADE_OFF = 0xFF5A8CD8;
        private static final String KEY = "recipe_viewer.biggerreactorsrework.stat.";

        /**
         * One property: its value, the largest value registered, its place counted from the highest (1 is the
         * highest) among {@code count} entries, and its rank from 0 (lowest) to 1 (highest). A trade-off property
         * has no better direction and is drawn in a neutral color.
         */
        public record Stat(String name, double value, double max, int place, int count, double rank, boolean tradeOff) {
        }

        public static List<Stat> moderator(ReactorModeratorRegistry.IModeratorProperties properties) {
            List<ReactorModeratorRegistry.IModeratorProperties> all = new ArrayList<>();
            ReactorModeratorRegistry.Client.forEach((block, entry) -> {
                if (!(block instanceof AirBlock)) {
                    all.add(entry);
                }
            });
            return List.of(
                    stat("moderation", properties.moderation(), all, ReactorModeratorRegistry.IModeratorProperties::moderation, false),
                    stat("absorption", properties.absorption(), all, ReactorModeratorRegistry.IModeratorProperties::absorption, true),
                    stat("efficiency", properties.heatEfficiency(), all, ReactorModeratorRegistry.IModeratorProperties::heatEfficiency, false),
                    stat("conductivity", properties.heatConductivity(), all, ReactorModeratorRegistry.IModeratorProperties::heatConductivity, false));
        }

        public static List<Stat> coil(TurbineCoilRegistry.CoilData data) {
            List<TurbineCoilRegistry.CoilData> all = new ArrayList<>(TurbineCoilRegistry.Client.getImmutableRegistry().values());
            return List.of(
                    stat("coil_efficiency", data.efficiency, all, coil -> coil.efficiency, false),
                    stat("bonus", data.bonus, all, coil -> coil.bonus, false),
                    stat("extraction", data.extractionRate, all, coil -> coil.extractionRate, false));
        }

        private static <T> Stat stat(String name, double value, List<T> all, ToDoubleFunction<T> property, boolean tradeOff) {
            double max = 0;
            int lower = 0, higher = 0, equal = 0;
            for (T entry : all) {
                double other = property.applyAsDouble(entry);
                max = Math.max(max, other);
                if (other < value) {
                    lower++;
                } else if (other > value) {
                    higher++;
                } else {
                    equal++;
                }
            }
            int count = Math.max(1, lower + higher + equal);
            double rank = count == 1 ? 1 : (lower + (equal - 1) / 2.0) / (count - 1);
            return new Stat(name, value, Math.max(max, value), higher + 1, count, Mth.clamp(rank, 0, 1), tradeOff);
        }

        public static int height(List<Stat> stats) {
            return stats.size() * ROW;
        }

        /** The slot frame for the block or fluid, centered on the left. */
        public static int slotY(List<Stat> stats) {
            return (height(stats) - 18) / 2;
        }

        public static void draw(GuiGraphics g, List<Stat> stats) {
            Font font = Minecraft.getInstance().font;
            int text = GuiTheme.textColor();
            GuiArt.slot(g, 0, slotY(stats), 18);
            for (int i = 0; i < stats.size(); i++) {
                Stat stat = stats.get(i);
                int y = i * ROW + 2;
                GuiArt.scrollingText(g, font, Component.translatable(KEY + stat.name()).getString(), LABEL_X, y, MARK_X - LABEL_X - 2, text);
                g.drawString(font, mark(stat), MARK_X, y, color(stat), false);
                int barY = y + 1;
                g.fill(BAR_X - 1, barY - 1, BAR_X + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, BAR_FRAME);
                g.fill(BAR_X, barY, BAR_X + BAR_WIDTH, barY + BAR_HEIGHT, BAR_TRACK);
                int fill = stat.max() <= 0 ? 0 : (int) Math.round(BAR_WIDTH * stat.value() / stat.max());
                if (stat.value() > 0) {
                    fill = Math.max(1, fill);
                }
                g.fill(BAR_X, barY, BAR_X + fill, barY + BAR_HEIGHT, color(stat));
                g.fill(BAR_X, barY, BAR_X + fill, barY + 1, FastColor.ARGB32.lerp(0.35f, color(stat), GuiArt.WHITE));
                String value = format(stat.value());
                g.drawString(font, value, WIDTH - font.width(value), y, text, false);
            }
        }

        /** Index of the row under the mouse, or -1. */
        public static int row(List<Stat> stats, double mouseX, double mouseY) {
            if (mouseX < LABEL_X || mouseX >= WIDTH || mouseY < 0) {
                return -1;
            }
            int row = (int) (mouseY / ROW);
            return row < stats.size() ? row : -1;
        }

        public static int rowX() {
            return LABEL_X;
        }

        public static int rowWidth() {
            return WIDTH - LABEL_X;
        }

        public static List<Component> tooltip(Stat stat) {
            String key = KEY + stat.name();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("guide.biggerreactorsrework.table." + stat.name() + ".title"));
            lines.add(Component.translatable("guide.biggerreactorsrework.table." + stat.name() + ".tip").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(KEY + "place", format(stat.value()), stat.place(), stat.count()).withStyle(ChatFormatting.DARK_AQUA));
            if (!stat.tradeOff()) {
                if (stat.rank() >= 2 / 3.0) {
                    lines.add(Component.translatable(key + ".high").withStyle(ChatFormatting.GREEN));
                } else if (stat.rank() <= 1 / 3.0) {
                    lines.add(Component.translatable(key + ".low").withStyle(ChatFormatting.RED));
                } else {
                    lines.add(Component.translatable(KEY + "average").withStyle(ChatFormatting.YELLOW));
                }
            }
            if (I18n.exists(key + ".pro")) {
                lines.add(Component.translatable(key + ".pro").withStyle(ChatFormatting.GREEN));
            }
            if (I18n.exists(key + ".con")) {
                lines.add(Component.translatable(key + ".con").withStyle(ChatFormatting.RED));
            }
            return lines;
        }

        private static String mark(Stat stat) {
            if (stat.tradeOff()) {
                return "±";
            }
            return stat.rank() >= 2 / 3.0 ? "▲" : stat.rank() <= 1 / 3.0 ? "▼" : "◆";
        }

        private static int color(Stat stat) {
            if (stat.tradeOff()) {
                return TRADE_OFF;
            }
            double rank = stat.rank();
            return rank < 0.5 ? FastColor.ARGB32.lerp((float) (rank * 2), LOW, MIDDLE) : FastColor.ARGB32.lerp((float) (rank * 2 - 1), MIDDLE, HIGH);
        }

        private static String format(double value) {
            String text = String.format(java.util.Locale.ROOT, "%.3f", value);
            text = text.replaceAll("0+$", "");
            return text.endsWith(".") ? text + "0" : text;
        }
    }
