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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Level;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages log level toggling and delivers log messages to subscribed players.
 */
public class LogToChatManager {

    private static final Set<Level> enabledLevels = ConcurrentHashMap.newKeySet();
    private static final Set<String> subscribedPlayers = ConcurrentHashMap.newKeySet();

    static {
        // 默认开启 ERROR 和 WARN 级别
        enabledLevels.add(Level.ERROR);
        enabledLevels.add(Level.WARN);
    }

    public static boolean isLevelEnabled(Level level) {
        return enabledLevels.contains(level);
    }

    public static void setLevelEnabled(Level level, boolean enabled) {
        if (enabled) {
            enabledLevels.add(level);
        } else {
            enabledLevels.remove(level);
        }
    }

    public static MutableComponent getEnabledLevelsComponent() {
        if (enabledLevels.isEmpty()) {
            return Component.translatable("cloudworks_api.console.no_levels")
                    .withStyle(ChatFormatting.RED);
        }

        MutableComponent result = Component.literal("");
        boolean first = true;
        for (Level level : enabledLevels) {
            if (!first) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.RESET));
            }
            result.append(
                    Component.translatable("cloudworks_api.console.log_level." + level.name().toLowerCase())
                            .withStyle(getLevelColor(level))
            );
            first = false;
        }
        return result;
    }

    public static void subscribePlayer(String playerName) {
        subscribedPlayers.add(playerName);
    }

    public static void unsubscribePlayer(String playerName) {
        subscribedPlayers.remove(playerName);
    }

    public static boolean isPlayerSubscribed(String playerName) {
        return subscribedPlayers.contains(playerName);
    }

    /**
     * 由 ChatAppender 调用，将日志消息发送到所有已订阅的 OP 玩家。
     */
    public static void processLogMessage(Level level, String formattedMessage) {
        if (!isLevelEnabled(level)) {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        MutableComponent chatMessage = buildChatMessage(level, formattedMessage);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2) && isPlayerSubscribed(player.getGameProfile().getName())) {
                player.sendSystemMessage(chatMessage);
            }
        }
    }

    private static MutableComponent buildChatMessage(Level level, String formattedMessage) {
        // 去除 ANSI 颜色代码
        String cleanMessage = LogFilter.stripAnsiCodes(formattedMessage);
        // 截断过长消息
        String truncatedMessage = LogFilter.truncateLogMessage(cleanMessage);

        MutableComponent message = Component.translatable("cloudworks_api.console.chat.prefix")
                .withStyle(ChatFormatting.GRAY);

        if (ConsoleSeekerConfig.isEnableTimestamp()) {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            message.append(Component.literal("[" + timestamp + "] ").withStyle(ChatFormatting.DARK_GRAY));
        }

        message.append(Component.literal("[")
                .append(Component.translatable("cloudworks_api.console.log_level." + level.name().toLowerCase())
                        .withStyle(getLevelColor(level), ChatFormatting.BOLD))
                .append("] "));

        message.append(Component.literal(truncatedMessage).withStyle(ChatFormatting.WHITE));

        return message;
    }

    private static ChatFormatting getLevelColor(Level level) {
        if (level == null) {
            return ChatFormatting.GRAY;
        }

        switch (level.getStandardLevel()) {
            case ERROR:
                return ChatFormatting.DARK_RED;
            case WARN:
                return ChatFormatting.GOLD;
            case INFO:
                return ChatFormatting.GREEN;
            default:
                return ChatFormatting.GRAY;
        }
    }
}