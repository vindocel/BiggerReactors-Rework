package biggerreactorsrework.gametest;

import biggerreactorsrework.advancement.ReworkAdvancements;
import biggerreactorsrework.multiblocks.turbine.TurbineRotorHazard;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.joml.Vector4i;

/** Added 2026-11-01: loaded native advancements, actual damage/immunities and genuine criterion dispatch. */
@GameTestHolder(biggerreactorsrework.BiggerReactors.modid)
@PrefixGameTestTemplate(false)
public final class AdvancementGameTests {
    private static net.minecraft.advancements.AdvancementHolder advancement(GameTestHelper h,String name) {
        return h.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("biggerreactorsrework","progression/"+name));
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"RotorAudit"),ClientInformation.createDefault());
        p.connection=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"NoNetwork")).connection;
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.8,2,3.5)));
        // A freshly created ServerPlayer starts with spawn protection, unlike one that has already been in the world.
        net.neoforged.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,p,0,"spawnInvulnerableTime");
        return p;
    }
    private static TurbineRotorHazard hazard(GameTestHelper h) {
        var hazard=new TurbineRotorHazard();
        hazard.rebuild(h.absolutePos(new BlockPos(3,1,3)),new Vec3i(0,1,0),List.of(new Vector4i(1,1,1,1),new Vector4i(0,0,0,0)));
        return hazard;
    }
    @GameTest(template="empty_7x7x7") public static void progressionLoadsWithNoRewardsAndHiddenAccident(GameTestHelper h) {
        for(String name:List.of("root","guide","reactor_formed","reactor_power","liquid_cooling","heat_exchange","turbine_power","battery_formed","cyanite_reprocessed","turbine_accident")) {
            var holder=advancement(h,name); h.assertTrue(holder!=null,"missing advancement "+name);
            h.assertTrue(holder.value().rewards().equals(net.minecraft.advancements.AdvancementRewards.EMPTY),"advancement grants a reward: "+name);
        }
        h.assertTrue(advancement(h,"turbine_accident").value().display().orElseThrow().isHidden(),"secret is visible before completion");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7") public static void bladeSweepExcludesCornersAndOtherLevelsForEveryAxis(GameTestHelper h) {
        for(var axis:net.minecraft.core.Direction.Axis.values()) {
            var sweep=new TurbineRotorHazard.Sweep(net.minecraft.world.phys.Vec3.ZERO,axis,2.5);
            h.assertTrue(sweep.intersects(new net.minecraft.world.phys.AABB(-.1,-.1,-.1,.1,.1,.1)),"sweep missed center");
            h.assertTrue(!sweep.intersects(new net.minecraft.world.phys.AABB(2,2,2,2.2,2.2,2.2)),"sweep hit corner outside disc");
            var outside=switch(axis){case X->new net.minecraft.world.phys.AABB(1,-.1,-.1,1.2,.1,.1);case Y->new net.minecraft.world.phys.AABB(-.1,1,-.1,.1,1.2,.1);case Z->new net.minecraft.world.phys.AABB(-.1,-.1,1,.1,.1,1.2);};
            h.assertTrue(!sweep.intersects(outside),"sweep hit another axial level");
        }
        h.succeed();
    }
    @GameTest(template="empty_7x7x7") public static void machineCriteriaAreSpecificAndDoNotGrantRewards(GameTestHelper h) {
        var p=player(h);int xp=p.totalExperience;
        ReworkAdvancements.award(p,"reactor_formed");
        h.assertTrue(p.getAdvancements().getOrStartProgress(advancement(h,"reactor_formed")).isDone(),"native event not awarded");
        h.assertTrue(!p.getAdvancements().getOrStartProgress(advancement(h,"reactor_power")).isDone(),"assembly awarded generation");
        h.assertTrue(!p.getAdvancements().getOrStartProgress(advancement(h,"turbine_accident")).isDone(),"ordinary event revealed secret");
        h.assertTrue(p.totalExperience==xp,"milestone gave XP");h.succeed();
    }
    @GameTest(template="empty_7x7x7") public static void rotorHitsArmorButNotStoppedCreativeSpectatorOrEmptyLevels(GameTestHelper h) {
        var hazard=hazard(h); var p=player(h);
        h.assertTrue(!hazard.hit(h.getLevel(),p,0)&&p.getHealth()==20,"stationary rotor hurt");
        p.setGameMode(GameType.CREATIVE);h.assertTrue(!hazard.hit(h.getLevel(),p,2200)&&p.getHealth()==20,"creative immunity bypassed");
        p.setGameMode(GameType.SPECTATOR);h.assertTrue(!hazard.hit(h.getLevel(),p,2200),"spectator hurt");
        p.setGameMode(GameType.SURVIVAL);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.8,3.5,3.5)));
        h.assertTrue(!hazard.hit(h.getLevel(),p,2200),"shaft-only level hurt");
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(3.8,2,3.5)));
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        p.setNoGravity(true);p.doTick();
        h.assertTrue(p.getArmorValue()==8,"equipped chestplate attributes were not applied");
        h.assertTrue(hazard.hit(h.getLevel(),p,2200)&&p.getHealth()>10&&p.getHealth()<20,"normal armor did not mitigate damage");
        hazard.clear();var safe=player(h);h.assertTrue(!hazard.hit(h.getLevel(),safe,2200)&&safe.getHealth()==20,"disassembled rotor retained hazard");
        h.succeed();
    }
    @GameTest(template="empty_7x7x7") public static void onlyActualRotorDeathUnlocksSecretAndTotemPreventsIt(GameTestHelper h) {
        var hazard=hazard(h);var p=player(h);p.setHealth(1);
        p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        hazard.hit(h.getLevel(),p,2200);
        h.assertTrue(p.isAlive()&&!p.getAdvancements().getOrStartProgress(advancement(h,"turbine_accident")).isDone(),"totem unlocked death milestone");
        var victim=player(h);victim.setHealth(1);hazard.hit(h.getLevel(),victim,2200);
        h.assertTrue(victim.isDeadOrDying()&&victim.getAdvancements().getOrStartProgress(advancement(h,"turbine_accident")).isDone(),"rotor death did not unlock secret");
        var other=player(h);other.setHealth(1);other.hurt(other.damageSources().generic(),20);
        h.assertTrue(!other.getAdvancements().getOrStartProgress(advancement(h,"turbine_accident")).isDone(),"unrelated death unlocked secret");
        h.succeed();
    }
}
