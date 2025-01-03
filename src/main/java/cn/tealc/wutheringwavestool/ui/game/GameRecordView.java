package cn.tealc.wutheringwavestool.ui.game;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.net.URL;
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
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleChange.textProperty().bind(viewModel.roleChangeProperty().asString());
        roleDeath.textProperty().bind(viewModel.roleDeathProperty().asString());
        battle.textProperty().bind(viewModel.battleProperty().asString());
        phantomGet.textProperty().bind(viewModel.phantomGetProperty().asString());
        phantomSkill.textProperty().bind(viewModel.phantomSkillProperty().asString());
        paralysis.textProperty().bind(viewModel.paralysisProperty().asString());
        transfer.textProperty().bind(viewModel.transferProperty().asString());
        parry.textProperty().bind(viewModel.parryProperty().asString());
        parryAttack.textProperty().bind(viewModel.parryAttackProperty().asString());


        totalRoleChange.textProperty().bind(viewModel.totalRoleChangeProperty().asString());
        totalRoleDeath.textProperty().bind(viewModel.totalRoleDeathProperty().asString());
        totalBattle.textProperty().bind(viewModel.totalBattleProperty().asString());
        totalPhantomGet.textProperty().bind(viewModel.totalPhantomGetProperty().asString());
        totalPhantomSkill.textProperty().bind(viewModel.totalPhantomSkillProperty().asString());
        totalParalysis.textProperty().bind(viewModel.totalParalysisProperty().asString());
        totalTransfer.textProperty().bind(viewModel.totalTransferProperty().asString());
        totalParry.textProperty().bind(viewModel.totalParryProperty().asString());
        totalParryAttack.textProperty().bind(viewModel.totalParryAttackProperty().asString());


        accountComboBox.setItems(viewModel.getRoleIdList());
        accountComboBox.getSelectionModel().select(viewModel.getRoleIdIndex());
        accountComboBox.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, t1) -> viewModel.updateIndex(t1.intValue()));


    }
}