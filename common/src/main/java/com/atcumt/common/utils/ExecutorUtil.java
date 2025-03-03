package com.atcumt.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ExecutorUtil {
    public static void shutdown(ExecutorService executor) {
        shutdown(executor, 30, TimeUnit.SECONDS);
    }

    public static void shutdown(ExecutorService executor, long timeout, TimeUnit timeUnit) {
        boolean terminated = false;
        try {
            executor.shutdown();
            terminated = executor.awaitTermination(timeout, timeUnit);
        } catch (InterruptedException e) {
            log.error("等待线程池关闭时发生异常", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
        }

        if (!terminated) {
            log.warn("任务未能在 {} {} 内完成，尝试强制关闭线程池", timeout, timeUnit);
            forceShutdown(executor);
        }
    }

    private static void forceShutdown(ExecutorService executor) {
        List<Runnable> remainingTasks = executor.shutdownNow();
        if (!remainingTasks.isEmpty()) {
            log.warn("强制关闭线程池，{} 个任务未执行", remainingTasks.size());
        }

        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.error("线程池无法完全关闭");
            }
        } catch (InterruptedException e) {
            log.error("强制关闭线程池时发生异常", e);
            Thread.currentThread().interrupt();
        }
    }
}
