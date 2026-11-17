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
package com.cloudworks.api.recipeparser.dsl;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Template config manager.
 *
 * 妯℃澘閰嶇疆绠＄悊鍣ㄣ€?
 * <p>
 * 璐熻矗鍔犺浇鍜岀鐞?templates_config/*.json 閰嶇疆鏂囦欢銆?
 * 姣忎釜閰嶇疆鏂囦欢瀵瑰簲涓€涓ā鏉块敭锛坢odid_recipetype锛夛紝
 * 鍖呭惈鍚勪釜閰嶆柟ID鐨勬祦浣撳埌鐗╁搧杞崲閰嶇疆銆?
 * </p>
 * <p>
 * 閰嶇疆鏂囦欢鏍煎紡涓?JSON 瀵硅薄锛岄敭涓洪厤鏂笽D锛屽€间负鍖呭惈 enable_transfer銆?
 * transfer_blacklist 鍜?methods 鏁扮粍鐨勯厤缃璞°€?
 * </p>
 */
public class TemplateConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("CloudWorks-TemplateConfig");

    /**
 * Loads the config file for the specified template key.
 *
 * 鍔犺浇鎸囧畾妯℃澘閿殑閰嶇疆鏂囦欢銆?
 * <p>
 * 浠?configDir 鐩綍涓嬭鍙?{templateKey}.json 鏂囦欢锛?
 * 瑙ｆ瀽鍏朵腑鐨勯厤鏂归厤缃苟杩斿洖 recipeId -> TemplateConfig 鐨勬槧灏勩€?
 * </p>
 *
 * @param configDir the config directory path
 * @param templateKey the template key (format: modid_recipetype)
 * @param configDir   閰嶇疆鐩綍璺緞
 * @param templateKey 妯℃澘閿紙鏍煎紡锛歮odid_recipetype锛?
 * @return a map of recipe IDs to configs, or empty map if file does not exist or parsing fails
 * @return 閰嶆柟ID鍒伴厤缃殑鏄犲皠锛屽鏋滄枃浠朵笉瀛樺湪鎴栬В鏋愬け璐ュ垯杩斿洖绌?map
 */
    public static Map<String, TemplateConfig> loadConfig(Path configDir, String templateKey) {
        Map<String, TemplateConfig> result = new LinkedHashMap<>();
        Path configFile = configDir.resolve(templateKey + ".json");
        if (!Files.exists(configFile)) return result;

        try {
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String recipeId = entry.getKey();
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject recipeObj = entry.getValue().getAsJsonObject();

                TemplateConfig config = parseRecipeConfig(recipeId, recipeObj, templateKey);
                if (config != null) {
                    result.put(recipeId, config);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load template config file: {}", configFile, e);
        }
        return result;
    }

    /**
 * Parses a single recipe's config JSON object.
 *
 * 瑙ｆ瀽鍗曚釜閰嶆柟鐨勯厤缃?JSON 瀵硅薄銆?
 *
 * @param recipeId the recipe ID
 * @param obj the JSON config object
 * @param templateKey the template key (for logging)
 * @param recipeId    閰嶆柟ID
 * @param obj         JSON 閰嶇疆瀵硅薄
 * @param templateKey 妯℃澘閿紙鐢ㄤ簬鏃ュ織锛?
 * @return the parsed config, or null if invalid
 * @return 瑙ｆ瀽鍚庣殑閰嶇疆锛屽鏋滄棤鏁堝垯杩斿洖 null
 */
    private static TemplateConfig parseRecipeConfig(String recipeId, JsonObject obj, String templateKey) {
        TemplateConfig config = new TemplateConfig(recipeId);

        // enable_transfer
        if (obj.has("enable_transfer")) {
            config.setEnableTransfer(obj.get("enable_transfer").getAsBoolean());
        } else {
            config.setEnableTransfer(true); // default true
        }

        // transfer_blacklist
        if (obj.has("transfer_blacklist") && obj.get("transfer_blacklist").isJsonArray()) {
            for (JsonElement e : obj.get("transfer_blacklist").getAsJsonArray()) {
                config.addTransferBlacklist(e.getAsString());
            }
        }

        // methods
        if (!obj.has("methods") || !obj.get("methods").isJsonArray()) {
            LOGGER.error("invalid template config of {}", templateKey);
            return null;
        }

        JsonArray methodsArr = obj.get("methods").getAsJsonArray();
        for (JsonElement e : methodsArr) {
            if (!e.isJsonObject()) continue;
            JsonObject methodObj = e.getAsJsonObject();
            TemplateConfig.TransferMethod method = parseMethod(methodObj, templateKey);
            if (method != null) {
                config.addMethod(method);
            }
        }

        if (config.getMethods().isEmpty()) {
            return null; // skip this config entirely
        }

        return config;
    }

    /**
 * Parses a single transfer method JSON object.
 *
 * 瑙ｆ瀽鍗曚釜杞崲鏂规硶 JSON 瀵硅薄銆?
 *
 * @param obj the transfer method JSON object
 * @param templateKey the template key (for logging)
 * @param obj         杞崲鏂规硶 JSON 瀵硅薄
 * @param templateKey 妯℃澘閿紙鐢ㄤ簬鏃ュ織锛?
 * @return the parsed transfer method, or null if invalid
 * @return 瑙ｆ瀽鍚庣殑杞崲鏂规硶锛屽鏋滄棤鏁堝垯杩斿洖 null
 */
    private static TemplateConfig.TransferMethod parseMethod(JsonObject obj, String templateKey) {
        TemplateConfig.TransferMethod method = new TemplateConfig.TransferMethod();

        // result (required)
        if (!obj.has("result") || !obj.get("result").isJsonObject()) {
            LOGGER.error("invalid transfer method of {}", templateKey);
            return null;
        }
        JsonObject resultObj = obj.get("result").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : resultObj.entrySet()) {
            try {
                method.addResult(entry.getKey(), entry.getValue().getAsInt());
            } catch (Exception ex) {
                LOGGER.warn("Invalid result entry in {}: {}={}", templateKey, entry.getKey(), entry.getValue());
            }
        }
        if (method.getResult().isEmpty()) {
            LOGGER.error("invalid transfer method of {}", templateKey);
            return null;
        }

        // rate (optional, default 100)
        if (obj.has("rate")) {
            try { method.setRate(obj.get("rate").getAsDouble()); } catch (Exception ignored) {}
        }

        // round (optional, default "default")
        if (obj.has("round")) {
            method.setRound(GlobalSettings.parseRound(obj.get("round").getAsString()));
        }

        // extra_input (optional)
        if (obj.has("extra_input") && obj.get("extra_input").isJsonObject()) {
            JsonObject eiObj = obj.get("extra_input").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : eiObj.entrySet()) {
                try {
                    method.addExtraInput(entry.getKey(), entry.getValue().getAsDouble());
                } catch (Exception ignored) {}
            }
        }

        return method;
    }
}