/*
 * CloudWorks API - Unified Recipe Parsing Interface
 * Copyright (C) 2026 CloudWorks Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.cloudworks.api.recipeparser;

import com.cloudworks.api.recipeparser.model.RecipeParseResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Dedicated async thread pool for recipe parsing operations.
 * <p>
 * All heavy recipe scanning and parsing work is offloaded to this pool,
 * preventing main-thread blocking and game lag.
 * </p>
 *
 * <p>
 * 配方解析专用异步线程池。
 * 所有繁重的配方扫描和解析工作均卸载到此线程池，避免主线程阻塞和游戏卡顿。
 * </p>
 */
public final class AsyncRecipeParser {

    private static final Logger LOGGER = LoggerFactory.getLogger("CloudWorks-Async");

    private static final int THREAD_COUNT = 2;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
        Thread t = new Thread(r, "CloudWorks-RecipeParser-Worker");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private AsyncRecipeParser() {}

    /**
     * Submits a task to the async thread pool.
     * <p>
     * 向异步线程池提交任务。
     * </p>
     *
     * @param task the task to run asynchronously
     * @param task 要异步运行的任务
     * @return a Future representing the pending result
     * @return 表示待处理结果的 Future
     */
    public static Future<?> submit(Runnable task) {
        return EXECUTOR.submit(task);
    }

    /**
     * Submits a callable task and returns a Future.
     * <p>
     * 提交一个可调用任务并返回 Future。
     * </p>
     *
     * @param task the callable task
     * @param task 可调用任务
     * @param <T>  the result type
     * @param <T>  结果类型
     * @return a Future
     * @return Future
     */
    public static <T> Future<T> submit(Callable<T> task) {
        return EXECUTOR.submit(task);
    }

    /**
     * Runs a recipe query asynchronously on the worker thread, then delivers
     * the result back to the caller on the Minecraft server thread.
     * <p>
     * 在工作线程上异步运行配方查询，然后将结果在 Minecraft 服务器线程上返回给调用方。
     * </p>
     *
     * @param query           the query to run (on worker thread)
     * @param query           要运行的查询（在工作线程上）
     * @param resultCallback  callback invoked on the server thread with results
     * @param resultCallback  在服务器线程上调用的回调，接收结果
     * @param errorCallback   callback invoked on the server thread if an error occurs
     * @param errorCallback   发生错误时在服务器线程上调用的回调
     * @param server          the Minecraft server, used to schedule the callback on the server thread
     * @param server           Minecraft 服务器，用于在服务器线程上调度回调
     * @param <T>             the result type
     * @param <T>             结果类型
     */
    public static <T> void runAsyncQuery(
            Callable<T> query,
            Consumer<T> resultCallback,
            Consumer<String> errorCallback,
            net.minecraft.server.MinecraftServer server) {
        EXECUTOR.submit(() -> {
            try {
                T result = query.call();
                server.execute(() -> {
                    try {
                        resultCallback.accept(result);
                    } catch (Exception e) {
                        LOGGER.error("Error in result callback", e);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Async query failed", e);
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                server.execute(() -> {
                    try {
                        errorCallback.accept(msg);
                    } catch (Exception ex) {
                        LOGGER.error("Error in error callback", ex);
                    }
                });
            }
        });
    }

    /**
     * Shuts down the thread pool. Called on mod unload.
     * <p>
     * 关闭线程池。在模组卸载时调用。
     * </p>
     */
    public static void shutdown() {
        LOGGER.info("Shutting down async recipe parser thread pool...");
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}