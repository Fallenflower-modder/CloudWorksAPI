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
 * Recipe not found exception.
 *
 * 閰嶆柟鏈壘鍒板紓甯搞€?
 * <p>
 * 褰撹姹傜殑閰嶆柟ID鍦?RecipeManager 涓笉瀛樺湪鏃舵姏鍑恒€?
 * </p>
 */
public class RecipeNotFoundException extends Exception {

    /**
 * Constructs a not-found exception with an error message.
 *
 * 鏋勯€犲甫閿欒淇℃伅鐨勬湭鎵惧埌寮傚父銆?
 *
 * @param message the error description message
 * @param message 閿欒鎻忚堪淇℃伅
 */
    public RecipeNotFoundException(String message) {
        super(message);
    }

    /**
 * Constructs a not-found exception with an error message and cause.
 *
 * 鏋勯€犲甫閿欒淇℃伅鍜屽師鍥犵殑鏈壘鍒板紓甯搞€?
 *
 * @param message the error description message
 * @param cause the original exception
 * @param message 閿欒鎻忚堪淇℃伅
 * @param cause   鍘熷寮傚父
 */
    public RecipeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}