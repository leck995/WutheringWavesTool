package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import atlantafx.base.controls.ProgressSliderSkin;
import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.teafx.utils.message.MessageType;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.calculator.result.Cost;
import de.saxsys.mvvmfx.*;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import org.controlsfx.control.RangeSlider;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

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
    private ImageView iconIV;
    @FXML
    private FlowPane allItemFlowPane,lackItemFlowPane,relateItemFlowPane;
    @FXML
    private GridPane otherSkillGroup;
    @FXML
    private Label roleNameLabel;
    @FXML
    private Label levelHighLabel,levelLowLabel;
    @FXML
    private Label skillHighLabel01,skillLowLabel01;
    @FXML
    private Label skillHighLabel02,skillLowLabel02;
    @FXML
    private Label skillHighLabel03,skillLowLabel03;
    @FXML
    private Label skillHighLabel04,skillLowLabel04;
    @FXML
    private Label skillHighLabel05,skillLowLabel05;
    @FXML
    private RangeSlider levelSlider;
    @FXML
    private RangeSlider skillSlider01;
    @FXML
    private RangeSlider skillSlider02;
    @FXML
    private RangeSlider skillSlider03;
    @FXML
    private RangeSlider skillSlider04;
    @FXML
    private RangeSlider skillSlider05;
    @FXML
    private CheckBox skillBreak01, skillBreak02,skillBreak03, skillBreak04,skillBreak05,
            skillBreak06,skillBreak07,skillBreak08,skillBreak09, skillBreak10;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleNameLabel.textProperty().bind(viewModel.roleNameProperty());
        iconIV.imageProperty().bind(viewModel.iconProperty());

        levelHighLabel.textProperty().bind(levelSlider.highValueProperty().asString("%02.0f"));
        levelLowLabel.textProperty().bind(levelSlider.lowValueProperty().asString("%02.0f"));
        levelSlider.highValueProperty().bindBidirectional(viewModel.roleHighLevelProperty());
        levelSlider.lowValueProperty().bindBidirectional(viewModel.roleLowLevelProperty());

        skillSlider01.highValueProperty().bindBidirectional(viewModel.skillHighLevel01Property());
        skillSlider02.highValueProperty().bindBidirectional(viewModel.skillHighLevel02Property());
        skillSlider03.highValueProperty().bindBidirectional(viewModel.skillHighLevel03Property());
        skillSlider04.highValueProperty().bindBidirectional(viewModel.skillHighLevel04Property());
        skillSlider05.highValueProperty().bindBidirectional(viewModel.skillHighLevel05Property());

        skillSlider01.lowValueProperty().bindBidirectional(viewModel.skillLowLevel01Property());
        skillSlider02.lowValueProperty().bindBidirectional(viewModel.skillLowLevel02Property());
        skillSlider03.lowValueProperty().bindBidirectional(viewModel.skillLowLevel03Property());
        skillSlider04.lowValueProperty().bindBidirectional(viewModel.skillLowLevel04Property());
        skillSlider05.lowValueProperty().bindBidirectional(viewModel.skillLowLevel05Property());

        skillHighLabel01.textProperty().bind(skillSlider01.highValueProperty().asString("%02.0f"));
        skillLowLabel01.textProperty().bind(skillSlider01.lowValueProperty().asString("%02.0f"));
        skillHighLabel02.textProperty().bind(skillSlider02.highValueProperty().asString("%02.0f"));
        skillLowLabel02.textProperty().bind(skillSlider02.lowValueProperty().asString("%02.0f"));
        skillHighLabel03.textProperty().bind(skillSlider03.highValueProperty().asString("%02.0f"));
        skillLowLabel03.textProperty().bind(skillSlider03.lowValueProperty().asString("%02.0f"));
        skillHighLabel04.textProperty().bind(skillSlider04.highValueProperty().asString("%02.0f"));
        skillLowLabel04.textProperty().bind(skillSlider04.lowValueProperty().asString("%02.0f"));
        skillHighLabel05.textProperty().bind(skillSlider05.highValueProperty().asString("%02.0f"));
        skillLowLabel05.textProperty().bind(skillSlider05.lowValueProperty().asString("%02.0f"));



        skillBreak01.selectedProperty().bindBidirectional(viewModel.skillBreak01Property());
        skillBreak02.selectedProperty().bindBidirectional(viewModel.skillBreak02Property());
        skillBreak03.selectedProperty().bindBidirectional(viewModel.skillBreak03Property());
        skillBreak04.selectedProperty().bindBidirectional(viewModel.skillBreak04Property());
        skillBreak05.selectedProperty().bindBidirectional(viewModel.skillBreak05Property());
        skillBreak06.selectedProperty().bindBidirectional(viewModel.skillBreak06Property());
        skillBreak07.selectedProperty().bindBidirectional(viewModel.skillBreak07Property());
        skillBreak08.selectedProperty().bindBidirectional(viewModel.skillBreak08Property());
        skillBreak09.selectedProperty().bindBidirectional(viewModel.skillBreak09Property());
        skillBreak10.selectedProperty().bindBidirectional(viewModel.skillBreak010Property());



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
        viewModel.getRelateCostList().addListener((ListChangeListener<? super Cost>) change -> {
            relateItemFlowPane.getChildren().clear();
            for (Cost cost : change.getList()) {
                relateItemFlowPane.getChildren().add(new Cell(cost));
            }
        });
        viewModel.ready();
    }


    @FXML
    void calculate(ActionEvent event) {
        boolean checked = checkNeedCalculate();
        if (checked) {
            ObservableList<Node> children = otherSkillGroup.getChildren();
            List<String> skill = children.filtered(node -> {
                if (node instanceof CheckBox checkBox)
                    return checkBox.isSelected();
                return false;
            }).stream().map(Node::getAccessibleText).toList();
            viewModel.calculate(skill);
        }else {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,
                    MessageInfo.success("当前角色无需任何材料"));
        }
    }

    @FXML
    void back(ActionEvent event) {
        Pane parent = (Pane) root.getParent();
        parent.getChildren().remove(root);
        parent.getChildren().forEach(node -> node.setVisible(true));


    }

    @FXML
    void reset(ActionEvent event) {
        levelSlider.setHighValue(90);
        levelSlider.setLowValue(1);
        skillSlider01.setHighValue(10);
        skillSlider02.setHighValue(10);
        skillSlider03.setHighValue(10);
        skillSlider04.setHighValue(10);
        skillSlider05.setHighValue(10);
        skillSlider01.setLowValue(1);
        skillSlider02.setLowValue(1);
        skillSlider03.setLowValue(1);
        skillSlider04.setLowValue(1);
        skillSlider05.setLowValue(1);
        skillBreak01.setSelected(true);
        skillBreak02.setSelected(true);
        skillBreak03.setSelected(true);
        skillBreak04.setSelected(true);
        skillBreak05.setSelected(true);
        skillBreak06.setSelected(true);
        skillBreak07.setSelected(true);
        skillBreak08.setSelected(true);
        skillBreak09.setSelected(true);
        skillBreak10.setSelected(true);
    }



    private boolean checkNeedCalculate() {
        // 检查所有 Slider 的 highValue != lowValue
        boolean hasSliderChanged = Stream.of(levelSlider, skillSlider01, skillSlider02,skillSlider03,skillSlider04,skillSlider05)
                .anyMatch(slider -> slider.getHighValue() != slider.getLowValue());

        // 检查是否有任意 CheckBox 被选中
        boolean hasBreakSelected = Stream.of(skillBreak01, skillBreak02, skillBreak03, skillBreak04, skillBreak05,
                        skillBreak06, skillBreak07, skillBreak08, skillBreak09, skillBreak10)
                .anyMatch(CheckBox::isSelected);
        return hasSliderChanged || hasBreakSelected;
    }

    public Label getSkillLowLabel03() {
        return skillLowLabel03;
    }

    public void setSkillLowLabel03(Label skillLowLabel03) {
        this.skillLowLabel03 = skillLowLabel03;
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