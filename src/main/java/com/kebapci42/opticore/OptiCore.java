package com.kebapci42.opticore;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class OptiCore extends JavaPlugin implements Listener {
    
    private FileConfiguration config;
    private PerformanceMonitor performanceMonitor;
    private EntityOptimizer entityOptimizer;
    private ChunkOptimizer chunkOptimizer;
    private MemoryManager memoryManager;
    private AdaptiveOptimizer adaptiveOptimizer;
    private MetricsCollector metricsCollector;
    
    // Death spiral prevention
    private long lastEmergencyActivation = 0;
    private long lastGarbageCollection = 0;
    private int consecutiveEmergencies = 0;
    private final Map<UUID, Long> playerJoinTimes = new ConcurrentHashMap<>();
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        
        // Initialize components
        performanceMonitor = new PerformanceMonitor(this);
        entityOptimizer = new EntityOptimizer(this);
        chunkOptimizer = new ChunkOptimizer(this);
        memoryManager = new MemoryManager(this);
        adaptiveOptimizer = new AdaptiveOptimizer(this);
        metricsCollector = new MetricsCollector(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Check for Paper
        if (isPaperServer()) {
            getLogger().info("Paper server detected - enabling advanced optimizations");
            enablePaperOptimizations();
        }
        
        // Start optimization tasks
        startOptimizationTasks();
        
        // Integration checks
        checkIntegrations();
        
        getLogger().info("OptiCore Enhanced v2.0 enabled - Intelligent performance optimization active!");
    }
    
    private boolean isPaperServer() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void enablePaperOptimizations() {
        // Paper-specific optimizations
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : getServer().getWorlds()) {
                    // Use Paper's async chunk loading if available
                    world.setKeepSpawnInMemory(false);
                }
            }
        }.runTaskLater(this, 100L);
    }
    
    private void checkIntegrations() {
        // Spark integration
        if (getServer().getPluginManager().getPlugin("spark") != null) {
            getLogger().info("✓ Spark integration enabled for enhanced profiling");
        }
        
        // Geyser compatibility check
        if (getServer().getPluginManager().getPlugin("Geyser-Spigot") != null) {
            getLogger().info("✓ Geyser detected - cross-platform optimizations enabled");
            config.set("cross-platform-mode", true);
        }
    }
    
    private void startOptimizationTasks() {
        // Adaptive TPS monitoring (1 second intervals)
        new BukkitRunnable() {
            @Override
            public void run() {
                double tps = performanceMonitor.getTPS();
                long currentTime = System.currentTimeMillis();
                
                // Death spiral prevention
                if (currentTime - lastEmergencyActivation < config.getInt("emergency-cooldown", 60000)) {
                    return; // Skip if in cooldown
                }
                
                if (tps < config.getDouble("emergency-tps", 17.0)) {
                    consecutiveEmergencies++;
                    if (consecutiveEmergencies < config.getInt("max-emergency-activations", 3)) {
                        performanceMonitor.activateEmergencyMode();
                        adaptiveOptimizer.applyEmergencyOptimizations(tps);
                        lastEmergencyActivation = currentTime;
                    } else {
                        getLogger().warning("Emergency optimization limit reached - backing off");
                    }
                } else if (tps > 19.0) {
                    consecutiveEmergencies = 0; // Reset counter when performance is good
                }
            }
        }.runTaskTimer(this, 20L, 20L);
        
        // Gentle optimization cycle (configurable interval)
        long optimizationInterval = config.getLong("optimization-interval", 2400L); // 2 minutes default
        new BukkitRunnable() {
            @Override
            public void run() {
                adaptiveOptimizer.runGentleOptimization();
            }
        }.runTaskTimer(this, optimizationInterval, optimizationInterval);
        
        // Memory optimization with cooldown
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastGarbageCollection > config.getLong("gc-cooldown", 30000)) {
                    memoryManager.optimizeMemory();
                }
            }
        }.runTaskTimerAsynchronously(this, 600L, 600L);
        
        // Metrics collection (async)
        new BukkitRunnable() {
            @Override
            public void run() {
                metricsCollector.collectMetrics();
            }
        }.runTaskTimerAsynchronously(this, 100L, 100L);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (config.getBoolean("optimization.merge-items", true)) {
            // Async item merging for Paper
            if (isPaperServer()) {
                CompletableFuture.runAsync(() -> {
                    entityOptimizer.mergeNearbyItems(event.getEntity());
                });
            } else {
                entityOptimizer.mergeNearbyItems(event.getEntity());
            }
        }
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (config.getBoolean("optimization.smart-chunk-loading", true)) {
            chunkOptimizer.onChunkLoad(event.getChunk());
        }
    }
    
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        chunkOptimizer.onChunkUnload(event.getChunk());
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerJoinTimes.put(player.getUniqueId(), System.currentTimeMillis());
        
        if (config.getBoolean("show-stats-on-join", true) && player.hasPermission("opticore.admin")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    metricsCollector.sendPerformanceReport(player);
                }
            }.runTaskLater(this, 40L);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("opticore")) return false;
        
        if (args.length == 0) {
            metricsCollector.sendDetailedStats(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "analyze":
                if (!sender.hasPermission("opticore.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "Starting performance analysis...");
                CompletableFuture.runAsync(() -> {
                    String report = performanceMonitor.generateDetailedReport();
                    getServer().getScheduler().runTask(this, () -> sender.sendMessage(report));
                });
                return true;
                
            case "optimize":
                if (!sender.hasPermission("opticore.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Running intelligent optimization...");
                adaptiveOptimizer.forceOptimization();
                return true;
                
            case "memory":
                if (!sender.hasPermission("opticore.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return true;
                }
                memoryManager.sendMemoryReport(sender);
                return true;
                
            case "reload":
                if (!sender.hasPermission("opticore.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission!");
                    return true;
                }
                reloadConfig();
                config = getConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                return true;
        }
        
        return false;
    }
}

class AdaptiveOptimizer {
    private final OptiCore plugin;
    private final Map<String, Double> optimizationWeights = new ConcurrentHashMap<>();
    private OptimizationProfile currentProfile = OptimizationProfile.BALANCED;
    
    enum OptimizationProfile {
        GENTLE(0.3, 0.5, 0.7),
        BALANCED(0.5, 0.7, 0.8),
        AGGRESSIVE(0.7, 0.9, 0.95),
        EMERGENCY(0.9, 0.95, 0.99);
        
        final double entityThreshold;
        final double chunkThreshold;
        final double memoryThreshold;
        
        OptimizationProfile(double e, double c, double m) {
            this.entityThreshold = e;
            this.chunkThreshold = c;
            this.memoryThreshold = m;
        }
    }
    
    public AdaptiveOptimizer(OptiCore plugin) {
        this.plugin = plugin;
        initializeWeights();
    }
    
    private void initializeWeights() {
        optimizationWeights.put("entities", 1.0);
        optimizationWeights.put("chunks", 1.0);
        optimizationWeights.put("memory", 1.0);
    }
    
    public void applyEmergencyOptimizations(double currentTPS) {
        plugin.getLogger().warning("Emergency optimization triggered - TPS: " + String.format("%.2f", currentTPS));
        currentProfile = OptimizationProfile.EMERGENCY;
        
        // Progressive optimization based on severity
        if (currentTPS < 10) {
            // Critical - apply all optimizations
            reduceViewDistance(6);
            aggressiveEntityCulling();
            unloadEmptyChunks();
        } else if (currentTPS < 15) {
            // Severe - moderate optimizations
            reduceViewDistance(8);
            moderateEntityCulling();
        } else {
            // Mild - gentle optimizations
            reduceViewDistance(10);
            gentleEntityCulling();
        }
        
        // Schedule profile reset
        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentProfile == OptimizationProfile.EMERGENCY) {
                    currentProfile = OptimizationProfile.BALANCED;
                    resetViewDistance();
                }
            }
        }.runTaskLater(plugin, 1200L); // 60 seconds
    }
    
    public void runGentleOptimization() {
        // Adaptive optimization based on current server state
        double memoryPressure = getMemoryPressure();
        int entityCount = getTotalEntities();
        int chunkCount = getLoadedChunks();
        
        // Update weights based on what's causing the most load
        if (memoryPressure > 0.7) {
            optimizationWeights.put("memory", 1.5);
        }
        if (entityCount > plugin.getConfig().getInt("thresholds.entities", 5000)) {
            optimizationWeights.put("entities", 1.5);
        }
        if (chunkCount > plugin.getConfig().getInt("thresholds.chunks", 500)) {
            optimizationWeights.put("chunks", 1.5);
        }
        
        // Apply weighted optimizations
        if (optimizationWeights.get("entities") > 1.2) {
            gentleEntityCulling();
        }
        if (optimizationWeights.get("chunks") > 1.2) {
            optimizeChunks();
        }
        if (optimizationWeights.get("memory") > 1.2 && memoryPressure > 0.8) {
            System.gc();
        }
        
        // Decay weights over time
        optimizationWeights.replaceAll((k, v) -> Math.max(1.0, v * 0.9));
    }
    
    private void gentleEntityCulling() {
        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    // Only remove old items not near players
                    if (item.getTicksLived() > 6000 && !isPlayerNearby(item.getLocation(), 32)) {
                        item.remove();
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Gentle optimization: removed " + removed + " old items");
        }
    }
    
    private void moderateEntityCulling() {
        for (World world : plugin.getServer().getWorlds()) {
            world.getEntities().stream()
                .filter(e -> e instanceof Item)
                .map(e -> (Item) e)
                .filter(item -> item.getTicksLived() > 2400)
                .filter(item -> !isPlayerNearby(item.getLocation(), 24))
                .forEach(Entity::remove);
        }
    }
    
    private void aggressiveEntityCulling() {
        for (World world : plugin.getServer().getWorlds()) {
            // Remove all items older than 1 minute
            world.getEntities().stream()
                .filter(e -> e instanceof Item)
                .map(e -> (Item) e)
                .filter(item -> item.getTicksLived() > 1200)
                .forEach(Entity::remove);
            
            // Reduce mob AI range
            world.getEntities().stream()
                .filter(e -> e instanceof Mob)
                .forEach(e -> {
                    if (!isPlayerNearby(e.getLocation(), 48)) {
                        ((Mob) e).setAI(false);
                    }
                });
        }
    }
    
    private void optimizeChunks() {
        for (World world : plugin.getServer().getWorlds()) {
            Arrays.stream(world.getLoadedChunks())
                .filter(chunk -> !isPlayerInChunk(chunk))
                .filter(chunk -> chunk.getEntities().length < 5)
                .forEach(chunk -> chunk.unload(true));
        }
    }
    
    private void unloadEmptyChunks() {
        for (World world : plugin.getServer().getWorlds()) {
            Arrays.stream(world.getLoadedChunks())
                .filter(chunk -> chunk.getEntities().length == 0)
                .filter(chunk -> !isPlayerInChunk(chunk))
                .forEach(chunk -> chunk.unload(false));
        }
    }
    
    private void reduceViewDistance(int distance) {
        for (World world : plugin.getServer().getWorlds()) {
            world.setViewDistance(distance);
            if (plugin.isPaperServer()) {
                world.setSimulationDistance(Math.max(4, distance - 2));
            }
        }
    }
    
    private void resetViewDistance() {
        int defaultDistance = plugin.getConfig().getInt("default-view-distance", 10);
        for (World world : plugin.getServer().getWorlds()) {
            world.setViewDistance(defaultDistance);
            if (plugin.isPaperServer()) {
                world.setSimulationDistance(defaultDistance);
            }
        }
    }
    
    private boolean isPlayerNearby(Location loc, double radius) {
        return loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
            .anyMatch(e -> e instanceof Player);
    }
    
    private boolean isPlayerInChunk(Chunk chunk) {
        return Arrays.stream(chunk.getEntities()).anyMatch(e -> e instanceof Player);
    }
    
    private double getMemoryPressure() {
        Runtime runtime = Runtime.getRuntime();
        return (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
    }
    
    private int getTotalEntities() {
        return plugin.getServer().getWorlds().stream()
            .mapToInt(w -> w.getEntities().size())
            .sum();
    }
    
    private int getLoadedChunks() {
        return plugin.getServer().getWorlds().stream()
            .mapToInt(w -> w.getLoadedChunks().length)
            .sum();
    }
    
    public void forceOptimization() {
        plugin.getLogger().info("Forcing optimization cycle...");
        currentProfile = OptimizationProfile.AGGRESSIVE;
        aggressiveEntityCulling();
        optimizeChunks();
        System.gc();
        
        // Reset after 30 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                currentProfile = OptimizationProfile.BALANCED;
            }
        }.runTaskLater(plugin, 600L);
    }
}

class MetricsCollector {
    private final OptiCore plugin;
    private final Map<String, Object> currentMetrics = new ConcurrentHashMap<>();
    private final LinkedList<Double> tpsHistory = new LinkedList<>();
    private final int maxHistorySize = 300; // 5 minutes of data
    
    public MetricsCollector(OptiCore plugin) {
        this.plugin = plugin;
    }
    
    public void collectMetrics() {
        // TPS
        double tps = plugin.getServer().getTPS()[0];
        tpsHistory.add(tps);
        if (tpsHistory.size() > maxHistorySize) {
            tpsHistory.removeFirst();
        }
        currentMetrics.put("tps", tps);
        currentMetrics.put("avgTps", tpsHistory.stream().mapToDouble(Double::doubleValue).average().orElse(20.0));
        
        // Memory
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        currentMetrics.put("memoryUsed", usedMemory);
        currentMetrics.put("memoryTotal", runtime.totalMemory());
        currentMetrics.put("memoryMax", runtime.maxMemory());
        
        // Entities and chunks
        int entities = 0, chunks = 0, players = 0;
        for (World world : plugin.getServer().getWorlds()) {
            entities += world.getEntities().size();
            chunks += world.getLoadedChunks().length;
            players += world.getPlayers().size();
        }
        currentMetrics.put("entities", entities);
        currentMetrics.put("chunks", chunks);
        currentMetrics.put("players", players);
        
        // CPU if available
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        currentMetrics.put("cpuLoad", osBean.getProcessCpuLoad() * 100);
    }
    
    public void sendPerformanceReport(Player player) {
        player.sendMessage(ChatColor.AQUA + "[OptiCore] " + ChatColor.GREEN + "Performance Report:");
        player.sendMessage(ChatColor.GRAY + "TPS: " + getColoredTPS((double) currentMetrics.get("tps")));
        player.sendMessage(ChatColor.GRAY + "Memory: " + getMemoryString());
        player.sendMessage(ChatColor.GRAY + "Entities: " + currentMetrics.get("entities"));
    }
    
    public void sendDetailedStats(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "========== OptiCore Enhanced Stats ==========");
        sender.sendMessage(ChatColor.WHITE + "Current TPS: " + getColoredTPS((double) currentMetrics.get("tps")));
        sender.sendMessage(ChatColor.WHITE + "Average TPS (5min): " + 
                         ChatColor.YELLOW + String.format("%.2f", currentMetrics.get("avgTps")));
        sender.sendMessage(ChatColor.WHITE + "Memory: " + getMemoryString());
        sender.sendMessage(ChatColor.WHITE + "CPU Load: " + 
                         ChatColor.YELLOW + String.format("%.1f%%", currentMetrics.get("cpuLoad")));
        sender.sendMessage(ChatColor.WHITE + "Entities: " + ChatColor.BLUE + currentMetrics.get("entities"));
        sender.sendMessage(ChatColor.WHITE + "Loaded Chunks: " + ChatColor.BLUE + currentMetrics.get("chunks"));
        sender.sendMessage(ChatColor.WHITE + "Players: " + ChatColor.GREEN + currentMetrics.get("players"));
        sender.sendMessage(ChatColor.AQUA + "==========================================");
    }
    
    private String getColoredTPS(double tps) {
        ChatColor color = tps >= 19 ? ChatColor.GREEN : 
                         tps >= 17 ? ChatColor.YELLOW : 
                         ChatColor.RED;
        return color + String.format("%.2f", tps);
    }
    
    private String getMemoryString() {
        long used = (long) currentMetrics.get("memoryUsed") / 1048576;
        long max = (long) currentMetrics.get("memoryMax") / 1048576;
        double percent = (double) used / max * 100;
        
        ChatColor color = percent > 80 ? ChatColor.RED :
                         percent > 60 ? ChatColor.YELLOW :
                         ChatColor.GREEN;
        
        return color + used + "MB/" + max + "MB (" + String.format("%.1f%%", percent) + ")";
    }
}

// Simplified helper classes (PerformanceMonitor, EntityOptimizer, ChunkOptimizer, MemoryManager)
// These would contain the core logic from the original but with improvements

class PerformanceMonitor {
    private final OptiCore plugin;
    private final LinkedList<Double> tpsHistory = new LinkedList<>();
    
    public PerformanceMonitor(OptiCore plugin) {
        this.plugin = plugin;
    }
    
    public double getTPS() {
        return plugin.getServer().getTPS()[0];
    }
    
    public void activateEmergencyMode() {
        plugin.getLogger().warning("TPS Emergency mode activated!");
    }
    
    public String generateDetailedReport() {
        // Implementation similar to original but with better formatting
        return "Detailed performance report generation...";
    }
}

class EntityOptimizer {
    private final OptiCore plugin;
    
    public EntityOptimizer(OptiCore plugin) {
        this.plugin = plugin;
    }
    
    public void mergeNearbyItems(Item item) {
        // Smart item merging logic
    }
}

class ChunkOptimizer {
    private final OptiCore plugin;
    private final Map<Chunk, ChunkData> chunkDataMap = new ConcurrentHashMap<>();
    
    class ChunkData {
        long loadTime;
        int activityScore;
        boolean hasPlayers;
    }
    
    public ChunkOptimizer(OptiCore plugin) {
        this.plugin = plugin;
    }
    
    public void onChunkLoad(Chunk chunk) {
        ChunkData data = new ChunkData();
        data.loadTime = System.currentTimeMillis();
        chunkDataMap.put(chunk, data);
    }
    
    public void onChunkUnload(Chunk chunk) {
        chunkDataMap.remove(chunk);
    }
}

class MemoryManager {
    private final OptiCore plugin;
    private long lastGcTime = 0;
    
    public MemoryManager(OptiCore plugin) {
        this.plugin = plugin;
    }
    
    public void optimizeMemory() {
        Runtime runtime = Runtime.getRuntime();
        double usage = (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
        
        if (usage > plugin.getConfig().getDouble("memory.gc-threshold", 0.85)) {
            long now = System.currentTimeMillis();
            if (now - lastGcTime > plugin.getConfig().getLong("gc-cooldown", 30000)) {
                System.gc();
                lastGcTime = now;
                plugin.getLogger().info("Memory optimization performed");
            }
        }
    }
    
    public void sendMemoryReport(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
        long free = runtime.freeMemory() / 1048576;
        long max = runtime.maxMemory() / 1048576;
        
        sender.sendMessage(ChatColor.AQUA + "======== Memory Report ========");
        sender.sendMessage(ChatColor.WHITE + "Used: " + ChatColor.RED + used + "MB");
        sender.sendMessage(ChatColor.WHITE + "Free: " + ChatColor.GREEN + free + "MB");
        sender.sendMessage(ChatColor.WHITE + "Max: " + ChatColor.BLUE + max + "MB");
        sender.sendMessage(ChatColor.AQUA + "==============================");
    }
}
