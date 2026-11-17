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

import net.minecraft.resources.ResourceLocation;

/**
 * Recipe parse result model.
 *
 * 閰嶆柟瑙ｆ瀽缁撴灉妯″瀷銆?
 * <p>
 * 鍖呭惈閰嶆柟ID鍙婂叾瀵瑰簲鐨勮В鏋愬悗鐨?RecipeData锛堣緭鍏?杈撳嚭鏁版嵁锛夈€?
 * </p>
 */
public class RecipeParseResult {
    /**
     * Recipe ID (ResourceLocation format)
     */
    /** 閰嶆柟ID锛圧esourceLocation 鏍煎紡锛?*/
    private final ResourceLocation recipeId;
    /**
     * Parsed recipe data
     */
    /** 瑙ｆ瀽鍚庣殑閰嶆柟鏁版嵁 */
    private final RecipeData data;

    /**
 * Constructs a recipe parse result instance.
 *
 * 鏋勯€犻厤鏂硅В鏋愮粨鏋滃疄渚嬨€?
 *
 * @param recipeId the recipe ID
 * @param data the parsed recipe data
 * @param recipeId 閰嶆柟ID
 * @param data     瑙ｆ瀽鍚庣殑閰嶆柟鏁版嵁
 */
    public RecipeParseResult(ResourceLocation recipeId, RecipeData data) {
        this.recipeId = recipeId;
        this.data = data;
    }

    /**
 *
 * @return the recipe ID
 * @return 閰嶆柟ID
 */
    public ResourceLocation getRecipeId() { return recipeId; }
    /**
 *
 * @return the parsed recipe data
 * @return 瑙ｆ瀽鍚庣殑閰嶆柟鏁版嵁
 */
    public RecipeData getData() { return data; }
}