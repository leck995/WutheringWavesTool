package cn.tealc.wwt.game.resource.internal.legacy.download;

/**
 * Download error codes matching C# KRDownloadError enum exactly.
 * Values aligned 1:1 with
 * c:\\Users\\saber\\Desktop\\launcher_main\\KRDownloader\\KRDownloadError.cs
 *
 * Note: C# STREAM_READ_BLOCK_TIMEOUT = 700106 (apparent typo in C# source for
 * 7001010/7001016);
 * we preserve the exact C# value for error-code compatibility.
 */
public final class DownloadError {
    public static final int NO_ERROR = 0;
    public static final int NETWORK = 7001001;
    public static final int UNKNOWN = 7001002;
    public static final int DELETE_FILE_ERROR = 7001003;
    public static final int NOT_SUPPORT_DOWNLOAD_RANGE = 7001004;
    public static final int GET_CONTENT_LENGTH_ERROR = 7001005;
    public static final int BUILD_CHUNK_TASKS_FAIL = 7001006;
    public static final int MERGE_CHUNK_FAIL = 7001007;
    public static final int PARSE_SINGLE_TASK_FAIL = 7001008;
    public static final int CHECK_MD5_FAILED = 7001009;
    // Note: 7001010 is skipped in C# enum (gap between CHECK_MD5_FAILED and
    // CREATE_FILE_STREAM_FAILED)
    public static final int CREATE_FILE_STREAM_FAILED = 7001011;
    public static final int MAKE_DIR_FAILED = 7001012;
    public static final int WRITE_FILE_FAIL = 7001013;
    public static final int CHECK_CONTENT_ENCODING_FAIL = 7001014;
    public static final int DISK_SPACE_CHECK_FAIL = 7001015;
    public static final int STREAM_READ_BLOCK_TIMEOUT = 700106;

    private DownloadError() {
    }
}
