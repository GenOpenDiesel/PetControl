package org.infotoast.petcontrol.command;

import net.minecraft.world.entity.TamableAnimal;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.infotoast.petcontrol.PetControl;

public class HealPetCommand implements CommandExecutor {
    private final PetControl plugin;

    public HealPetCommand(PetControl plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (sender.hasPermission("petcontrol.heal")) {
                Player player = (Player) sender;
                Entity playerFacing = PetControl.getPlayerFacingEntity(player);
                if (playerFacing != null) {
                    CraftEntity craftPlayerFacing = (CraftEntity)playerFacing;
                    net.minecraft.world.entity.Entity animalFacing = craftPlayerFacing.getHandle();
                    if (animalFacing instanceof TamableAnimal) {
                        TamableAnimal tamableAnimalFacing = (TamableAnimal) animalFacing;
                        tamableAnimalFacing.setHealth(tamableAnimalFacing.getMaxHealth());
                        sender.sendMessage("§aZwierzak wyleczony!");
                        return true;
                    }
                }
                sender.sendMessage("§cWyceluj w zwierzaka, którego da się oswoić (pies, kot, koń itp.).");
                return true;
            }
            sender.sendMessage("§cNie masz uprawnień do tej komendy.");
            return true;
        }
        sender.sendMessage("§cTę komendę może wykonać tylko gracz (nie konsola).");
        return true;
    }
}
