package io.github.hectorvent.floci.core.common;

public final class AwsArnUtils {

    private AwsArnUtils() {}

    /**
     * Converts an SQS ARN to a queue URL using the given base URL.
     * Example: arn:aws:sqs:us-east-1:000000000000:my-queue → http://localhost:4566/000000000000/my-queue
     */
    public static String arnToQueueUrl(String arn, String baseUrl) {
        String[] parts = arn.split(":");
        return baseUrl + "/" + parts[4] + "/" + parts[5];
    }
}