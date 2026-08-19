package com.kr.launcher.download;

import com.kr.launcher.model.DownloadInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for single-file DownloadTask.
 * Corresponds to KRDownloadTaskBuilder.cs.
 *
 * Used by KRResourceHelper.GetFileIndexInfo to download the index file with
 * backup URLs and MD5 verification. Produces a DownloadTask configured with
 * the builder's parameters.
 */
public class DownloadTaskBuilder {
    private final String url;
    private final String filePath;
    private List<String> backUpUrls;
    private String tag;
    private String md5;
    private String id;
    private long fileSize = -1L;
    private long rangeBegin;
    private long rangeEnd;
    private int maxRetryCount = Integer.MAX_VALUE;
    private boolean checkContentLength;
    private boolean checkContentEncoding;
    private boolean disableDownloadRange;
    private boolean disableCheckFileSize;

    public DownloadTaskBuilder(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    public DownloadTaskBuilder withFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public DownloadTaskBuilder withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public DownloadTaskBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public DownloadTaskBuilder withMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public DownloadTaskBuilder withRangeBegin(long rangeBegin) {
        this.rangeBegin = rangeBegin;
        return this;
    }

    public DownloadTaskBuilder withRangeEnd(long rangeEnd) {
        this.rangeEnd = rangeEnd;
        return this;
    }

    public DownloadTaskBuilder withMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public DownloadTaskBuilder withBackUpUrls(List<String> backUpUrls) {
        this.backUpUrls = backUpUrls;
        return this;
    }

    public DownloadTaskBuilder withBackUpUrl(String backUpUrl) {
        if (backUpUrl == null || backUpUrl.trim().isEmpty()) {
            return this;
        }
        List<String> list = new ArrayList<>();
        list.add(backUpUrl);
        this.backUpUrls = list;
        return this;
    }

    public DownloadTaskBuilder withCheckContentLength(boolean enable) {
        this.checkContentLength = enable;
        return this;
    }

    public DownloadTaskBuilder withCheckContentEncoding(boolean enable) {
        this.checkContentEncoding = enable;
        return this;
    }

    public DownloadTaskBuilder withDisableDownloadRange(boolean disable) {
        this.disableDownloadRange = disable;
        return this;
    }

    public DownloadTaskBuilder withDisableCheckFileSize(boolean disable) {
        this.disableCheckFileSize = disable;
        return this;
    }

    public DownloadTask build() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        DownloadInfo info = new DownloadInfo(url, filePath, fileSize, md5);
        DownloadTask task = new DownloadTask(info, filePath, backUpUrls, maxRetryCount);
        task.setCheckContentLength(checkContentLength);
        task.setCheckContentEncoding(checkContentEncoding);
        task.setDisableDownloadRange(disableDownloadRange);
        task.setDisableCheckFileSize(disableCheckFileSize);
        return task;
    }
}
