package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CardTableShowViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(CardTableShowViewModel.class);

    private Map<String, List<CardInfo>> poolDataMap = Collections.emptyMap();
    private final ObservableList<CardInfo> allData = FXCollections.observableArrayList();
    private final ObservableList<CardInfo> pagedData = FXCollections.observableArrayList();

    private final ObservableList<String> poolTypes = FXCollections.observableArrayList();
    private final StringProperty selectedPoolType = new SimpleStringProperty();

    private final IntegerProperty currentPage = new SimpleIntegerProperty(1);
    private final IntegerProperty pageSize = new SimpleIntegerProperty(20);
    private final ReadOnlyIntegerWrapper totalPages = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper totalCount = new ReadOnlyIntegerWrapper(0);

    private final BooleanProperty empty = new SimpleBooleanProperty(true);
    private final BooleanProperty hasPrev = new SimpleBooleanProperty(false);
    private final BooleanProperty hasNext = new SimpleBooleanProperty(false);
    private final BooleanProperty loaded = new SimpleBooleanProperty(false);

    public void initialize() {
        System.out.println("FFFFFFFFFFFFFFFFFFFF");
        currentPage.addListener((obs, old, val) -> refreshPage());
        pageSize.addListener((obs, old, val) -> {
            currentPage.set(1);
            refreshPage();
        });
        selectedPoolType.addListener((obs, old, val) -> {
            if (val != null) switchPool(val);
        });

        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_CHANGE,(s, objects) -> {
            empty.set(false);
            String playerId = (String) objects[0];
            reset();
            loadData(playerId);

        });

        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_EMPTY,(s, objects) -> {
            empty.set(true);
            reset();
        });
    }

    private void reset() {
        poolDataMap.clear();
        poolTypes.clear();
        hasNext.set(false);
        hasPrev.set(false);
        pageSize.set(20);
        currentPage.set(1);
        selectedPoolType.set(null);
    }


    public void loadData(String playerId) {
        Thread.startVirtualThread(()->{
            File poolJson = new File(String.format("data/%s/pool.json", playerId));
            if (!poolJson.exists()) return;
            ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);
            try {
                poolDataMap = mapper.readValue(poolJson,
                        new TypeReference<Map<String, List<CardInfo>>>() {});
            } catch (IOException e) {
                LOG.error("加载抽卡数据失败", e);
                poolDataMap = Collections.emptyMap();
            }

            Platform.runLater(()->{
                poolTypes.setAll(poolDataMap.keySet());
                loaded.set(true);
                if (!poolTypes.isEmpty()) {
                    selectedPoolType.set(poolTypes.getFirst());
                }
            });
        });
    }

    private void switchPool(String poolType) {
        List<CardInfo> cards = poolDataMap.getOrDefault(poolType, Collections.emptyList());
        allData.setAll(cards);
        totalCount.set(cards.size());
        empty.set(cards.isEmpty());
        currentPage.set(1);
        refreshPage();
    }

    private void refreshPage() {
        int size = pageSize.get();
        int page = Math.max(1, currentPage.get());
        int total = allData.size();
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        totalPages.set(pages);

        if (pages > 0 && page > pages) {
            currentPage.set(pages);
            return;
        }

        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        pagedData.setAll(allData.subList(from, to));

        hasPrev.set(page > 1);
        hasNext.set(page < pages);
    }

    public void nextPage() { if (getHasNext()) currentPage.set(currentPage.get() + 1); }
    public void prevPage() { if (getHasPrev()) currentPage.set(currentPage.get() - 1); }
    public void firstPage() { currentPage.set(1); }
    public void lastPage() { currentPage.set(getTotalPages()); }

    public ObservableList<CardInfo> getPagedData() { return pagedData; }
    public ObservableList<String> getPoolTypes() { return poolTypes; }

    public String getSelectedPoolType() { return selectedPoolType.get(); }
    public StringProperty selectedPoolTypeProperty() { return selectedPoolType; }

    public int getCurrentPage() { return currentPage.get(); }
    public IntegerProperty currentPageProperty() { return currentPage; }

    public int getPageSize() { return pageSize.get(); }
    public IntegerProperty pageSizeProperty() { return pageSize; }

    public int getTotalPages() { return totalPages.get(); }
    public ReadOnlyIntegerProperty totalPagesProperty() { return totalPages.getReadOnlyProperty(); }

    public int getTotalCount() { return totalCount.get(); }
    public ReadOnlyIntegerProperty totalCountProperty() { return totalCount.getReadOnlyProperty(); }

    public boolean isEmpty() { return empty.get(); }
    public BooleanProperty emptyProperty() { return empty; }

    public boolean getHasPrev() { return hasPrev.get(); }
    public BooleanProperty hasPrevProperty() { return hasPrev; }

    public boolean getHasNext() { return hasNext.get(); }
    public BooleanProperty hasNextProperty() { return hasNext; }

    public boolean isLoaded() { return loaded.get(); }
    public BooleanProperty loadedProperty() { return loaded; }
}
