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
package com.cloudworks.api;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CloudWorks API mod configuration.
 *
 * CloudWorks API 模组配置。
 * <p>
 * 配置文件位于 {@code config/cloudworks_api-common.toml}。
 * </p>
 */
public final class CloudWorksConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * Whether to log a warning for every recipe that has no matching template.
     *
     * 是否为每条匹配不到模板的配方单独输出警告。
     * <p>
     * 默认 {@code false}：只在配方遍历结束后输出一条汇总统计。
     * 设为 {@code true} 时逐条输出警告，便于定位缺失的模板类型。
     * </p>
     */
    public static final ModConfigSpec.BooleanValue DEBUG_RECIPE = BUILDER
            .comment("Log a warning for every recipe that has no matching template.",
                    "false (default): only log a summary count after recipe iteration.",
                    "true: log one warning per unmatched recipe.",
                    "是否为每条匹配不到模板的配方单独输出警告。默认只输出汇总统计。")
            .define("debug_recipe", false);

    /**
     * Built configuration spec.
     */
    /** 构建完成的配置规范 */
    public static final ModConfigSpec SPEC = BUILDER.build();

    private CloudWorksConfig() {
    }

    /**
     * Returns whether per-recipe debug logging is enabled.
     *
     * 返回是否启用逐条配方调试日志。
     *
     * @return true if debug_recipe is enabled
     * @return 若 debug_recipe 为 true 则返回 true
     */
    public static boolean isDebugRecipe() {
        try {
            return DEBUG_RECIPE.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet
            return false;
        }
    }
}
