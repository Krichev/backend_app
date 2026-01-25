package com.my.challenger.service;

import com.my.challenger.config.StorageProperties;
import com.my.challenger.entity.enums.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BucketResolver {

    private final StorageProperties storageProperties;

    public String getBucket(MediaType mediaType) {
        return storageProperties.getBucketForMediaType(mediaType);
    }
}
