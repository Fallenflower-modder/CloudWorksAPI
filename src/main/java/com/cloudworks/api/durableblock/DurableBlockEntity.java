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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * DurableBlock 的方块实体替代——兼具 {@link LivingEntity} 的可被攻击能力和
 * 类 BlockEntity 的数据持久化能力。
 *
 * <h3>生命周期</h3>
 * <ul>
 *   <li>创建：{@link DurableBlock} 放置时产生，位于方块中心</li>
 *   <li>销毁：方块被破坏时由 {@link DurableBlock#onRemove} 调用 {@link #discard()}</li>
 *   <li>耐久归零：调用 {@link #onDurabilityZero()} 破坏方块并移除自身</li>
 *   <li>区块加载：tick 中周期性校验方块是否仍存在，否则自毁</li>
 * </ul>
 *
 * <h3>伤害处理</h3>
 * <p>
 * 玩家可左键攻击实体（碰撞箱比方块略小，瞄准方块边缘可挖掘），
 * 其余伤害来源按类型分类，经过抗性计算后扣减耐久值。
 * </p>
 *
 * <h3>持久化</h3>
 * <p>
 * 耐久值和方块属性通过 {@link #addAdditionalSaveData} / {@link #readAdditionalSaveData}
 * 存入区块 NBT，区块加载时自动恢复。
 * </p>
 */
public class DurableBlockEntity extends LivingEntity {

    private static final String TAG_CURRENT_DURABILITY = "CurrentDurability";
    private static final String TAG_MAX_DURABILITY = "MaxDurability";
    private static final String TAG_BASE_RESISTANCE = "BaseResistance";
    private static final String TAG_EXPLOSION_RESISTANCE = "ExplosionResistance";
    private static final String TAG_PHYSICAL_RESISTANCE = "PhysicalResistance";
    private static final String TAG_MAGIC_RESISTANCE = "MagicResistance";
    private static final String TAG_RECOVERY_RATE = "RecoveryRate";
    private static final String TAG_BOUND_POS = "BoundPos";

    /**
     * 同步到客户端的耐久比例（0.0 ~ 1.0），用于裂纹渲染。
     */
    public static final EntityDataAccessor<Float> DATA_DURABILITY_RATIO =
            SynchedEntityData.defineId(DurableBlockEntity.class, EntityDataSerializers.FLOAT);

    /**
     * 实体类型引用，由 {@link com.cloudworks.api.CloudWorksAPI} 在注册阶段注入。
     * 必须在注册完成后、任何 DurableBlock 被放置前完成设置。
     */
    public static Supplier<EntityType<DurableBlockEntity>> ENTITY_TYPE = () -> null;

    protected float currentDurability;
    private float maxDurability;
    private float baseResistance;
    private float explosionResistance;
    private float physicalResistance;
    private float magicResistance;
    private float recoveryRate;

    /**
     * 无敌帧计时器：每次受到有效伤害后，接下来的 3 tick 内免疫所有伤害。
     * 用于限制实体每秒最多受击 5 次（20 tick / 4 = 5），
     * 防止每 tick 伤害源（如无尽鞘翅俯冲）造成耐久过量消耗和并发问题。
     */
    protected int invulnerableTick = 0;

    /**
     * 标记是否为内部移除流程（forceDiscard / forceRemove），
     * 用于在 {@link #remove(RemovalReason)} 中区分内部/外部移除。
     */
    protected boolean internalRemoval = false;

    /**
     * 该实体绑定的方块坐标。
     *
     * <p>实体与此坐标处的方块绑定生命周期：</p>
     * <ul>
     *   <li>tick 中检查该坐标处的方块是否仍为 {@link DurableBlock}，
     *       若不是则自动清除实体（防止僵尸方块）。</li>
     *   <li>外部移除实体时，同步破坏该坐标处的方块（防止僵尸实体）。</li>
     * </ul>
     *
     * <p>在 {@link DurableBlock#spawnEntity} 中通过 {@link #setBoundPos(BlockPos)} 设置，
     * 并持久化到 NBT 中。</p>
     */
    protected BlockPos boundPos = BlockPos.ZERO;

    /**
     * EntityType 构造工厂。属性由 {@link #setDurableBlock(DurableBlock)} 后续设置。
     */
    public DurableBlockEntity(EntityType<? extends DurableBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setInvulnerable(false);
        this.setSilent(false);
        this.blocksBuilding = false;
    }

    /**
     * 从 {@link DurableBlock} 实例初始化属性。
     * 在方块放置后调用。
     */
    public void setDurableBlock(DurableBlock block) {
        this.maxDurability = block.getMaxBlockDurability();
        this.currentDurability = this.maxDurability;
        this.baseResistance = block.getBaseResistance();
        this.explosionResistance = block.getExplosionResistance();
        this.physicalResistance = block.getPhysicalResistance();
        this.magicResistance = block.getMagicResistance();
        this.recoveryRate = block.getRecoveryRate();
    }

    /**
     * 设置该实体绑定的方块坐标。
     * 由 {@link DurableBlock#spawnEntity} 在放置实体后调用。
     */
    public void setBoundPos(BlockPos pos) {
        this.boundPos = pos.immutable();
    }

    // ======================== 属性 ========================

    public float getCurrentDurability() { return currentDurability; }
    public float getMaxDurability() { return maxDurability; }
    public float getBaseResistance() { return baseResistance; }
    public float getExplosionResistance() { return explosionResistance; }
    public float getPhysicalResistance() { return physicalResistance; }
    public float getMagicResistance() { return magicResistance; }
    public float getRecoveryRate() { return recoveryRate; }

    // ======================== 生命周期 ========================

    @Override
    public void tick() {
        super.tick();
        // 无敌帧计时器递减（无论客户端/服务端都需要维护）
        if (this.invulnerableTick > 0) {
            this.invulnerableTick--;
        }
        if (this.level().isClientSide()) return;

        // 始终保持实体位于方块下方（防止任何外力推动，且不阻碍方块上方操作）
        BlockPos pos = this.boundPos;
        this.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // 每 tick 检查绑定的方块是否仍存在
        // 不再依赖方块破坏事件来清除实体，而是由实体自行管理生命周期
        BlockState state = this.level().getBlockState(pos);
        if (!(state.getBlock() instanceof DurableBlock)) {
            this.forceDiscard();
            return;
        }

        // 自动恢复耐久
        if (this.recoveryRate > 0 && this.currentDurability < this.maxDurability) {
            this.currentDurability = Math.min(this.maxDurability,
                    this.currentDurability + this.recoveryRate / 20.0f);
        }
    }

    // ======================== 伤害处理 ========================

    /**
     * 伤害处理入口。
     * <p>
     * 玩家近战攻击直接拦截（玩家需通过挖掘破坏方块），
     * 其余伤害按类型分类，经过抗性计算后扣减耐久值。
     * </p>
     */
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;

        // 耐久归零后不再处理任何伤害（防止并发伤害导致 destroyBlock 被跳过）
        if (this.currentDurability <= 0) {
            return false;
        }

        // 无敌帧保护：每次受击后 3 tick 内免疫所有伤害
        if (this.invulnerableTick > 0) {
            return false;
        }

        // 拦截 /kill 命令等无敌穿透伤害
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        // 忽略环境伤害（溺水/窒息等），避免频繁播放音效
        if (source.is(DamageTypeTags.IS_DROWNING) || source.typeHolder().is(DamageTypes.IN_WALL)) {
            return false;
        }

        // 播放方块被挖掘时的声音（在任何有效攻击时都播放，即使伤害被抗性完全吸收）
        BlockState state = this.level().getBlockState(this.blockPosition());
        SoundType soundType = state.getSoundType();
        this.level().playSound(null, this.blockPosition(), soundType.getHitSound(),
                SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f,
                soundType.getPitch() * 0.8f);

        // 分类伤害并计算实际扣减
        DurableBlockDamageType type = classifyDamage(source);
        float actualDamage = calculateActualDamage(amount, type);

        if (actualDamage <= 0) return false;

        this.currentDurability = Math.max(0, this.currentDurability - actualDamage);

        // 同步耐久比例到客户端
        this.entityData.set(DATA_DURABILITY_RATIO,
                this.maxDurability > 0 ? this.currentDurability / this.maxDurability : 0f);

        // 设置无敌帧：3 tick 内免疫后续伤害
        this.invulnerableTick = 3;

        if (this.currentDurability <= 0) {
            onDurabilityZero();
        }

        return true;
    }

    /**
     * 禁止扣血动画和原版伤害逻辑，所有伤害由 {@link #hurt} 中的耐久系统接管。
     */
    @Override
    protected void actuallyHurt(@NotNull DamageSource damageSource, float amount) {
        // 不调用 super，禁止原版扣血流程
    }

    /**
     * 根据伤害来源的标签分类到三种伤害类型。
     */
    private static DurableBlockDamageType classifyDamage(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return DurableBlockDamageType.EXPLOSION;
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return DurableBlockDamageType.MAGIC;
        }
        return DurableBlockDamageType.PHYSICAL;
    }

    /**
     * 计算实际耐久扣减值。
     *
     * <pre>
     * 实际扣减 = max(0, 原始伤害 - 基础抗性) * (1 - 对应类型减免)
     * </pre>
     */
    private float calculateActualDamage(float rawDamage, DurableBlockDamageType type) {
        float afterBase = Math.max(0, rawDamage - this.baseResistance);
        if (afterBase <= 0) return 0;

        float resistance = switch (type) {
            case EXPLOSION -> this.explosionResistance;
            case PHYSICAL -> this.physicalResistance;
            case MAGIC -> this.magicResistance;
        };

        return afterBase * (1.0f - resistance);
    }

    /**
     * 耐久归零时，破坏方块并移除自身。
     * 方块掉落由战利品表决定。
     */
    protected void onDurabilityZero() {
        Level level = this.level();
        BlockPos pos = this.boundPos;
        this.forceDiscard();  // 先移除实体，避免 destroyBlock 触发销毁流程时重复操作
        level.destroyBlock(pos, true);
    }

    // ======================== NBT 持久化 ========================

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(TAG_CURRENT_DURABILITY, this.currentDurability);
        tag.putFloat(TAG_MAX_DURABILITY, this.maxDurability);
        tag.putFloat(TAG_BASE_RESISTANCE, this.baseResistance);
        tag.putFloat(TAG_EXPLOSION_RESISTANCE, this.explosionResistance);
        tag.putFloat(TAG_PHYSICAL_RESISTANCE, this.physicalResistance);
        tag.putFloat(TAG_MAGIC_RESISTANCE, this.magicResistance);
        tag.putFloat(TAG_RECOVERY_RATE, this.recoveryRate);
        tag.put(TAG_BOUND_POS, NbtUtils.writeBlockPos(this.boundPos));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_MAX_DURABILITY)) {
            this.currentDurability = tag.getFloat(TAG_CURRENT_DURABILITY);
            this.maxDurability = tag.getFloat(TAG_MAX_DURABILITY);
            this.baseResistance = tag.getFloat(TAG_BASE_RESISTANCE);
            this.explosionResistance = tag.getFloat(TAG_EXPLOSION_RESISTANCE);
            this.physicalResistance = tag.getFloat(TAG_PHYSICAL_RESISTANCE);
            this.magicResistance = tag.getFloat(TAG_MAGIC_RESISTANCE);
            this.recoveryRate = tag.getFloat(TAG_RECOVERY_RATE);
        }
        if (tag.contains(TAG_BOUND_POS)) {
            this.boundPos = NbtUtils.readBlockPos(tag, TAG_BOUND_POS).orElse(BlockPos.ZERO);
        }
    }

    // ======================== 物理属性抑制 ========================

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public void kill() {
        // 禁止 /kill 命令杀死方块实体，必须通过挖掘或伤害归零来破坏
    }

    /**
     * 强制移除实体，绕过子类对 {@link #remove(RemovalReason)} 的重写。
     * 仅在内部流程（方块消失、耐久归零等）中使用。
     */
    public final void forceRemove(Entity.RemovalReason reason) {
        internalRemoval = true;
        super.remove(reason);
    }

    /**
     * 强制丢弃实体，绕过子类对 {@link #discard()} 的重写。
     * 仅在内部流程（方块消失、耐久归零等）中使用。
     */
    public final void forceDiscard() {
        internalRemoval = true;
        super.discard();
    }

    /**
     * 拦截外部移除请求：非内部流程的移除会同步破坏绑定的方块，
     * 实现"实体消失 → 方块消失"的一致性。
     *
     * <p>使用 {@link #boundPos} 而非 {@link #blockPosition()} 来确定绑定的方块，
     * 避免因实体碰撞箱延伸到相邻方块区域而误删其他耐久方块的实体。</p>
     */
    @Override
    public void remove(RemovalReason reason) {
        if (!internalRemoval && !this.level().isClientSide()) {
            BlockPos pos = this.boundPos;
            Level level = this.level();
            if (level.getBlockState(pos).getBlock() instanceof DurableBlock) {
                level.destroyBlock(pos, true);
            }
        }
        internalRemoval = false;
        super.remove(reason);
    }

    /**
     * 阻止实体推搡其他实体。
     */
    @Override
    public void push(@NotNull Entity entity) {
        // DurableBlockEntity 不应推搡任何实体
    }

    /**
     * 禁用实体推搡逻辑，阻止遍历附近实体并施加推力。
     */
    @Override
    protected void pushEntities() {
        // DurableBlockEntity 不应推搡任何实体
    }

    /**
     * 禁止与任何实体发生碰撞。
     */
    @Override
    public boolean canCollideWith(@NotNull Entity entity) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
        // DurableBlock 实体没有装备栏，空实现
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return java.util.Collections.emptyList();
    }

    @Override
    public @NotNull Iterable<ItemStack> getHandSlots() {
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    // ======================== 实体数据同步 ========================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DURABILITY_RATIO, 1.0f);
    }

    // ======================== 属性注册 ========================

    /**
     * 为该实体类型创建属性实例。
     * 所有属性值设为最低，因为实体不需要移动、战斗等原版行为。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0);
    }
}