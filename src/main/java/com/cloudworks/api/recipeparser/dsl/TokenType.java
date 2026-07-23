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

/**
 * Token type enum for the template tokenizer.
 *
 * 妯℃澘璇嶆硶鍒嗘瀽鍣ㄤ腑 Token 鐨勭被鍨嬫灇涓俱€?
 * <p>
 * 娑电洊 JSON 缁撴瀯绗﹀彿锛堣姳鎷彿銆佹柟鎷彿銆佸啋鍙枫€侀€楀彿锛夈€?
 * 瀛楅潰閲忕被鍨嬶紙瀛楃涓层€佹暟瀛椼€佸竷灏斻€乶ull锛変互鍙?
 * RPML 妯℃澘鐗规湁鏍囪锛圡ARKER 鍜?MARKER_CLOSE锛夈€?
 * </p>
 */
public enum TokenType {
    /**
     * Left brace "{"
     */
    /** 宸﹁姳鎷彿 "{" */
    LBRACE,
    /**
     * Right brace "}"
     */
    /** 鍙宠姳鎷彿 "}" */
    RBRACE,
    /**
     * Left bracket "["
     */
    /** 宸︽柟鎷彿 "[" */
    LBRACKET,
    /**
     * Right bracket "]"
     */
    /** 鍙虫柟鎷彿 "]" */
    RBRACKET,
    /**
     * Colon ":"
     */
    /** 鍐掑彿 ":" */
    COLON,
    /**
     * Comma ","
     */
    /** 閫楀彿 "," */
    COMMA,
    /**
     * String literal
     */
    /** 瀛楃涓插瓧闈㈤噺 */
    STRING,
    /**
     * Number literal
     */
    /** 鏁板瓧瀛楅潰閲?*/
    NUMBER,
    /**
     * Boolean literal
     */
    /** 甯冨皵瀛楅潰閲?*/
    BOOLEAN,
    /**
     * null literal
     */
    /** null 瀛楅潰閲?*/
    NULL,
    /**
     * RPML marker start "&lt;type,params...&gt;"
     */
    /** RPML 鏍囪寮€濮?"&lt;type,params...&gt;" */
    MARKER,
    /**
     * RPML marker end "&lt;/type&gt;"
     */
    /** RPML 鏍囪缁撴潫 "&lt;/type&gt;" */
    MARKER_CLOSE
}