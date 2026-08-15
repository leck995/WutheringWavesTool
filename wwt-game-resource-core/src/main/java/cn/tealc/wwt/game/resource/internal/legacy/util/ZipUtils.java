package cn.tealc.wwt.game.resource.internal.legacy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Zip decompression utility with progress callback.
 * Corresponds to KRSharedUtils/KRZipUtils.cs.
 * Used by ZipApplyTask to extract .krzip archives.
 */
public final class ZipUtils {
    private static final Logger log = LoggerFactory.getLogger(ZipUtils.class);

    /**
     * Progress callback: (unCompressedCount, totalCount, unCompressedSize,
     * totalSize)
     */
    public interface ProgressCallback {
        void onProgress(long unCompressedCount, long totalCount, long unCompressedSize, long totalSize);
    }

    /**
     * Result callback: (success, errorCode, errorMessage)
     */
    public interface ResultCallback {
        void onResult(boolean success, int errorCode, String errorMessage);
    }

    /**
     * Decompress a zip archive with progress reporting.
     * Matches C# KRZipUtils.DeCompressFileWithProgressCallback.
     *
     * @param archiveFile      path to the .zip/.krzip file
     * @param unZipPath        destination directory for extracted files
     * @param progressCallback called with progress updates
     * @param resultCallback   called when extraction completes or fails
     */
    public static void deCompressFileWithProgressCallback(
            String archiveFile,
            String unZipPath,
            ProgressCallback progressCallback,
            ResultCallback resultCallback) {
        try {
            File destDir = new File(unZipPath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            try (ZipFile zipFile = new ZipFile(archiveFile)) {
                int totalCount = zipFile.size();
                long totalSize = 0;
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                // First pass: calculate total size
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    totalSize += entry.getSize() > 0 ? entry.getSize() : 0;
                }

                progressCallback.onProgress(0, totalCount, 0, totalSize);

                long unCompressedSize = 0;
                int index = 0;
                Enumeration<? extends ZipEntry> entries2 = zipFile.entries();
                while (entries2.hasMoreElements()) {
                    ZipEntry entry = entries2.nextElement();
                    long entrySize = entry.getSize() > 0 ? entry.getSize() : 0;
                    unCompressedSize += entrySize;

                    Path fullPath = Path.of(unZipPath, entry.getName());

                    if (entry.isDirectory()) {
                        Files.createDirectories(fullPath);
                    } else {
                        File parent = fullPath.getParent().toFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            Files.copy(is, fullPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                    // C# KRZipUtils.cs#L53: OnProgressCallback(i, count, num2, num)
                    // reports the 0-based index of the just-processed entry, NOT the
                    // count of processed entries. Match C# exactly to avoid progress
                    // reporting divergence (C# never reaches count/count = 100%).
                    progressCallback.onProgress(index, totalCount, unCompressedSize, totalSize);
                    index++;
                }

                resultCallback.onResult(true, 0, "");
            }
        } catch (IOException e) {
            log.error("DeCompress File Fail, ErrorCode: {}, ErrorMessage: {}",
                    ExceptionUtils.getHResult(e), e.getMessage(), e);
            resultCallback.onResult(false, ExceptionUtils.getHResult(e), e.getMessage());
        }
    }

    private ZipUtils() {
    }
}
