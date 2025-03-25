package cn.tealc.wutheringwavestool.model.ui;

import cn.tealc.wutheringwavestool.model.SourceType;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-08 23:55
 */
public class ServerData {
    private SourceType type;
    private boolean exit;

    public ServerData(SourceType type, boolean exit) {
        this.type = type;
        this.exit = exit;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", type.name(), exit ? "存在":"不存在");
    }
}