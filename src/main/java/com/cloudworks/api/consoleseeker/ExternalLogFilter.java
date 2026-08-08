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

import com.cloudworks.api.consoleseeker.event.ConsoleSeekerLogEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Set;

/**
 * ConsoleSeeker 外部过滤器抽象类。
 * <p>
 * 下游模组继承此类，实现自定义的日志解析逻辑。工作流程：
 * <ol>
 *   <li>订阅 NeoForge 事件总线上对应类型的日志事件</li>
 *   <li>匹配事件的定向投送标记（空标记集匹配所有，非空需有交集）</li>
 *   <li>通过 {@link #parse(ConsoleSeekerLogEvent)} 将原始事件解析为自定义类型</li>
 *   <li>通过 {@link #onReceive(Object)} 将解析结果传递给下游逻辑</li>
 * </ol>
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * public class MyCpuFilter extends ExternalLogFilter&lt;Integer&gt; {
 *     public MyCpuFilter() {
 *         super(new DirectedDeliveryTag("mymod", "cpu_alert"));
 *     }
 *     &#64;Override
 *     protected Integer parse(ConsoleSeekerLogEvent event) {
 *         return Integer.parseInt(event.getMessage());
 *     }
 *     &#64;Override
 *     protected void onReceive(Integer value) {
 *         System.out.println("CPU usage: " + value);
 *     }
 * }
 * </pre>
 *
 * @param <T> 解析后的自定义数据类型
 */
public abstract class ExternalLogFilter<T> {

    private final Set<DirectedDeliveryTag> interestedTags;
    private boolean registered;

    /**
     * 创建外部过滤器实例。
     *
     * @param tags 本过滤器关心的投送标记，至少指定一个
     */
    protected ExternalLogFilter(DirectedDeliveryTag... tags) {
        if (tags == null || tags.length == 0) {
            throw new IllegalArgumentException("At least one DirectedDeliveryTag must be specified.");
        }
        this.interestedTags = Set.of(tags);
        this.registered = false;
    }

    /**
     * 注册到 NeoForge 事件总线，开始接收日志事件。
     */
    public final void register() {
        if (!registered) {
            NeoForge.EVENT_BUS.register(this);
            registered = true;
        }
    }

    /**
     * 从 NeoForge 事件总线注销，停止接收日志事件。
     */
    public final void unregister() {
        if (registered) {
            NeoForge.EVENT_BUS.unregister(this);
            registered = false;
        }
    }

    /**
     * 检查是否已注册。
     */
    public final boolean isRegistered() {
        return registered;
    }

    /**
     * NeoForge 事件回调，匹配标记后解析并转发。
     * <p>
     * 子类不应覆写此方法。如需自定义解析逻辑，覆写 {@link #parse(ConsoleSeekerLogEvent)}；
     * 如需处理解析结果，覆写 {@link #onReceive(Object)}。
     * </p>
     */
    @SubscribeEvent
    public final void onLogEvent(ConsoleSeekerLogEvent event) {
        if (!tagMatches(event)) {
            return;
        }
        T parsed = parse(event);
        onReceive(parsed);
    }

    /**
     * 检查事件的投送标记集合是否与本过滤器关心的标记有交集。
     * <p>
     * 事件标记集合为空 → 匹配所有（一次也没匹配的情况视为匹配所有）。
     * 事件标记集合非空 → 与本过滤器 interestedTags 有任意交集即匹配。
     * </p>
     */
    protected boolean tagMatches(ConsoleSeekerLogEvent event) {
        Set<DirectedDeliveryTag> eventTags = event.getTagSet();
        // 空标记集匹配所有（原生事件、无过滤单元通过）
        if (eventTags.isEmpty()) {
            return true;
        }
        for (DirectedDeliveryTag eventTag : eventTags) {
            for (DirectedDeliveryTag interested : interestedTags) {
                if (eventTag.matches(interested)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 解析日志事件为自定义数据类型。
     * <p>
     * 默认实现返回原始日志消息字符串。下游模组可覆写此方法实现自定义解析逻辑。
     * </p>
     *
     * @param event 日志事件
     * @return 解析后的数据
     */
    @SuppressWarnings("unchecked")
    protected T parse(ConsoleSeekerLogEvent event) {
        return (T) event.getMessage();
    }

    /**
     * 解析完成后的回调，下游模组必须实现此方法处理解析结果。
     *
     * @param parsed 解析后的数据
     */
    protected abstract void onReceive(T parsed);
}