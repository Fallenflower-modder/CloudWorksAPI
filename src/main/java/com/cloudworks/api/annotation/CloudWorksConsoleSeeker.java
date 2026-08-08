/*
 * CloudWorks API - ConsoleSeeker Module
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.cloudworks.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ConsoleSeeker API 启用注解。
 * <p>
 * 将此注解添加到任何被 NeoForge 扫描的类上，即可强制启用 ConsoleSeeker 本体和 API 功能。
 * 当此注解存在时，配置文件中的 enable_module 和 enable_api 设置将被忽略，
 * ConsoleSeeker 本体和 API 均会强制启用。
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * {@code @CloudWorksConsoleSeeker}
 * public class MyMod {
 *     // ...
 * }
 * </pre>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CloudWorksConsoleSeeker {
}