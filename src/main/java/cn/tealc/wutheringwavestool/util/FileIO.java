package cn.tealc.wutheringwavestool.util;

import java.io.File;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-23 23:02
 */
public class FileIO {
    public static boolean rename(File file, String newName, boolean overwrite) {
        if (!file.exists()) return false;
        File newFile = new File(file.getParent(), newName);
        if (overwrite && newFile.exists()) {
            newFile.delete();
        }
        return file.renameTo(newFile);

    }
}