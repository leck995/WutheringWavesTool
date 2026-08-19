package com.kr.launcher.model;

/**
 * Resource error code constants.
 * Corresponds to KRResources/KRResourceError.cs.
 * Used by PrepareTask, UpdateFlow, RepairFlow, ResCheckFlow, etc.
 */
public final class ResourceError {
    public static final int NO_ERROR = 0;

    public static final int GAME_CONFIG_PARSE_ERROR = 7002001;

    public static final int LAUNCHER_CACHE_DIR_EMPTY = 7002002;

    public static final int GAME_CONFIG_INIT_FAIL = 7002003;

    public static final int UPDATE_FLOW_RES_STATE_EMPTY = 7002004;

    public static final int UPDATE_FLOW_UPDATE_INFO_INVALID = 7002005;

    public static final int CHUNK_CHECK_ERROR_FILE_NOT_EXIST = 7002006;

    public static final int CHUNK_CHECK_ERROR_FILE_MD5_NOT_MATCH = 7002007;

    public static final int CHUNK_CHECK_ERROR_FILE_SIZE_NOT_MATCH = 7002008;

    public static final int PARSE_ORIGIN_INDEX_FILE_ERROR = 7002009;

    public static final int REPAIR_FLOW_WITHOUT_GAME_SERVER_CONFIG = 7002011;

    public static final int REPAIR_FLOW_WITH_WRONG_GAME_SERVER_CONFIG = 7002012;

    public static final int REPAIR_FLOW_UPDATE_INFO_INVALID = 7002013;

    public static final int CHECK_DISK_SPACE_NOT_ENOUGH = -2147024784;

    public static final int UNKNOWN_ERROR = 7002015;

    public static final int PATCH_PROCESS_IS_RUNNING = 7002016;

    public static final int APPLY_CHECK_MD5_NOT_MATCH = 7002017;

    public static final int CHECK_LOCAL_STATE_FAIL = 7002018;

    public static final int START_PATCH_PROCESS_FAILED = 7002019;

    private ResourceError() {
    }
}
