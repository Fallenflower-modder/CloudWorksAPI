/*
 * CloudWorks API - DurableBlock Module
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.cloudworks.api.durableblock;

import net.minecraft.world.entity.EntityType;

/**
 * 持久化耐久方块——使用 {@link PersistentDurableBlockEntity} 进行代码博弈保护。
 *
 * <p>与普通 {@link DurableBlock} 的唯一区别在于，其关联的实体类型为
 * {@link PersistentDurableBlockEntity}，能够拦截即死类攻击（如寰宇支配之剑），
 * 将每次即死攻击转换为 20% 耐久扣减，而非直接移除实体。</p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * Block myBlock = new PersistentDurableBlock(
 *     BlockBehaviour.Properties.of()
 *         .strength(3.0f)
 *         .requiresCorrectToolForDrops(),
 *     200,    // maxBlockDurability
 *     2,      // baseResistance
 *     0.3f,   // explosionResistance
 *     0.5f,   // physicalResistance
 *     0.1f,   // magicResistance
 *     0       // recoveryRate
 * );
 * </pre>
 *
 * @see PersistentDurableBlockEntity
 * @see DurableBlock
 */
public class PersistentDurableBlock extends DurableBlock {

    public PersistentDurableBlock(Properties properties,
                                  float maxBlockDurability,
                                  float baseResistance,
                                  float explosionResistance,
                                  float physicalResistance,
                                  float magicResistance,
                                  float recoveryRate) {
        super(properties, maxBlockDurability, baseResistance,
                explosionResistance, physicalResistance, magicResistance, recoveryRate);
    }

    @Override
    protected EntityType<? extends DurableBlockEntity> getEntityType() {
        return PersistentDurableBlockEntity.ENTITY_TYPE.get();
    }
}