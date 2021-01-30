package exchange.core2.benchmarks.generator.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class AsyncProgressLogger {

    public static LongConsumer createLoggingConsumer(int totalTransactionsNumber,
                                                     Consumer<String> logger,
                                                     int progressLogIntervalSeconds) {

        final long progressLogInterval = 1_000_000_000L * progressLogIntervalSeconds;
        final AtomicLong nextUpdateTime = new AtomicLong(System.nanoTime() + progressLogInterval);
        final LongAdder progress = new LongAdder();
        return transactions -> {
            progress.add(transactions);
            final long whenLogNext = nextUpdateTime.get();
            final long timeNow = System.nanoTime();
            if (timeNow > whenLogNext) {
                if (nextUpdateTime.compareAndSet(whenLogNext, timeNow + progressLogInterval)) {
                    // whichever thread won - it should print progress
                    final long done = progress.sum();
                    logger.accept(String.format("%.01f%% done (%d of %d)",
                            done * 100.0 / totalTransactionsNumber, done, totalTransactionsNumber));
                }
            }
        };
    }
}
