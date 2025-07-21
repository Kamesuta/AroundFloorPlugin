package com.kamesuta.aroundfloor;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Config {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    // 設定値
    private int visibilityRange;
    private int sourceYMin;
    private int sourceYMax;
    private int displayYMin;
    private int displayYMax;
    private int yOffset;
    private int updateInterval;
    private boolean debug;
    
    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // 設定値を読み込み
        visibilityRange = config.getInt("aroundfloor.visibility-range", 10);
        sourceYMin = config.getInt("aroundfloor.source-y-min", -50);
        sourceYMax = config.getInt("aroundfloor.source-y-max", 319);
        displayYMin = config.getInt("aroundfloor.display-y-min", 50);
        displayYMax = config.getInt("aroundfloor.display-y-max", 419);
        yOffset = config.getInt("aroundfloor.y-offset", 100);
        updateInterval = config.getInt("aroundfloor.update-interval", 20);
        debug = config.getBoolean("aroundfloor.debug", false);
        
        // 設定値の検証
        validateConfig();
        
        if (debug) {
            plugin.getLogger().info("設定を読み込みました:");
            plugin.getLogger().info("  可視化範囲: " + visibilityRange + "ブロック");
            plugin.getLogger().info("  対象Y範囲: " + sourceYMin + "～" + sourceYMax);
            plugin.getLogger().info("  表示Y範囲: " + displayYMin + "～" + displayYMax);
            plugin.getLogger().info("  Yオフセット: " + yOffset);
            plugin.getLogger().info("  更新間隔: " + updateInterval + "tick");
        }
    }
    
    private void validateConfig() {
        if (visibilityRange <= 0) {
            plugin.getLogger().warning("可視化範囲は1以上である必要があります。デフォルト値(10)を使用します。");
            visibilityRange = 10;
        }
        
        if (sourceYMax <= sourceYMin) {
            plugin.getLogger().warning("対象Y座標の最大値は最小値より大きい必要があります。デフォルト値を使用します。");
            sourceYMin = -50;
            sourceYMax = 319;
        }
        
        if (displayYMax <= displayYMin) {
            plugin.getLogger().warning("表示Y座標の最大値は最小値より大きい必要があります。デフォルト値を使用します。");
            displayYMin = 50;
            displayYMax = 419;
        }
        
        if (updateInterval <= 0) {
            plugin.getLogger().warning("更新間隔は1以上である必要があります。デフォルト値(20)を使用します。");
            updateInterval = 20;
        }
    }
    
    // Getter methods
    public int getVisibilityRange() { return visibilityRange; }
    public int getSourceYMin() { return sourceYMin; }
    public int getSourceYMax() { return sourceYMax; }
    public int getDisplayYMin() { return displayYMin; }
    public int getDisplayYMax() { return displayYMax; }
    public int getYOffset() { return yOffset; }
    public int getUpdateInterval() { return updateInterval; }
    public boolean isDebug() { return debug; }
    
    // Y座標変換メソッド
    public int calculateDisplayY(int sourceY) {
        return sourceY + yOffset;
    }
    
    // 対象Y範囲内かチェック
    public boolean isInSourceRange(int y) {
        return y >= sourceYMin && y <= sourceYMax;
    }
    
    // 表示Y範囲内かチェック
    public boolean isInDisplayRange(int y) {
        return y >= displayYMin && y <= displayYMax;
    }
} 