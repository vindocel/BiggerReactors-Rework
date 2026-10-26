p='src/main/java/biggerreactorsrework/client/deps/emi/BiggerReactorsEmiPlugin.java'
s=open(p,newline='').read(); crlf='\r\n' in s; s=s.replace('\r\n','\n')
s=s.replace('''    private static final ResourceLocation INFO_BACKGROUND = ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "textures/jei/common.png");
    private static final int INFO_WIDTH = 144;
    private static final int INFO_TEXT_X = 80;
    private static final int INFO_LINE = 12;
    private static final int INFO_TEXT = 0xFF000000;
''','')
s=s.replace('''RecipeViewerData.moderatorLines(moderator.properties()), 0, 46));''','''Stats.moderator(moderator.properties())));''')
s=s.replace('''RecipeViewerData.coilLines(coil.data()), 6, 34));''','''Stats.coil(coil.data())));''')
a=s.index('    /** A block or fluid with its properties written beside it')
b=s.rindex('}')
s=s[:a]+'''    /** A block or fluid with its properties drawn as bars beside it, like the JEI categories. */
    private static final class Info extends BasicEmiRecipe {
        private final EmiIngredient stack;
        private final List<Stats.Stat> stats;

        Info(EmiRecipeCategory category, String kind, EmiStack stack, List<Stats.Stat> stats) {
            super(category, syntheticId(kind, stack.getId()), Stats.WIDTH, Stats.height(stats));
            this.stack = stack;
            this.stats = stats;
            inputs.add(stack);
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, Stats.WIDTH, Stats.height(stats), (g, mouseX, mouseY, delta) -> Stats.draw(g, stats));
            widgets.addSlot(stack, 0, Stats.slotY(stats)).drawBack(false);
            for (int i = 0; i < stats.size(); i++) {
                widgets.addTooltipText(Stats.tooltip(stats.get(i)), Stats.rowX(), i * Stats.ROW, Stats.rowWidth(), Stats.ROW);
            }
        }
    }
'''+s[b:]
s=s.replace('import biggerreactorsrework.client.deps.RecipeViewerData.Cyanite;\n','import biggerreactorsrework.client.deps.RecipeViewerData.Cyanite;\nimport biggerreactorsrework.client.deps.RecipeViewerData.Stats;\n')
s=s.replace('import dev.emi.emi.api.widget.TextWidget;\n','')
if crlf: s=s.replace('\n','\r\n')
open(p,'w',newline='').write(s); print('ok')
