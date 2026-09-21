package com.abhishek.banking.concurrency;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abhishek.banking.domain.exception.BankingException;

public class TransactionEngine {
    private static final Logger logger = LoggerFactory.getLogger(TransactionEngine.class);
    
    private final ThreadPoolExecutor executor;
    private final int queueCapacity;

    public TransactionEngine(int workerCount, int queueCapacity) {
        this.queueCapacity = queueCapacity;
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
        
        // Custom thread factory to name worker threads
        this.executor = new ThreadPoolExecutor(
            workerCount, workerCount,
            0L, TimeUnit.MILLISECONDS,
            queue,
            runnable -> new Thread(runnable, "TxWorker-" + System.nanoTime()),
            new ThreadPoolExecutor.AbortPolicy() // Reject when full
        );
    }

    public <T> Future<T> submit(Callable<T> task) {
        try {
            return executor.submit(task);
        } catch (RejectedExecutionException e) {
            logger.warn("Transaction queue is full (capacity {}), rejecting transaction", queueCapacity);
            throw new BankingException("Server is too busy. Please try again later.", e);
        }
    }

    public void shutdown() {
        logger.info("Shutting down TransactionEngine...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("TransactionEngine did not terminate in 30 seconds. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("TransactionEngine shut down complete.");
    }
}
