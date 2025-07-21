package com.kamesuta.aroundfloor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AroundFloorPlugin extends JavaPlugin {
    
    private Config config;
    private BlockManager blockManager;
    private PlayerListener playerListener;
    private BukkitTask statisticsTask;

    @Override
    public void onEnable() {
        try {
            getLogger().info("AroundFloorPlugin を開始しています...");
            
            // 設定を初期化
            config = new Config(this);
            getLogger().info("設定ファイルを読み込みました");
            
            // ブロック管理システムを初期化
            blockManager = new BlockManager(this, config);
            getLogger().info("ブロック管理システムを初期化しました");
            
            // プレイヤーリスナーを初期化・登録
            playerListener = new PlayerListener(this, blockManager, config);
            getServer().getPluginManager().registerEvents(playerListener, this);
            getLogger().info("プレイヤーイベントリスナーを登録しました");
            
            // 既にオンラインのプレイヤーを初期化
            for (Player player : getServer().getOnlinePlayers()) {
                blockManager.onPlayerJoin(player);
            }
            
            // 統計情報の定期出力タスクを開始（デバッグモード時のみ）
            if (config.isDebug()) {
                startStatisticsTask();
            }
            
            getLogger().info("AroundFloorPlugin が正常に開始されました!");
            getLogger().info("可視化範囲: " + config.getVisibilityRange() + "ブロック");
            getLogger().info("対象Y範囲: " + config.getSourceYMin() + "～" + config.getSourceYMax());
            getLogger().info("表示Y範囲: " + config.getDisplayYMin() + "～" + config.getDisplayYMax());
            
        } catch (Exception e) {
            getLogger().severe("AroundFloorPlugin の開始中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("AroundFloorPlugin を停止しています...");
            
            // 統計タスクを停止
            if (statisticsTask != null) {
                statisticsTask.cancel();
                statisticsTask = null;
            }
            
            // プレイヤーリスナーをクリーンアップ
            if (playerListener != null) {
                playerListener.cleanup();
            }
            
            // ブロック管理システムをクリーンアップ
            if (blockManager != null) {
                blockManager.cleanup();
            }
            
            getLogger().info("AroundFloorPlugin が正常に停止されました");
            
        } catch (Exception e) {
            getLogger().severe("AroundFloorPlugin の停止中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("aroundfloor")) {
            return false;
        }
        
        // 権限チェック
        if (!sender.hasPermission("aroundfloor.admin")) {
            sender.sendMessage("§c権限がありません。");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReloadCommand(sender);
            case "stats":
                return handleStatsCommand(sender);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                sender.sendMessage("§c不明なサブコマンドです: " + args[0]);
                sendHelpMessage(sender);
                return true;
        }
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        try {
            sender.sendMessage("§e設定を再読み込みしています...");
            
            // 設定を再読み込み
            config.loadConfig();
            
            // 統計タスクを再起動（デバッグモード変更に対応）
            if (statisticsTask != null) {
                statisticsTask.cancel();
                statisticsTask = null;
            }
            if (config.isDebug()) {
                startStatisticsTask();
            }
            
            sender.sendMessage("§a設定の再読み込みが完了しました！");
            sender.sendMessage("§f可視化範囲: §e" + config.getVisibilityRange() + "ブロック");
            sender.sendMessage("§f対象Y範囲: §e" + config.getSourceYMin() + "～" + config.getSourceYMax());
            sender.sendMessage("§f表示Y範囲: §e" + config.getDisplayYMin() + "～" + config.getDisplayYMax());
            
            getLogger().info(sender.getName() + " が設定を再読み込みしました");
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c設定の再読み込み中にエラーが発生しました: " + e.getMessage());
            getLogger().warning("設定再読み込みエラー: " + e.getMessage());
            return true;
        }
    }
    
    private boolean handleStatsCommand(CommandSender sender) {
        try {
            sender.sendMessage("§e=== AroundFloor統計情報 ===");
            sender.sendMessage("§f" + blockManager.getStatistics());
            sender.sendMessage("§fオンラインプレイヤー数: §e" + getServer().getOnlinePlayers().size());
            sender.sendMessage("§f可視化範囲: §e" + config.getVisibilityRange() + "ブロック");
            sender.sendMessage("§f更新間隔: §e" + config.getUpdateInterval() + "tick");
            sender.sendMessage("§fデバッグモード: §e" + (config.isDebug() ? "有効" : "無効"));
            sender.sendMessage("§e========================");
            
            if (playerListener != null) {
                playerListener.printStatistics();
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c統計情報の取得中にエラーが発生しました: " + e.getMessage());
            getLogger().warning("統計情報取得エラー: " + e.getMessage());
            return true;
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§e=== AroundFloorPlugin ヘルプ ===");
        sender.sendMessage("§f/aroundfloor reload §7- 設定を再読み込み");
        sender.sendMessage("§f/aroundfloor stats §7- 統計情報を表示");
        sender.sendMessage("§f/aroundfloor help §7- このヘルプを表示");
        sender.sendMessage("§7エイリアス: /af");
        sender.sendMessage("§e============================");
    }
    
    private void startStatisticsTask() {
        statisticsTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (playerListener != null) {
                playerListener.printStatistics();
            }
        }, 1200L, 1200L); // 1分後に開始、1分間隔で実行
    }
    
    // Getter methods for other classes
    public Config getPluginConfig() {
        return config;
    }
    
    public BlockManager getBlockManager() {
        return blockManager;
    }
    
    public PlayerListener getPlayerListener() {
        return playerListener;
    }
}
