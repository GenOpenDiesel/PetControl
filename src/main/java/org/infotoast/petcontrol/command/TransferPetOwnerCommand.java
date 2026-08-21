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
import org.infotoast.petcontrol.cachefile.EntryType;
import org.infotoast.petcontrol.cachefile.TamedAnimalEntry;

import java.util.Objects;
import java.util.UUID;

public class TransferPetOwnerCommand implements CommandExecutor {
    private final PetControl plugin;

    public TransferPetOwnerCommand(PetControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("petcontrol.transferpetowner")) {
                Player player = (Player) sender;
                Entity playerFacing = PetControl.getPlayerFacingEntity(player);
                if (playerFacing != null) {
                    if (playerFacing instanceof Tameable) {
                        Tameable tamableAnimal = (Tameable) playerFacing;
                        if (tamableAnimal.isTamed()) {
                            UUID lastOwnerUUID = Objects.requireNonNull(tamableAnimal.getOwnerUniqueId());
                            String lastOwnerName = Bukkit.getOfflinePlayer(lastOwnerUUID).getName();
                            if (args.length == 1) {
                                String nextOwnerName = args[0];
                                OfflinePlayer newOwner = Bukkit.getOfflinePlayer(nextOwnerName);
                                tamableAnimal.setOwner(newOwner);
                                TamedAnimalEntry tae = PetControl.cacheManager.getTamedAnimalFromUUID(tamableAnimal.getUniqueId());
                                PetControl.cacheManager.removeByUUID(tamableAnimal.getUniqueId(), EntryType.TAMED);
                                tae.setOwnerUUID(newOwner.getUniqueId());
                                tae.setOwnerName(newOwner.getName());
                                PetControl.cacheManager.markDirty();
                                PetControl.cacheManager.addTamedAnimalEntry(tae);
                                sender.sendMessage("§7Poprzedni właściciel: §f" + lastOwnerName);
                                sender.sendMessage("§7Nowy właściciel: §f" + nextOwnerName);
                                sender.sendMessage("§aZwierzak został przekazany nowemu właścicielowi!");
                                return true;
                            }
                            sender.sendMessage("§cPodaj nazwę gracza. Użycie: §7/transferpetowner <gracz>");
                            return true;
                        }
                    }
                    sender.sendMessage("§cTo stworzenie nie jest oswojone - najpierw je oswój.");
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
