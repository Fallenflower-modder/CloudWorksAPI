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

import java.util.ArrayList;
import java.util.List;

/**
 * RPML template tokenizer.
 *
 * RPML 妯℃澘璇嶆硶鍒嗘瀽鍣紙Tokenizer锛夈€?
 * <p>
 * 灏?RPML 妯℃澘鏂囨湰瀛楃涓茶В鏋愪负 Token 鍒楄〃銆?
 * 鏀寔鐨?Token 绫诲瀷鍖呮嫭 JSON 缁撴瀯绗﹀彿锛坽}銆乕]銆?銆?锛夈€?
 * 瀛楅潰閲忥紙瀛楃涓层€佹暟瀛椼€佸竷灏斻€乶ull锛変互鍙?RPML 鐗规湁鐨勬爣璁拌娉曪紙&lt;marker&gt; 鍜?&lt;/marker&gt;锛夈€?
 * 浼氳嚜鍔ㄨ烦杩囩┖鐧藉瓧绗︺€?
 * </p>
 */
public class TemplateTokenizer {

    /**
     * Input string to be parsed
     */
    /** 寰呰В鏋愮殑杈撳叆瀛楃涓?*/
    private final String input;
    /**
     * Current parse position
     */
    /** 褰撳墠瑙ｆ瀽浣嶇疆 */
    private int pos;

    /**
 * Constructs a tokenizer.
 *
 * 鏋勯€犺瘝娉曞垎鏋愬櫒銆?
 *
 * @param input the RPML template string to parse
 * @param input 寰呰В鏋愮殑 RPML 妯℃澘瀛楃涓?
 */
    public TemplateTokenizer(String input) {
        this.input = input;
        this.pos = 0;
    }

    /**
 * Executes tokenization, parsing the input string into a list of Tokens.
 *
 * 鎵ц璇嶆硶鍒嗘瀽锛屽皢杈撳叆瀛楃涓茶В鏋愪负 Token 鍒楄〃銆?
 *
 * @return the parsed token list
 * @return 瑙ｆ瀽鍚庣殑 Token 鍒楄〃
 */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }
            if (c == '{') {
                tokens.add(new Token(TokenType.LBRACE, "{"));
                pos++;
            } else if (c == '}') {
                tokens.add(new Token(TokenType.RBRACE, "}"));
                pos++;
            } else if (c == '[') {
                tokens.add(new Token(TokenType.LBRACKET, "["));
                pos++;
            } else if (c == ']') {
                tokens.add(new Token(TokenType.RBRACKET, "]"));
                pos++;
            } else if (c == ':') {
                tokens.add(new Token(TokenType.COLON, ":"));
                pos++;
            } else if (c == ',') {
                tokens.add(new Token(TokenType.COMMA, ","));
                pos++;
            } else if (c == '"') {
                tokens.add(readString());
            } else if (c == '<') {
                Token markerToken = readMarker();
                tokens.add(markerToken);
            } else if (c == '-' || Character.isDigit(c)) {
                tokens.add(readNumber());
            } else if (c == 't' || c == 'f') {
                tokens.add(readBoolean());
            } else if (c == 'n') {
                tokens.add(readNull());
            } else {
                // Skip unknown characters (shouldn't normally happen)
                pos++;
            }
        }
        return tokens;
    }

    /**
 * Reads a double-quoted string literal, supporting escape characters.
 *
 * 璇诲彇鍙屽紩鍙峰寘瑁圭殑瀛楃涓插瓧闈㈤噺锛屾敮鎸佽浆涔夊瓧绗︺€?
 *
 * @return a STRING type token
 * @return STRING 绫诲瀷鐨?Token
 */
    private Token readString() {
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos < input.length()) {
                    sb.append(input.charAt(pos));
                }
            } else if (c == '"') {
                pos++;
                break;
            } else {
                sb.append(c);
            }
            pos++;
        }
        return new Token(TokenType.STRING, sb.toString());
    }

    /**
 * Reads a number literal (supports integers, decimals, and negatives).
 *
 * 璇诲彇鏁板瓧瀛楅潰閲忥紙鏀寔鏁存暟銆佸皬鏁板拰璐熸暟锛夈€?
 *
 * @return a NUMBER type token
 * @return NUMBER 绫诲瀷鐨?Token
 */
    private Token readNumber() {
        int start = pos;
        if (input.charAt(pos) == '-') pos++;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }
        return new Token(TokenType.NUMBER, input.substring(start, pos));
    }

    /**
 * Reads a boolean literal (true or false).
 *
 * 璇诲彇甯冨皵瀛楅潰閲忥紙true 鎴?false锛夈€?
 *
 * @return a BOOLEAN type token
 * @return BOOLEAN 绫诲瀷鐨?Token
 */
    private Token readBoolean() {
        if (input.startsWith("true", pos)) {
            pos += 4;
            return new Token(TokenType.BOOLEAN, "true");
        } else {
            pos += 5;
            return new Token(TokenType.BOOLEAN, "false");
        }
    }

    /**
 * Reads a null literal.
 *
 * 璇诲彇 null 瀛楅潰閲忋€?
 *
 * @return a NULL type token
 * @return NULL 绫诲瀷鐨?Token
 */
    private Token readNull() {
        pos += 4;
        return new Token(TokenType.NULL, "null");
    }

    /**
 * Reads a marker (&lt;type,params&gt; or &lt;/type&gt;).
 *
 * 璇诲彇鏍囪锛?lt;type,params&gt; 鎴?&lt;/type&gt;锛夈€?
 *
 * @return a MARKER or MARKER_CLOSE type token
 * @return MARKER 鎴?MARKER_CLOSE 绫诲瀷鐨?Token
 */
    private Token readMarker() {
        pos++; // skip opening '<'
        if (pos < input.length() && input.charAt(pos) == '/') {
            return readMarkerClose();
        }
        return readMarkerOpen();
    }

    /**
 * Reads a closing tag (&lt;/type&gt;).
 *
 * 璇诲彇鍏抽棴鏍囪锛?lt;/type&gt;锛夈€?
 *
 * @return a MARKER_CLOSE type token
 * @return MARKER_CLOSE 绫诲瀷鐨?Token
 */
    private Token readMarkerClose() {
        pos++; // skip '/'
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && input.charAt(pos) != '>') {
            sb.append(input.charAt(pos));
            pos++;
        }
        pos++; // skip '>'
        return new Token(TokenType.MARKER_CLOSE, sb.toString().trim());
    }

    /**
 * Reads an opening tag (&lt;type,params...&gt;), supporting nested angle brackets.
 *
 * 璇诲彇寮€濮嬫爣璁帮紙&lt;type,params...&gt;锛夛紝鏀寔宓屽鐨勫皷鎷彿銆?
 *
 * @return a MARKER type token
 * @return MARKER 绫诲瀷鐨?Token
 */
    private Token readMarkerOpen() {
        StringBuilder body = new StringBuilder();
        int depth = 0;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                if (depth == 0) {
                    pos++;
                    break;
                }
                depth--;
            }
            body.append(c);
            pos++;
        }
        return new Token(TokenType.MARKER, body.toString().trim());
    }
}