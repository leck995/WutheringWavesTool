package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.release.Release;
import cn.tealc.wutheringwavestool.model.release.ReleaseList;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AppCheckVersionTask extends Task<ResponseBody<Release>> {
    private static final Logger LOG= LoggerFactory.getLogger(AppCheckVersionTask.class);

    private static final String TIP="发现新版本：%s,可在设置中获取更新详细信息";
    private static final String TIP_ERROR="检查版本更新失败，请检查网络状况";

    private final boolean checkSkip;//是否检测跳过的版本,从设置中过来的无需检测跳过
    public AppCheckVersionTask(boolean checkSkip) {
        this.checkSkip = checkSkip;
    }

    @Override
    protected ResponseBody<Release> call() throws Exception {
        ResponseBody<Release> releaseData = getNetReleaseData();
        if (releaseData != null) return releaseData;
        return new ResponseBody<>(-1,"无法检测更新");
    }

    private ResponseBody<Release> getNetReleaseData() {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        boolean isDev = Config.setting().isDevModel();
        String[] urls = isDev
                ? new String[]{AppConstants.URL_APP_UPDATE_DEV, AppConstants.URL_APP_UPDATE_DEV_2}
                : new String[]{AppConstants.URL_APP_UPDATE, AppConstants.URL_APP_UPDATE_2};

        for (int i = 0; i < urls.length; i++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urls[i])).GET().timeout(Duration.ofSeconds(8)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                    ReleaseList releaseList = mapper.readValue(response.body(), ReleaseList.class);
                    if (releaseList != null) {
                        Release latestRelease = releaseList.getLatestRelease();
                        String version = latestRelease.getVersion();
                        double net = Double.parseDouble(version.replace(".", ""));
                        double now = Double.parseDouble(AppConstants.VERSION.replace(".", ""));
                        if (checkSkip && Config.setting().getSkipVersion() != null) {
                            double skip = Double.parseDouble(Config.setting().getSkipVersion().replace(".", ""));
                            if (net <= skip) {
                                LOG.info("检测到跳过版本更新");
                                return new ResponseBody<>(1, "无更新");
                            }
                        }

                        if (now < net) {
                            LOG.info("检测到有新版本需要更新");
                            ResponseBody<Release> body = new ResponseBody<>();
                            body.setCode(200);
                            body.setData(latestRelease);
                            return body;
                        } else {
                            LOG.info("检测到无新版本需要更新");
                            return new ResponseBody<>(1, "无更新");
                        }
                    }
                } else if (response.statusCode() == 404) {
                    if (i < urls.length - 1) {
                        LOG.warn("{} 返回404，尝试备用地址", urls[i]);
                        continue;
                    }
                    return new ResponseBody<>(404, "找不到更新信息：404");
                } else {
                    LOG.error("{} 返回状态码: {}", urls[i], response.statusCode());
                    if (i < urls.length - 1) {
                        LOG.warn("尝试备用地址");
                        continue;
                    }
                    return new ResponseBody<>(-1, "无法检测更新，错误代码：" + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LOG.error("{} 请求失败", urls[i], e);
                if (i < urls.length - 1) {
                    LOG.warn("尝试备用地址");
                    continue;
                }
                return new ResponseBody<>(-1, "无法检测更新");
            }
        }
        return new ResponseBody<>(-1, "无法检测更新");
    }
}