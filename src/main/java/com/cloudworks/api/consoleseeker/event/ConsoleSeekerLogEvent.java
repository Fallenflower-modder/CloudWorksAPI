/*
 * CloudWorks API - ConsoleSeeker Module
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.cloudworks.api.consoleseeker.event;

import com.cloudworks.api.consoleseeker.DirectedDeliveryTag;
import net.neoforged.bus.api.Event;
import org.apache.logging.log4j.Level;

import java.util.Collections;
import java.util.Set;

/**
 * ConsoleSeeker 日志事件基类，包含日志的完整详细信息。
 * <p>
 * 三个具体事件管线分别对应不同日志级别：
 * <ul>
 *   <li>{@link ConsoleSeekerInfoEvent} — INFO 级别</li>
 *   <li>{@link ConsoleSeekerWarnEvent} — WARN 级别</li>
 *   <li>{@link ConsoleSeekerErrorEvent} — ERROR 级别</li>
 * </ul>
 * 下游模组可通过 {@code @SubscribeEvent} 订阅对应级别的事件。
 * </p>
 */
public abstract class ConsoleSeekerLogEvent extends Event {

    private final String loggerName;
    private final Level level;
    private final String message;
    private final long timestamp;
    private final String threadName;
    private final String thrownString;
    private final Set<DirectedDeliveryTag> tagSet;

    protected ConsoleSeekerLogEvent(String loggerName, Level level, String message,
                                    long timestamp, String threadName, String thrownString,
                                    Set<DirectedDeliveryTag> tagSet) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.thrownString = thrownString;
        this.tagSet = Collections.unmodifiableSet(tagSet);
    }

    /**
     * @return 产生该日志的 Logger 名称
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * @return 日志级别
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return 格式化后的日志消息（已去除 ANSI 颜色代码）
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return 日志产生时的 Unix 时间戳（毫秒）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return 产生日志的线程名称
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * @return 异常堆栈信息字符串，无异常时为 {@code null}
     */
    public String getThrownString() {
        return thrownString;
    }

    /**
     * @return 定向投送标记集合。空集表示原生事件，匹配所有外部过滤器
     */
    public Set<DirectedDeliveryTag> getTagSet() {
        return tagSet;
    }
}