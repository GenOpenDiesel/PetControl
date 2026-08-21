package org.infotoast.petcontrol.cachefile;

import org.infotoast.petcontrol.PetControl;
import org.infotoast.petcontrol.customanimals.RoamingCat;
import org.infotoast.petcontrol.customanimals.RoamingDog;
import org.infotoast.petcontrol.exception.EntityNotTamableException;

import javax.annotation.Nullable;
import java.io.*;
import java.util.UUID;

public class CacheFileManager {
    private PetControl plugin;
    private File cacheFile;
    private CacheFile data;
    // Zapisujemy tylko wtedy, gdy cokolwiek sie zmienilo - WorldSaveEvent leci raz na kazdy swiat.
    private volatile boolean dirty = false;
    private final Object writeLock = new Object();
    public CacheFileManager(PetControl plugin) {
        this.plugin = plugin;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void rebuildIndex() {
        if (data != null) {
            data.rebuildIndex();
            markDirty();
        }
    }

    public static byte[] intToBytes(int i) {
        return new byte[] {
                (byte)(i >> 24),
                (byte)(i >> 16),
                (byte)(i >> 8),
                (byte)(i),
        };
        /*byte[] bytes = new byte[Integer.BYTES+1];
        bytes[0] = (byte)((i < 0) ? 0x1 : 0x2);
        for (int j = 1; j < bytes.length; j++) {
            bytes[bytes.length-j-1] = (byte) (i & 0xFF);
            i >>= 8;
        }
        return bytes;*/
    }

    public static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF));
        /*int i = 0;
        for (int j = 1; j < bytes.length; j++) {
            i = (i << 8) + (bytes[j] & 0xFF);
        }
        if (bytes[0] == 0x1) {
            return -i;
        } else {
            return i;
        }*/
    }

    public void onStartup() {
        // First, see if cache file exists
        try {
            String fileName = plugin.getDataFolder().getPath() + File.separator + "cache.dat";
            this.cacheFile = new File(fileName);
            if (this.cacheFile.exists() && !(this.cacheFile.length() == 0)) {
                CacheFileLoader loader = new CacheFileLoader(this.cacheFile);
                try {
                    this.data = loader.getFromFile();
                } catch (RuntimeException e) {
                    // Keep the unreadable file around instead of overwriting it on shutdown,
                    // so the pet data can still be recovered.
                    File backup = new File(fileName + ".broken-" + System.currentTimeMillis());
                    this.cacheFile.renameTo(backup);
                    this.data = new CacheFile();
                    PetControl.logger.severe("Could not read cache.dat (" + e + "). It has been moved to "
                            + backup.getName() + " and a fresh cache was started.");
                }
            } else {
                plugin.getDataFolder().mkdir();
                this.cacheFile.createNewFile();
                this.data = new CacheFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.data == null) {
            this.data = new CacheFile();
        }
    }

    private void writeFile() {
        if (this.cacheFile == null || this.data == null) {
            return;
        }
        writeBytes(data.getBytes());
    }

    private void writeBytes(byte[] bytes) {
        synchronized (writeLock) {
            try (FileOutputStream fos = new FileOutputStream(this.cacheFile, false)) {
                fos.write(bytes);
            } catch (IOException e) {
                PetControl.logger.severe("Nie udalo sie zapisac cache.dat: " + e);
            }
        }
    }

    public void addRoamingCat(RoamingCat cat) {
        RoamingAnimalEntry entry = new RoamingAnimalEntry(AnimalType.CAT,
                cat.getRoamingRadius(), cat.getRoamingCenterX(), cat.getRoamingCenterZ(), cat.getUUID(), cat.isGuarded(), cat.isOrderedToSit());

        data.addAnimal(entry);
        markDirty();
    }

    public void addRoamingDog(RoamingDog dog) {
        RoamingAnimalEntry entry = new RoamingAnimalEntry(AnimalType.DOG,
                dog.getRoamingRadius(), dog.getRoamingCenterX(), dog.getRoamingCenterZ(), dog.getUUID(), dog.isGuarded(), dog.isOrderedToSit());

        data.addAnimal(entry);
        markDirty();
    }

    public void removeByUUID(UUID uuid, EntryType entType) {
        data.removeByUUID(uuid, entType);
        markDirty();
    }

    public AnimalType getAnimalTypeFromEntity(org.bukkit.entity.Entity entity) {
        if (entity instanceof org.bukkit.entity.Cat) {
            return AnimalType.CAT;
        } else if (entity instanceof org.bukkit.entity.Wolf) {
            return AnimalType.DOG;
        } else if (entity instanceof org.bukkit.entity.Parrot) {
            return AnimalType.PARROT;
        } else if (entity instanceof org.bukkit.entity.Nautilus) {
            return AnimalType.NAUTILUS;
        } else if (entity instanceof org.bukkit.entity.ZombieNautilus) {
            return AnimalType.ZOMBIE_NAUTILUS;
        } else {
            throw new EntityNotTamableException("Entity is not a tamable animal. Please report this error to the developers.");
        }
    }

    public RoamingAnimalEntry getRoamingAnimalFromUUID(UUID uuid) {
        return data.getRoaming(uuid);
    }

    public TamedAnimalEntry getTamedAnimalFromUUID(UUID uuid) {
        return data.getTamed(uuid);
    }

    public void addTamedAnimalEntry(TamedAnimalEntry entry) {
        data.addAnimal(entry);
        markDirty();
    }

    public void addRoamingAnimalEntry(RoamingAnimalEntry entry) {
        data.addAnimal(entry);
        markDirty();
    }

    public void onShutdown() {
        // Przy wylaczaniu zapisujemy synchronicznie - scheduler juz nie wystartuje zadania.
        dirty = false;
        writeFile();
    }

    public void saveCache() {
        writeFile();
        dirty = false;
    }

    /**
     * Wolane przy zapisie swiata. Serializacja idzie na glownym watku (dane musza byc spojne),
     * ale samo pisanie po dysku laduje na watek poboczny, zeby nie zacinac tickow.
     */
    public void onAutoSave() {
        if (!dirty || this.cacheFile == null || this.data == null) {
            return;
        }
        dirty = false;
        byte[] bytes = data.getBytes();
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeBytes(bytes));
        } else {
            writeBytes(bytes);
        }
    }
}
