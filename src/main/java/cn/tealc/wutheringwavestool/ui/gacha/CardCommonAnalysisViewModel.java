package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CardCommonAnalysisViewModel implements ViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(CardCommonAnalysisViewModel.class);

    private final ObservableList<AnalysisData> analysisDataList = FXCollections.observableArrayList();
    private final SimpleBooleanProperty empty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(true);

    public CardCommonAnalysisViewModel(boolean isEmpty) {
        empty.set(isEmpty);
        loading.set(!isEmpty);
        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_UPDATE, (s, objects) -> {
            @SuppressWarnings("unchecked")
            List<AnalysisData> list = (List<AnalysisData>) objects[0];
            List<AnalysisData> filterList = list.stream().filter(analysisData -> analysisData.getTotalCount() > 0).toList();
            analysisDataList.setAll(filterList);
            loading.set(false);
            empty.set(false);
        });

        NotificationManager.subscribe(NotificationKey.CARD_POOL_USER_EMPTY, (s, objects) -> {
            empty.set(true);
            loading.set(false);
            analysisDataList.clear();
        });
    }

    public ObservableList<AnalysisData> getAnalysisDataList() {
        return analysisDataList;
    }

    public boolean isEmpty() {
        return empty.get();
    }

    public SimpleBooleanProperty emptyProperty() {
        return empty;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public SimpleBooleanProperty loadingProperty() {
        return loading;
    }
}