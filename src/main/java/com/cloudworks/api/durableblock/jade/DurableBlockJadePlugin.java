/*
 * CloudWorks API - DurableBlock Module - Jade Compat
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.cloudworks.api.durableblock.jade;

import com.cloudworks.api.durableblock.DurableBlock;
import com.cloudworks.api.durableblock.DurableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 适配插件：为 {@link DurableBlock} 提供耐久信息显示。
 *
 * <p>在 Jade 的方块名下一行显示"耐久：当前值 / 最大值"，实际耐久保留 2 位小数。</p>
 *
 * <p>由于耐久方块实体采用服务端仅存方案（{@code clientTrackingRange(0)}），
 * 客户端无法直接获取耐久数据。因此使用 {@link IServerDataProvider} 在服务端
 * 收集耐久数据并通过 Jade 的数据包同步到客户端，再由 {@link IBlockComponentProvider}
 * 在客户端渲染显示。</p>
 */
@WailaPlugin
public class DurableBlockJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("cloudworks_api", "durable_block_durability");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DurableBlockDataProvider.INSTANCE, DurableBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DurableBlockComponentProvider.INSTANCE, DurableBlock.class);
    }

    /**
     * 服务端数据提供者：在服务端查找耐久方块实体，将耐久数据写入 NBT 同步到客户端。
     */
    public enum DurableBlockDataProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            Level level = accessor.getLevel();
            if (level.isClientSide()) return;

            BlockPos pos = accessor.getPosition();

            // 在服务端查找绑定在该方块位置的 DurableBlockEntity
            // 实体始终位于方块中心，使用 blockPosition() 即可匹配
            // 构建覆盖整个方块的 AABB，确保能捕获到中心位置的实体
            AABB aabb = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
            level.getEntitiesOfClass(DurableBlockEntity.class, aabb, entity ->
                    entity.blockPosition().equals(pos)
            ).stream().findFirst().ifPresent(entity -> {
                data.putFloat("durability", entity.getCurrentDurability());
                data.putFloat("maxDurability", entity.getMaxDurability());
            });
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    /**
     * 客户端组件提供者：从服务端同步的数据中读取耐久值并显示在 Jade 工具提示中。
     */
    public enum DurableBlockComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag serverData = accessor.getServerData();
            if (serverData.contains("durability")) {
                float durability = serverData.getFloat("durability");
                float maxDurability = serverData.getFloat("maxDurability");
                tooltip.add(Component.translatable("jade.cloudworks_api.durability",
                        String.format("%.2f", durability),
                        String.format("%.2f", maxDurability)));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}