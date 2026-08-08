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
import org.apache.logging.log4j.Level;

import java.util.Set;

/**
 * ERROR 级别日志事件。
 * <p>
 * 当 ConsoleSeeker 捕获到 ERROR 级别的日志且通过内部过滤单元时，此事件会被发布到
 * NeoForge {@code EVENT_BUS}。下游模组可通过 {@code @SubscribeEvent} 订阅。
 * </p>
 */
public class ConsoleSeekerErrorEvent extends ConsoleSeekerLogEvent {

    public ConsoleSeekerErrorEvent(String loggerName, String message,
                                   long timestamp, String threadName, String thrownString,
                                   Set<DirectedDeliveryTag> tagSet) {
        super(loggerName, Level.ERROR, message, timestamp, threadName, thrownString, tagSet);
    }
}