package moe.taleculling.taleculling;

import moe.taleculling.taleculling.adapter.IAdapter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class ChunkTileVisibilityManager {

    private static final double TileRayStyep = 0.25D;

    private final IAdapter adapter;
    private final PlayerChunkTracker playerTracker;
    private final VisibilityCache visibilityCache;
    private final ChunkCache chunkCache;

    public ChunkTileVisibilityManager(IAdapter adapter, PlayerChunkTracker playerTracker, VisibilityCache visibilityCache, ChunkCache chunkCache) {
        this.adapter = adapter;
        this.playerTracker = playerTracker;
        this.visibilityCache = visibilityCache;
        this.chunkCache = chunkCache;
    }

    public void updateVisibility(Player player) {
        World world = player.getWorld();

        long[] trackedChunks = playerTracker.getTrackedChunks(player);
        if (trackedChunks == null) {
            return;
        }

        for (long chunkKey : trackedChunks) {
            List<BlockState> tiles = chunkCache.getChunkTiles(world, chunkKey);
            if (tiles == null) {
                continue;
            }

            for (BlockState block : tiles) {
                Location location = block.getLocation();

                if (visibilityCache.isBwockHidden(player, location) &&  canSeeTile(player, block)) {
                    visibilityCache.setBwockHidden(player, location, false);
                    sendRealTile(player, block);
                } else if (!visibilityCache.isBwockHidden(player, location) && ! canSeeTile(player, block)) {
                    visibilityCache.setBwockHidden(player, location, true);
                    sendHiddenTile(player, block);
                }
            }
        }
    }

    public void restoreVisibility(Player player) {
        World world = player.getWorld();

        long[] trackedChunks = playerTracker.getTrackedChunks(player);
        if (trackedChunks == null) {
            return;
        }
        for (long chunkKey : trackedChunks) {
            List<BlockState> tiles = chunkCache.getChunkTiles(world, chunkKey);
            if (tiles == null) {
                continue;
            }
            for (BlockState block : tiles) {
                Location location = block.getLocation();
                if (!visibilityCache.isBwockHidden(player, location)) {
                    continue;
                }
                visibilityCache.setBwockHidden(player, location, false);
                sendRealTile(player, block);
            }
        }
    }

    private void sendHiddenTile(Player player, BlockState block) {
        adapter.hideTile(player, block.getLocation(), null);
    }

    private void sendRealTile(Player player, BlockState block) {
        Location location = block.getLocation();

        // firstly, restore real block data
        // then if it was TileState - restore tile data
        adapter.hideTile(player, location, block.getBlockData());

        if (block instanceof TileState) {
            adapter.showTile(player, location, block);
        }
    }

    private boolean canSeeTile(Player player, BlockState blockState) {
        Location eye = player.getEyeLocation();
        Location blockLocation = blockState.getLocation();

        if (!eye.getWorld().equals(blockLocation.getWorld())) {
            return false;
        }

        int x = blockLocation.getBlockX();
        int y = blockLocation.getBlockY();
        int z = blockLocation.getBlockZ();

        return clearLineToTile(eye, new Vector(x + 0.5D, y + 0.5D, z + 0.5D), blockState)
                || clearLineToTile(eye, new Vector(x + 0.5D, y + 0.85D, z + 0.5D), blockState)
                || clearLineToTile(eye, new Vector(x + 0.5D, y + 0.15D, z + 0.5D), blockState)
                || clearLineToTile(eye, new Vector(x + 0.15D, y + 0.5D, z + 0.5D), blockState)
                || clearLineToTile(eye, new Vector(x + 0.85D, y + 0.5D, z + 0.5D), blockState)
                || clearLineToTile(eye, new Vector(x + 0.5D, y + 0.5D, z + 0.15D), blockState)
                || clearLineToTile(eye, new Vector(x + 0.5D, y + 0.5D, z + 0.85D), blockState);
    }

    private boolean clearLineToTile(Location from, Vector target, BlockState targetBlockState) {
        World world = from.getWorld();

        Vector start = from.toVector();
        Vector direction = target.clone().subtract(start);
        double distance = direction.length();

        if (distance <= 0.01D) {
            return true;
        }

        direction.normalize();

        int targetX = targetBlockState.getX();
        int targetY = targetBlockState.getY();
        int targetZ = targetBlockState.getZ();

        double traveled = 0.0D;

        while (traveled < distance) {
            Vector current = start.clone().add(direction.clone().multiply(traveled));

            // tile shouldnt block trace from themselves
            if (current.getBlockX() == targetX && current.getBlockY() == targetY && current.getBlockZ() == targetZ) {
                traveled += TileRayStyep;
                continue;
            }
            Block block = world.getBlockAt(current.getBlockX(), current.getBlockY(), current.getBlockZ());

            if (isBlocking(block.getType())) {
                return false;
            }
            traveled += TileRayStyep;
        }
        return true;
    }

    private boolean isBlocking(Material material) {
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }
        return CullingPlugin.isOccluding(material);
    }
}