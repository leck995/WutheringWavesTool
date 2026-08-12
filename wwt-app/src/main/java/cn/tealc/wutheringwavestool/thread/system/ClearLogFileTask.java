package cn.tealc.wutheringwavestool.thread.system;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @description: 清理过期的日志
 * @author: Leck
 * @create: 2025-09-06 21:00
 */
public class ClearLogFileTask implements Runnable {
    private static final String LOG_PATH = "log";
    private static final Logger LOG = LoggerFactory.getLogger(ClearLogFileTask.class);

    @Override
    public void run() {
        File logDir = new File(LOG_PATH);
        if (!logDir.isDirectory()) {
            return;
        }
        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (logFiles == null || logFiles.length <= 7) {
            return;
        }
        Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < logFiles.length - 7; i++) {
            if (!logFiles[i].delete()) {
                LOG.warn("日志文件清理失败");
            }
        }
    }
}