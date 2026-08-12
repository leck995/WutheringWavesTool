package com.kuro.game.model.launcher.item;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-10 16:44
 */
public class ResourcesDiff {
    public GameInfo currentGameInfo;
    public GameInfo previousGameInfo;

    public GameInfo getCurrentGameInfo() {
        return currentGameInfo;
    }

    public void setCurrentGameInfo(GameInfo currentGameInfo) {
        this.currentGameInfo = currentGameInfo;
    }

    public GameInfo getPreviousGameInfo() {
        return previousGameInfo;
    }

    public void setPreviousGameInfo(GameInfo previousGameInfo) {
        this.previousGameInfo = previousGameInfo;
    }
}