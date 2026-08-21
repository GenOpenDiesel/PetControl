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

public class RoamCommand implements CommandExecutor {
    private final PetControl plugin;

    public RoamCommand(PetControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("petcontrol.roam")) {
                Player player = (Player) sender;
                Entity playerFacing = PetControl.getPlayerFacingEntity(player);
                if (playerFacing != null) {
                    CraftEntity craftPlayerFacing = (CraftEntity)playerFacing;
                    net.minecraft.world.entity.Entity animalFacing = craftPlayerFacing.getHandle();
                    if (animalFacing instanceof TamableAnimal) {
                        TamableAnimal tamableAnimal = (TamableAnimal) animalFacing;
                        if (tamableAnimal.isTame()) {
                            UUID ownerUUID = Objects.requireNonNull(tamableAnimal.getOwnerReference()).getUUID();
                            if (args.length >= 1) {
                                // Get coordinates
                                int x;
                                int z;
                                int radius = Integer.parseInt(args[0]);
                                if (args.length >= 3) {
                                    x = Integer.parseInt(args[1]);
                                    z = Integer.parseInt(args[2]);
                                } else {
                                    x = tamableAnimal.getBlockX();
                                    z = tamableAnimal.getBlockZ();
                                }

                                if (ownerUUID.equals(player.getUniqueId()) || sender.hasPermission("petcontrol.roam.others")) {
                                    if (tamableAnimal instanceof Cat) {
                                        playerFacing.remove();
                                        RoamingCat rcat = RoamingCat.convertFromCat((Cat) tamableAnimal, x, z, radius, false);
                                        sender.sendMessage("§aKot chodzi teraz po wyznaczonym terenie. §7UUID: §f" + rcat.getUUID());
                                        enableRoamingInCache(tamableAnimal, rcat);
                                        return true;
                                    } else if (tamableAnimal instanceof Wolf) {
                                        playerFacing.remove();
                                        RoamingDog rdog = RoamingDog.convertFromWolf((Wolf) tamableAnimal, x, z, radius, false);
                                        sender.sendMessage("§aPies chodzi teraz po wyznaczonym terenie. §7UUID: §f" + rdog.getUUID());
                                        enableRoamingInCache(tamableAnimal, rdog);
                                        return true;
                                    }
                                } else {
                                    sender.sendMessage("§cMożesz ustawiać wędrowanie tylko swoim zwierzakom.");
                                    return true;
                                }
                            } else {
                                sender.sendMessage("§cBłędne użycie komendy:");
                                sender.sendMessage("§7/roam <promień> [środek x] [środek z] [pilnowanie]");
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
                    sender.sendMessage("§cWyceluj w zwierzaka, którego da się oswoić (pies, kot, koń itp.).");
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
        sender.sendMessage("§cCoś poszło nie tak. Zgłoś to administracji.");
        return true;
    }

    private void enableRoamingInCache(TamableAnimal oldAnimal, TamableAnimal newAnimal) {
        TamedAnimalEntry ent = PetControl.cacheManager.getTamedAnimalFromUUID(oldAnimal.getUUID());
        PetControl.cacheManager.removeByUUID(oldAnimal.getUUID(), EntryType.TAMED);
        ent.setUUID(newAnimal.getUUID());
        ent.setRoaming(true);
        PetControl.cacheManager.addTamedAnimalEntry(ent);
    }
}
