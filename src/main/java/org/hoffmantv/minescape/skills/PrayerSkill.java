package org.hoffmantv.minescape.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.hoffmantv.minescape.managers.SkillManager;

import java.util.HashMap;
import java.util.Map;

public class PrayerSkill implements Listener {

    private final SkillManager skillManager;
    private final Plugin plugin;
    private final Map<Material, BoneData> boneDataMap = new HashMap<>();

    // Cooldown map to prevent spamming
    private final Map<Player, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 10 * 1000; // 10 seconds in milliseconds

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

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block clickedBlock = event.getClickedBlock();

        if (item == null || clickedBlock == null) {
            return;
        }

        Material itemType = item.getType();

        if (boneDataMap.containsKey(itemType)) {
            // Check cooldown
            if (cooldowns.containsKey(player)) {
                long timeLeft = cooldowns.get(player) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    player.sendMessage(ChatColor.YELLOW + "You can use this bone again in " + (timeLeft / 1000) + " seconds.");
                    return;
                }
            }

            BoneData boneData = boneDataMap.get(itemType);
            double playerPrayerLevel = skillManager.getSkillLevel(player, SkillManager.Skill.PRAYER);

            // Optional: Check for any required Prayer level to use the bone
            // Example:
            // int requiredPrayerLevel = boneData.getRequiredPrayerLevel();
            // if (playerPrayerLevel < requiredPrayerLevel) {
            //     player.sendMessage(ChatColor.RED + "You need Prayer level " + requiredPrayerLevel + " to use this bone.");
            //     return;
            // }

            // Apply the effect to the player
            applyEffect(player, boneData);

            // Award XP
            skillManager.addXP(player, SkillManager.Skill.PRAYER, boneData.getXpValue());

            // Provide feedback
            String usedBoneMessage = "You have used " + boneData.getDisplayName() + " and earned " + ChatColor.GOLD + boneData.getXpValue() + ChatColor.GREEN + " Prayer XP!";
            player.sendMessage(ChatColor.GREEN + usedBoneMessage);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendActionBar(ChatColor.BLUE + "Prayer +" + boneData.getXpValue());

            plugin.getLogger().info("Player " + player.getName() + " used " + boneData.getDisplayName() + " and earned " + boneData.getXpValue() + " Prayer XP.");

            // Apply visual digging effect to the clicked block
            applyDiggingEffect(clickedBlock, boneData);

            // Consume the bone from the player's inventory
            consumeBone(player, item);

            // Set cooldown
            cooldowns.put(player, System.currentTimeMillis() + COOLDOWN_TIME);

            // Schedule removal of cooldown
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> cooldowns.remove(player), COOLDOWN_TIME / 50); // Convert ms to ticks (20 ticks = 1 second)
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

        // Check if the particle requires BlockData
        if (particle.equals(Particle.BLOCK_CRACK) || particle.equals(Particle.BLOCK_DUST)) {
            // Obtain the BlockData from the clicked block
            BlockData blockData = block.getBlockData();

            // Spawn particles with BlockData
            try {
                block.getWorld().spawnParticle(
                        particle,
                        block.getLocation().add(0.5, 0.5, 0.5),
                        30, // Number of particles
                        0.5, // Offset X
                        0.5, // Offset Y
                        0.5, // Offset Z
                        0.1, // Extra (speed)
                        blockData // Required BlockData
                );
                plugin.getLogger().info("Applied " + particle.name() + " particles to block at " + block.getLocation());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to spawn particle: " + particle.name() + ". " + e.getMessage());
            }
        } else {
            // Spawn particles without BlockData
            try {
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
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to spawn particle: " + particle.name() + ". " + e.getMessage());
            }
        }
    }

    /**
     * Consumes one bone from the player's inventory.
     *
     * @param player The player.
     * @param item   The bone item stack.
     */
    private void consumeBone(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().remove(item);
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