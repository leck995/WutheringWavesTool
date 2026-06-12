package cn.tealc.wutheringwavestool.ui.system.account;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.saxsys.mvvmfx.utils.commands.Action;
import de.saxsys.mvvmfx.utils.commands.Command;
import de.saxsys.mvvmfx.utils.commands.DelegateCommand;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AppAccountViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(AppAccountViewModel.class);
    private final SimpleStringProperty username =new SimpleStringProperty();
    private final SimpleStringProperty password = new SimpleStringProperty();
    private Command verifyCommand;

    public void initialize(){
        username.set(Config.setting().getServerUsername());
        password.set(Config.setting().getServerPassword());
        verifyCommand = new DelegateCommand(() -> new Action() {
            @Override
            protected void action() {
                verify();
            }
        },true);
    }

    public void verify(){
        ResponseBody<Void> request = request();
        if (request.getCode() == 200){
            NotificationManager.message(MessageInfo.success(request.getMsg()));
            save();
        }else {
            NotificationManager.message(MessageInfo.warning(request.getMsg()));
        }
    }

    public ResponseBody<Void> request() {
/*        AuthVerifyTask task = new AuthVerifyTask(getUsername(),getPassword());
        task.setOnSucceeded(event -> {
            ResponseBody<Void> value = task.getValue();
            if (value.getCode() == 200){
                NotificationManager.message(MessageInfo.success(value.getMsg()));
                save();
            }else {
                NotificationManager.message(MessageInfo.warning(value.getMsg()));
            }
        });
        Thread.startVirtualThread(task);*/
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
        try {
            String formBody = "username=" + getUsername() + "&password=" + getPassword();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.URL_AUTH_VERIFY))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty()) {
                LOG.error("验证响应为空, 状态码: {}", response.statusCode());
                return new ResponseBody<>(-1, "验证失败，服务器无响应");
            }
            return mapper.readValue(responseBody,
                    new TypeReference<ResponseBody<Void>>() {});
        } catch (IOException | InterruptedException e) {
            LOG.error("验证出现异常", e);
            return new ResponseBody<>(-1, "验证失败，请检查网络状况");
        }
    }


    public void save(){
        Config.setting().setServerUsername(getUsername());
        Config.setting().setServerPassword(getPassword());
    }


    public String getUsername() {
        return username.get();
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public String getPassword() {
        return password.get();
    }

    public SimpleStringProperty passwordProperty() {
        return password;
    }

    public Command getVerifyCommand() {
        return verifyCommand;
    }
}
