package io.github.lizhangqu.fastmultidex

import org.gradle.api.Project

import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Stream
import org.gradle.api.GradleException

/**
 * from atlas
 */
class ExecutorServicesHelper {
    private Project project
    private String name
    private int threadCount

    ExecutorServicesHelper(Project project, String name, int threadCount) {
        this.project = project
        this.name = name
        if (threadCount == 0) {
            threadCount = (Runtime.getRuntime().availableProcessors() / 2) + 1
        }
        this.threadCount = threadCount
    }

    public AtomicInteger index = new AtomicInteger(0)
    private Throwable exception

    public void execute(List<InformationRunnable> runnableList) throws InterruptedException {
        if (runnableList == null || runnableList.isEmpty()) {
            return
        }

        Stream<InformationRunnable> stream = runnableList.stream()
        if (threadCount > 1) {
            stream = stream.parallel()
        }
        stream.forEach(new Consumer<InformationRunnable>() {
            @Override
            void accept(InformationRunnable runnable) {
                try {
                    if (exception == null) {
                        long start = System.currentTimeMillis()
                        runnable.run()
                        long end = System.currentTimeMillis()
                        runnable.printInformation(name, index.incrementAndGet(), runnableList.size(), (end - start))
                    }
                } catch (Throwable gradleException) {
                    exception = gradleException
                }
            }
        })
        if (exception != null) {
            throw new GradleException(exception.getMessage(), exception)
        }
    }
}
