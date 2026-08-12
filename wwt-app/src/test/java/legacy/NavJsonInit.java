package legacy;

import cn.tealc.wutheringwavestool.model.system.NavData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-17 18:01
 */
public class NavJsonInit {
    public static void main(String[] args) throws JsonProcessingException {
        NavData navData = new NavData();
        navData.setId(1001);
        navData.setTitle("ui.main.button.nav.type02");
        navData.setIcon("mdral-home");
        navData.setFxml(true);
        navData.setKujiequ(false);
        navData.setVisible(true);
        navData.setOrder(1);
        navData.setViewClass("cn.tealc.wutheringwavestool.ui.HomeView");


        List<NavData> list = new ArrayList<NavData>();
        list.add(navData);
        ObjectMapper mapper = new ObjectMapper();
        String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        System.out.println(s);
    }
}