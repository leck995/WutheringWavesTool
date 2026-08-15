package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.ResourceOperationState;
import cn.tealc.wwt.game.resource.model.ResourceProgress;

/** Receives operation events on the worker threads executing the operation. */
public interface ResourceOperationListener {
    default void onStateChanged(ResourceOperationState state) {
    }

    default void onProgress(ResourceProgress progress) {
    }
}
