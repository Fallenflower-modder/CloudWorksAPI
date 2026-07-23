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
package com.cloudworks.api.recipeparser.model;

/**
 * Recipe query mode enum.
 *
 * 閰嶆柟鏌ヨ妯″紡鏋氫妇銆?
 * <p>
 * 鐢ㄤ簬鎸囧畾閰嶆柟鏌ヨ鏃舵寜鐗╁搧锛圛TEM锛夎繕鏄寜娴佷綋锛團LUID锛夎繘琛屽尮閰嶃€?
 * </p>
 */
public enum QueryMode {
    /**
     * Query by item mode
     */
    /** 鎸夌墿鍝佹ā寮忔煡璇?*/
    ITEM,
    /**
     * Query by fluid mode
     */
    /** 鎸夋祦浣撴ā寮忔煡璇?*/
    FLUID
}