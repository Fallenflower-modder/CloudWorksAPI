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

import java.util.Collections;
import java.util.List;

/**
 * Recipe data model.
 *
 * 閰嶆柟鏁版嵁妯″瀷銆?
 * <p>
 * 鍖呭惈瑙ｆ瀽鍚庣殑閰嶆柟杈撳叆鏉愭枡鍒楄〃鍜屼骇鍑虹墿鍒楄〃銆?
 * 鎵€鏈夊垪琛ㄥ潎涓轰笉鍙慨鏀圭殑鍙瑙嗗浘銆?
 * </p>
 */
public class RecipeData {
    /**
     * Recipe input material list (unmodifiable)
     */
    /** 閰嶆柟杈撳叆鏉愭枡鍒楄〃锛堜笉鍙慨鏀癸級 */
    private final List<Ingredient> inputs;
    /**
     * Recipe output product list (unmodifiable)
     */
    /** 閰嶆柟浜у嚭鐗╁垪琛紙涓嶅彲淇敼锛?*/
    private final List<Product> outputs;

    /**
 * Constructs a recipe data instance.
 *
 * 鏋勯€犻厤鏂规暟鎹疄渚嬨€?
 *
 * @param inputs the input material list
 * @param outputs the output product list
 * @param inputs  杈撳叆鏉愭枡鍒楄〃
 * @param outputs 浜у嚭鐗╁垪琛?
 */
    public RecipeData(List<Ingredient> inputs, List<Product> outputs) {
        this.inputs = Collections.unmodifiableList(inputs);
        this.outputs = Collections.unmodifiableList(outputs);
    }

    /**
 *
 * @return the input material list (unmodifiable)
 * @return 杈撳叆鏉愭枡鍒楄〃锛堜笉鍙慨鏀癸級
 */
    public List<Ingredient> getInputs() { return inputs; }
    /**
 *
 * @return the output product list (unmodifiable)
 * @return 浜у嚭鐗╁垪琛紙涓嶅彲淇敼锛?
 */
    public List<Product> getOutputs() { return outputs; }
}