package cn.tealc.wwt.game.resource;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Identifies one installed game, the working directory used by native patch tools,
 * the download cache directory (where temporary resource files are staged), and the
 * release channel used to resolve the launcher configuration.
 * The cache directory defaults to {@code gameDirectory/launcherDownload}, but may be
 * overridden to a custom path (e.g. on a different volume) to conserve disk space on
 * the game volume.
 */
public record ResourceContext(Path gameDirectory, Path workingDirectory, Path cacheDirectory,
        GameDownloadSource source) {
    private static final String DEFAULT_CACHE_FOLDER = "launcherDownload";

    public ResourceContext {
        gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath().normalize();
        workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory")
                .toAbsolutePath().normalize();
        cacheDirectory = cacheDirectory == null
                ? gameDirectory.resolve(DEFAULT_CACHE_FOLDER)
                : cacheDirectory.toAbsolutePath().normalize();
        source = source == null ? GameDownloadSource.MAINLAND : source;
    }

    public static ResourceContext forGame(Path gameDirectory) {
        return new ResourceContext(gameDirectory, Path.of("."), null, GameDownloadSource.MAINLAND);
    }
}