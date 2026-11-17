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
 * Template global settings.
 *
 * 妯℃澘鍏ㄥ眬璁剧疆銆?
 * <p>
 * 閫氳繃 .rpml 鏂囦欢涓殑 &lt;script&gt; 鏍囪閰嶇疆锛屾帶鍒舵祦浣撳埌鐗╁搧杞崲鐨勫叏灞€琛屼负銆?
 * 鍖呮嫭鏄惁鍚敤鍏ㄥ眬娴佷綋杞崲銆侀粯璁よ浆鎹㈢巼銆侀粯璁よ浆鎹㈢粨鏋溿€?
 * 棰濆杈撳叆闇€姹傘€佹诞鐐瑰彇鏁存ā寮忎互鍙婃槸鍚﹀惎鐢ㄦā鏉块厤缃€?
 * </p>
 */
public class GlobalSettings {

    /**
     * Floating-point rounding mode enum.
     */
    /** 娴偣鏁板彇鏁存ā寮忔灇涓俱€?*/
    public enum FloatRound {
        /**
     * Round up
     */
    /** 鍚戜笂鍙栨暣 */
        ROUND_UP,
        /**
     * Round down
     */
    /** 鍚戜笅鍙栨暣 */
        ROUND_DOWN,
        /**
     * Default rounding (half-up)
     */
    /** 榛樿鍥涜垗浜斿叆 */
        DEFAULT
    }

    /**
     * Whether to enable global fluid transfer, default false
     */
    /** 鏄惁鍚敤鍏ㄥ眬娴佷綋杞崲锛岄粯璁?false */
    private boolean globalFluidTransfer = false;
    /**
     * Global default transfer rate, default 100.0
     */
    /** 鍏ㄥ眬榛樿杞崲鐜囷紝榛樿 100.0 */
    private double globalDefaultTransferRate = 100.0;
    /**
     * Global default transfer result item ID, null means same as fluid ID
     */
    /** 鍏ㄥ眬榛樿杞崲缁撴灉鐗╁搧ID锛宯ull 琛ㄧず涓庢祦浣揑D鐩稿悓 */
    private String globalDefaultTransferResult = null;
    /**
     * Global default transfer extra input mapping
     */
    /** 鍏ㄥ眬榛樿杞崲棰濆杈撳叆鏄犲皠 */
    private final Map<String, Double> globalDefaultTransferExtraInput = new LinkedHashMap<>();
    /**
     * Global default transfer floating-point rounding mode
     */
    /** 鍏ㄥ眬榛樿杞崲娴偣鍙栨暣妯″紡 */
    private FloatRound globalDefaultTransferFloatRound = FloatRound.DEFAULT;
    /**
     * Whether to enable template config, default true
     */
    /** 鏄惁鍚敤妯℃澘閰嶇疆锛岄粯璁?true */
    private boolean globalEnableTemplateConfig = true;

    /**
 *
 * @return whether global fluid transfer is enabled
 * @return 鏄惁鍚敤鍏ㄥ眬娴佷綋杞崲
 */
    public boolean isGlobalFluidTransfer() { return globalFluidTransfer; }
    /**
 *
 * @param v whether to enable global fluid transfer
 * @param v 鏄惁鍚敤鍏ㄥ眬娴佷綋杞崲
 */
    public void setGlobalFluidTransfer(boolean v) { this.globalFluidTransfer = v; }

    /**
 *
 * @return the global default transfer rate
 * @return 鍏ㄥ眬榛樿杞崲鐜?
 */
    public double getGlobalDefaultTransferRate() { return globalDefaultTransferRate; }
    /**
 *
 * @param v the global default transfer rate
 * @param v 鍏ㄥ眬榛樿杞崲鐜?
 */
    public void setGlobalDefaultTransferRate(double v) { this.globalDefaultTransferRate = v; }

    /**
 *
 * @return the global default transfer result item ID
 * @return 鍏ㄥ眬榛樿杞崲缁撴灉鐗╁搧ID
 */
    public String getGlobalDefaultTransferResult() { return globalDefaultTransferResult; }
    /**
 *
 * @param v the global default transfer result item ID
 * @param v 鍏ㄥ眬榛樿杞崲缁撴灉鐗╁搧ID
 */
    public void setGlobalDefaultTransferResult(String v) { this.globalDefaultTransferResult = v; }

    /**
 *
 * @return the global default transfer extra input mapping
 * @return 鍏ㄥ眬榛樿杞崲棰濆杈撳叆鏄犲皠
 */
    public Map<String, Double> getGlobalDefaultTransferExtraInput() { return globalDefaultTransferExtraInput; }
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
    public void addGlobalDefaultTransferExtraInput(String id, double count) { globalDefaultTransferExtraInput.put(id, count); }

    /**
 *
 * @return the global default transfer floating-point rounding mode
 * @return 鍏ㄥ眬榛樿杞崲娴偣鍙栨暣妯″紡
 */
    public FloatRound getGlobalDefaultTransferFloatRound() { return globalDefaultTransferFloatRound; }
    /**
 *
 * @param v the floating-point rounding mode
 * @param v 娴偣鍙栨暣妯″紡
 */
    public void setGlobalDefaultTransferFloatRound(FloatRound v) { this.globalDefaultTransferFloatRound = v; }

    /**
 *
 * @return whether template config is enabled
 * @return 鏄惁鍚敤妯℃澘閰嶇疆
 */
    public boolean isGlobalEnableTemplateConfig() { return globalEnableTemplateConfig; }
    /**
 *
 * @param v whether to enable template config
 * @param v 鏄惁鍚敤妯℃澘閰嶇疆
 */
    public void setGlobalEnableTemplateConfig(boolean v) { this.globalEnableTemplateConfig = v; }

    /**
 * Applies a script parameter starting with "set_global_".
 *
 * 搴旂敤涓€涓互 "set_global_" 寮€澶寸殑鑴氭湰鍙傛暟銆?
 * <p>
 * 鏀寔鐨勮缃」鍖呮嫭锛?
 * <ul>
 *   <li>fluid_transfer - 鏄惁鍚敤娴佷綋杞崲</li>
 *   <li>default_transfer_rate - 榛樿杞崲鐜?/li>
 *   <li>default_transfer_result - 榛樿杞崲缁撴灉</li>
 *   <li>default_transfer_extra_input - 棰濆杈撳叆</li>
 *   <li>default_transfer_float_round - 娴偣鍙栨暣妯″紡</li>
 *   <li>enable_template_config - 鏄惁鍚敤妯℃澘閰嶇疆</li>
 * </ul>
 * </p>
 *
 * @param key the script parameter key name
 * @param value the script parameter value
 * @param key   鑴氭湰鍙傛暟閿悕
 * @param value 鑴氭湰鍙傛暟鍊?
 */
    public void applyScriptParam(String key, String value) {
        if (!key.startsWith("set_global_")) return;
        String setting = key.substring("set_global_".length());
        switch (setting) {
            case "fluid_transfer":
                globalFluidTransfer = Boolean.parseBoolean(value);
                break;
            case "default_transfer_rate":
                try { globalDefaultTransferRate = Double.parseDouble(value); } catch (NumberFormatException ignored) {}
                break;
            case "default_transfer_result":
                globalDefaultTransferResult = value.isEmpty() ? null : value;
                break;
            case "default_transfer_extra_input": {
                // Value format: "key1=1.0,key2=2.0" or empty array "[]"
                globalDefaultTransferExtraInput.clear();
                if (value != null && !value.isEmpty() && !"[]".equals(value)) {
                    for (String part : value.split(",")) {
                        String[] kv = part.split("=", 2);
                        if (kv.length == 2) {
                            try {
                                globalDefaultTransferExtraInput.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                break;
            }
            case "default_transfer_float_round":
                try {
                    globalDefaultTransferFloatRound = FloatRound.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    globalDefaultTransferFloatRound = FloatRound.DEFAULT;
                }
                break;
            case "enable_template_config":
                globalEnableTemplateConfig = Boolean.parseBoolean(value);
                break;
        }
    }

    /**
 * Parses a rounding mode string to an enum value.
 *
 * 瑙ｆ瀽鍙栨暣妯″紡瀛楃涓蹭负鏋氫妇鍊笺€?
 *
 * @param s the rounding mode string ("round_up", "round_down", or "default")
 * @param s 鍙栨暣妯″紡瀛楃涓诧紙"round_up"銆?round_down" 鎴?"default"锛?
 * @return the corresponding FloatRound enum value, or DEFAULT on parse failure
 * @return 瀵瑰簲鐨?FloatRound 鏋氫妇鍊硷紝瑙ｆ瀽澶辫触鏃惰繑鍥?DEFAULT
 */
    public static FloatRound parseRound(String s) {
        if (s == null) return FloatRound.DEFAULT;
        try {
            return FloatRound.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FloatRound.DEFAULT;
        }
    }

    /**
 * Rounds a floating-point number according to the rounding mode.
 *
 * 鏍规嵁鍙栨暣妯″紡瀵规诞鐐规暟杩涜鍙栨暣銆?
 *
 * @param value the floating-point number to round
 * @param round the rounding mode
 * @param value 寰呭彇鏁寸殑娴偣鏁?
 * @param round 鍙栨暣妯″紡
 * @return the rounded integer result
 * @return 鍙栨暣鍚庣殑鏁存暟缁撴灉
 */
    public static int applyRound(double value, FloatRound round) {
        switch (round) {
            case ROUND_UP: return (int) Math.ceil(value);
            case ROUND_DOWN: return (int) Math.floor(value);
            case DEFAULT:
            default: return (int) Math.round(value);
        }
    }
}