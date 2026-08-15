package cn.tealc.wwt.game.resource.model;

/** Terminal result for update, repair, and pre-download operations. */
public record ResourceOperationResult(boolean successful, int errorCode, String errorMessage,
        int nativeState) {
    public ResourceOperationResult {
        errorMessage = errorMessage != null ? errorMessage : "";
    }

    public static ResourceOperationResult failure(String message) {
        return new ResourceOperationResult(false, 0, message, 0);
    }
}
