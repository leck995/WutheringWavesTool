package cn.tealc.wutheringwavestool.ui.game.manage;

import atlantafx.base.theme.Styles;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.GameEdition;
import cn.tealc.wutheringwavestool.model.SourceType;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class GameBaseSettingView implements FxmlView<GameBaseSettingViewModel>, Initializable {
    @InjectViewModel private GameBaseSettingViewModel viewModel;

    @FXML private ComboBox<GameEdition> installationSelector;
    @FXML private TextField gameDirField;
    @FXML private TextField gameStartAppField;
    @FXML private TextField gameOfficialLauncherField;
    @FXML private StackPane gameStartAppGroup;
    @FXML private RadioButton gameStartAppRadioDefault;
    @FXML private ToggleGroup gameStartAppType;
    @FXML private ListView<String> paramListView;
    @FXML private TextField paramField;
    @FXML private ToggleGroup gameDxToggleGroup;
    @FXML private RadioButton mainlandServerButton;
    @FXML private RadioButton bilibiliServerButton;
    @FXML private RadioButton globalServerButton;
    @FXML private ToggleGroup serverSwitchSourceGroup;
    @FXML private Label currentServerLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        viewModel.init();
        initInstallationSettings();
        initStartParams();
        initServerSwitch();
    }

    private void initInstallationSettings() {
        gameDirField.textProperty().bindBidirectional(viewModel.gameDirProperty());
        gameStartAppField.textProperty().bindBidirectional(viewModel.startAppPathProperty());
        gameOfficialLauncherField.textProperty().bindBidirectional(viewModel.launcherPathProperty());
        gameDirField.setEditable(false);

        installationSelector.getItems().setAll(GameEdition.CHINA, GameEdition.GLOBAL);
        installationSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(GameEdition edition) {
                return edition == GameEdition.GLOBAL
                        ? LanguageManager.getString("ui.game_manager.base.global_installation")
                        : LanguageManager.getString("ui.game_manager.base.china_installation");
            }

            @Override
            public GameEdition fromString(String value) {
                return value != null && value.equals(LanguageManager.getString(
                        "ui.game_manager.base.global_installation"))
                        ? GameEdition.GLOBAL : GameEdition.CHINA;
            }
        });
        installationSelector.valueProperty().addListener((observable, oldValue, newValue) ->
                viewModel.setEditingEdition(newValue));

        gameStartAppGroup.disableProperty().bind(gameStartAppType.selectedToggleProperty()
                .isEqualTo(gameStartAppRadioDefault));
        viewModel.editingEditionProperty().addListener((observable, oldValue, newValue) -> refreshInstallationControls());
        refreshInstallationControls();
    }

    private void initStartParams() {
        paramField.setOnAction(event -> {
            String param = paramField.getText();
            if (!param.trim().isEmpty()) {
                viewModel.addParam(param);
                paramField.clear();
            }
        });
        paramListView.setItems(viewModel.getStartUpParams());
        paramListView.setCellFactory(list -> new ParamListCell());
        updateDxSelection();
    }

    private void initServerSwitch() {
        viewModel.currentServerProperty().addListener((observable, oldValue, newValue) -> updateServerSelection(newValue));
        updateServerSelection(viewModel.currentServerProperty().get());
    }

    @FXML
    void chooseGameDirectory(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LanguageManager.getString("ui.setting.file.game_dir.title"));
        File current = currentDirectory(gameDirField.getText());
        if (current != null) chooser.setInitialDirectory(current);
        File selected = chooser.showDialog(gameDirField.getScene().getWindow());
        if (selected == null) return;
        if (!new File(selected, "Wuthering Waves.exe").isFile()) {
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.game_manager.message01")));
            return;
        }
        viewModel.setGameDirectory(selected.toPath());
    }

    @FXML
    void chooseStartApp(ActionEvent event) {
        File selected = chooseExecutable("ui.setting.file.app.title");
        if (selected != null) viewModel.setStartAppPath(selected.getAbsolutePath());
    }

    @FXML
    void chooseLauncher(ActionEvent event) {
        File selected = chooseExecutable("ui.setting.default.app_updater");
        if (selected != null) viewModel.setLauncherPath(selected.getAbsolutePath());
    }

    @FXML
    void setAppPathMode(ActionEvent event) {
        viewModel.setStartAppMode(isCustomMode(event));
    }

    @FXML
    void setDX(ActionEvent event) {
        if (event.getSource() instanceof RadioButton button) {
            if ("DX11".equals(button.getText())) viewModel.addDx11();
            else if ("DX12".equals(button.getText())) viewModel.addDx12();
        }
    }

    @FXML
    void switchToMainland(ActionEvent event) {
        viewModel.switchServer(SourceType.DEFAULT);
        updateServerSelection(viewModel.currentServerProperty().get());
    }

    @FXML
    void switchToBilibili(ActionEvent event) {
        viewModel.switchServer(SourceType.BILIBILI);
        updateServerSelection(viewModel.currentServerProperty().get());
    }

    @FXML
    void switchToGlobal(ActionEvent event) {
        viewModel.switchServer(SourceType.GLOBAL);
        updateServerSelection(viewModel.currentServerProperty().get());
    }

    private void refreshInstallationControls() {
        GameEdition edition = viewModel.editingEditionProperty().get();
        if (installationSelector.getValue() != edition) installationSelector.setValue(edition);
        selectAppMode(viewModel.startAppCustomProperty().get());
        updateDxSelection();
    }

    private void updateServerSelection(SourceType source) {
        RadioButton selected = switch (source) {
            case BILIBILI -> bilibiliServerButton;
            case GLOBAL -> globalServerButton;
            default -> mainlandServerButton;
        };
        serverSwitchSourceGroup.selectToggle(selected);
        String serverName = selected.getText();
        currentServerLabel.setText(LanguageManager.getString("ui.game_manager.base.server_switch.current") + serverName);
    }

    private void updateDxSelection() {
        if (viewModel.isDx11()) gameDxToggleGroup.selectToggle(gameDxToggleGroup.getToggles().getFirst());
        else if (viewModel.isDx12()) gameDxToggleGroup.selectToggle(gameDxToggleGroup.getToggles().getLast());
        else gameDxToggleGroup.selectToggle(null);
    }

    private File chooseExecutable(String titleKey) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LanguageManager.getString(titleKey));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("可执行文件", "*.exe", "*.*"));
        return chooser.showOpenDialog(gameDirField.getScene().getWindow());
    }

    private static File currentDirectory(String path) {
        if (path == null || path.isBlank()) return null;
        File directory = new File(path);
        return directory.isDirectory() ? directory : null;
    }

    private static boolean isCustomMode(ActionEvent event) {
        return event.getSource() instanceof RadioButton button && "custom".equals(button.getUserData());
    }

    private void selectAppMode(boolean custom) {
        if (gameStartAppType.getToggles().size() >= 2) {
            gameStartAppType.selectToggle(gameStartAppType.getToggles().get(custom ? 1 : 0));
        }
    }

    private final class ParamListCell extends ListCell<String> {
        private final StackPane child = new StackPane();
        private final Label row = new Label();
        private final Button deleteButton = new Button(null, new FontIcon(Material2AL.DELETE_OUTLINE));

        private ParamListCell() {
            deleteButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, "delete-btn");
            deleteButton.setOnAction(event -> viewModel.deleteParam(getIndex()));
            child.getChildren().addAll(row, deleteButton);
            StackPane.setAlignment(row, Pos.CENTER_LEFT);
            StackPane.setAlignment(deleteButton, Pos.CENTER_RIGHT);
        }

        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            setGraphic(empty ? null : child);
            row.setText(empty ? null : value);
        }
    }
}
