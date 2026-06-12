package cn.tealc.wutheringwavestool.ui.game;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-11-16 22:44
 */
public class GameRecordView implements FxmlView<GameRecordViewModel>, Initializable {

    @InjectViewModel
    private GameRecordViewModel viewModel;

    @FXML
    private AnchorPane root;

    @FXML
    private ComboBox<String> accountComboBox;
    @FXML
    private Label battle;
    @FXML
    private Label paralysis;
    @FXML
    private Label parry;
    @FXML
    private Label parryAttack;
    @FXML
    private Label phantomGet;
    @FXML
    private Label phantomSkill;
    @FXML
    private Label roleChange;
    @FXML
    private Label roleDeath;
    @FXML
    private Label totalBattle;
    @FXML
    private Label totalParalysis;
    @FXML
    private Label totalParry;
    @FXML
    private Label totalParryAttack;
    @FXML
    private Label totalPhantomGet;
    @FXML
    private Label totalPhantomSkill;
    @FXML
    private Label totalRoleChange;
    @FXML
    private Label totalRoleDeath;
    @FXML
    private Label totalTransfer;
    @FXML
    private Label transfer;

    private static final String ROLE_IMAGE_DIR = "/cn/tealc/wutheringwavestool/image/role/";
    private static final String[] ROLE_IMAGES = {
            "1211.png", "1108.png", "1210.png", "1508.png", "1509.png",
            "1409.png", "1505.png", "1205.png", "1304.png", "1604.png",
            "1107.png", "1506.png", "1607.png"
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleChange.textProperty().bind(Bindings.concat("今日 ", viewModel.roleChangeProperty().asString()));
        roleDeath.textProperty().bind(Bindings.concat("今日 ", viewModel.roleDeathProperty().asString()));
        battle.textProperty().bind(Bindings.concat("今日 ", viewModel.battleProperty().asString()));
        phantomGet.textProperty().bind(Bindings.concat("今日 ", viewModel.phantomGetProperty().asString()));
        phantomSkill.textProperty().bind(Bindings.concat("今日 ", viewModel.phantomSkillProperty().asString()));
        paralysis.textProperty().bind(Bindings.concat("今日 ", viewModel.paralysisProperty().asString()));
        transfer.textProperty().bind(Bindings.concat("今日 ", viewModel.transferProperty().asString()));
        parry.textProperty().bind(Bindings.concat("今日 ", viewModel.parryProperty().asString()));
        parryAttack.textProperty().bind(Bindings.concat("今日 ", viewModel.parryAttackProperty().asString()));

        totalRoleChange.textProperty().bind(Bindings.concat("总计 ", viewModel.totalRoleChangeProperty().asString()));
        totalRoleDeath.textProperty().bind(Bindings.concat("总计 ", viewModel.totalRoleDeathProperty().asString()));
        totalBattle.textProperty().bind(Bindings.concat("总计 ", viewModel.totalBattleProperty().asString()));
        totalPhantomGet.textProperty().bind(Bindings.concat("总计 ", viewModel.totalPhantomGetProperty().asString()));
        totalPhantomSkill.textProperty().bind(Bindings.concat("总计 ", viewModel.totalPhantomSkillProperty().asString()));
        totalParalysis.textProperty().bind(Bindings.concat("总计 ", viewModel.totalParalysisProperty().asString()));
        totalTransfer.textProperty().bind(Bindings.concat("总计 ", viewModel.totalTransferProperty().asString()));
        totalParry.textProperty().bind(Bindings.concat("总计 ", viewModel.totalParryProperty().asString()));
        totalParryAttack.textProperty().bind(Bindings.concat("总计 ", viewModel.totalParryAttackProperty().asString()));

        accountComboBox.setItems(viewModel.getRoleIdList());
        accountComboBox.getSelectionModel().select(viewModel.getRoleIdIndex());
        accountComboBox.getSelectionModel().selectedIndexProperty().addListener(
                (observableValue, number, t1) -> viewModel.updateIndex(t1.intValue()));

        assignRandomAvatars();
    }

    private void assignRandomAvatars() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < ROLE_IMAGES.length; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, new Random());

        root.lookupAll(".avatar").forEach(node -> {
            if (node instanceof ImageView imageView) {
                if (indices.isEmpty()) {
                    for (int i = 0; i < ROLE_IMAGES.length; i++) indices.add(i);
                    Collections.shuffle(indices, new Random());
                }
                int idx = indices.remove(0);
                Image image = new Image(
                        getClass().getResource(ROLE_IMAGE_DIR + ROLE_IMAGES[idx]).toExternalForm(),
                        60, 60, true, true);
                imageView.setImage(image);

                Circle clip = new Circle(30, 30, 30);
                imageView.setClip(clip);
            }
        });
    }
}