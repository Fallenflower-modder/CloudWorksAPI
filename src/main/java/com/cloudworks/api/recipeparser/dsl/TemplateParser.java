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
 * RPML template syntax parser.
 *
 * RPML 妯℃澘璇硶瑙ｆ瀽鍣紙Parser锛夈€?
 * <p>
 * 灏嗚瘝娉曞垎鏋愬櫒鐢熸垚鐨?Token 鍒楄〃瑙ｆ瀽涓烘ā鏉胯娉曟爲锛圓ST锛夈€?
 * 鏀寔 JSON 瀵硅薄/鏁扮粍缁撴瀯銆侀敭鍊煎銆佸瓧闈㈤噺浠ュ強 RPML 鏍囪璇硶銆?
 * 鑷姩涓烘病鏈夋樉寮忔寚瀹?ID 鐨勬爣璁扮敓鎴愬敮涓€鏍囪瘑绗︼紝骞跺鐞嗛噸澶?ID 鍐茬獊銆?
 * </p>
 * <p>
 * 鏍囪浣撴牸寮忎负锛?code>type,param1=val1,param2=val2 if cond for target</code>銆?
 * 棣栧厛鎻愬彇鏍囪绫诲瀷锛岀劧鍚庤В鏋愬墿浣欏弬鏁帮紝鍖哄垎灞炴€ч敭鍊煎鍜屽弬鏁版搷浣溿€?
 * </p>
 */
public class TemplateParser {

    /**
     * Token list
     */
    /** Token 鍒楄〃 */
    private final List<Token> tokens;
    /**
     * Current parse position
     */
    /** 褰撳墠瑙ｆ瀽浣嶇疆 */
    private int pos;
    /**
     * Set of used IDs for uniqueness checking
     */
    /** 宸蹭娇鐢ㄧ殑 ID 闆嗗悎锛岀敤浜庡敮涓€鎬ф鏌?*/
    private final Set<String> usedIds = new HashSet<>();
    /**
     * Counter for auto-generated IDs
     */
    /** 鑷姩鐢熸垚 ID 鐨勮鏁板櫒 */
    private int autoIdCounter = 0;

    /**
 * Constructs a syntax parser.
 *
 * 鏋勯€犺娉曡В鏋愬櫒銆?
 *
 * @param tokens the tokenized token list
 * @param tokens 璇嶆硶鍒嗘瀽鍚庣殑 Token 鍒楄〃
 */
    public TemplateParser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    /**
 * Executes syntax parsing, returns the AST root node.
 *
 * 鎵ц璇硶瑙ｆ瀽锛岃繑鍥炶娉曟爲鏍硅妭鐐广€?
 *
 * @return the parsed AST root node
 * @return 瑙ｆ瀽鍚庣殑璇硶鏍戞牴鑺傜偣
 * @throws ParseException if a syntax error is encountered
 * @throws ParseException 濡傛灉閬囧埌璇硶閿欒
 */
    public TemplateNode parse() {
        return parseValue();
    }

    /**
 * Peeks at the token at the current position without moving the pointer.
 *
 * 鏌ョ湅褰撳墠浣嶇疆鐨?Token锛屼笉绉诲姩鎸囬拡銆?
 *
 * @return the current token, or null if at end
 * @return 褰撳墠 Token锛屽鏋滃凡鍒版湯灏惧垯杩斿洖 null
 */
    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }

    /**
 * Consumes the current token and moves the pointer.
 *
 * 娑堣垂褰撳墠 Token 骞剁Щ鍔ㄦ寚閽堛€?
 *
 * @return the current token
 * @return 褰撳墠 Token
 */
    private Token consume() {
        return tokens.get(pos++);
    }

    /**
 * Expects the current token to be of the specified type, otherwise throws an exception.
 *
 * 鏈熸湜褰撳墠 Token 涓烘寚瀹氱被鍨嬶紝鍚﹀垯鎶涘嚭寮傚父銆?
 *
 * @param type the expected token type
 * @param type 鏈熸湜鐨?Token 绫诲瀷
 * @throws ParseException if the type does not match
 * @throws ParseException 濡傛灉绫诲瀷涓嶅尮閰?
 */
    private void expect(TokenType type) {
        Token t = consume();
        if (t.type != type) {
            throw new ParseException("Expected " + type + " but got " + t);
        }
    }

    /**
 * Dispatches parsing to the corresponding method based on the current token type.
 *
 * 鏍规嵁褰撳墠 Token 绫诲瀷鍒嗗彂瑙ｆ瀽鍒板搴旂殑瑙ｆ瀽鏂规硶銆?
 *
 * @return the parsed node
 * @return 瑙ｆ瀽鍚庣殑鑺傜偣
 */
    private TemplateNode parseValue() {
        Token t = peek();
        if (t == null) return null;

        switch (t.type) {
            case LBRACE: return parseObject();
            case LBRACKET: return parseArray();
            case STRING: return parseJsonLiteral(consume());
            case NUMBER: return parseJsonLiteral(consume());
            case BOOLEAN: return parseJsonLiteral(consume());
            case NULL: return parseJsonLiteral(consume());
            case MARKER: return parseMarker();
            default:
                throw new ParseException("Unexpected token: " + t);
        }
    }

    /**
 * Parses a JSON object ({}).
 *
 * 瑙ｆ瀽 JSON 瀵硅薄锛坽}锛夈€?
 *
 * @return an OBJECT type node
 * @return OBJECT 绫诲瀷鐨勮妭鐐?
 */
    private TemplateNode parseObject() {
        TemplateNode object = TemplateNode.objectNode();
        expect(TokenType.LBRACE);

        while (peek() != null && peek().type != TokenType.RBRACE) {
            if (peek().type == TokenType.COMMA) {
                consume();
                continue;
            }
            // Handle standalone markers (e.g., <optional>, <object>, <duplicate>) inside objects
            if (peek().type == TokenType.MARKER) {
                TemplateNode markerNode = parseMarker();
                object.addChild(markerNode);
                continue;
            }
            // KEY_VALUE: STRING COLON value
            Token keyToken = consume();
            if (keyToken.type != TokenType.STRING) {
                throw new ParseException("Expected STRING key but got " + keyToken);
            }
            expect(TokenType.COLON);
            TemplateNode value = parseValue();
            value.setKey(keyToken.value);
            object.addChild(value);
        }

        expect(TokenType.RBRACE);
        return object;
    }

    /**
 * Parses a JSON array ([]).
 *
 * 瑙ｆ瀽 JSON 鏁扮粍锛圼]锛夈€?
 *
 * @return an ARRAY type node
 * @return ARRAY 绫诲瀷鐨勮妭鐐?
 */
    private TemplateNode parseArray() {
        TemplateNode array = TemplateNode.arrayNode();
        expect(TokenType.LBRACKET);

        while (peek() != null && peek().type != TokenType.RBRACKET) {
            if (peek().type == TokenType.COMMA) {
                consume();
                continue;
            }
            TemplateNode value = parseValue();
            array.addChild(value);
        }

        expect(TokenType.RBRACKET);
        return array;
    }

    /**
 * Converts a literal token to a JSON_LITERAL node.
 *
 * 灏嗗瓧闈㈤噺 Token 杞崲涓?JSON_LITERAL 鑺傜偣銆?
 *
 * @param t the literal token
 * @param t 瀛楅潰閲?Token
 * @return a JSON_LITERAL type node
 * @return JSON_LITERAL 绫诲瀷鐨勮妭鐐?
 */
    private TemplateNode parseJsonLiteral(Token t) {
        return TemplateNode.jsonLiteralNode(t.value);
    }

    /**
 * 瑙ｆ瀽鏍囪锛?lt;type,params...&gt;锛夈€?
 * <p>
 * 濡傛灉鏄垚瀵规爣璁帮紙object銆乷ptional銆乵atrix锛夛紝鍒欑户缁В鏋愬叾瀛愯妭鐐圭洿鍒伴亣鍒板叧闂爣璁般€?
 * </p>
 *
 *
 * 瑙ｆ瀽鏍囪锛?lt;type,params...&gt;锛夈€?
 * <p>
 * 濡傛灉鏄垚瀵规爣璁帮紙object銆乷ptional銆乵atrix锛夛紝鍒欑户缁В鏋愬叾瀛愯妭鐐圭洿鍒伴亣鍒板叧闂爣璁般€?
 * </p>
 *
 * @return a MARKER type node (may contain child nodes)
 * @return MARKER 绫诲瀷鐨勮妭鐐癸紙鍙兘鍖呭惈瀛愯妭鐐癸級
 */
    private TemplateNode parseMarker() {
        Token markerToken = consume();
        String rawMarker = markerToken.value;

        // Parse marker body: "type,param1=val1,param2=val2"
        MarkerParseResult parsed = parseMarkerBody(rawMarker);

        // Check if this is a paired marker (has children)
        List<TemplateNode> children = new ArrayList<>();
        if (isPairedMarkerType(parsed.markerType)) {
            // Parse children: "object" and "optional" have key-value children,
            // "matrix" has value children
            if (parsed.markerType.equals("object") || parsed.markerType.equals("optional")) {
                while (peek() != null) {
                    if (peek().type == TokenType.MARKER_CLOSE) {
                        Token close = consume();
                        if (!close.value.equals(parsed.markerType)) {
                            throw new ParseException("Mismatched closing tag: expected </" + parsed.markerType + "> but got </" + close.value + ">");
                        }
                        break;
                    }
                    if (peek().type == TokenType.COMMA) {
                        consume();
                        continue;
                    }
                    // Handle nested markers inside object/optional
                    if (peek().type == TokenType.MARKER) {
                        TemplateNode child = parseMarker();
                        children.add(child);
                        continue;
                    }
                    // KEY_VALUE: STRING COLON value
                    Token keyToken = consume();
                    if (keyToken.type != TokenType.STRING) {
                        throw new ParseException("Expected STRING key inside <" + parsed.markerType + "> but got " + keyToken);
                    }
                    expect(TokenType.COLON);
                    TemplateNode value = parseValue();
                    value.setKey(keyToken.value);
                    children.add(value);
                }
            } else {
                // Parse children as values (e.g., <matrix>)
                while (peek() != null) {
                    if (peek().type == TokenType.MARKER_CLOSE) {
                        Token close = consume();
                        if (!close.value.equals(parsed.markerType)) {
                            throw new ParseException("Mismatched closing tag: expected </" + parsed.markerType + "> but got </" + close.value + ">");
                        }
                        break;
                    }
                    TemplateNode child = parseValue();
                    children.add(child);
                }
            }
        }

        MarkerDef marker = new MarkerDef(parsed.id, parsed.markerTypeEnum, parsed.markerType, parsed.attributes, parsed.parameters);
        TemplateNode node = TemplateNode.markerNode(marker);
        for (TemplateNode child : children) {
            node.addChild(child);
        }
        return node;
    }

    /**
 * Determines whether the marker type is a paired marker (requiring a closing tag).
 *
 * 鍒ゆ柇鏍囪绫诲瀷鏄惁涓烘垚瀵规爣璁帮紙闇€瑕佸叧闂爣璁帮級銆?
 *
 * @param type the marker type string
 * @param type 鏍囪绫诲瀷瀛楃涓?
 * @return true if it is a paired marker
 * @return 濡傛灉鏄垚瀵规爣璁板垯杩斿洖 true
 */
    private boolean isPairedMarkerType(String type) {
        return type.equals("object") || type.equals("optional") || type.equals("matrix");
    }

    /**
 * Parses marker body content, extracting marker type, parameters, and attributes.
 *
 * 瑙ｆ瀽鏍囪浣撳唴瀹癸紝鎻愬彇鏍囪绫诲瀷銆佸弬鏁板拰灞炴€с€?
 *
 * @param body the marker body string
 * @param body 鏍囪浣撳瓧绗︿覆
 * @return the parsed marker intermediate result
 * @return 瑙ｆ瀽鍚庣殑鏍囪涓棿缁撴灉
 */
    private MarkerParseResult parseMarkerBody(String body) {
        // body format: "type,param1=val1,param2=val2 if cond for target"
        // First, extract the marker type (everything before the first comma)
        int firstComma = body.indexOf(',');
        String markerType;
        String rest;
        if (firstComma == -1) {
            markerType = body.trim();
            rest = "";
        } else {
            markerType = body.substring(0, firstComma).trim();
            rest = body.substring(firstComma + 1).trim();
        }

        String id = null;
        Map<String, String> attributes = new LinkedHashMap<>();
        List<ParameterOp> parameters = new ArrayList<>();

        // Parse remaining parameters
        List<String> paramParts = splitParameters(rest);
        for (String part : paramParts) {
            ParseParamResult result = parseParameterPart(part);
            if (result == null) continue;

            // Check if this is the "id" parameter
            if (result.key.equals("id")) {
                id = result.value;
                continue;
            }

            // Check if this is a parameter operation (starts with special prefixes)
            if (isParameterOp(result.key)) {
                parameters.add(new ParameterOp(result.key, result.value, result.condition, result.forTarget));
            } else {
                attributes.put(result.key, result.value);
            }
        }

        if (id == null) {
            // Auto-generate unique ID for markers without explicit id
            // IO_ATTRIBUTE and SCRIPT: use marker type name + counter
            // Other types: use "auto" prefix + counter
            MarkerDef.MarkerType resolvedType = MarkerParseResult.resolveMarkerType(markerType);
            if (resolvedType == MarkerDef.MarkerType.IO_ATTRIBUTE || resolvedType == MarkerDef.MarkerType.SCRIPT) {
                id = generateUniqueId(markerType);
            } else {
                id = generateUniqueId("auto");
            }
        } else {
            // Register the user-provided ID and check for duplicates.
            // INPUT and OUTPUT markers are allowed to share IDs 鈥?they are targets
            // of input_id/output_id references from IO_ATTRIBUTE markers.
            // Uniquifying them would break the reference chain.
            MarkerDef.MarkerType resolvedType = MarkerParseResult.resolveMarkerType(markerType);
            boolean isInputOrOutput = resolvedType == MarkerDef.MarkerType.INPUT
                                   || resolvedType == MarkerDef.MarkerType.OUTPUT;
            if (!isInputOrOutput && !usedIds.add(id)) {
                // Duplicate ID - generate a unique one instead
                id = generateUniqueId(id);
            } else {
                usedIds.add(id);
            }
        }

        return new MarkerParseResult(id, markerType, attributes, parameters);
    }

    /**
 * Splits parameter list by commas, considering nested parentheses and angle bracket depth.
 *
 * 鎸夐€楀彿鍒嗗壊鍙傛暟鍒楄〃锛岃€冭檻宓屽鎷彿鍜屽皷鎷彿鐨勬繁搴︺€?
 *
 * @param rest the parameter string
 * @param rest 鍙傛暟瀛楃涓?
 * @return the split parameter list
 * @return 鍒嗗壊鍚庣殑鍙傛暟鍒楄〃
 */
    private List<String> splitParameters(String rest) {
        List<String> parts = new ArrayList<>();
        if (rest.isEmpty()) return parts;

        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == '(') depth++;
            else if (c == ')') depth--;

            if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    /**
 * Parses a single parameter part, extracting key, value, condition, and target.
 *
 * 瑙ｆ瀽鍗曚釜鍙傛暟閮ㄥ垎锛屾彁鍙栭敭銆佸€笺€佹潯浠跺拰鐩爣銆?
 *
 * @param part the parameter string
 * @param part 鍙傛暟瀛楃涓?
 * @return the parsed parameter result, or null if invalid
 * @return 瑙ｆ瀽鍚庣殑鍙傛暟缁撴灉锛屽鏋滄棤鏁堝垯杩斿洖 null
 */
    private ParseParamResult parseParameterPart(String part) {
        if (part.isEmpty()) return null;

        // Split by "=" but only at the top level
        int eqIdx = findTopLevel(part, '=');
        if (eqIdx == -1) return null;

        String key = part.substring(0, eqIdx).trim();
        String valueRest = part.substring(eqIdx + 1).trim();

        // Check for "if" and "for" clauses
        String condition = null;
        String forTarget = null;

        // From right to left: "if cond for target" means for is outer, if is inner
        // But the syntax allows both orders, so we need to find both
        int forIdx = findTopLevelKeyword(valueRest, " for ");
        int ifIdx = findTopLevelKeyword(valueRest, " if ");

        if (forIdx != -1) {
            // Extract the for target (after " for ")
            String forPart = valueRest.substring(forIdx + 5).trim();
            forTarget = forPart;
            valueRest = valueRest.substring(0, forIdx).trim();

            // Check if there's also an "if" before the "for"
            ifIdx = findTopLevelKeyword(valueRest, " if ");
        }

        if (ifIdx != -1) {
            condition = valueRest.substring(ifIdx + 4).trim();
            valueRest = valueRest.substring(0, ifIdx).trim();
        }

        return new ParseParamResult(key, valueRest, condition, forTarget);
    }

    /**
 * Finds the position of a specified character at the top level (not inside nested parentheses/angle brackets).
 *
 * 鍦ㄥ瓧绗︿覆涓煡鎵鹃《灞傦紙涓嶅湪宓屽鎷彿/灏栨嫭鍙峰唴锛夌殑鎸囧畾瀛楃浣嶇疆銆?
 *
 * @param s the string to search
 * @param target the target character
 * @param s      寰呮悳绱㈢殑瀛楃涓?
 * @param target 鐩爣瀛楃
 * @return the index of the target character, or -1 if not found
 * @return 鐩爣瀛楃鐨勭储寮曚綅缃紝鏈壘鍒拌繑鍥?-1
 */
    private int findTopLevel(String s, char target) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == target && depth == 0) return i;
        }
        return -1;
    }

    /**
 * Finds the position of a keyword at the top level (not inside nested parentheses/angle brackets).
 *
 * 鍦ㄥ瓧绗︿覆涓煡鎵鹃《灞傦紙涓嶅湪宓屽鎷彿/灏栨嫭鍙峰唴锛夌殑鍏抽敭璇嶄綅缃€?
 *
 * @param s the string to search
 * @param keyword the target keyword
 * @param s       寰呮悳绱㈢殑瀛楃涓?
 * @param keyword 鐩爣鍏抽敭璇?
 * @return the starting index of the keyword, or -1 if not found
 * @return 鍏抽敭璇嶇殑璧峰绱㈠紩浣嶇疆锛屾湭鎵惧埌杩斿洖 -1
 */
    private int findTopLevelKeyword(String s, String keyword) {
        int depth = 0;
        for (int i = 0; i <= s.length() - keyword.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (depth == 0 && s.startsWith(keyword, i)) return i;
        }
        return -1;
    }

    /**
 * Determines whether a key name is a parameter operation (rather than a regular attribute).
 *
 * 鍒ゆ柇閿悕鏄惁涓哄弬鏁版搷浣滐紙鑰岄潪鏅€氬睘鎬э級銆?
 *
 * @param key the key name
 * @param key 閿悕
 * @return true if it is a parameter operation
 * @return 濡傛灉鏄弬鏁版搷浣滃垯杩斿洖 true
 */
    private boolean isParameterOp(String key) {
        // "input_id" and "output_id" are marker attributes that reference other markers,
        // NOT parameter operations. They must be excluded or the IO_ATTRIBUTE linking breaks.
        if (key.equals("input_id") || key.equals("output_id")) return false;
        return key.startsWith("default_") || key.startsWith("set_") || key.startsWith("override_")
                || key.startsWith("variables_") || key.startsWith("input_") || key.startsWith("output_");
    }

    /**
 * Generates a unique ID, appending a numeric suffix if the base ID is already taken.
 *
 * 鐢熸垚鍞竴鐨?ID锛屽鏋滃熀鍑?ID 宸茶鍗犵敤鍒欒拷鍔犳暟瀛楀悗缂€銆?
 *
 * @param base the base ID
 * @param base 鍩哄噯 ID
 * @return a unique ID
 * @return 鍞竴鐨?ID
 */
    private String generateUniqueId(String base) {
        if (usedIds.add(base)) {
            return base;
        }
        String id;
        do {
            id = base + autoIdCounter++;
        } while (!usedIds.add(id));
        return id;
    }

    // --- Inner classes ---

    /**
     * Parameter parsing intermediate result.
     */
    /** 鍙傛暟瑙ｆ瀽涓棿缁撴灉銆?*/
    private static class ParseParamResult {
        /**
     * Parameter key name
     */
    /** 鍙傛暟閿悕 */
        final String key;
        /**
     * Parameter value
     */
    /** 鍙傛暟鍊?*/
        final String value;
        /**
     * Condition clause
     */
    /** 鏉′欢瀛愬彞 */
        final String condition;
        /**
     * For-target clause
     */
    /** 鎸囧畾鐩爣瀛愬彞 */
        final String forTarget;

        ParseParamResult(String key, String value, String condition, String forTarget) {
            this.key = key;
            this.value = value;
            this.condition = condition;
            this.forTarget = forTarget;
        }
    }

    /**
     * Marker body parsing intermediate result.
     */
    /** 鏍囪浣撹В鏋愪腑闂寸粨鏋溿€?*/
    private static class MarkerParseResult {
        /**
     * Marker ID
     */
    /** 鏍囪 ID */
        final String id;
        /**
     * Marker type string
     */
    /** 鏍囪绫诲瀷瀛楃涓?*/
        final String markerType;
        /**
     * Resolved marker type enum
     */
    /** 瑙ｆ瀽鍚庣殑鏍囪绫诲瀷鏋氫妇 */
        final MarkerDef.MarkerType markerTypeEnum;
        /**
     * Attribute key-value pairs
     */
    /** 灞炴€ч敭鍊煎 */
        final Map<String, String> attributes;
        /**
     * Parameter operation list
     */
    /** 鍙傛暟鎿嶄綔鍒楄〃 */
        final List<ParameterOp> parameters;

        MarkerParseResult(String id, String markerType, Map<String, String> attributes, List<ParameterOp> parameters) {
            this.id = id;
            this.markerType = markerType;
            this.markerTypeEnum = resolveMarkerType(markerType);
            this.attributes = attributes;
            this.parameters = parameters;
        }

        /**
 * 灏嗘爣璁扮被鍨嬪瓧绗︿覆瑙ｆ瀽涓?MarkerType 鏋氫妇銆?
 * 宸茬煡绫诲瀷锛坕nput銆乷utput銆乿ariable 绛夛級鐩存帴鏄犲皠锛?
 * 鍏朵綑绫诲瀷锛堝 count銆乼ype銆乽nit銆乺ate锛夊綊绫讳负 IO_ATTRIBUTE銆?
 *
 *
 * 灏嗘爣璁扮被鍨嬪瓧绗︿覆瑙ｆ瀽涓?MarkerType 鏋氫妇銆?
 * 宸茬煡绫诲瀷锛坕nput銆乷utput銆乿ariable 绛夛級鐩存帴鏄犲皠锛?
 * 鍏朵綑绫诲瀷锛堝 count銆乼ype銆乽nit銆乺ate锛夊綊绫讳负 IO_ATTRIBUTE銆?
 *
 * @param type the marker type string
 * @param type 鏍囪绫诲瀷瀛楃涓?
 * @return the corresponding MarkerType enum
 * @return 瀵瑰簲鐨?MarkerType 鏋氫妇
 */
        private static MarkerDef.MarkerType resolveMarkerType(String type) {
            switch (type) {
                case "input": return MarkerDef.MarkerType.INPUT;
                case "output": return MarkerDef.MarkerType.OUTPUT;
                case "variable": return MarkerDef.MarkerType.VARIABLE;
                case "object": return MarkerDef.MarkerType.OBJECT;
                case "optional": return MarkerDef.MarkerType.OPTIONAL;
                case "matrix": return MarkerDef.MarkerType.MATRIX;
                case "matrixline": return MarkerDef.MarkerType.MATRIXLINE;
                case "duplicate": return MarkerDef.MarkerType.DUPLICATE;
                case "script": return MarkerDef.MarkerType.SCRIPT;
                default: return MarkerDef.MarkerType.IO_ATTRIBUTE; // count, type, unit, rate, etc.
            }
        }
    }

    /**
     * Template parse exception.
     */
    /** 妯℃澘瑙ｆ瀽寮傚父銆?*/
    public static class ParseException extends RuntimeException {
        /**
 * Constructs a parse exception.
 *
 * 鏋勯€犺В鏋愬紓甯搞€?
 *
 * @param message the error message
 * @param message 閿欒淇℃伅
 */
        public ParseException(String message) {
            super(message);
        }
    }
}