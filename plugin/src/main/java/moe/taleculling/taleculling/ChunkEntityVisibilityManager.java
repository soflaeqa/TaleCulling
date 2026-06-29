package moe.taleculling.taleculling;

import moe.taleculling.taleculling.adapter.IAdapter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

public class ChunkEntityVisibilityManager {
    private static final double RayStyep = 0.25D;

    private final IAdapter adapter;
    private final VisibilityCache visibilityCache;

    private final SettingsHolder settings;
    private final HiddenEntityRegistry hiddenEntityRegistry;
    private final PlayerChunkTracker playerTracker;

    public ChunkEntityVisibilityManager(SettingsHolder settings, IAdapter adapter, PlayerChunkTracker playerTracker, VisibilityCache visibilityCache, HiddenEntityRegistry hiddenEntityRegistry) {
        this.settings = settings;
        this.adapter = adapter;
        this.playerTracker = playerTracker;
        this.visibilityCache = visibilityCache;
        this.hiddenEntityRegistry = hiddenEntityRegistry;
    }

    public void updateVisibility(Player player) {
        World world = player.getWorld();

        long[] trackedChunks = playerTracker.getTrackedChunks(player);

        if (trackedChunks == null) {
            return;
        }

        for (long chunkKey : trackedChunks) {
            int chunkX = (int) chunkKey;
            int chunkZ = (int) (chunkKey >> 32);

            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            updateChunkEntities(player, chunk);
        }
    }

    private void updateChunkEntities(Player player, Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (!shouldCull(entity, player)) {
                continue;
            }

            if (!adapter.isEntityTrackedByPlayer(player, entity)) {
                continue;
            }

            boolean canSee = canSeeEntity(player, entity);
            boolean hidden = visibilityCache.isEntityHidden(player, entity.getEntityId());

            if (hidden && canSee) {
                visibilityCache.setEntityHidden(player, entity.getEntityId(), false);
                adapter.showEntity(player, entity);
            } else if (!hidden && !canSee) {
                visibilityCache.setEntityHidden(player, entity.getEntityId(), true);
                adapter.hideEntity(player, entity);
            }
        }
    }

    private boolean canSeeEntity(Player player, Entity entity) {
        Location eye = player.getEyeLocation();
        Location entityLocation = entity.getLocation();

        if (!eye.getWorld().equals(entityLocation.getWorld())) {
            return false;
        }

        double x = entityLocation.getX();
        double y = entityLocation.getY();
        double z = entityLocation.getZ();

        double height = Math.max(entity.getHeight(), 1.8D);

        // dots logic
        return hasClearLine(eye, new Vector(x, y + 0.15D, z))
                || hasClearLine(eye, new Vector(x, y + height * 0.5D, z))
                || hasClearLine(eye, new Vector(x, y + height * 0.95D, z))
                || hasClearLine(eye, new Vector(x + 0.35D, y + height * 0.5D, z))
                || hasClearLine(eye, new Vector(x - 0.35D, y + height * 0.5D, z))
                || hasClearLine(eye, new Vector(x, y + height * 0.5D, z + 0.35D))
                || hasClearLine(eye, new Vector(x, y + height * 0.5D, z - 0.35D));
    }

    private boolean hasClearLine(Location from, Vector target) {
        World world = from.getWorld();

        Vector start = from.toVector();
        Vector direction = target.clone().subtract(start);

        double distance = direction.length();

        if (distance <= 0.01D) {
            return true;
        }

        direction.normalize();

        double traveled = 0.0D;

        while (traveled < distance) {
            Vector current = start.clone().add(direction.clone().multiply(traveled));

            Block block = world.getBlockAt(current.getBlockX(), current.getBlockY(), current.getBlockZ());

            if (isBlocking(block.getType())) {
                return false;
            }

            traveled += RayStyep;
        }

        return true;
    }

    private boolean isBlocking(Material material) {
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }

        return CullingPlugin.isOccluding(material);
    }

    private boolean shouldCull(Entity entity, Player viewer) {
        if (entity == viewer) {
            return false;
        }

        if (entity instanceof Player) {
            return hiddenEntityRegistry.shouldHide(entity);
        }

        if (entity instanceof ArmorStand) {
            return hiddenEntityRegistry.shouldHide(entity);
        }

        if (hiddenEntityRegistry.shouldHide(entity)) {
            return true;
        }

        return settings.isHideLivingEntities() && entity instanceof LivingEntity;
    }


    public void restoreVisibility(Player player) {
        World world = player.getWorld();

        long[] trackedChunks = playerTracker.getTrackedChunks(player);

        if (trackedChunks == null) {
            return;
        }

        for (long chunkKey : trackedChunks) {
            int chunkX = (int) chunkKey;
            int chunkZ = (int) (chunkKey >> 32);

            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ);

            for (Entity entity : chunk.getEntities()) {
                if (!visibilityCache.isEntityHidden(player, entity.getEntityId())) {
                    continue;
                }

                visibilityCache.setEntityHidden(player, entity.getEntityId(), false);
                adapter.showEntity(player, entity);
            }
        }
    }
}