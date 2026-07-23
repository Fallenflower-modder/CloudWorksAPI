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

import java.util.*;

/**
 * RPML template marker definition.
 *
 * RPML 妯℃澘鏍囪瀹氫箟銆?
 * <p>
 * 琛ㄧず妯℃澘涓殑涓€涓爣璁帮紙marker锛夛紝鐢ㄤ簬鎻忚堪閰嶆柟涓煇涓瓧娈电殑璇箟瑙掕壊銆?
 * 鏍囪绫诲瀷鍖呮嫭 INPUT锛堣緭鍏ワ級銆丱UTPUT锛堣緭鍑猴級銆両O_ATTRIBUTE锛堝睘鎬э級銆?
 * VARIABLE锛堝彉閲忥級銆丱BJECT锛堝璞★級銆丱PTIONAL锛堝彲閫夛級銆丮ATRIX锛堢煩闃碉級銆?
 * MATRIXLINE锛堢煩闃佃锛夈€丏UPLICATE锛堝鍒剁粨鏋勶級鍜?SCRIPT锛堣剼鏈級銆?
 * </p>
 */
public class MarkerDef {
    /**
     * Marker type enum.
     */
    /** 鏍囪绫诲瀷鏋氫妇銆?*/
    public enum MarkerType {
        /**
     * Input marker, indicates a recipe input item
     */
    /** 杈撳叆鏍囪锛岃〃绀洪厤鏂硅緭鍏ョ墿鍝?*/
        INPUT,
        /**
     * Output marker, indicates a recipe output item
     */
    /** 杈撳嚭鏍囪锛岃〃绀洪厤鏂逛骇鍑虹墿鍝?*/
        OUTPUT,
        /**
     * IO attribute marker, used to modify input/output attributes (e.g., count, type, unit, rate, etc.)
     */
    /** IO 灞炴€ф爣璁帮紝鐢ㄤ簬淇グ杈撳叆/杈撳嚭鐨勫睘鎬э紙濡傛暟閲忋€佺被鍨嬨€佸崟浣嶃€佹鐜囩瓑锛?*/
        IO_ATTRIBUTE,
        /**
     * Variable marker, used to store intermediate values
     */
    /** 鍙橀噺鏍囪锛岀敤浜庡瓨鍌ㄤ腑闂村€?*/
        VARIABLE,
        /**
     * Object marker, defines a sub-structure
     */
    /** 瀵硅薄鏍囪锛屽畾涔変竴涓瓙缁撴瀯浣?*/
        OBJECT,
        /**
     * Optional marker, defines an optional structure
     */
    /** 鍙€夋爣璁帮紝瀹氫箟涓€涓彲閫夌殑缁撴瀯浣?*/
        OPTIONAL,
        /**
     * Matrix marker, defines a 2D matrix structure
     */
    /** 鐭╅樀鏍囪锛屽畾涔変竴涓簩缁寸煩闃电粨鏋?*/
        MATRIX,
        /**
     * Matrix line marker, defines a row in a matrix
     */
    /** 鐭╅樀琛屾爣璁帮紝瀹氫箟鐭╅樀涓殑涓€琛?*/
        MATRIXLINE,
        /**
     * Duplicate marker, references a defined marker structure for repetition
     */
    /** 澶嶅埗缁撴瀯鏍囪锛屽紩鐢ㄥ凡瀹氫箟鐨勬爣璁扮粨鏋勮繘琛岄噸澶?*/
        DUPLICATE,
        /**
     * Script marker, used to execute global settings scripts
     */
    /** 鑴氭湰鏍囪锛岀敤浜庢墽琛屽叏灞€璁剧疆鑴氭湰 */
        SCRIPT
    }

    /**
     * Unique identifier of the marker
     */
    /** 鏍囪鐨勫敮涓€鏍囪瘑绗?*/
    private final String id;
    /**
     * Marker type
     */
    /** 鏍囪绫诲瀷 */
    private final MarkerType markerType;
    /**
     * Original marker type name (e.g., "count", "type", "unit", "rate")
     */
    /** 鍘熷鏍囪绫诲瀷鍚嶇О锛堝 "count"銆?type"銆?unit"銆?rate"锛?*/
    private final String typeName;
    /**
     * Marker attribute key-value pairs
     */
    /** 鏍囪鐨勫睘鎬ч敭鍊煎 */
    private final Map<String, String> attributes;
    /**
     * Marker parameter operation list
     */
    /** 鏍囪鐨勫弬鏁版搷浣滃垪琛?*/
    private final List<ParameterOp> parameters;

    /**
 * Constructs a marker definition instance.
 *
 * 鏋勯€犱竴涓爣璁板畾涔夊疄渚嬨€?
 *
 * @param id the marker unique identifier
 * @param markerType the marker type
 * @param typeName the original marker type name
 * @param attributes the attribute key-value pairs
 * @param parameters the parameter operation list
 * @param id         鏍囪鍞竴鏍囪瘑绗?
 * @param markerType 鏍囪绫诲瀷
 * @param typeName   鍘熷鏍囪绫诲瀷鍚嶇О
 * @param attributes 灞炴€ч敭鍊煎
 * @param parameters 鍙傛暟鎿嶄綔鍒楄〃
 */
    public MarkerDef(String id, MarkerType markerType, String typeName, Map<String, String> attributes, List<ParameterOp> parameters) {
        this.id = id;
        this.markerType = markerType;
        this.typeName = typeName;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    /**
 *
 * @return the marker unique identifier
 * @return 鏍囪鍞竴鏍囪瘑绗?
 */
    public String getId() { return id; }
    /**
 *
 * @return the marker type
 * @return 鏍囪绫诲瀷
 */
    public MarkerType getMarkerType() { return markerType; }
    /**
 *
 * @return the original marker type name
 * @return 鍘熷鏍囪绫诲瀷鍚嶇О
 */
    public String getTypeName() { return typeName; }
    /**
 *
 * @return the attribute key-value pairs (unmodifiable)
 * @return 灞炴€ч敭鍊煎锛堜笉鍙慨鏀癸級
 */
    public Map<String, String> getAttributes() { return attributes; }
    /**
 *
 * @return the parameter operation list (unmodifiable)
 * @return 鍙傛暟鎿嶄綔鍒楄〃锛堜笉鍙慨鏀癸級
 */
    public List<ParameterOp> getParameters() { return parameters; }

    /**
 * Gets the attribute value for the specified key.
 *
 * 鑾峰彇鎸囧畾閿殑灞炴€у€笺€?
 *
 * @param key the attribute key name
 * @param key 灞炴€ч敭鍚?
 * @return the attribute value, or null if not found
 * @return 灞炴€у€硷紝濡傛灉涓嶅瓨鍦ㄥ垯杩斿洖 null
 */
    public String getAttribute(String key) { return attributes.get(key); }

    @Override
    public String toString() {
        return markerType + "[" + id + "]";
    }
}