package io.github.lizhangqu.fastmultidex

import org.gradle.api.Project
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.stream.Stream
import org.gradle.api.GradleException

/**
 * from atlas
 */
class ExecutorServicesHelper {
    private ExecutorService executorService = null
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

    public void execute(List<Runnable> runnableList) throws InterruptedException {
        if (runnableList == null || runnableList.isEmpty()) {
            return
        }

        Stream<Runnable> stream = runnableList.stream()
        if (threadCount > 1) {
            stream = stream.parallel()
        }
        stream.forEach(new Consumer<Runnable>() {
            @Override
            void accept(Runnable runnable) {
                try {
                    if (exception != null) {
                        project.logger.error("excute " +
                                name +
                                " task at " +
                                index.incrementAndGet() +
                                "/" +
                                runnables.size())
                        runnable.run()
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

    public <T> void execute(BlockingQueue<T> blockingQueue,
                            Handler<T> handler) throws InterruptedException {

        if (blockingQueue == null || blockingQueue.isEmpty()) {
            return
        }

        if (null == this.executorService) {
            this.executorService = Executors.newFixedThreadPool(threadCount)
        }

        final CountDownLatch countDownLatch = new CountDownLatch(this.threadCount)

        for (int i = 0; i < this.threadCount; i++) {
            this.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            T t = blockingQueue.poll()
                            if (null == t) {
                                break;
                            }
                            handler.handle(t)
                        }
                    } catch (GradleException gradleException) {
                        exception = gradleException
                    } finally {
                        countDownLatch.countDown()
                    }
                }
            })
        }
        countDownLatch.await()
        if (exception != null) {
            throw new GradleException(exception.getMessage(), exception)
        }
    }

    public static interface Handler<T> {
        public void handle(T t)
    }
}
