package moe.taleculling.taleculling;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class HiddenEntityRegistry {

    private final Logger logger;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

    private EntityType[] hiddenTypes = new EntityType[0];

    public HiddenEntityRegistry(Logger logger) {
        this.logger = logger;
    }

    public void load(ConfigurationSection config) {
        Set<EntityType> types = new HashSet<>();

        if (config == null) {
            logger.warning("Missing hiddenEntities config section!");
            load(types);
            return;
        }

        for (String typeName : config.getStringList("types")) {
            EntityType type;

            try {
                type = EntityType.valueOf(typeName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logger.warning("Entity type " + typeName + " is invalid!");
                continue;
            }

            types.add(type);
        }

        load(types);
    }

    public void load(Collection<EntityType> types) {
        try {
            writeLock.lock();
            hiddenTypes = types.toArray(new EntityType[0]);
        } finally {
            writeLock.unlock();
        }

        logger.info("Loaded " + types.size() + " hidden entity types");
    }

    public boolean shouldHide(Entity entity) {
        return shouldHide(entity.getType());
    }

    public boolean shouldHide(EntityType type) {
        try {
            readLock.lock();

            for (EntityType current : hiddenTypes) {
                if (current == type) {
                    return true;
                }
            }

            return false;
        } finally {
            readLock.unlock();
        }
    }
}