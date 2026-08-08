/*
 * CloudWorks API - ConsoleSeeker Module
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.cloudworks.api.consoleseeker;

import java.util.Objects;

/**
 * ConsoleSeeker 事件定向投送标记。
 * <p>
 * 标准格式为 {@code "modid:tagName"}，用于将事件路由到对应的过滤单元壳。
 * 空标记 {@link #EMPTY} 匹配所有其他标记。
 * </p>
 */
public final class DirectedDeliveryTag {

    /** 空标记，匹配所有其他标记 */
    public static final DirectedDeliveryTag EMPTY = new DirectedDeliveryTag("");

    private final String fullTag;

    /**
     * 通过模组 ID 和自定义标签名创建定向投送标记。
     *
     * @param modId   模组 ID
     * @param tagName 自定义标签名
     */
    public DirectedDeliveryTag(String modId, String tagName) {
        this.fullTag = modId + ":" + tagName;
    }

    private DirectedDeliveryTag(String fullTag) {
        this.fullTag = fullTag;
    }

    /**
     * @return 完整标记字符串，格式为 {@code "modid:tagName"}
     */
    public String getFullTag() {
        return fullTag;
    }

    /**
     * @return 是否为空标记
     */
    public boolean isEmpty() {
        return fullTag.isEmpty();
    }

    /**
     * 匹配检查：任意一方为空标记即视为匹配成功。
     *
     * @param other 另一个标记
     * @return true 如果匹配
     */
    public boolean matches(DirectedDeliveryTag other) {
        if (this.isEmpty() || other.isEmpty()) {
            return true;
        }
        return this.fullTag.equals(other.fullTag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DirectedDeliveryTag that)) return false;
        return fullTag.equals(that.fullTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullTag);
    }

    @Override
    public String toString() {
        return isEmpty() ? "EMPTY" : fullTag;
    }
}