package cn.tealc.wutheringwavestool.ui.kujiequ.other;

import atlantafx.base.controls.Spacer;
import cn.tealc.wutheringwavestool.ui.component.EmptyTipPane;
import com.kuro.kujiequ.model.resourcebriefing.Item;
import com.kuro.kujiequ.model.resourcebriefing.Title;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

public class ResourceBriefingView implements FxmlView<ResourceBriefingViewModel> {

    @InjectViewModel
    private ResourceBriefingViewModel viewModel;


    @FXML
    private ToggleGroup childSelectedToggle;

    @FXML
    private ToggleGroup childSelectedToggle1;

    @FXML
    private StackPane content;

    @FXML
    private HBox headerPane;

    @FXML
    private ComboBox<Title> recordTypeBox;

    @FXML
    private HBox actionBox;

    @FXML
    private ImageView starIcon;

    @FXML
    private VBox starGroup,coinGroup;

    @FXML
    private Label starNumLabel,coinNumLabel;
    @FXML
    private StackPane root;

    public void initialize() {
        starNumLabel.textProperty().bind(viewModel.starNumProperty().asString());
        coinNumLabel.textProperty().bind(viewModel.coinNumProperty().asString());
        recordTypeBox.setItems(viewModel.getRecordTypeList());

        viewModel.getStarList().addListener((ListChangeListener<? super Item>) change -> {
            starGroup.getChildren().clear();
            for (Item item : change.getList()) {
                starGroup.getChildren().add(new ItemCell(item, viewModel.getStarNum(), "star-bar"));
            }
        });

        viewModel.getCoinList().addListener((ListChangeListener<? super Item>) change -> {
            coinGroup.getChildren().clear();
            for (Item item : change.getList()) {
                coinGroup.getChildren().add(new ItemCell(item, viewModel.getCoinNum(), "coin-bar"));
            }
        });

        viewModel.subscribe(ResourceBriefingViewModel.EVENT_SELECT_BOX,(s, objects) -> {
            recordTypeBox.getSelectionModel().selectLast();
        });
        recordTypeBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue!=null) {
                viewModel.refresh(newValue);
            }
        });

        viewModel.subscribe("EMPTY",(s, objects) -> {
            EmptyTipPane tipPane = new EmptyTipPane("无主账号","请前往账号-库街区添加设置主账号", Material2MZ.PERSON_ADD_DISABLED);
            root.getChildren().setAll(tipPane);
        });
        viewModel.init();

        // 手机视图浏览按钮
        Button phoneBtn = new Button(null, new FontIcon(Material2MZ.SMARTPHONE));
        phoneBtn.getStyleClass().addAll("button-icon", "flat", "accent");
        phoneBtn.setTooltip(new Tooltip("手机视图浏览"));
        phoneBtn.setOnAction(e -> viewModel.openResourceBriefingInWebView());
        actionBox.getChildren().add(phoneBtn);
    }


    @FXML
    void toMonth(ActionEvent event) {
        viewModel.toMonth();
    }

    @FXML
    void toVersion(ActionEvent event) {
        viewModel.toVersion();
    }

    @FXML
    void toWeek(ActionEvent event) {
        viewModel.toWeek();
    }


    static class ItemCell extends VBox {
        private static final double BAR_WIDTH = 240.0;

        public ItemCell(Item item, long total, String barStyleClass) {
            setSpacing(3);
            getStyleClass().add("item-row-container");

            HBox row = new HBox();
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.getStyleClass().add("item-row");

            Label name = new Label(item.getType());
            name.getStyleClass().add("item-name");

            Label count = new Label(String.format("%,d", item.getNum()));
            count.getStyleClass().add("item-count");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(name, spacer, count);

            if (total > 0) {
                double pct = item.getNum() / (double) total;
                Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                pctLabel.getStyleClass().add("item-pct");
                row.getChildren().add(pctLabel);

                ProgressBar bar = new ProgressBar(pct);
                bar.setPrefWidth(BAR_WIDTH);
                bar.setMaxWidth(BAR_WIDTH);
                bar.getStyleClass().addAll("item-bar", barStyleClass);
                getChildren().addAll(row, bar);
            } else {
                getChildren().add(row);
            }
        }
    }
}