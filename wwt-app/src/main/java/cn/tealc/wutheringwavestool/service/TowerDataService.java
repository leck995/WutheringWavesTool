package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameTowerDataDao;
import cn.tealc.wutheringwavestool.model.tower.TowerData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.towerData.Difficulty;
import com.kuro.kujiequ.model.towerData.DifficultyTotal;
import com.kuro.kujiequ.model.towerData.Floor;
import com.kuro.kujiequ.model.towerData.TowerArea;
import com.kuro.kujiequ.model.towerData.SimpleRole;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class TowerDataService {

    @Inject
    private GameTowerDataDao gameTowerDataDao;

    @Inject
    public TowerDataService() {}

    /** 将 API 响应中的深塔数据转为扁平 DB 记录列表 */
    public List<TowerData> extractTowerRecords(DifficultyTotal difficultyTotal, String roleId) {
        List<TowerData> records = new ArrayList<>();
        Difficulty first = difficultyTotal.getDifficultyList().getFirst();
        if (first.getDifficulty() == 3) {
            long seasonEndTime = difficultyTotal.getSeasonEndTime();
            long date = convertToHourlyTimestamp(System.currentTimeMillis() + seasonEndTime);
            for (TowerArea area : first.getTowerAreaList()) {
                for (Floor floor : area.getFloorList()) {
                    TowerData data = new TowerData();
                    data.setRoleId(roleId);
                    data.setAreaId(area.getAreaId());
                    data.setAreaName(area.getAreaName());
                    data.setDifficulty(first.getDifficulty());
                    data.setDifficultyName(first.getDifficultyName());
                    data.setFloor(floor.getFloor());
                    data.setStar(floor.getStar());
                    data.setPicUrl(floor.getPicUrl());
                    if (floor.getRoleList() != null) {
                        data.setRoleList(floor.getRoleList().stream()
                                .map(r -> String.valueOf(r.getRoleId()))
                                .collect(Collectors.joining(",")));
                    }
                    data.setEndTime(date);
                    records.add(data);
                }
            }
        }
        return records;
    }

    /** 将深塔数据保存到数据库（原 TowerDataDetailTask.saveToDB 上移） */
    public void saveToDB(DifficultyTotal difficultyTotal, String roleId) {
        List<TowerData> records = extractTowerRecords(difficultyTotal, roleId);
        records.forEach(gameTowerDataDao::add);
    }

    /** 将 DB 记录反向构建为嵌套的 Difficulty -> TowerArea -> Floor 结构 */
    public List<TowerArea> buildTowerAreaHistory(List<TowerData> records, Map<Integer, Role> roleMap) {
        Map<Integer, TowerArea> areaMap = new LinkedHashMap<>();
        for (TowerData record : records) {
            TowerArea area = areaMap.computeIfAbsent(record.getAreaId(), id -> {
                TowerArea a = new TowerArea();
                a.setAreaId(record.getAreaId());
                a.setAreaName(record.getAreaName());
                a.setFloorList(new ArrayList<>());
                return a;
            });
            Floor floor = new Floor();
            floor.setFloor(record.getFloor());
            floor.setStar(record.getStar());
            floor.setPicUrl(record.getPicUrl());
            if (record.getRoleList() != null && !record.getRoleList().isEmpty()) {
                floor.setRoleList(Arrays.stream(record.getRoleList().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(roleIdStr -> {
                            Role role = roleMap.get(Integer.parseInt(roleIdStr));
                            return role != null ? new SimpleRole(role.getRoleId(), role.getRoleIconUrl()) : null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
            area.getFloorList().add(floor);
        }
        return new ArrayList<>(areaMap.values());
    }

    /** 深塔难度排序：难度 3 始终在最前，其余降序 */
    public void sortDifficulties(List<Difficulty> difficulties) {
        difficulties.sort((o1, o2) -> {
            if (o1.getDifficulty() == 3) return -1;
            if (o2.getDifficulty() == 3) return 1;
            return o2.getDifficulty() - o1.getDifficulty();
        });
    }

    /** 将给定的时间戳转换成当天 4 点（原 TowerDataDetailTask.convertToHourlyTimestamp 上移） */
    public long convertToHourlyTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
