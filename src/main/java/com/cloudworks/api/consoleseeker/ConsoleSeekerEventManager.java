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

import com.cloudworks.api.consoleseeker.event.ConsoleSeekerErrorEvent;
import com.cloudworks.api.consoleseeker.event.ConsoleSeekerInfoEvent;
import com.cloudworks.api.consoleseeker.event.ConsoleSeekerWarnEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ConsoleSeeker 事件管理器。
 * <p>
 * 管理日志事件管线的核心组件，负责：
 * <ul>
 *   <li>维护内部过滤单元列表</li>
 *   <li>收集通过单元的定向投送标记并注入事件</li>
 *   <li>将日志事件发布到 NeoForge 事件总线</li>
 *   <li>控制 API 启用状态</li>
 * </ul>
 * </p>
 *
 * <h3>数据流</h3>
 * <pre>
 * Log4j LogEvent
 *   → 内部过滤单元列表检查（收集 tagSet）
 *   → 创建 ConsoleSeekerLogEvent（含 tagSet）
 *   → NeoForge EVENT_BUS.post()
 *   → ExternalLogFilter 订阅处理
 * </pre>
 */
public class ConsoleSeekerEventManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleSeekerEventManager.class);

    /** 内部过滤单元列表（线程安全） */
    private static final List<InternalFilterUnit> internalFilterUnits = new CopyOnWriteArrayList<>();

    /** API 是否启用 */
    private static volatile boolean apiEnabled = false;

    private ConsoleSeekerEventManager() {}

    // ======================== 内部过滤单元管理 ========================

    /**
     * 添加内部过滤单元。
     * <p>
     * 当有日志事件到达时，ConsoleSeeker 会遍历所有单元。若单元返回 {@code true}，
     * 其标记会被加入事件的投送标记集合。重复添加同一个实例会被忽略。
     * </p>
     *
     * @param unit 内部过滤单元实例
     */
    public static void addInternalFilterUnit(InternalFilterUnit unit) {
        if (unit == null) return;
        if (!internalFilterUnits.contains(unit)) {
            internalFilterUnits.add(unit);
            LOGGER.info("CloudWorks API - InternalFilterUnit added. Total: {}", internalFilterUnits.size());
        }
    }

    /**
     * 移除内部过滤单元。
     *
     * @param unit 要移除的内部过滤单元实例
     */
    public static void removeInternalFilterUnit(InternalFilterUnit unit) {
        if (unit == null) return;
        internalFilterUnits.remove(unit);
        LOGGER.info("CloudWorks API - InternalFilterUnit removed. Total: {}", internalFilterUnits.size());
    }

    /**
     * 获取当前已注册的内部过滤单元数量。
     *
     * @return 内部过滤单元数量
     */
    public static int getInternalFilterUnitCount() {
        return internalFilterUnits.size();
    }

    // ======================== API 启用控制 ========================

    /**
     * 设置 API 启用状态。
     * <p>
     * 仅当 API 启用时，日志事件才会被发布到 NeoForge 事件总线。
     * 过滤单元注册不受此状态影响。
     * </p>
     *
     * @param enabled 是否启用 API
     */
    public static void setApiEnabled(boolean enabled) {
        apiEnabled = enabled;
        LOGGER.info("CloudWorks API - ConsoleSeeker API {}", enabled ? "enabled" : "disabled");
    }

    /**
     * 检查 API 是否已启用。
     *
     * @return API 启用状态
     */
    public static boolean isApiEnabled() {
        return apiEnabled;
    }

    // ======================== 事件发布 ========================

    /**
     * 由 ChatAppender 调用，将日志事件发布到 NeoForge 事件总线。
     * <p>
     * 发布流程：
     * <ol>
     *   <li>检查 API 是否启用，未启用则直接返回</li>
     *   <li>遍历内部过滤单元列表：
     *     <ul>
     *       <li>列表为空 → tagSet 为空集，事件直接发布</li>
     *       <li>列表非空 → 收集所有通过单元的标记到 tagSet</li>
     *       <li>所有单元均未通过 → 丢弃事件</li>
     *     </ul>
     *   </li>
     *   <li>创建对应级别的事件实例（含 tagSet）</li>
     *   <li>发布到 NeoForge EVENT_BUS</li>
     * </ol>
     * </p>
     *
     * @param logEvent Log4j2 原始日志事件
     */
    public static void fireLogEvent(LogEvent logEvent) {
        if (!apiEnabled) {
            return;
        }

        // 内部过滤单元检查
        Set<DirectedDeliveryTag> tagSet;
        if (internalFilterUnits.isEmpty()) {
            // 列表为空 → 不过滤，tagSet 为空集（匹配所有）
            tagSet = Set.of();
        } else {
            tagSet = new HashSet<>();
            for (InternalFilterUnit unit : internalFilterUnits) {
                try {
                    if (unit.test(logEvent)) {
                        tagSet.add(unit.getTag());
                    }
                } catch (Exception e) {
                    LOGGER.warn("CloudWorks API - InternalFilterUnit.test() threw exception: {}", e.getMessage());
                }
            }
            if (tagSet.isEmpty()) {
                // 所有单元均未通过，丢弃事件
                return;
            }
        }

        // 提取日志信息
        String loggerName = logEvent.getLoggerName();
        Level level = logEvent.getLevel();
        String message = logEvent.getMessage().getFormattedMessage();
        long timestamp = logEvent.getTimeMillis();
        String threadName = logEvent.getThreadName();
        String thrownString = null;
        if (logEvent.getThrown() != null) {
            StringWriter sw = new StringWriter();
            logEvent.getThrown().printStackTrace(new PrintWriter(sw));
            thrownString = sw.toString();
        }

        // 去除 ANSI 颜色代码
        message = LogFilter.stripAnsiCodes(message);

        // 根据级别发布对应事件
        try {
            switch (level.getStandardLevel()) {
                case INFO:
                    NeoForge.EVENT_BUS.post(new ConsoleSeekerInfoEvent(
                            loggerName, message, timestamp, threadName, thrownString, tagSet));
                    break;
                case WARN:
                    NeoForge.EVENT_BUS.post(new ConsoleSeekerWarnEvent(
                            loggerName, message, timestamp, threadName, thrownString, tagSet));
                    break;
                case ERROR:
                    NeoForge.EVENT_BUS.post(new ConsoleSeekerErrorEvent(
                            loggerName, message, timestamp, threadName, thrownString, tagSet));
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LOGGER.warn("CloudWorks API - Failed to post ConsoleSeeker event: {}", e.getMessage());
        }
    }
}