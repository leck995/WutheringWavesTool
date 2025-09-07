package cn.tealc.wutheringwavestool.ui.kujiequ.calculator;

import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @description:
 * @author: Leck
 * @create: 2025-03-26 17:00
 */
public class CalculatorView implements FxmlView<CalculatorViewModel>, Initializable {
    @InjectViewModel
    private CalculatorViewModel viewModel;
    @FXML
    private StackPane root;
    @FXML
    private TabPane tabPane;
    @FXML
    private ToggleGroup childSelectedToggle;
    @FXML
    private FlowPane roleFlowPane;
    @FXML
    private FlowPane weaponFlowPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        viewModel.getRoleList().addListener((ListChangeListener<? super RoleForCalculator>) change -> {
            roleFlowPane.getChildren().clear();
            for (RoleForCalculator role : change.getList()) {
                roleFlowPane.getChildren().add(new Cell(role));
            }
        });

        viewModel.getWeaponList().addListener((ListChangeListener<? super WeaponForCalculator>) change -> {
            weaponFlowPane.getChildren().clear();
            for (WeaponForCalculator weapon : change.getList()) {
                weaponFlowPane.getChildren().add(new Cell(weapon));
            }
        });


        viewModel.ready();
    }


    @FXML
    void toRoleTab(ActionEvent event) {
        tabPane.getSelectionModel().select(0);
        Animations.slideInUp(tabPane.getTabs().get(0).getContent(), Duration.millis(300)).play();
    }

    @FXML
    void toWeaponTab(ActionEvent event) {
        tabPane.getSelectionModel().select(1);
        Animations.slideInUp(tabPane.getTabs().get(1).getContent(), Duration.millis(300)).play();
    }


    class Cell extends StackPane {
        private ImageView iv;
        private Label nameLabel;

        public Cell(RoleForCalculator role) {
            this(role.getRoleId(), role.getRoleName(), role.getRoleIconUrl());
            setOnMouseClicked(event -> {
                showRole(role);
            });
        }

        public Cell(WeaponForCalculator weapon) {
            this(weapon.getWeaponId(), weapon.getWeaponName(), weapon.getWeaponIcon());
            setOnMouseClicked(event -> {
                showWeapon(weapon);
            });
        }

        private Cell(int id, String name, String iconUrl) {
            Image image = LocalResourcesManager.header(id, 60, 60);
            if (image != null) {
                iv = new ImageView(image);
            } else {
                iv = new ImageView(new Image(iconUrl, 60, 60, true, true, true));
            }
            nameLabel = new Label(name);
            VBox vbox = new VBox(3.0, iv, nameLabel);
            vbox.setAlignment(Pos.CENTER);
            getChildren().add(vbox);
            getStyleClass().add("item-cell");
        }


        private void showRole(RoleForCalculator role) {
            root.getChildren().forEach(node -> node.setVisible(false));
            ViewTuple<CalculatorRoleEditView, CalculatorRoleEditViewModel> viewTuple =
                    FluentViewLoader.fxmlView(CalculatorRoleEditView.class).viewModel(new CalculatorRoleEditViewModel(role,iv.getImage())).load();
            root.getChildren().add(viewTuple.getView());

        }


        private void showWeapon(WeaponForCalculator weapon) {
            root.getChildren().forEach(node -> node.setVisible(false));
            ViewTuple<CalculatorWeaponEditView, CalculatorWeaponEditViewModel> viewTuple =
                    FluentViewLoader.fxmlView(CalculatorWeaponEditView.class).viewModel(new CalculatorWeaponEditViewModel(weapon,iv.getImage())).load();
            root.getChildren().add(viewTuple.getView());

        }
    }
}