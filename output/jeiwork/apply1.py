import re
root='src/main/java/biggerreactorsrework/client/deps/'
def rd(p):
    return open(p,newline='').read()
def wr(p,s,crlf):
    s=s.replace('\r\n','\n')
    if crlf: s=s.replace('\n','\r\n')
    open(p,'w',newline='').write(s)
p=root+'RecipeViewerData.java'
s=rd(p); crlf='\r\n' in s; s=s.replace('\r\n','\n')
start=s.index('    public static List<Component> moderatorLines')
end=s.index('    /**\n     * The Cyanite Reprocessor')
stats=open('output/jeiwork/stats.java').read()
s=s[:start]+stats+'\n'+s[end:]
s=s.replace('import biggerreactorsrework.client.GuiArt;\n','import biggerreactorsrework.client.GuiArt;\nimport biggerreactorsrework.client.GuiTheme;\n')
s=s.replace('import net.minecraft.Util;\n','import net.minecraft.ChatFormatting;\nimport net.minecraft.Util;\nimport net.minecraft.client.Minecraft;\nimport net.minecraft.client.gui.Font;\n')
s=s.replace('import net.minecraft.network.chat.Component;\n','import net.minecraft.client.resources.language.I18n;\nimport net.minecraft.network.chat.Component;\nimport net.minecraft.util.FastColor;\nimport net.minecraft.util.Mth;\n')
s=s.replace('import java.util.List;\n','import java.util.List;\nimport java.util.function.ToDoubleFunction;\n')
wr(p,s,crlf)
print('ok')
