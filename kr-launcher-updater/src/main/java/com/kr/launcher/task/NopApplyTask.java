package com.kr.launcher.task;

import com.kr.launcher.model.FileInfo;
import com.kr.launcher.model.IndexFile;
import com.kr.launcher.model.MoveFileRecord;
import com.kr.launcher.model.UpdateInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * No-op apply task: directly creates a move file record from the resource list
 * without any patch/zip/group processing.
 * Corresponds to KRResources/KRNopApplyTask.cs.
 *
 * Used by UpdateFlow when the update type doesn't require decompression
 * (i.e., the downloaded files are the final files and just need to be moved).
 */
public class NopApplyTask extends ApplyTask {

    public NopApplyTask(UpdateInfo updateInfo, String gameDirPath, String downloadResPath,
            IndexFile updateIndexFile, List<FileInfo> updateInfoFileList,
            ApplyProgressCallBack progressCallback, ApplyResultCallback resultCallback) {
        super(updateInfo, gameDirPath, downloadResPath, updateIndexFile, updateInfoFileList,
                progressCallback, resultCallback);
    }

    @Override
    public void applyFiles() {
        List<FileInfo> moveFileInfos;
        if (updateIndexFile == null || updateIndexFile.getResource() == null || updateInfoFileList == null) {
            moveFileInfos = new ArrayList<>();
        } else {
            moveFileInfos = updateIndexFile.getResource();
        }

        MoveFileRecord record = new MoveFileRecord(downloadResPath, moveFileInfos);

        moveFileRecords = new ArrayList<>();
        moveFileRecords.add(record);
        isFinished = true;
        onApplyResultCallback(true, moveFileRecords, null, 0, "");
    }

    @Override
    public long getNeedDiskSize() {
        return 0L;
    }
}
