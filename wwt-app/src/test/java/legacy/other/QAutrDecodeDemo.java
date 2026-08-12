package legacy.other;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuro.launcher.model.LocalCacheUser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class QAutrDecodeDemo {

    public static void main(String[] args) throws IOException {
// 1. 构造文件路径
        // 对应 PowerShell 的 Join-Path $env:APPDATA 'KR_G153\A1730\KRSDKUserLauncherCache.json'
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            System.err.println("错误: 无法找到 APPDATA 环境变量。");
            return;
        }
        String path = Paths.get(appData, "KR_G152", "A1381", "KRSDKUserLauncherCache.json").toString();
        //String path = Paths.get(appData, "KR_G153", "A1730", "KRSDKUserLauncherCache.json").toString();
        File file = new File(path);

        // 2. 检查文件是否存在
        if (!file.exists()) {
            System.err.println("错误: 文件未找到: " + path);
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        List<LocalCacheUser> localCacheUsers = mapper.readValue(file, new TypeReference<List<LocalCacheUser>>() {
        });

        localCacheUsers.stream().map(user -> decodeXor5(user.getCuid())).forEach(System.out::println);
        System.out.println(localCacheUsers.size());

    }

    private static String decodeXor5(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append((char) (((int) c) ^ 5));
        }
        return sb.toString();
    }
}
