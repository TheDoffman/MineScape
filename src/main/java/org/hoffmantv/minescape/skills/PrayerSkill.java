package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.HashMap;
import java.util.Map;

public class PrayerSkill implements Listener {

    private final SkillManager skillManager;
    private final Plugin plugin;
    private final Map<Material, BoneData> boneDataMap = new HashMap<>();

    /**
     * Constructor for PrayerSkill.
     *
     * @param skillManager The SkillManager instance.
     * @param prayerConfig The ConfigurationSection for prayer from skills.yml.
     * @param plugin       The main plugin instance.
     */
    public PrayerSkill(SkillManager skillManager, ConfigurationSection prayerConfig, Plugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
        loadPrayerConfigs(prayerConfig);
    }

    /**
     * Loads prayer configurations for bones.
     *
     * @param prayerConfig The ConfigurationSection for prayer.
     */
    private void loadPrayerConfigs(ConfigurationSection prayerConfig) {
        if (prayerConfig != null) {
            plugin.getLogger().info("Loading Prayer Skill configurations...");

            ConfigurationSection bonesSection = prayerConfig.getConfigurationSection("bones");
            if (bonesSection != null) {
                for (String key : bonesSection.getKeys(false)) {
                    ConfigurationSection boneConfig = bonesSection.getConfigurationSection(key);
                    if (boneConfig != null) {
                        Material boneMaterial = Material.getMaterial(boneConfig.getString("material", "BONE").toUpperCase());
                        String displayName = ChatColor.translateAlternateColorCodes('&', boneConfig.getString("displayName", "Bone"));
                        double xpValue = boneConfig.getDouble("xpValue", 10);
                        String effectType = boneConfig.getString("effectType", "HEALING");
                        int effectDuration = boneConfig.getInt("effectDuration", 5); // in seconds
                        int effectAmplifier = boneConfig.getInt("effectAmplifier", 1);
                        String particleType = boneConfig.getString("particleType", "BLOCK_CRACK"); // Default particle

                        if (boneMaterial != null) {
                            BoneData boneData = new BoneData(boneMaterial, displayName, xpValue, effectType, effectDuration, effectAmplifier, particleType);
                            boneDataMap.put(boneMaterial, boneData);
                            plugin.getLogger().info("Loaded Bone: " + boneMaterial + " | Display Name: " + displayName + " | XP: " + xpValue + " | Effect: " + effectType + " | Particle: " + particleType);
                        } else {
                            plugin.getLogger().warning("Invalid bone material in prayer config: " + key);
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("No 'bones' section found in skills.yml under 'skills.prayer'");
            }

            plugin.getLogger().info("Prayer Skill configurations loaded successfully.");
        } else {
            plugin.getLogger().warning("Prayer configuration section is null.");
        }
    }

    /**
     * Event handler for player interactions (e.g., right-clicking with a bone).
     *
     * @param event The PlayerInteractEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if the action is right-click on a block
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Determine which hand was used (main or off-hand)
        EquipmentSlot hand = event.getHand();
        if (hand == null) { // Some older versions might not have getHand()
            hand = EquipmentSlot.HAND;
        }

        Player player = event.getPlayer();
        ItemStack item = (hand == EquipmentSlot.HAND) ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        Block clickedBlock = event.getClickedBlock();

        if (item == null || clickedBlock == null) {
            return;
        }

        Material itemType = item.getType();

        if (boneDataMap.containsKey(itemType)) {
            BoneData boneData = boneDataMap.get(itemType);
            double playerPrayerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.PRAYER);

            // Optional: Check for any required Prayer level to use the bone
            // Example:
            // if (playerPrayerLevel < boneData.getRequiredPrayerLevel()) {
            //     player.sendMessage(ChatColor.RED + "You need Prayer level " + boneData.getRequiredPrayerLevel() + " to use this bone.");
            //     return;
            // }

            // Apply the effect to the player
            applyEffect(player, boneData);

            // Award XP
            skillManager.addXP(player, SkillManager.Skill.PRAYER, boneData.getXpValue());

            // Provide feedback
            player.sendMessage(ChatColor.GREEN + "You have used " + boneData.getDisplayName() + " and earned " + ChatColor.GOLD + boneData.getXpValue() + ChatColor.GREEN + " Prayer XP!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendActionBar(ChatColor.BLUE + "Prayer +" + boneData.getXpValue());

            plugin.getLogger().info("Player " + player.getName() + " used " + boneData.getDisplayName() + " and earned " + boneData.getXpValue() + " Prayer XP.");

            // Apply visual digging effect to the clicked block
            applyDiggingEffect(clickedBlock, boneData);

            // Consume the bone from the player's inventory
            consumeBone(player, item, hand);
        }
    }

    /**
     * Applies the configured effect to the player.
     *
     * @param player   The player.
     * @param boneData The BoneData containing effect details.
     */
    private void applyEffect(Player player, BoneData boneData) {
        PotionEffectType effectType = PotionEffectType.getByName(boneData.getEffectType().toUpperCase());
        if (effectType == null) {
            plugin.getLogger().warning("Invalid effect type for bone: " + boneData.getBoneMaterial());
            return;
        }

        PotionEffect effect = new PotionEffect(effectType, boneData.getEffectDuration() * 20, boneData.getEffectAmplifier() - 1, false, false);
        player.addPotionEffect(effect);
        plugin.getLogger().info("Applied " + effectType.getName() + " effect to " + player.getName() + " for " + boneData.getEffectDuration() + " seconds.");
    }

    /**
     * Applies a visual digging effect on the specified block.
     *
     * @param block    The block to apply the effect on.
     * @param boneData The BoneData containing particle details.
     */
    private void applyDiggingEffect(Block block, BoneData boneData) {
        Particle particle;
        try {
            particle = Particle.valueOf(boneData.getParticleType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + boneData.getParticleType() + ". Defaulting to BLOCK_CRACK.");
            particle = Particle.BLOCK_CRACK;
        }

        // Adjust the particle spawn parameters as needed
        block.getWorld().spawnParticle(
                particle,
                block.getLocation().add(0.5, 0.5, 0.5),
                30, // Number of particles
                0.5, // Offset X
                0.5, // Offset Y
                0.5, // Offset Z
                0.1 // Extra (speed)
        );

        plugin.getLogger().info("Applied " + particle.name() + " particles to block at " + block.getLocation());
    }

    /**
     * Consumes one bone from the player's inventory.
     *
     * @param player The player.
     * @param item   The bone item stack.
     * @param hand   The hand used (main or off-hand).
     */
    private void consumeBone(Player player, ItemStack item, EquipmentSlot hand) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(item);
            } else {
                player.getInventory().setItemInOffHand(item);
            }
        } else {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }
        plugin.getLogger().info("Consumed one " + item.getType() + " from " + player.getName() + "'s inventory.");
    }

    /**
     * Inner class to store bone-related data.
     */
    private static class BoneData {
        private final Material boneMaterial;
        private final String displayName;
        private final double xpValue;
        private final String effectType;
        private final int effectDuration; // in seconds
        private final int effectAmplifier;
        private final String particleType;

        public BoneData(Material boneMaterial, String displayName, double xpValue, String effectType, int effectDuration, int effectAmplifier, String particleType) {
            this.boneMaterial = boneMaterial;
            this.displayName = displayName;
            this.xpValue = xpValue;
            this.effectType = effectType;
            this.effectDuration = effectDuration;
            this.effectAmplifier = effectAmplifier;
            this.particleType = particleType;
        }

        public Material getBoneMaterial() {
            return boneMaterial;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getXpValue() {
            return xpValue;
        }

        public String getEffectType() {
            return effectType;
        }

        public int getEffectDuration() {
            return effectDuration;
        }

        public int getEffectAmplifier() {
            return effectAmplifier;
        }

        public String getParticleType() {
            return particleType;
        }
    }
}