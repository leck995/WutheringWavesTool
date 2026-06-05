package cn.tealc.wutheringwavestool.ui.kujiequ.tower;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.ui.game.GameRecordView;
import cn.tealc.wutheringwavestool.ui.game.GameRecordViewModel;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class TowerGroupView implements FxmlView<TowerGroupViewModel> {
    @InjectViewModel
    private TowerGroupViewModel viewModel;
    @FXML
    private ToggleGroup childSelectedToggle;
    @FXML
    private StackPane content;
    @FXML
    private HBox headerPane;

    private Node towerView;
    private Node slashView;
    private Node newTowerView;

    public void initialize() {
        createTowerView();
    }



    @FXML
    void toSlash(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            if (slashView == null) {
                ViewTuple<SlashView, SlashViewModel> viewTuple = FluentViewLoader.fxmlView(SlashView.class).load();
                slashView = viewTuple.getView();
            }
            content.getChildren().setAll(slashView);
            Animations.slideInUp(slashView, Duration.millis(300)).play();
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toNewTower(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            if (newTowerView == null) {
                ViewTuple<NewTowerView, NewTowerViewModel> viewTuple = FluentViewLoader.fxmlView(NewTowerView.class).load();
                newTowerView = viewTuple.getView();
            }
            content.getChildren().setAll(newTowerView);
            Animations.slideInUp(newTowerView, Duration.millis(300)).play();
        }else {
            toggleButton.setSelected(true);
        }
    }


    @FXML
    void toTower(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            createTowerView();
        }else {
            toggleButton.setSelected(true);
        }
    }

    private void createTowerView(){
        if (towerView == null) {
            ViewTuple<TowerView, TowerViewModel> viewTuple = FluentViewLoader.fxmlView(TowerView.class).load();
            towerView = viewTuple.getView();
        }
        content.getChildren().setAll(towerView);
        Animations.slideInUp(towerView, Duration.millis(300)).play();
    }
}