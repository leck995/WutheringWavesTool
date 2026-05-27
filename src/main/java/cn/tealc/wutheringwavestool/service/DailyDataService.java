package cn.tealc.wutheringwavestool.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.roleData.user.BoxInfo;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Singleton
public class DailyDataService {

    @Inject
    public DailyDataService() {}

    /** 格式化体力刷新时间文本 */
    public String formatEnergyRefreshTime(long refreshTimestampSec, String[] strengthLabels) {
        if (refreshTimestampSec == 0) {
            return strengthLabels[2]; // 已满
        }
        long timestamp = refreshTimestampSec * 1000;
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDate refreshDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        boolean isSameDay = refreshDate.equals(LocalDate.now());
        SimpleDateFormat formatter = new SimpleDateFormat(isSameDay ? strengthLabels[0] : strengthLabels[1]);
        return formatter.format(new Date(timestamp));
    }

    /** 匹配宝箱名称并返回数量 */
    public static int findChestCount(BoxInfo boxInfo, String[] chestNames) {
        for (int i = 0; i < chestNames.length; i++) {
            if (boxInfo.getBoxName().equals(chestNames[i])) return i;
        }
        return -1;
    }

    /** 检查是否为每周最后一天 */
    public boolean isWeeklyDeadline() {
        return LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /** 检查周活动是否需要提醒（肉鸽分数 < 6000 需要提醒） */
    public boolean needsRougeReminder(RoleInfo roleInfo) {
        return roleInfo != null && roleInfo.getRougeScore() < 6000;
    }

    /** 检查周本是否已完成 */
    public boolean hasWeeklyInstRemaining(RoleInfo roleInfo) {
        return roleInfo != null && roleInfo.getWeeklyInstCount() != 0;
    }
}
