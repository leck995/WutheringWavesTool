package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.ResourceProgress;

@FunctionalInterface
public interface ResourceProgressListener {
    void onProgress(ResourceProgress progress);
}
