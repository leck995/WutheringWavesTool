package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.jna.GameAppListener;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class GameLaunchService {
    private static final Logger LOG = LoggerFactory.getLogger(GameLaunchService.class);

    private final GameInstallationManager installationManager;

    @Inject
    public GameLaunchService(GameInstallationManager installationManager) {
        this.installationManager = installationManager;
    }

    /** 删除游戏日志文件，确保每次启动日志是最新的 */
    public void deleteLogFiles() {
        File dir = GameResourcesManager.getGameLogDir();
        if (dir != null && dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }

    /** 判断启动来源是否有效 */
    public boolean canLaunch() {
        return Config.setting().getGameRootDirSource() != SourceType.WE_GAME;
    }

    /** 获取游戏可执行文件 */
    public File resolveGameExecutable() {
        String dir = Config.setting().getGameRootDir();
        if (dir == null) return null;

        if (Config.setting().isGameStartAppCustom()) {
            File exe = new File(Config.setting().getGameStarAppPath());
            return exe.exists() ? exe : null;
        }
        return GameResourcesManager.getGameExeBase();
    }

    /** 组装启动参数 */
    public String[] buildLaunchCommand(File exe) {
        List<String> paramsList = new ArrayList<>(installationManager.activeStartUpParams());
        if (!paramsList.isEmpty()) {
            paramsList.addFirst(exe.getAbsolutePath());
            return paramsList.toArray(new String[0]);
        }
        return new String[]{exe.getAbsolutePath()};
    }

    /** 高级自定义启动（使用 cmd.exe 提权） */
    public void launchWithCustomParams(String... params) {
        String[] mergedArray = Stream.concat(
                Stream.of("cmd.exe", "/c", "start", "\"\""),
                Stream.of(params)
        ).toArray(String[]::new);
        ProcessBuilder pb = new ProcessBuilder(mergedArray);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        String path = params[0];
        if (path.contains("WWMI Loader.exe")) {
            File file = new File(path);
            if (file.exists() && file.getParentFile().exists()) {
                pb.directory(file.getParentFile());
            }
        }

        try {
            GameAppListener.getInstance().setStartFromApp(true);
            pb.start();
        } catch (IOException e) {
            GameAppListener.getInstance().setStartFromApp(false);
            LOG.error("高级启动无法启动鸣潮", e);
            throw new RuntimeException("启动失败: " + e.getMessage());
        }
    }

    /** 默认启动（使用 Desktop.open） */
    public void launchDefault(File exe) throws IOException {
        GameAppListener.getInstance().setStartFromApp(true);
        Desktop.getDesktop().open(exe);
    }
}
