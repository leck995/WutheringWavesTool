package cn.tealc.wwt.game.resource;

/** Receives server-switch progress on the worker thread that performs the operation. */
@FunctionalInterface
public interface ServerSwitchProgressListener {
    void onProgress(ServerSwitchPhase phase, long completedBytes, long totalBytes, String detail);

    static ServerSwitchProgressListener noop() {
        return (phase, completedBytes, totalBytes, detail) -> { };
    }
}
