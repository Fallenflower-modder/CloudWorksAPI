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

import com.cloudworks.api.annotation.CloudWorksConsoleSeeker;
import com.cloudworks.api.command.DebugCommand;
import com.cloudworks.api.consoleseeker.ChatAppender;
import com.cloudworks.api.consoleseeker.ConsoleSeekerConfig;
import com.cloudworks.api.consoleseeker.ConsoleSeekerEventManager;
import com.cloudworks.api.durableblock.DurableBlock;
import com.cloudworks.api.durableblock.DurableBlockEntity;
import com.cloudworks.api.durableblock.PersistentDurableBlock;
import com.cloudworks.api.durableblock.PersistentDurableBlockEntity;
import com.cloudworks.api.recipeparser.ApiSelfTest;
import com.cloudworks.api.recipeparser.RecipeParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * CloudWorks API main mod class.
 *
 * CloudWorks API 模组主类。
 * <p>
 * 作为 NeoForge 模组入口点，负责初始化 RecipeParser 模块、
 * 注册事件监听器和调试命令。
 * </p>
 */
@Mod(CloudWorksAPI.MOD_ID)
public class CloudWorksAPI {

    public static final String MOD_ID = "cloudworks_api";
    public static final Logger LOGGER = LoggerFactory.getLogger(CloudWorksAPI.class);

    // ======================== DurableBlock 实体类型注册 ========================

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MOD_ID);

    public static final Supplier<EntityType<DurableBlockEntity>> DURABLE_BLOCK_ENTITY_TYPE =
            ENTITY_TYPES.register("durable_block",
                    () -> EntityType.Builder.<DurableBlockEntity>of(DurableBlockEntity::new, MobCategory.MISC)
                            .sized(1.02f, 1.02f)
                            .clientTrackingRange(0)
                            .build("durable_block"));

    public static final Supplier<EntityType<PersistentDurableBlockEntity>> PERSISTENT_DURABLE_BLOCK_ENTITY_TYPE =
            ENTITY_TYPES.register("persistent_durable_block",
                    () -> EntityType.Builder.<PersistentDurableBlockEntity>of(PersistentDurableBlockEntity::new, MobCategory.MISC)
                            .sized(1.02f, 1.02f)
                            .clientTrackingRange(0)
                            .build("persistent_durable_block"));

    // ======================== 方块和物品注册 ========================

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, MOD_ID);

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, MOD_ID);

    /**
     * 测试用 DurableBlock，属性与橡木原木一致（爆炸抗性 2100）。
     * <ul>
     *   <li>最大耐久：1024</li>
     *   <li>基础抗性：3</li>
     *   <li>全伤害类型减免：90%</li>
     * </ul>
     */
    public static final Supplier<Block> TEST_DURABLE_BLOCK =
            BLOCKS.register("test_durable_block",
                    () -> new DurableBlock(BlockBehaviour.Properties.of()
                            .strength(2.0f, 2100.0f)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()
                            .instrument(NoteBlockInstrument.BASS),
                            64,     // maxBlockDurability
                            3,      // baseResistance
                            0.9f,   // explosionResistance
                            0.9f,   // physicalResistance
                            0.9f,   // magicResistance
                            0       // recoveryRate
                    ));

    public static final Supplier<Item> TEST_DURABLE_BLOCK_ITEM =
            ITEMS.register("test_durable_block",
                    () -> new BlockItem(TEST_DURABLE_BLOCK.get(), new Item.Properties()));

    /**
     * 测试用 PersistentDurableBlock，属性与橡木原木一致（爆炸抗性 2100），
     * 具有代码博弈保护，能够抵抗即死类攻击。
     * <ul>
     *   <li>最大耐久：64</li>
     *   <li>基础抗性：3</li>
     *   <li>全伤害类型减免：90%</li>
     * </ul>
     */
    public static final Supplier<Block> TEST_PERSISTENT_DURABLE_BLOCK =
            BLOCKS.register("test_persistent_durable_block",
                    () -> new PersistentDurableBlock(BlockBehaviour.Properties.of()
                            .strength(2.0f, 2100.0f)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()
                            .instrument(NoteBlockInstrument.BASS),
                            64,     // maxBlockDurability
                            3,      // baseResistance
                            0.9f,   // explosionResistance
                            0.9f,   // physicalResistance
                            0.9f,   // magicResistance
                            0       // recoveryRate
                    ));

    public static final Supplier<Item> TEST_PERSISTENT_DURABLE_BLOCK_ITEM =
            ITEMS.register("test_persistent_durable_block",
                    () -> new BlockItem(TEST_PERSISTENT_DURABLE_BLOCK.get(), new Item.Properties()));

    // ======================== 实例字段 ========================

    // ConsoleSeeker 启用标志（构造函数中确定，onCommonSetup 中使用）
    private boolean enableConsoleSeeker = false;

    /**
     * Mod constructor, called by the NeoForge framework.
     *
     * @param modEventBus the mod event bus
     */
    public CloudWorksAPI(IEventBus modEventBus) {
        LOGGER.info("CloudWorks API initializing...");

        // 注册 DurableBlock 实体类型
        ENTITY_TYPES.register(modEventBus);
        DurableBlockEntity.ENTITY_TYPE = DURABLE_BLOCK_ENTITY_TYPE;
        PersistentDurableBlockEntity.ENTITY_TYPE = PERSISTENT_DURABLE_BLOCK_ENTITY_TYPE;
        modEventBus.addListener(this::onEntityAttributeCreation);
        LOGGER.info("CloudWorks API - DurableBlock entity types registered.");

        // 注册方块和物品
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        LOGGER.info("CloudWorks API - Blocks and items registered.");

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onCreativeModeTabBuild);
        NeoForge.EVENT_BUS.register(this);

        // 加载 ConsoleSeeker 配置
        ConsoleSeekerConfig.load();

        // 检测 @CloudWorksConsoleSeeker 注解
        boolean annotationFound = isConsoleSeekerAnnotationPresent();

        // 决定 ConsoleSeeker 启用状态
        boolean enableApi;

        if (annotationFound) {
            enableConsoleSeeker = true;
            enableApi = true;
            LOGGER.info("CloudWorks API - @CloudWorksConsoleSeeker annotation detected. "
                    + "ConsoleSeeker and API are force-enabled.");
        } else if (!ConsoleSeekerConfig.isEnableModule()) {
            enableConsoleSeeker = false;
            enableApi = false;
            LOGGER.info("CloudWorks API - ConsoleSeeker module is disabled via config.");
        } else {
            enableConsoleSeeker = true;
            enableApi = ConsoleSeekerConfig.isEnableApi();
            LOGGER.info("CloudWorks API - ConsoleSeeker basic enabled. API: {}", enableApi ? "enabled" : "disabled");
        }

        ConsoleSeekerEventManager.setApiEnabled(enableApi);
    }

    /**
     * 注册 DurableBlockEntity 的属性。
     */
    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(DURABLE_BLOCK_ENTITY_TYPE.get(), DurableBlockEntity.createAttributes().build());
        event.put(PERSISTENT_DURABLE_BLOCK_ENTITY_TYPE.get(), DurableBlockEntity.createAttributes().build());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("CloudWorks API - Initializing modules...");

        if (enableConsoleSeeker) {
            setupChatAppender();
        }

        try {
            RecipeParser.getInstance().initialize();
            NeoForge.EVENT_BUS.register(RecipeParser.getInstance());
            NeoForge.EVENT_BUS.register(new ApiSelfTest());
            LOGGER.info("CloudWorks API - RecipeParser module initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("CloudWorks API - RecipeParser initialization failed: {}", e.getMessage(), e);
            LOGGER.warn("CloudWorks API - Debug commands will be available but RecipeParser features will be disabled.");
        }
    }

    /**
     * 将测试方块添加到创造模式物品栏。
     */
    private void onCreativeModeTabBuild(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(TEST_DURABLE_BLOCK_ITEM.get());
            event.accept(TEST_PERSISTENT_DURABLE_BLOCK_ITEM.get());
        }
    }

    @SubscribeEvent
    private void onRegisterCommands(RegisterCommandsEvent event) {
        try {
            LOGGER.info("CloudWorks API - Registering debug commands...");
            DebugCommand.register(event.getDispatcher());
            LOGGER.info("CloudWorks API - Debug commands registered successfully.");
        } catch (Exception e) {
            LOGGER.error("CloudWorks API - Failed to register debug commands: {}", e.getMessage(), e);
        }
    }

    private void setupChatAppender() {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();

            if (config.getAppender("ChatAppender") != null) {
                LOGGER.info("CloudWorks API - ChatAppender already exists, skipping setup.");
                return;
            }

            ChatAppender appender = ChatAppender.createAppender("ChatAppender", null, null);

            if (appender != null) {
                appender.start();
                config.addAppender(appender);
                config.getRootLogger().addAppender(appender, null, null);
                ctx.updateLoggers(config);
                LOGGER.info("CloudWorks API - ChatAppender registered successfully.");
            }
        } catch (Exception e) {
            LOGGER.warn("CloudWorks API - Failed to setup ChatAppender: {}", e.getMessage());
        }
    }

    private static boolean isConsoleSeekerAnnotationPresent() {
        Type annotationType = Type.getType(CloudWorksConsoleSeeker.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (annotation.annotationType().equals(annotationType)) {
                    return true;
                }
            }
        }
        return false;
    }
}