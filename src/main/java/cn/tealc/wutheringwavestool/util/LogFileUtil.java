package cn.tealc.wutheringwavestool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-04 16:38
 */
public class LogFileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(LogFileUtil.class);
    /**
     * @description: 从日志文件读取卡池链接
     * @param:	file	日志文件
     * @return  java.lang.String
     * @date:   2024/7/4
     */
    public static String getLogFileUrl(File file){
        Pattern pattern = Pattern.compile("https.*/aki/gacha/index.html#/record[?=&\\w\\-]+");
        List<String> list = new ArrayList<String>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    list.add(matcher.group(0));
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(),e);
        }

        if (!list.isEmpty()) {
            return list.getLast();
        }
        return null;
    }

    /**
     * @description: 获取卡池链接的参数
     * @param:	row	卡池链接
     * @return  java.util.Map<java.lang.String,java.lang.String>
     * @date:   2024/7/4
     */
    public static Map<String,String> getParamFromUrl(String row){
        Map<String, String> parameters = new HashMap<>();
        String paramRow = row.substring(row.indexOf("?") + 1);
        String[] strings = paramRow.split("&");

        for (String string : strings) {
            String[] split = string.split("=");
            switch (split[0]) {
                case "player_id" -> parameters.put("playerId", split[1]);
                case "record_id" -> parameters.put("recordId", split[1]);
                case "resources_id" -> parameters.put("cardPoolId", split[1]);
                case "gacha_type" -> parameters.put("cardPoolType", split[1]);
                case "svr_id" -> parameters.put("serverId", split[1]);
                case "lang" -> parameters.put("languageCode", split[1]);
            }
        }
        return parameters;
    }



}