package org.hoffmantv.minescape.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.hoffmantv.minescape.managers.SkillManager;

public class DefenseSkill implements Listener {

    private final SkillManager skillManager;

    public DefenseSkill(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return; // Exit if the entity taking damage isn't a player
        }

        Player player = (Player) event.getEntity();

        // Calculate XP based on damage taken.
        // We assume 2 damage = 1 heart, hence we multiply by 0.5 to get hearts
        int xpToGive = (int) (event.getFinalDamage() * 0.5);

        // Give the XP to the player's defense skill
        skillManager.addXP(player, SkillManager.Skill.DEFENCE, xpToGive);

        // You can optionally inform the player about XP gain here.
    }
}
