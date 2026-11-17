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
 * RPML template model.
 *
 * RPML 妯℃澘妯″瀷銆?
 * <p>
 * 琛ㄧず涓€涓畬鏁寸殑 RPML 妯℃澘锛屽寘鍚墍灞炴ā缁処D銆侀厤鏂圭被鍨嬨€?
 * 瑙ｆ瀽鍚庣殑璇硶鏍戞牴鑺傜偣浠ュ強鍏ㄥ眬璁剧疆銆?
 * </p>
 */
public class Template {
    /**
     * Mod ID, e.g., "minecraft"
     */
    /** 妯＄粍ID锛屽 "minecraft" */
    private final String modId;
    /**
     * Recipe type, e.g., "crafting_shaped"
     */
    /** 閰嶆柟绫诲瀷锛屽 "crafting_shaped" */
    private final String recipeType;
    /**
     * Template AST root node
     */
    /** 妯℃澘璇硶鏍戞牴鑺傜偣 */
    private final TemplateNode root;
    /**
     * Global settings
     */
    /** 鍏ㄥ眬璁剧疆 */
    private final GlobalSettings globalSettings;

    /**
 * Constructs a template instance (using default global settings).
 *
 * 鏋勯€犳ā鏉垮疄渚嬶紙浣跨敤榛樿鍏ㄥ眬璁剧疆锛夈€?
 *
 * @param modId the mod ID
 * @param recipeType the recipe type
 * @param root the AST root node
 * @param modId      妯＄粍ID
 * @param recipeType 閰嶆柟绫诲瀷
 * @param root       璇硶鏍戞牴鑺傜偣
 */
    public Template(String modId, String recipeType, TemplateNode root) {
        this(modId, recipeType, root, new GlobalSettings());
    }

    /**
 * Constructs a template instance (with specified global settings).
 *
 * 鏋勯€犳ā鏉垮疄渚嬶紙鎸囧畾鍏ㄥ眬璁剧疆锛夈€?
 *
 * @param modId the mod ID
 * @param recipeType the recipe type
 * @param root the AST root node
 * @param globalSettings the global settings
 * @param modId          妯＄粍ID
 * @param recipeType     閰嶆柟绫诲瀷
 * @param root           璇硶鏍戞牴鑺傜偣
 * @param globalSettings 鍏ㄥ眬璁剧疆
 */
    public Template(String modId, String recipeType, TemplateNode root, GlobalSettings globalSettings) {
        this.modId = modId;
        this.recipeType = recipeType;
        this.root = root;
        this.globalSettings = globalSettings;
    }

    /**
 *
 * @return the mod ID
 * @return 妯＄粍ID
 */
    public String getModId() { return modId; }
    /**
 *
 * @return the recipe type
 * @return 閰嶆柟绫诲瀷
 */
    public String getRecipeType() { return recipeType; }
    /**
 *
 * @return the AST root node
 * @return 璇硶鏍戞牴鑺傜偣
 */
    public TemplateNode getRoot() { return root; }
    /**
 *
 * @return the global settings
 * @return 鍏ㄥ眬璁剧疆
 */
    public GlobalSettings getGlobalSettings() { return globalSettings; }

    @Override
    public String toString() {
        return "Template[" + modId + "_" + recipeType + "]";
    }
}