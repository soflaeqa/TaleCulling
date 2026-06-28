package moe.taleculling.taleculling.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import moe.taleculling.taleculling.CullingPlugin;
import moe.taleculling.taleculling.HiddenTileRegistry;
import moe.taleculling.taleculling.PlayerChunkTracker;
import moe.taleculling.taleculling.adapter.IAdapter;
import moe.taleculling.taleculling.util.LocationUtilities;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class ChunkPacketListener extends PacketAdapter {

	private final HiddenTileRegistry hiddenTileRegistry;
	private final IAdapter adapter;
	private final PlayerChunkTracker playerChunkTracker;

	public ChunkPacketListener(CullingPlugin plugin, HiddenTileRegistry hiddenTileRegistry, IAdapter adapter, PlayerChunkTracker playerChunkTracker) {
		super(plugin, ListenerPriority.HIGHEST, Arrays.asList(PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.UNLOAD_CHUNK), ListenerOptions.ASYNC);
		this.hiddenTileRegistry = hiddenTileRegistry;
		this.plugin = plugin;
		this.adapter = adapter;
		this.playerChunkTracker = playerChunkTracker;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		PacketContainer packet = event.getPacket();

		int chunkX = packet.getIntegers().read(0);
		int chunkZ = packet.getIntegers().read(1);
		long chunkKey = LocationUtilities.getChunkKey(chunkX, chunkZ);
		if (packet.getType() == PacketType.Play.Server.MAP_CHUNK) {
			adapter.transformPacket(player, packet, hiddenTileRegistry::shouldHide);
			playerChunkTracker.trackChunk(player, chunkKey);
		} else if (packet.getType() == PacketType.Play.Server.UNLOAD_CHUNK) {
			playerChunkTracker.untrackChunk(player, chunkKey);
		}
	}

}
