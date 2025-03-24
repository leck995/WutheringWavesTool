package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.model.CardInfo;
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

    public CardPoolAnalysisTask(String playerId) {
        this.playerId = playerId;
        baseSSRList = List.of(LanguageManager.getStringArray("ui.analysis.base_role"));
    }

    @Override
    protected ResponseBody<List<AnalysisData>> call() {
        File poolJson = new File(String.format(POOL_FILE, playerId));
        ObjectMapper mapper = new ObjectMapper();
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


    private AnalysisData analysis(String name, List<CardInfo> cardInfoList) {
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


        //先获取五星在列表中的索引
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



    private String getStartDate(List<CardInfo> cardInfoList){
        CardInfo last = cardInfoList.getLast();
        String time = last.getTime();
        if (time != null && !time.isEmpty()) {
            return time.substring(0, time.indexOf(" "));
        }
        return time;
    }

    private String getEndDate(List<CardInfo> cardInfoList){
        CardInfo last = cardInfoList.getFirst();
        String time = last.getTime();
        if (time != null && !time.isEmpty()) {
            return time.substring(0, time.indexOf(" "));
        }
        return time;
    }


    private void analysisNoUp(AnalysisData analysisData,List<CardInfo> cardInfoList, List<Integer> ssrIndexList, List<Integer> srIndexList){
        if (!ssrIndexList.isEmpty()) {
            Integer ssrIndex = ssrIndexList.getFirst();
            analysisData.setNoUpSsrCount(ssrIndex); //设置已垫次数，实际上等于最后一次出现SSR的索引
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

    private void analysisSSR(AnalysisData analysisData,List<CardInfo> cardInfoList ,List<Integer> ssrIndexList){
        if (!ssrIndexList.isEmpty()) {
            List<SsrData> ssrDataList = new ArrayList<>();
            SsrData ssrData;
            int ssrMin = 80;
            int ssrMax = 0;
            double totalCount = 0;
            //按照五星索引进行列表分段
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
        } else {
            analysisData.setSsrAvg(0.0);
            analysisData.setSsrMax(0);
            analysisData.setSsrMin(0);
            analysisData.setSsrDataList(new ArrayList<>());
        }
    }
    private void analysisSR(AnalysisData analysisData,List<CardInfo> cardInfoList ,List<Integer> srIndexList){
        if (!srIndexList.isEmpty()) {
            List<SsrData> srDataList = new ArrayList<>();
            SsrData ssrData;
            int ssrMin = 10;
            int ssrMax = 0;
            double totalCount = 0;
            //按照五星索引进行列表分段
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