package cn.tealc.wwt.game.resource.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceProgressTest {

    @Test
    void mapsLegacyProgressStatesToPublicPhases() {
        assertEquals(ResourceOperationPhase.VERIFYING, progress(0).operationPhase());
        assertEquals(ResourceOperationPhase.DOWNLOADING, progress(1).operationPhase());
        assertEquals(ResourceOperationPhase.APPLYING, progress(5).operationPhase());
        assertEquals(ResourceOperationPhase.VERIFYING, progress(9).operationPhase());
        assertEquals(ResourceOperationPhase.UNKNOWN, progress(99).operationPhase());
    }

    private static ResourceProgress progress(int nativeState) {
        return new ResourceProgress(nativeState, 0, 0, 0, 0);
    }
}
