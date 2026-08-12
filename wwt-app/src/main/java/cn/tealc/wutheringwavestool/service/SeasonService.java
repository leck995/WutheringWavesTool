package cn.tealc.wutheringwavestool.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

@Singleton
public class SeasonService {

    /** 赛季制度变更的截止日期 */
    private static final LocalDate SEASON_CUTOFF_DATE = LocalDate.of(2025, 2, 3);

    @Inject
    public SeasonService() {}

    /** 将时间戳归一化为当天凌晨 4:00 */
    public long normalizeTo4AM(long timestampMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /** 获取指定时间戳所在赛季的起始日期（截止日前 14 天周期，之后 28 天周期） */
    public String getSeasonStartDate(long seasonEndTimestampMillis) {
        LocalDate endDate = LocalDate.ofEpochDay(
                seasonEndTimestampMillis / (1000 * 60 * 60 * 24));
        int cycleDays = endDate.isBefore(SEASON_CUTOFF_DATE) ? 14 : 28;
        return endDate.minusDays(cycleDays).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /** 判断赛季使用哪个周期（14 天或 28 天） */
    public int getSeasonCycleDays(long seasonEndTimestampMillis) {
        LocalDate endDate = LocalDate.ofEpochDay(
                seasonEndTimestampMillis / (1000 * 60 * 60 * 24));
        return endDate.isBefore(SEASON_CUTOFF_DATE) ? 14 : 28;
    }
}
