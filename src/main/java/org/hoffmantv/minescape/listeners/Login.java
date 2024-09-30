package org.hoffmantv.minescape.listeners;

import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.hoffmantv.minescape.MineScape;
import org.hoffmantv.minescape.skills.SkillManager;
import org.hoffmantv.minescape.skills.SkillsHologram;

import java.util.HashMap;
import java.util.Map;

public class Login implements Listener {
    private final SkillsHologram skillsHologram;
    private final MineScape plugin;
    private final SkillManager skillManager;
    private final ConfigurationSection config;

    // Cooldown map to prevent spamming (optional)
    private final Map<Player, Long> cooldowns;
    private final long COOLDOWN_TIME = 10 * 1000; // 10 seconds in milliseconds

    public Login(SkillsHologram skillsHologram, MineScape plugin, SkillManager skillManager) {
        this.skillsHologram = skillsHologram;
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.config = plugin.getConfig().getConfigurationSection("loginFeature");
        this.cooldowns = new HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        skillsHologram.toggleHologram(player);
        if (config == null || !config.getBoolean("enabled", true)) {
            return; // Feature is disabled or config section is missing
        }

        // Cooldown check (optional)
        if (cooldowns.containsKey(player)) {
            long timeLeft = cooldowns.get(player) - System.currentTimeMillis();
            if (timeLeft > 0) {
                String cooldownMsg = getString(config, "messages.cooldownMessage", "&ePlease wait {seconds} seconds before using this feature again.");
                cooldownMsg = cooldownMsg.replace("{seconds}", String.valueOf(timeLeft / 1000));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', cooldownMsg));
                return;
            }
        }

        // 1. Display Welcome Messages
        displayWelcomeMessages(player);

        // 2. Trigger Particle Effects
        triggerParticleEffects(player);

        // 3. Play Sound Effects
        playSoundEffects(player);

        // 4. Display Boss Bar
        displayBossBar(player);

        // 5. Apply Buffs Based on Combat Level or Skills
        applyLoginBuffs(player);

        // 6. Daily Login Bonuses
        giveDailyLoginBonus(player);

        // Set cooldown (optional)
        cooldowns.put(player, System.currentTimeMillis() + COOLDOWN_TIME);

        // Schedule removal of cooldown
        Bukkit.getScheduler().runTaskLater(plugin, () -> cooldowns.remove(player), COOLDOWN_TIME / 50); // Convert ms to ticks (20 ticks = 1 second)
    }

    private void displayWelcomeMessages(Player player) {
        String title = getString(config, "welcomeMessage.title", "&6Welcome to MineScape!");
        String subtitle = getString(config, "welcomeMessage.subtitle", "&7Embark on your adventure!");

        player.sendTitle(ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 70, 20); // fadeIn, stay, fadeOut in ticks
    }

    private void triggerParticleEffects(Player player) {
        ConfigurationSection particleConfig = config.getConfigurationSection("particleEffect");
        if (particleConfig == null) {
            plugin.getLogger().warning("Particle effect configuration is missing.");
            return;
        }

        String particleTypeStr = getString(particleConfig, "type", "VILLAGER_HAPPY");
        Particle particle;
        try {
            particle = Particle.valueOf(particleTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + particleTypeStr + ". Defaulting to VILLAGER_HAPPY.");
            particle = Particle.VILLAGER_HAPPY;
        }

        int count = particleConfig.getInt("count", 50);
        double offsetX = particleConfig.getDouble("offsetX", 1.0);
        double offsetY = particleConfig.getDouble("offsetY", 1.0);
        double offsetZ = particleConfig.getDouble("offsetZ", 1.0);
        double speed = particleConfig.getDouble("speed", 0.1);

        // Spawn particles around the player
        player.getWorld().spawnParticle(
                particle,
                player.getLocation(),
                count,
                offsetX,
                offsetY,
                offsetZ,
                speed
        );
    }

    private void playSoundEffects(Player player) {
        ConfigurationSection soundConfig = config.getConfigurationSection("soundEffect");
        if (soundConfig == null) {
            plugin.getLogger().warning("Sound effect configuration is missing.");
            return;
        }

        String soundTypeStr = getString(soundConfig, "type", "ENTITY_PLAYER_LEVELUP");
        Sound sound;
        try {
            sound = Sound.valueOf(soundTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type: " + soundTypeStr + ". Defaulting to ENTITY_PLAYER_LEVELUP.");
            sound = Sound.ENTITY_PLAYER_LEVELUP;
        }

        float volume = (float) soundConfig.getDouble("volume", 1.0);
        float pitch = (float) soundConfig.getDouble("pitch", 1.0);

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void displayBossBar(Player player) {
        ConfigurationSection bossBarConfig = config.getConfigurationSection("bossBar");
        if (bossBarConfig == null || !bossBarConfig.getBoolean("enabled", true)) {
            return; // Boss bar feature is disabled or config section is missing
        }

        String title = getString(bossBarConfig, "title", "&eMineScape Tips");
        String message = getString(bossBarConfig, "message", "&fTip: Use your Prayer skill for boosts!");
        String colorStr = getString(bossBarConfig, "color", "GREEN").toUpperCase();
        String styleStr = getString(bossBarConfig, "style", "SOLID").toUpperCase();
        int duration = bossBarConfig.getInt("duration", 10); // in seconds

        BarColor color;
        try {
            color = BarColor.valueOf(colorStr);
        } catch (IllegalArgumentException e) {
            color = BarColor.GREEN;
            plugin.getLogger().warning("Invalid boss bar color: " + colorStr + ". Defaulting to GREEN.");
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(styleStr);
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
            plugin.getLogger().warning("Invalid boss bar style: " + styleStr + ". Defaulting to SOLID.");
        }

        BossBar bossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', title),
                color,
                style
        );

        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);

        // Schedule boss bar removal after the specified duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> bossBar.removeAll(), duration * 20L); // Convert seconds to ticks
    }

    private void applyLoginBuffs(Player player) {
        // Example: Give a small Strength boost if the player has a high Attack level
        int attackLevel = skillManager.getSkillLevel(player, SkillManager.Skill.ATTACK);
        if (attackLevel >= 50) {
            player.sendMessage(ChatColor.GOLD + "You've received a Strength buff for your high Attack level!");
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INCREASE_DAMAGE, 20 * 60, 1)); // 1-minute strength buff
        }
    }

    private void giveDailyLoginBonus(Player player) {
        // Example: Give a daily bonus of gold ingots
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
        player.sendMessage(ChatColor.GREEN + "Daily login bonus: " + ChatColor.GOLD + "5 Gold Ingots!");
    }

    /**
     * Helper method to safely retrieve a String from a ConfigurationSection with a default value.
     */
    private String getString(ConfigurationSection section, String key, String defaultValue) {
        return section.getString(key, defaultValue);
    }
}