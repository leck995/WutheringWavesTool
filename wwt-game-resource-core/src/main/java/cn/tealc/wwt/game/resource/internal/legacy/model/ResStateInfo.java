package cn.tealc.wwt.game.resource.internal.legacy.model;

public class ResStateInfo {
    public static final int STATE_NEED_DOWNLOAD = 0;
    public static final int STATE_DOWNLOADING = 1;
    public static final int STATE_UP_TO_DATE = 2;
    public static final int STATE_PRE_DOWNLOAD = 3;
    public static final int STATE_REPAIRING = 4;
    public static final int STATE_ROLLBACK = 5;

    public String newVersion;
    public String usingVersion;
    public String preDownloadVersion;
    public boolean enablePreDownload;
    public int state;
    public long size;
    public long originSize;
    public long neededSize;
    public long preDownloadSize;
    public long preDownloadNeededSize;
    public long preDownloadOriginSize;
    public boolean isFirstPreDownload = true;
    public boolean preDownloadComplete;
    public long unCompressSize;
}
