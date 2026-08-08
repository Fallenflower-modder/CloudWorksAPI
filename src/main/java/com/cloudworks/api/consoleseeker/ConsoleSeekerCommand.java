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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;

import java.util.List;

/**
 * ConsoleSeeker command handler for /cloudworks console and /cloudworks config console.
 */
public class ConsoleSeekerCommand {

    private ConsoleSeekerCommand() {}

    // ======================== /cloudworks console 权限检查 ========================

    /**
     * Brigadier 的 requires 谓词，用于 /cloudworks console 子命令的权限控制。
     */
    public static boolean canUseConsole(CommandSourceStack source) {
        return ConsoleSeekerConfig.canPlayerUseCommand(source);
    }

    /**
     * 检查 ConsoleSeeker 模块是否启用，未启用时向玩家发送警告并返回 false。
     */
    private static boolean requireModuleEnabled(CommandSourceStack source) {
        if (!ConsoleSeekerConfig.isEnableModule()) {
            source.sendFailure(
                    Component.translatable("cloudworks_api.console.module_disabled")
                            .withStyle(ChatFormatting.RED)
            );
            return false;
        }
        return true;
    }

    // ======================== /cloudworks console <level> <on|off> ========================

    /**
     * 执行 /cloudworks console &lt;level&gt; &lt;on|off&gt; 命令。
     */
    public static int executeConsole(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!requireModuleEnabled(source)) return 0;
        String levelStr = StringArgumentType.getString(context, "level").toUpperCase();
        String action = StringArgumentType.getString(context, "action").toLowerCase();

        Level level = switch (levelStr) {
            case "INFO" -> Level.INFO;
            case "WARN" -> Level.WARN;
            case "ERROR" -> Level.ERROR;
            default -> null;
        };

        if (level == null) {
            context.getSource().sendFailure(
                    Component.translatable("cloudworks_api.console.invalid_level", levelStr)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        if (!"on".equals(action) && !"off".equals(action)) {
            context.getSource().sendFailure(
                    Component.translatable("cloudworks_api.console.invalid_action")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        boolean enable = "on".equals(action);
        LogToChatManager.setLevelEnabled(level, enable);

        // 订阅/取消订阅当前玩家
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            String playerName = player.getGameProfile().getName();
            if (enable) {
                LogToChatManager.subscribePlayer(playerName);
            } else {
                boolean hasOtherEnabledLevels = false;
                for (Level lvl : new Level[]{Level.INFO, Level.WARN, Level.ERROR}) {
                    if (lvl != level && LogToChatManager.isLevelEnabled(lvl)) {
                        hasOtherEnabledLevels = true;
                        break;
                    }
                }
                if (!hasOtherEnabledLevels) {
                    LogToChatManager.unsubscribePlayer(playerName);
                }
            }
        }

        MutableComponent levelDisplay = Component.translatable("cloudworks_api.console.log_level." + levelStr.toLowerCase())
                .withStyle(getTextColor(level));

        MutableComponent actionDisplay = Component.translatable(
                enable ? "cloudworks_api.console.toggle.on" : "cloudworks_api.console.toggle.off"
        ).withStyle(enable ? ChatFormatting.GREEN : ChatFormatting.RED);

        MutableComponent statusDisplay = LogToChatManager.getEnabledLevelsComponent();

        context.getSource().sendSuccess(() ->
                Component.translatable("cloudworks_api.console.success", levelDisplay, actionDisplay, statusDisplay),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    // ======================== /cloudworks config console player_list add ========================

    public static int executeConfigPlayerListAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!requireModuleEnabled(source)) return 0;
        String playerName = StringArgumentType.getString(context, "player_name");
        String listType = ConsoleSeekerConfig.getListType();
        String listTypeDisplayKey = "whitelist".equals(listType)
                ? "cloudworks_api.console.config.player_list.list_type_whitelist"
                : "cloudworks_api.console.config.player_list.list_type_blacklist";

        if (ConsoleSeekerConfig.addPlayerToList(playerName)) {
            ConsoleSeekerConfig.save();
            context.getSource().sendSuccess(() ->
                    Component.translatable("cloudworks_api.console.config.player_list.add_success",
                            playerName,
                            Component.translatable(listTypeDisplayKey)),
                    true
            );
        } else {
            context.getSource().sendSuccess(() ->
                    Component.translatable("cloudworks_api.console.config.player_list.add_duplicate",
                            playerName,
                            Component.translatable(listTypeDisplayKey)),
                    false
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    // ======================== /cloudworks config console player_list remove ========================

    public static int executeConfigPlayerListRemove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!requireModuleEnabled(source)) return 0;
        String playerName = StringArgumentType.getString(context, "player_name");
        String listType = ConsoleSeekerConfig.getListType();
        String listTypeDisplayKey = "whitelist".equals(listType)
                ? "cloudworks_api.console.config.player_list.list_type_whitelist"
                : "cloudworks_api.console.config.player_list.list_type_blacklist";

        if (ConsoleSeekerConfig.removePlayerFromList(playerName)) {
            ConsoleSeekerConfig.save();
            context.getSource().sendSuccess(() ->
                    Component.translatable("cloudworks_api.console.config.player_list.remove_success",
                            playerName,
                            Component.translatable(listTypeDisplayKey)),
                    true
            );
        } else {
            context.getSource().sendSuccess(() ->
                    Component.translatable("cloudworks_api.console.config.player_list.remove_not_found",
                            playerName,
                            Component.translatable(listTypeDisplayKey)),
                    false
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    // ======================== /cloudworks config console player_list query ========================

    public static int executeConfigPlayerListQuery(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!requireModuleEnabled(source)) return 0;
        String listType = ConsoleSeekerConfig.getListType();
        String listTypeDisplayKey = "whitelist".equals(listType)
                ? "cloudworks_api.console.config.player_list.list_type_whitelist"
                : "cloudworks_api.console.config.player_list.list_type_blacklist";
        MutableComponent listTypeComponent = Component.translatable(listTypeDisplayKey);

        List<String> list = ConsoleSeekerConfig.getPlayerListCopy();

        context.getSource().sendSuccess(() ->
                Component.translatable("cloudworks_api.console.config.player_list.query_header", listTypeComponent),
                false
        );

        if (list.isEmpty()) {
            context.getSource().sendSuccess(() ->
                    Component.translatable("cloudworks_api.console.config.player_list.query_empty"),
                    false
            );
        } else {
            for (String name : list) {
                context.getSource().sendSuccess(() -> Component.literal("  " + name), false);
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    // ======================== 工具方法 ========================

    private static ChatFormatting getTextColor(Level level) {
        if (level == null) return ChatFormatting.GRAY;

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