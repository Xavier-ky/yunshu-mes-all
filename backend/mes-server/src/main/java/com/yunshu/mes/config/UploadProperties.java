package com.yunshu.mes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yunshu.upload")
public class UploadProperties {

    /** Local directory for uploaded files (relative or absolute). */
    private String path = "./data/upload";

    /** Max upload size in bytes (default 5MB). */
    private long maxSize = 5L * 1024 * 1024;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
}
