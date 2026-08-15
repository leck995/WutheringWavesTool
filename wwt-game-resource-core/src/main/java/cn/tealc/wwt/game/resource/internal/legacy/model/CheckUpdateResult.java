package cn.tealc.wwt.game.resource.internal.legacy.model;

import java.util.List;
import java.util.Map;

public class CheckUpdateResult {
    public boolean succ = true;
    public int errorCode;
    public String errorMessage = "";
    public ResStateInfo stateInfo;
    public UpdateInfo updateInfo;
    public UpdateInfo predownloadUpdateInfo;
    public int cSharpErrorCode;
    public List<Object> rhiConfigInfos;
    public Map<String, Object> functionCode;
}
