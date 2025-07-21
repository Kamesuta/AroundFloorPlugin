package com.kamesuta.aroundfloor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockManager {
    private final AroundFloorPlugin plugin;
    private final Config config;
    
    // プレイヤーごとの可視化ブロック管理
    private final Map<UUID, Set<Location>> playerVisibleBlocks;
    
    // y=50～HEIGHTでの表示ブロック管理（どのプレイヤーが参照しているかをカウント）
    private final Map<Location, Set<UUID>> displayedBlocks;
    
    // プレイヤーの前回位置（最適化用）
    private final Map<UUID, Location> lastPlayerLocations;
    
    public BlockManager(AroundFloorPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.playerVisibleBlocks = new ConcurrentHashMap<>();
        this.displayedBlocks = new ConcurrentHashMap<>();
        this.lastPlayerLocations = new ConcurrentHashMap<>();
    }
    
    /**
     * プレイヤー周囲のブロックを更新
     */
    public void updateVisibleBlocks(Player player) {
        UUID playerId = player.getUniqueId();
        Location currentLocation = player.getLocation();
        
        // 移動距離チェック（最適化）
        Location lastLocation = lastPlayerLocations.get(playerId);
        if (lastLocation != null && currentLocation.distanceSquared(lastLocation) < 1.0) {
            return; // 1ブロック未満の移動は無視
        }
        
        lastPlayerLocations.put(playerId, currentLocation.clone());
        
        // 現在の可視化ブロックを取得
        Set<Location> currentVisibleBlocks = playerVisibleBlocks.computeIfAbsent(playerId, k -> new HashSet<>());
        
        // 新しい可視化範囲のブロックを計算
        Set<Location> newVisibleBlocks = calculateVisibleBlocks(currentLocation);
        
        // 新たに範囲内に入ったブロック
        Set<Location> blocksToAdd = new HashSet<>(newVisibleBlocks);
        blocksToAdd.removeAll(currentVisibleBlocks);
        
        // 範囲外に出たブロック
        Set<Location> blocksToRemove = new HashSet<>(currentVisibleBlocks);
        blocksToRemove.removeAll(newVisibleBlocks);
        
        // ブロックを追加
        for (Location sourceLocation : blocksToAdd) {
            copyBlockToDisplay(sourceLocation, playerId);
        }
        
        // ブロックを削除
        for (Location sourceLocation : blocksToRemove) {
            removeBlockFromDisplay(sourceLocation, playerId);
        }
        
        // プレイヤーの可視化ブロックリストを更新
        playerVisibleBlocks.put(playerId, newVisibleBlocks);
        
        if (config.isDebug()) {
            plugin.getLogger().info(player.getName() + "の可視化ブロック更新: 追加=" + blocksToAdd.size() + ", 削除=" + blocksToRemove.size());
        }
    }
    
    /**
     * プレイヤーの可視化範囲内のブロックを計算
     */
    private Set<Location> calculateVisibleBlocks(Location playerLocation) {
        Set<Location> visibleBlocks = new HashSet<>();
        World world = playerLocation.getWorld();
        
        if (world == null) return visibleBlocks;
        
        int range = config.getVisibilityRange();
        int centerX = playerLocation.getBlockX();
        int centerZ = playerLocation.getBlockZ();
        
        // 円形範囲でスキャン
        for (int x = centerX - range; x <= centerX + range; x++) {
            for (int z = centerZ - range; z <= centerZ + range; z++) {
                // 距離チェック
                double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
                if (distance > range) continue;
                
                // Y範囲でスキャン
                for (int y = config.getSourceYMin(); y <= config.getSourceYMax(); y++) {
                    Location sourceLocation = new Location(world, x, y, z);
                    Block block = world.getBlockAt(sourceLocation);
                    
                    // 空気ブロックは無視
                    if (block.getType() == Material.AIR) continue;
                    
                    // チャンクが読み込まれているかチェック
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    
                    visibleBlocks.add(sourceLocation);
                }
            }
        }
        
        return visibleBlocks;
    }
    
    /**
     * y=-50～HEIGHTからy=50～HEIGHTへのブロックコピー
     */
    private void copyBlockToDisplay(Location sourceLocation, UUID playerId) {
        World world = sourceLocation.getWorld();
        if (world == null) return;
        
        // 表示Y座標を計算
        int displayY = config.calculateDisplayY(sourceLocation.getBlockY());
        
        // 表示範囲外の場合は無視
        if (!config.isInDisplayRange(displayY)) return;
        
        Location displayLocation = new Location(world, sourceLocation.getX(), displayY, sourceLocation.getZ());
        
        // 既に他のプレイヤーが参照している場合はカウントを増やすだけ
        Set<UUID> referencingPlayers = displayedBlocks.computeIfAbsent(displayLocation, k -> new HashSet<>());
        referencingPlayers.add(playerId);
        
        // 初回の場合のみブロックを実際にコピー
        if (referencingPlayers.size() == 1) {
            Block sourceBlock = world.getBlockAt(sourceLocation);
            Block displayBlock = world.getBlockAt(displayLocation);
            
            // ブロックをコピー
            displayBlock.setType(sourceBlock.getType());
            if (sourceBlock.getBlockData() != null) {
                displayBlock.setBlockData(sourceBlock.getBlockData());
            }
            
            if (config.isDebug()) {
                plugin.getLogger().info("ブロックコピー: " + sourceLocation + " → " + displayLocation + " (Type: " + sourceBlock.getType() + ")");
            }
        }
    }
    
    /**
     * y=50～HEIGHTからのブロック削除
     */
    private void removeBlockFromDisplay(Location sourceLocation, UUID playerId) {
        World world = sourceLocation.getWorld();
        if (world == null) return;
        
        // 表示Y座標を計算
        int displayY = config.calculateDisplayY(sourceLocation.getBlockY());
        
        // 表示範囲外の場合は無視
        if (!config.isInDisplayRange(displayY)) return;
        
        Location displayLocation = new Location(world, sourceLocation.getX(), displayY, sourceLocation.getZ());
        
        Set<UUID> referencingPlayers = displayedBlocks.get(displayLocation);
        if (referencingPlayers == null) return;
        
        // プレイヤーの参照を削除
        referencingPlayers.remove(playerId);
        
        // 誰も参照していない場合はブロックを削除
        if (referencingPlayers.isEmpty()) {
            displayedBlocks.remove(displayLocation);
            
            Block displayBlock = world.getBlockAt(displayLocation);
            displayBlock.setType(Material.AIR);
            
            if (config.isDebug()) {
                plugin.getLogger().info("ブロック削除: " + displayLocation);
            }
        }
    }
    
    /**
     * プレイヤー参加時の初期化
     */
    public void onPlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();
        playerVisibleBlocks.put(playerId, new HashSet<>());
        lastPlayerLocations.put(playerId, player.getLocation().clone());
        
        if (config.isDebug()) {
            plugin.getLogger().info(player.getName() + "のブロック管理を初期化しました");
        }
    }
    
    /**
     * プレイヤー退出時のクリーンアップ
     */
    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        
        // プレイヤーが参照していたブロックをすべて削除
        Set<Location> visibleBlocks = playerVisibleBlocks.get(playerId);
        if (visibleBlocks != null) {
            for (Location sourceLocation : visibleBlocks) {
                removeBlockFromDisplay(sourceLocation, playerId);
            }
        }
        
        // プレイヤーのデータを削除
        playerVisibleBlocks.remove(playerId);
        lastPlayerLocations.remove(playerId);
        
        if (config.isDebug()) {
            plugin.getLogger().info(player.getName() + "のブロック管理をクリーンアップしました");
        }
    }
    
    /**
     * 範囲内判定
     */
    public boolean isWithinRange(Location playerLocation, Location blockLocation) {
        if (!playerLocation.getWorld().equals(blockLocation.getWorld())) {
            return false;
        }
        
        double distance = Math.sqrt(
            Math.pow(playerLocation.getX() - blockLocation.getX(), 2) +
            Math.pow(playerLocation.getZ() - blockLocation.getZ(), 2)
        );
        
        return distance <= config.getVisibilityRange();
    }
    
    /**
     * 統計情報を取得（デバッグ用）
     */
    public String getStatistics() {
        int totalVisibleBlocks = playerVisibleBlocks.values().stream()
            .mapToInt(Set::size)
            .sum();
        
        return String.format("プレイヤー数: %d, 総可視化ブロック数: %d, 表示ブロック数: %d",
            playerVisibleBlocks.size(), totalVisibleBlocks, displayedBlocks.size());
    }
    
    /**
     * 全データのクリーンアップ
     */
    public void cleanup() {
        // すべての表示ブロックを削除
        for (Location displayLocation : new HashSet<>(displayedBlocks.keySet())) {
            World world = displayLocation.getWorld();
            if (world != null) {
                Block block = world.getBlockAt(displayLocation);
                block.setType(Material.AIR);
            }
        }
        
        playerVisibleBlocks.clear();
        displayedBlocks.clear();
        lastPlayerLocations.clear();
        
        plugin.getLogger().info("ブロック管理システムをクリーンアップしました");
    }
} 