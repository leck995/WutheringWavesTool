package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import atlantafx.base.controls.ProgressSliderSkin;
import com.kuro.kujiequ.model.calculator.result.Cost;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 22:33
 */
public class CalculatorWeaponEditView implements FxmlView<CalculatorWeaponEditViewModel>, Initializable {
    @InjectViewModel
    private CalculatorWeaponEditViewModel viewModel;
    @FXML
    private FlowPane allItemFlowPane;

    @FXML
    private Slider endSlider;

    @FXML
    private ImageView iconIV;

    @FXML
    private FlowPane lackItemFlowPane;

    @FXML
    private Label startLabel;

    @FXML
    private AnchorPane root;

    @FXML
    private Label endLabel;

    @FXML
    private Slider startSlider;

    @FXML
    private Label weaponNameLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        weaponNameLabel.textProperty().bind(viewModel.weaponNameProperty());
        iconIV.imageProperty().bind(viewModel.iconProperty());

        setSliderSkin(startSlider);
        setSliderSkin(endSlider);
        startSlider.valueProperty().bindBidirectional(viewModel.startLevelProperty());
        endSlider.valueProperty().bindBidirectional(viewModel.endLevelProperty());
        startLabel.textProperty().bind(startSlider.valueProperty().asString("LV%02.0f"));
        endLabel.textProperty().bind(endSlider.valueProperty().asString("LV%02.0f"));


        viewModel.getTotalCostList().addListener((ListChangeListener<? super Cost>) change -> {
            allItemFlowPane.getChildren().clear();
            for (Cost cost : change.getList()) {
                allItemFlowPane.getChildren().add(new Cell(cost));
            }
        });

        viewModel.getMissingCostList().addListener((ListChangeListener<? super Cost>) change -> {
            lackItemFlowPane.getChildren().clear();
            for (Cost cost : change.getList()) {
                lackItemFlowPane.getChildren().add(new Cell(cost));
            }
        });
    }

    private void setSliderSkin(Slider slider) {
        ProgressSliderSkin skin = new ProgressSliderSkin(slider);
        slider.setSkin(skin);
    }

    @FXML
    void back(ActionEvent event) {
        Pane parent = (Pane) root.getParent();
        parent.getChildren().remove(root);
        parent.getChildren().forEach(node -> node.setVisible(true));
    }

    @FXML
    void calculate(ActionEvent event) {
        viewModel.calculate();
    }

    class Cell extends StackPane {
        private ImageView iv;
        private Label nameLabel;
        private Label numLabel;

        private Cell(Cost cost) {
            iv = new ImageView(new Image(cost.getIconUrl(), 60, 60, true, true, true));
            nameLabel = new Label(cost.getName());
            numLabel = new Label(String.valueOf(cost.getNum()));
            VBox vbox = new VBox(3.0, iv, nameLabel,numLabel);
            vbox.setAlignment(Pos.CENTER);
            getChildren().add(vbox);
        }
    }

}