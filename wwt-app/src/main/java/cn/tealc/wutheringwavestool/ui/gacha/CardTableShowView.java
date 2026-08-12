package cn.tealc.wutheringwavestool.ui.gacha;

import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class CardTableShowView implements FxmlView<CardTableShowViewModel>, Initializable {

    @InjectViewModel
    private CardTableShowViewModel viewModel;

    @FXML
    private TableView<CardInfo> cardTable;
    @FXML
    private TableColumn<CardInfo, String> nameCol;
    @FXML
    private TableColumn<CardInfo, Integer> qualityCol;
    @FXML
    private TableColumn<CardInfo, String> typeCol;
    @FXML
    private TableColumn<CardInfo, String> poolCol;
    @FXML
    private TableColumn<CardInfo, String> timeCol;

    @FXML
    private VBox poolTypeBar;
    @FXML
    private Button firstBtn, prevBtn, nextBtn, lastBtn;
    @FXML
    private Label pageInfoLabel, totalCountLabel, poolNameLabel;
    @FXML
    private ComboBox<Integer> pageSizeCombo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        qualityCol.setCellValueFactory(new PropertyValueFactory<>("qualityLevel"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("resourceType"));
        poolCol.setCellValueFactory(new PropertyValueFactory<>("cardPoolType"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));

        cardTable.setItems(viewModel.getPagedData());

        cardTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(CardInfo item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("ssr-row", "sr-row");
                if (!empty && item != null) {
                    switch (item.getQualityLevel()) {
                        case 5 -> getStyleClass().add("ssr-row");
                        case 4 -> getStyleClass().add("sr-row");
                    }
                }
            }
        });

        pageSizeCombo.getItems().setAll(10, 20, 50, 100);
        pageSizeCombo.setValue(viewModel.getPageSize());
        pageSizeCombo.setOnAction(e ->
                viewModel.pageSizeProperty().set(pageSizeCombo.getValue()));

        viewModel.pageSizeProperty().addListener((obs, old, val) -> {
            if (!val.equals(pageSizeCombo.getValue())) {
                pageSizeCombo.setValue(val.intValue());
            }
        });

        firstBtn.disableProperty().bind(viewModel.hasPrevProperty().not());
        prevBtn.disableProperty().bind(viewModel.hasPrevProperty().not());
        nextBtn.disableProperty().bind(viewModel.hasNextProperty().not());
        lastBtn.disableProperty().bind(viewModel.hasNextProperty().not());

        pageInfoLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> viewModel.getCurrentPage() + " / " + viewModel.getTotalPages(),
                        viewModel.currentPageProperty(), viewModel.totalPagesProperty()));

        totalCountLabel.textProperty().bind(
                viewModel.totalCountProperty().asString());

        poolNameLabel.textProperty().bind(viewModel.selectedPoolTypeProperty());

        ToggleGroup poolGroup = new ToggleGroup();
        viewModel.getPoolTypes().addListener((javafx.collections.ListChangeListener<String>) c -> {
            poolTypeBar.getChildren().clear();
            while (c.next()) {
                for (String poolType : c.getAddedSubList()) {
                    ToggleButton btn = new ToggleButton();

                    String replace = poolType.replace("唤取", "").replace(" Pull","");
                    if (replace.contains("感恩定向")){
                        btn.setText("感恩定向");
                    }else if (replace.contains("Gratitude Directional Pull")){
                        btn.setText("Gratitude Directional Pull");
                    }else {
                        btn.setText(replace);
                    }

                    btn.setToggleGroup(poolGroup);
                    btn.getStyleClass().add("child-select");
                    btn.setOnAction(e -> viewModel.selectedPoolTypeProperty().set(poolType));
                    poolTypeBar.getChildren().add(btn);
                }
            }
            if (!poolTypeBar.getChildren().isEmpty()) {
                ((ToggleButton) poolTypeBar.getChildren().get(0)).setSelected(true);
            }
        });

        viewModel.selectedPoolTypeProperty().addListener((obs, old, val) -> {
            if (val != null) {
                for (javafx.scene.Node node : poolTypeBar.getChildren()) {
                    ToggleButton btn = (ToggleButton) node;
                    if (val.equals(btn.getText())) {
                        btn.setSelected(true);
                        break;
                    }
                }
            }
        });

        cardTable.visibleProperty().bind(viewModel.emptyProperty().not());
    }

    @FXML
    void onFirstPage(ActionEvent event) { viewModel.firstPage(); }

    @FXML
    void onPrevPage(ActionEvent event) { viewModel.prevPage(); }

    @FXML
    void onNextPage(ActionEvent event) { viewModel.nextPage(); }

    @FXML
    void onLastPage(ActionEvent event) { viewModel.lastPage(); }
}
