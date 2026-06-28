package moe.taleculling.taleculling;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public class VisibilityUpdateThread extends Thread {

	public static final int TASK_INTERVAL = 50;

	private final ChunkTileVisibilityManager chunkTileVisibilityManager;
    private final ChunkEntityVisibilityManager chunkEntityVisibilityManager;

	private final AtomicBoolean running = new AtomicBoolean(false);

    public VisibilityUpdateThread(ChunkTileVisibilityManager chunkTileVisibilityManager,
                                  ChunkEntityVisibilityManager entityVisibilityManager) {
        super("TaleCulling-VisibilityUpdateThread");
        this.chunkTileVisibilityManager = chunkTileVisibilityManager;
        this.chunkEntityVisibilityManager = entityVisibilityManager;
    }

	@Override
	public synchronized void start() {
		running.set(true);
		super.start();
	}

	public void shutdown() {
		running.set(false);
		interrupt();
	}

	@Override
	public void run() {
		while (running.get()) {
			long start = System.currentTimeMillis();
			for (Player player : Bukkit.getOnlinePlayers()) {
				chunkTileVisibilityManager.updateVisibility(player);
                chunkEntityVisibilityManager.updateVisibility(player);
			}
			long took = System.currentTimeMillis() - start;
			long sleep = Math.max(0, TASK_INTERVAL - took);
			if (sleep > 0) {
				try {
					//noinspection BusyWait
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

}
