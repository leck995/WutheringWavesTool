package cn.tealc.wutheringwavestool.thread.gacha;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.Message;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.util.FileIO;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: WutheringWavesTool
 * @description: 获取卡池数据
 * @author: Leck
 * @create: 2024-07-03 00:14
 */
public class CardPoolGetUrlTask extends Task<ResponseBody<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(CardPoolGetUrlTask.class);

    @Override
    protected ResponseBody<String> call() throws Exception {
        String dir = Config.setting().getGameRootDir();
        if (dir != null) {
            File gameLogFile = GameResourcesManager.getGameLogFile();
            if (gameLogFile.exists()) {
                String url = getLogFileUrl(gameLogFile);
                if (url != null){
                    return ResponseBody.create(200,"获取成功",url);
                }else { //找不到卡池链接时
                    return new ResponseBody<>(101,LanguageManager.getString("ui.analysis.message.type04"));
                }
            }else { //日志文件不存在时
                return new ResponseBody<>(102,String.format(LanguageManager.getString("ui.analysis.message.type05"),gameLogFile.getAbsolutePath()));
            }
        }else { //游戏根目录未设置
            return new ResponseBody<>(103,LanguageManager.getString("ui.analysis.message.type06"));
        }
    }

    /**
     * 解密日志文件并从中提取卡池URL
     */
    private String getLogFileUrl(File file) {
        String decrypted = decryptLog(file);
        if (decrypted == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("https.*/aki/gacha/index.html#/record[?=&\\w\\-]+");
        Matcher matcher = pattern.matcher(decrypted);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(0);
        }
        return lastMatch;
    }


    /**
     * 解密 Client.log 文件
     */
    private String decryptLog(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {
            long fileSize = channel.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            channel.read(buffer);
            buffer.flip();
            byte[] bytes = buffer.array();
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i] & 0xFF;
                if (((b & 0x0F) % 2) == 1) {
                    bytes[i] = (byte) (b ^ 0xA5);
                } else {
                    bytes[i] = (byte) (b ^ 0xEF);
                }
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("解密日志文件失败: {}", e.getMessage(), e);
            return null;
        }
    }

}
