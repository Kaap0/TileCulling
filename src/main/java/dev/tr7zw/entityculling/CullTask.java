package dev.tr7zw.entityculling;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.level.Level;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;

//import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
//import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
//import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
//import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
//import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.datafixers.util.Pair;

import dev.tr7zw.entityculling.occlusionculling.OcclusionCullingInstance;
import dev.tr7zw.entityculling.occlusionculling.BlockChangeListener.ChunkCoords;


//import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.world.entity.EntityLiving;


import net.minecraft.server.level.EntityPlayer;


import net.minecraft.world.entity.EnumItemSlot;


import net.minecraft.world.item.ItemStack;


import net.minecraft.network.protocol.Packet;


import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;


import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;

import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
//mport net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata;


// import net.minecraft.server.v1_16_R3.PacketPlayOutSpawnEntity;
// import net.minecraft.server.v1_16_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;


//import net.minecraft.server.v1_16_R3.PlayerChunkMap.EntityTracker;
//import net.minecraft.server.level.ChunkMap.TrackedEntity;
import net.minecraft.server.level.PlayerChunkMap.EntityTracker;


// import net.minecraft.server.v1_16_R3.WorldServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Todo: cleanup this mess
 *
 * @author tr7zw
 */
public class CullTask implements Runnable {

    private CullingPlugin instance;
    private int counter = 0;
//    private AxisAlignedBB blockAABB = new AxisAlignedBB(0d, 0d, 0d, 1d, 1d, 1d);
    private AxisAlignedBB entityAABB = new AxisAlignedBB(0d, 0d, 0d, 1d, 2d, 1d);
    private OcclusionCullingInstance culling = new OcclusionCullingInstance();

    public CullTask(CullingPlugin pl) {
        instance = pl;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        counter++;
        Set<ChunkCoords> entityUpdateChunks = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            culling.resetCache();
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    Location loc = player.getLocation().add(x * 16, 0, y * 16);
                    ChunkCoords coods = instance.blockChangeListener.getChunkCoords(loc);
                    if (counter >= 20) {
                        entityUpdateChunks.add(coods);
                    }
                    if (instance.blockChangeListener.isInLoadedChunk(coods)) {


                        //DROPPING SUPPORT FOR BLOCKS :)

                        // ChunkSnapshot chunkSnapshot = instance.blockChangeListener.getChunk(loc);
//                        BlockState[] tiles = instance.blockChangeListener.getChunkTiles(coods);
//                        if (tiles != null) {
//                            for (BlockState block : tiles) {
//                                //if (block.getType() == Material.CHEST) {
//                                boolean canSee = culling.isAABBVisible(block.getLocation(), blockAABB,
//                                        player.getEyeLocation(), false);
//                                boolean hidden = instance.cache.isHidden(player, block.getLocation());
//                                if (hidden && canSee) {
//                                    instance.cache.setHidden(player, block.getLocation(), false);
//                                    player.sendBlockChange(block.getLocation(), block.getBlockData());
//                                } else if (!hidden && !canSee) {
//                                    instance.cache.setHidden(player, block.getLocation(), true);
//                                    player.sendBlockChange(block.getLocation(), Material.BARRIER, (byte) 0);
//                                }
//                                //}
//                            }
//                        }


                        Entity[] entities = instance.blockChangeListener.getChunkEntities(coods);


                        World world = player.getWorld();
                        CraftWorld craftWorld = (CraftWorld) world;
                        WorldServer worldServer = craftWorld.getHandle();

                        Int2ObjectMap<EntityTracker> trackers = worldServer.k().a.K;


//                        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
                        if (entities != null && trackers != null) {
                            for (Entity entity : entities) {
                                EntityTracker tracker = trackers.get(entity.getEntityId());
                                if (tracker == null) {
                                    continue;
                                }
//                                if(!tracker.f.contains(nmsPlayer)) { //TODO OLI if(!tracker.trackedPlayers.contains(nmsPlayer))
//                                    continue;
//                                }


                                //Entity Filtering

                                if (instance.config.getList("always-visible-entities").contains(entity.getType().toString())) {
                                    continue;
                                }


                                boolean canSee = culling.isAABBVisible(entity.getLocation(), entityAABB,
                                        player.getEyeLocation(), true);
                                boolean hidden = instance.cache.isHidden(player, entity);
                                if (hidden && canSee) {
                                    instance.cache.setHidden(player, entity, false);
                                    //  Bukkit.broadcastMessage("Unhiding entity "+entity.getName()+" from player "+player.getName()); //TODO DEBUG
                                    if (entity instanceof Player) {
                                        // Do nothing!
                                    } else if (entity instanceof LivingEntity) {
                                        PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity(
                                                (EntityLiving) ((CraftEntity) entity).getHandle());
                                        sendPacket(player, PacketType.Play.Server.SPAWN_ENTITY, packet);
                                        List<Pair<EnumItemSlot, ItemStack>> armor = new ArrayList<>();
                                        for (EnumItemSlot slot : EnumItemSlot.values()) {
                                            armor.add(Pair.of(slot, ((EntityLiving) ((CraftEntity) entity).getHandle()).c(slot)));
                                        }
                                        PacketPlayOutEntityEquipment equip = new PacketPlayOutEntityEquipment(entity.getEntityId(), armor);
                                        sendPacket(player, PacketType.Play.Server.ENTITY_EQUIPMENT, equip);
                                    } else {
                                        PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity(
                                                ((CraftEntity) entity).getHandle());
                                        sendPacket(player, PacketType.Play.Server.SPAWN_ENTITY, packet);
                                    }
                                    PacketPlayOutEntityMetadata metaPacket = new PacketPlayOutEntityMetadata(entity.getEntityId(), ((CraftEntity) entity).getHandle().aj().c()); //TODO ?
                                    sendPacket(player, PacketType.Play.Server.ENTITY_METADATA, metaPacket);
                                } else if (!hidden && !canSee) { // hide entity
                                    if (!(entity instanceof Player) && !(entity instanceof ExperienceOrb) && !(entity instanceof Painting)) {
                                        instance.cache.setHidden(player, entity, true);
                                        //  Bukkit.broadcastMessage("Hiding entity "+entity.getName()+" from player "+player.getName()); //TODO DEBUG
                                        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(
                                                entity.getEntityId());
                                        sendPacket(player, PacketType.Play.Server.ENTITY_DESTROY, packet);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (counter >= 20) { // Pesky entities are able to move, so we need to update these chunks entity data every now and then
            counter = 0;
            CullingPlugin.instance.blockChangeListener.updateCachedChunkEntitiesSync(entityUpdateChunks);
        }
        //Bukkit.broadcastMessage("Time: " + (System.currentTimeMillis() - start)); //TODO DEBUG
    }

    private void sendPacket(Player player, PacketType type, Packet<?> packet) {
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, new PacketContainer(type, packet));
    }

}
