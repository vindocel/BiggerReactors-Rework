package biggerreactorsrework;

import biggerreactorsrework.machine.client.CyaniteReprocessorScreen;
import biggerreactorsrework.machine.containers.CyaniteReprocessorContainer;
import biggerreactorsrework.multiblocks.heatexchanger.client.HeatExchangerFluidPortScreen;
import biggerreactorsrework.multiblocks.heatexchanger.client.HeatExchangerTerminalScreen;
import biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerFluidPortContainer;
import biggerreactorsrework.multiblocks.heatexchanger.containers.HeatExchangerTerminalContainer;
import biggerreactorsrework.multiblocks.reactor.client.*;
import biggerreactorsrework.multiblocks.reactor.containers.*;
import biggerreactorsrework.multiblocks.turbine.client.TurbineFluidPortScreen;
import biggerreactorsrework.multiblocks.turbine.client.TurbineTerminalScreen;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineFluidPortContainer;
import biggerreactorsrework.multiblocks.turbine.containers.TurbineTerminalContainer;
import biggerreactorsrework.multiblocks.heatexchanger.client.HeatExchangerChannelModels;
import biggerreactorsrework.multiblocks.turbine.client.TurbineRotorRenderer;
import biggerreactorsrework.multiblocks.turbine.tiles.TurbineRotorBearingTile;
import biggerreactorsrework.core.registry.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only bootstrap. Only referenced behind a dist check so a dedicated server never loads it.
 */
public final class BiggerReactorsClient {
    private BiggerReactorsClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(BiggerReactorsClient::registerScreens);
        modBus.addListener(BiggerReactorsClient::registerRenderers);
        modBus.addListener(BiggerReactorsClient::registerClientExtensions);
        HeatExchangerChannelModels.init(modBus);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        assert TurbineRotorBearingTile.SUPPLIER.TYPE != null;
        event.registerBlockEntityRenderer(TurbineRotorBearingTile.SUPPLIER.TYPE, TurbineRotorRenderer::new);
    }
    
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (var info : Registry.FLUID_CLIENT_INFO) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return info.stillTexture();
                }
                
                @Override
                public ResourceLocation getFlowingTexture() {
                    return info.flowingTexture();
                }
                
                @Override
                public ResourceLocation getOverlayTexture() {
                    return info.overlayTexture();
                }
                
                @Override
                public int getTintColor() {
                    return info.tintColor();
                }
            }, info.type());
        }
    }
    
    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ReactorTerminalContainer.INSTANCE, CommonReactorTerminalScreen::new);
        event.register(ReactorCoolantPortContainer.INSTANCE, ReactorCoolantPortScreen::new);
        event.register(ReactorAccessPortContainer.INSTANCE, ReactorAccessPortScreen::new);
        event.register(ReactorControlRodContainer.INSTANCE, ReactorControlRodScreen::new);
        event.register(ReactorRedstonePortContainer.INSTANCE, ReactorRedstonePortScreen::new);
        event.register(TurbineTerminalContainer.INSTANCE, TurbineTerminalScreen::new);
        event.register(TurbineFluidPortContainer.INSTANCE, TurbineFluidPortScreen::new);
        event.register(HeatExchangerTerminalContainer.INSTANCE, HeatExchangerTerminalScreen::new);
        event.register(HeatExchangerFluidPortContainer.INSTANCE, HeatExchangerFluidPortScreen::new);
        event.register(CyaniteReprocessorContainer.INSTANCE, CyaniteReprocessorScreen::new);
    }
}
