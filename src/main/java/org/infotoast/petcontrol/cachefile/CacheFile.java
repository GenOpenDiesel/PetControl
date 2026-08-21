package org.infotoast.petcontrol.cachefile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wpisy trzymamy w mapach po UUID, a nie na jednej liscie.
 * Wczesniej kazde wyszukanie zwierzaka (a robimy je przy kazdym wczytaniu chunka,
 * kazdym siadnieciu i kazdym obrazeniu) przechodzilo liniowo przez cala liste.
 */
public class CacheFile {
    private final Map<UUID, TamedAnimalEntry> tamed = new LinkedHashMap<>();
    private final Map<UUID, RoamingAnimalEntry> roaming = new LinkedHashMap<>();

    public void addAnimal(CacheFileEntry animal) {
        if (animal instanceof TamedAnimalEntry tamedEntry) {
            tamed.put(tamedEntry.getUUID(), tamedEntry);
        } else if (animal instanceof RoamingAnimalEntry roamingEntry) {
            roaming.put(roamingEntry.getUUID(), roamingEntry);
        }
    }

    public TamedAnimalEntry getTamed(UUID uuid) {
        return tamed.get(uuid);
    }

    public RoamingAnimalEntry getRoaming(UUID uuid) {
        return roaming.get(uuid);
    }

    public void removeByUUID(UUID uuid, EntryType entryType) {
        if (entryType == EntryType.TAMED) {
            tamed.remove(uuid);
        } else {
            roaming.remove(uuid);
        }
    }

    /**
     * Wpisom mozna podmienic UUID juz po dodaniu (dzieje sie tak, gdy zwierzak w trybie
     * wedrowania jest odtwarzany po wczytaniu chunka), wiec klucze trzeba wtedy przeliczyc.
     */
    public void rebuildIndex() {
        Collection<TamedAnimalEntry> tamedEntries = new ArrayList<>(tamed.values());
        Collection<RoamingAnimalEntry> roamingEntries = new ArrayList<>(roaming.values());
        tamed.clear();
        roaming.clear();
        for (TamedAnimalEntry entry : tamedEntries) {
            tamed.put(entry.getUUID(), entry);
        }
        for (RoamingAnimalEntry entry : roamingEntries) {
            roaming.put(entry.getUUID(), entry);
        }
    }

    public int size() {
        return tamed.size() + roaming.size();
    }

    public byte[] getBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x2);
        for (RoamingAnimalEntry entry : roaming.values()) {
            byte[] b = entry.getAsBytes();
            out.write(b, 0, b.length);
        }
        for (TamedAnimalEntry entry : tamed.values()) {
            byte[] b = entry.getAsBytes();
            out.write(b, 0, b.length);
        }
        return out.toByteArray();
    }
}
