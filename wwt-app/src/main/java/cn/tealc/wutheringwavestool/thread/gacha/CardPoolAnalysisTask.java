package cn.tealc.wutheringwavestool.thread.gacha;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-22 23:17
 */
public class CardPoolAnalysisTask extends Task<ResponseBody<List<AnalysisData>>> {
    private static final Logger LOG = LoggerFactory.getLogger(CardPoolAnalysisTask.class);
    private static final String POOL_FILE = "data/%s/pool.json";
    private final List<String> baseSSRList;
    private final String playerId;
    private Map<String, List<CardInfo>> poolData;
    private boolean skipFirstSSR = false;

    public CardPoolAnalysisTask(String playerId,boolean skipFirstSSR) {
        this.playerId = playerId;
        this.skipFirstSSR = skipFirstSSR;
        baseSSRList = List.of(LanguageManager.getStringArray("ui.analysis.base_role"));
    }

    @Override
    protected ResponseBody<List<AnalysisData>> call() {
        File poolJson = new File(String.format(POOL_FILE, playerId));
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
        try {
            if (poolJson.exists()) {
                poolData = mapper.readValue(poolJson, new TypeReference<Map<String, List<CardInfo>>>() {
                });
                List<AnalysisData> list = new ArrayList<>();
                for (Map.Entry<String, List<CardInfo>> entry : poolData.entrySet()) {
                    if (!entry.getValue().isEmpty()){
                        list.add(analysis(entry.getKey(), entry.getValue().stream().toList()));
                    }else {
                        list.add(analysis(entry.getKey(), new ArrayList<>()));
                    }

                }
                return ResponseBody.create(200,"分析完成", list);
            }
            return ResponseBody.create(-1,String.format("角色 %s 的抽卡数据不存在",playerId), null);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return ResponseBody.create(-1,"抽卡数据解析错误",null);
        }
    }


    /**
     * 分析单个卡池的抽卡数据。cardInfoList 按时间倒序排列（索引0 = 最新抽卡）。
     */
    private AnalysisData analysis(String name, List<CardInfo> cardInfoList) {
        // 跳过末尾未完成的五星保底窗口，避免数据失真
        if (skipFirstSSR){
            for (int i = cardInfoList.size() - 1; i >= 0; i--) {
                CardInfo cardInfo = cardInfoList.get(i);
                if (cardInfo.getQualityLevel() == 5){
                    cardInfoList = new ArrayList<>(cardInfoList.subList(0, i));
                    break;
                }
            }
        }

        AnalysisData analysisData = new AnalysisData();
        analysisData.setTotalCount(cardInfoList.size());
        analysisData.setPoolName(name);

        if (cardInfoList.isEmpty()){
            analysisData.setEmpty(true);
            return analysisData;
        }
        analysisData.setEmpty(false);
        analysisData.setStartDate(getStartDate(cardInfoList));
        analysisData.setEndDate(getEndDate(cardInfoList));


        // cardInfoList 按时间倒序：索引0=最新，获取各稀有度在列表中的位置
        List<Integer> ssrIndexList = new ArrayList<>();
        List<Integer> srIndexList = new ArrayList<>();
        List<Integer> rIndexList = new ArrayList<>();
        for (int i = 0; i < cardInfoList.size(); i++) {
            CardInfo cardInfo = cardInfoList.get(i);
            if (cardInfo.getQualityLevel() == 5) {
                ssrIndexList.add(i);
            } else if (cardInfo.getQualityLevel() == 4) {
                srIndexList.add(i);
            } else if (cardInfo.getQualityLevel() == 3) {
                rIndexList.add(i);
            }
        }

        analysisData.setSsrCount(ssrIndexList.size());
        analysisData.setSrCount(srIndexList.size());
        analysisData.setrCount(rIndexList.size());

        analysisNoUp(analysisData,cardInfoList,ssrIndexList,srIndexList);

        analysisSSR(analysisData,cardInfoList,ssrIndexList);
        analysisSR(analysisData,cardInfoList,srIndexList);

        return analysisData;
    }



    // 列表倒序排列，getLast()=最旧的记录=开始日期
    private String getStartDate(List<CardInfo> cardInfoList){
        CardInfo last = cardInfoList.getLast();
        String time = last.getTime();
        if (time != null && !time.isEmpty()) {
            return time.substring(0, time.indexOf(" "));
        }
        return time;
    }

    // 列表倒序排列，getFirst()=最新的记录=结束日期
    private String getEndDate(List<CardInfo> cardInfoList){
        CardInfo last = cardInfoList.getFirst();
        String time = last.getTime();
        if (time != null && !time.isEmpty()) {
            return time.substring(0, time.indexOf(" "));
        }
        return time;
    }


    /**
     * 计算当前保底进度（已垫抽数）。列表倒序排列，首个SSR/SR的索引即距最新一抽的距离。
     */
    private void analysisNoUp(AnalysisData analysisData,List<CardInfo> cardInfoList, List<Integer> ssrIndexList, List<Integer> srIndexList){
        if (!ssrIndexList.isEmpty()) {
            Integer ssrIndex = ssrIndexList.getFirst();
            analysisData.setNoUpSsrCount(ssrIndex);
            if (!srIndexList.isEmpty()){
                Integer srIndex = srIndexList.getFirst();
                if (srIndex < ssrIndex){
                    analysisData.setNoUpSrCount(srIndex);
                }else {
                    analysisData.setNoUpSrCount(ssrIndex);
                }
            }else {
                analysisData.setNoUpSrCount(cardInfoList.size());
            }
        }else {
            Integer ssrIndex = cardInfoList.size();
            analysisData.setNoUpSsrCount(ssrIndex);
            if (!srIndexList.isEmpty()){
                Integer srIndex = srIndexList.getFirst();
                if (srIndex < ssrIndex){
                    analysisData.setNoUpSrCount(srIndex);
                }else {
                    analysisData.setNoUpSrCount(ssrIndex);
                }
            }else {
                analysisData.setNoUpSrCount(ssrIndex);
            }
        }
    }

    /**
     * 分析五星数据。按SSR索引将列表分段，每段从当前SSR到下一个SSR（不含），
     * 段长度即为"抽到该五星所需的抽数"。
     */
    private void analysisSSR(AnalysisData analysisData,List<CardInfo> cardInfoList ,List<Integer> ssrIndexList){
        if (!ssrIndexList.isEmpty()) {
            List<SsrData> ssrDataList = new ArrayList<>();
            SsrData ssrData;
            int ssrMin = 80;
            int ssrMax = 0;
            double totalCount = 0;
            for (int i = 0; i < ssrIndexList.size(); i++) {
                Integer index = ssrIndexList.get(i);
                CardInfo cardInfo = cardInfoList.get(ssrIndexList.get(i));
                List<CardInfo> cardInfos;
                if (i == ssrIndexList.size() - 1) {
                    cardInfos = cardInfoList.subList(index, cardInfoList.size());
                } else {
                    cardInfos = cardInfoList.subList(index, ssrIndexList.get(i + 1));
                }
                if (cardInfos.size() < ssrMin) {
                    ssrMin = cardInfos.size();
                }
                if (cardInfos.size() > ssrMax) {
                    ssrMax = cardInfos.size();
                }
                totalCount += cardInfos.size();
                ssrData = new SsrData();
                ssrData.setId(cardInfo.getResourceId());
                ssrData.setDate(cardInfo.getTime());
                ssrData.setName(cardInfo.getName());
                ssrData.setCount(cardInfos.size());

                ssrData.setEvent(!baseSSRList.contains(String.valueOf(cardInfo.getResourceId())));
                ssrDataList.add(ssrData);
            }
            double avg = totalCount / ssrIndexList.size();
            analysisData.setSsrAvg(avg);
            analysisData.setSsrMax(ssrMax);
            analysisData.setSsrMin(ssrMin);
            analysisData.setSsrDataList(ssrDataList);

            // UP五星统计：按时间正序遍历（最早→最新），计算连续UP之间的间隔抽数
            // 间隔包含了两个UP之间的所有常驻五星，即"抽到UP前所需的总抽数"
            int upSsrCount = 0;
            int totalUpPulls = 0;
            Integer lastChronoUpIdx = null;
            for (int i = ssrIndexList.size() - 1; i >= 0; i--) {
                int idx = ssrIndexList.get(i);
                CardInfo cardInfo = cardInfoList.get(idx);
                boolean isUp = !baseSSRList.contains(String.valueOf(cardInfo.getResourceId()));
                if (isUp) {
                    upSsrCount++;
                    if (lastChronoUpIdx == null) {
                        // 最早一次UP：从卡池最旧端到该UP的距离
                        totalUpPulls += cardInfoList.size() - idx;
                    } else {
                        // 后续UP：从上一个UP（时间上更早）到当前UP的距离
                        totalUpPulls += lastChronoUpIdx - idx;
                    }
                    lastChronoUpIdx = idx;
                }
            }
            analysisData.setUpSsrCount(upSsrCount);
            analysisData.setUpSsrAvg(upSsrCount > 0 ? (double) totalUpPulls / upSsrCount : 0);
            // UP率：UP角色占全部五星的比例（旧 nonBannerRate 的计算逻辑）
            analysisData.setUpRate(ssrIndexList.size() > 0 ? (double) upSsrCount / ssrIndexList.size() : 0);

            // 计算真实的不歪率（50/50 胜率）：按时间正序遍历五星列表，
            // 非大保底状态下出UP=不歪（赢了50/50），出常驻=歪了（输了50/50，下次进入大保底）
            int totalFiftyFifty = 0;
            int wonFiftyFifty = 0;
            boolean isGuaranteed = false;
            for (int i = ssrIndexList.size() - 1; i >= 0; i--) {
                int idx = ssrIndexList.get(i);
                CardInfo cardInfo = cardInfoList.get(idx);
                boolean isUp = !baseSSRList.contains(String.valueOf(cardInfo.getResourceId()));
                if (isGuaranteed) {
                    // 大保底：必出UP角色，不计入50/50统计
                    isGuaranteed = false;
                } else {
                    // 小保底（50/50情形）
                    totalFiftyFifty++;
                    if (isUp) {
                        wonFiftyFifty++;
                    } else {
                        isGuaranteed = true;
                    }
                }
            }
            analysisData.setNonBannerRate(totalFiftyFifty > 0 ? (double) wonFiftyFifty / totalFiftyFifty : 0);
        } else {
            analysisData.setSsrAvg(0.0);
            analysisData.setSsrMax(0);
            analysisData.setSsrMin(0);
            analysisData.setSsrDataList(new ArrayList<>());
            analysisData.setUpSsrCount(0);
            analysisData.setUpSsrAvg(0.0);
            analysisData.setUpRate(0.0);
            analysisData.setNonBannerRate(0.0);
        }
    }
    /**
     * 分析四星数据，分段逻辑与五星相同。
     */
    private void analysisSR(AnalysisData analysisData,List<CardInfo> cardInfoList ,List<Integer> srIndexList){
        if (!srIndexList.isEmpty()) {
            List<SsrData> srDataList = new ArrayList<>();
            SsrData ssrData;
            int ssrMin = 10;
            int ssrMax = 0;
            double totalCount = 0;
            for (int i = 0; i < srIndexList.size(); i++) {
                Integer index = srIndexList.get(i);
                CardInfo cardInfo = cardInfoList.get(srIndexList.get(i));
                List<CardInfo> cardInfos;
                if (i == srIndexList.size() - 1) {
                    cardInfos = cardInfoList.subList(index, cardInfoList.size());
                } else {
                    cardInfos = cardInfoList.subList(index, srIndexList.get(i + 1));
                }
                if (cardInfos.size() < ssrMin) {
                    ssrMin = cardInfos.size();
                }
                if (cardInfos.size() > ssrMax) {
                    ssrMax = cardInfos.size();
                }
                totalCount += cardInfos.size();
                ssrData = new SsrData();
                ssrData.setId(cardInfo.getResourceId());
                ssrData.setDate(cardInfo.getTime());
                ssrData.setName(cardInfo.getName());
                ssrData.setCount(cardInfos.size());

                ssrData.setEvent(false);
                srDataList.add(ssrData);
            }
            double avg = totalCount / srIndexList.size();
            analysisData.setSrAvg(avg);
            analysisData.setSrMax(ssrMax);
            analysisData.setSrMin(ssrMin);
            analysisData.setSrDataList(srDataList);
        } else {
            analysisData.setSrAvg(0.0);
            analysisData.setSrMax(0);
            analysisData.setSrMin(0);
            analysisData.setSrDataList(new ArrayList<>());
        }
    }


    private void analysisR(AnalysisData analysisData,List<CardInfo> cardInfoList ,List<Integer> rIndexList){
        if (!rIndexList.isEmpty()) {
            List<SsrData> ssrDataList = new ArrayList<>();
            SsrData ssrData;
            int ssrMin = 80;
            int ssrMax = 0;
            double totalCount = 0;
            //按照五星索引进行列表分段
            for (int i = 0; i < rIndexList.size(); i++) {
                Integer index = rIndexList.get(i);
                CardInfo cardInfo = cardInfoList.get(rIndexList.get(i));
                List<CardInfo> cardInfos;
                if (i == rIndexList.size() - 1) {
                    cardInfos = cardInfoList.subList(index, cardInfoList.size());
                } else {
                    cardInfos = cardInfoList.subList(index, rIndexList.get(i + 1));
                }
                if (cardInfos.size() < ssrMin) {
                    ssrMin = cardInfos.size();
                }
                if (cardInfos.size() > ssrMax) {
                    ssrMax = cardInfos.size();
                }
                totalCount += cardInfos.size();
                ssrData = new SsrData();
                ssrData.setId(cardInfo.getResourceId());
                ssrData.setDate(cardInfo.getTime());
                ssrData.setName(cardInfo.getName());
                ssrData.setCount(cardInfos.size());

                ssrData.setEvent(!baseSSRList.contains(String.valueOf(cardInfo.getResourceId())));
                ssrDataList.add(ssrData);
            }
            double avg = totalCount / rIndexList.size();
            analysisData.setrAvg(avg);
            analysisData.setrMax(ssrMax);
            analysisData.setrMin(ssrMin);
            analysisData.setrDataList(ssrDataList);
        } else {
            analysisData.setrAvg(0.0);
            analysisData.setrMax(0);
            analysisData.setrMin(0);
            analysisData.setrDataList(new ArrayList<>());
        }
    }
}