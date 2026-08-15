package com.t4kash.api.marketplace.service;

public record DownloadedAttachment(
        String fileName,
        String contentType,
        byte[] content
) {
}
