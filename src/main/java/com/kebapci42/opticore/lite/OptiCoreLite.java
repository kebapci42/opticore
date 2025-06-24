package com.kebapci42.opticore.lite;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;

import java.util.List;

public class OptiCoreLite extends JavaPlugin implements Listener {
    
    private FileConfiguration config;
    private int optimizedItems = 0;
    private int emergencyActivations = 0;
    private long lastEmergencyTime = 0;
    private long lastGcTime = 0;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        
        getServer().getPluginManager().registerEvents(this, this);
        
        // Much gentler optimization - every 2 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                optimizeEntities();
            }
        }.runTaskTimer(this, 2400L, 2400L); // Every 2 minutes
        
        getLogger().info("OptiCore Lite v1.0 enabled - Performance optimization active!");
    }
    
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (config.getBoolean("merge-items", true)) {
            if (mergeNearbyItems(event.getEntity())) {
                optimizedItems++;
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("opticore.admin")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Runtime runtime = Runtime.getRuntime();
                    long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
                    long maxMemory = runtime.maxMemory() / 1048576;
                    
                    player.sendMessage(ChatColor.AQUA + "[OptiCore] " + ChatColor.GREEN + 
                                     "Performance monitoring active!");
                    player.sendMessage(ChatColor.GRAY + "Memory: " + usedMemory + "MB / " + maxMemory + "MB");
                }
            }.runTaskLater(this, 40L);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("opticore")) {
            
            if (args.length == 0 || args[0].equalsIgnoreCase("stats")) {
                displayStats(sender);
                return true;
            }
            
            if (args[0].equalsIgnoreCase("optimize")) {
                if (!sender.hasPermission("opticore.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return true;
                }
                
                sender.sendMessage(ChatColor.YELLOW + "Running manual optimization...");
                int removed = forceOptimization();
                sender.sendMessage(ChatColor.GREEN + "Optimization complete! Removed " + removed + " entities");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("memory")) {
                if (!sender.hasPermission("opticore.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return true;
                }
                
                displayMemoryInfo(sender);
                return true;
            }
            
            if (args[0].equalsIgnoreCase("gc")) {
                if (!sender.hasPermission("opticore.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return true;
                }
                
                long now = System.currentTimeMillis();
                if (now - lastGcTime > 30000) { // 30 second cooldown
                    sender.sendMessage(ChatColor.YELLOW + "Running garbage collection...");
                    System.gc();
                    lastGcTime = now;
                    sender.sendMessage(ChatColor.GREEN + "Garbage collection completed!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Please wait before running GC again!");
                }
                return true;
            }
        }
        return false;
    }
    
    private void displayStats(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "========== OptiCore Stats ==========");
        sender.sendMessage(ChatColor.WHITE + "Items Optimized: " + ChatColor.GREEN + optimizedItems);
        sender.sendMessage(ChatColor.WHITE + "Emergency Activations: " + ChatColor.YELLOW + emergencyActivations);
        sender.sendMessage(ChatColor.WHITE + "Total Entities: " + ChatColor.BLUE + getTotalEntities());
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
        long maxMemory = runtime.maxMemory() / 1048576;
        double memoryPercent = (double) usedMemory / maxMemory * 100;
        
        String memoryColor = memoryPercent > 80 ? ChatColor.RED.toString() : 
                           memoryPercent > 60 ? ChatColor.YELLOW.toString() : 
                           ChatColor.GREEN.toString();
        
        sender.sendMessage(ChatColor.WHITE + "Memory: " + memoryColor + 
                          usedMemory + "MB / " + maxMemory + "MB (" + 
                          String.format("%.1f", memoryPercent) + "%)");
        sender.sendMessage(ChatColor.AQUA + "==================================");
    }
    
    private void displayMemoryInfo(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
        long freeMemory = runtime.freeMemory() / 1048576;
        long maxMemory = runtime.maxMemory() / 1048576;
        
        sender.sendMessage(ChatColor.AQUA + "======== Memory Information ========");
        sender.sendMessage(ChatColor.WHITE + "Used: " + ChatColor.RED + usedMemory + "MB");
        sender.sendMessage(ChatColor.WHITE + "Free: " + ChatColor.GREEN + freeMemory + "MB");
        sender.sendMessage(ChatColor.WHITE + "Max: " + ChatColor.BLUE + maxMemory + "MB");
        sender.sendMessage(ChatColor.WHITE + "Usage: " + ChatColor.YELLOW + 
                          (usedMemory * 100 / maxMemory) + "%");
        
        // Memory recommendations
        if (usedMemory * 100 / maxMemory > 85) {
            sender.sendMessage(ChatColor.RED + "⚠ Memory usage is very high!");
            sender.sendMessage(ChatColor.YELLOW + "Recommendation: Increase server RAM or reduce plugins");
        } else if (usedMemory * 100 / maxMemory > 70) {
            sender.sendMessage(ChatColor.YELLOW + "⚠ Memory usage is getting high");
        } else {
            sender.sendMessage(ChatColor.GREEN + "✓ Memory usage is healthy");
        }
        
        sender.sendMessage(ChatColor.AQUA + "==================================");
    }
    
    public int getTotalEntities() {
        return getServer().getWorlds().stream()
            .mapToInt(world -> world.getEntities().size())
            .sum();
    }
    
    private boolean mergeNearbyItems(Item item) {
        if (item == null || !item.isValid()) return false;
        
        try {
            List<Entity> nearby = item.getNearbyEntities(2.0, 2.0, 2.0);
            for (Entity entity : nearby) {
                if (entity instanceof Item && entity != item) {
                    Item nearbyItem = (Item) entity;
                    if (nearbyItem.getItemStack().isSimilar(item.getItemStack())) {
                        int totalAmount = item.getItemStack().getAmount() + 
                                        nearbyItem.getItemStack().getAmount();
                        if (totalAmount <= item.getItemStack().getMaxStackSize()) {
                            item.getItemStack().setAmount(totalAmount);
                            nearbyItem.remove();
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle any errors
        }
        return false;
    }
    
    private void optimizeEntities() {
        int removed = 0;
        int itemCount = 0;
        
        for (World world : getServer().getWorlds()) {
            List<Entity> entities = world.getEntities();
            for (Entity entity : entities) {
                if (entity instanceof Item) {
                    itemCount++;
                    Item item = (Item) entity;
                    // Only remove very old items (10 minutes)
                    if (item.getTicksLived() > 12000) {
                        item.remove();
                        removed++;
                    }
                }
            }
        }
        
        if (removed > 0) {
            getLogger().info("Gentle optimization: removed " + removed + " very old items (" + itemCount + " total items on server)");
        }
        
        // Very gentle memory check - only if memory is critically high
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
        
        if (memoryUsage > 0.9) { // Only if above 90%
            long now = System.currentTimeMillis();
            if (now - lastEmergencyTime > 60000) { // 1 minute cooldown
                getLogger().warning("Critical memory usage detected: " + String.format("%.1f", memoryUsage * 100) + "%");
                emergencyActivations++;
                lastEmergencyTime = now;
                
                // Only GC if we haven't done it recently
                if (now - lastGcTime > 30000) {
                    System.gc();
                    lastGcTime = now;
                    getLogger().info("Emergency garbage collection completed");
                }
            }
        }
    }
    
    private int forceOptimization() {
        int removed = 0;
        
        for (World world : getServer().getWorlds()) {
            List<Entity> entities = world.getEntities();
            for (Entity entity : entities) {
                if (!(entity instanceof Player)) {
                    if (entity instanceof Item) {
                        Item item = (Item) entity;
                        if (item.getTicksLived() > 2400) { // 2 minutes
                            item.remove();
                            removed++;
                        }
                    }
                }
            }
        }
        
        // Only GC if we haven't recently
        long now = System.currentTimeMillis();
        if (now - lastGcTime > 30000) {
            System.gc();
            lastGcTime = now;
        }
        
        return removed;
    }
}
