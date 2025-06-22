package com.kuro.game.thread;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.game.ApiConfig;
import com.kuro.game.model.launcher.item.UpdateData;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-26 22:55
 */
@Deprecated
public class BaseInfoTask extends Task<ResponseBody<UpdateData>> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseInfoTask.class);

    private String url;
    public BaseInfoTask() {
        switch (Config.setting.getGameRootDirSource()){
            case DEFAULT -> url= ApiConfig.INDEX_CN;
            case GLOBAL -> url= ApiConfig.INDEX_GLOBAL;
        }
    }

    @Override
    protected ResponseBody<UpdateData> call() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://prod-cn-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json"))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<UpdateData> responseBody = new ResponseBody<>();

                System.out.println(response.body());
                String row ="{\"hashCacheCheckAccSwitch\":1,\"default\":{\"cdnList\":[{\"K1\":1,\"K2\":1,\"P\":0,\"url\":\"https://pcdownload-huoshan.aki-game.com/\"},{\"K1\":1,\"K2\":1,\"P\":10614,\"url\":\"https://pcdownload-wangsu.aki-game.com/\"},{\"K1\":1,\"K2\":1,\"P\":1717,\"url\":\"https://pcdownload-qcloud.aki-game.com/\"},{\"K1\":1,\"K2\":1,\"P\":0,\"url\":\"https://pcdownload-aliyun.aki-game.com/\"}],\"changelog\":{},\"changelogVisible\":0,\"resources\":\"pcstarter/prod/game/G152/1.3.0/xZnXWq0k2gcdGmGs9XJgVaNGxD9OG2Ep/resource.json\",\"resourcesBasePath\":\"pcstarter/prod/game/G152/1.3.0/xZnXWq0k2gcdGmGs9XJgVaNGxD9OG2Ep/zip\",\"resourcesDiff\":{\"currentGameInfo\":{\"fileName\":\"Wuthering Waves.zip\",\"md5\":\"500cef4065674095c8fea144b921f4ff\",\"version\":\"1.3.0\"},\"previousGameInfo\":{\"fileName\":\"Wuthering Waves.zip\",\"md5\":\"95554f61e771b3f909517ee53318e61b\",\"version\":\"1.2.0\"}},\"resourcesExcludePath\":[],\"resourcesExcludePathNeedUpdate\":[],\"sampleHashSwitch\":0,\"version\":\"1.3.0\"},\"predownloadSwitch\":1,\"RHIOptionSwitch\":1,\"RHIOptionList\":[{\"cmdOption\":\"-dx12\",\"isShow\":0,\"text\":{}},{\"cmdOption\":\"-dx11\",\"isShow\":1,\"text\":{\"zh-Hans\":\"使用DX11启动（游戏异常时选择）\",\"de\":\"Start mit DX11 (bei Anomalie)\",\"zh-Hant\":\"使用DX11啟動（遊戲異常時選擇）\",\"ko\":\"DX11로 시작하기 (게임 에러 시 선택)\",\"ja\":\"DX11起動（異常発生時に選択）\",\"en\":\"Launch with DirectX 11 (in case of game anomalies)\",\"fr\":\"Démarrer en DX11 (à choisir en cas d'anomalie)\",\"es\":\"Iniciar con DirectX 11 (si hay problemas)\"}}],\"resourcesLogin\":{\"host\":\"\",\"loginSwitch\":0},\"checkExeIsRunning\":0,\"keyFileCheckSwitch\":1,\"keyFileCheckList\":[\"/Client/Binaries/Win64/Client-Win64-Shipping.exe\",\"/Client/Binaries/Win64/Client-Win64-ShippingBase.dll\",\"/Wuthering Waves.exe\"]}";
                JsonNode tree = mapper.readTree(row);
                JsonNode node = tree.get("default");

                UpdateData updateData = mapper.treeToValue(node, UpdateData.class);
                responseBody.setData(updateData);
                responseBody.setCode(200);
                return responseBody;
            }else {
                ResponseBody<UpdateData> responseBody = new ResponseBody<>();
                LOG.error("网络请求失败，错误代码：{}",response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("网络请求失败，响应状态码:" + response.statusCode());
                return responseBody;
            }
        }catch (IOException | InterruptedException e) {
            LOG.error(e.getMessage(),e);
            return new ResponseBody<>(1,e.getMessage());
        }
    }
}