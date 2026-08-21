package org.infotoast.petcontrol.command;

import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.infotoast.petcontrol.PetControl;
import org.infotoast.petcontrol.cachefile.TamedAnimalEntry;

public class ToggleSitCommand implements CommandExecutor {
    private final PetControl plugin;

    public ToggleSitCommand(PetControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("petcontrol.togglesit")) {
                Player player = (Player) sender;
                Entity playerFacing = PetControl.getPlayerFacingEntity(player);
                if (playerFacing != null) {
                    CraftEntity craftPlayerFacing = (CraftEntity)playerFacing;
                    net.minecraft.world.entity.Entity animalFacing = craftPlayerFacing.getHandle();
                    if (animalFacing instanceof TamableAnimal) {
                        TamableAnimal tamableAnimal = (TamableAnimal) animalFacing;
                        if (tamableAnimal.isTame()) {
                            tamableAnimal.setOrderedToSit(!tamableAnimal.isOrderedToSit());
                            PetControl.cacheManager.getTamedAnimalFromUUID(tamableAnimal.getUUID()).setSitting(tamableAnimal.isOrderedToSit());
                            PetControl.cacheManager.markDirty();
                            sender.sendMessage("§aZmieniono pozycję zwierzaka (siedzi/stoi).");
                            return true;
                        }
                        sender.sendMessage("§cTo stworzenie nie jest oswojone - najpierw je oswój.");
                        return true;
                    }
                }
                sender.sendMessage("§cWyceluj w oswojonego zwierzaka.");
                return true;
            }
            sender.sendMessage("§cNie masz uprawnień do tej komendy.");
            return true;
        }
        sender.sendMessage("§cTę komendę może wykonać tylko gracz (nie konsola).");
        return true;
    }
}
