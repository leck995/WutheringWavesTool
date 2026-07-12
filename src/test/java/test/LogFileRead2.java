package test;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-11-12 22:41
 */
public class LogFileRead2 {
    @Test
    void 角色切换(){

        Map<String,Integer> map = new LinkedHashMap<>();
        map.put("角色下场，立即隐藏",0); //统计切人
        map.put("切换玩家状态: 进入战斗造成伤害",0); //统计战斗次数
        map.put("召唤系幻象的出生特效",0); //统计幻象
        map.put("结束技能名称: 变身幻象",0);//统计幻象

 /*       map.put("进入 => [CBoss瘫痪开始",0);  //激活节点[Boss瘫痪蒙太奇
        map.put("进入 => [Boss瘫痪开始",0);
        map.put("激活节点[Boss瘫痪蒙太奇",0);*/
        map.put("进入倒地状态",0);//统计Boss瘫痪
        map.put("进入 => [C瘫痪表演开始",0); //大怪瘫痪
        map.put("传送:完成",0);


        map.put("结束技能名称: 极限闪避前闪",0);
        map.put("结束技能名称: 极限闪避后闪",0);
        map.put("结束技能名称: 极限闪避反击",0);

        map.put("[技能名称: 初次幻象收服]",0);

        map.put("前台角色死亡进行切人",0);


        //String key ="角色下场，立即隐藏";
        String filePath = "C:\\Leck\\Game\\Wuthering Waves\\Wuthering Waves Game\\Client\\Saved\\Logs\\Client.log"; // 替换为你的文件路径

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {

                for (String key : map.keySet()) {
                    if (line.contains(key)) {
                        map.put(key, map.get(key) + 1);
                    }
                }
               /* if (line.contains("瘫痪"))
                    System.out.println(line);*/


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        entries.forEach(entry -> {
            System.out.println(entry.getKey()+":"+entry.getValue());
        });

    }


    @Test
    void readTime() {

        String filePath = "C:\\Leck\\Game\\Wuthering Waves\\Wuthering Waves Game\\Client\\Saved\\Logs\\Client-backup-2024.11.25-23.55.56-2024.11.26-00.12.13.log"; // 替换为你的文件路径
        String regex = "\\[(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}:\\d{3})\\]";
        Pattern pattern = Pattern.compile(regex);
        System.out.println(System.currentTimeMillis());
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String time = matcher.group(1);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss:SSS");
                    try {
                        Date date = format.parse(time);
                        // 获取毫秒
                        //System.out.println(date.getTime());
                    } catch (ParseException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(System.currentTimeMillis());
    }

    @Test
    void name() {
        File dir = new File("D:\\Wuthering Waves\\Wuthering Waves Game\\Client\\Saved\\Logs");
        File[] files = dir.listFiles(file-> file.getName().startsWith("Client"));
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (File file : files) {
            System.out.println(file.getName() + file.lastModified());
        }


    }
}