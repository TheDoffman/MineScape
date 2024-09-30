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
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Prayer implements Listener {

    private final SkillManager skillManager;
    private final Plugin plugin;
    private final Map<Material, BoneData> boneDataMap = new HashMap<>();

    // Cooldown map to prevent spamming
    private final Map<Player, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 10 * 1000; // 10 seconds in milliseconds

    // XP multiplier for using bones on altars
    private final double ALTAR_XP_MULTIPLIER = 2.5;

    public Prayer(SkillManager skillManager, ConfigurationSection prayerConfig, Plugin plugin) {
        this.skillManager = skillManager;
        this.plugin = plugin;
        loadPrayerConfigs(prayerConfig);
    }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
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
            double xpValue = boneData.getXpValue();

            // Check if using on an altar for additional XP
            if (isAltar(clickedBlock)) {
                xpValue *= ALTAR_XP_MULTIPLIER;
                player.sendMessage(ChatColor.AQUA + "You used the bone on an altar and earned extra XP!");
            }

            // Award XP
            addPrayerXP(player, xpValue);

            // Provide feedback
            String usedBoneMessage = "You have used " + boneData.getDisplayName() + " and earned " + ChatColor.GOLD + xpValue + ChatColor.GREEN + " Prayer XP!";
            player.sendMessage(ChatColor.GREEN + usedBoneMessage);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendActionBar(ChatColor.BLUE + "Prayer +" + xpValue);

            plugin.getLogger().info("Player " + player.getName() + " used " + boneData.getDisplayName() + " and earned " + xpValue + " Prayer XP.");

            // Apply visual digging effect to the clicked block
            applyDiggingEffect(clickedBlock, boneData);

            // Consume the bone from the player's inventory
            consumeBone(player, item);

            // Set cooldown
            cooldowns.put(player, System.currentTimeMillis() + COOLDOWN_TIME);

            // Schedule removal of cooldown
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> cooldowns.remove(player), COOLDOWN_TIME / 50);
        }
    }

    private void addPrayerXP(Player player, double xp) {
        skillManager.addXP(player, SkillManager.Skill.PRAYER, xp);  // Add XP to prayer skill
        UUID playerUUID = player.getUniqueId();

        // Save XP to playerdata.yml
        skillManager.getPlayerDataConfig().set(playerUUID.toString() + ".prayer.xp", skillManager.getXP(player, SkillManager.Skill.PRAYER));
        skillManager.savePlayerDataAsync();  // Save asynchronously
    }

    private boolean isAltar(Block block) {
        // Example: Altar blocks could be gold blocks in this implementation
        return block.getType() == Material.GOLD_BLOCK;
    }

    private void applyDiggingEffect(Block block, BoneData boneData) {
        Particle particle;
        try {
            particle = Particle.valueOf(boneData.getParticleType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + boneData.getParticleType() + ". Defaulting to BLOCK_CRACK.");
            particle = Particle.BLOCK_CRACK;
        }

        if (particle.equals(Particle.BLOCK_CRACK) || particle.equals(Particle.BLOCK_DUST)) {
            BlockData blockData = block.getBlockData();

            block.getWorld().spawnParticle(
                    particle,
                    block.getLocation().add(0.5, 0.5, 0.5),
                    30,
                    0.5,
                    0.5,
                    0.5,
                    0.1,
                    blockData
            );
        } else {
            block.getWorld().spawnParticle(
                    particle,
                    block.getLocation().add(0.5, 0.5, 0.5),
                    30,
                    0.5,
                    0.5,
                    0.5,
                    0.1
            );
        }
    }

    private void consumeBone(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().remove(item);
        }
    }

    private static class BoneData {
        private final Material boneMaterial;
        private final String displayName;
        private final double xpValue;
        private final String effectType;
        private final int effectDuration;
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