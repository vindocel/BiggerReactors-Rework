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
import net.minecraft.resources.Identifier;
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
                                           java.util.Map<String, String> previous, java.util.Map<String, String> next) {
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

    static void init(IEventBus modBus) {
        modBus.addListener(biggerreactorsrework.multiblocks.turbine.client.TurbineAtmosphereEffects::registerParticles);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(biggerreactorsrework.registries.ModParticles.COOLANT_BUBBLE.get(), net.minecraft.client.particle.BubbleParticle.Provider::new);
            event.registerSpriteSet(biggerreactorsrework.registries.ModParticles.COOLANT_BUBBLE_POP.get(), net.minecraft.client.particle.BubblePopParticle.Provider::new);
            event.registerSpriteSet(biggerreactorsrework.registries.ModParticles.COOLANT_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) ->
                biggerreactorsrework.multiblocks.turbine.client.TurbineSteamRenderer.registerPipelines(event));
        // Modified 2026-10-28: positional loops and per-machine local volume.
        biggerreactorsrework.client.sound.MachineSoundManager.init();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                biggerreactorsrework.multiblocks.turbine.client.TurbineAtmosphereEffects::tick);
        modBus.addListener(biggerreactorsrework.client.HeatedFluidSprites::register);
        biggerreactorsrework.client.GuiSettingsEvents.register();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) -> {
                    biggerreactorsrework.registries.CoolantSync.clearClient();
                    biggerreactorsrework.client.calculator.CalculatorClient.disconnected();
                });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) -> event.registerPipeline(biggerreactorsrework.client.GuiTheme.DARK_PIPELINE));
        modBus.addListener(BiggerReactorsClient::registerFluidModels);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) -> {
            if (event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof biggerreactorsrework.core.modular.block.BlockTooltip block) {
                block.appendTooltip(event.getItemStack(), event.getToolTip()::add);
            }
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) ->
                event.register(biggerreactorsrework.client.guide.GuideBookRenderer.ID, biggerreactorsrework.client.guide.GuideBookRenderer.Unbaked.MAP_CODEC));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) ->
                biggerreactorsrework.client.guide.GuidePictures.register(event));
        modBus.addListener(biggerreactorsrework.compat.DarkModeCompat::onInterModEnqueue);
        biggerreactorsrework.compat.DarkModeCompat.register();
        // Added 2026-11-03: describe the Frozen Obsidian process on its bucket.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) -> {
            if (event.getItemStack().is(biggerreactorsrework.fluids.FrozenObsidian.INSTANCE.getBucket()))
                event.getToolTip().add(net.minecraft.network.chat.Component.translatable("fluid.biggerreactorsrework.frozen_obsidian.description")
                        .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
        });
        modBus.addListener(BiggerReactorsClient::registerScreens);
        modBus.addListener(BiggerReactorsClient::registerRenderers);
        modBus.addListener(BiggerReactorsClient::registerClientExtensions);
        HeatExchangerChannelModels.init(modBus);
        biggerreactorsrework.client.ConnectedGlassModels.init(modBus);
        biggerreactorsrework.client.ReactorRodModels.init(modBus);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() ->
                biggerreactorsrework.items.tools.GuideBookItem.open = biggerreactorsrework.client.guide.GuideBookAnimation::requestOpen));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(biggerreactorsrework.client.guide.GuideBookAnimation::onClientTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesReceivedEvent event) ->
                biggerreactorsrework.machine.recipe.RecipeAccess.setClientRecipes(event.getRecipeMap()));
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterStandalone event) ->
                biggerreactorsrework.client.guide.GuideBookRenderer.models().forEach(id -> biggerreactorsrework.client.guide.ModelRendering.register(event, id)));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        assert TurbineRotorBearingTile.SUPPLIER.TYPE != null;
        event.registerBlockEntityRenderer(TurbineRotorBearingTile.SUPPLIER.TYPE, TurbineRotorRenderer::new);
        event.registerBlockEntityRenderer(biggerreactorsrework.multiblocks.reactor.tiles.ReactorManifoldTile.SUPPLIER.TYPE,ReactorManifoldRenderer::new);
        event.registerBlockEntityRenderer(biggerreactorsrework.multiblocks.heatexchanger.tiles.HeatExchangerChannelTile.SUPPLIER.TYPE,biggerreactorsrework.multiblocks.heatexchanger.client.HeatExchangerFluidRenderer::new);
    }
    
    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
    }

    private static void registerFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        for (var info : Registry.FLUID_CLIENT_INFO) {
            var heated = info.type() == biggerreactorsrework.fluids.HeatedCoolant.INSTANCE.getFluidType();
            var model = new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                    new net.minecraft.client.resources.model.sprite.Material(info.stillTexture(), true),
                    new net.minecraft.client.resources.model.sprite.Material(info.flowingTexture(), true),
                    null, net.neoforged.neoforge.client.fluid.FluidTintSources.constant(info.tintColor()),
                    heated ? biggerreactorsrework.client.HeatedFluidSprites::renderPlaced : null);
            net.minecraft.core.registries.BuiltInRegistries.FLUID.stream().filter(fluid -> fluid.getFluidType() == info.type()).forEach(fluid -> event.register(model, fluid));
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
