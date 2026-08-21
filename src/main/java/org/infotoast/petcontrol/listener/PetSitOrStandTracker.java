package org.infotoast.petcontrol.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.infotoast.petcontrol.PetControl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Pilnuje, czy oswojony zwierzak siadl lub wstal, i wysyla o tym PetSitOrStandEvent.
 *
 * Wczesniej ta klasa co 3 ticki przelatywala przez WSZYSTKIE encje we wszystkich swiatach
 * (razem z budowaniem nowej listy przez stream) - przy kilku tysiacach mobow to byly dziesiatki
 * tysiecy iteracji na sekunde na glownym watku. Teraz utrzymujemy rejestr samych oswajalnych
 * zwierzat, aktualizowany eventami wejscia/wyjscia encji ze swiata, i sprawdzamy tylko je.
 */
public class PetSitOrStandTracker implements Listener {
    // Stan siadania zmienia sie rzadko i nie ma na to wlasnego eventu, wiec dalej odpytujemy -
    // ale 2 razy na sekunde i tylko po zwierzakach, a nie po calym swiecie.
    private static final long CHECK_INTERVAL_TICKS = 10L;

    private final PetControl plugin;
    private final Map<UUID, Entity> trackedPets = new HashMap<>();
    private final Map<UUID, Boolean> lastState = new HashMap<>();
    private BukkitTask task;

    public PetSitOrStandTracker(PetControl plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Przy /reload czesc encji jest juz w swiecie - zbieramy je raz, na starcie.
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                trackIfPet(entity);
            }
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
        trackedPets.clear();
        lastState.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        trackIfPet(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        // Bez tego mapa stanu rosla w nieskonczonosc - kazdy zabity zwierzak zostawal w niej do restartu.
        UUID uuid = event.getEntity().getUniqueId();
        trackedPets.remove(uuid);
        lastState.remove(uuid);
    }

    private void trackIfPet(Entity entity) {
        if (asTamable(entity) != null) {
            trackedPets.put(entity.getUniqueId(), entity);
        }
    }

    private TamableAnimal asTamable(Entity entity) {
        if (!(entity instanceof CraftEntity craftEntity)) {
            return null;
        }
        return (craftEntity.getHandleRaw() instanceof TamableAnimal tamable) ? tamable : null;
    }

    public void tick() {
        Iterator<Map.Entry<UUID, Entity>> iterator = trackedPets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Entity> tracked = iterator.next();
            Entity entity = tracked.getValue();
            TamableAnimal tameable = asTamable(entity);
            if (tameable == null || !entity.isValid()) {
                iterator.remove();
                lastState.remove(tracked.getKey());
                continue;
            }
            if (!tameable.isTame()) {
                continue;
            }

            UUID uuid = tracked.getKey();
            boolean current = tameable.isOrderedToSit();
            Boolean previous = lastState.put(uuid, current);
            if (previous != null && previous == current) {
                continue;
            }
            if (previous == null && !current) {
                // Pierwsze spotkanie ze stojacym zwierzakiem to nie jest zmiana stanu.
                continue;
            }

            UUID ownerUUID = (tameable.getOwnerReference() != null) ? tameable.getOwnerReference().getUUID() : null;
            if (ownerUUID == null) {
                plugin.getLogger().warning("Failed to find owner for tamed animal with UUID: " + uuid);
                continue;
            }

            plugin.getServer().getPluginManager().callEvent(new PetSitOrStandEvent(entity, ownerUUID, current));
        }
    }
}
