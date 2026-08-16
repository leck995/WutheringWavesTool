package cn.tealc.wutheringwavestool.theme;

import cn.tealc.wutheringwavestool.FXResourcesLoader;
import javafx.scene.Node;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局字体管理：统一决定应用使用的字体族，负责内置字体的加载，
 * 并负责把当前字体应用到主窗口、弹窗等各个场景。
 */
public final class FontManager {
    private static final Logger LOG = LoggerFactory.getLogger(FontManager.class);

    /** 系统默认字体：优先微软雅黑 */
    private static final String DEFAULT_FONT_FAMILY = "Microsoft YaHei";
    /** 无微软雅黑时的内置回退字体 */
    private static final String FALLBACK_FONT_FAMILY = "HarmonyOS Sans SC";
    private static final String BUNDLED_FONT_PATH = "font/HarmonyOS_Sans_SC_Bold.ttf";

    /** 用户手动指定的字体族；为 null 时按系统情况自动选择。 */
    private static volatile String customFontFamily;

    private FontManager() {
    }

    /** 返回当前实际生效的字体族。 */
    public static String getFontFamily() {
        if (customFontFamily != null) {
            return customFontFamily;
        }
        return Font.getFamilies().contains(DEFAULT_FONT_FAMILY)
                ? DEFAULT_FONT_FAMILY
                : FALLBACK_FONT_FAMILY;
    }

    /** 指定全局字体族（例如设置界面选择）；传 null 恢复为自动选择。 */
    public static void setFontFamily(String fontFamily) {
        customFontFamily = fontFamily;
    }

    /** 确保字体可用：系统无微软雅黑时加载内置字体。 */
    public static void ensureDefaultLoaded() {
        if (FALLBACK_FONT_FAMILY.equals(getFontFamily())) {
            LOG.info("默认字体不存在，加载内置字体");
            Font.loadFonts(FXResourcesLoader.loadStream(BUNDLED_FONT_PATH), 12);
        }
    }

    /** 将当前字体应用到节点，字体族沿节点向下继承。 */
    public static void applyTo(Node node) {
        node.setStyle("-fx-font-family: \"" + getFontFamily() + "\";");
    }
}
