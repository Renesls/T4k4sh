package com.t4kash.api.marketplace.service;

public interface ObjectStorage {
    String bucketName();

    void upload(String objectPath, String contentType, byte[] content);

    byte[] download(String objectPath);

    void delete(String objectPath);
}
