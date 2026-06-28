package moe.taleculling.taleculling.occlusionculling;

import com.logisticscraft.occlusionculling.DataProvider;
import com.logisticscraft.occlusionculling.util.Vec3d;
import moe.taleculling.taleculling.ChunkCache;
import moe.taleculling.taleculling.CullingPlugin;
import moe.taleculling.taleculling.util.LocationUtilities;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;

import java.lang.reflect.Method;

public class PaperDataProvider implements DataProvider {

    private static final Method MinheightMethod = findMinHeightThingy();
    private final ChunkCache chunkCache;

    private World world;
    private ChunkSnapshot snapshot;

    public PaperDataProvider(ChunkCache chunkCache) {
        this.chunkCache = chunkCache;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        if (world == null) {
            throw new IllegalStateException("World not loaded into DataProvider!");
        }
        if (snapshot != null && chunkX == snapshot.getX() && chunkZ == snapshot.getZ()) {
            return true; // Already cached
        }

        long chunkKey = LocationUtilities.getChunkKey(chunkX, chunkZ);
        snapshot = chunkCache.getChunk(world, chunkKey);
        return snapshot != null;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        if (snapshot == null) {
            throw new IllegalStateException("Chunk not loaded into DataProvider!");
        }

        if (y < getMinHeightNya(world) || y >= world.getMaxHeight()) {
            return false;
        }

        int relativeX = x & 0xF;
        int relativeZ = z & 0xF;
        Material material = snapshot.getBlockType(relativeX, y, relativeZ);
        return CullingPlugin.isOccluding(material);
    }

    @Override
    public void cleanup() {
        snapshot = null;
    }

    @Override
    public void checkingPosition(Vec3d[] targetPoints, int size, Vec3d viewerPosition) {
    }


    // in case if there is a -64 height in 1.17+ MC versions
    private static Method findMinHeightThingy() {
        try {
            return World.class.getMethod("getMinHeight");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static int getMinHeightNya(World world) {
        if (MinheightMethod == null) {
            return 0;
        }
        try {
            Object result = MinheightMethod.invoke(world);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
