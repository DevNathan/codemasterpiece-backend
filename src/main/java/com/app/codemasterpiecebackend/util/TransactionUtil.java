package com.app.codemasterpiecebackend.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionUtil {

    public static void afterCommit(Runnable r) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { r.run(); }
        });
    }

    public static void afterCompletion(java.util.function.IntConsumer statusConsumer) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { statusConsumer.accept(status); }
        });
    }

    public static void onRollback(Runnable r) {
        afterCompletion(status -> {
            if (status == TransactionSynchronization.STATUS_ROLLED_BACK) r.run();
        });
    }
}
