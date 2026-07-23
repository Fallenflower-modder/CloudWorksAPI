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
package com.cloudworks.api.recipeparser.exception;

/**
 * Recipe parse exception.
 *
 * 閰嶆柟瑙ｆ瀽寮傚父銆?
 * <p>
 * 褰撻厤鏂规暟鎹В鏋愯繃绋嬩腑鍙戠敓閿欒鏃舵姏鍑猴紝渚嬪妯℃澘涓嶅瓨鍦ㄣ€?
 * 妯℃澘涓庨厤鏂规暟鎹笉鍖归厤銆丣SON 搴忓垪鍖栧け璐ョ瓑鎯呭喌銆?
 * </p>
 */
public class RecipeParseException extends Exception {

    /**
 * Constructs a parse exception with an error message.
 *
 * 鏋勯€犲甫閿欒淇℃伅鐨勮В鏋愬紓甯搞€?
 *
 * @param message the error description message
 * @param message 閿欒鎻忚堪淇℃伅
 */
    public RecipeParseException(String message) {
        super(message);
    }

    /**
 * Constructs a parse exception with an error message and cause.
 *
 * 鏋勯€犲甫閿欒淇℃伅鍜屽師鍥犵殑瑙ｆ瀽寮傚父銆?
 *
 * @param message the error description message
 * @param cause the original exception
 * @param message 閿欒鎻忚堪淇℃伅
 * @param cause   鍘熷寮傚父
 */
    public RecipeParseException(String message, Throwable cause) {
        super(message, cause);
    }
}