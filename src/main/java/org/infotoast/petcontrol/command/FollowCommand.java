package org.infotoast.petcontrol.command;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.infotoast.petcontrol.PetControl;
import org.infotoast.petcontrol.cachefile.EntryType;
import org.infotoast.petcontrol.cachefile.TamedAnimalEntry;
import org.infotoast.petcontrol.customanimals.RoamingCat;
import org.infotoast.petcontrol.customanimals.RoamingDog;

import java.util.Objects;
import java.util.UUID;

public class FollowCommand implements CommandExecutor {
    private final PetControl plugin;
    public FollowCommand(PetControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("petcontrol.follow")) {
                Player player = (Player) sender;
                Entity playerFacing = PetControl.getPlayerFacingEntity(player);
                if (playerFacing != null) {
                    CraftEntity craftPlayerFacing = (CraftEntity)playerFacing;
                    net.minecraft.world.entity.Entity animalFacing = craftPlayerFacing.getHandle();
                    if (animalFacing instanceof TamableAnimal) {
                        TamableAnimal tamableAnimal = (TamableAnimal) animalFacing;
                        if (tamableAnimal.isTame()) {
                            UUID ownerUUID = Objects.requireNonNull(tamableAnimal.getOwnerReference()).getUUID();
                            if (ownerUUID.equals(player.getUniqueId()) || sender.hasPermission("petcontrol.follow.others")) {
                                if (tamableAnimal instanceof RoamingCat) {
                                    Cat newCat = ((RoamingCat) tamableAnimal).convertToCat();
                                    sender.sendMessage("§aKot wrócił do normalnego trybu - będzie chodził za Tobą i teleportował się.");
                                    sender.sendMessage("§7UUID: §f" + newCat.getUUID());
                                    disableRoamingInCache(tamableAnimal, newCat);
                                    return true;
                                } else if (tamableAnimal instanceof RoamingDog) {
                                    Wolf newWolf = ((RoamingDog) tamableAnimal).convertToWolf();
                                    sender.sendMessage("§aPies wrócił do normalnego trybu - będzie chodził za Tobą i teleportował się.");
                                    sender.sendMessage("§7UUID: §f" + newWolf.getUUID());
                                    disableRoamingInCache(tamableAnimal, newWolf);
                                    return true;
                                } else {
                                    sender.sendMessage("§cWyceluj w kota lub psa, który jest w trybie wędrowania (/roam).");
                                    return true;
                                }
                            } else {
                                sender.sendMessage("§cMożesz zmieniać tryb tylko swoim zwierzakom.");
                                return true;
                            }
                        } else {
                            sender.sendMessage("§cTo stworzenie nie jest oswojone - najpierw je oswój.");
                            return true;
                        }
                    } else {
                        sender.sendMessage("§cTego stworzenia nie da się oswoić. Wyceluj w psa, kota, konia itp.");
                        return true;
                    }
                } else {
                    sender.sendMessage("§cNie patrzysz na żadne stworzenie. Podejdź bliżej (do 10 kratek) i wyceluj w nie kursorem.");
                    return true;
                }
            } else {
                sender.sendMessage("§cNie masz uprawnień do tej komendy.");
                return true;
            }
        } else {
            sender.sendMessage("§cTę komendę może wykonać tylko gracz (nie konsola).");
            return true;
        }
    }

    private void disableRoamingInCache(TamableAnimal oldAnimal, TamableAnimal newAnimal) {
        TamedAnimalEntry ent = PetControl.cacheManager.getTamedAnimalFromUUID(oldAnimal.getUUID());
        PetControl.cacheManager.removeByUUID(oldAnimal.getUUID(), EntryType.TAMED);
        ent.setUUID(newAnimal.getUUID());
        ent.setRoaming(false);
        PetControl.cacheManager.addTamedAnimalEntry(ent);
    }
}
