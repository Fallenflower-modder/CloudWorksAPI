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

import java.util.*;

/**
 * Configuration for a single recipe, loaded from templates_config/*.json files.
 *
 * 鍗曚釜閰嶆柟鐨勯厤缃紝浠?templates_config/*.json 鏂囦欢涓姞杞姐€?
 * <p>
 * 鍖呭惈娴佷綋鍒扮墿鍝佽浆鎹㈢殑閰嶇疆淇℃伅锛屽鏄惁鍚敤杞崲銆佽浆鎹㈤粦鍚嶅崟銆?
 * 浠ュ強鍏蜂綋鐨勮浆鎹㈡柟娉曞垪琛ㄣ€?
 * </p>
 */
public class TemplateConfig {

    /**
 * Fluid-to-item transfer method definition.
 *
 * 娴佷綋鍒扮墿鍝佺殑杞崲鏂规硶瀹氫箟銆?
 * <p>
 * 鍖呭惈杞崲鐜囥€佹诞鐐瑰彇鏁存ā寮忋€侀澶栬緭鍏ラ渶姹備互鍙婅浆鎹㈢粨鏋滄槧灏勩€?
 * </p>
 */
    public static class TransferMethod {
        /**
     * Transfer rate, default 100.0
     */
    /** 杞崲鐜囷紝榛樿 100.0 */
        private double rate = 100.0;
        /**
     * Floating-point rounding mode, default DEFAULT
     */
    /** 娴偣鍙栨暣妯″紡锛岄粯璁?DEFAULT */
        private GlobalSettings.FloatRound round = GlobalSettings.FloatRound.DEFAULT;
        /**
     * Extra input requirements (item ID -> count)
     */
    /** 棰濆杈撳叆闇€姹傦紙鐗╁搧ID -> 鏁伴噺锛?*/
        private final Map<String, Double> extraInput = new LinkedHashMap<>();
        /**
     * Transfer result mapping (item ID -> count)
     */
    /** 杞崲缁撴灉鏄犲皠锛堢墿鍝両D -> 鏁伴噺锛?*/
        private final Map<String, Integer> result = new LinkedHashMap<>();

        /**
 *
 * @return the transfer rate
 * @return 杞崲鐜?
 */
        public double getRate() { return rate; }
        /**
 *
 * @param v the transfer rate
 * @param v 杞崲鐜?
 */
        public void setRate(double v) { this.rate = v; }

        /**
 *
 * @return the floating-point rounding mode
 * @return 娴偣鍙栨暣妯″紡
 */
        public GlobalSettings.FloatRound getRound() { return round; }
        /**
 *
 * @param v the floating-point rounding mode
 * @param v 娴偣鍙栨暣妯″紡
 */
        public void setRound(GlobalSettings.FloatRound v) { this.round = v; }

        /**
 *
 * @return the extra input requirements mapping
 * @return 棰濆杈撳叆闇€姹傛槧灏?
 */
        public Map<String, Double> getExtraInput() { return extraInput; }
        /**
 *
 * @param id the item ID
 * @param id    鐗╁搧ID
 */
        /**
 *
 * @param count the count
 * @param count 鏁伴噺
 */
        public void addExtraInput(String id, double count) { extraInput.put(id, count); }

        /**
 *
 * @return the transfer result mapping
 * @return 杞崲缁撴灉鏄犲皠
 */
        public Map<String, Integer> getResult() { return result; }
        /**
 *
 * @param id the item ID
 * @param id    鐗╁搧ID
 */
        /**
 *
 * @param count the count
 * @param count 鏁伴噺
 */
        public void addResult(String id, int count) { result.put(id, count); }
    }

    /**
     * Recipe ID
     */
    /** 閰嶆柟ID */
    private final String recipeId;
    /**
     * Whether to enable fluid-to-item transfer, default true
     */
    /** 鏄惁鍚敤娴佷綋鍒扮墿鍝佽浆鎹紝榛樿 true */
    private boolean enableTransfer = true;
    /**
     * Transfer blacklist (list of fluid IDs)
     */
    /** 杞崲榛戝悕鍗曪紙娴佷綋ID鍒楄〃锛?*/
    private final List<String> transferBlacklist = new ArrayList<>();
    /**
     * Transfer method list
     */
    /** 杞崲鏂规硶鍒楄〃 */
    private final List<TransferMethod> methods = new ArrayList<>();

    /**
 * Constructs a recipe config instance.
 *
 * 鏋勯€犻厤鏂归厤缃疄渚嬨€?
 *
 * @param recipeId the recipe ID
 * @param recipeId 閰嶆柟ID
 */
    public TemplateConfig(String recipeId) {
        this.recipeId = recipeId;
    }

    /**
 *
 * @return the recipe ID
 * @return 閰嶆柟ID
 */
    public String getRecipeId() { return recipeId; }

    /**
 *
 * @return whether transfer is enabled
 * @return 鏄惁鍚敤杞崲
 */
    public boolean isEnableTransfer() { return enableTransfer; }
    /**
 *
 * @param v whether to enable transfer
 * @param v 鏄惁鍚敤杞崲
 */
    public void setEnableTransfer(boolean v) { this.enableTransfer = v; }

    /**
 *
 * @return the transfer blacklist
 * @return 杞崲榛戝悕鍗?
 */
    public List<String> getTransferBlacklist() { return transferBlacklist; }
    /**
 *
 * @param id the fluid ID to add to the blacklist
 * @param id 瑕佸姞鍏ラ粦鍚嶅崟鐨勬祦浣揑D
 */
    public void addTransferBlacklist(String id) { transferBlacklist.add(id); }

    /**
 *
 * @return the transfer method list
 * @return 杞崲鏂规硶鍒楄〃
 */
    public List<TransferMethod> getMethods() { return methods; }
    /**
 *
 * @param m the transfer method
 * @param m 杞崲鏂规硶
 */
    public void addMethod(TransferMethod m) { methods.add(m); }
}