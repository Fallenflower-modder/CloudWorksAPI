/*
 * CloudWorks API - ConsoleSeeker Module
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.cloudworks.api.consoleseeker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ConsoleSeeker configuration stored in cloudworks/console_seeker/config.json.
 */
public class ConsoleSeekerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleSeekerConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.GAMEDIR.get().resolve("cloudworks").resolve("console_seeker");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    private static String version = "";
    private static boolean enableModule = true;
    private static boolean enableApi = false;
    private static boolean enableCommandForAnyOperator = true;
    private static String listType = "whitelist";
    private static final List<String> playerList = new ArrayList<>();
    private static int maxLogLength = 150;
    private static boolean enableTimestamp = false;

    private ConsoleSeekerConfig() {}

    // ======================== 文件读写 ========================

    /**
     * 加载配置文件。版本不对齐时记录旧值并用默认值重新生成。
     */
    public static void load() {
        String currentVersion = getCurrentVersion();

        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            LOGGER.error("CloudWorks API - Failed to create ConsoleSeeker config directory: {}", e.getMessage());
            return;
        }

        if (Files.exists(CONFIG_FILE)) {
            ConfigFileData data = null;
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                data = GSON.fromJson(reader, ConfigFileData.class);
            } catch (Exception e) {
                LOGGER.error("CloudWorks API - Failed to parse ConsoleSeeker config: {}", e.getMessage());
            }

            if (data != null && currentVersion.equals(data.version)) {
                applyFileData(data);
                LOGGER.info("CloudWorks API - ConsoleSeeker config loaded. enable_command_for_any_operator={}, list_type={}, player_list={}",
                        enableCommandForAnyOperator, listType, playerList);
            } else {
                String oldVersion = (data != null) ? data.version : "null";
                LOGGER.info("CloudWorks API - ConsoleSeeker config version mismatch (current: {}, file: {}). Regenerating with defaults.",
                        currentVersion, oldVersion);
                if (data != null) {
                    LOGGER.info("CloudWorks API - Old config values: enable_command_for_any_operator={}, list_type={}, player_list={}, max_log_length={}, enable_timestamp={}",
                            data.enableCommandForAnyOperator, data.listType, data.playerList,
                            data.maxLogLength, data.enableTimestamp);
                }
                resetToDefaults();
                version = currentVersion;
                save();
            }
        } else {
            resetToDefaults();
            version = currentVersion;
            save();
            LOGGER.info("CloudWorks API - ConsoleSeeker config created at {}", CONFIG_FILE);
        }
    }

    /**
     * 保存配置文件。
     */
    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            ConfigFileData data = new ConfigFileData();
            data.version = version;
            data.enableModule = enableModule;
            data.enableApi = enableApi;
            data.enableCommandForAnyOperator = enableCommandForAnyOperator;
            data.listType = listType;
            synchronized (playerList) {
                data.playerList = new ArrayList<>(playerList);
            }
            data.maxLogLength = maxLogLength;
            data.enableTimestamp = enableTimestamp;

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            LOGGER.error("CloudWorks API - Failed to save ConsoleSeeker config: {}", e.getMessage());
        }
    }

    // ======================== 权限校验 ========================

    /**
     * 检查命令源是否可以使用 /cloudworks console 指令。
     * 专用服务器后台始终可用；玩家需 OP 等级 2+，且根据 list_type 和 player_list 判断。
     */
    public static boolean canPlayerUseCommand(CommandSourceStack source) {
        // 专用服务器后台始终可用
        if (source.getEntity() == null) {
            return true;
        }

        // 需要 OP
        if (!source.hasPermission(2)) {
            return false;
        }

        // 如果向所有 OP 开放，直接通过
        if (enableCommandForAnyOperator) {
            return true;
        }

        // 检查 player_list
        if (source.getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            synchronized (playerList) {
                if ("whitelist".equals(listType)) {
                    return playerList.contains(playerName);
                } else {
                    return !playerList.contains(playerName);
                }
            }
        }

        return true;
    }

    // ======================== player_list 管理 ========================

    public static boolean addPlayerToList(String playerName) {
        synchronized (playerList) {
            if (playerList.contains(playerName)) {
                return false;
            }
            playerList.add(playerName);
            return true;
        }
    }

    public static boolean removePlayerFromList(String playerName) {
        synchronized (playerList) {
            return playerList.remove(playerName);
        }
    }

    public static List<String> getPlayerListCopy() {
        synchronized (playerList) {
            return new ArrayList<>(playerList);
        }
    }

    // ======================== Getters / Setters ========================

    public static boolean isEnableModule() {
        return enableModule;
    }

    public static boolean isEnableApi() {
        return enableApi;
    }

    public static boolean isEnableCommandForAnyOperator() {
        return enableCommandForAnyOperator;
    }

    public static String getListType() {
        return listType;
    }

    public static int getMaxLogLength() {
        return maxLogLength;
    }

    public static boolean isEnableTimestamp() {
        return enableTimestamp;
    }

    // ======================== 内部方法 ========================

    private static String getCurrentVersion() {
        return ModList.get().getModContainerById("cloudworks_api")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private static void applyFileData(ConfigFileData data) {
        version = data.version;
        enableModule = data.enableModule;
        enableApi = data.enableApi;
        enableCommandForAnyOperator = data.enableCommandForAnyOperator;
        listType = validateListType(data.listType);
        synchronized (playerList) {
            playerList.clear();
            if (data.playerList != null) {
                playerList.addAll(data.playerList);
            }
        }
        maxLogLength = Math.max(0, Math.min(data.maxLogLength, 1000));
        enableTimestamp = data.enableTimestamp;
    }

    private static void resetToDefaults() {
        version = "";
        enableModule = true;
        enableApi = false;
        enableCommandForAnyOperator = true;
        listType = "whitelist";
        synchronized (playerList) {
            playerList.clear();
        }
        maxLogLength = 150;
        enableTimestamp = false;
    }

    private static String validateListType(String type) {
        if ("blacklist".equals(type)) {
            return "blacklist";
        }
        return "whitelist";
    }

    // ======================== JSON 数据类 ========================

    @SuppressWarnings("FieldMayBeFinal")
    private static class ConfigFileData {
        String version = "";
        @SerializedName("enable_module")
        boolean enableModule = true;
        @SerializedName("enable_api")
        boolean enableApi = false;
        @SerializedName("enable_command_for_any_operator")
        boolean enableCommandForAnyOperator = true;
        @SerializedName("list_type")
        String listType = "whitelist";
        @SerializedName("player_list")
        List<String> playerList = new ArrayList<>();
        @SerializedName("max_log_length")
        int maxLogLength = 150;
        @SerializedName("enable_timestamp")
        boolean enableTimestamp = false;
    }
}