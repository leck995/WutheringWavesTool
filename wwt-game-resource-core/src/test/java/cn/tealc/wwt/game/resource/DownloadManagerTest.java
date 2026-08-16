package cn.tealc.wwt.game.resource;

import cn.tealc.wwt.game.resource.model.DownloadInfo;
import cn.tealc.wwt.game.resource.model.DownloadState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void encodesSpacesWhenResolvingCdnUrl() {
        DownloadInfo info = DownloadInfo.of(
                "Client/Binaries/Win64/ThirdParty/KrPcSdk_Mainland/KRSDKRes/res_font/Source Han Sans CN Regular.otf",
                "font.otf", 1, null);

        List<String> urls = DownloadManager.resolveUrlsByBase(info, List.of("https://cdn.example/resources/"));

        assertEquals(List.of("https://cdn.example/resources/Client/Binaries/Win64/ThirdParty/"
                + "KrPcSdk_Mainland/KRSDKRes/res_font/Source%20Han%20Sans%20CN%20Regular.otf"), urls);
    }

    @Test
    void reportsMalformedUrlAsFailedInsteadOfCompleted() throws Exception {
        DownloadInfo info = DownloadInfo.of("https://cdn.example/file with space.otf", "font.otf", 1, null);
        DownloadManager manager = new DownloadManagerBuilder(List.of(info), temporaryDirectory)
                .maxParallel(1)
                .maxRetry(0)
                .build();
        AtomicReference<DownloadState> state = new AtomicReference<>();
        manager.setStateListener((nextState, message) -> state.set(nextState));

        manager.run();

        assertEquals(DownloadState.FAILED, state.get());
        assertEquals(DownloadState.FAILED, manager.getItems().getFirst().getState());
    }
}
