package cn.tealc.wwt.game.resource.internal.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Index file structure.
 * Corresponds to KRIndexFile.cs
 *
 * The Resource property in C# returns groupResource when
 * applyMethodFeature=="group",
 * otherwise returns normalResource.
 */
public class IndexFile {
    /**
     * Apply type used by the factory to select the correct resource list.
     * Set by IndexFileFactory. Not serialized in JSON.
     */
    public transient String applyType = "";

    @JsonProperty("resource")
    public List<FileInfo> normalResource;

    @JsonProperty("groupResource")
    public List<FileInfo> groupResource;

    @JsonProperty("deleteFiles")
    public List<String> deleteFiles;

    @JsonProperty("patchInfos")
    public List<MixedFileInfo> patchInfos;

    @JsonProperty("zipInfos")
    public List<MixedFileInfo> zipInfos;

    @JsonProperty("groupInfos")
    public List<GroupFileInfo> groupInfos;

    @JsonProperty("applyTypes")
    public List<String> applyTypes;

    /**
     * Get resource list based on the applyType field set by IndexFileFactory.
     * Corresponds to C# KRIndexFile.Resource property getter, which uses
     * _targetApplyType to select between normalResource and groupResource.
     */
    public List<FileInfo> getResource() {
        return getResource(applyType);
    }

    /**
     * Get resource list based on explicit apply type.
     */
    public List<FileInfo> getResource(String applyType) {
        if ("group".equals(applyType) && applyTypes != null
                && applyTypes.contains(applyType) && groupResource != null) {
            return groupResource;
        }
        return normalResource;
    }
}
