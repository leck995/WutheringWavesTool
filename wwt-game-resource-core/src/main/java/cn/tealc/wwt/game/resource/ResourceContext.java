package cn.tealc.wwt.game.resource;

import java.nio.file.Path;
import java.util.Objects;

/** Identifies one installed game and the working directory used by native patch tools. */
public record ResourceContext(Path gameDirectory, Path workingDirectory) {
    public ResourceContext {
        gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory")
                .toAbsolutePath().normalize();
        workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory")
                .toAbsolutePath().normalize();
    }

    public static ResourceContext forGame(Path gameDirectory) {
        return new ResourceContext(gameDirectory, Path.of("."));
    }
}
