package cn.tealc.wwt.game.resource.internal.legacy.download;

import cn.tealc.wwt.game.resource.BandwidthLimiter;
import cn.tealc.wwt.game.resource.internal.legacy.model.CdnConfig;
import cn.tealc.wwt.game.resource.internal.legacy.model.DownloadInfo;

import java.util.List;
import java.util.UUID;

/**
 * Builder for CDNDownloadTask.
 * Corresponds to KRCDNDownloadTaskBuilder.cs.
 *
 * Used by KRResourcesDownloadTask to construct the underlying CDNDownloadTask
 * with read block timeout, CDN test duration, and retry count.
 */
public class CDNDownloadTaskBuilder {
    private final List<DownloadInfo> downloadInfoList;
    private final List<CdnConfig> cdnConfigList;
    private int maxRetryCount = Integer.MAX_VALUE;
    private String basePath;
    private String baseDestPath;
    private String tag;
    private String id;
    private long readBlockTimeout;
    private long cdnSelectTestDuration = 3000L;
    private int maxParallelCount = 4;
    private BandwidthLimiter bandwidthLimiter;

    public CDNDownloadTaskBuilder(List<DownloadInfo> downloadInfoList, List<CdnConfig> cdnConfigs) {
        this.downloadInfoList = downloadInfoList;
        this.cdnConfigList = cdnConfigs;
    }

    public CDNDownloadTaskBuilder withMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public CDNDownloadTaskBuilder withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public CDNDownloadTaskBuilder withBasePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    public CDNDownloadTaskBuilder withBaseDestPath(String baseDestPath) {
        this.baseDestPath = baseDestPath;
        return this;
    }

    public CDNDownloadTaskBuilder withReadBlockTimeout(long readBlockTimeout) {
        this.readBlockTimeout = readBlockTimeout;
        return this;
    }

    public CDNDownloadTaskBuilder withCdnSelectTestDuration(long cdnSelectTestDuration) {
        this.cdnSelectTestDuration = cdnSelectTestDuration;
        return this;
    }

    public CDNDownloadTaskBuilder withMaxParallelCount(int maxParallelCount) {
        this.maxParallelCount = maxParallelCount;
        return this;
    }

    public CDNDownloadTaskBuilder withBandwidthLimiter(BandwidthLimiter bandwidthLimiter) {
        this.bandwidthLimiter = bandwidthLimiter;
        return this;
    }

    public CDNDownloadTask build() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        CDNDownloadTask task = new CDNDownloadTask(cdnConfigList, downloadInfoList, basePath, baseDestPath);
        task.setMaxRetryCount(maxRetryCount);
        task.setCdnSelectTestDuration(cdnSelectTestDuration);
        task.setReadBlockTimeout(readBlockTimeout);
        task.setMaxParallelCount(maxParallelCount);
        task.setBandwidthLimiter(bandwidthLimiter);
        return task;
    }
}
