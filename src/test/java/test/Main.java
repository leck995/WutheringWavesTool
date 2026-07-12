package test;

import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.thread.rolebox.tower.TowerDataDetailTask;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-15 23:34
 */
public class Main {
    public static void main(String[] args) {
        UserInfoDao dao = new UserInfoDao();
        UserInfo main = dao.getMain();
        TowerDataDetailTask task = new TowerDataDetailTask(main);

        Thread.startVirtualThread(task);
    }
}