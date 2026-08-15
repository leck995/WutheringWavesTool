package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.ResourceOperationResult;

@FunctionalInterface
public interface ResourceCompletionListener {
    void onComplete(ResourceOperationResult result);
}
