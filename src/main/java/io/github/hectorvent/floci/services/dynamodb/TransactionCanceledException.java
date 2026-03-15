package io.github.hectorvent.floci.services.dynamodb;

import io.github.hectorvent.floci.core.common.AwsException;

import java.util.List;

/**
 * Thrown when a TransactWriteItems condition check fails.
 * Carries per-item cancellation reasons for the AWS response.
 */
public class TransactionCanceledException extends AwsException {

    private final List<String> cancellationReasons;

    public TransactionCanceledException(List<String> cancellationReasons) {
        super("TransactionCanceledException",
                "Transaction cancelled, please refer cancellation reasons for specific reasons [" +
                        String.join(", ", cancellationReasons) + "]",
                400);
        this.cancellationReasons = cancellationReasons;
    }

    public List<String> getCancellationReasons() {
        return cancellationReasons;
    }
}
