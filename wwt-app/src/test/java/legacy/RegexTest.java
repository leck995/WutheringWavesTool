package legacy;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-11-18 17:22
 */
public class RegexTest {
    @Test
    void testRegex() {
        String data = "[data: [{UPs:159,wPs:1731906090,s5n:5},{UPs:136,wPs:1731906135,s5n:6}]]";
        String regex = "UPs:(\\d+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);

        while (matcher.find()) {
            System.out.println(matcher.group(1)); // 输出匹配到的数字
        }
    }


    @Test
    void timestamp() {
        String row= "[2024.11.21-23.52.28:426][363][GameThread]Puerts: Display: (0x00000001ABAC6970) [436][I][Performance][XWX][832][23.52.28:424] SetUserId [playerId: 115759821]";
        String regex = "\\[(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}:\\d{3})\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(row);

        if (matcher.find()) {
            String time = matcher.group(1);
            System.out.println("提取的时间: " + time);
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss:SSS");
            try {
                Date date = format.parse(time);
                long timestampInMillis = date.getTime(); // 获取毫秒
                System.out.println("时间戳（毫秒）: " + timestampInMillis);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}