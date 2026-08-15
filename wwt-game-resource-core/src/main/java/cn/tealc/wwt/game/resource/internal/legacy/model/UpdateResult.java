package cn.tealc.wwt.game.resource.internal.legacy.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of an update operation.
 * Corresponds to KRUpdateResult.cs.
 *
 * Error codes use Windows HRESULT values (negative ints) for file-system errors,
 * matching C# KRUpdateResult constants. Resource errors use 7xxxxxx codes
 * matching C# KRResourceError constants.
 */
public class UpdateResult {
    public static final int ERROR_TYPE_NO_ERROR = -1;
    public static final int ERROR_TYPE_UNKNOWN = 0;
    public static final int ERROR_TYPE_FILE_OCCUPANCY = 1;
    public static final int ERROR_TYPE_FILE_PERMISSION_DENY = 2;
    public static final int ERROR_TYPE_FILE_MISSING = 3;
    public static final int ERROR_TYPE_NETWORK = 4;
    public static final int ERROR_TYPE_DISK_NOT_ENOUGH_SPACE = 5;
    public static final int ERROR_TYPE_GET_INDEX_FILE_ERROR = 6;
    public static final int ERROR_TYPE_RETRY_COUNT_EXCEEDED = 7;

    public static final int ERROR_CODE_FILE_OCCUPANCY = -2147024864;
    public static final int ERROR_CODE_DISK_NOT_ENOUGH_SPACE = -2147024784;
    public static final int ERROR_CODE_FILE_PERMISSION_DENY = -2147024891;
    public static final int ERROR_CODE_FILE_MISSING = -2147024894;

    public static final int APPLY_CHECK_MD5_NOT_MATCH = 7002017;

    public boolean success = true;
    public int errorCode;
    public String errorMessage = "";
    private int errorType = ERROR_TYPE_UNKNOWN;
    public int state = 6;
    public Map<String, Object> extInfos = new HashMap<>();

    public int getErrorType() {
        if (errorCode == 0) {
            return ERROR_TYPE_NO_ERROR;
        }
        if (errorCode == ERROR_CODE_FILE_OCCUPANCY) {
            return ERROR_TYPE_FILE_OCCUPANCY;
        }
        if (errorCode == ERROR_CODE_FILE_PERMISSION_DENY) {
            return ERROR_TYPE_FILE_PERMISSION_DENY;
        }
        if (errorCode == ERROR_CODE_FILE_MISSING) {
            return ERROR_TYPE_FILE_MISSING;
        }
        if (errorCode == ERROR_CODE_DISK_NOT_ENOUGH_SPACE) {
            return ERROR_TYPE_DISK_NOT_ENOUGH_SPACE;
        }
        return errorType;
    }

    public void setErrorType(int type) {
        this.errorType = type;
    }
}
