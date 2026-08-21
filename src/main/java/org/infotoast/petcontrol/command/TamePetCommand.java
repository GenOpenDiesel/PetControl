package org.infotoast.petcontrol.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.bukkit.entity.Tameable;
import org.infotoast.petcontrol.PetControl;
import org.infotoast.petcontrol.cachefile.AnimalType;
import org.infotoast.petcontrol.cachefile.CacheFileManager;
import org.infotoast.petcontrol.cachefile.TamedAnimalEntry;

public class TamePetCommand implements CommandExecutor {
    private final PetControl plugin;

    public TamePetCommand(PetControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("petcontrol.tamepet.self") || sender.hasPermission("petcontrol.tamepet.others")) {
                Player player = (Player) sender;
                Entity playerFacing = PetControl.getPlayerFacingEntity(player);
                if (playerFacing != null) {
                    if (playerFacing instanceof Tameable) {
                        Tameable tamableAnimal = (Tameable) playerFacing;
                        if (!tamableAnimal.isTamed()) {
                            OfflinePlayer newOwner;
                            if (args.length == 1) {
                                if (sender.hasPermission("petcontrol.tamepet.others")) {
                                    String nextOwnerName = args[0];
                                    newOwner = Bukkit.getOfflinePlayer(nextOwnerName);
                                    if (!newOwner.hasPlayedBefore()) {
                                        sender.sendMessage("§cNie ma takiego gracza albo nigdy nie był na serwerze.");
                                        return true;
                                    }
                                } else {
                                    sender.sendMessage("§cNie masz uprawnień, by oswajać zwierzaki dla innych graczy.");
                                    return true;
                                }
                            } else if (args.length == 0) {
                                if (sender.hasPermission("petcontrol.tamepet.self")) {
                                    newOwner = (Player)sender;
                                } else {
                                    sender.sendMessage("§cNie masz uprawnień, by oswajać zwierzaki.");
                                    return true;
                                }
                            } else {
                                sender.sendMessage("§cZa dużo argumentów. Użycie: §7/tamepet [gracz]");
                                return true;
                            }
                            tamableAnimal.setOwner(newOwner);
                            sender.sendMessage("§aZwierzak został oswojony!");
                            AnimalType animalType = PetControl.cacheManager.getAnimalTypeFromEntity(tamableAnimal);
                            TamedAnimalEntry tae = new TamedAnimalEntry(animalType, tamableAnimal.getUniqueId(), newOwner.getUniqueId(), tamableAnimal.getName(), newOwner.getName(), true, false, false);
                            PetControl.cacheManager.addTamedAnimalEntry(tae);
                            return true;
                        }
                        sender.sendMessage("§cTen zwierzak ma już właściciela. Użyj §7/transferpetowner§c, aby go przekazać.");
                        return true;
                    }
                    sender.sendMessage("§cTego stworzenia nie da się oswoić. Wyceluj w psa, kota, konia itp.");
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
