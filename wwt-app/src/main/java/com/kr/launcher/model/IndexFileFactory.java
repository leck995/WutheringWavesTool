package com.kr.launcher.model;

import com.kr.launcher.util.JsonUtils;

/**
 * Factory that creates IndexFile from JSON content with applyType-based
 * resource selection.
 * Corresponds to KRResources/KRIndexFileFactory.cs.
 *
 * The C# version deserializes into a temp KRIndexFile, then constructs a new
 * one with the applyType set so that getResource(applyType) returns the correct
 * resource list (patch vs group vs zip).
 *
 * In Java, IndexFile.getResource(applyType) already handles applyType-based
 * selection, so this factory simply deserializes and returns the IndexFile.
 */
public class IndexFileFactory {

    private final String content;
    private final String applyType;

    public IndexFileFactory(String content, String applyType) {
        this.content = content;
        this.applyType = applyType;
    }

    /**
     * Create an IndexFile from the JSON content.
     * Corresponds to C# KRIndexFileFactory.Create which uses
     * JsonUtils.Deserialize (throwing) — parse errors propagate to the caller.
     * @return IndexFile (with applyType set) or null for null/empty content
     * @throws com.google.gson.JsonSyntaxException on malformed JSON
     */
    public IndexFile create() {
        if (content == null || content.isEmpty()) {
            return null;
        }
        // C# KRIndexFileFactory.cs:19: JsonUtils.Deserialize<KRIndexFile>(_content)
        // — no try/catch, throws on malformed JSON. ResourceHelper.parseFileIndexInfo
        // wraps this in a try/catch and reports failure via callback.
        IndexFile indexFile = JsonUtils.deserialize(content, IndexFile.class);
        if (indexFile != null) {
            indexFile.applyType = applyType;
        }
        return indexFile;
    }
}
