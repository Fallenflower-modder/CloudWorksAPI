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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * 持久化耐久方块实体——对即死类攻击具有代码博弈保护。
 *
 * <p>重写 {@link #remove(RemovalReason)}、{@link #discard()} 和 {@link #setHealth(float)}，
 * 拦截所有来自外部的移除尝试，将其转换为 20% 耐久扣减，而非直接移除实体。</p>
 *
 * <p>内部流程（耐久归零、方块消失等）通过 {@link #forceDiscard()} /
 * {@link #forceRemove(RemovalReason)} 正常执行，它们设置 {@code internalRemoval} 标记位，
 * 使本类的拦截方法放行。</p>
 *
 * <p><b>服务端仅存实体：</b>该实体注册时使用 {@code clientTrackingRange(0)}，
 * 实体仅在服务端运行，客户端完全不知道实体存在。这带来以下好处：</p>
 * <ul>
 *   <li>玩家无法近战攻击到实体（客户端无实体目标），只能通过挖掘破坏方块</li>
 *   <li>不阻碍玩家在方块上表面放置方块或长按左键挖掘</li>
 *   <li>生物（凋灵、僵尸等）和远程攻击可在服务端正常攻击到实体</li>
 *   <li>无需处理客户端/服务端实体数据同步问题</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>
 * public class MyPersistentBlock extends PersistentDurableBlock {
 *     public MyPersistentBlock() {
 *         super(Properties.of().strength(3.0f), 200, 2, 0.3f, 0.5f, 0.1f, 0);
 *     }
 * }
 * </pre>
 *
 * @see PersistentDurableBlock
 * @see DurableBlockEntity
 */
public class PersistentDurableBlockEntity extends DurableBlockEntity {

    /**
     * 即死武器物品标签：标记为 {@code cloudworks_api:instant_kill_weapon} 的物品，
     * 其伤害事件将在 {@link #hurt(DamageSource, float)} 中被直接取消，
     * 避免因高额伤害一次扣除所有耐久。
     */
    public static final TagKey<Item> INSTANT_KILL_WEAPON =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("cloudworks_api", "instant_kill_weapon"));

    /**
     * 一次博弈拦截扣除的耐久比例。
     */
    private static final float RESIST_LOSS_FRACTION = 0.2f;

    /**
     * 实体类型引用，由 {@link com.cloudworks.api.CloudWorksAPI} 在注册阶段注入。
     */
    public static Supplier<EntityType<PersistentDurableBlockEntity>> ENTITY_TYPE = () -> null;

    public PersistentDurableBlockEntity(EntityType<? extends PersistentDurableBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    // ======================== 生命周期 ========================

    /**
     * 每 tick 检查并重置被外部代码（如寰宇支配之剑）设置的死亡状态。
     *
     * <p>该实体采用服务端仅存方案（{@code clientTrackingRange(0)}），
     * 客户端完全不知道实体存在，因此无需向客户端同步任何数据。
     * 但服务端仍需维护 {@code dead} 标志的正确性，以确保
     * {@link #hurt(DamageSource, float)} 中的死亡状态检查正常工作。</p>
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;

        // 外部代码（如寰宇支配之剑）可能直接设置 this.dead = true，
        // 导致后续的 hurt() 调用因死亡状态检查而拒绝处理伤害。
        // 此处兜底重置死亡状态，确保服务端逻辑正常运转。
        if (this.dead && !this.internalRemoval) {
            this.dead = false;
            this.deathTime = 0;
        }
    }

    // ======================== 死亡事件拦截 ========================

    /**
     * 拦截外部代码（如寰宇支配之剑）的 {@code die()} 调用，阻止实体被移除。
     *
     * <p>该实体采用服务端仅存方案（{@code clientTrackingRange(0)}），
     * 客户端完全不知道实体存在。但外部代码仍可能在服务端调用
     * {@code die()}，触发实体移除流程。此方法拦截这类调用，
     * 重置死亡状态以保持实体存活。</p>
     *
     * <p>耐久扣减已在 {@link #setHealth(float)} 或 {@link #hurt(DamageSource, float)}
     * 中处理，本方法仅负责阻止实体被移除。</p>
     */
    @Override
    public void die(@NotNull DamageSource source) {
        if (!this.internalRemoval && !this.level().isClientSide()) {
            // 重置死亡状态，阻止死亡事件广播到客户端
            this.dead = false;
            this.deathTime = 0;
            return;
        }
        super.die(source);
    }

    // ======================== 伤害拦截 ========================

    /**
     * 拦截即死武器的伤害事件，避免因高额伤害一次扣除所有耐久。
     *
     * <p>检测逻辑分三层：</p>
     * <ol>
     *   <li>如果实体已被外部代码标记为死亡（{@code dead == true}），
     *       直接拒绝伤害处理（防御性检查）。</li>
     *   <li>获取伤害源实体（同时检查 {@link DamageSource#getEntity()}
     *       和 {@link DamageSource#getDirectEntity()}），
     *       检查其主手物品是否被标记为 {@link #INSTANT_KILL_WEAPON} 即死武器标签，
     *       若是则取消伤害事件。</li>
     *   <li>兜底保护：单次伤害上限不超过当前最大耐久值，
     *       防止因标签检测失败导致的高额伤害一击清空所有耐久。</li>
     * </ol>
     *
     * <p>即死武器的耐久扣减由 {@link #setHealth(float)} 拦截统一处理，
     * 每次拦截扣除 {@link #RESIST_LOSS_FRACTION}（20%）最大耐久。</p>
     */
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // 死亡状态下不处理伤害（防御外部代码在设置 dead=true 后再次调用 hurt()）
        if (this.dead && !this.internalRemoval) {
            return false;
        }

        // 检测伤害源实体是否持有即死武器
        // 同时检查 getEntity()（造成者）和 getDirectEntity()（直接来源），
        // 因为不同 DamageSource 子类可能将攻击者放在不同字段中
        Entity attacker = source.getEntity();
        if (attacker == null) {
            attacker = source.getDirectEntity();
        }
        if (attacker instanceof LivingEntity livingAttacker) {
            if (isInstantKillWeapon(livingAttacker.getMainHandItem())) {
                return false;
            }
        }

        // 兜底上限：单次伤害不超过最大耐久值，防止标签检测失败时的一击清空
        float cappedAmount = Math.min(amount, this.getMaxDurability());
        return super.hurt(source, cappedAmount);
    }

    /**
     * 检查物品是否为即死武器（被标记为 {@code cloudworks_api:instant_kill_weapon} 标签）。
     *
     * @param stack 待检查的物品堆
     * @return 如果物品属于即死武器标签则返回 {@code true}
     */
    public static boolean isInstantKillWeapon(ItemStack stack) {
        return stack.is(INSTANT_KILL_WEAPON);
    }

    // ======================== 移除拦截 ========================

    /**
     * 拦截所有外部移除请求，将其转换为耐久扣减。
     * 内部移除流程（{@code internalRemoval == true}）正常放行。
     */
    @Override
    public void remove(RemovalReason reason) {
        if (internalRemoval) {
            // 内部流程（forceDiscard / forceRemove），直接放行
            super.remove(reason);
            return;
        }
        // 外部移除请求，转换为耐久扣减
        handleExternalKill();
    }

    // ======================== 生命值归零拦截 ========================

    /**
     * 拦截外部将生命值设为 0 的行为，将其转换为耐久扣减并恢复满生命值。
     */
    @Override
    public void setHealth(float health) {
        if (health <= 0.0f) {
            handleExternalKill();
            super.setHealth(this.getMaxHealth());
            return;
        }
        super.setHealth(health);
    }

    // ======================== 内部逻辑 ========================

    /**
     * 处理外部即死攻击：扣除 20% 最大耐久值。
     * 如果耐久归零，则通过 {@link #onDurabilityZero()} 正常破坏方块。
     *
     * <p>同时重置死亡状态（{@code dead}、{@code deathTime}），
     * 防止外部代码（如寰宇支配之剑）将实体标记为死亡后导致
     * 后续伤害处理被 {@link #hurt(DamageSource, float)} 中的死亡状态检查拒绝。</p>
     */
    private void handleExternalKill() {
        // 重置死亡状态：防止外部代码（如寰宇支配之剑的 die() 方法）
        // 直接设置 dead=true 和 Pose.DYING 导致实体进入"假死"状态
        this.dead = false;
        this.deathTime = 0;

        float loss = this.getMaxDurability() * RESIST_LOSS_FRACTION;
        float newDurability = Math.max(0, this.getCurrentDurability() - loss);

        this.currentDurability = newDurability;

        this.entityData.set(DATA_DURABILITY_RATIO,
                this.getMaxDurability() > 0 ? this.currentDurability / this.getMaxDurability() : 0f);

        if (this.currentDurability <= 0) {
            onDurabilityZero();
        }
    }

    // ======================== 姿态拦截 ========================

    /**
     * 拦截外部将实体设为死亡姿态（{@link Pose#DYING}）的尝试。
     * 内部流程（{@code internalRemoval == true}）正常放行。
     */
    @Override
    public void setPose(Pose pose) {
        if (pose == Pose.DYING && !internalRemoval) {
            return; // 阻止外部代码将实体设为死亡姿态
        }
        super.setPose(pose);
    }
}