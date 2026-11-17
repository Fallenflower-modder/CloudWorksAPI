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
 * Recipe input material model.
 *
 * 閰嶆柟杈撳叆鏉愭枡妯″瀷銆?
 * <p>
 * 琛ㄧず涓€鏉￠厤鏂逛腑鐨勫崟涓緭鍏ョ墿鍝侊紝鍖呭惈鐗╁搧ID銆佹暟閲忋€佸崟浣嶃€佺被鍨嬨€?
 * 姒傜巼銆佹槸鍚︿笉鍙崯鍧忋€佸揩鎹锋柟寮忕瓑淇℃伅銆?
 * </p>
 */
public class Ingredient {
    /**
     * Item or fluid ID, e.g., "minecraft:iron_ingot"
     */
    /** 鐗╁搧鎴栨祦浣揑D锛屼緥濡?"minecraft:iron_ingot" */
    private final String id;
    /**
     * Material count
     */
    /** 鏉愭枡鏁伴噺 */
    private final double count;
    /**
     * Unit type, e.g., "item" or "fluid"
     */
    /** 鍗曚綅绫诲瀷锛屼緥濡?"item" 鎴?"fluid" */
    private final String unit;
    /**
     * Item type, e.g., "solid", "tag", "fluid"
     */
    /** 鐗╁搧绫诲瀷锛屼緥濡?"solid"銆?tag"銆?fluid" */
    private final String type;
    /**
     * Rate, range 0.0 ~ 1.0
     */
    /** 浜у嚭姒傜巼锛岃寖鍥?0.0 ~ 1.0 */
    private final double rate;
    /**
     * Whether non-damageable (e.g., tool items)
     */
    /** 鏄惁涓嶅彲鎹熷潖锛堝宸ュ叿绫荤墿鍝侊級 */
    private final boolean nondamageable;
    /**
     * Shortcut flag, may be null
     */
    /** 蹇嵎鏂瑰紡鏍囪锛屽彲鑳戒负 null */
    private final String shortcut;
    /**
     * Whether to count in total statistics
     */
    /** 鏄惁璁″叆鎬绘暟閲忕粺璁?*/
    private final boolean counton;

    /**
 * Constructs an input material instance.
 *
 * 鏋勯€犱竴涓緭鍏ユ潗鏂欏疄渚嬨€?
 *
 * @param id the item or fluid ID
 * @param count the material count
 * @param unit the unit type
 * @param type the item type
 * @param rate the rate
 * @param nondamageable whether non-damageable
 * @param shortcut the shortcut flag
 * @param counton whether to count in total
 * @param id            鐗╁搧鎴栨祦浣揑D
 * @param count         鏉愭枡鏁伴噺
 * @param unit          鍗曚綅绫诲瀷
 * @param type          鐗╁搧绫诲瀷
 * @param rate          浜у嚭姒傜巼
 * @param nondamageable 鏄惁涓嶅彲鎹熷潖
 * @param shortcut      蹇嵎鏂瑰紡鏍囪
 * @param counton       鏄惁璁″叆鎬绘暟閲忕粺璁?
 */
    public Ingredient(String id, double count, String unit, String type, double rate,
                      boolean nondamageable, String shortcut, boolean counton) {
        this.id = id;
        this.count = count;
        this.unit = unit;
        this.type = type;
        this.rate = rate;
        this.nondamageable = nondamageable;
        this.shortcut = shortcut;
        this.counton = counton;
    }

    /**
 *
 * @return the item or fluid ID
 * @return 鐗╁搧鎴栨祦浣揑D
 */
    public String getId() { return id; }
    /**
 *
 * @return the material count
 * @return 鏉愭枡鏁伴噺
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
 * @return whether non-damageable
 * @return 鏄惁涓嶅彲鎹熷潖
 */
    public boolean isNondamageable() { return nondamageable; }
    /**
 *
 * @return the shortcut flag, may be null
 * @return 蹇嵎鏂瑰紡鏍囪锛屽彲鑳戒负 null
 */
    public String getShortcut() { return shortcut; }
    /**
 *
 * @return whether counted in total
 * @return 鏄惁璁″叆鎬绘暟閲忕粺璁?
 */
    public boolean isCounton() { return counton; }
}