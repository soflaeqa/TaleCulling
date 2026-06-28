package moe.taleculling.taleculling.adapter;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.function.Function;

public interface IAdapter {

	void hideTile(Player player, Location location, BlockData blockData);

	void showTile(Player player, Location location, BlockState block);

    void transformTilePacket(Player player, PacketContainer container, Function<String, Boolean> tileEntityTypeFilter);

    void hideEntity(Player player, org.bukkit.entity.Entity entity);

    void showEntity(Player player, org.bukkit.entity.Entity entity);

    boolean isEntityTrackedByPlayer(Player player, Entity entity);

}
