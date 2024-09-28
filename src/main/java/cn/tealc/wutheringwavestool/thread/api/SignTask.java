package cn.tealc.wutheringwavestool.thread.api;

import cn.tealc.wutheringwavestool.dao.SignHistoryDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.sign.SignRecord;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import cn.tealc.wutheringwavestool.util.HttpRequestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 14:24
 */
public class SignTask extends Task<String> {

    private static final Logger LOG= LoggerFactory.getLogger(SignTask.class);
    private SignHistoryDao historyDao = new SignHistoryDao();
    @Override
    protected String call() throws Exception {
        UserInfoDao dao = new UserInfoDao();
        List<UserInfo> userInfos = dao.getAll();
        StringBuilder sb=new StringBuilder();
        for (int i = 0; i < userInfos.size(); i++) {
            UserInfo user = userInfos.get(i);
            if (i < 9){
                String sign = sign(user.getRoleId(), user.getUserId(), user.getToken());
                sb.append(String.format("当前用户ID:%s,签到状态：%s",user.getUserId(), sign));
                sb.append("\n");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }else {
                sb.append(String.format("当前用户ID:%s,签到状态：签到账号数量超过最大限制",user.getUserId()));
            }
        }

        sb.append("===签到完成===");
        return sb.toString();

    }

    private String sign(String roleId,String userId,String token){
        LocalDate today = LocalDate.now();
        int monthValue= today.getMonthValue();
        String url=String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s&reqMonth=%02d"
                ,ApiConfig.SIGNIN_URL,ApiConfig.PARAM_GAME_ID,ApiConfig.PARAM_SERVER_ID,roleId,userId,monthValue);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                if (response.body().contains("请求成功")){
                    updateHistory(roleId,userId,token);//获取更新签到记录
                    return "签到成功";
                }else if (response.body().contains("请勿重复签到")){
                    updateHistory(roleId,userId,token);//获取更新签到记录
                    return "请勿重复签到";
                }else {
                    return "签到失败";
                }

            }else {
                return "签到失败";
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateHistory(String roleId,String userId,String token){
        String url=String.format("%s?gameId=%s&serverId=%s&roleId=%s&userId=%s"
                ,ApiConfig.SIGNIN_QUERY_URL,ApiConfig.PARAM_GAME_ID,ApiConfig.PARAM_SERVER_ID,roleId,userId);
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequestUtil.getRequest(url,token);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOG.debug("连接成功，添加签到历史记录");
                ObjectMapper mapper = new ObjectMapper();
                ResponseBody<List<SignRecord>> responseBody = mapper.readValue(response.body(), new TypeReference<ResponseBody<List<SignRecord>>>() {
                });
                if (responseBody.getSuccess()){
                    for (SignRecord record : responseBody.getData()) {
                        record.setRoleId(roleId);
                        record.setUserId(userId);
                        historyDao.addHistory(record);
                    }
                }
            }else {
               LOG.info("连接失败");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("JSON异常",e);
        }
    }

}