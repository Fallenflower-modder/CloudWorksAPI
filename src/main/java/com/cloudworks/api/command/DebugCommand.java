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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

/**
 * CloudWorks debug command.
 *
 * CloudWorks 璋冭瘯鍛戒护銆?
 * <p>
 * 鎻愪緵 Minecraft 娓告垙鍐呭懡浠わ紝鐢ㄤ簬娴嬭瘯鍜岃皟璇曢厤鏂硅В鏋愬櫒鍔熻兘銆?
 * 鏀寔浠ヤ笅瀛愬懡浠わ細
 * <ul>
 *   <li>/cw listtemplates - 鍒楀嚭鎵€鏈夊凡鍔犺浇鐨勬ā鏉?/li>
 *   <li>/cw parse &lt;recipe_id&gt; - 瑙ｆ瀽鎸囧畾閰嶆柟</li>
 *   <li>/cw parsebatch &lt;modid&gt; &lt;recipetype&gt; - 鎵归噺瑙ｆ瀽鎸囧畾绫诲瀷鐨勯厤鏂?/li>
 *   <li>/cw produce &lt;item_or_fluid&gt; [item|fluid] - 鏌ユ壘浜у嚭鎸囧畾鐗╁搧/娴佷綋鐨勯厤鏂?/li>
 *   <li>/cw usage &lt;item_or_fluid&gt; [item|fluid] - 鏌ユ壘浣跨敤鎸囧畾鐗╁搧/娴佷綋浣滀负杈撳叆鐨勯厤鏂?/li>
 *   <li>/cw status - 鏌ョ湅 RecipeParser 妯″潡鐘舵€?/li>
 * </ul>
 * </p>
 */
public class DebugCommand {

    /**
     * Command description prefix
     */
    /** 鍛戒护鎻忚堪鍓嶇紑 */
    private static final String HELP_PREFIX = "CloudWorks RecipeParser Debug Command";

    /**
 * Registers the debug command with the command dispatcher.
 *
 * 灏嗚皟璇曞懡浠ゆ敞鍐屽埌鍛戒护璋冨害鍣ㄣ€?
 *
 * @param dispatcher the command dispatcher
 * @param dispatcher 鍛戒护璋冨害鍣?
 */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cw")
                .then(Commands.literal("listtemplates")
                    .executes(DebugCommand::listTemplates))
                .then(Commands.literal("parse")
                    .then(Commands.argument("recipe_id", StringArgumentType.string())
                        .suggests(RecipeIdSuggestionProvider.INSTANCE)
                        .executes(DebugCommand::parseRecipe)))
                .then(Commands.literal("parsebatch")
                    .then(Commands.argument("modid", StringArgumentType.string())
                        .then(Commands.argument("recipetype", StringArgumentType.string())
                            .executes(DebugCommand::parseBatch))))
                .then(Commands.literal("produce")
                    .then(Commands.argument("target", StringArgumentType.string())
                        .then(Commands.argument("mode", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("item", "fluid"), builder))
                            .executes(DebugCommand::produceRecipe))))
                .then(Commands.literal("usage")
                    .then(Commands.argument("target", StringArgumentType.string())
                        .then(Commands.argument("mode", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("item", "fluid"), builder))
                            .executes(DebugCommand::usageRecipe))))
                .then(Commands.literal("status")
                    .executes(DebugCommand::status))
        );
    }

    /**
     * Command handler for listing all loaded templates.
     */
    /** 鍒楀嚭鎵€鏈夊凡鍔犺浇妯℃澘鐨勫懡浠ゅ鐞嗐€?*/
    private static int listTemplates(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        java.util.Set<String> keys = RecipeParser.getInstance().getAllTemplateKeys();
        source.sendSuccess(() -> Component.literal("=== Loaded Templates (" + keys.size() + ") ==="), false);
        for (String key : keys) {
            source.sendSuccess(() -> Component.literal("  " + key), false);
        }
        return 1;
    }

    /**
     * Command handler for parsing a single recipe.
     */
    /** 瑙ｆ瀽鍗曚釜閰嶆柟鍛戒护澶勭悊銆?*/
    private static int parseRecipe(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String recipeIdStr = StringArgumentType.getString(ctx, "recipe_id");
        ResourceLocation recipeId = ResourceLocation.tryParse(recipeIdStr);
        if (recipeId == null) {
            source.sendFailure(Component.literal("Invalid recipe ID: " + recipeIdStr));
            return 0;
        }

        RecipeManager recipeManager = source.getServer().getRecipeManager();
        try {
            RecipeData data = RecipeParser.getInstance().getRecipeData(recipeId, recipeManager);
            source.sendSuccess(() -> Component.literal("=== Recipe: " + recipeId + " ==="), false);
            source.sendSuccess(() -> Component.literal("Inputs:"), false);
            for (Ingredient ing : data.getInputs()) {
                source.sendSuccess(() -> Component.literal(
                    String.format("  %s x%.1f (%s) [%s]", ing.getId(), ing.getCount(), ing.getUnit(), ing.getType())), false);
            }
            source.sendSuccess(() -> Component.literal("Outputs:"), false);
            for (Product prod : data.getOutputs()) {
                source.sendSuccess(() -> Component.literal(
                    String.format("  %s x%.1f (%s) [%s]", prod.getId(), prod.getCount(), prod.getUnit(), prod.getType())), false);
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
        }
        return 1;
    }

    /**
     * Command handler for batch parsing.
     */
    /** 鎵归噺瑙ｆ瀽鍛戒护澶勭悊銆?*/
    private static int parseBatch(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String modId = StringArgumentType.getString(ctx, "modid");
        String recipeType = StringArgumentType.getString(ctx, "recipetype");
        RecipeManager recipeManager = source.getServer().getRecipeManager();

        List<ResourceLocation> recipeIds = RecipeParser.getInstance().getParsableRecipes(modId, recipeType, recipeManager);
        source.sendSuccess(() -> Component.literal("Found " + recipeIds.size() + " parsable recipes for " + modId + ":" + recipeType), false);

        int success = 0;
        int failed = 0;
        for (ResourceLocation id : recipeIds) {
            try {
                RecipeData data = RecipeParser.getInstance().getRecipeData(id, recipeManager);
                success++;
            } catch (Exception e) {
                failed++;
                source.sendSuccess(() -> Component.literal("  FAILED: " + id + " - " + e.getMessage()), false);
            }
        }
        final int finalSuccess = success;
        final int finalFailed = failed;
        source.sendSuccess(() -> Component.literal("Complete: " + finalSuccess + " success, " + finalFailed + " failed"), false);
        return 1;
    }

    /**
     * Command handler for finding recipes that produce a target.
     */
    /** 鏌ユ壘浜у嚭閰嶆柟鍛戒护澶勭悊銆?*/
    private static int produceRecipe(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String targetStr = StringArgumentType.getString(ctx, "target");
        String modeStr = StringArgumentType.getString(ctx, "mode");
        ResourceLocation targetId = ResourceLocation.tryParse(targetStr);
        if (targetId == null) {
            source.sendFailure(Component.literal("Invalid target ID: " + targetStr));
            return 0;
        }

        QueryMode mode = "fluid".equalsIgnoreCase(modeStr) ? QueryMode.FLUID : QueryMode.ITEM;
        RecipeManager recipeManager = source.getServer().getRecipeManager();

        List<RecipeParseResult> results = RecipeParser.getInstance().parseProduceRecipe(targetId, mode, recipeManager);
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
        return 1;
    }

    /**
     * Command handler for finding recipes that use a target as input.
     */
    /** 鏌ユ壘浣跨敤閰嶆柟鍛戒护澶勭悊銆?*/
    private static int usageRecipe(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String targetStr = StringArgumentType.getString(ctx, "target");
        String modeStr = StringArgumentType.getString(ctx, "mode");
        ResourceLocation targetId = ResourceLocation.tryParse(targetStr);
        if (targetId == null) {
            source.sendFailure(Component.literal("Invalid target ID: " + targetStr));
            return 0;
        }

        QueryMode mode = "fluid".equalsIgnoreCase(modeStr) ? QueryMode.FLUID : QueryMode.ITEM;
        RecipeManager recipeManager = source.getServer().getRecipeManager();

        List<RecipeParseResult> results = RecipeParser.getInstance().parseUsageRecipe(targetId, mode, recipeManager);
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
        return 1;
    }

    /**
     * Command handler for viewing module status.
     */
    /** 鏌ョ湅妯″潡鐘舵€佸懡浠ゅ鐞嗐€?*/
    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        RecipeParser parser = RecipeParser.getInstance();
        source.sendSuccess(() -> Component.literal("=== CloudWorks RecipeParser Status ==="), false);
        source.sendSuccess(() -> Component.literal("Enabled: " + parser.isEnabled()), false);
        source.sendSuccess(() -> Component.literal("Templates: " + parser.getAllTemplateKeys().size()), false);
        return 1;
    }

    /**
     * Recipe ID suggestion provider, offers auto-completion for all parsable recipes.
     */
    /** 閰嶆柟ID寤鸿鎻愪緵鍣紝鎻愪緵鎵€鏈夊彲瑙ｆ瀽閰嶆柟鐨勮嚜鍔ㄨˉ鍏ㄣ€?*/
    private enum RecipeIdSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        INSTANCE;

        @Override
        public java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> getSuggestions(
                CommandContext<CommandSourceStack> ctx,
                com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
            RecipeManager recipeManager = ctx.getSource().getServer().getRecipeManager();
            for (var entry : recipeManager.getRecipes()) {
                if (entry.id() != null) {
                    builder.suggest(entry.id().toString());
                }
            }
            return builder.buildFuture();
        }
    }
}