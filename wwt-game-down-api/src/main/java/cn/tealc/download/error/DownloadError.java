package cn.tealc.download.error;

/**
 * 下载错误码，与 KR Launcher 的 KRDownloadError 对齐。
 */
public final class DownloadError {
    private DownloadError() {}

    public static final int NO_ERROR = 0;
    /** 服务器不支持 HTTP Range（请求了 Range 却返回 200） */
    public static final int NOT_SUPPORT_DOWNLOAD_RANGE = 1;
    /** 服务器返回的 Content-Length 与实际不符 */
    public static final int GET_CONTENT_LENGTH_ERROR = 2;
    /** Content-Encoding 校验失败 */
    public static final int CHECK_CONTENT_ENCODING_FAIL = 3;
    /** MD5 校验失败 */
    public static final int CHECK_MD5_FAILED = 4;
    /** 流读取超时 */
    public static final int STREAM_READ_BLOCK_TIMEOUT = 5;
    /** 磁盘空间不足 */
    public static final int DISK_SPACE_CHECK_FAIL = 6;
    /** 网络错误（连接失败、重置、不可达等） */
    public static final int NETWORK = 7;
    /** 磁盘已满（写入阶段） */
    public static final int DISK_FULL = 8;
}