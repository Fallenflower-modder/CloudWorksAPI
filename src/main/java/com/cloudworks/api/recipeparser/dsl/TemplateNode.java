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
 * RPML template AST node.
 *
 * RPML 妯℃澘璇硶鏍戣妭鐐广€?
 * <p>
 * 琛ㄧず妯℃澘瑙ｆ瀽鍚庣殑鎶借薄璇硶鏍戯紙AST锛変腑鐨勪竴涓妭鐐广€?
 * 鑺傜偣绫诲瀷鍖呮嫭 OBJECT锛堝璞★級銆丄RRAY锛堟暟缁勶級銆並EY_VALUE锛堥敭鍊煎锛夈€?
 * JSON_LITERAL锛圝SON 瀛楅潰閲忥級鍜?MARKER锛堟爣璁板紩鐢級銆?
 * </p>
 */
public class TemplateNode {
    /**
     * Template node type enum.
     */
    /** 妯℃澘鑺傜偣绫诲瀷鏋氫妇銆?*/
    public enum NodeType {
        /**
     * Object node, corresponding to {} in JSON
     */
    /** 瀵硅薄鑺傜偣锛屽搴?JSON 涓殑 {} */
        OBJECT,
        /**
     * Array node, corresponding to [] in JSON
     */
    /** 鏁扮粍鑺傜偣锛屽搴?JSON 涓殑 [] */
        ARRAY,
        /**
     * Key-value pair node, e.g., "key": value
     */
    /** 閿€煎鑺傜偣锛屽 "key": value */
        KEY_VALUE,
        /**
     * JSON literal node (string, number, boolean, null)
     */
    /** JSON 瀛楅潰閲忚妭鐐癸紙瀛楃涓层€佹暟瀛椼€佸竷灏斻€乶ull锛?*/
        JSON_LITERAL,
        /**
     * Marker node, referencing a MarkerDef
     */
    /** 鏍囪鑺傜偣锛屽紩鐢ㄤ竴涓?MarkerDef */
        MARKER
    }

    /**
     * Node type
     */
    /** 鑺傜偣绫诲瀷 */
    private NodeType type;
    /**
     * Key name (valid only for KEY_VALUE type)
     */
    /** 閿悕锛堜粎 KEY_VALUE 绫诲瀷鏈夋晥锛?*/
    private String key;
    /**
     * JSON literal value (valid only for JSON_LITERAL type)
     */
    /** JSON 瀛楅潰閲忓€硷紙浠?JSON_LITERAL 绫诲瀷鏈夋晥锛?*/
    private String jsonValue;
    /**
     * Associated marker definition (valid only for MARKER type)
     */
    /** 鍏宠仈鐨勬爣璁板畾涔夛紙浠?MARKER 绫诲瀷鏈夋晥锛?*/
    private MarkerDef marker;
    /**
     * Child node list
     */
    /** 瀛愯妭鐐瑰垪琛?*/
    private final List<TemplateNode> children;

    /**
 * Constructs a template node of the specified type.
 *
 * 鏋勯€犳寚瀹氱被鍨嬬殑妯℃澘鑺傜偣銆?
 *
 * @param type the node type
 * @param type 鑺傜偣绫诲瀷
 */
    public TemplateNode(NodeType type) {
        this.type = type;
        this.children = new ArrayList<>();
    }

    /**
 * Creates an object node.
 *
 * 鍒涘缓涓€涓璞¤妭鐐广€?
 *
 * @return a new object node
 * @return 鏂扮殑瀵硅薄鑺傜偣
 */
    public static TemplateNode objectNode() {
        return new TemplateNode(NodeType.OBJECT);
    }

    /**
 * Creates an array node.
 *
 * 鍒涘缓涓€涓暟缁勮妭鐐广€?
 *
 * @return a new array node
 * @return 鏂扮殑鏁扮粍鑺傜偣
 */
    public static TemplateNode arrayNode() {
        return new TemplateNode(NodeType.ARRAY);
    }

    /**
 * Creates a key-value pair node.
 *
 * 鍒涘缓涓€涓敭鍊煎鑺傜偣銆?
 *
 * @param key the key name
 * @param key 閿悕
 * @return a new key-value pair node
 * @return 鏂扮殑閿€煎鑺傜偣
 */
    public static TemplateNode keyValueNode(String key) {
        TemplateNode node = new TemplateNode(NodeType.KEY_VALUE);
        node.key = key;
        return node;
    }

    /**
 * Creates a JSON literal node.
 *
 * 鍒涘缓涓€涓?JSON 瀛楅潰閲忚妭鐐广€?
 *
 * @param value the literal value
 * @param value 瀛楅潰閲忓€?
 * @return a new JSON literal node
 * @return 鏂扮殑 JSON 瀛楅潰閲忚妭鐐?
 */
    public static TemplateNode jsonLiteralNode(String value) {
        TemplateNode node = new TemplateNode(NodeType.JSON_LITERAL);
        node.jsonValue = value;
        return node;
    }

    /**
 * Creates a marker node.
 *
 * 鍒涘缓涓€涓爣璁拌妭鐐广€?
 *
 * @param marker the associated marker definition
 * @param marker 鍏宠仈鐨勬爣璁板畾涔?
 * @return a new marker node
 * @return 鏂扮殑鏍囪鑺傜偣
 */
    public static TemplateNode markerNode(MarkerDef marker) {
        TemplateNode node = new TemplateNode(NodeType.MARKER);
        node.marker = marker;
        return node;
    }

    /**
 *
 * @return the node type
 * @return 鑺傜偣绫诲瀷
 */
    public NodeType getType() { return type; }
    /**
 *
 * @return the key name
 * @return 閿悕
 */
    public String getKey() { return key; }
    /**
 *
 * @return the JSON literal value
 * @return JSON 瀛楅潰閲忓€?
 */
    public String getJsonValue() { return jsonValue; }
    /**
 *
 * @return the associated marker definition
 * @return 鍏宠仈鐨勬爣璁板畾涔?
 */
    public MarkerDef getMarker() { return marker; }
    /**
 *
 * @return the child node list
 * @return 瀛愯妭鐐瑰垪琛?
 */
    public List<TemplateNode> getChildren() { return children; }

    /**
 * Adds a child node.
 *
 * 娣诲姞瀛愯妭鐐广€?
 *
 * @param child the child node
 * @param child 瀛愯妭鐐?
 */
    public void addChild(TemplateNode child) {
        children.add(child);
    }

    /**
 * Sets the key name and converts the node type from MARKER to KEY_VALUE.
 *
 * 璁剧疆閿悕锛屽苟灏嗚妭鐐圭被鍨嬩粠 MARKER 杞崲涓?KEY_VALUE銆?
 * <p>
 * ARRAY銆丱BJECT 鍜?JSON_LITERAL 鑺傜偣淇濇寔鍘熸湁绫诲瀷涓嶅彉锛?
 * 纭繚 extractNode 鏂规硶鑳芥纭垎鍙戙€?
 * </p>
 *
 * @param key the key name
 * @param key 閿悕
 */
    public void setKey(String key) {
        this.key = key;
        // Only change type for MARKER nodes. ARRAY, OBJECT, and JSON_LITERAL
        // nodes keep their original type so that extractNode dispatches correctly.
        if (this.type == NodeType.MARKER) {
            this.type = NodeType.KEY_VALUE;
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case OBJECT: return "Object" + children;
            case ARRAY: return "Array" + children;
            case KEY_VALUE: return key + ":" + (children.isEmpty() ? "?" : children.get(0));
            case JSON_LITERAL: return "\"" + jsonValue + "\"";
            case MARKER: return marker.toString();
            default: return "?";
        }
    }
}