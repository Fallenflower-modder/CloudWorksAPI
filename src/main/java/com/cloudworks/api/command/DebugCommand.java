/*
 * CloudWorks API - Unified Recipe Parsing Interface
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.cloudworks.api.command;

import com.cloudworks.api.consoleseeker.ConsoleSeekerCommand;
import com.cloudworks.api.consoleseeker.ConsoleSeekerConfig;
import com.cloudworks.api.consoleseeker.ConsoleSeekerEventManager;
import com.cloudworks.api.consoleseeker.LogToChatManager;
import com.cloudworks.api.recipeparser.RecipeParser;
import com.cloudworks.api.recipeparser.RecipeParserAPI;
import com.cloudworks.api.recipeparser.model.QueryMode;
import com.cloudworks.api.recipeparser.model.RecipeData;
import com.cloudworks.api.recipeparser.model.RecipeParseResult;
import com.cloudworks.api.recipeparser.model.Ingredient;
import com.cloudworks.api.recipeparser.model.Product;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

/**
 * CloudWorks debug command.
 *
 * CloudWorks 调试命令。
 * <p>
 * 提供 Minecraft 游戏内命令，用于测试和调试配方解析器及其他模块功能。
 * 作为所有模块调试指令的统一入口点。
 * 支持以下子命令：
 * <ul>
 *   <li>/cloudworks recipe listtemplates - 列出所有已加载的模板</li>
 *   <li>/cloudworks recipe parse &lt;produce|usage&gt; &lt;item|liquid&gt; [&lt;itemID|fluidID&gt;] - 查找配方（不填ID则使用手持物品）</li>
 *   <li>/cloudworks recipe parsebatch &lt;modid&gt; &lt;recipetype&gt; - 批量解析指定类型的配方</li>
 *   <li>/cloudworks status - 查看模块状态</li>
 *   <li>/cloudworks console &lt;info|warn|error&gt; &lt;on|off&gt; - 控制台日志输出到聊天栏</li>
 *   <li>/cloudworks config console player_list &lt;add|remove|query&gt; - 管理 ConsoleSeeker player_list</li>
 * </ul>
 * </p>
 */
public class DebugCommand {

    /**
     * 检查 RecipeParser 模块是否就绪，未就绪时发送失败消息。
     */
    private static boolean requireParserReady(CommandSourceStack source) {
        if (!RecipeParser.getInstance().isEnabled()) {
            source.sendFailure(Component.literal(
                "RecipeParser module is not enabled. This command requires RecipeParser to be running.\n" +
                "Check server logs for initialization errors."
            ));
            return false;
        }
        return true;
    }

    /**
     * 从执行者手持物品中获取目标 ID。
     * 如果执行者不是玩家或没有手持物品，返回 null（调用方应发送提示）。
     */
    private static String resolveTargetFromHeldItem(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                source.sendFailure(Component.literal("No item in hand. Please specify an item/fluid ID."));
                return null;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
            source.sendSuccess(() -> Component.literal("Using held item: " + id), false);
            return id.toString();
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                "This command must be executed by a player or with an explicit item/fluid ID."
            ));
            return null;
        }
    }

    /**
     * 将调试命令注册到命令调度器。
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cloudworks")
                // /cloudworks - 显示帮助
                .executes(DebugCommand::help)
                // /cloudworks status - 查看模块状态
                .then(Commands.literal("status")
                    .executes(DebugCommand::status))
                // /cloudworks console <info|warn|error> <on|off> - 控制台日志输出到聊天栏
                .then(Commands.literal("console")
                    .requires(ConsoleSeekerCommand::canUseConsole)
                    .then(Commands.argument("level", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                            SharedSuggestionProvider.suggest(new String[]{"info", "warn", "error"}, builder))
                        .then(Commands.argument("action", StringArgumentType.word())
                            .suggests((ctx, builder) ->
                                SharedSuggestionProvider.suggest(new String[]{"on", "off"}, builder))
                            .executes(ConsoleSeekerCommand::executeConsole))))
                // /cloudworks config console player_list <add|remove> <player_name> - 管理 ConsoleSeeker player_list
                .then(Commands.literal("config")
                    .then(Commands.literal("console")
                        .then(Commands.literal("player_list")
                            .then(Commands.literal("add")
                                .requires(source -> source.getEntity() == null)
                                .then(Commands.argument("player_name", StringArgumentType.word())
                                    .executes(ConsoleSeekerCommand::executeConfigPlayerListAdd)))
                            .then(Commands.literal("remove")
                                .requires(source -> source.getEntity() == null)
                                .then(Commands.argument("player_name", StringArgumentType.word())
                                    .executes(ConsoleSeekerCommand::executeConfigPlayerListRemove)))
                            .then(Commands.literal("query")
                                .requires(source -> source.getEntity() == null || source.hasPermission(4))
                                .executes(ConsoleSeekerCommand::executeConfigPlayerListQuery)))))
                // /cloudworks recipe ... - 配方模块指令
                .then(Commands.literal("recipe")
                    .then(Commands.literal("listtemplates")
                        .executes(DebugCommand::listTemplates))
                    .then(Commands.literal("parsebatch")
                        .then(Commands.argument("modid", StringArgumentType.string())
                            .then(Commands.argument("recipetype", StringArgumentType.string())
                                .executes(DebugCommand::parseBatch))))
                    .then(Commands.literal("parse")
                        // /cloudworks recipe parse produce item [itemID]
                        .then(Commands.literal("produce")
                            .then(Commands.literal("item")
                                .executes(DebugCommand::parseProduceItemNoArg)
                                .then(Commands.argument("item_id", StringArgumentType.greedyString())
                                    .executes(DebugCommand::parseProduceItem)))
                            .then(Commands.literal("liquid")
                                .executes(DebugCommand::parseProduceLiquidNoArg)
                                .then(Commands.argument("fluid_id", StringArgumentType.greedyString())
                                    .executes(DebugCommand::parseProduceLiquid))))
                        // /cloudworks recipe parse usage item [itemID]
                        .then(Commands.literal("usage")
                            .then(Commands.literal("item")
                                .executes(DebugCommand::parseUsageItemNoArg)
                                .then(Commands.argument("item_id", StringArgumentType.greedyString())
                                    .executes(DebugCommand::parseUsageItem)))
                            .then(Commands.literal("liquid")
                                .executes(DebugCommand::parseUsageLiquidNoArg)
                                .then(Commands.argument("fluid_id", StringArgumentType.greedyString())
                                    .executes(DebugCommand::parseUsageLiquid)))))
                )
        );
    }

    // ======================== 帮助命令 ========================

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("=== CloudWorks Debug Commands ==="), false);
        source.sendSuccess(() -> Component.literal("/cloudworks status"), false);
        source.sendSuccess(() -> Component.literal("/cloudworks recipe listtemplates"), false);
        source.sendSuccess(() -> Component.literal("/cloudworks recipe parse produce|usage item|liquid [id]"), false);
        source.sendSuccess(() -> Component.literal("/cloudworks recipe parsebatch <modid> <recipetype>"), false);
        source.sendSuccess(() -> Component.literal("/cloudworks console <info|warn|error> <on|off>"), false);
        source.sendSuccess(() -> Component.literal("/cloudworks config console player_list add|remove <player>"), false);
        source.sendSuccess(() -> Component.literal("/cloudworks config console player_list query"), false);
        return 1;
    }

    // ======================== 命令处理函数 ========================

    /**
     * 列出所有已加载模板的命令处理。
     */
    private static int listTemplates(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        java.util.Set<String> keys = RecipeParser.getInstance().getAllTemplateKeys();
        source.sendSuccess(() -> Component.literal("=== Loaded Templates (" + keys.size() + ") ==="), false);
        for (String key : keys) {
            source.sendSuccess(() -> Component.literal("  " + key), false);
        }
        return 1;
    }

    /**
     * 批量解析命令处理。
     */
    private static int parseBatch(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String modId = StringArgumentType.getString(ctx, "modid");
        String recipeType = StringArgumentType.getString(ctx, "recipetype");
        MinecraftServer server = source.getServer();
        RecipeManager recipeManager = server.getRecipeManager();

        List<ResourceLocation> recipeIds = RecipeParser.getInstance().getParsableRecipes(modId, recipeType, recipeManager);
        source.sendSuccess(() -> Component.literal("Found " + recipeIds.size() + " parsable recipes for " + modId + ":" + recipeType + ". Parsing in background..."), false);

        RecipeParserAPI.getRecipeDataBatchAsync(
            recipeIds, recipeManager,
            dataMap -> {
                int success = dataMap.size();
                int failed = recipeIds.size() - success;
                source.sendSuccess(() -> Component.literal("Complete: " + success + " success, " + failed + " failed"), false);
                for (ResourceLocation id : recipeIds) {
                    if (!dataMap.containsKey(id)) {
                        source.sendSuccess(() -> Component.literal("  FAILED: " + id), false);
                    }
                }
            },
            error -> source.sendFailure(Component.literal("Batch parse error: " + error)),
            server
        );
        return 1;
    }

    // ======================== produce item ========================

    /** /cloudworks recipe parse produce item - 使用手持物品ID */
    private static int parseProduceItemNoArg(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = resolveTargetFromHeldItem(source);
        if (targetStr == null) return 0;
        return executeProduceQuery(source, targetStr, QueryMode.ITEM);
    }

    /** /cloudworks recipe parse produce item <itemID> */
    private static int parseProduceItem(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = StringArgumentType.getString(ctx, "item_id");
        return executeProduceQuery(source, targetStr, QueryMode.ITEM);
    }

    // ======================== produce liquid ========================

    /** /cloudworks recipe parse produce liquid - 使用手持物品ID */
    private static int parseProduceLiquidNoArg(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = resolveTargetFromHeldItem(source);
        if (targetStr == null) return 0;
        return executeProduceQuery(source, targetStr, QueryMode.FLUID);
    }

    /** /cloudworks recipe parse produce liquid <fluidID> */
    private static int parseProduceLiquid(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = StringArgumentType.getString(ctx, "fluid_id");
        return executeProduceQuery(source, targetStr, QueryMode.FLUID);
    }

    private static int executeProduceQuery(CommandSourceStack source, String targetStr, QueryMode mode) {
        ResourceLocation targetId = ResourceLocation.tryParse(targetStr);
        if (targetId == null) {
            source.sendFailure(Component.literal("Invalid target ID: " + targetStr));
            return 0;
        }

        MinecraftServer server = source.getServer();
        RecipeManager recipeManager = server.getRecipeManager();

        RecipeParserAPI.parseProduceRecipeAsync(
            targetId, mode, recipeManager,
            results -> {
                source.sendSuccess(() -> Component.literal("=== Recipes producing " + targetId + " (" + mode + ") ==="), false);
                source.sendSuccess(() -> Component.literal("Found " + results.size() + " recipes"), false);
                for (RecipeParseResult result : results) {
                    RecipeData data = result.getData();
                    source.sendSuccess(() -> Component.literal("  " + result.getRecipeId()), false);
                    for (Ingredient ing : data.getInputs()) {
                        source.sendSuccess(() -> Component.literal(
                            String.format("    IN:  %s x%.1f", ing.getId(), ing.getCount())), false);
                    }
                    for (Product prod : data.getOutputs()) {
                        source.sendSuccess(() -> Component.literal(
                            String.format("    OUT: %s x%.1f", prod.getId(), prod.getCount())), false);
                    }
                }
            },
            error -> source.sendFailure(Component.literal("Error: " + error)),
            server
        );
        return 1;
    }

    // ======================== usage item ========================

    /** /cloudworks recipe parse usage item - 使用手持物品ID */
    private static int parseUsageItemNoArg(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = resolveTargetFromHeldItem(source);
        if (targetStr == null) return 0;
        return executeUsageQuery(source, targetStr, QueryMode.ITEM);
    }

    /** /cloudworks recipe parse usage item <itemID> */
    private static int parseUsageItem(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = StringArgumentType.getString(ctx, "item_id");
        return executeUsageQuery(source, targetStr, QueryMode.ITEM);
    }

    // ======================== usage liquid ========================

    /** /cloudworks recipe parse usage liquid - 使用手持物品ID */
    private static int parseUsageLiquidNoArg(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = resolveTargetFromHeldItem(source);
        if (targetStr == null) return 0;
        return executeUsageQuery(source, targetStr, QueryMode.FLUID);
    }

    /** /cloudworks recipe parse usage liquid <fluidID> */
    private static int parseUsageLiquid(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!requireParserReady(source)) return 0;
        String targetStr = StringArgumentType.getString(ctx, "fluid_id");
        return executeUsageQuery(source, targetStr, QueryMode.FLUID);
    }

    private static int executeUsageQuery(CommandSourceStack source, String targetStr, QueryMode mode) {
        ResourceLocation targetId = ResourceLocation.tryParse(targetStr);
        if (targetId == null) {
            source.sendFailure(Component.literal("Invalid target ID: " + targetStr));
            return 0;
        }

        MinecraftServer server = source.getServer();
        RecipeManager recipeManager = server.getRecipeManager();

        RecipeParserAPI.parseUsageRecipeAsync(
            targetId, mode, recipeManager,
            results -> {
                source.sendSuccess(() -> Component.literal("=== Recipes using " + targetId + " (" + mode + ") ==="), false);
                source.sendSuccess(() -> Component.literal("Found " + results.size() + " recipes"), false);
                for (RecipeParseResult result : results) {
                    RecipeData data = result.getData();
                    source.sendSuccess(() -> Component.literal("  " + result.getRecipeId()), false);
                    for (Ingredient ing : data.getInputs()) {
                        source.sendSuccess(() -> Component.literal(
                            String.format("    IN:  %s x%.1f", ing.getId(), ing.getCount())), false);
                    }
                    for (Product prod : data.getOutputs()) {
                        source.sendSuccess(() -> Component.literal(
                            String.format("    OUT: %s x%.1f", prod.getId(), prod.getCount())), false);
                    }
                }
            },
            error -> source.sendFailure(Component.literal("Error: " + error)),
            server
        );
        return 1;
    }

    // ======================== status 命令处理 ========================

    /**
     * 查看模块状态的命令处理。
     */
    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("=== CloudWorks Module Status ==="), false);

        // RecipeParser 状态
        RecipeParser parser = RecipeParser.getInstance();
        boolean enabled = parser.isEnabled();
        source.sendSuccess(() -> Component.literal("RecipeParser Enabled: " + enabled), false);
        if (enabled) {
            source.sendSuccess(() -> Component.literal("  Templates: " + parser.getAllTemplateKeys().size()), false);
        } else {
            source.sendSuccess(() -> Component.literal("  RecipeParser module is disabled. Check server logs for errors."), false);
        }

        // ConsoleSeeker 状态
        if (ConsoleSeekerConfig.isEnableModule()) {
            source.sendSuccess(() -> Component.literal("ConsoleSeeker: Active"), false);
            source.sendSuccess(() -> Component.literal("  Enabled Levels: ")
                    .append(LogToChatManager.getEnabledLevelsComponent()), false);
            source.sendSuccess(() -> Component.literal("  All OPs: " + ConsoleSeekerConfig.isEnableCommandForAnyOperator()), false);
            if (!ConsoleSeekerConfig.isEnableCommandForAnyOperator()) {
                source.sendSuccess(() -> Component.literal("  List Type: " + ConsoleSeekerConfig.getListType()), false);
                source.sendSuccess(() -> Component.literal("  Player List: " + ConsoleSeekerConfig.getPlayerListCopy().size() + " entries"), false);
            }
            source.sendSuccess(() -> Component.literal("  API: " + (ConsoleSeekerEventManager.isApiEnabled() ? "Enabled" : "Disabled")), false);
            source.sendSuccess(() -> Component.literal("  Internal Filter Units: " + ConsoleSeekerEventManager.getInternalFilterUnitCount()), false);
        } else {
            source.sendSuccess(() -> Component.literal("ConsoleSeeker: Disabled (enable_module = false)"), false);
        }

        source.sendSuccess(() -> Component.literal("Debug Commands: Active"), false);
        return 1;
    }
}