package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Range implements Listener {

    private final JavaPlugin plugin;
    private final SkillManager skillManager;

    public Range(JavaPlugin plugin, SkillManager skillsManager) {
        this.plugin = plugin;
        this.skillManager = skillsManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // Check if the killer exists and if the last damage cause was an arrow.
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageEvent.DamageCause cause = entity.getLastDamageCause().getCause();

            if (cause == EntityDamageEvent.DamageCause.PROJECTILE) {
                Entity damager = ((EntityDamageByEntityEvent) entity.getLastDamageCause()).getDamager();
                Arrow arrow = (Arrow) damager;

                if (arrow.getShooter() instanceof Player) {
                    double distance = arrow.getLocation().distance(killer.getLocation());
                    grantXPBasedOnDistance(killer, distance);
                }
            }
        }
    }
            private void grantXPBasedOnDistance (Player killer,double distance){
                int baseXP = 10; // Base XP for killing with a bow

                // We'll use a simple linear formula: additionalXP = distance/5.
                int additionalXP = (int) (distance / 2);
                int xpEarned = baseXP + additionalXP;

                // Using SkillsManager to add the XP to the player's skills in the skills.yml file
                skillManager.addXP(killer, SkillManager.Skill.RANGE, xpEarned);

                killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                killer.sendActionBar(ChatColor.GOLD + "Range +" + xpEarned);
            }
        }
