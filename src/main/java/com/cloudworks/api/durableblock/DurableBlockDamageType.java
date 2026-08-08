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

/**
 * 伤害类型分类，用于 DurableBlock 的抗性计算。
 * <p>
 * Minecraft 中的各种伤害来源被归入以下三类之一，
 * 每种类型对应不同的伤害减免属性。
 * </p>
 */
public enum DurableBlockDamageType {

    /**
     * 爆炸伤害：TNT、苦力怕、末影水晶、床/重生锚爆炸等。
     * 对应减免属性：{@link DurableBlock#getExplosionResistance()}
     */
    EXPLOSION,

    /**
     * 物理伤害：生物近战攻击、箭矢、三叉戟、弹射物、玩家远程攻击等。
     * 对应减免属性：{@link DurableBlock#getPhysicalResistance()}
     */
    PHYSICAL,

    /**
     * 魔法伤害：药水、龙息、凋零效果、虚空伤害、火焰/闪电等非物理来源。
     * 对应减免属性：{@link DurableBlock#getMagicResistance()}
     */
    MAGIC
}