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
 * Recipe product model.
 *
 * 閰嶆柟浜у嚭鐗╂ā鍨嬨€?
 * <p>
 * 琛ㄧず涓€鏉￠厤鏂逛腑鐨勫崟涓骇鍑虹墿鍝侊紝鍖呭惈鐗╁搧ID銆佹暟閲忋€佸崟浣嶃€佺被鍨嬨€?
 * 姒傜巼銆佹槸鍚﹁嚜鍔ㄨ浆鎹€佽浆鎹㈢巼绛変俊鎭€?
 * </p>
 */
public class Product {
    /**
     * Item or fluid ID, e.g., "minecraft:iron_ingot"
     */
    /** 鐗╁搧鎴栨祦浣揑D锛屼緥濡?"minecraft:iron_ingot" */
    private final String id;
    /**
     * Output count
     */
    /** 浜у嚭鏁伴噺 */
    private final double count;
    /**
     * Unit type, e.g., "item" or "fluid"
     */
    /** 鍗曚綅绫诲瀷锛屼緥濡?"item" 鎴?"fluid" */
    private final String unit;
    /**
     * Item type, e.g., "solid", "fluid"
     */
    /** 鐗╁搧绫诲瀷锛屼緥濡?"solid"銆?fluid" */
    private final String type;
    /**
     * Rate, range 0.0 ~ 1.0
     */
    /** 浜у嚭姒傜巼锛岃寖鍥?0.0 ~ 1.0 */
    private final double rate;
    /**
     * Whether to enable auto fluid-to-item transfer
     */
    /** 鏄惁鍚敤娴佷綋鍒扮墿鍝佺殑鑷姩杞崲 */
    private final boolean autotransfer;
    /**
     * Fluid-to-item transfer rate
     */
    /** 娴佷綋鍒扮墿鍝佽浆鎹㈢殑杞崲鐜?*/
    private final double transferrate;

    /**
 * Constructs a product instance.
 *
 * 鏋勯€犱竴涓骇鍑虹墿瀹炰緥銆?
 *
 * @param id the item or fluid ID
 * @param count the output count
 * @param unit the unit type
 * @param type the item type
 * @param rate the rate
 * @param autotransfer whether to enable auto transfer
 * @param transferrate the transfer rate
 * @param id           鐗╁搧鎴栨祦浣揑D
 * @param count        浜у嚭鏁伴噺
 * @param unit         鍗曚綅绫诲瀷
 * @param type         鐗╁搧绫诲瀷
 * @param rate         浜у嚭姒傜巼
 * @param autotransfer 鏄惁鍚敤鑷姩杞崲
 * @param transferrate 杞崲鐜?
 */
    public Product(String id, double count, String unit, String type, double rate,
                   boolean autotransfer, double transferrate) {
        this.id = id;
        this.count = count;
        this.unit = unit;
        this.type = type;
        this.rate = rate;
        this.autotransfer = autotransfer;
        this.transferrate = transferrate;
    }

    /**
 *
 * @return the item or fluid ID
 * @return 鐗╁搧鎴栨祦浣揑D
 */
    public String getId() { return id; }
    /**
 *
 * @return the output count
 * @return 浜у嚭鏁伴噺
 */
    public double getCount() { return count; }
    /**
 *
 * @return the unit type
 * @return 鍗曚綅绫诲瀷
 */
    public String getUnit() { return unit; }
    /**
 *
 * @return the item type
 * @return 鐗╁搧绫诲瀷
 */
    public String getType() { return type; }
    /**
 *
 * @return the rate
 * @return 浜у嚭姒傜巼
 */
    public double getRate() { return rate; }
    /**
 *
 * @return whether auto transfer is enabled
 * @return 鏄惁鍚敤鑷姩杞崲
 */
    public boolean isAutotransfer() { return autotransfer; }
    /**
 *
 * @return the transfer rate
 * @return 杞崲鐜?
 */
    public double getTransferrate() { return transferrate; }
}