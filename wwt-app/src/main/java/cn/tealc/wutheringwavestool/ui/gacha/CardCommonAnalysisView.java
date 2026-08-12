package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.teafx.utils.AnchorPaneUtil;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.analysis.AnalysisData;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CardCommonAnalysisView implements FxmlView<CardCommonAnalysisViewModel>, Initializable {
    private static final String AVG_TEMPLATE = "%.0f";
    private static final String PERCENT_TEMPLATE = "%d  [%05.2f%%]";
    private static final int PAGE_SIZE = 4;

    @InjectViewModel
    private CardCommonAnalysisViewModel viewModel;

    @FXML
    private AnchorPane cardArea;

    @FXML
    private HBox contentPane;

    @FXML
    private StackPane emptyPane, loadingPane;

    @FXML
    private Label leftArrow, rightArrow;

    private int currentPage = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        emptyPane.visibleProperty().bind(viewModel.emptyProperty());
        loadingPane.visibleProperty().bind(viewModel.loadingProperty());
        BooleanBinding contentVisibleBinding = Bindings.and(viewModel.emptyProperty().not(), viewModel.loadingProperty().not());
        cardArea.visibleProperty().bind(contentVisibleBinding);

        // HBox fills available height
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        leftArrow.setOnMouseClicked(e -> {
            if (currentPage > 0) {
                currentPage--;
                rebuildCards();
            }
        });
        rightArrow.setOnMouseClicked(e -> {
            int totalPages = (int) Math.ceil((double) viewModel.getAnalysisDataList().size() / PAGE_SIZE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                rebuildCards();
            }
        });

        viewModel.getAnalysisDataList().addListener((ListChangeListener<AnalysisData>) change -> {
            currentPage = 0;
            rebuildCards();
        });
    }

    private void rebuildCards() {
        List<AnalysisData> allData = viewModel.getAnalysisDataList();
        int totalPages = (int) Math.ceil((double) allData.size() / PAGE_SIZE);
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allData.size());
        int cardCount = end - start;

        contentPane.getChildren().clear();
        for (int i = start; i < end; i++) {
            contentPane.getChildren().add(createPoolCard(allData.get(i), cardCount));
        }

        leftArrow.setDisable(currentPage <= 0);
        rightArrow.setDisable(currentPage >= totalPages - 1 || totalPages <= 1);
    }

    private VBox createPoolCard(AnalysisData data, int cardCount) {
        VBox card = new VBox();
        card.getStyleClass().add("pool-pane");

        // 每个卡池宽度固定为 容器宽度/4（PAGE_SIZE），
        // 不论实际卡池数量：4 个时正好铺满，2 个时保持同样宽度不拉伸，多出空间留白
        double spacing = contentPane.getSpacing();
        card.prefWidthProperty().bind(contentPane.widthProperty()
                .subtract(spacing * (PAGE_SIZE - 1))
                .divide(PAGE_SIZE));

        // Title row: pool name + total count
        StackPane titleRow = new StackPane();
        Label titleLabel = new Label(data.getPoolName());
        titleLabel.getStyleClass().add("title-3");
        StackPane.setAlignment(titleLabel, Pos.CENTER_LEFT);
        Label totalLabel = new Label(String.valueOf(data.getTotalCount()));
        totalLabel.getStyleClass().addAll("title-3","total-count");
        StackPane.setAlignment(totalLabel, Pos.CENTER_RIGHT);
        titleRow.getChildren().addAll(titleLabel, totalLabel);

        // Date
        Label dateLabel = new Label();
        dateLabel.getStyleClass().add("text-subtle");
        if (data.isEmpty()) {
            dateLabel.setText("No Data");
        } else {
            dateLabel.setText(data.getStartDate() + " - " + data.getEndDate());
        }

        Separator sep1 = new Separator();

        // SR no-up
        StackPane srNoUpRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.sr.noup"),
                data.isEmpty() ? "0" : String.valueOf(data.getNoUpSrCount()));
        srNoUpRow.getStyleClass().add("sr-no-up");

        // SSR no-up
        StackPane ssrNoUpRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.ssr.noup"),
                data.isEmpty() ? "0" : String.valueOf(data.getNoUpSsrCount()));
        ssrNoUpRow.getStyleClass().add("ssr-no-up");

        // SSR avg
        StackPane ssrAvgRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.ssr.avg"),
                data.isEmpty() ? "0" : String.format(AVG_TEMPLATE, data.getSsrAvg()));
        ssrAvgRow.getStyleClass().add("ssr-avg");

        // UP SSR avg
        StackPane upSsrAvgRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.ssr.up.avg"),
                data.isEmpty() ? "0" : String.format(AVG_TEMPLATE, data.getUpSsrAvg()));
        upSsrAvgRow.getStyleClass().add("up-ssr-avg");

        Separator sep2 = new Separator();
        // SSR count
        StackPane ssrCountRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.ssr.count"),
                data.isEmpty() ? "0" : String.format(PERCENT_TEMPLATE, data.getSsrCount(),
                        (double) data.getSsrCount() / data.getTotalCount() * 100.0));
        ssrCountRow.getStyleClass().add("ssr-count");

        // SR count
        StackPane srCountRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.sr.count"),
                data.isEmpty() ? "0" : String.format(PERCENT_TEMPLATE, data.getSrCount(),
                        (double) data.getSrCount() / data.getTotalCount() * 100.0));
        srCountRow.getStyleClass().add("sr-count");


        // UP SSR count
        StackPane upSsrCountRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.ssr.up.count"),
                data.isEmpty() ? "0" : String.format(PERCENT_TEMPLATE, data.getUpSsrCount(),
                        (double) data.getUpSsrCount() / data.getTotalCount() * 100.0));
        upSsrCountRow.getStyleClass().add("up-ssr-count");
        if ( data.getUpSsrCount() == 0){
            upSsrCountRow.setVisible(false);
        }


        // Non-banner rate
        StackPane nonBannerRateRow = createConclusionRow(
                Config.language.getString("ui.analysis.common.ssr.non.banner.rate"),
                data.isEmpty() ? "0" : String.format("%.2f%%", data.getNonBannerRate() * 100));
        nonBannerRateRow.getStyleClass().add("non-banner-rate");

        if (data.getUpRate() == null || data.getUpRate() == 0){
            nonBannerRateRow.setVisible(false);
        }


        // SSR list view
        ListView<SsrData> listView = new ListView<>();
        listView.setCellFactory(lv -> new SsrCell());
        Label placeholder = new Label(Config.language.getString("ui.analysis.pool.tip.empty02"));
        placeholder.setStyle("-fx-font-weight: bold;");
        listView.setPlaceholder(placeholder);
        VBox.setVgrow(listView, Priority.ALWAYS);
        if (!data.isEmpty() && data.getSsrDataList() != null) {
            listView.getItems().setAll(data.getSsrDataList());
        }

        card.getChildren().addAll(titleRow, dateLabel, sep1, srNoUpRow, ssrNoUpRow, ssrAvgRow, upSsrAvgRow,
                sep2, srCountRow, ssrCountRow, upSsrCountRow, nonBannerRateRow, listView);

        return card;
    }

    private StackPane createConclusionRow(String leftText, String rightText) {
        StackPane row = new StackPane();
        row.getStyleClass().add("conclusion");
        Label left = new Label(leftText);
        StackPane.setAlignment(left, Pos.CENTER_LEFT);
        Label right = new Label(rightText);
        StackPane.setAlignment(right, Pos.CENTER_RIGHT);
        row.getChildren().addAll(left, right);
        return row;
    }

    class SsrCell extends ListCell<SsrData> {
        private final BorderPane root;
        private ImageView iv;
        private Label name;
        private Label date;
        private Label count;
        private Label upLabel;
        private ProgressBar progressBar;

        public SsrCell() {
            root = new BorderPane();

            iv = new ImageView();
            iv.setFitHeight(36);
            iv.setFitWidth(36);
            iv.setSmooth(true);

            name = new Label();
            name.getStyleClass().add("role-name");
            date = new Label();
            date.getStyleClass().add("role-date");
            VBox center = new VBox(name, date);
            center.setPadding(new Insets(0, 0, 0, 5));
            center.setAlignment(Pos.CENTER_LEFT);

            upLabel = new Label("UP!");
            upLabel.getStyleClass().add("up-tag");
            upLabel.setVisible(false);
            Label desc = new Label();
            desc.getStyleClass().add("role-desc");
            count = new Label();
            count.getStyleClass().add("role-name");
            HBox left = new HBox(8.0, upLabel, count);
            left.setAlignment(Pos.CENTER_RIGHT);

            progressBar = new ProgressBar();
            progressBar.setProgress(0);
            AnchorPane bottom = new AnchorPane(progressBar);
            AnchorPaneUtil.setPosition(progressBar, 0);

            root.setLeft(iv);
            root.setCenter(center);
            root.setRight(left);
            root.setBottom(bottom);

            root.getStyleClass().addAll("role-cell");
        }

        @Override
        protected void updateItem(SsrData ssrData, boolean b) {
            super.updateItem(ssrData, b);
            if (!b) {
                Thread.startVirtualThread(() -> {
                    Image image = LocalResourcesManager.header(ssrData.getId(), 60, 60);
                    Platform.runLater(() -> iv.setImage(image));
                });

                name.setText(ssrData.getName());
                date.setText(ssrData.getDate());
                count.setText(String.format("%02d", ssrData.getCount()));
                progressBar.setProgress(ssrData.getCount() / 80.0);

                if (ssrData.isEvent()) {
                    upLabel.setVisible(true);
                    count.getStyleClass().remove("unup");
                    count.getStyleClass().add("up");
                    progressBar.getStyleClass().remove("unup");
                    progressBar.getStyleClass().add("up");
                } else {
                    upLabel.setVisible(false);
                    count.getStyleClass().remove("up");
                    count.getStyleClass().add("unup");
                    progressBar.getStyleClass().remove("up");
                    progressBar.getStyleClass().add("unup");
                }
                setGraphic(root);
            } else {
                setGraphic(null);
            }
        }
    }
}
