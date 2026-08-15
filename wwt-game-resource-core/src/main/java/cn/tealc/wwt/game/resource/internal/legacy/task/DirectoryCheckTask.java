package cn.tealc.wwt.game.resource.internal.legacy.task;

import cn.tealc.wwt.game.resource.internal.legacy.model.DirectoryCheckEntry;
import cn.tealc.wwt.game.resource.internal.legacy.model.FileInfo;
import cn.tealc.wwt.game.resource.internal.legacy.model.IndexFile;
import cn.tealc.wwt.game.resource.internal.legacy.util.FileUtils;
import cn.tealc.wwt.game.resource.internal.legacy.util.PathUtils;
import cn.tealc.wwt.game.resource.internal.legacy.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds and deletes redundant files not in the index.
 * Corresponds to KRDirectoryCheckTask.cs.
 *
 * Walks configured directories (with optional extensions and recursion) and
 * deletes any file that is not part of the index's resource list. Used by
 * KRRepairFlow after a repair to clean up leftover files.
 */
public class DirectoryCheckTask {
    private static final Logger log = LoggerFactory.getLogger(DirectoryCheckTask.class);

    public enum CheckState {
        NOT_NEED_CHECK,
        CHECK_FAILED,
        CHECK_SUCCESS
    }

    public interface CheckDirectoryResultCallback {
        void onResult(CheckState checkState, int cSharpErrorCode);
    }

    private final String baseDestPath;
    private final IndexFile indexFile;
    private final List<DirectoryCheckEntry> directoryCheckEntries;
    private final CheckDirectoryResultCallback checkResultCallback;

    public DirectoryCheckTask(String baseDestPath,
            IndexFile indexFile,
            List<DirectoryCheckEntry> checkEntries,
            CheckDirectoryResultCallback checkResultCallback) {
        this.baseDestPath = baseDestPath;
        this.indexFile = indexFile;
        this.directoryCheckEntries = checkEntries;
        this.checkResultCallback = checkResultCallback;
    }

    public void run() {
        Thread thread = new Thread(() -> {
            List<String> redundantFiles;
            try {
                redundantFiles = findRedundantFiles();
            } catch (Exception e) {
                log.error("Failed to calc redundant files", e);
                checkResultCallback.onResult(CheckState.CHECK_FAILED, ExceptionUtils.getHResult(e));
                return;
            }

            if (redundantFiles.isEmpty()) {
                checkResultCallback.onResult(CheckState.NOT_NEED_CHECK, 0);
                return;
            }

            try {
                // C# KRDirectoryCheckTask.cs:79-85 uses File.Delete (throws on failure).
                // Java's File.delete() returns false silently; use Files.delete via
                // FileUtils.deleteFile which throws and also resets readonly attribute.
                for (String file : redundantFiles) {
                    FileUtils.deleteFile(file);
                }
            } catch (Exception e) {
                log.error("Failed to delete redundant files", e);
                checkResultCallback.onResult(CheckState.CHECK_FAILED, ExceptionUtils.getHResult(e));
                return;
            }
            checkResultCallback.onResult(CheckState.CHECK_SUCCESS, 0);
        }, "DirectoryCheckTask");
        thread.setDaemon(true);
        thread.start();
    }

    private List<String> searchEligibleFiles() throws IOException {
        List<String> result = new ArrayList<>();
        if (directoryCheckEntries == null)
            return result;

        for (DirectoryCheckEntry entry : directoryCheckEntries) {
            if (entry.directory == null || entry.directory.trim().isEmpty()) {
                continue;
            }
            String checkPath = PathUtils.normalize(PathUtils.combine(baseDestPath, entry.directory));
            if (!new File(checkPath).exists()) {
                log.info("Path checkPath:{} does not exist in the game directory", checkPath);
                continue;
            }

            List<String> extensions = entry.extensions != null && !entry.extensions.isEmpty()
                    ? entry.extensions
                    : java.util.Collections.singletonList("*");

            Path startDir = Paths.get(checkPath);
            for (String ext : extensions) {
                try {
                    String glob = ext.equals("*") ? "*" : "*." + ext;
                    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
                    if (entry.recursive) {
                        Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (ext.equals("*") || matcher.matches(file.getFileName())) {
                                    result.add(file.toAbsolutePath().toString());
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        try (DirectoryStream<Path> ds = Files.newDirectoryStream(startDir, glob)) {
                            for (Path p : ds) {
                                result.add(p.toAbsolutePath().toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to enumerate all files in checkPath:{} with extension:{}", checkPath, ext, e);
                    throw e;
                }
            }
        }
        return result;
    }

    private Set<String> calcVersionedFileSet() {
        List<FileInfo> resource = indexFile != null ? indexFile.getResource() : null;
        if (resource == null || resource.isEmpty()) {
            log.error("_indexFile.Resource is invalid");
            return null;
        }
        Set<String> set = new HashSet<>();
        for (FileInfo fi : resource) {
            String versionedFilePath = PathUtils.normalize(
                    PathUtils.combine(baseDestPath, fi.path));
            try {
                set.add(new File(versionedFilePath).getAbsolutePath());
            } catch (Exception e) {
                log.warn("Failed to access versionedFilePath:{}", versionedFilePath, e);
                throw e;
            }
        }
        return set;
    }

    private List<String> findRedundantFiles() throws IOException {
        List<String> redundant = new ArrayList<>();
        Set<String> versionedFiles = calcVersionedFileSet();
        if (versionedFiles == null) {
            return redundant;
        }
        for (String file : searchEligibleFiles()) {
            boolean isVersioned = false;
            for (String v : versionedFiles) {
                if (v.equalsIgnoreCase(file)) {
                    isVersioned = true;
                    break;
                }
            }
            if (!isVersioned) {
                redundant.add(file);
            }
        }
        return redundant;
    }
}
