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
 * Parameter operation definition.
 *
 * 鍙傛暟鎿嶄綔瀹氫箟銆?
 * <p>
 * 琛ㄧず妯℃澘鏍囪涓殑涓€涓弬鏁版搷浣滐紝渚嬪 "default_count=2" 鎴?
 * "set_input1_count=5 if condition for target"銆?
 * 鏀寔鏉′欢鍒ゆ柇锛坕f锛夊拰鎸囧畾鐩爣锛坒or锛夊瓙鍙ャ€?
 * </p>
 */
public class ParameterOp {
    /**
     * Parameter key name, e.g., "default_count", "set_xxx_count", etc.
     */
    /** 鍙傛暟閿悕锛屽 "default_count"銆?set_xxx_count" 绛?*/
    private final String key;
    /**
     * Parameter value
     */
    /** 鍙傛暟鍊?*/
    private final String value;
    /**
     * Condition clause, may be null
     */
    /** 鏉′欢瀛愬彞锛屽彲鑳戒负 null */
    private final String condition;
    /**
     * For-target clause, may be null
     */
    /** 鎸囧畾鐩爣瀛愬彞锛屽彲鑳戒负 null */
    private final String forTarget;

    /**
 * Constructs a parameter operation instance.
 *
 * 鏋勯€犱竴涓弬鏁版搷浣滃疄渚嬨€?
 *
 * @param key the parameter key name
 * @param value the parameter value
 * @param condition the condition clause, nullable
 * @param forTarget the for-target clause, nullable
 * @param key       鍙傛暟閿悕
 * @param value     鍙傛暟鍊?
 * @param condition 鏉′欢瀛愬彞锛屽彲涓?null
 * @param forTarget 鎸囧畾鐩爣瀛愬彞锛屽彲涓?null
 */
    public ParameterOp(String key, String value, String condition, String forTarget) {
        this.key = key;
        this.value = value;
        this.condition = condition;
        this.forTarget = forTarget;
    }

    /**
 *
 * @return the parameter key name
 * @return 鍙傛暟閿悕
 */
    public String getKey() { return key; }
    /**
 *
 * @return the parameter value
 * @return 鍙傛暟鍊?
 */
    public String getValue() { return value; }
    /**
 *
 * @return the condition clause, may be null
 * @return 鏉′欢瀛愬彞锛屽彲鑳戒负 null
 */
    public String getCondition() { return condition; }
    /**
 *
 * @return the for-target clause, may be null
 * @return 鎸囧畾鐩爣瀛愬彞锛屽彲鑳戒负 null
 */
    public String getForTarget() { return forTarget; }

    /**
 *
 * @return whether it has a condition clause
 * @return 鏄惁鍖呭惈鏉′欢瀛愬彞
 */
    public boolean hasCondition() { return condition != null; }
    /**
 *
 * @return whether it has a for-target clause
 * @return 鏄惁鍖呭惈鎸囧畾鐩爣瀛愬彞
 */
    public boolean hasForTarget() { return forTarget != null; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append("=").append(value);
        if (condition != null) sb.append(" if ").append(condition);
        if (forTarget != null) sb.append(" for ").append(forTarget);
        return sb.toString();
    }
}