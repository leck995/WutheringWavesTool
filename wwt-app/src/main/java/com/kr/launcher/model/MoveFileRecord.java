package com.kr.launcher.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One batch of files to move from a common source directory.
 * Corresponds to KRMoveFileRecord.cs.
 *
 * Tracks per-file move results so that:
 *  - already-moved files can be skipped on resume (GetMoveFileResult == true)
 *  - the move task knows which files were actually moved this run
 */
public class MoveFileRecord {
    public String srcDir;
    public List<FileInfo> fileInfos;

    /** path -> movedSuccessfully. False initially; set true after a successful move. */
    private transient Map<String, Boolean> moveFileRecordMap = new HashMap<>();

    public MoveFileRecord(String srcDir, List<FileInfo> fileInfos) {
        this.srcDir = srcDir;
        this.fileInfos = fileInfos;
        if (fileInfos != null) {
            for (FileInfo fi : fileInfos) {
                if (fi != null && fi.path != null) {
                    // C# KRMoveFileRecord.cs uses Dictionary.Add which throws on duplicate key.
                    if (moveFileRecordMap.containsKey(fi.path)) {
                        throw new IllegalArgumentException("Duplicate file path in MoveFileRecord: " + fi.path);
                    }
                    moveFileRecordMap.put(fi.path, false);
                }
            }
        }
    }

    /** Mark a file's move result. */
    public void updateMoveFileRecord(String filePath, boolean moveResult) {
        moveFileRecordMap.put(filePath, moveResult);
    }

    /** Returns true if the file was already moved successfully in a prior call. */
    public boolean getMoveFileResult(String filePath) {
        Boolean result = moveFileRecordMap.get(filePath);
        return result != null && result;
    }
}
