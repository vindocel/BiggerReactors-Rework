package biggerreactorsrework.compat.jade;

import biggerreactorsrework.core.client.gui.RenderHelper;
import net.minecraft.nbt.CompoundTag;
//? if =26.1.2 || =26.3 {
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
//?} else if =1.21.1 {
/*import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
*/
//?}
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client side of the energy bar: stored and capacity in FE, written the same way as the screens' energy tooltip.
 */
enum EnergyBarComponent implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(BiggerReactorsJadePlugin.ENERGY)) {
            return;
//? if =26.1.2 || =26.3 {
        }
        long energy = Math.max(0, data.getLongOr(BiggerReactorsJadePlugin.ENERGY, 0L));
        long capacity = Math.max(1, data.getLongOr(BiggerReactorsJadePlugin.CAPACITY, 0L));
        tooltip.add(EnergyBarElement.of(energy / (double) capacity, Component.literal(
//?} else if =1.21.1 {
/*        }
        long energy = Math.max(0, data.getLong(BiggerReactorsJadePlugin.ENERGY));
        long capacity = Math.max(1, data.getLong(BiggerReactorsJadePlugin.CAPACITY));
        tooltip.add(EnergyBarElement.of(energy / (double) capacity, Component.literal(
*/
//?}
                RenderHelper.formatValue(energy, "FE") + " / " + RenderHelper.formatValue(capacity, "FE"))));
    }

//? if =26.1.2 || =26.3 {
    @Override
    public Identifier getUid() {
        return BiggerReactorsJadePlugin.UID;
//?} else if =1.21.1 {
/*    @Override
    public ResourceLocation getUid() {
        return BiggerReactorsJadePlugin.UID;
*/
//?}
    }
}
