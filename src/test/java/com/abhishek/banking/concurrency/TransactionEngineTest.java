package com.abhishek.banking.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.abhishek.banking.domain.exception.BankingException;

class TransactionEngineTest {

    @Test
    void testEngine_success() throws Exception {
        TransactionEngine engine = new TransactionEngine(2, 10);
        
        Future<String> future = engine.submit(() -> "Success");
        
        assertEquals("Success", future.get());
        engine.shutdown();
    }

    @Test
    void testEngine_rejectionWhenFull() {
        TransactionEngine engine = new TransactionEngine(1, 1);
        
        // Block the single worker
        engine.submit(() -> {
            Thread.sleep(500);
            return null;
        });
        
        // Fill the queue (capacity 1)
        engine.submit(() -> {
            Thread.sleep(100);
            return null;
        });
        
        // Third submission should be rejected
        assertThrows(BankingException.class, () -> {
            engine.submit(() -> "Rejected");
        });
        
        engine.shutdown();
    }
}
