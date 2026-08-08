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

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * 带耐久值的方块基类。
 *
 * <h3>核心概念</h3>
 * <p>
 * 该方块不依赖传统 BlockEntity，而是通过一个 {@link DurableBlockEntity}
 * （{@link LivingEntity} 子类）来存储耐久数据和接收伤害。
 * 该实体位于方块内部，碰撞箱与方块一致，可被生物主动攻击，
 * 但不阻挡移动（移动阻挡由方块本身的碰撞箱属性决定）。
 * </p>
 *
 * <p><b>生命周期管理：</b>实体与方块绑定，绑定坐标存储在 {@link DurableBlockEntity#boundPos} 中。
 * 实体每 tick 检查绑定的方块是否仍存在，若不存在则自动清除自身。
 * 方块破坏事件不再负责清除实体，实体生命周期由实体自身管理。
 * 这种设计避免了因实体碰撞箱延伸到相邻方块而误删其他实体的 Bug。</p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * public class MyConcreteBlock extends DurableBlock {
 *     public MyConcreteBlock() {
 *         super(Properties.of()
 *                 .strength(3.0f)
 *                 .requiresCorrectToolForDrops(),
 *             200,    // maxBlockDurability
 *             2,      // baseResistance
 *             0.3f,   // explosionResistance
 *             0.5f,   // physicalResistance
 *             0.1f,   // magicResistance
 *             0       // recoveryRate (per second)
 *         );
 *     }
 * }
 * </pre>
 *
 * @see DurableBlockEntity
 * @see DurableBlockDamageType
 */
public class DurableBlock extends Block {

    private final float maxBlockDurability;
    private final float baseResistance;
    private final float explosionResistance;
    private final float physicalResistance;
    private final float magicResistance;
    private final float recoveryRate;

    /**
     * 构造带耐久属性的方块。
     *
     * @param properties          原版方块属性（硬度、挖掘等级等）
     * @param maxBlockDurability  最大耐久值
     * @param baseResistance      基础抗性（等量减免原始伤害，最低减到 0）
     * @param explosionResistance 爆炸伤害减免（0.0 ~ 1.0）
     * @param physicalResistance  物理伤害减免（0.0 ~ 1.0）
     * @param magicResistance     魔法伤害减免（0.0 ~ 1.0）
     * @param recoveryRate        每秒自动恢复耐久值（0 = 不恢复）
     */
    public DurableBlock(Properties properties,
                        float maxBlockDurability,
                        float baseResistance,
                        float explosionResistance,
                        float physicalResistance,
                        float magicResistance,
                        float recoveryRate) {
        super(properties);
        this.maxBlockDurability = maxBlockDurability;
        this.baseResistance = baseResistance;
        this.explosionResistance = Math.clamp(explosionResistance, 0.0f, 1.0f);
        this.physicalResistance = Math.clamp(physicalResistance, 0.0f, 1.0f);
        this.magicResistance = Math.clamp(magicResistance, 0.0f, 1.0f);
        this.recoveryRate = recoveryRate;
    }

    // ======================== 属性访问器 ========================

    public float getMaxBlockDurability() { return maxBlockDurability; }
    public float getBaseResistance() { return baseResistance; }
    public float getExplosionResistance() { return explosionResistance; }
    public float getPhysicalResistance() { return physicalResistance; }
    public float getMagicResistance() { return magicResistance; }
    public float getRecoveryRate() { return recoveryRate; }

    // ======================== 方块 → 实体生命周期 ========================

    /**
     * 方块放置时，由 {@link #onPlace} 统一处理实体生成，本方法不再干预。
     * <p>
     * 保留空实现以允许子类重写，但实体生成逻辑已统一移至 {@link #onPlace}，
     * 以确保所有放置途径（玩家放置、{@code /setblock}、{@code /fill} 等）行为一致。
     * </p>
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // 实体生成由 onPlace 统一处理
    }

    /**
     * 方块被放置时，在方块位置生成对应的 {@link DurableBlockEntity}。
     * <p>
     * 此方法覆盖所有方块放置途径，包括 {@code /setblock}、{@code /fill} 等命令，
     * 弥补 {@link #setPlacedBy} 仅适用于玩家放置的局限。
     * 放置前会检查该位置是否已有绑定到本格的实体，避免重复生成。
     * </p>
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) return;

        // 检查是否已有绑定到本格位置的耐久方块实体，避免重复
        AABB aabb = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        boolean exists = level.getEntitiesOfClass(DurableBlockEntity.class, aabb, entity ->
                entity.blockPosition().equals(pos)
        ).stream().findFirst().isPresent();

        if (!exists) {
            spawnEntity(level, pos);
        }
    }

    /**
     * 方块被移除时，不再主动清除实体。
     * 实体生命周期由实体自身的 tick 方法管理：
     * 实体每 tick 检查绑定的方块是否仍存在，若不存在则自动清除自身。
     * 这种设计避免了因实体碰撞箱延伸到相邻方块区域而误删其他实体的 Bug。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        // 实体生命周期由实体自身管理，方块移除事件不干预实体
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ======================== 实体管理 ========================

    /**
     * 返回该方块对应的实体类型。
     * 子类可重写以返回不同的实体类型（如 {@code PersistentDurableBlockEntity}）。
     */
    protected EntityType<? extends DurableBlockEntity> getEntityType() {
        return DurableBlockEntity.ENTITY_TYPE.get();
    }

    /**
     * 在指定位置生成 {@link DurableBlockEntity}。
     */
    private void spawnEntity(Level level, BlockPos pos) {
        DurableBlockEntity entity = (DurableBlockEntity) getEntityType().create(level);
        if (entity == null) return;

        entity.setDurableBlock(this);
        entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        entity.setBoundPos(pos);
        level.addFreshEntity(entity);
    }

    // ======================== 战利品表 ========================

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        // 实体生命周期由实体自身管理，玩家破坏事件不干预实体
        return super.playerWillDestroy(level, pos, state, player);
    }
}