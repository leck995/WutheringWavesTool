package cn.tealc.download;

/**
 * 游戏下载协议的统一响应。
 */
public final class GameDownloadResponse<T> {
    private final int code;
    private final String message;
    private final T data;

    private GameDownloadResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> GameDownloadResponse<T> success(T data) {
        return new GameDownloadResponse<>(200, "", data);
    }

    public static <T> GameDownloadResponse<T> failure(String message) {
        return new GameDownloadResponse<>(-1, message, null);
    }

    public boolean isSuccessful() {
        return code == 200 && data != null;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
