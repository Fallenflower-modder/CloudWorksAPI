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

import com.cloudworks.api.recipeparser.dsl.Template;
import com.cloudworks.api.recipeparser.model.Ingredient;
import com.cloudworks.api.recipeparser.model.Product;
import com.cloudworks.api.recipeparser.model.RecipeData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Debug output writer.
 *
 * 璋冭瘯杈撳嚭鍐欏叆鍣ㄣ€?
 * <p>
 * 鍦ㄦ湇鍔″櫒鍚姩鏃跺皢鎵€鏈夊彲瑙ｆ瀽鐨勯厤鏂规暟鎹鍑轰负 JSON 鏂囦欢锛?
 * 渚涘閮ㄥ伐鍏凤紙濡?AI 閲囨牱鍣級鍒嗘瀽鍜岃皟璇曘€?
 * 杈撳嚭鏂囦欢浣嶄簬娓告垙鐩綍涓嬬殑 cloudworks/recipe_parser/debug_output/ 鏂囦欢澶逛腑銆?
 * </p>
 * <p>
 * 杈撳嚭鏍煎紡涓?JSON 鏁扮粍锛屾瘡涓厓绱犲寘鍚厤鏂笽D銆佽緭鍏ュ垪琛ㄥ拰杈撳嚭鍒楄〃銆?
 * 鍚屾椂鐢熸垚涓€涓厤鏂圭储寮曟枃浠讹紝璁板綍姣忎釜閰嶆柟ID鍒板叾杈撳嚭鏂囦欢璺緞鐨勬槧灏勩€?
 * </p>
 */
public class DebugOutputWriter {

    /**
     * Logger
     */
    /** 鏃ュ織璁板綍鍣?*/
    private static final Logger LOGGER = LoggerFactory.getLogger("CloudWorks-DebugOutput");
    /**
     * Debug output directory
     */
    /** 璋冭瘯杈撳嚭鐩綍 */
    private static final String DEBUG_OUTPUT_DIR = "cloudworks/recipe_parser/debug_output";

    /**
 * Writes debug output for all parsable recipes.
 *
 * 鍐欏叆鎵€鏈夊彲瑙ｆ瀽閰嶆柟鐨勮皟璇曡緭鍑恒€?
 * <p>
 * 閬嶅巻鎵€鏈夊凡鍔犺浇妯℃澘瀵瑰簲绫诲瀷鐨勯厤鏂癸紝瑙ｆ瀽姣忎釜閰嶆柟骞惰緭鍑哄埌 JSON 鏂囦欢銆?
 * 缁撴灉鎸夋ā缁処D鍒嗙粍锛屾瘡涓ā缁勪竴涓緭鍑烘枃浠躲€?
 * </p>
 *
 * @param recipeManager the recipe manager
 * @param templateIndex the template index (modid_recipetype -> Template)
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @param templateIndex 妯℃澘绱㈠紩锛坢odid_recipetype -> Template锛?
 */
    public static void writeDebugOutput(RecipeManager recipeManager, Map<String, Template> templateIndex) {
        LOGGER.info("{} Starting full recipe parse dump...", RecipeParser.LOG_PREFIX);

        Path outputDir = Paths.get(DEBUG_OUTPUT_DIR);
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            LOGGER.error("{} Failed to create debug output directory: {}", RecipeParser.LOG_PREFIX, e.getMessage());
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray indexArray = new JsonArray();

        int totalParsed = 0;
        int totalFailed = 0;

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            ResourceLocation id = holder.id();
            if (id == null) continue;

            try {
                RecipeData data = RecipeParser.getInstance().getRecipeData(id, recipeManager);
                JsonObject recipeObj = new JsonObject();
                recipeObj.addProperty("recipe_id", id.toString());
                recipeObj.addProperty("mod_id", id.getNamespace());
                recipeObj.addProperty("recipe_type", RecipeParser.getRecipeTypeKey(holder.value()));

                JsonArray inputs = new JsonArray();
                for (Ingredient ing : data.getInputs()) {
                    JsonObject ingObj = new JsonObject();
                    ingObj.addProperty("id", ing.getId());
                    ingObj.addProperty("count", ing.getCount());
                    ingObj.addProperty("unit", ing.getUnit());
                    ingObj.addProperty("type", ing.getType());
                    ingObj.addProperty("rate", ing.getRate());
                    inputs.add(ingObj);
                }
                recipeObj.add("inputs", inputs);

                JsonArray outputs = new JsonArray();
                for (Product prod : data.getOutputs()) {
                    JsonObject prodObj = new JsonObject();
                    prodObj.addProperty("id", prod.getId());
                    prodObj.addProperty("count", prod.getCount());
                    prodObj.addProperty("unit", prod.getUnit());
                    prodObj.addProperty("type", prod.getType());
                    prodObj.addProperty("rate", prod.getRate());
                    outputs.add(prodObj);
                }
                recipeObj.add("outputs", outputs);

                // Write to mod-specific file
                String modId = id.getNamespace();
                Path modFile = outputDir.resolve(modId + ".json");

                // Append to array
                JsonArray modArray;
                try {
                    if (Files.exists(modFile)) {
                        String existing = Files.readString(modFile, StandardCharsets.UTF_8);
                        modArray = com.google.gson.JsonParser.parseString(existing).getAsJsonArray();
                    } else {
                        modArray = new JsonArray();
                    }
                } catch (Exception e) {
                    modArray = new JsonArray();
                }

                modArray.add(recipeObj);
                Files.writeString(modFile, gson.toJson(modArray), StandardCharsets.UTF_8);

                // Add to index
                JsonObject indexEntry = new JsonObject();
                indexEntry.addProperty("recipe_id", id.toString());
                indexEntry.addProperty("file", modId + ".json");
                indexArray.add(indexEntry);

                totalParsed++;
            } catch (Exception e) {
                totalFailed++;
                LOGGER.warn("{} Debug dump failed for {}: {}", RecipeParser.LOG_PREFIX, id, e.getMessage());
            }
        }

        // Write index file
        try {
            Path indexFile = outputDir.resolve("_index.json");
            Files.writeString(indexFile, gson.toJson(indexArray), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("{} Failed to write debug index: {}", RecipeParser.LOG_PREFIX, e.getMessage());
        }

        LOGGER.info("{} Debug dump complete: {} parsed, {} failed. Output: {}", RecipeParser.LOG_PREFIX, totalParsed, totalFailed, outputDir.toAbsolutePath());
    }

    /**
 * Asynchronously writes debug output for all parsable recipes.
 * The heavy dump work is offloaded to the dedicated worker thread.
 *
 * 异步写入所有可解析配方的调试输出。
 * 繁重的转储工作卸载到专用工作线程。
 *
 * @param recipeManager the recipe manager / 配方管理器
 * @param templateIndex the template index / 模板索引
 * @param server        the Minecraft server / Minecraft 服务器
 */
    public static void writeDebugOutputAsync(RecipeManager recipeManager, Map<String, Template> templateIndex, MinecraftServer server) {
        AsyncRecipeParser.submit(() -> writeDebugOutput(recipeManager, templateIndex));
    }
}