package cn.tealc.wutheringwavestool.model;

import java.util.ArrayList;
import java.util.List;

/** Persisted configuration for one physical game installation. */
public final class GameInstallation {
    private String id;
    private String name;
    private GameEdition edition;
    private SourceType source;
    private String gameDir;
    private String version;
    private String startAppPath;
    private boolean startAppCustom;
    private String officialLauncherDir;
    private List<String> startUpParams = new ArrayList<>();

    public GameInstallation() {
    }

    public GameInstallation(String id, String name, GameEdition edition, SourceType source) {
        this.id = id;
        this.name = name;
        this.edition = edition;
        this.source = source;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public GameEdition getEdition() { return edition; }
    public void setEdition(GameEdition edition) { this.edition = edition; }
    public SourceType getSource() { return source; }
    public void setSource(SourceType source) { this.source = source; }
    public String getGameDir() { return gameDir; }
    public void setGameDir(String gameDir) { this.gameDir = gameDir; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStartAppPath() { return startAppPath; }
    public void setStartAppPath(String startAppPath) { this.startAppPath = startAppPath; }
    public boolean isStartAppCustom() { return startAppCustom; }
    public void setStartAppCustom(boolean startAppCustom) { this.startAppCustom = startAppCustom; }
    public String getOfficialLauncherDir() { return officialLauncherDir; }
    public void setOfficialLauncherDir(String officialLauncherDir) { this.officialLauncherDir = officialLauncherDir; }
    public List<String> getStartUpParams() {
        if (startUpParams == null) {
            startUpParams = new ArrayList<>();
        }
        return startUpParams;
    }
    public void setStartUpParams(List<String> startUpParams) {
        this.startUpParams = startUpParams != null ? new ArrayList<>(startUpParams) : new ArrayList<>();
    }
}
