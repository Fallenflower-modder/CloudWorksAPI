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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Log4j2 appender that forwards log events to the Minecraft chat.
 * Filters out [CHAT] messages to prevent infinite feedback loops.
 */
@Plugin(name = "ChatAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class ChatAppender extends AbstractAppender {

    protected ChatAppender(String name, Filter filter, PatternLayout layout) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    @PluginFactory
    public static ChatAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") PatternLayout layout) {

        if (layout == null) {
            layout = PatternLayout.newBuilder()
                    .withPattern("%d{HH:mm:ss} [%t/%level] [%logger{36}]: %msg")
                    .build();
        }

        return new ChatAppender(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        String formattedMessage = getLayout().toSerializable(event).toString();

        // 发布到 ConsoleSeeker API 事件管线（所有日志事件都发布，包括 CHAT 消息）
        ConsoleSeekerEventManager.fireLogEvent(event);

        // 含有 [CHAT] 的日志不广播到游戏内聊天栏，防止聊天消息→日志→聊天消息的无限循环
        if (formattedMessage.contains("[CHAT]")) {
            return;
        }

        LogToChatManager.processLogMessage(event.getLevel(), formattedMessage);
    }
}