package io.github.hectorvent.floci.services.s3.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RegisterForReflection
public class MultipartUpload {

    private String uploadId;
    private String bucket;
    private String key;
    private String contentType;
    private Instant initiated;
    private final Map<Integer, Part> parts = new ConcurrentHashMap<>();

    public MultipartUpload() {}

    public MultipartUpload(String bucket, String key, String contentType) {
        this.uploadId = UUID.randomUUID().toString();
        this.bucket = bucket;
        this.key = key;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.initiated = Instant.now();
    }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Instant getInitiated() { return initiated; }
    public void setInitiated(Instant initiated) { this.initiated = initiated; }

    public Map<Integer, Part> getParts() { return parts; }
}
