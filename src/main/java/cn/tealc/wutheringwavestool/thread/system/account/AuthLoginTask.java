package cn.tealc.wutheringwavestool.thread.system.account;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.AuthLoginResult;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthLoginTask extends Task<ResponseBody<AuthLoginResult>> {
    private static final Logger LOG = LoggerFactory.getLogger(AuthLoginTask.class);

    private final String username;
    private final String password;

    public AuthLoginTask(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected ResponseBody<AuthLoginResult> call() throws Exception {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
        try {
            String formBody = "username=" + username + "&password=" + password;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.URL_AUTH_LOGIN))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty()) {
                LOG.error("登录响应为空, 状态码: {}", response.statusCode());
                return new ResponseBody<>(-1, "登录失败，服务器无响应");
            }
            return mapper.readValue(responseBody,
                    new TypeReference<ResponseBody<AuthLoginResult>>() {});
        } catch (IOException | InterruptedException e) {
            LOG.error("登录出现异常", e);
            return new ResponseBody<>(-1, "登录失败，请检查网络状况");
        }
    }
}
