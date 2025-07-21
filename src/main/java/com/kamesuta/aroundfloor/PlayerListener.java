package com.kamesuta.aroundfloor;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final AroundFloorPlugin plugin;
    private final BlockManager blockManager;
    private final Config config;
    
    // プレイヤーの最後の更新時間（レート制限用）
    private final Map<UUID, Long> lastUpdateTime;
    
    public PlayerListener(AroundFloorPlugin plugin, BlockManager blockManager, Config config) {
        this.plugin = plugin;
        this.blockManager = blockManager;
        this.config = config;
        this.lastUpdateTime = new ConcurrentHashMap<>();
        
        // 定期的な更新タスクを開始
        startPeriodicUpdateTask();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // toがnullの場合は処理しない
        if (to == null) return;
        
        // ワールドが変わった場合は必ず更新
        boolean worldChanged = !from.getWorld().equals(to.getWorld());
        
        // 移動距離が小さい場合はスキップ（ワールド変更時は除く）
//        if (!worldChanged && from.distanceSquared(to) < 0.25) { // 0.5ブロック未満
//            return;
//        }
        
        // レート制限チェック
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerId);
        
//        if (!worldChanged && lastUpdate != null && (currentTime - lastUpdate) < 500) { // 0.5秒未満は無視
//            return;
//        }
        
        lastUpdateTime.put(playerId, currentTime);
        
        // 直接ブロック更新を実行（既にメインスレッド）
        try {
            blockManager.updateVisibleBlocks(player);
        } catch (Exception e) {
            plugin.getLogger().warning("プレイヤー " + player.getName() + " のブロック更新中にエラーが発生しました: " + e.getMessage());
            if (config.isDebug()) {
                e.printStackTrace();
            }
        }
        
        if (config.isDebug()) {
            plugin.getLogger().info(player.getName() + " が移動しました: " + 
                String.format("(%.1f, %.1f, %.1f) → (%.1f, %.1f, %.1f)", 
                    from.getX(), from.getY(), from.getZ(),
                    to.getX(), to.getY(), to.getZ()));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 直接プレイヤー初期化を実行（既にメインスレッド）
        try {
            blockManager.onPlayerJoin(player);
            blockManager.updateVisibleBlocks(player);
        } catch (Exception e) {
            plugin.getLogger().warning("プレイヤー " + player.getName() + " の参加処理中にエラーが発生しました: " + e.getMessage());
            if (config.isDebug()) {
                e.printStackTrace();
            }
        }
        
        if (config.isDebug()) {
            plugin.getLogger().info(player.getName() + " がサーバーに参加しました");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 直接クリーンアップを実行（既にメインスレッド）
        try {
            blockManager.onPlayerQuit(player);
            lastUpdateTime.remove(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("プレイヤー " + player.getName() + " の退出処理中にエラーが発生しました: " + e.getMessage());
            if (config.isDebug()) {
                e.printStackTrace();
            }
        }
        
        if (config.isDebug()) {
            plugin.getLogger().info(player.getName() + " がサーバーから退出しました");
        }
    }
    
    /**
     * 定期的な更新タスクを開始
     */
    private void startPeriodicUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // オンラインの全プレイヤーをチェック（メインスレッドで実行）
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        try {
                            blockManager.updateVisibleBlocks(player);
                        } catch (Exception e) {
                            plugin.getLogger().warning("プレイヤー " + player.getName() + " の定期更新中にエラーが発生しました: " + e.getMessage());
                            if (config.isDebug()) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    if (config.isDebug()) {
                        plugin.getLogger().info("定期更新実行: " + blockManager.getStatistics());
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("定期更新中にエラーが発生しました: " + e.getMessage());
                    if (config.isDebug()) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, config.getUpdateInterval()); // 1秒後に開始、設定間隔で実行
    }
    
    /**
     * 統計情報を出力（デバッグ用）
     */
    public void printStatistics() {
        if (config.isDebug()) {
            plugin.getLogger().info("=== AroundFloor統計情報 ===");
            plugin.getLogger().info(blockManager.getStatistics());
            plugin.getLogger().info("アクティブプレイヤー数: " + lastUpdateTime.size());
            plugin.getLogger().info("========================");
        }
    }
    
    /**
     * リスナーのクリーンアップ
     */
    public void cleanup() {
        lastUpdateTime.clear();
        plugin.getLogger().info("PlayerListenerをクリーンアップしました");
    }
} 