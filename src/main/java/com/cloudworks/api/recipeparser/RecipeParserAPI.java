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
package com.cloudworks.api.recipeparser;

import com.cloudworks.api.recipeparser.exception.RecipeNotFoundException;
import com.cloudworks.api.recipeparser.exception.RecipeParseException;
import com.cloudworks.api.recipeparser.model.QueryMode;
import com.cloudworks.api.recipeparser.model.RecipeData;
import com.cloudworks.api.recipeparser.model.RecipeParseResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Static API facade class for RecipeParser.
 *
 * RecipeParser 鐨勯潤鎬?API 澶栬绫汇€?
 * <p>
 * 涓哄閮ㄨ皟鐢ㄦ柟鎻愪緵绠€娲佺殑闈欐€佹柟娉曟帴鍙ｏ紝鎵€鏈夋柟娉曞潎濮旀墭缁?
 * {@link RecipeParser#getInstance()} 鍗曚緥澶勭悊銆?
 * 杩欐槸璋冪敤鏂逛娇鐢?RecipeParser 鍔熻兘鐨勪富瑕佸叆鍙ｇ偣銆?
 * </p>
 */
public class RecipeParserAPI {

    /**
     * Private constructor to prevent instantiation.
     */
    /** 绉佹湁鏋勯€犲嚱鏁帮紝闃叉瀹炰緥鍖栥€?*/
    private RecipeParserAPI() {}

    /**
 * Gets the parsed data for the specified recipe ID.
 *
 * 鑾峰彇鎸囧畾閰嶆柟ID鐨勮В鏋愭暟鎹€?
 *
 * @param recipeId the recipe ID
 * @param recipeManager the recipe manager
 * @param recipeId      閰嶆柟ID
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return the parsed recipe data
 * @return 瑙ｆ瀽鍚庣殑閰嶆柟鏁版嵁
 * @throws RecipeNotFoundException if the specified recipe is not found
 * @throws RecipeParseException if recipe parsing fails
 * @throws RecipeNotFoundException 濡傛灉鎵句笉鍒版寚瀹氶厤鏂?
 * @throws RecipeParseException    濡傛灉閰嶆柟瑙ｆ瀽澶辫触
 */
    public static RecipeData getRecipeData(ResourceLocation recipeId, RecipeManager recipeManager)
            throws RecipeNotFoundException, RecipeParseException {
        return RecipeParser.getInstance().getRecipeData(recipeId, recipeManager);
    }

    /**
 * Batch gets parsed data for multiple recipe IDs.
 *
 * 鎵归噺鑾峰彇澶氫釜閰嶆柟ID鐨勮В鏋愭暟鎹€?
 * 鍗曚釜閰嶆柟瑙ｆ瀽澶辫触涓嶄細褰卞搷鍏朵粬閰嶆柟鐨勮В鏋愩€?
 *
 * @param recipeIds the set of recipe IDs
 * @param recipeManager the recipe manager
 * @param recipeIds     閰嶆柟ID闆嗗悎
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return a map of recipe IDs to parsed data (only successfully parsed recipes)
 * @return 閰嶆柟ID鍒拌В鏋愭暟鎹殑鏄犲皠锛堜粎鍖呭惈鎴愬姛瑙ｆ瀽鐨勯厤鏂癸級
 */
    public static Map<ResourceLocation, RecipeData> getRecipeDataBatch(
            Collection<ResourceLocation> recipeIds, RecipeManager recipeManager) {
        return RecipeParser.getInstance().getRecipeDataBatch(recipeIds, recipeManager);
    }

    /**
 * Checks whether the specified recipe is parsable (i.e., whether a corresponding template exists).
 *
 * 妫€鏌ユ寚瀹氶厤鏂规槸鍚﹀彲瑙ｆ瀽锛堝嵆鏄惁瀛樺湪瀵瑰簲鐨勬ā鏉匡級銆?
 *
 * @param recipeId the recipe ID
 * @param recipeManager the recipe manager
 * @param recipeId      閰嶆柟ID
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return true if the recipe is parsable
 * @return 濡傛灉閰嶆柟鍙В鏋愬垯杩斿洖 true
 */
    public static boolean isRecipeParsable(ResourceLocation recipeId, RecipeManager recipeManager) {
        return RecipeParser.getInstance().isRecipeParsable(recipeId, recipeManager);
    }

    /**
 * Gets a list of all parsable recipe IDs for the specified mod and recipe type.
 *
 * 鑾峰彇鎸囧畾妯＄粍鍜岄厤鏂圭被鍨嬩笅鎵€鏈夊彲瑙ｆ瀽鐨勯厤鏂笽D鍒楄〃銆?
 *
 * @param modId the mod ID
 * @param recipeType the recipe type
 * @param recipeManager the recipe manager
 * @param modId         妯＄粍ID
 * @param recipeType    閰嶆柟绫诲瀷
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return the list of parsable recipe IDs
 * @return 鍙В鏋愮殑閰嶆柟ID鍒楄〃
 */
    public static List<ResourceLocation> getParsableRecipes(String modId, String recipeType, RecipeManager recipeManager) {
        return RecipeParser.getInstance().getParsableRecipes(modId, recipeType, recipeManager);
    }

    /**
 * Finds all recipes that produce the specified target item/fluid.
 *
 * 鏌ユ壘鎵€鏈変骇鍑烘寚瀹氱洰鏍囩墿鍝?娴佷綋鐨勯厤鏂广€?
 * 瀵逛簬 ITEM 妯″紡锛屽悓鏃跺寘鍚祦浣撳埌鐗╁搧鐨勮浆鎹㈤厤鏂广€?
 *
 * @param targetId the target item or fluid ID
 * @param mode the query mode (ITEM or FLUID)
 * @param recipeManager the recipe manager
 * @param targetId      鐩爣鐗╁搧鎴栨祦浣揑D
 * @param mode          鏌ヨ妯″紡锛圛TEM 鎴?FLUID锛?
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return the list of matching recipe parse results
 * @return 鍖归厤鐨勯厤鏂硅В鏋愮粨鏋滃垪琛?
 */
    public static List<RecipeParseResult> parseProduceRecipe(
            ResourceLocation targetId, QueryMode mode, RecipeManager recipeManager) {
        return RecipeParser.getInstance().parseProduceRecipe(targetId, mode, recipeManager);
    }

    /**
 * Finds all recipes that use the specified target item/fluid as input.
 *
 * 鏌ユ壘鎵€鏈変娇鐢ㄦ寚瀹氱洰鏍囩墿鍝?娴佷綋浣滀负杈撳叆鐨勯厤鏂广€?
 *
 * @param targetId the target item or fluid ID
 * @param mode the query mode (ITEM or FLUID)
 * @param recipeManager the recipe manager
 * @param targetId      鐩爣鐗╁搧鎴栨祦浣揑D
 * @param mode          鏌ヨ妯″紡锛圛TEM 鎴?FLUID锛?
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return the list of matching recipe parse results
 * @return 鍖归厤鐨勯厤鏂硅В鏋愮粨鏋滃垪琛?
 */
    public static List<RecipeParseResult> parseUsageRecipe(
            ResourceLocation targetId, QueryMode mode, RecipeManager recipeManager) {
        return RecipeParser.getInstance().parseUsageRecipe(targetId, mode, recipeManager);
    }

    // ======================== Async API Methods ========================

    /**
 * Asynchronously gets the parsed data for the specified recipe ID.
 * The heavy parsing work runs on the dedicated worker thread;
 * the result callback is invoked on the server thread.
 *
 * 异步获取指定配方ID的解析数据。
 * 繁重的解析工作运行在专用工作线程上；结果回调在服务器线程上调用。
 *
 * @param recipeId       the recipe ID / 配方ID
 * @param recipeManager  the recipe manager / 配方管理器
 * @param resultCallback callback invoked on server thread with the parsed data / 在服务器线程上调用的回调，接收解析数据
 * @param errorCallback  callback invoked on server thread if an error occurs / 发生错误时在服务器线程上调用的回调
 * @param server         the Minecraft server / Minecraft 服务器
 */
    public static void getRecipeDataAsync(
            ResourceLocation recipeId,
            RecipeManager recipeManager,
            Consumer<RecipeData> resultCallback,
            Consumer<String> errorCallback,
            MinecraftServer server) {
        AsyncRecipeParser.runAsyncQuery(
            () -> getRecipeData(recipeId, recipeManager),
            resultCallback,
            errorCallback,
            server
        );
    }

    /**
 * Asynchronously batch gets parsed data for multiple recipe IDs.
 * The heavy parsing work runs on the dedicated worker thread;
 * the result callback is invoked on the server thread.
 *
 * 异步批量获取多个配方ID的解析数据。
 * 繁重的解析工作运行在专用工作线程上；结果回调在服务器线程上调用。
 *
 * @param recipeIds      the set of recipe IDs / 配方ID集合
 * @param recipeManager  the recipe manager / 配方管理器
 * @param resultCallback callback invoked on server thread with the parsed data map / 在服务器线程上调用的回调，接收解析数据映射
 * @param errorCallback  callback invoked on server thread if an error occurs / 发生错误时在服务器线程上调用的回调
 * @param server         the Minecraft server / Minecraft 服务器
 */
    public static void getRecipeDataBatchAsync(
            Collection<ResourceLocation> recipeIds,
            RecipeManager recipeManager,
            Consumer<Map<ResourceLocation, RecipeData>> resultCallback,
            Consumer<String> errorCallback,
            MinecraftServer server) {
        AsyncRecipeParser.runAsyncQuery(
            () -> getRecipeDataBatch(recipeIds, recipeManager),
            resultCallback,
            errorCallback,
            server
        );
    }

    /**
 * Asynchronously finds all recipes that produce the specified target item/fluid.
 * The heavy scanning and parsing work runs on the dedicated worker thread;
 * the result callback is invoked on the server thread.
 *
 * 异步查找所有产出指定目标物品/流体的配方。
 * 繁重的扫描和解析工作运行在专用工作线程上；结果回调在服务器线程上调用。
 *
 * @param targetId       the target item or fluid ID / 目标物品或流体ID
 * @param mode           the query mode (ITEM or FLUID) / 查询模式（ITEM 或 FLUID）
 * @param recipeManager  the recipe manager / 配方管理器
 * @param resultCallback callback invoked on server thread with the list of parse results / 在服务器线程上调用的回调，接收解析结果列表
 * @param errorCallback  callback invoked on server thread if an error occurs / 发生错误时在服务器线程上调用的回调
 * @param server         the Minecraft server / Minecraft 服务器
 */
    public static void parseProduceRecipeAsync(
            ResourceLocation targetId,
            QueryMode mode,
            RecipeManager recipeManager,
            Consumer<List<RecipeParseResult>> resultCallback,
            Consumer<String> errorCallback,
            MinecraftServer server) {
        AsyncRecipeParser.runAsyncQuery(
            () -> parseProduceRecipe(targetId, mode, recipeManager),
            resultCallback,
            errorCallback,
            server
        );
    }

    /**
 * Asynchronously finds all recipes that use the specified target item/fluid as input.
 * The heavy scanning and parsing work runs on the dedicated worker thread;
 * the result callback is invoked on the server thread.
 *
 * 异步查找所有使用指定目标物品/流体作为输入的配方。
 * 繁重的扫描和解析工作运行在专用工作线程上；结果回调在服务器线程上调用。
 *
 * @param targetId       the target item or fluid ID / 目标物品或流体ID
 * @param mode           the query mode (ITEM or FLUID) / 查询模式（ITEM 或 FLUID）
 * @param recipeManager  the recipe manager / 配方管理器
 * @param resultCallback callback invoked on server thread with the list of parse results / 在服务器线程上调用的回调，接收解析结果列表
 * @param errorCallback  callback invoked on server thread if an error occurs / 发生错误时在服务器线程上调用的回调
 * @param server         the Minecraft server / Minecraft 服务器
 */
    public static void parseUsageRecipeAsync(
            ResourceLocation targetId,
            QueryMode mode,
            RecipeManager recipeManager,
            Consumer<List<RecipeParseResult>> resultCallback,
            Consumer<String> errorCallback,
            MinecraftServer server) {
        AsyncRecipeParser.runAsyncQuery(
            () -> parseUsageRecipe(targetId, mode, recipeManager),
            resultCallback,
            errorCallback,
            server
        );
    }
}