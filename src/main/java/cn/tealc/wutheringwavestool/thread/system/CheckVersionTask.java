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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CheckVersionTask extends Task<ResponseBody<Release>> {
    private static final Logger LOG= LoggerFactory.getLogger(CheckVersionTask.class);

    private static final String TIP="发现新版本：%s,可在设置中获取更新详细信息";
    private static final String TIP_ERROR="检查版本更新失败，请检查网络状况";

    private final boolean checkSkip;//是否检测跳过的版本,从设置中过来的无需检测跳过
    public CheckVersionTask(boolean checkSkip) {
        this.checkSkip = checkSkip;
    }

    @Override
    protected ResponseBody<Release> call() throws Exception {
        if (Config.setting.isDevModel()){
            return getLocalReleaseData();
        }else {
            ResponseBody<Release> releaseData = getNetReleaseData();
            if (releaseData != null) return releaseData;
        }
        return new ResponseBody<>(-1,"无法检测更新");
    }


    private ResponseBody<Release> getLocalReleaseData() {
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
        ReleaseList releaseList = null;
        try {
            releaseList = mapper.readValue(new File("release.json"), ReleaseList.class);
        } catch (IOException e) {
            LOG.error("检测更新出现异常",e);
            return new ResponseBody<>(-1, "无法检测更新");
        }
        if (releaseList != null) {
            if (!true){ //开启预览版时

            }else {
                Release latestRelease = releaseList.getLatestRelease();
                String version = latestRelease.getVersion();
                double net = Double.parseDouble(version.replace(".",""));
                double now = Double.parseDouble(AppConstants.VERSION.replace(".",""));
                if (checkSkip && Config.setting.getSkipVersion() != null){
                    double skip = Double.parseDouble(Config.setting.getSkipVersion().replace(".",""));
                    if (net <= skip){ //网络版本低于跳过版本
                        LOG.info("检测到跳过版本更新");
                        return new ResponseBody<>(1, "无更新");
                    }
                }

                if (now < net){
                    LOG.info("检测到有新版本需要更新");
                    ResponseBody<Release> body = new ResponseBody<>();
                    body.setCode(200);
                    body.setData(latestRelease);
                    return body;
                }else {
                    LOG.info("检测到无新版本需要更新");
                    return new ResponseBody<>(1, "无更新");
                }
            }
        }
        return new ResponseBody<>(-1,"无法检测更新");
    }

    private ResponseBody<Release> getNetReleaseData() {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AppConstants.URL_APP_UPDATE)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
                ReleaseList releaseList = mapper.readValue(response.body(), ReleaseList.class);
                if (releaseList != null) {
                    if (!true){ //开启预览版时

                    }else {
                        Release latestRelease = releaseList.getLatestRelease();
                        String version = latestRelease.getVersion();
                        double net = Double.parseDouble(version.replace(".",""));
                        double now = Double.parseDouble(AppConstants.VERSION.replace(".",""));
                        if (checkSkip && Config.setting.getSkipVersion() != null){
                            double skip = Double.parseDouble(Config.setting.getSkipVersion().replace(".",""));
                            if (net <= skip){ //网络版本低于跳过版本
                                LOG.info("检测到跳过版本更新");
                                return new ResponseBody<>(1, "无更新");
                            }
                        }

                        if (now < net){
                            LOG.info("检测到有新版本需要更新");
                            ResponseBody<Release> body = new ResponseBody<>();
                            body.setCode(200);
                            body.setData(latestRelease);
                            return body;
                        }else {
                            LOG.info("检测到无新版本需要更新");
                            return new ResponseBody<>(1, "无更新");
                        }
                    }
                }
            }else if (response.statusCode() == 404){
                return new ResponseBody<>(404, "找不到更新信息");
            }else {
                LOG.error("检测更新出现异常");
                return new ResponseBody<>(-1, "无法检测更新");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("检测更新出现异常",e);
            return new ResponseBody<>(-1, "无法检测更新");
        }
        return null;
    }
}