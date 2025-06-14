package cn.tealc.wutheringwavestool.ui.kujiequ;

import atlantafx.base.controls.Spacer;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
    private ImageView starIcon;

    @FXML
    private VBox starGroup,coinGroup;

    @FXML
    private Label starNumLabel,coinNumLabel;

    public void initialize() {
        starNumLabel.textProperty().bind(viewModel.starNumProperty().asString());
        coinNumLabel.textProperty().bind(viewModel.coinNumProperty().asString());
        recordTypeBox.setItems(viewModel.getRecordTypeList());

        viewModel.getStarList().addListener((ListChangeListener<? super Item>) change -> {
            starGroup.getChildren().clear();
            for (Item item : change.getList()) {
                starGroup.getChildren().add(new ItemCell(item,viewModel.getStarNum()));
            }
        });

        viewModel.getCoinList().addListener((ListChangeListener<? super Item>) change -> {
            coinGroup.getChildren().clear();
            for (Item item : change.getList()) {
                coinGroup.getChildren().add(new ItemCell(item,viewModel.getCoinNum()));
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


    static class ItemCell extends HBox{
        public ItemCell(Item item,long size) {
            Label name = new Label(item.getType());
            double numValue = item.getNum();
            Label num;
            if (size > 0){
                num = new Label(String.format("%d -- %02.1f%%", item.getNum(), numValue / (double) size * 100));
            }else {
                num = new Label(String.format("%d", item.getNum()));
            }

            getChildren().addAll(name,new Spacer(),num);
        }
    }
}