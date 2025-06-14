package com.kuro.kujiequ.thread.rolebox.role;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.kujiequ.AccessTokenException;
import com.kuro.kujiequ.ApiConfig;
import com.kuro.kujiequ.ApiDecryptException;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.BaseTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 获取拥有角色列表
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class GameRoleDataTask extends BaseTask<ResponseBody<List<Role>>> {
    private static final Logger LOG = LoggerFactory.getLogger(GameRoleDataTask.class);
    private final UserInfo userInfo;

    public GameRoleDataTask(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    protected ResponseBody<List<Role>> call() throws Exception {
        return getRoleDate();
    }

    private ResponseBody<List<Role>> getRoleDate() {
        String url = String.format(
                "%s?gameId=3&serverId=76402e5b20be2c39f095a152090afddc&roleId=%s", ApiConfig.ROLE_DATA_URL, userInfo.getRoleId());
        try {
            HttpRequest.Builder builder = getBuilder(url, null, userInfo);
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ResponseBody<List<Role>> responseBody = new ResponseBody<>();
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                LOG.debug(response.body());
                JsonNode tree = mapper.readTree(response.body());
                int code = tree.get("code").asInt();

                if (code == 200 || code == 10902) {
                    responseBody.setCode(200);
                    String data = tree.get("data").asText();
                    JsonNode dataTree = mapper.readTree(data);
                    JsonNode jsonNode = dataTree.get("roleList");
                    List<Role> roleList = mapper.readValue(jsonNode.toString(), new TypeReference<List<Role>>() {
                    });
                    responseBody.setSuccess(tree.get("success").asBoolean());
                    roleList.sort((o1, o2) -> Integer.compare(o2.getLevel(), o1.getLevel()));
                    responseBody.setData(roleList);
                } else {
                    responseBody.setCode(code);
                    LOG.error("服务器返回异常，错误代码：{}", code);
                    JsonNode node = tree.get("msg");
                    responseBody.setMsg(node.asText());
                }

            } else {
                LOG.error("网络请求失败，错误代码：{}", response.statusCode());
                responseBody.setCode(response.statusCode());
                responseBody.setSuccess(false);
                responseBody.setMsg("连接失败，响应状态码:" + response.statusCode());
            }
            return responseBody;

        } catch (IOException | InterruptedException | AccessTokenException e) {
            LOG.error("错误", e);
            return new ResponseBody<>(1, e.getMessage());
        }
    }
}