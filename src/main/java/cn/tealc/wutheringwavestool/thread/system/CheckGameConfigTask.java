package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 检测鸣潮日志功能是否被关闭
 * @author: Leck
 * @create: 2025-01-04 15:01
 */
public class CheckGameConfigTask extends Task<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(CheckGameConfigTask.class);
    @Override
    protected Boolean call() throws Exception {
        File engineIni = GameResourcesManager.getGameEngineIni();
        if (engineIni == null) {
            return true; // 如果 engineIni 为 null，返回 true
        }

        List<String> updatedLines = new ArrayList<>();
        boolean isOpenLog = true;

        // 读取配置文件
        try (BufferedReader br = new BufferedReader(new FileReader(engineIni))) {
            String line;
            while ((line = br.readLine()) != null) {
                String normalizedLine = line.toLowerCase().replaceAll("\\s+", "");
                if (normalizedLine.contains("global=")) {
                    // 如果找到 global=，则不添加到更新后的内容中
                    isOpenLog = false;
                } else {
                    // 添加不包含 global= 的行
                    updatedLines.add(line);
                }
            }
        } catch (IOException e) {
            LOG.error("读取配置文件失败", e);
        }

        // 将更新后的内容写回文件
        try {
            Files.write(engineIni.toPath(), updatedLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOG.error("更新配置文件失败", e);
        }

        return isOpenLog;
    }
}