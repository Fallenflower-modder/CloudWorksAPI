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

import com.cloudworks.api.command.DebugCommand;
import com.cloudworks.api.recipeparser.ApiSelfTest;
import com.cloudworks.api.recipeparser.RecipeParser;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CloudWorks API main mod class.
 *
 * CloudWorks API 妯＄粍涓荤被銆?
 * <p>
 * 浣滀负 NeoForge 妯＄粍鍏ュ彛鐐癸紝璐熻矗鍒濆鍖?RecipeParser 妯″潡銆?
 * 娉ㄥ唽浜嬩欢鐩戝惉鍣ㄥ拰璋冭瘯鍛戒护銆?
 * 閰嶆柟瑙ｆ瀽鍔熻兘閫氳繃 {@link com.cloudworks.api.annotation.CloudworksRecipeParser} 娉ㄨВ鍚敤銆?
 * </p>
 */
@Mod(CloudWorksAPI.MOD_ID)
public class CloudWorksAPI {

    /**
 * 妯＄粍ID
 *
 * 妯＄粍ID
 */
    public static final String MOD_ID = "cloudworks_api";
    /**
 * 妯＄粍鏃ュ織璁板綍鍣?
 *
 * 妯＄粍鏃ュ織璁板綍鍣?
 */
    public static final Logger LOGGER = LoggerFactory.getLogger(CloudWorksAPI.class);

    /**
 * Mod constructor, called by the NeoForge framework.
 *
 * 妯＄粍鏋勯€犲嚱鏁帮紝鐢?NeoForge 妗嗘灦璋冪敤銆?
 * <p>
 * 娉ㄥ唽 CommonSetup 浜嬩欢鍜?RegisterCommands 浜嬩欢鐨勭洃鍚櫒銆?
 * </p>
 *
 * @param modEventBus the mod event bus
 * @param modEventBus 妯＄粍浜嬩欢鎬荤嚎
 */
    public CloudWorksAPI(IEventBus modEventBus) {
        LOGGER.info("CloudWorks API initializing...");
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    /**
 * Common setup callback, initializes RecipeParser and ApiSelfTest.
 *
 * 閫氱敤璁剧疆鍥炶皟锛屽垵濮嬪寲 RecipeParser 鍜?ApiSelfTest銆?
 *
 * @param event the common setup event
 * @param event 閫氱敤璁剧疆浜嬩欢
 */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("CloudWorks API - Initializing modules...");
        RecipeParser.getInstance().initialize();
        NeoForge.EVENT_BUS.register(RecipeParser.getInstance());
        NeoForge.EVENT_BUS.register(new ApiSelfTest());
    }

    /**
 * Register commands callback, registers debug commands.
 *
 * 娉ㄥ唽鍛戒护鍥炶皟锛屾敞鍐岃皟璇曞懡浠ゃ€?
 *
 * @param event the register commands event
 * @param event 娉ㄥ唽鍛戒护浜嬩欢
 */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("CloudWorks API - Registering debug commands...");
        DebugCommand.register(event.getDispatcher());
    }
}