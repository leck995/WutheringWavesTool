package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import atlantafx.base.controls.ProgressSliderSkin;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.calculator.result.Cost;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 19:42
 */
public class CalculatorRoleEditView implements FxmlView<CalculatorRoleEditViewModel>, Initializable {
    @InjectViewModel
    private CalculatorRoleEditViewModel viewModel;

    @FXML
    private AnchorPane root;
    @FXML
    private FlowPane allItemFlowPane;
    @FXML
    private ImageView iconIV;
    @FXML
    private FlowPane lackItemFlowPane;
    @FXML
    private Label levelLabel;
    @FXML
    private Slider levelSlider;
    @FXML
    private GridPane otherSkillGroup;
    @FXML
    private Label roleNameLabel;
    @FXML
    private Label skillLabel01;
    @FXML
    private Label skillLabel02;
    @FXML
    private Label skillLabel03;
    @FXML
    private Label skillLabel04;
    @FXML
    private Label skillLabel05;
    @FXML
    private Slider skillSlider01;
    @FXML
    private Slider skillSlider02;
    @FXML
    private Slider skillSlider03;
    @FXML
    private Slider skillSlider04;
    @FXML
    private Slider skillSlider05;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleNameLabel.textProperty().bind(viewModel.roleNameProperty());
        iconIV.imageProperty().bind(viewModel.iconProperty());

        setSliderSkin(levelSlider);
        setSliderSkin(skillSlider01);
        setSliderSkin(skillSlider02);
        setSliderSkin(skillSlider03);
        setSliderSkin(skillSlider04);
        setSliderSkin(skillSlider05);
        levelSlider.valueProperty().bindBidirectional(viewModel.roleLevelProperty());
        levelLabel.textProperty().bind(levelSlider.valueProperty().asString("LV%02.0f"));

        skillSlider01.valueProperty().bindBidirectional(viewModel.skillLevel01Property());
        skillSlider02.valueProperty().bindBidirectional(viewModel.skillLevel02Property());
        skillSlider03.valueProperty().bindBidirectional(viewModel.skillLevel03Property());
        skillSlider04.valueProperty().bindBidirectional(viewModel.skillLevel04Property());
        skillSlider05.valueProperty().bindBidirectional(viewModel.skillLevel05Property());

        skillLabel01.textProperty().bind(skillSlider01.valueProperty().asString("LV%02.0f"));
        skillLabel02.textProperty().bind(skillSlider02.valueProperty().asString("LV%02.0f"));
        skillLabel03.textProperty().bind(skillSlider03.valueProperty().asString("LV%02.0f"));
        skillLabel04.textProperty().bind(skillSlider04.valueProperty().asString("LV%02.0f"));
        skillLabel05.textProperty().bind(skillSlider05.valueProperty().asString("LV%02.0f"));



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
    void calculate(ActionEvent event) {
        ObservableList<Node> children = otherSkillGroup.getChildren();
        List<String> skill = children.filtered(node -> {
            if (node instanceof CheckBox checkBox)
                return checkBox.isSelected();
            return false;
        }).stream().map(Node::getAccessibleText).toList();
        viewModel.calculate(skill);

    }

    @FXML
    void reset(ActionEvent event) {
        Pane parent = (Pane) root.getParent();
        parent.getChildren().remove(root);
        parent.getChildren().forEach(node -> node.setVisible(true));
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