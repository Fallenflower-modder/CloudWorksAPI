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

import org.apache.logging.log4j.core.LogEvent;

/**
 * ConsoleSeeker 内部过滤单元接口。
 * <p>
 * 下游模组实现此接口，通过 {@link ConsoleSeekerEventManager#addInternalFilterUnit(InternalFilterUnit)}
 * 注册到内部过滤单元列表。当有日志事件到达时，ConsoleSeeker 会遍历所有已注册的单元：
 * <ul>
 *   <li>若单元返回 {@code true}，该单元的 {@link #getTag()} 标记会被加入事件的投送标记集合</li>
 *   <li>若所有单元都返回 {@code false}，事件被丢弃</li>
 *   <li>若列表为空，事件直接发布（投送标记集合为空集）</li>
 * </ul>
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * InternalFilterUnit myUnit = new InternalFilterUnit() {
 *     public DirectedDeliveryTag getTag() {
 *         return new DirectedDeliveryTag("mymod", "cpu_alert");
 *     }
 *     public boolean test(LogEvent event) {
 *         return event.getLoggerName().startsWith("com.example");
 *     }
 * };
 * ConsoleSeekerEventManager.addInternalFilterUnit(myUnit);
 * </pre>
 */
public interface InternalFilterUnit {

    /**
     * @return 该过滤单元的定向投送标记，当此单元通过时会被加入事件标记集合
     */
    DirectedDeliveryTag getTag();

    /**
     * 测试原始 Log4j2 日志事件是否应被发布。
     *
     * @param event Log4j2 原始日志事件
     * @return {@code true} 表示该日志应被发布，并将本单元的标记加入投送标记集合
     */
    boolean test(LogEvent event);
}