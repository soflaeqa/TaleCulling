package moe.taleculling.taleculling;

import com.comphenix.protocol.ProtocolLibrary;
import moe.taleculling.taleculling.adapter.IAdapter;
import moe.taleculling.taleculling.protocol.ChunkPacketListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.Objects;

public class CullingPlugin extends JavaPlugin {

    private SettingsHolder settings;
    private HiddenTileRegistry hiddenTileRegistry;
    private HiddenEntityRegistry hiddenEntityRegistry;

    private IAdapter adapter;
    private ChunkTileVisibilityManager chunkTileVisibilityManager;
    private ChunkEntityVisibilityManager chunkEntityVisibilityManager;
    private PlayerChunkTracker playerChunkTracker;
    private ChunkCache chunkCache;
    private VisibilityCache visibilityCache;
    private ChunkPacketListener chunkPacketListener;

    private VisibilityUpdateThread visibilityUpdateThread;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        settings = new SettingsHolder();
        settings.load(getConfig().getConfigurationSection("settings"));
        hiddenTileRegistry = new HiddenTileRegistry(getLogger());
        hiddenTileRegistry.load(getConfig().getConfigurationSection("hiddenTiles"));
        hiddenEntityRegistry = new HiddenEntityRegistry(getLogger());
        hiddenEntityRegistry.load(getConfig().getConfigurationSection("hiddenEntities"));

        try {
            String nmsVersion = getServer().getClass().getName().split("\\.")[3].substring(1);
            getLogger().info("Loading NMS adapter: " + nmsVersion);
            //noinspection unchecked
            Class<? extends IAdapter> adapterClass = (Class<? extends IAdapter>) Class.forName("moe.taleculling.taleculling.adapter.Adapter_" + nmsVersion);
            Objects.requireNonNull(adapterClass, "Missing NMS adapter for this MC version!");
            Constructor<? extends IAdapter> adapterConstructor = adapterClass.getConstructor();
            Objects.requireNonNull(adapterConstructor, "Invalid NMS adapter: missing constructor!");
            adapter = adapterConstructor.newInstance();
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to setup the NMS adapter!", t);
        }

        playerChunkTracker = new PlayerChunkTracker(this);
        visibilityCache = new VisibilityCache();
        chunkCache = new ChunkCache(this, hiddenTileRegistry);
        chunkTileVisibilityManager = new ChunkTileVisibilityManager(adapter, playerChunkTracker, visibilityCache, chunkCache);
        chunkEntityVisibilityManager = new ChunkEntityVisibilityManager(settings, adapter, playerChunkTracker, visibilityCache, hiddenEntityRegistry);

        getServer().getPluginManager().registerEvents(playerChunkTracker, this);
        getServer().getPluginManager().registerEvents(chunkCache, this);
        getServer().getPluginManager().registerEvents(visibilityCache, this);

        chunkPacketListener = new ChunkPacketListener(this, hiddenTileRegistry, adapter, playerChunkTracker);
        ProtocolLibrary.getProtocolManager().addPacketListener(chunkPacketListener);

        visibilityUpdateThread = new VisibilityUpdateThread(chunkTileVisibilityManager, chunkEntityVisibilityManager);
        visibilityUpdateThread.start();
    }

    @Override
    public void onDisable() {
        // Remove packet listeners
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);

        // Stop update task
        if (visibilityUpdateThread != null) {
            visibilityUpdateThread.shutdown();
        }

        // Restore visibility
        for (Player player : getServer().getOnlinePlayers()) {
            if (chunkTileVisibilityManager != null) {
                chunkTileVisibilityManager.restoreVisibility(player);
            }

            if (chunkEntityVisibilityManager != null) {
                chunkEntityVisibilityManager.restoreVisibility(player);
            }
        }
    }

    // TODO: registry
    public static boolean isOccluding(Material material) {
        switch (material) {
            case BARRIER:
            case SPAWNER:
                return false;
        }
        return material.isOccluding();
    }
}
