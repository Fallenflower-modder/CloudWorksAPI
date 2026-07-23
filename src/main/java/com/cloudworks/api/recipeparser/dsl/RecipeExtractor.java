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

import com.cloudworks.api.recipeparser.model.Ingredient;
import com.cloudworks.api.recipeparser.model.Product;
import com.cloudworks.api.recipeparser.model.RecipeData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Recipe data extractor.
 *
 * 閰嶆柟鏁版嵁鎻愬彇鍣ㄣ€?
 * <p>
 * 鏍规嵁妯℃澘璇硶鏍戯紙AST锛夊拰閰嶆柟 JSON 鏁版嵁锛屾彁鍙栭厤鏂圭殑杈撳叆鏉愭枡鍜屼骇鍑虹墿銆?
 * 閬嶅巻妯℃澘璇硶鏍戯紝灏嗘ā鏉夸腑鐨勬爣璁帮紙marker锛変笌 JSON 涓殑瀹為檯瀛楁瀵瑰簲锛?
 * 鏀堕泦鎵€鏈?INPUT 鍜?OUTPUT 鏍囪瀵瑰簲鐨勬暟鎹紝鏈€缁堢敓鎴?RecipeData 瀹炰緥銆?
 * </p>
 * <p>
 * 鏀寔鐨勫姛鑳藉寘鎷細
 * <ul>
 *   <li>瀵硅薄/鏁扮粍/閿€煎/瀛楅潰閲忕殑閫掑綊閬嶅巻</li>
 *   <li>鍔ㄦ€侀敭鍚嶏紙OBJECT 鏍囪锛?/li>
 *   <li>鏁扮粍鍏冪礌涓庣粨鏋勬ā鏉跨殑鍖归厤锛圖UPLICATE 鏍囪锛?/li>
 *   <li>鍙€夊瓧娈靛鐞嗭紙OPTIONAL 鏍囪锛?/li>
 *   <li>鐭╅樀鍜岀煩闃佃澶勭悊锛圡ATRIX/MATRIXLINE 鏍囪锛?/li>
 *   <li>绗﹀彿鏄犲皠涓庡伐鑹哄浘妗堣В鏋愶紙crafting-shaped 閰嶆柟锛?/li>
 *   <li>鑴氭湰鍙傛暟鎵ц锛圫CRIPT 鏍囪锛?/li>
 *   <li>IO 灞炴€у叧鑱旓紙IO_ATTRIBUTE 鏍囪鐨?input_id/output_id 寮曠敤锛?/li>
 * </ul>
 * </p>
 */
public class RecipeExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger("CloudWorks-RecipeExtractor");

    /**
     * Current template in use
     */
    /** 褰撳墠浣跨敤鐨勬ā鏉?*/
    private final Template template;
    /**
     * Recipe JSON data
     */
    /** 閰嶆柟 JSON 鏁版嵁 */
    private final JsonElement recipeJson;
    /**
     * Recipe ID
     */
    /** 閰嶆柟ID */
    private final String recipeId;

    /**
     * Collected input material list
     */
    /** 鏀堕泦鍒扮殑杈撳叆鏉愭枡鍒楄〃 */
    private final List<Ingredient> ingredients = new ArrayList<>();
    /**
     * Collected product list
     */
    /** 鏀堕泦鍒扮殑浜у嚭鐗╁垪琛?*/
    private final List<Product> products = new ArrayList<>();
    /**
     * Marker data storage: key is "markerId_index", value is data map
     */
    /** 鏍囪鏁版嵁瀛樺偍锛歬ey 涓?"markerId_index"锛寁alue 涓烘暟鎹槧灏?*/
    private final Map<String, Map<String, Object>> markerData = new HashMap<>();

    /**
     * Symbol mapping: single-character key -> item ID (for crafting-shaped recipes)
     */
    /** 绗﹀彿鏄犲皠锛氬崟瀛楃閿?-> 鐗╁搧ID锛堢敤浜?crafting-shaped 閰嶆柟锛?*/
    private final Map<String, String> symbolItemMap = new LinkedHashMap<>();
    /**
     * Pattern count: item ID -> accumulated count
     */
    /** 鍥炬璁℃暟锛氱墿鍝両D -> 绱鏁伴噺 */
    private final Map<String, Integer> patternCounts = new LinkedHashMap<>();
    /**
     * Whether the pattern has been processed
     */
    /** 鍥炬鏄惁宸插鐞?*/
    private boolean patternProcessed = false;

    /**
 * Constructs a recipe extractor.
 *
 * 鏋勯€犻厤鏂规彁鍙栧櫒銆?
 *
 * @param template the template object
 * @param recipeJson the recipe JSON data
 * @param recipeId the recipe ID
 * @param template   妯℃澘瀵硅薄
 * @param recipeJson 閰嶆柟 JSON 鏁版嵁
 * @param recipeId   閰嶆柟ID
 */
    public RecipeExtractor(Template template, JsonElement recipeJson, String recipeId) {
        this.template = template;
        this.recipeJson = recipeJson;
        this.recipeId = recipeId;
    }

    /**
 * Executes extraction, returns RecipeData.
 *
 * 鎵ц鎻愬彇锛岃繑鍥?RecipeData銆?
 *
 * @return the extracted recipe data
 * @return 鎻愬彇鍚庣殑閰嶆柟鏁版嵁
 */
    public RecipeData extract() {
        extractNode(template.getRoot(), recipeJson, new HashMap<>());
        finalizePatternInputs();
        return new RecipeData(ingredients, products);
    }

    /**
 * Dispatches to the corresponding extraction method based on template node type.
 *
 * 鏍规嵁妯℃澘鑺傜偣绫诲瀷鍒嗗彂鍒板搴旂殑鎻愬彇鏂规硶銆?
 *
 * @param templateNode the template node
 * @param jsonElement the corresponding JSON data
 * @param context the context (containing _index, _key, etc.)
 * @param templateNode 妯℃澘鑺傜偣
 * @param jsonElement  瀵瑰簲鐨?JSON 鏁版嵁
 * @param context      涓婁笅鏂囷紙鍖呭惈 _index銆乢key 绛変俊鎭級
 */
    private void extractNode(TemplateNode templateNode, JsonElement jsonElement, Map<String, Object> context) {
        switch (templateNode.getType()) {
            case OBJECT:
                if (jsonElement.isJsonObject()) {
                    extractObject(templateNode, jsonElement.getAsJsonObject(), context);
                }
                break;
            case ARRAY:
                if (jsonElement.isJsonArray()) {
                    extractArray(templateNode, jsonElement.getAsJsonArray(), context);
                }
                break;
            case KEY_VALUE:
                extractKeyValue(templateNode, jsonElement, context);
                break;
            case MARKER:
                extractMarker(templateNode, jsonElement, context);
                break;
            case JSON_LITERAL:
                // Literal values are fixed constants - just verify they match
                break;
        }
    }

    /**
 * Extracts JSON objects, matching JSON keys by template node children.
 *
 * 鎻愬彇 JSON 瀵硅薄锛屾寜妯℃澘鑺傜偣鐨勫瓙鑺傜偣鍖归厤 JSON 閿€?
 *
 * @param templateNode the template node
 * @param jsonObject the JSON object
 * @param context the context
 * @param templateNode 妯℃澘鑺傜偣
 * @param jsonObject   JSON 瀵硅薄
 * @param context      涓婁笅鏂?
 */
    private void extractObject(TemplateNode templateNode, JsonObject jsonObject, Map<String, Object> context) {
        // Check if this object has an OBJECT marker child for dynamic-key handling
        TemplateNode structureNode = null;
        for (TemplateNode child : templateNode.getChildren()) {
            if (child.getMarker() != null
                && child.getMarker().getMarkerType() == MarkerDef.MarkerType.OBJECT) {
                structureNode = child;
                break;
            }
        }

        if (structureNode != null) {
            // Check if any KEY_VALUE child of the structure matches a JSON key
            boolean hasMatchingKey = false;
            for (TemplateNode child : structureNode.getChildren()) {
                if (child.getKey() != null && jsonObject.has(child.getKey())) {
                    hasMatchingKey = true;
                    break;
                }
            }
            if (!hasMatchingKey && !jsonObject.entrySet().isEmpty()) {
                // Dynamic keys: iterate through all JSON entries
                int index = 0;
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    Map<String, Object> keyContext = new HashMap<>(context);
                    keyContext.put("_key", entry.getKey());
                    keyContext.put("_index", index);
                    extractStructureDuplicated(structureNode, entry.getValue(), keyContext, index);
                    index++;
                }
                // Process remaining non-MARKER children (e.g., other KEY_VALUE siblings)
                for (TemplateNode child : templateNode.getChildren()) {
                    if (child == structureNode) continue;
                    if (child.getKey() != null) {
                        String key = child.getKey();
                        JsonElement value = jsonObject.get(key);
                        if (value != null) {
                            extractNode(child, value, context);
                        }
                    }
                }
                return;
            }
        }

        for (TemplateNode child : templateNode.getChildren()) {
            if (child.getKey() != null) {
                String key = child.getKey();
                JsonElement value = jsonObject.get(key);
                if (value != null) {
                    extractNode(child, value, context);
                }
            } else {
                extractNode(child, jsonObject, context);
            }
        }
    }

    /**
 * Extracts JSON arrays, handling array structure templates and pattern lines.
 *
 * 鎻愬彇 JSON 鏁扮粍锛屽鐞嗘暟缁勭粨鏋勬ā鏉垮拰鍥炬琛屻€?
 *
 * @param templateNode the template node
 * @param jsonArray the JSON array
 * @param context the context
 * @param templateNode 妯℃澘鑺傜偣
 * @param jsonArray    JSON 鏁扮粍
 * @param context      涓婁笅鏂?
 */
    private void extractArray(TemplateNode templateNode, JsonArray jsonArray, Map<String, Object> context) {
        // Check for patternline marker: process each array element as a pattern string
        for (TemplateNode child : templateNode.getChildren()) {
            if (child.getMarker() != null) {
                MarkerDef marker = child.getMarker();
                if (marker.getMarkerType() == MarkerDef.MarkerType.IO_ATTRIBUTE
                    && "patternline".equals(marker.getTypeName())) {
                    for (int j = 0; j < jsonArray.size(); j++) {
                        JsonElement element = jsonArray.get(j);
                        if (element.isJsonPrimitive()) {
                            processPatternLine(element.getAsString());
                        }
                    }
                    return;
                }
            }
        }

        // Process array children: typically object markers followed by duplicate
        TemplateNode structureNode = null;
        TemplateNode duplicateNode = null;
        int structureIndex = -1;

        for (int i = 0; i < templateNode.getChildren().size(); i++) {
            TemplateNode child = templateNode.getChildren().get(i);
            if (child.getMarker() != null) {
                MarkerDef marker = child.getMarker();
                if (marker.getMarkerType() == MarkerDef.MarkerType.OBJECT) {
                    structureNode = child;
                    structureIndex = i;
                } else if (marker.getMarkerType() == MarkerDef.MarkerType.DUPLICATE) {
                    duplicateNode = child;
                }
            }
        }

        if (duplicateNode != null && structureNode != null) {
            // Apply structure template to each array element
            int actualIndex = 0;
            for (int i = structureIndex; i < jsonArray.size(); i++) {
                JsonElement element = jsonArray.get(i);
                Map<String, Object> dupContext = new HashMap<>(context);
                dupContext.put("_index", actualIndex);
                extractStructureDuplicated(structureNode, element, dupContext, actualIndex);
                actualIndex++;
            }
        } else if (structureNode != null) {
            // Match structure template against array elements one by one
            for (int i = 0; i < jsonArray.size(); i++) {
                Map<String, Object> childContext = new HashMap<>(context);
                extractStructureDuplicated(structureNode, jsonArray.get(i), childContext, i);
            }
        }
    }

    /**
 * Applies a structure template to a single JSON element, recursively extracting all child markers.
 *
 * 灏嗙粨鏋勬ā鏉垮簲鐢ㄤ簬鍗曚釜 JSON 鍏冪礌锛岄€掑綊鎻愬彇鎵€鏈夊瓙鏍囪銆?
 *
 * @param structureNode the structure template node
 * @param jsonElement the JSON element
 * @param context the context
 * @param index the current element index
 * @param structureNode 缁撴瀯妯℃澘鑺傜偣
 * @param jsonElement   JSON 鍏冪礌
 * @param context       涓婁笅鏂?
 * @param index         褰撳墠鍏冪礌绱㈠紩
 */
    private void extractStructureDuplicated(TemplateNode structureNode, JsonElement jsonElement,
                                             Map<String, Object> context, int index) {
        if (structureNode.getMarker() != null) {
            // Set index on the structure marker
            Map<String, Object> structData = getOrCreateMarkerData(structureNode.getMarker().getId(), index);
            structData.put("index", index);

            // Process children with proper JSON key resolution
            for (TemplateNode child : structureNode.getChildren()) {
                if (child.getKey() != null && jsonElement.isJsonObject()) {
                    String key = child.getKey();
                    JsonElement value = jsonElement.getAsJsonObject().get(key);
                    if (value != null) {
                        extractNode(child, value, context);
                    }
                } else {
                    extractNode(child, jsonElement, context);
                }
            }
        }
    }

    /**
 * Extracts key-value pair nodes. If wrapping a marker, extracts the marker directly.
 *
 * 鎻愬彇閿€煎鑺傜偣锛屽鏋滃寘瑁呬簡鏍囪鍒欑洿鎺ユ彁鍙栨爣璁般€?
 *
 * @param templateNode the template node
 * @param jsonElement the JSON data
 * @param context the context
 * @param templateNode 妯℃澘鑺傜偣
 * @param jsonElement  JSON 鏁版嵁
 * @param context      涓婁笅鏂?
 */
    private void extractKeyValue(TemplateNode templateNode, JsonElement jsonElement, Map<String, Object> context) {
        // If this KEY_VALUE node wraps a marker (e.g., "item": <input>), extract it directly
        if (templateNode.getMarker() != null) {
            extractMarker(templateNode, jsonElement, context);
            return;
        }
        if (templateNode.getChildren().isEmpty()) return;
        TemplateNode child = templateNode.getChildren().get(0);
        extractNode(child, jsonElement, context);
    }

    /**
 * Dispatches to the corresponding marker handling method based on marker type.
 *
 * 鏍规嵁鏍囪绫诲瀷鍒嗗彂鍒板搴旂殑鏍囪澶勭悊鏂规硶銆?
 *
 * @param templateNode the template node
 * @param jsonElement the JSON data
 * @param context the context
 * @param templateNode 妯℃澘鑺傜偣
 * @param jsonElement  JSON 鏁版嵁
 * @param context      涓婁笅鏂?
 */
    private void extractMarker(TemplateNode templateNode, JsonElement jsonElement, Map<String, Object> context) {
        MarkerDef marker = templateNode.getMarker();
        int index = context.containsKey("_index") ? (int) context.get("_index") : 0;

        switch (marker.getMarkerType()) {
            case INPUT:
                extractInputMarker(marker, jsonElement, index);
                break;
            case OUTPUT:
                extractOutputMarker(marker, jsonElement, index);
                break;
            case IO_ATTRIBUTE:
                extractIOAttribute(marker, jsonElement, context, index);
                break;
            case VARIABLE:
                extractVariable(marker, jsonElement, context, index);
                break;
            case OPTIONAL:
                extractOptional(templateNode, jsonElement, context, index);
                break;
            case OBJECT:
                // Object as structure - handled by extractStructureDuplicated
                for (TemplateNode child : templateNode.getChildren()) {
                    extractNode(child, jsonElement, context);
                }
                break;
            case MATRIX:
                extractMatrix(templateNode, jsonElement, context, index);
                break;
            case MATRIXLINE:
                extractMatrixLine(marker, jsonElement, index);
                break;
            case DUPLICATE:
                // Duplicate handled at array level
                break;
            case SCRIPT:
                executeScript(marker, context, index);
                break;
        }
    }

    /**
 * 鎻愬彇鍙€夋爣璁帮紝妫€鏌?JSON 涓槸鍚﹀瓨鍦ㄥ尮閰嶇殑閿€?
 * 濡傛灉瀛樺湪鍒欐甯告彁鍙栧瓙鑺傜偣锛屽惁鍒欐爣璁?existance 涓?false銆?
 *
 *
 * 鎻愬彇鍙€夋爣璁帮紝妫€鏌?JSON 涓槸鍚﹀瓨鍦ㄥ尮閰嶇殑閿€?
 * 濡傛灉瀛樺湪鍒欐甯告彁鍙栧瓙鑺傜偣锛屽惁鍒欐爣璁?existance 涓?false銆?
 *
 * @param templateNode the template node
 * @param jsonElement the JSON data
 * @param context the context
 * @param index the current index
 * @param templateNode 妯℃澘鑺傜偣
 * @param jsonElement  JSON 鏁版嵁
 * @param context      涓婁笅鏂?
 * @param index        褰撳墠绱㈠紩
 */
    private void extractOptional(TemplateNode templateNode, JsonElement jsonElement, Map<String, Object> context, int index) {
        MarkerDef marker = templateNode.getMarker();
        Map<String, Object> optData = getOrCreateMarkerData(marker.getId(), index);

        if (!jsonElement.isJsonObject()) {
            optData.put("existance", false);
            return;
        }

        JsonObject jsonObj = jsonElement.getAsJsonObject();

        // Check existence: any key from the optional's children matches
        boolean exists = false;
        for (TemplateNode child : templateNode.getChildren()) {
            if (child.getKey() != null) {
                if (jsonObj.has(child.getKey())) {
                    exists = true;
                    break;
                }
            }
        }

        optData.put("existance", exists);

        if (exists) {
            for (TemplateNode child : templateNode.getChildren()) {
                // Resolve the key from JSON before passing to child
                if (child.getKey() != null) {
                    JsonElement resolved = jsonObj.get(child.getKey());
                    if (resolved != null) {
                        extractNode(child, resolved, context);
                    }
                } else {
                    extractNode(child, jsonElement, context);
                }
            }
            // Execute optional's parameters
            executeParameters(marker, context, index);
        }
    }

    /**
 * Extracts input markers, creates Ingredient objects.
 *
 * 鎻愬彇杈撳叆鏍囪锛屽垱寤?Ingredient 瀵硅薄銆?
 *
 * @param marker the marker definition
 * @param jsonElement the JSON data
 * @param index the current index
 * @param marker      鏍囪瀹氫箟
 * @param jsonElement JSON 鏁版嵁
 * @param index       褰撳墠绱㈠紩
 */
    private void extractInputMarker(MarkerDef marker, JsonElement jsonElement, int index) {
        Map<String, Object> data = getOrCreateMarkerData(marker.getId(), index);
        data.put("content", jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : jsonElement.toString());

        String id = jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : "";
        double count = getDoubleAttr(marker, "count", 1.0);
        String unit = getStringAttr(marker, "unit", "item");
        String type = getStringAttr(marker, "type", "item");
        double rate = getDoubleAttr(marker, "rate", 1.0);
        boolean nondamageable = getBooleanAttr(marker, "nondamageable", false);
        String shortcut = getStringAttr(marker, "shortcut", null);
        boolean counton = getBooleanAttr(marker, "counton", false);

        // Apply default parameters
        for (ParameterOp param : marker.getParameters()) {
            applyDefaultParam(param, data);
        }

        // Override from markerData (populated by IO_ATTRIBUTE markers like <count>, <amount>, <chance>)
        if (data.containsKey("count")) {
            Object c = data.get("count");
            if (c instanceof Number) count = ((Number) c).doubleValue();
        }
        if (data.containsKey("amount")) {
            Object c = data.get("amount");
            if (c instanceof Number) count = ((Number) c).doubleValue();
        }
        if (data.containsKey("chance")) {
            Object c = data.get("chance");
            if (c instanceof Number) rate = ((Number) c).doubleValue();
        }
        if (data.containsKey("rate")) {
            Object c = data.get("rate");
            if (c instanceof Number) rate = ((Number) c).doubleValue();
        }
        if (data.containsKey("type")) {
            type = data.get("type").toString();
        }
        if (data.containsKey("unit")) {
            unit = data.get("unit").toString();
        }

        // Check if this input is part of a symbol mapping (crafting key)
        if (data.containsKey("symbol") && !id.isEmpty()) {
            String symbol = (String) data.get("symbol");
            symbolItemMap.put(symbol, id);
            return; // Don't add to ingredients; will be resolved from pattern
        }

        ingredients.add(new Ingredient(id, count, unit, type, rate, nondamageable, shortcut, counton));
    }

    /**
 * Extracts output markers, creates Product objects.
 *
 * 鎻愬彇杈撳嚭鏍囪锛屽垱寤?Product 瀵硅薄銆?
 *
 * @param marker the marker definition
 * @param jsonElement the JSON data
 * @param index the current index
 * @param marker      鏍囪瀹氫箟
 * @param jsonElement JSON 鏁版嵁
 * @param index       褰撳墠绱㈠紩
 */
    private void extractOutputMarker(MarkerDef marker, JsonElement jsonElement, int index) {
        Map<String, Object> data = getOrCreateMarkerData(marker.getId(), index);
        data.put("content", jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : jsonElement.toString());

        String id = jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : "";
        double count = getDoubleAttr(marker, "count", 1.0);
        String unit = getStringAttr(marker, "unit", "item");
        String type = getStringAttr(marker, "type", "item");
        double rate = getDoubleAttr(marker, "rate", 1.0);
        boolean autotransfer = getBooleanAttr(marker, "autotransfer", false);
        double transferrate = getDoubleAttr(marker, "transferrate", 0.0);

        for (ParameterOp param : marker.getParameters()) {
            applyDefaultParam(param, data);
        }

        // Override from markerData (populated by IO_ATTRIBUTE markers like <count>, <amount>, <chance>)
        if (data.containsKey("count")) {
            Object c = data.get("count");
            if (c instanceof Number) count = ((Number) c).doubleValue();
        }
        if (data.containsKey("amount")) {
            Object c = data.get("amount");
            if (c instanceof Number) count = ((Number) c).doubleValue();
        }
        if (data.containsKey("chance")) {
            Object c = data.get("chance");
            if (c instanceof Number) rate = ((Number) c).doubleValue();
        }
        if (data.containsKey("rate")) {
            Object c = data.get("rate");
            if (c instanceof Number) rate = ((Number) c).doubleValue();
        }
        if (data.containsKey("type")) {
            type = data.get("type").toString();
        }
        if (data.containsKey("unit")) {
            unit = data.get("unit").toString();
        }

        products.add(new Product(id, count, unit, type, rate, autotransfer, transferrate));
    }

    /**
 * 鎻愬彇 IO 灞炴€ф爣璁帮紝灏嗗睘鎬у€煎啓鍏ョ洰鏍囨爣璁扮殑鏁版嵁涓€?
 * 閫氳繃 input_id 鎴?output_id 灞炴€у叧鑱旂洰鏍囨爣璁般€?
 *
 *
 * 鎻愬彇 IO 灞炴€ф爣璁帮紝灏嗗睘鎬у€煎啓鍏ョ洰鏍囨爣璁扮殑鏁版嵁涓€?
 * 閫氳繃 input_id 鎴?output_id 灞炴€у叧鑱旂洰鏍囨爣璁般€?
 *
 * @param marker the marker definition
 * @param jsonElement the JSON data
 * @param context the context
 * @param index the current index
 * @param marker      鏍囪瀹氫箟
 * @param jsonElement JSON 鏁版嵁
 * @param context     涓婁笅鏂?
 * @param index       褰撳墠绱㈠紩
 */
    private void extractIOAttribute(MarkerDef marker, JsonElement jsonElement, Map<String, Object> context, int index) {
        Map<String, Object> data = getOrCreateMarkerData(marker.getId(), index);
        String content = jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : jsonElement.toString();
        data.put("content", content);

        // Handle symbol marker: capture the JSON key name (single-char crafting key)
        if ("symbol".equals(marker.getTypeName())) {
            String keyName = (String) context.get("_key");
            String inputId = marker.getAttribute("input_id");
            if (keyName != null && inputId != null) {
                Map<String, Object> targetData = getOrCreateMarkerData(inputId, index);
                targetData.put("symbol", keyName);
            }
            return;
        }

        String inputId = marker.getAttribute("input_id");
        String outputId = marker.getAttribute("output_id");
        double multiplier = getDoubleAttr(marker, "multiplier", 1.0);

        String targetId = inputId != null ? inputId : outputId;
        if (targetId != null) {
            Map<String, Object> targetData = getOrCreateMarkerData(targetId, index);
            // Use the original marker type name (e.g., "count", "type", "unit", "rate")
            String attrName = marker.getTypeName();
            if (content.matches("-?\\d+(\\.\\d+)?")) {
                double val = Double.parseDouble(content) * multiplier;
                targetData.put(attrName, val);
            } else {
                targetData.put(attrName, content);
            }
        }
    }

    /**
 * Extracts variable markers, stores their content and executes parameters.
 *
 * 鎻愬彇鍙橀噺鏍囪锛屽瓨鍌ㄥ叾鍐呭骞舵墽琛屽弬鏁般€?
 *
 * @param marker the marker definition
 * @param jsonElement the JSON data
 * @param context the context
 * @param index the current index
 * @param marker      鏍囪瀹氫箟
 * @param jsonElement JSON 鏁版嵁
 * @param context     涓婁笅鏂?
 * @param index       褰撳墠绱㈠紩
 */
    private void extractVariable(MarkerDef marker, JsonElement jsonElement, Map<String, Object> context, int index) {
        Map<String, Object> data = getOrCreateMarkerData(marker.getId(), index);
        String content = jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : jsonElement.toString();
        data.put("content", content);

        executeParameters(marker, context, index);
    }

    /**
 * Extracts matrix markers, recording the matrix's row and column counts.
 *
 * 鎻愬彇鐭╅樀鏍囪锛岃褰曠煩闃电殑琛屾暟鍜屽垪鏁般€?
 *
 * @param templateNode the template node
 * @param jsonElement the JSON data
 * @param context the context
 * @param index the current index
 * @param templateNode 妯℃澘鑺傜偣
 * @param jsonElement  JSON 鏁版嵁
 * @param context      涓婁笅鏂?
 * @param index        褰撳墠绱㈠紩
 */
    private void extractMatrix(TemplateNode templateNode, JsonElement jsonElement, Map<String, Object> context, int index) {
        MarkerDef marker = templateNode.getMarker();
        Map<String, Object> data = getOrCreateMarkerData(marker.getId(), index);

        if (jsonElement.isJsonArray()) {
            JsonArray arr = jsonElement.getAsJsonArray();
            data.put("line", arr.size());
            if (!arr.isEmpty() && arr.get(0).isJsonArray()) {
                data.put("column", arr.get(0).getAsJsonArray().size());
            } else {
                data.put("column", arr.size());
            }
        }

        for (TemplateNode child : templateNode.getChildren()) {
            extractNode(child, jsonElement, context);
        }
    }

    /**
 * Extracts matrix line markers, stores their content.
 *
 * 鎻愬彇鐭╅樀琛屾爣璁帮紝瀛樺偍鍏跺唴瀹广€?
 *
 * @param marker the marker definition
 * @param jsonElement the JSON data
 * @param index the current index
 * @param marker      鏍囪瀹氫箟
 * @param jsonElement JSON 鏁版嵁
 * @param index       褰撳墠绱㈠紩
 */
    private void extractMatrixLine(MarkerDef marker, JsonElement jsonElement, int index) {
        Map<String, Object> data = getOrCreateMarkerData(marker.getId(), index);
        data.put("content", jsonElement.toString());
    }

    /**
 * Executes script marker parameters.
 *
 * 鎵ц鑴氭湰鏍囪鐨勫弬鏁般€?
 *
 * @param marker the marker definition
 * @param context the context
 * @param index the current index
 * @param marker  鏍囪瀹氫箟
 * @param context 涓婁笅鏂?
 * @param index   褰撳墠绱㈠紩
 */
    private void executeScript(MarkerDef marker, Map<String, Object> context, int index) {
        executeParameters(marker, context, index);
    }

    /**
 * Executes all parameter operations of the marker (set_ and override_ prefixes).
 *
 * 鎵ц鏍囪鐨勬墍鏈夊弬鏁版搷浣滐紙set_ 鍜?override_ 鍓嶇紑锛夈€?
 *
 * @param marker the marker definition
 * @param context the context
 * @param index the current index
 * @param marker  鏍囪瀹氫箟
 * @param context 涓婁笅鏂?
 * @param index   褰撳墠绱㈠紩
 */
    private void executeParameters(MarkerDef marker, Map<String, Object> context, int index) {
        for (ParameterOp param : marker.getParameters()) {
            String key = param.getKey();
            if (key.startsWith("set_")) {
                // set_ID_attributeName = value
                String target = key.substring(4);
                int underscoreIdx = target.indexOf('_');
                if (underscoreIdx > 0) {
                    String targetId = target.substring(0, underscoreIdx);
                    String targetAttr = target.substring(underscoreIdx + 1);
                    Map<String, Object> targetData = getOrCreateMarkerData(targetId, index);
                    targetData.put(targetAttr, resolveValue(param.getValue(), context, index));
                }
            } else if (key.startsWith("override_")) {
                // override_ID_readonlyParam = value
                String target = key.substring(9);
                int underscoreIdx = target.indexOf('_');
                if (underscoreIdx > 0) {
                    String targetId = target.substring(0, underscoreIdx);
                    String targetParam = target.substring(underscoreIdx + 1);
                    Map<String, Object> targetData = getOrCreateMarkerData(targetId, index);
                    targetData.put(targetParam + "_overridden", resolveValue(param.getValue(), context, index));
                }
            }
        }
    }

    /**
 * Applies default parameters (default_ prefix), sets if the target data does not contain the attribute.
 *
 * 搴旂敤榛樿鍙傛暟锛坉efault_ 鍓嶇紑锛夛紝濡傛灉鐩爣鏁版嵁涓笉鍖呭惈璇ュ睘鎬у垯璁剧疆銆?
 *
 * @param param the parameter operation
 * @param data the target data
 * @param param 鍙傛暟鎿嶄綔
 * @param data  鐩爣鏁版嵁
 */
    private void applyDefaultParam(ParameterOp param, Map<String, Object> data) {
        String key = param.getKey();
        if (key.startsWith("default_")) {
            String attrName = key.substring(8);
            if (!data.containsKey(attrName)) {
                data.put(attrName, resolveSimpleValue(param.getValue()));
            }
        }
    }

    /**
 * Resolves parameter values (tries to parse as number, returns string on failure).
 *
 * 瑙ｆ瀽鍙傛暟鍊硷紙灏濊瘯瑙ｆ瀽涓烘暟瀛楋紝澶辫触鍒欒繑鍥炲瓧绗︿覆锛夈€?
 *
 * @param value the parameter value
 * @param context the context
 * @param index the current index
 * @param value   鍙傛暟鍊?
 * @param context 涓婁笅鏂?
 * @param index   褰撳墠绱㈠紩
 * @return the resolved value
 * @return 瑙ｆ瀽鍚庣殑鍊?
 */
    private Object resolveValue(String value, Map<String, Object> context, int index) {
        if (value == null) return null;
        // Try to resolve as number
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Not a number, return as string
        }
        return value;
    }

    /**
 * Resolves simple values (tries to parse as number, then boolean, otherwise returns string).
 *
 * 瑙ｆ瀽绠€鍗曞€硷紙灏濊瘯瑙ｆ瀽涓烘暟瀛楋紝澶辫触鍒欏皾璇曡В鏋愪负甯冨皵锛屽惁鍒欒繑鍥炲瓧绗︿覆锛夈€?
 *
 * @param value the parameter value
 * @param value 鍙傛暟鍊?
 * @return the resolved value
 * @return 瑙ｆ瀽鍚庣殑鍊?
 */
    private Object resolveSimpleValue(String value) {
        if (value == null) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if ("true".equals(value)) return true;
            if ("false".equals(value)) return false;
        }
        return value;
    }

    /**
 * Gets or creates the data store for the specified marker ID and index.
 *
 * 鑾峰彇鎴栧垱寤烘寚瀹氭爣璁癐D鍜岀储寮曠殑鏁版嵁瀛樺偍銆?
 *
 * @param id the marker ID
 * @param index the index
 * @param id    鏍囪ID
 * @param index 绱㈠紩
 * @return the data map
 * @return 鏁版嵁鏄犲皠
 */
    private Map<String, Object> getOrCreateMarkerData(String id, int index) {
        String key = id + "_" + index;
        return markerData.computeIfAbsent(key, k -> new HashMap<>());
    }

    /**
 * Gets the double attribute value of the marker.
 *
 * 鑾峰彇鏍囪鐨?double 绫诲瀷灞炴€у€笺€?
 *
 * @param marker the marker definition
 * @param attr the attribute name
 * @param defaultValue the default value
 * @param marker       鏍囪瀹氫箟
 * @param attr         灞炴€у悕
 * @param defaultValue 榛樿鍊?
 * @return the attribute value, or the default on parse failure
 * @return 灞炴€у€硷紝瑙ｆ瀽澶辫触鏃惰繑鍥為粯璁ゅ€?
 */
    private double getDoubleAttr(MarkerDef marker, String attr, double defaultValue) {
        String val = marker.getAttribute(attr);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
 * Gets the string attribute value of the marker.
 *
 * 鑾峰彇鏍囪鐨勫瓧绗︿覆灞炴€у€笺€?
 *
 * @param marker the marker definition
 * @param attr the attribute name
 * @param defaultValue the default value
 * @param marker       鏍囪瀹氫箟
 * @param attr         灞炴€у悕
 * @param defaultValue 榛樿鍊?
 * @return the attribute value, or the default if not present
 * @return 灞炴€у€硷紝涓嶅瓨鍦ㄦ椂杩斿洖榛樿鍊?
 */
    private String getStringAttr(MarkerDef marker, String attr, String defaultValue) {
        return marker.getAttribute(attr) != null ? marker.getAttribute(attr) : defaultValue;
    }

    /**
 * Gets the boolean attribute value of the marker.
 *
 * 鑾峰彇鏍囪鐨勫竷灏斿睘鎬у€笺€?
 *
 * @param marker the marker definition
 * @param attr the attribute name
 * @param defaultValue the default value
 * @param marker       鏍囪瀹氫箟
 * @param attr         灞炴€у悕
 * @param defaultValue 榛樿鍊?
 * @return the attribute value, or the default if not present
 * @return 灞炴€у€硷紝涓嶅瓨鍦ㄦ椂杩斿洖榛樿鍊?
 */
    private boolean getBooleanAttr(MarkerDef marker, String attr, boolean defaultValue) {
        String val = marker.getAttribute(attr);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val);
    }

    /**
 * 澶勭悊鍗曡宸ヨ壓鍥炬锛坈rafting matrix锛夈€?
 * 灏嗘瘡涓瓧绗﹀湪绗﹀彿鏄犲皠琛ㄤ腑鏌ユ壘瀵瑰簲鐨勭墿鍝両D锛屽苟绱姞璁℃暟銆?
 *
 *
 * 澶勭悊鍗曡宸ヨ壓鍥炬锛坈rafting matrix锛夈€?
 * 灏嗘瘡涓瓧绗﹀湪绗﹀彿鏄犲皠琛ㄤ腑鏌ユ壘瀵瑰簲鐨勭墿鍝両D锛屽苟绱姞璁℃暟銆?
 *
 * @param line the pattern line string
 * @param line 鍥炬琛屽瓧绗︿覆
 */
    private void processPatternLine(String line) {
        for (int i = 0; i < line.length(); i++) {
            String ch = String.valueOf(line.charAt(i));
            String itemId = symbolItemMap.get(ch);
            if (itemId != null) {
                patternCounts.merge(itemId, 1, Integer::sum);
            }
        }
    }

    /**
     * After all extraction is complete, converts accumulated pattern counts into Ingredient objects.
     */
    /** 鍦ㄦ墍鏈夋彁鍙栧畬鎴愬悗锛屽皢绱Н鐨勫浘妗堣鏁拌浆鎹负 Ingredient 瀵硅薄銆?*/
    private void finalizePatternInputs() {
        if (patternProcessed || patternCounts.isEmpty()) return;
        patternProcessed = true;
        for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
            String id = entry.getKey();
            int count = entry.getValue();
            // Determine type from the ID format: tags start with #
            String type = id.startsWith("#") ? "tag" : "solid";
            String actualId = id.startsWith("#") ? id.substring(1) : id;
            ingredients.add(new Ingredient(actualId, count, "item", type, 1.0, false, null, false));
        }
    }
}