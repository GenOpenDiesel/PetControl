package org.infotoast.petcontrol.command;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.infotoast.petcontrol.PetControl;
import org.infotoast.petcontrol.cachefile.EntryType;
import org.infotoast.petcontrol.cachefile.RoamingAnimalEntry;
import org.infotoast.petcontrol.cachefile.TamedAnimalEntry;
import org.infotoast.petcontrol.customanimals.RoamingCat;
import org.infotoast.petcontrol.customanimals.RoamingDog;
import org.infotoast.petcontrol.exception.EntityNotTamableException;

import java.util.UUID;

public class PetInfoCommand implements CommandExecutor {
    private final PetControl plugin;

    public PetInfoCommand(PetControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("petcontrol.petinfo")) {
                Player player = (Player) sender;
                Entity playerFacing = PetControl.getPlayerFacingEntity(player);
                if (playerFacing != null) {
                    CraftEntity craftPlayerFacing = (CraftEntity)playerFacing;
                    net.minecraft.world.entity.Entity animalFacing = craftPlayerFacing.getHandle();
                    if (animalFacing instanceof TamableAnimal) {
                        TamableAnimal tamableAnimal = (TamableAnimal) animalFacing;
                        sender.sendMessage("§6§l--------< INFO O ZWIERZAKU >--------");
                        sender.sendMessage("§6§l| §r§1Typ: §r§9" + playerFacing.getType());
                        if (playerFacing.getName() != null) {
                            sender.sendMessage("§6§l| §r§1Nazwa: §r§9" + playerFacing.getName());
                        }
                        sender.sendMessage("§6§l| §r§1UUID: §r§9" + playerFacing.getUniqueId());
                        sender.sendMessage("§6§l| §r§1Zdrowie: §r§9" + tamableAnimal.getHealth() + "/" + tamableAnimal.getMaxHealth());
                        String isTame = (tamableAnimal.isTame()) ? "Tak" : "Nie";
                        sender.sendMessage("§6§l| §r§1Oswojone: §r§9" + isTame);
                        if (tamableAnimal.isTame()) {
                            EntityReference<? extends net.minecraft.world.entity.LivingEntity> ownerRef = tamableAnimal.getOwnerReference();
                            UUID ownerUUID = (ownerRef != null) ? ownerRef.getUUID() : null;

                            // Wlasciciela pokazujemy zawsze, zanim dotkniemy cache - cache obsluguje
                            // tylko czesc zwierzat (pies/kot/papuga/nautilus) i dla konia czy lamy
                            // rzucal wyjatkiem, przez co reszta info sie nie wypisywala.
                            if (ownerUUID != null) {
                                String ownerName = Bukkit.getOfflinePlayer(ownerUUID).getName();
                                if (ownerName == null) {
                                    ownerName = ownerUUID.toString();
                                }
                                sender.sendMessage("§6§l| §r§1Właściciel: §r§9" + ownerName);
                            } else {
                                sender.sendMessage("§6§l| §r§1Właściciel: §r§9nieznany");
                            }
                            sender.sendMessage("§6§l| §r§1Siedzi: §r§9" + (tamableAnimal.isInSittingPose() ? "Tak" : "Nie"));

                            RoamingAnimalEntry rae = PetControl.cacheManager.getRoamingAnimalFromUUID(playerFacing.getUniqueId());
                            boolean roaming = (rae != null);
                            if (roaming) {
                                sender.sendMessage("§6§l| §r§1Wędrowanie: §r§9Tak");
                                sender.sendMessage("§6§l| §r§1Promień wędrowania: §r§9" + rae.getRadius());
                                sender.sendMessage("§6§l| §r§1Środek wędrowania: §r§9" + rae.getCenterX() + ", " + rae.getCenterZ());
                            } else {
                                sender.sendMessage("§6§l| §r§1Wędrowanie: §r§9Nie");
                            }

                            TamedAnimalEntry tae = PetControl.cacheManager.getTamedAnimalFromUUID(playerFacing.getUniqueId());
                            if (ownerUUID != null) {
                                try {
                                    boolean guarded = (tae != null) && tae.isGuarded();
                                    TamedAnimalEntry updated = new TamedAnimalEntry(PetControl.cacheManager.getAnimalTypeFromEntity(playerFacing), playerFacing.getUniqueId(), ownerUUID, playerFacing.getName(), Bukkit.getOfflinePlayer(ownerUUID).getName(), tamableAnimal.isOrderedToSit(), guarded, roaming);
                                    if (tae != null) {
                                        PetControl.cacheManager.removeByUUID(tae.getUUID(), EntryType.TAMED);
                                    }
                                    PetControl.cacheManager.addTamedAnimalEntry(updated);
                                    tae = updated;
                                } catch (EntityNotTamableException e) {
                                    // Typ zwierzaka nieobslugiwany przez cache - info i tak pokazujemy
                                }
                            }
                            if (tae != null) {
                                sender.sendMessage("§6§l| §r§1Pilnuje terenu: §r§9" + (tae.isGuarded() ? "Tak" : "Nie"));
                            }
                        }
                        sender.sendMessage("§6§l------------------------------------");
                        return true;
                    } else if (playerFacing instanceof LivingEntity && !(playerFacing instanceof Player)) {
                        // Support for every other living entity, not only the near-tamable ones
                        LivingEntity playerFacingLiving = (LivingEntity) playerFacing;
                        sender.sendMessage("§6§l--------< INFO O STWORZENIU >--------");
                        sender.sendMessage("§6§l| §r§1Typ: §r§9" + playerFacing.getType());
                        if (playerFacing.getName() != null) {
                            sender.sendMessage("§6§l| §r§1Nazwa: §r§9" + playerFacing.getName());
                        }
                        sender.sendMessage("§6§l| §r§1UUID: §r§9" + playerFacing.getUniqueId());
                        sender.sendMessage("§6§l| §r§1Zdrowie: §r§9" + playerFacingLiving.getHealth() + "/" + playerFacingLiving.getMaxHealth());
                        sender.sendMessage("§6§l---------------------------------------");
                        return true;
                    }
                    sender.sendMessage("§cNie potrafię pokazać informacji o tym obiekcie - wyceluj w żywe stworzenie.");
                    return true;
                }
                sender.sendMessage("§cNie patrzysz na żadne stworzenie. Podejdź bliżej (do 10 kratek) i wyceluj w nie kursorem.");
                return true;
            }
            sender.sendMessage("§cNie masz uprawnień do tej komendy.");
            return true;
        }
        sender.sendMessage("§cTę komendę może wykonać tylko gracz (nie konsola).");
        return true;
    }
}
