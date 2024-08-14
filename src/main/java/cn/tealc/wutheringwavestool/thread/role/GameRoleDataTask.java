package cn.tealc.wutheringwavestool.thread.role;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.roleData.Role;
import cn.tealc.wutheringwavestool.model.sign.SignUserInfo;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class GameRoleDataTask extends Task<ResponseBody<List<Role>>> {
    private static final Logger LOG= LoggerFactory.getLogger(GameRoleDataTask.class);
    private SignUserInfo signUserInfo;

    public GameRoleDataTask(SignUserInfo signUserInfo) {
        this.signUserInfo = signUserInfo;
    }

    @Override
    protected ResponseBody<List<Role>> call() throws Exception {
        return getRoleDate(signUserInfo.getRoleId(),signUserInfo.getToken());
    }

    private ResponseBody<List<Role>> getRoleDate(String roleId,String token){
        String url=String.format(
                "https://api.kurobbs.com/gamer/roleBox/aki/roleData?gameId=3&serverId=76402e5b20be2c39f095a152090afddc&roleId=%s",roleId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ResponseBody<List<Role>> responseBody = new ResponseBody<>();
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();
                responseBody.setCode(code);
                if (code == 200) {
                    JsonNode jsonNode = tree.get("data").get("roleList");
                    List<Role> roleList = mapper.readValue(jsonNode.toString(), new TypeReference<List<Role>>() {
                    });

                    roleList.sort((o1, o2) -> Integer.compare(o2.getLevel(),o1.getLevel()));
                    responseBody.setData(roleList);
                }else {
                    LOG.error("服务器返回异常，错误代码：{}",code);
                    JsonNode node = tree.get("msg");
                    responseBody.setMsg(node.asText());
                }
                responseBody.setSuccess(tree.get("success").asBoolean());
            }else {
                LOG.error("网络请求失败，错误代码：{}",response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，无法获取数据");
            }
            return responseBody;

        } catch (IOException | InterruptedException e) {
            LOG.error("错误",e);
            return new ResponseBody<>(1,"角色数据获取错误");
        }
    }
}