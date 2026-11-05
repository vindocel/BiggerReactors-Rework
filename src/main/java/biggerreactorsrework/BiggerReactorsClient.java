package biggerreactorsrework;

// Modified 2026-10-26: scope derived sprites to synchronized generic coolant profiles.

import biggerreactorsrework.machine.client.IndustrialProcessorScreen;
import biggerreactorsrework.machine.containers.IndustrialProcessorContainer;
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

    public static boolean connected() { return net.minecraft.client.Minecraft.getInstance().getConnection()!=null; }

    public static void refreshPlacedCoolant(net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                           @javax.annotation.Nullable java.util.Map<String, String> previous, java.util.Map<String, String> next) {
        if (!(holder instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.level != chunk.getLevel()) return;
        var dirtySections = new java.util.HashSet<net.minecraft.core.SectionPos>();
        var changed = new java.util.HashSet<>(next.keySet());
        if (previous != null) changed.addAll(previous.keySet());
        for (String key : changed) {
            if (!java.util.Objects.equals(previous == null ? null : previous.get(key), next.get(key))) {
                dirtySections.add(net.minecraft.core.SectionPos.of(net.minecraft.core.BlockPos.of(Long.parseLong(key))));
            }
        }
        for (var section : dirtySections) client.levelRenderer.setSectionDirtyWithNeighbors(section.x(), section.y(), section.z());
    }
    private static boolean coolantReloadPending;
    public static void refreshCoolantSprites(java.util.List<biggerreactorsrework.registries.FluidTransitionRegistry.FluidTransition> profiles) {
        if (!coolantReloadPending && biggerreactorsrework.client.HeatedFluidSprites.missing(profiles)) {
            coolantReloadPending=true;
            net.minecraft.client.Minecraft.getInstance().reloadResourcePacks().whenComplete((unused,error)->coolantReloadPending=false);
        }
    }

    // Modified 2026-11-01: isolated here so a dedicated server never resolves client screen classes.
    static void registerConfigScreen(net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (net.neoforged.neoforge.client.gui.IConfigScreenFactory) (mod, parent) -> new biggerreactorsrework.client.PackConfigScreen(parent));
    }

    static void init(IEventBus modBus) {
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(biggerreactorsrework.registries.ModParticles.COOLANT_BUBBLE.get(), net.minecraft.client.particle.BubbleParticle.Provider::new);
            event.registerSpriteSet(biggerreactorsrework.registries.ModParticles.COOLANT_BUBBLE_POP.get(), net.minecraft.client.particle.BubblePopParticle.Provider::new);
            event.registerSpriteSet(biggerreactorsrework.registries.ModParticles.COOLANT_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        });
        // Modified 2026-10-28: positional loops and per-machine local volume.
        biggerreactorsrework.client.sound.MachineSoundManager.init();
        modBus.addListener(biggerreactorsrework.client.HeatedFluidSprites::register);
        modBus.addListener(biggerreactorsrework.multiblocks.turbine.client.TurbineAtmosphereEffects::registerParticles);
        biggerreactorsrework.client.GuiSettingsEvents.register();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                biggerreactorsrework.multiblocks.turbine.client.TurbineAtmosphereEffects::tick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> {
                    biggerreactorsrework.registries.CoolantSync.clearClient();
                    biggerreactorsrework.client.calculator.CalculatorClient.disconnected();
                });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterShadersEvent event) -> {
            try {
                event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BiggerReactors.modid, "dark_gui"),
                        com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX), biggerreactorsrework.client.GuiTheme::setShader);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException("Could not load the machine GUI theme shader", e);
            }
        });
        modBus.addListener(biggerreactorsrework.compat.DarkModeCompat::onInterModEnqueue);
        biggerreactorsrework.compat.DarkModeCompat.register();
        modBus.addListener(BiggerReactorsClient::registerScreens);
        modBus.addListener(BiggerReactorsClient::registerRenderers);
        modBus.addListener(BiggerReactorsClient::registerClientExtensions);
        HeatExchangerChannelModels.init(modBus);
        biggerreactorsrework.client.ConnectedGlassModels.init(modBus);
        biggerreactorsrework.client.ReactorRodModels.init(modBus);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() ->
                biggerreactorsrework.items.tools.GuideBookItem.open = biggerreactorsrework.client.guide.GuideBookAnimation::requestOpen));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(biggerreactorsrework.client.guide.GuideBookAnimation::onClientTick);
        // Added 2026-11-03: a short description of how Frozen Obsidian behaves.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) -> {
            if (event.getItemStack().is(biggerreactorsrework.fluids.FrozenObsidian.INSTANCE.getBucket()))
                event.getToolTip().add(net.minecraft.network.chat.Component.translatable("fluid.biggerreactorsrework.frozen_obsidian.description")
                        .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
        });
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) -> {
            biggerreactorsrework.client.guide.GuideBookRenderer.models().forEach(id -> event.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(id)));
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        assert TurbineRotorBearingTile.SUPPLIER.TYPE != null;
        event.registerBlockEntityRenderer(TurbineRotorBearingTile.SUPPLIER.TYPE, TurbineRotorRenderer::new);
        event.registerBlockEntityRenderer(biggerreactorsrework.multiblocks.reactor.tiles.ReactorManifoldTile.SUPPLIER.TYPE,ReactorManifoldRenderer::new);
        event.registerBlockEntityRenderer(biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerChannelTile.SUPPLIER.TYPE,biggerreactorsrework.multiblocks.heatexchanger.client.HeatExchangerFluidRenderer::new);
    }
    
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private final biggerreactorsrework.client.guide.GuideBookRenderer renderer = new biggerreactorsrework.client.guide.GuideBookRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, biggerreactorsrework.items.tools.GuideBookItem.INSTANCE);
        for (var info : Registry.FLUID_CLIENT_INFO) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return heated()?biggerreactorsrework.client.HeatedFluidSprites.fallback(false):info.stillTexture();
                }
                private boolean heated() {
                    return info.type()==biggerreactorsrework.fluids.HeatedCoolant.INSTANCE.getFluidType();
                }
                @Override public ResourceLocation getStillTexture(net.minecraft.world.level.material.FluidState state, net.minecraft.world.level.BlockAndTintGetter getter, net.minecraft.core.BlockPos pos) {
                    return heated()?biggerreactorsrework.client.HeatedFluidSprites.texture(biggerreactorsrework.client.HeatedFluidSprites.placedSource(pos),false):getStillTexture();
                }
                @Override public ResourceLocation getFlowingTexture(net.minecraft.world.level.material.FluidState state, net.minecraft.world.level.BlockAndTintGetter getter, net.minecraft.core.BlockPos pos) {
                    return heated()?biggerreactorsrework.client.HeatedFluidSprites.texture(biggerreactorsrework.client.HeatedFluidSprites.placedSource(pos),true):getFlowingTexture();
                }
                @Override public ResourceLocation getStillTexture(net.neoforged.neoforge.fluids.FluidStack stack) {
                    return stack.getFluid()==biggerreactorsrework.fluids.HeatedCoolant.INSTANCE?biggerreactorsrework.client.HeatedFluidSprites.texture(stack,false):getStillTexture();
                }
                @Override public ResourceLocation getFlowingTexture(net.neoforged.neoforge.fluids.FluidStack stack) {
                    return stack.getFluid()==biggerreactorsrework.fluids.HeatedCoolant.INSTANCE?biggerreactorsrework.client.HeatedFluidSprites.texture(stack,true):getFlowingTexture();
                }
                
                @Override
                public ResourceLocation getFlowingTexture() {
                    return heated()?biggerreactorsrework.client.HeatedFluidSprites.fallback(true):info.flowingTexture();
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
        event.register(biggerreactorsrework.multiblocks.turbine.containers.TurbineRedstonePortContainer.INSTANCE,
                biggerreactorsrework.multiblocks.turbine.client.TurbineRedstonePortScreen::new);
        event.register(TurbineFluidPortContainer.INSTANCE, TurbineFluidPortScreen::new);
        event.register(HeatExchangerTerminalContainer.INSTANCE, HeatExchangerTerminalScreen::new);
        event.register(HeatExchangerFluidPortContainer.INSTANCE, HeatExchangerFluidPortScreen::new);
        event.register(IndustrialProcessorContainer.INSTANCE, IndustrialProcessorScreen::new);
        event.register(biggerreactorsrework.multiblocks.battery.containers.BatteryTerminalContainer.INSTANCE,
                biggerreactorsrework.multiblocks.battery.client.BatteryTerminalScreen::new);
        event.register(biggerreactorsrework.multiblocks.battery.containers.BatteryEnergyPortContainer.INSTANCE,
                biggerreactorsrework.multiblocks.battery.client.BatteryEnergyPortScreen::new);
        event.register(biggerreactorsrework.multiblocks.battery.containers.BatteryRedstonePortContainer.INSTANCE,
                biggerreactorsrework.multiblocks.battery.client.BatteryRedstonePortScreen::new);
    }
}
