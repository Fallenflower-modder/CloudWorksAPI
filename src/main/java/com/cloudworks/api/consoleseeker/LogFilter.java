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

/**
 * Log message filter and formatter for ConsoleSeeker.
 */
public class LogFilter {

    /**
     * 截断日志消息，超过最大长度时追加省略号。
     */
    public static String truncateLogMessage(String message) {
        int maxLength = ConsoleSeekerConfig.getMaxLogLength();

        if (maxLength <= 0 || message.length() <= maxLength) {
            return message;
        }

        return message.substring(0, maxLength) + "...";
    }

    /**
     * 移除 ANSI 颜色代码，使聊天栏输出更干净。
     */
    public static String stripAnsiCodes(String message) {
        return message.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}