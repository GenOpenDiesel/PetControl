package org.infotoast.petcontrol.cachefile;

import org.infotoast.petcontrol.PetControl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class CacheFileLoader {
    private static final byte ENTRY_DELIMITER = 0xf;
    private File cacheFile;
    public CacheFileLoader(File cacheFile) {
        this.cacheFile = cacheFile;
    }

    public CacheFile getFromFile() {
        CacheFile data = new CacheFile();
        try {
            FileInputStream fis = new FileInputStream(this.cacheFile);
            byte[] file = fis.readAllBytes();
            fis.close();

            // Entries are walked structurally rather than by scanning for the 0xf delimiter:
            // the delimiter is not escaped, and it occurs naturally inside payloads (coordinate
            // bytes, a 15 character owner or pet name), which used to split entries mid-record.
            // Cache version 2 - TamedAnimalEntry and sitting bit
            if (file.length > 0 && file[0] == 0x2) {
                readEntries(data, file, 1, false);
            } else {
                readEntries(data, file, 0, true);
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    private void readEntries(CacheFile data, byte[] file, int start, boolean legacy) {
        int pos = start;
        while (pos < file.length) {
            int length;
            try {
                length = entryLength(file, pos, legacy);
            } catch (ArrayIndexOutOfBoundsException e) {
                warn("truncated entry at offset " + pos + ", skipping the rest of the cache file");
                return;
            }

            if (length <= 0 || pos + length > file.length) {
                warn("bad entry length at offset " + pos + ", skipping the rest of the cache file");
                return;
            }
            if (file[pos + length - 1] != ENTRY_DELIMITER) {
                warn("missing entry delimiter at offset " + pos + ", skipping the rest of the cache file");
                return;
            }

            byte[] b = Arrays.copyOfRange(file, pos, pos + length);
            pos += length;

            try {
                if (legacy) {
                    data.addAnimal(RoamingAnimalEntry.readBytesLegacy(b));
                } else if (b[0] == 0x0) {
                    data.addAnimal(RoamingAnimalEntry.readBytes(b));
                } else {
                    data.addAnimal(TamedAnimalEntry.readBytes(b));
                }
            } catch (RuntimeException e) {
                // A single unreadable entry must not take the whole cache (and the plugin) down.
                warn("could not read entry at offset " + (pos - length) + " (" + e + "), skipping it");
            }
        }
    }

    // Both entry types are self describing, so the on disk size of a record can be computed
    // without relying on the delimiter.
    private int entryLength(byte[] file, int pos, boolean legacy) {
        if (legacy) {
            int uuidLength = file[pos + 14] & 0xFF;
            return 18 + uuidLength;
        }
        byte entryType = file[pos];
        if (entryType == 0x0) {
            int uuidLength = file[pos + 14] & 0xFF;
            return 19 + uuidLength;
        } else if (entryType == 0x1) {
            int uuidLength = file[pos + 2] & 0xFF;
            int ownerUuidLength = file[pos + 3 + uuidLength] & 0xFF;
            int ownerNameLength = file[pos + 4 + uuidLength + ownerUuidLength] & 0xFF;
            int nameLength = file[pos + 5 + uuidLength + ownerUuidLength + ownerNameLength] & 0xFF;
            return 10 + uuidLength + ownerUuidLength + ownerNameLength + nameLength;
        }
        return -1;
    }

    private void warn(String message) {
        if (PetControl.logger != null) {
            PetControl.logger.warning("cache.dat: " + message);
        }
    }
}
