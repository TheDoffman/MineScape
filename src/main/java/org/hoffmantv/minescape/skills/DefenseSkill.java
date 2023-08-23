package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.hoffmantv.minescape.managers.CombatLevelSystem;
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Handles defensive skills when a player is damaged by an entity.
 */
public class DefenseSkill implements Listener {

    private final SkillManager skillManager;
    private final CombatLevel combatLevel;
    private final AttackSkill attackSkill;

    public DefenseSkill(SkillManager skillManager, CombatLevel combatLevel, AttackSkill attackSkill) {
        this.skillManager = skillManager;
        this.combatLevel = combatLevel;
        this.attackSkill = attackSkill;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;  // Return if the damaged entity isn't a player
        }

        Player player = (Player) event.getEntity();
        LivingEntity damager = (LivingEntity) event.getDamager();

        // Ensure the damage is coming from a living entity (e.g., a mob)
        if (!(damager instanceof LivingEntity)) {
            return;
        }

        Integer damagerLevel = CombatLevelSystem.extractMobLevelFromName(damager);
        if (damagerLevel == null) {
            return;
        }

        // Process defense skill logic
        processDefensiveAction(player, event.getFinalDamage());
    }

    /**
     * Process the defensive action of the player.
     * Calculate XP based on the damage taken and notify the player.
     */
    private void processDefensiveAction(Player player, double damageTaken) {
        // Calculate XP based on the damage taken
        int xpAmount = calculateXpReward(damageTaken);

        // Add the XP to the player's defense skill
        skillManager.addXP(player, SkillManager.Skill.DEFENCE, xpAmount);

        // Notify the player
        player.sendActionBar(ChatColor.GOLD + "Defence +" + xpAmount);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
    }

    /**
     * Calculate the XP reward based on the damage taken.
     *
     * @param damageTaken the damage taken by the player
     * @return the XP amount to be rewarded
     */
    private int calculateXpReward(double damageTaken) {
        // 10 XP per heart of damage taken
        return (int) (damageTaken * 2);
    }
    private static final Map<Material, Integer> ARMOR_LEVEL_REQUIREMENTS;

    static {
        Map<Material, Integer> tempMap = new HashMap<>();

        tempMap.put(Material.LEATHER_BOOTS, 1);
        tempMap.put(Material.LEATHER_LEGGINGS, 1);
        tempMap.put(Material.LEATHER_CHESTPLATE, 1);
        tempMap.put(Material.LEATHER_HELMET, 1);

        tempMap.put(Material.CHAINMAIL_HELMET, 10);
        tempMap.put(Material.CHAINMAIL_CHESTPLATE, 10);
        tempMap.put(Material.CHAINMAIL_LEGGINGS, 10);
        tempMap.put(Material.CHAINMAIL_BOOTS, 10);

        tempMap.put(Material.GOLDEN_HELMET, 15);
        tempMap.put(Material.GOLDEN_CHESTPLATE, 15);
        tempMap.put(Material.GOLDEN_LEGGINGS, 15);
        tempMap.put(Material.GOLDEN_BOOTS, 15);

        tempMap.put(Material.IRON_HELMET, 20);
        tempMap.put(Material.IRON_CHESTPLATE, 20);
        tempMap.put(Material.IRON_LEGGINGS, 20);
        tempMap.put(Material.IRON_BOOTS, 20);

        tempMap.put(Material.DIAMOND_HELMET, 30);
        tempMap.put(Material.DIAMOND_CHESTPLATE, 30);
        tempMap.put(Material.DIAMOND_LEGGINGS, 30);
        tempMap.put(Material.DIAMOND_BOOTS, 30);

        tempMap.put(Material.NETHERITE_HELMET, 50);
        tempMap.put(Material.NETHERITE_CHESTPLATE, 50);
        tempMap.put(Material.NETHERITE_LEGGINGS, 50);
        tempMap.put(Material.NETHERITE_BOOTS, 50);

        ARMOR_LEVEL_REQUIREMENTS = Collections.unmodifiableMap(tempMap);
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if the player is trying to equip armor with a right-click action
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (ARMOR_LEVEL_REQUIREMENTS.containsKey(item.getType())) {
                int requiredLevel = ARMOR_LEVEL_REQUIREMENTS.get(item.getType());
                int playerDefenseLevel = skillManager.getSkillLevel(player, SkillManager.Skill.DEFENCE);

                if (playerDefenseLevel < requiredLevel) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You need a defense level of " + requiredLevel + " to wear this armor.");
                }
            }
        }

    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Armor they're trying to equip or swap
        ItemStack clickedItem = event.getCursor();
        if (clickedItem == null) {
            return;
        }

        Material armorMaterial = clickedItem.getType();
        if (!ARMOR_LEVEL_REQUIREMENTS.containsKey(armorMaterial)) {
            return; // This armor doesn't have any level requirements
        }

        int requiredLevel = ARMOR_LEVEL_REQUIREMENTS.get(armorMaterial);
        int playerDefenseLevel = skillManager.getSkillLevel(player, SkillManager.Skill.DEFENCE);

        if (event.getSlotType() == InventoryType.SlotType.ARMOR ||
                (event.getClick().equals(ClickType.SHIFT_LEFT) || event.getClick().equals(ClickType.SHIFT_RIGHT))) {
            if (playerDefenseLevel < requiredLevel) {
                event.setCancelled(true);
                player.getInventory().addItem(clickedItem);  // Return the item to the player's inventory
                player.sendMessage(ChatColor.RED + "You need a defense level of " + requiredLevel + " to wear this armor.");
            }
        }
    }


}


