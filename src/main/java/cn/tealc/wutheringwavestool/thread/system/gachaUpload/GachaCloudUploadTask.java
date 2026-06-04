package cn.tealc.wutheringwavestool.thread.system.gachaUpload;

import cn.tealc.wutheringwavestool.base.AppConstants;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.FileUploadResult;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GachaCloudUploadTask extends Task<ResponseBody<FileUploadResult>> {
    private static final Logger LOG = LoggerFactory.getLogger(GachaCloudUploadTask.class);

    private final String username;
    private final String password;
    private final String filename;
    private final File file;

    public GachaCloudUploadTask(String username, String password, String filename, File file) {
        this.username = username;
        this.password = password;
        this.filename = filename;
        this.file = file;
    }

    @Override
    protected ResponseBody<FileUploadResult> call() throws Exception {
        HttpClient client = AppInjector.getInstance(HttpClient.class);
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);

        byte[] minified = mapper.writeValueAsBytes(mapper.readTree(file));

        MultipartFormBody body = new MultipartFormBody.Builder()
                .addPart("username", username)
                .addPart("password", password)
                .addPart("filename", filename)
                .addPart("file", minified, "application/json", file.getName())
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConstants.URL_FILE_UPLOAD))
                .header("Content-Type", body.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBody()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        if (responseBody == null || responseBody.isEmpty()) {
            LOG.error("上传响应为空, 状态码: {}", response.statusCode());
            return new ResponseBody<>(-1, "上传失败，服务器无响应");
        }
        return mapper.readValue(responseBody,
                new TypeReference<ResponseBody<FileUploadResult>>() {});
    }


}