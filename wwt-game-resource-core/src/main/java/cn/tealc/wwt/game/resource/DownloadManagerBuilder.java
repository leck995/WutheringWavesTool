package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.DownloadInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * {@link DownloadManager} 的流式构建器。
 */
public final class DownloadManagerBuilder {
    private List<DownloadInfo> infos;
    private Path destRoot;
    private List<String> cdnBaseUrls = List.of();
    private int maxParallel = 4;
    private int maxRetry = 5;
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 30_000;

    public DownloadManagerBuilder(List<DownloadInfo> infos, Path destRoot) {
        this.infos = infos;
        this.destRoot = destRoot;
    }

    public DownloadManagerBuilder cdnBaseUrls(List<String> urls) {
        this.cdnBaseUrls = urls;
        return this;
    }

    public DownloadManagerBuilder maxParallel(int n) {
        this.maxParallel = n;
        return this;
    }

    public DownloadManagerBuilder maxRetry(int n) {
        this.maxRetry = n;
        return this;
    }

    public DownloadManagerBuilder connectTimeoutMs(int ms) {
        this.connectTimeoutMs = ms;
        return this;
    }

    public DownloadManagerBuilder readTimeoutMs(int ms) {
        this.readTimeoutMs = ms;
        return this;
    }

    public DownloadManager build() {
        return new DownloadManager(infos, destRoot, cdnBaseUrls, maxParallel, maxRetry,
                connectTimeoutMs, readTimeoutMs);
    }
}