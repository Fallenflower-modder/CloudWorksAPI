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

import com.cloudworks.api.annotation.CloudworksRecipeParser;
import com.cloudworks.api.recipeparser.dsl.*;
import com.cloudworks.api.recipeparser.exception.RecipeNotFoundException;
import com.cloudworks.api.recipeparser.exception.RecipeParseException;
import com.cloudworks.api.recipeparser.model.RecipeData;
import com.cloudworks.api.recipeparser.model.QueryMode;
import com.cloudworks.api.recipeparser.model.RecipeParseResult;
import com.cloudworks.api.recipeparser.model.Ingredient;
import com.cloudworks.api.recipeparser.model.Product;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recipe parser core class.
 *
 * 閰嶆柟瑙ｆ瀽鍣ㄦ牳蹇冪被銆?
 * <p>
 * CloudWorks 妯＄粍鐨勬牳蹇冪粍浠讹紝璐熻矗灏?Minecraft 閰嶆柟鏁版嵁瑙ｆ瀽涓虹粨鏋勫寲鐨?
 * 杈撳叆/杈撳嚭妯″瀷锛坽@link RecipeData}锛夈€?
 * 浣跨敤 RPML 妯℃澘璇█瀹氫箟閰嶆柟缁撴瀯锛岄€氳繃妯℃澘鍖归厤 JSON 閰嶆柟鏁版嵁鏉ユ彁鍙?
 * 杈撳叆鏉愭枡鍜屼骇鍑虹墿淇℃伅銆?
 * </p>
 *
 * <p>
 * 涓昏鍔熻兘锛?
 * <ul>
 *   <li>鑷姩妫€娴?{@link CloudworksRecipeParser} 娉ㄨВ浠ュ惎鐢ㄦā鍧?/li>
 *   <li>浠?JAR 璧勬簮涓彁鍙栧唴缃?RPML 妯℃澘鍒版父鎴忕洰褰?/li>
 *   <li>鍔犺浇骞惰В鏋?RPML 妯℃澘鏂囦欢</li>
 *   <li>鍔犺浇妯℃澘閰嶇疆锛堟祦浣撳埌鐗╁搧杞崲锛?/li>
 *   <li>瑙ｆ瀽鍗曚釜鎴栨壒閲忛厤鏂规暟鎹?/li>
 *   <li>鎸変骇鍑虹墿鎴栬緭鍏ョ墿鏌ヨ閰嶆柟</li>
 *   <li>鏈嶅姟鍣ㄥ惎鍔ㄦ椂杈撳嚭璋冭瘯鏁版嵁</li>
 * </ul>
 * </p>
 *
 * <p>
 * 浣跨敤鏂瑰紡锛氶€氳繃 {@link RecipeParserAPI} 闈欐€佹柟娉曡皟鐢紝鎴栫洿鎺ヤ娇鐢?
 * {@link #getInstance()} 鑾峰彇鍗曚緥銆?
 * </p>
 */
public class RecipeParser {

    /**
     * Log prefix
     */
    /** 鏃ュ織鍓嶇紑 */
    public static final String LOG_PREFIX = "[CloudWorks-RecipeParser]";
    /**
     * Logger
     */
    /** 鏃ュ織璁板綍鍣?*/
    private static final Logger LOGGER = LoggerFactory.getLogger("CloudWorks-RecipeParser");

    /**
     * Template file directory
     */
    /** 妯℃澘鏂囦欢鐩綍 */
    private static final String TEMPLATE_DIR = "cloudworks/recipe_parser/templates";
    /**
     * Template config file directory
     */
    /** 妯℃澘閰嶇疆鏂囦欢鐩綍 */
    private static final String TEMPLATE_CONFIG_DIR = "cloudworks/recipe_parser/templates_config";
    /**
     * Recipe parser directory
     */
    /** 閰嶆柟瑙ｆ瀽鍣ㄧ洰褰?*/
    private static final String RECIPE_PARSER_DIR = "cloudworks/recipe_parser";
    /**
     * Update config file name
     */
    /** 鏇存柊閰嶇疆鏂囦欢鍚嶇О */
    private static final String UPDATE_CONFIG_FILE = "config.json";
    /**
     * Template resource path in JAR
     */
    /** JAR 涓ā鏉胯祫婧愯矾寰?*/
    private static final String JAR_TEMPLATE_PATH = "/cloudworks/recipe_parser/templates";
    /**
     * Template config resource path in JAR
     */
    /** JAR 涓ā鏉块厤缃祫婧愯矾寰?*/
    private static final String JAR_TEMPLATE_CONFIG_PATH = "/cloudworks/recipe_parser/templates_config";

    /**
     * Singleton instance
     */
    /** 鍗曚緥瀹炰緥 */
    private static RecipeParser instance;

    /**
     * Whether the module is enabled
     */
    /** 妯″潡鏄惁宸插惎鐢?*/
    private boolean enabled = false;
    /**
     * Registry accessor (available after server startup)
     */
    /** 娉ㄥ唽琛ㄨ闂櫒锛堟湇鍔″櫒鍚姩鍚庡彲鐢級 */
    private RegistryAccess registryAccess = null;
    /**
     * Template index: modid_recipetype -> Template
     */
    /** 妯℃澘绱㈠紩锛歮odid_recipetype -> Template */
    private final Map<String, Template> templateIndex = new HashMap<>();
    /**
     * Template config index: modid_recipetype -> recipeId -> TemplateConfig
     */
    /** 妯℃澘閰嶇疆绱㈠紩锛歮odid_recipetype -> recipeId -> TemplateConfig */
    private final Map<String, Map<String, TemplateConfig>> templateConfigs = new HashMap<>();

    /**
 * Gets the RecipeParser singleton instance.
 *
 * 鑾峰彇 RecipeParser 鍗曚緥瀹炰緥銆?
 *
 * @return the singleton instance
 * @return 鍗曚緥瀹炰緥
 */
    public static RecipeParser getInstance() {
        if (instance == null) {
            instance = new RecipeParser();
        }
        return instance;
    }

    /**
 * Initializes the RecipeParser module.
 *
 * 鍒濆鍖?RecipeParser 妯″潡銆?
 * <p>
 * 妫€鏌?{@link CloudworksRecipeParser} 娉ㄨВ鏄惁瀛樺湪锛?
 * 濡傛灉瀛樺湪鍒欐彁鍙栨ā鏉挎枃浠躲€佸姞杞芥ā鏉垮拰閰嶇疆锛屽苟鍚敤妯″潡銆?
 * 濡傛灉鍒濆鍖栧け璐ワ紝妯″潡灏嗚绂佺敤銆?
 * </p>
 */
    public void initialize() {
        if (!isAnnotationPresent()) {
            LOGGER.info("{} No @CloudworksRecipeParser annotation found. Module disabled.", LOG_PREFIX);
            return;
        }
        LOGGER.info("{} @CloudworksRecipeParser detected. Activating RecipeParser module...", LOG_PREFIX);

        Path templateDir = FMLPaths.GAMEDIR.get().resolve(TEMPLATE_DIR);
        Path configDir = FMLPaths.GAMEDIR.get().resolve(TEMPLATE_CONFIG_DIR);
        try {
            // Check update config first to determine what to extract
            Set<String> ignoreNames = checkUpdateConfig();

            extractTemplates(templateDir, ignoreNames);
            extractTemplateConfigs(configDir, ignoreNames);
            loadTemplates(templateDir);
            loadTemplateConfigs(configDir);
            enabled = true;
            printStatistics();
        } catch (Exception e) {
            LOGGER.error("{} ========================================", LOG_PREFIX);
            LOGGER.error("{} Failed to initialize RecipeParser module", LOG_PREFIX);
            LOGGER.error("{} * Reason: {}", LOG_PREFIX, e.getMessage());
            LOGGER.error("{} * Action: Please ensure the game directory is writable.", LOG_PREFIX);
            LOGGER.error("{} RecipeParser module disabled.", LOG_PREFIX);
            LOGGER.error("{} ========================================", LOG_PREFIX);
            enabled = false;
        }
    }

    /**
 * Checks whether the current mod scan data contains the {@link CloudworksRecipeParser} annotation.
 *
 * 妫€鏌ュ綋鍓嶆ā缁勬壂鎻忔暟鎹腑鏄惁鍖呭惈 {@link CloudworksRecipeParser} 娉ㄨВ銆?
 *
 * @return true if the annotation is present
 * @return 濡傛灉娉ㄨВ瀛樺湪鍒欒繑鍥?true
 */
    private boolean isAnnotationPresent() {
        Type annotationType = Type.getType(CloudworksRecipeParser.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (annotation.annotationType().equals(annotationType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
 * Gets the current mod version number.
 *
 * 鑾峰彇褰撳墠妯＄粍鐗堟湰鍙枫€?
 *
 * @return the version string, or "unknown" on failure
 * @return 鐗堟湰鍙峰瓧绗︿覆锛岃幏鍙栧け璐ユ椂杩斿洖 "unknown"
 */
    private String getModVersion() {
        try {
            return ModList.get().getModContainerById("cloudworks_api")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
 * Checks cloudworks/recipe_parser/config.json and decides whether an update is needed.
 *
 * 妫€鏌?cloudworks/recipe_parser/config.json 骞跺喅瀹氭槸鍚﹂渶瑕佹洿鏂般€?
 * <p>
 * 鏍规嵁閰嶇疆涓殑 version銆乪nable_update銆乫orce_update 鍜?update_ignore
 * 瀛楁鍐冲畾鎻愬彇绛栫暐銆傝繑鍥?update_ignore 涓寚瀹氱殑鏂囦欢鍚嶉泦鍚堬紝
 * 杩欎簺鏂囦欢鍦ㄦ彁鍙栨椂灏嗚璺宠繃銆俧orce_update 涓?true 鏃惰繑鍥炵┖闆嗗悎銆?
 * </p>
 *
 * @return the set of filenames to ignore during extraction
 * @return 鎻愬彇鏃堕渶瑕佸拷鐣ョ殑鏂囦欢鍚嶉泦鍚?
 */
    private Set<String> checkUpdateConfig() {
        Path recipeParserDir = FMLPaths.GAMEDIR.get().resolve(RECIPE_PARSER_DIR);
        Path configFile = recipeParserDir.resolve(UPDATE_CONFIG_FILE);
        String currentVersion = getModVersion();

        // If config.json doesn't exist, create it and release everything
        if (!Files.exists(configFile)) {
            LOGGER.info("{} No config.json found. Creating default config and releasing all files.", LOG_PREFIX);
            try {
                Files.createDirectories(recipeParserDir);
                writeUpdateConfig(configFile, currentVersion, true, false, Collections.emptyList());
            } catch (IOException e) {
                LOGGER.warn("{} Failed to create config.json: {}", LOG_PREFIX, e.getMessage());
            }
            return Collections.emptySet();
        }

        // Read existing config
        JsonObject config;
        try {
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            config = JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warn("{} Failed to parse config.json: {}. Releasing all files.", LOG_PREFIX, e.getMessage());
            try {
                writeUpdateConfig(configFile, currentVersion, true, false, Collections.emptyList());
            } catch (IOException ignored) {}
            return Collections.emptySet();
        }

        // --- force_update ---
        boolean forceUpdate = false;
        if (config.has("force_update")) {
            forceUpdate = config.get("force_update").getAsBoolean();
        } else {
            config.addProperty("force_update", false);
        }

        if (forceUpdate) {
            LOGGER.info("{} force_update=true, releasing all files.", LOG_PREFIX);
            try {
                config.addProperty("version", currentVersion);
                writeUpdateConfigRaw(configFile, config);
            } catch (IOException ignored) {}
            return Collections.emptySet();
        }

        // --- enable_update ---
        boolean enableUpdate;
        if (config.has("enable_update")) {
            enableUpdate = config.get("enable_update").getAsBoolean();
        } else {
            enableUpdate = true;
            config.addProperty("enable_update", true);
        }

        if (!enableUpdate) {
            LOGGER.info("{} enable_update=false, skipping update.", LOG_PREFIX);
            return collectIgnoreNames(config); // still return ignores for reference
        }

        // --- version ---
        if (config.has("version")) {
            String storedVersion = config.get("version").getAsString();
            if (currentVersion.equals(storedVersion)) {
                LOGGER.info("{} Version matches ({}), skipping update.", LOG_PREFIX, currentVersion);
                return collectIgnoreNames(config);
            }
            LOGGER.info("{} Version mismatch: stored={}, current={}. Updating...", LOG_PREFIX, storedVersion, currentVersion);
        } else {
            config.addProperty("version", currentVersion);
            LOGGER.info("{} No version field in config.json. Setting to {} and updating...", LOG_PREFIX, currentVersion);
        }

        // Update version and write back
        config.addProperty("version", currentVersion);
        try {
            writeUpdateConfigRaw(configFile, config);
        } catch (IOException e) {
            LOGGER.warn("{} Failed to write updated config.json: {}", LOG_PREFIX, e.getMessage());
        }

        return collectIgnoreNames(config);
    }

    /**
 * Collects the update_ignore list from the config JSON.
 *
 * 浠庨厤缃?JSON 涓敹闆?update_ignore 鍒楄〃銆?
 *
 * @param config the config JSON object
 * @param config 閰嶇疆 JSON 瀵硅薄
 * @return the set of ignored filenames
 * @return 蹇界暐鏂囦欢鍚嶉泦鍚?
 */
    private Set<String> collectIgnoreNames(JsonObject config) {
        Set<String> ignore = new HashSet<>();
        if (config.has("update_ignore") && config.get("update_ignore").isJsonArray()) {
            for (JsonElement e : config.get("update_ignore").getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    ignore.add(e.getAsString());
                }
            }
        }
        return ignore;
    }

    /**
 * Writes the update config file.
 *
 * 鍐欏叆鏇存柊閰嶇疆鏂囦欢銆?
 *
 * @param configFile the config file path
 * @param version the version number
 * @param enableUpdate whether to enable update
 * @param forceUpdate whether to force update
 * @param ignoreList the ignore file list
 * @param configFile   閰嶇疆鏂囦欢璺緞
 * @param version      鐗堟湰鍙?
 * @param enableUpdate 鏄惁鍚敤鏇存柊
 * @param forceUpdate  鏄惁寮哄埗鏇存柊
 * @param ignoreList   蹇界暐鏂囦欢鍒楄〃
 * @throws IOException if writing fails
 * @throws IOException 濡傛灉鍐欏叆澶辫触
 */
    private void writeUpdateConfig(Path configFile, String version, boolean enableUpdate,
                                    boolean forceUpdate, List<String> ignoreList) throws IOException {
        JsonObject config = new JsonObject();
        config.addProperty("version", version);
        config.addProperty("enable_update", enableUpdate);
        config.addProperty("force_update", forceUpdate);
        JsonArray arr = new JsonArray();
        for (String name : ignoreList) {
            arr.add(name);
        }
        config.add("update_ignore", arr);
        writeUpdateConfigRaw(configFile, config);
    }

    /**
 * Writes the JSON config object to a file (with formatting).
 *
 * 灏?JSON 閰嶇疆瀵硅薄鍐欏叆鏂囦欢锛堝甫鏍煎紡鍖栵級銆?
 *
 * @param configFile the config file path
 * @param config the JSON config object
 * @param configFile 閰嶇疆鏂囦欢璺緞
 * @param config     JSON 閰嶇疆瀵硅薄
 * @throws IOException if writing fails
 * @throws IOException 濡傛灉鍐欏叆澶辫触
 */
    private void writeUpdateConfigRaw(Path configFile, JsonObject config) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonStr = gson.toJson(config);
        Files.writeString(configFile, jsonStr, StandardCharsets.UTF_8);
    }

    /**
 * Extracts built-in template files from JAR resources to the game directory.
 *
 * 浠?JAR 璧勬簮涓彁鍙栧唴缃ā鏉挎枃浠跺埌娓告垙鐩綍銆?
 *
 * @param templateDir the template output directory
 * @param ignoreNames the set of filenames to ignore
 * @param templateDir 妯℃澘杈撳嚭鐩綍
 * @param ignoreNames 闇€瑕佸拷鐣ョ殑鏂囦欢鍚嶉泦鍚?
 * @throws IOException if extraction fails
 * @throws IOException 濡傛灉鎻愬彇澶辫触
 */
    private void extractTemplates(Path templateDir, Set<String> ignoreNames) throws IOException {
        LOGGER.info("{} Extracting built-in templates...", LOG_PREFIX);

        Files.createDirectories(templateDir);

        // Scan JAR resources for templates
        List<String> templateFiles = getBuiltInTemplateList();
        if (templateFiles.isEmpty()) {
            LOGGER.info("{} No built-in templates found in JAR.", LOG_PREFIX);
            return;
        }

        int count = 0;
        int skipped = 0;
        for (String fileName : templateFiles) {
            // Check if this file should be ignored (skip only if the file already exists)
            if (ignoreNames.contains(fileName)) {
                Path targetFile = templateDir.resolve(fileName);
                if (Files.exists(targetFile)) {
                    LOGGER.info("{} * {} (skipped - in update_ignore)", LOG_PREFIX, fileName);
                    skipped++;
                    continue;
                }
            }

            String resourcePath = JAR_TEMPLATE_PATH + "/" + fileName;
            try (InputStream in = openJarResource(resourcePath)) {
                if (in == null) {
                    LOGGER.warn("{} Template resource not found: {}", LOG_PREFIX, resourcePath);
                    continue;
                }
                Path targetFile = templateDir.resolve(fileName);
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("{} * {} -> {}/", LOG_PREFIX, fileName, TEMPLATE_DIR);
                count++;
            }
        }

        LOGGER.info("{} Template extraction complete. {} released, {} skipped.", LOG_PREFIX, count, skipped);
    }

    /**
 * Tries multiple ClassLoader approaches to open resource files in the JAR.
 *
 * 灏濊瘯澶氱 ClassLoader 鏂瑰紡鏉ユ墦寮€ JAR 涓殑璧勬簮鏂囦欢銆?
 * <p>
 * 渚濇灏濊瘯锛欳ontextClassLoader銆丆lass.getResourceAsStream銆?
 * Class.getClassLoader銆丯eoForge ModList API銆?
 * </p>
 *
 * @param resourcePath the resource path (starting with "/")
 * @param resourcePath 璧勬簮璺緞锛堜互 "/" 寮€澶达級
 * @return the input stream, or null if the resource cannot be found
 * @return 杈撳叆娴侊紝濡傛灉鏃犳硶鎵惧埌璧勬簮鍒欒繑鍥?null
 */
    private InputStream openJarResource(String resourcePath) {
        // Strip leading / for ClassLoader-based approaches
        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

        InputStream in = null;

        // Approach 1: ContextClassLoader
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) in = cl.getResourceAsStream(path);
        } catch (Exception ignored) {}

        // Approach 2: Class.getResourceAsStream
        if (in == null) {
            try {
                in = getClass().getResourceAsStream(resourcePath);
            } catch (Exception ignored) {}
        }

        // Approach 3: Class.getClassLoader
        if (in == null) {
            try {
                ClassLoader cl = getClass().getClassLoader();
                if (cl != null) in = cl.getResourceAsStream(path);
            } catch (Exception ignored) {}
        }

        // Approach 4: NeoForge ModList API (most reliable in modded environment)
        if (in == null) {
            try {
                var modFile = ModList.get().getModFileById("cloudworks_api");
                if (modFile != null) {
                    Path foundPath = modFile.getFile().findResource(path);
                    if (foundPath != null) {
                        in = Files.newInputStream(foundPath);
                    }
                }
            } catch (Exception ignored) {}
        }

        return in;
    }

    /**
 * 鑾峰彇 JAR 涓唴缃ā鏉挎枃浠跺垪琛ㄣ€?
 * 浠?template-list.txt 涓鍙栨ā鏉垮悕绉帮紝骞朵负姣忎釜鍚嶇О杩藉姞 .rpml 鍚庣紑銆?
 *
 *
 * 鑾峰彇 JAR 涓唴缃ā鏉挎枃浠跺垪琛ㄣ€?
 * 浠?template-list.txt 涓鍙栨ā鏉垮悕绉帮紝骞朵负姣忎釜鍚嶇О杩藉姞 .rpml 鍚庣紑銆?
 *
 * @return the list of template filenames (with .rpml suffix)
 * @return 妯℃澘鏂囦欢鍚嶅垪琛紙鍚?.rpml 鍚庣紑锛?
 */
    private List<String> getBuiltInTemplateList() {
        List<String> result = new ArrayList<>();
        String listPath = JAR_TEMPLATE_PATH + "/template-list.txt";

        try (InputStream listIn = openJarResource(listPath)) {
            if (listIn == null) {
                LOGGER.warn("{} Could not find template-list.txt via any classloader approach", LOG_PREFIX);
                return result;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(listIn, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    result.add(line + ".rpml");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("{} Could not read template-list.txt: {}", LOG_PREFIX, e.getMessage());
        }

        return result;
    }

    /**
 * 鍔犺浇鎸囧畾鐩綍涓嬬殑鎵€鏈?.rpml 妯℃澘鏂囦欢銆?
 * 鏂囦欢鍚嶆牸寮忚姹備负 {@code <modid>_<recipetype>.rpml}銆?
 *
 *
 * 鍔犺浇鎸囧畾鐩綍涓嬬殑鎵€鏈?.rpml 妯℃澘鏂囦欢銆?
 * 鏂囦欢鍚嶆牸寮忚姹備负 {@code <modid>_<recipetype>.rpml}銆?
 *
 * @param templateDir 妯℃澘鐩綍
 * @param templateDir 妯℃澘鐩綍
 * @throws IOException if reading fails
 * @throws IOException 濡傛灉璇诲彇澶辫触
 */
    private void loadTemplates(Path templateDir) throws IOException {
        LOGGER.info("{} Loading templates from {}...", LOG_PREFIX, templateDir);

        if (!Files.exists(templateDir)) {
            LOGGER.info("{} Template directory does not exist. No templates loaded.", LOG_PREFIX);
            return;
        }

        List<Path> rpmlFiles;
        try (var stream = Files.list(templateDir)) {
            rpmlFiles = stream
                    .filter(p -> p.toString().endsWith(".rpml"))
                    .collect(Collectors.toList());
        }

        for (Path file : rpmlFiles) {
            String fileName = file.getFileName().toString();
            String nameWithoutExt = fileName.substring(0, fileName.length() - 5); // remove .rpml

            // Parse modid_recipetype
            int underscoreIdx = nameWithoutExt.indexOf('_');
            if (underscoreIdx <= 0) {
                LOGGER.warn("{} Invalid template filename: {} (expected <modid>_<recipetype>.rpml)", LOG_PREFIX, fileName);
                continue;
            }

            String modId = nameWithoutExt.substring(0, underscoreIdx);
            String recipeType = nameWithoutExt.substring(underscoreIdx + 1);

            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                Template template = parseTemplate(modId, recipeType, content);
                String key = modId + "_" + recipeType;
                templateIndex.put(key, template);
                LOGGER.info("{} Loaded template: {} ({} markers)", LOG_PREFIX, fileName, countMarkers(template.getRoot()));
            } catch (TemplateParser.ParseException e) {
                LOGGER.error("{} Failed to parse template: {}", LOG_PREFIX, fileName);
                LOGGER.error("{} * Reason: {}", LOG_PREFIX, e.getMessage());
            } catch (Exception e) {
                LOGGER.error("{} Error loading template: {} - {}", LOG_PREFIX, fileName, e.getMessage());
            }
        }
    }

    /**
 * Extracts built-in template config files from JAR resources to the game directory.
 *
 * 浠?JAR 璧勬簮涓彁鍙栧唴缃ā鏉块厤缃枃浠跺埌娓告垙鐩綍銆?
 *
 * @param configDir the config output directory
 * @param ignoreNames the set of filenames to ignore
 * @param configDir   閰嶇疆杈撳嚭鐩綍
 * @param ignoreNames 闇€瑕佸拷鐣ョ殑鏂囦欢鍚嶉泦鍚?
 * @throws IOException if extraction fails
 * @throws IOException 濡傛灉鎻愬彇澶辫触
 */
    private void extractTemplateConfigs(Path configDir, Set<String> ignoreNames) throws IOException {
        LOGGER.info("{} Extracting built-in template configs...", LOG_PREFIX);
        Files.createDirectories(configDir);

        try (InputStream listIn = openJarResource(JAR_TEMPLATE_CONFIG_PATH + "/template-config-list.txt")) {
            if (listIn == null) {
                LOGGER.info("{} No template-config-list.txt found in JAR. Skipping config extraction.", LOG_PREFIX);
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(listIn, StandardCharsets.UTF_8));
            String line;
            int count = 0;
            int skipped = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String configFileName = line + ".json";

                // Check if this config file should be ignored
                if (ignoreNames.contains(configFileName)) {
                    Path targetFile = configDir.resolve(configFileName);
                    if (Files.exists(targetFile)) {
                        LOGGER.info("{} * {} (skipped - in update_ignore)", LOG_PREFIX, configFileName);
                        skipped++;
                        continue;
                    }
                }

                String resourcePath = JAR_TEMPLATE_CONFIG_PATH + "/" + configFileName;
                try (InputStream in = openJarResource(resourcePath)) {
                    if (in == null) {
                        LOGGER.warn("{} Config resource not found: {}", LOG_PREFIX, resourcePath);
                        continue;
                    }
                    Path targetFile = configDir.resolve(configFileName);
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("{} * {} -> {}/", LOG_PREFIX, configFileName, TEMPLATE_CONFIG_DIR);
                    count++;
                }
            }
            LOGGER.info("{} Template config extraction complete. {} released, {} skipped.", LOG_PREFIX, count, skipped);
        }
    }

    /**
 * 鍔犺浇鎵€鏈夋ā鏉跨殑閰嶇疆鏂囦欢銆?
 * 浠呭綋妯℃澘鐨勫叏灞€璁剧疆涓?global_enable_template_config 涓?true 鏃舵墠鍔犺浇銆?
 *
 * 鍔犺浇鎵€鏈夋ā鏉跨殑閰嶇疆鏂囦欢銆?
 * 浠呭綋妯℃澘鐨勫叏灞€璁剧疆涓?global_enable_template_config 涓?true 鏃舵墠鍔犺浇銆?
 */
    private void loadTemplateConfigs(Path configDir) {
        templateConfigs.clear();

        if (!Files.exists(configDir)) return;

        for (Template template : templateIndex.values()) {
            String templateKey = template.getModId() + "_" + template.getRecipeType();
            GlobalSettings settings = template.getGlobalSettings();

            // Only load config if global_enable_template_config is true
            if (!settings.isGlobalEnableTemplateConfig()) continue;

            Map<String, TemplateConfig> configs = TemplateConfigManager.loadConfig(configDir, templateKey);
            if (!configs.isEmpty()) {
                templateConfigs.put(templateKey, configs);
                LOGGER.info("{} Loaded {} per-recipe configs for template: {}", LOG_PREFIX, configs.size(), templateKey);
            } else {
                // Warn if global_enable_template_config is true but no config file exists
                LOGGER.warn("{} global_enable_template_config=true but no config file found for template: {}", LOG_PREFIX, templateKey);
            }
        }
    }

    /**
 * 瑙ｆ瀽 RPML 妯℃澘鍐呭涓?Template 瀵硅薄銆?
 * 鎵ц璇嶆硶鍒嗘瀽銆佽娉曡В鏋愩€侀獙璇佸拰鍏ㄥ眬璁剧疆鎻愬彇銆?
 *
 *
 * 瑙ｆ瀽 RPML 妯℃澘鍐呭涓?Template 瀵硅薄銆?
 * 鎵ц璇嶆硶鍒嗘瀽銆佽娉曡В鏋愩€侀獙璇佸拰鍏ㄥ眬璁剧疆鎻愬彇銆?
 *
 * @param modId the mod ID
 * @param recipeType the recipe type
 * @param content the RPML template content
 * @param modId      妯＄粍ID
 * @param recipeType 閰嶆柟绫诲瀷
 * @param content    RPML 妯℃澘鍐呭
 * @return the parsed Template object
 * @return 瑙ｆ瀽鍚庣殑 Template 瀵硅薄
 * @throws TemplateParser.ParseException if template validation fails
 * @throws TemplateParser.ParseException 濡傛灉妯℃澘楠岃瘉澶辫触
 */
    private Template parseTemplate(String modId, String recipeType, String content) {
        TemplateTokenizer tokenizer = new TemplateTokenizer(content);
        List<Token> tokens = tokenizer.tokenize();
        TemplateParser parser = new TemplateParser(tokens);
        TemplateNode root = parser.parse();

        // Validate
        TemplateValidator.ValidationResult validation = TemplateValidator.validate(null, root);
        if (!validation.isSuccess()) {
            LOGGER.error("{} ========================================", LOG_PREFIX);
            LOGGER.error("{} Template Validation Failed", LOG_PREFIX);
            LOGGER.error("{} * Mod ID     : {}", LOG_PREFIX, modId);
            LOGGER.error("{} * Recipe Type : {}", LOG_PREFIX, recipeType);
            LOGGER.error("{} * Template File: {}_{}.rpml", LOG_PREFIX, modId, recipeType);
            for (String error : validation.getErrors()) {
                LOGGER.error("{} * Reason : {}", LOG_PREFIX, error);
            }
            LOGGER.error("{} * Action : PLEASE FIX TEMPLATE", LOG_PREFIX);
            LOGGER.error("{} ========================================", LOG_PREFIX);
            throw new TemplateParser.ParseException("Template validation failed: " + String.join(", ", validation.getErrors()));
        }

        // Extract global settings from <script> markers
        GlobalSettings settings = extractGlobalSettings(root);

        return new Template(modId, recipeType, root, settings);
    }

    /**
 * Traverses the template AST, collecting all SCRIPT marker parameters into a GlobalSettings object.
 *
 * 閬嶅巻妯℃澘璇硶鏍戯紝鏀堕泦鎵€鏈?SCRIPT 鏍囪鐨勫弬鏁板埌 GlobalSettings 瀵硅薄銆?
 *
 * @param node the AST node
 * @param node 璇硶鏍戣妭鐐?
 * @return the extracted global settings
 * @return 鎻愬彇鐨勫叏灞€璁剧疆
 */
    private GlobalSettings extractGlobalSettings(TemplateNode node) {
        GlobalSettings settings = new GlobalSettings();
        extractGlobalSettingsRecursive(node, settings);
        return settings;
    }

    /**
 * Recursively extracts global settings.
 *
 * 閫掑綊鎻愬彇鍏ㄥ眬璁剧疆銆?
 *
 * @param node the AST node
 * @param settings the global settings object
 * @param node     璇硶鏍戣妭鐐?
 * @param settings 鍏ㄥ眬璁剧疆瀵硅薄
 */
    private void extractGlobalSettingsRecursive(TemplateNode node, GlobalSettings settings) {
        if (node.getMarker() != null && node.getMarker().getMarkerType() == MarkerDef.MarkerType.SCRIPT) {
            MarkerDef marker = node.getMarker();
            for (ParameterOp param : marker.getParameters()) {
                settings.applyScriptParam(param.getKey(), param.getValue());
            }
        }
        for (TemplateNode child : node.getChildren()) {
            extractGlobalSettingsRecursive(child, settings);
        }
    }

    /**
 * Recursively counts the number of markers in the template AST.
 *
 * 閫掑綊缁熻妯℃澘璇硶鏍戜腑鐨勬爣璁版暟閲忋€?
 *
 * @param node the AST node
 * @param node 璇硶鏍戣妭鐐?
 * @return the total number of markers
 * @return 鏍囪鎬绘暟
 */
    private int countMarkers(TemplateNode node) {
        int count = 0;
        if (node.getMarker() != null) count++;
        for (TemplateNode child : node.getChildren()) {
            count += countMarkers(child);
        }
        return count;
    }

    /**
     * Prints initialization statistics.
     */
    /** 鎵撳嵃鍒濆鍖栫粺璁′俊鎭€?*/
    private void printStatistics() {
        LOGGER.info("{} ========================================", LOG_PREFIX);
        LOGGER.info("{} RecipeParser Initialization Complete", LOG_PREFIX);
        LOGGER.info("{} * Templates loaded: {}", LOG_PREFIX, templateIndex.size());
        for (Template template : templateIndex.values()) {
            LOGGER.info("{} * {}:{}", LOG_PREFIX, template.getModId(), template.getRecipeType());
        }
        LOGGER.info("{} ========================================", LOG_PREFIX);
    }

    // --- Public API methods ---

    /**
 * Checks whether the RecipeParser module is enabled.
 *
 * 妫€鏌?RecipeParser 妯″潡鏄惁宸插惎鐢ㄣ€?
 *
 * @return true if the module is enabled
 * @return 濡傛灉妯″潡宸插惎鐢ㄥ垯杩斿洖 true
 */
    public boolean isEnabled() {
        return enabled;
    }

    /**
 * Gets the parsed data for the specified recipe ID.
 *
 * 鑾峰彇鎸囧畾閰嶆柟ID鐨勮В鏋愭暟鎹€?
 * <p>
 * 鏌ユ壘閰嶆柟瀵瑰簲鐨勬ā鏉匡紝灏嗛厤鏂瑰簭鍒楀寲涓?JSON锛岀劧鍚庝娇鐢ㄦā鏉挎彁鍙栬緭鍏?杈撳嚭鏁版嵁銆?
 * </p>
 *
 * @param recipeId the recipe ID
 * @param recipeManager the recipe manager
 * @param recipeId      閰嶆柟ID
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return the parsed recipe data
 * @return 瑙ｆ瀽鍚庣殑閰嶆柟鏁版嵁
 * @throws RecipeNotFoundException if the specified recipe is not found
 * @throws RecipeParseException if the module is not enabled, template not found, or parsing fails
 * @throws RecipeNotFoundException 濡傛灉鎵句笉鍒版寚瀹氶厤鏂?
 * @throws RecipeParseException    濡傛灉妯″潡鏈惎鐢ㄣ€佹ā鏉夸笉瀛樺湪鎴栬В鏋愬け璐?
 */
    public RecipeData getRecipeData(ResourceLocation recipeId, RecipeManager recipeManager)
            throws RecipeNotFoundException, RecipeParseException {
        if (!enabled) {
            throw new RecipeParseException("RecipeParser module is not enabled");
        }

        Optional<RecipeHolder<?>> recipeOpt = recipeManager.byKey(recipeId);
        if (recipeOpt.isEmpty()) {
            throw new RecipeNotFoundException("Recipe not found: " + recipeId);
        }

        RecipeHolder<?> holder = recipeOpt.get();
        Recipe<?> recipe = holder.value();
        String recipeTypeName = getRecipeTypeKey(recipe);
        String key = recipeTypeName.replace(':', '_');
        Template template = templateIndex.get(key);

        if (template == null) {
            LOGGER.warn("{} No template for type={} (lookup key={})", LOG_PREFIX, recipeTypeName, key);
            throw new RecipeParseException("No template found for type=" + recipeTypeName);
        }

        try {
            // Serialize recipe to JSON
            JsonElement recipeJson = serializeRecipe(recipe);
            RecipeExtractor extractor = new RecipeExtractor(template, recipeJson, recipeId.toString());
            return extractor.extract();
        } catch (Exception e) {
            LOGGER.error("{} ========================================", LOG_PREFIX);
            LOGGER.error("{} Recipe Extraction Failed", LOG_PREFIX);
            LOGGER.error("{} * Mod ID     : {}", LOG_PREFIX, recipeTypeName.split(":")[0]);
            LOGGER.error("{} * Recipe Type : {}", LOG_PREFIX, recipeTypeName);
            LOGGER.error("{} * Recipe ID  : {}", LOG_PREFIX, recipeId);
            LOGGER.error("{} * Reason : {}", LOG_PREFIX, e.getMessage());
            LOGGER.error("{} * Action : UPDATE TEMPLATE via external AI sampler", LOG_PREFIX);
            LOGGER.error("{} ========================================", LOG_PREFIX);
            throw new RecipeParseException("Recipe extraction failed: " + e.getMessage());
        }
    }

    /**
 * Batch gets parsed data for multiple recipe IDs.
 *
 * 鎵归噺鑾峰彇澶氫釜閰嶆柟ID鐨勮В鏋愭暟鎹€?
 * 鍗曚釜閰嶆柟瑙ｆ瀽澶辫触浠呰褰曡鍛婃棩蹇楋紝涓嶅奖鍝嶅叾浠栭厤鏂圭殑瑙ｆ瀽銆?
 *
 * @param recipeIds the set of recipe IDs
 * @param recipeManager the recipe manager
 * @param recipeIds     閰嶆柟ID闆嗗悎
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return a map of recipe IDs to parsed data (only successfully parsed recipes)
 * @return 閰嶆柟ID鍒拌В鏋愭暟鎹殑鏄犲皠锛堜粎鍖呭惈鎴愬姛瑙ｆ瀽鐨勯厤鏂癸級
 */
    public Map<ResourceLocation, RecipeData> getRecipeDataBatch(Collection<ResourceLocation> recipeIds, RecipeManager recipeManager) {
        Map<ResourceLocation, RecipeData> results = new LinkedHashMap<>();
        for (ResourceLocation id : recipeIds) {
            try {
                results.put(id, getRecipeData(id, recipeManager));
            } catch (Exception e) {
                LOGGER.warn("{} Skipping recipe {}: {}", LOG_PREFIX, id, e.getMessage());
            }
        }
        return results;
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
    public boolean isRecipeParsable(ResourceLocation recipeId, RecipeManager recipeManager) {
        if (!enabled) return false;
        Optional<RecipeHolder<?>> recipeOpt = recipeManager.byKey(recipeId);
        if (recipeOpt.isEmpty()) return false;
        Recipe<?> recipe = recipeOpt.get().value();
        String key = getRecipeTypeKey(recipe).replace(':', '_');
        return templateIndex.containsKey(key);
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
    public List<ResourceLocation> getParsableRecipes(String modId, String recipeType, RecipeManager recipeManager) {
        List<ResourceLocation> result = new ArrayList<>();
        if (!enabled) return result;

        String key = modId + "_" + recipeType;
        if (!templateIndex.containsKey(key)) return result;

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            ResourceLocation id = holder.id();
            if (id != null && id.getNamespace().equals(modId) && getRecipeTypeKey(holder.value()).equals(recipeType)) {
                result.add(id);
            }
        }
        return result;
    }

    /**
 * Finds all recipes that produce the specified target item/fluid.
 *
 * 鏌ユ壘鎵€鏈変骇鍑烘寚瀹氱洰鏍囩墿鍝?娴佷綋鐨勯厤鏂广€?
 * 瀵逛簬 ITEM 妯″紡锛屽悓鏃跺寘鍚洿鎺ヤ骇鍑哄尮閰嶅拰娴佷綋鍒扮墿鍝佺殑杞崲閰嶆柟銆?
 * 瀵逛簬 FLUID 妯″紡锛屽悓鏃跺寘鍚洿鎺ユ祦浣撲骇鍑哄尮閰嶅拰鍙嶅悜杞崲鍖归厤銆?
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
    public List<RecipeParseResult> parseProduceRecipe(ResourceLocation targetId, QueryMode mode, RecipeManager recipeManager) {
        List<RecipeParseResult> results = new ArrayList<>();

        if (mode == QueryMode.ITEM) {
            // Direct getResultItem matches
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                try {
                    ItemStack result = holder.value().getResultItem(null);
                    if (!result.isEmpty()) {
                        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
                        if (targetId.equals(resultId)) {
                            RecipeData data = getRecipeData(holder.id(), recipeManager);
                            results.add(new RecipeParseResult(holder.id(), data));
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Fluid->item transfer matches
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                try {
                    ResourceLocation rid = holder.id();
                    boolean alreadyAdded = false;
                    for (RecipeParseResult r : results) {
                        if (r.getRecipeId().equals(rid)) { alreadyAdded = true; break; }
                    }
                    if (alreadyAdded) continue;

                    List<TransferEntry> entries = computeTransferResults(holder, recipeManager);
                    if (entries != null) {
                        for (TransferEntry e : entries) {
                            if (targetId.toString().equals(e.itemId)) {
                                RecipeData data = getRecipeData(holder.id(), recipeManager);
                                results.add(new RecipeParseResult(holder.id(), data));
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

        } else {
            // FLUID mode: direct fluid output matches
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                try {
                    RecipeData data = getRecipeData(holder.id(), recipeManager);
                    for (Product prod : data.getOutputs()) {
                        if ("fluid".equals(prod.getUnit()) && targetId.toString().equals(prod.getId())) {
                            results.add(new RecipeParseResult(holder.id(), data));
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Also check fluid->item transfer reverse-match
            if (BuiltInRegistries.ITEM.containsKey(targetId)) {
                for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                    try {
                        ResourceLocation rid = holder.id();
                        boolean alreadyAdded = false;
                        for (RecipeParseResult r : results) {
                            if (r.getRecipeId().equals(rid)) { alreadyAdded = true; break; }
                        }
                        if (alreadyAdded) continue;

                        List<TransferEntry> entries = computeTransferResults(holder, recipeManager);
                        if (entries != null) {
                            for (TransferEntry e : entries) {
                                if (targetId.toString().equals(e.itemId)) {
                                    RecipeData data = getRecipeData(holder.id(), recipeManager);
                                    results.add(new RecipeParseResult(holder.id(), data));
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return results;
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
    public List<RecipeParseResult> parseUsageRecipe(ResourceLocation targetId, QueryMode mode, RecipeManager recipeManager) {
        List<RecipeParseResult> results = new ArrayList<>();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            try {
                RecipeData data = getRecipeData(holder.id(), recipeManager);
                boolean matches = false;

                for (Ingredient ing : data.getInputs()) {
                    if (mode == QueryMode.ITEM && "item".equals(ing.getUnit())) {
                        if (targetId.toString().equals(ing.getId())) {
                            matches = true;
                            break;
                        }
                    } else if (mode == QueryMode.FLUID && "fluid".equals(ing.getUnit())) {
                        if (targetId.toString().equals(ing.getId())) {
                            matches = true;
                            break;
                        }
                    }
                }

                if (matches) {
                    results.add(new RecipeParseResult(holder.id(), data));
                }
            } catch (Exception ignored) {}
        }

        return results;
    }

    // --- Fluid->Item transfer helpers ---

    /**
     * Fluid-to-item transfer result entry.
     */
    /** 娴佷綋鍒扮墿鍝佽浆鎹㈢粨鏋滄潯鐩€?*/
    private static class TransferEntry {
        /**
     * Item ID
     */
    /** 鐗╁搧ID */
        final String itemId;
        /**
     * Count
     */
    /** 鏁伴噺 */
        final int count;

        TransferEntry(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }
    }

    /**
 * Computes the fluid-to-item transfer results for a recipe.
 *
 * 璁＄畻閰嶆柟鐨勬祦浣撳埌鐗╁搧杞崲缁撴灉銆?
 * <p>
 * 鏍规嵁妯℃澘鐨勫叏灞€璁剧疆鎴栭厤鏂归厤缃腑鐨勮浆鎹㈡柟娉曪紝灏嗘祦浣撲骇鍑鸿浆鎹负瀵瑰簲鐨勭墿鍝佷骇鍑恒€?
 * </p>
 *
 * @param holder the recipe holder
 * @param recipeManager the recipe manager
 * @param holder        閰嶆柟鎸佹湁鑰?
 * @param recipeManager 閰嶆柟绠＄悊鍣?
 * @return the list of transfer result entries, or null if transfer is not needed or unavailable
 * @return 杞崲缁撴灉鏉＄洰鍒楄〃锛屽鏋滄棤闇€杞崲鎴栬浆鎹笉鍙敤鍒欒繑鍥?null
 */
    private List<TransferEntry> computeTransferResults(RecipeHolder<?> holder, RecipeManager recipeManager) {
        Recipe<?> recipe = holder.value();
        String recipeTypeKey = getRecipeTypeKey(recipe).replace(':', '_');
        Template template = templateIndex.get(recipeTypeKey);
        if (template == null) return null;

        GlobalSettings settings = template.getGlobalSettings();
        if (!settings.isGlobalFluidTransfer()) return null;

        RecipeData data;
        try {
            data = getRecipeData(holder.id(), recipeManager);
        } catch (Exception e) {
            return null;
        }

        List<Product> fluidOutputs = new ArrayList<>();
        for (Product prod : data.getOutputs()) {
            if ("fluid".equals(prod.getUnit())) {
                fluidOutputs.add(prod);
            }
        }
        if (fluidOutputs.isEmpty()) return null;

        TemplateConfig config = getRecipeConfig(recipeTypeKey, holder.id().toString());
        List<TransferEntry> results = new ArrayList<>();

        if (config != null) {
            if (!config.isEnableTransfer()) return null;
            for (Product fluidProd : fluidOutputs) {
                if (config.getTransferBlacklist().contains(fluidProd.getId())) continue;
                for (TemplateConfig.TransferMethod method : config.getMethods()) {
                    double totalFluid = fluidProd.getCount();
                    double rate = method.getRate();
                    int totalMapped = GlobalSettings.applyRound(totalFluid / rate, method.getRound());
                    for (Map.Entry<String, Integer> entry : method.getResult().entrySet()) {
                        int count = entry.getValue() * totalMapped;
                        if (count > 0) {
                            results.add(new TransferEntry(entry.getKey(), count));
                        }
                    }
                }
            }
        } else {
            for (Product fluidProd : fluidOutputs) {
                double totalFluid = fluidProd.getCount();
                double rate = settings.getGlobalDefaultTransferRate();
                int totalMapped = GlobalSettings.applyRound(totalFluid / rate, settings.getGlobalDefaultTransferFloatRound());
                String resultId = settings.getGlobalDefaultTransferResult();
                if (resultId == null) {
                    resultId = fluidProd.getId();
                }
                if (totalMapped > 0) {
                    results.add(new TransferEntry(resultId, totalMapped));
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
 * 灏嗛厤鏂瑰簭鍒楀寲涓?JSON 鏍煎紡銆?
 * 浣跨敤 RegistryOps 鏀寔 Holder 寮曠敤锛堝鑽按銆侀檮榄旂瓑锛夈€?
 *
 *
 * 灏嗛厤鏂瑰簭鍒楀寲涓?JSON 鏍煎紡銆?
 * 浣跨敤 RegistryOps 鏀寔 Holder 寮曠敤锛堝鑽按銆侀檮榄旂瓑锛夈€?
 *
 * @param recipe the recipe object
 * @param recipe 閰嶆柟瀵硅薄
 * @return the JSON representation of the recipe
 * @return 閰嶆柟鐨?JSON 琛ㄧず
 */
    private JsonElement serializeRecipe(Recipe<?> recipe) {
        // Use RegistryOps to support Holder references (potion, enchantment, etc.)
        com.mojang.serialization.JsonOps baseOps = com.mojang.serialization.JsonOps.INSTANCE;
        if (registryAccess != null) {
            var ops = RegistryOps.create(baseOps, registryAccess);
            var result = net.minecraft.world.item.crafting.Recipe.CODEC.encodeStart(ops, recipe);
            return result.getOrThrow().getAsJsonObject();
        }
        // Fallback for edge cases where registryAccess is not yet available
        var result = net.minecraft.world.item.crafting.Recipe.CODEC.encodeStart(baseOps, recipe);
        return result.getOrThrow().getAsJsonObject();
    }

    /**
 * Gets the canonical type key for the recipe, in the format "namespace:path".
 *
 * 鑾峰彇閰嶆柟鐨勮鑼冪被鍨嬮敭锛屾牸寮忎负 "namespace:path"銆?
 * <p>
 * 浣跨敤 BuiltInRegistries 鑾峰彇瀹屾暣鐨?ResourceLocation銆?
 * 瀵逛簬 crafting 绫诲瀷锛屽尯鍒?shaped 鍜?shapeless 瀛愮被鍨嬨€?
 * </p>
 *
 * @param recipe the recipe object
 * @param recipe 閰嶆柟瀵硅薄
 * @return the recipe type key (e.g., "minecraft:crafting_shaped")
 * @return 閰嶆柟绫诲瀷閿紙濡?"minecraft:crafting_shaped"锛?
 */
    public static String getRecipeTypeKey(Recipe<?> recipe) {
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        if (key != null) {
            // For crafting recipes, differentiate between shaped and shapeless.
            // BuiltInRegistries returns "minecraft:crafting" for both, but we have
            // separate templates for each sub-type.
            if (key.toString().equals("minecraft:crafting")) {
                if (recipe instanceof ShapedRecipe) {
                    return "minecraft:crafting_shaped";
                }
                if (recipe instanceof ShapelessRecipe) {
                    return "minecraft:crafting_shapeless";
                }
            }
            return key.toString();
        }
        // Fallback for unregistered types
        return recipe.getType().toString();
    }

    // --- Debug Output ---

    /**
 * 鏈嶅姟鍣ㄥ惎鍔ㄤ簨浠跺洖璋冿紝瑙﹀彂瀹屾暣鐨勯厤鏂硅В鏋愯浆鍌ㄣ€?
 * 璁剧疆 registryAccess 骞惰皟鐢?DebugOutputWriter 杈撳嚭璋冭瘯鏁版嵁銆?
 *
 *
 * 鏈嶅姟鍣ㄥ惎鍔ㄤ簨浠跺洖璋冿紝瑙﹀彂瀹屾暣鐨勯厤鏂硅В鏋愯浆鍌ㄣ€?
 * 璁剧疆 registryAccess 骞惰皟鐢?DebugOutputWriter 杈撳嚭璋冭瘯鏁版嵁銆?
 *
 * @param event the server startup event
 * @param event 鏈嶅姟鍣ㄥ惎鍔ㄤ簨浠?
 */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.registryAccess = event.getServer().registryAccess();
        if (!enabled) return;
        LOGGER.info("{} Server starting - triggering full recipe parse dump (async)...", LOG_PREFIX);
        RecipeManager recipeManager = event.getServer().getRecipeManager();
        DebugOutputWriter.writeDebugOutputAsync(recipeManager, templateIndex, event.getServer());
    }

    /**
 * Server shutdown event callback, gracefully shuts down the async thread pool.
 *
 * 服务器关闭事件回调，优雅关闭异步线程池。
 *
 * @param event the server stopping event
 * @param event 服务器停止事件
 */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("{} Server stopping - shutting down async thread pool...", LOG_PREFIX);
        AsyncRecipeParser.shutdown();
    }

    /**
 * Gets the template index (for external use, e.g., debug output).
 *
 * 鑾峰彇妯℃澘绱㈠紩锛堢敤浜庡閮ㄤ娇鐢紝濡傝皟璇曡緭鍑猴級銆?
 *
 * @return an unmodifiable template index mapping
 * @return 涓嶅彲淇敼鐨勬ā鏉跨储寮曟槧灏?
 */
    Map<String, Template> getTemplateIndex() {
        return Collections.unmodifiableMap(templateIndex);
    }

    /**
 * Gets the recipe config for the specified recipe ID and template key.
 *
 * 鑾峰彇鎸囧畾閰嶆柟ID鍜屾ā鏉块敭鐨勯厤鏂归厤缃€?
 *
 * @param templateKey the template key (format: modid_recipetype)
 * @param recipeId the recipe ID
 * @param templateKey 妯℃澘閿紙鏍煎紡锛歮odid_recipetype锛?
 * @param recipeId    閰嶆柟ID
 * @return the recipe config, or null if not found
 * @return 閰嶆柟閰嶇疆锛屽鏋滀笉瀛樺湪鍒欒繑鍥?null
 */
    public TemplateConfig getRecipeConfig(String templateKey, String recipeId) {
        Map<String, TemplateConfig> configs = templateConfigs.get(templateKey);
        if (configs == null) return null;
        return configs.get(recipeId);
    }

    /**
 * Gets all recipe configs for the specified template key.
 *
 * 鑾峰彇鎸囧畾妯℃澘閿殑鎵€鏈夐厤鏂归厤缃€?
 *
 * @param templateKey the template key (format: modid_recipetype)
 * @param templateKey 妯℃澘閿紙鏍煎紡锛歮odid_recipetype锛?
 * @return a map of recipe IDs to configs
 * @return 閰嶆柟ID鍒伴厤缃殑鏄犲皠
 */
    public Map<String, TemplateConfig> getTemplateConfigs(String templateKey) {
        return templateConfigs.getOrDefault(templateKey, Collections.emptyMap());
    }

    /**
 * Gets the global settings for the specified template key.
 *
 * 鑾峰彇鎸囧畾妯℃澘閿殑鍏ㄥ眬璁剧疆銆?
 *
 * @param templateKey the template key (format: modid_recipetype)
 * @param templateKey 妯℃澘閿紙鏍煎紡锛歮odid_recipetype锛?
 * @return the global settings, or null if the template does not exist
 * @return 鍏ㄥ眬璁剧疆锛屽鏋滄ā鏉夸笉瀛樺湪鍒欒繑鍥?null
 */
    public GlobalSettings getGlobalSettings(String templateKey) {
        Template template = templateIndex.get(templateKey);
        return template != null ? template.getGlobalSettings() : null;
    }

    /**
 * Gets the set of all template keys (for fluid transfer queries).
 *
 * 鑾峰彇鎵€鏈夋ā鏉块敭鐨勯泦鍚堬紙鐢ㄤ簬娴佷綋杞崲鏌ヨ锛夈€?
 *
 * @return an unmodifiable set of template keys
 * @return 涓嶅彲淇敼鐨勬ā鏉块敭闆嗗悎
 */
    public Set<String> getAllTemplateKeys() {
        return Collections.unmodifiableSet(templateIndex.keySet());
    }

    /**
 * Gets the template object for the specified key.
 *
 * 鑾峰彇鎸囧畾閿殑妯℃澘瀵硅薄銆?
 *
 * @param templateKey the template key (format: modid_recipetype)
 * @param templateKey 妯℃澘閿紙鏍煎紡锛歮odid_recipetype锛?
 * @return the template object, or null if not found
 * @return 妯℃澘瀵硅薄锛屽鏋滀笉瀛樺湪鍒欒繑鍥?null
 */
    public Template getTemplate(String templateKey) {
        return templateIndex.get(templateKey);
    }
}