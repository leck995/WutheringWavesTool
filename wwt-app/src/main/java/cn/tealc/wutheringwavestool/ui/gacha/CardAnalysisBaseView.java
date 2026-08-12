package cn.tealc.wutheringwavestool.ui.gacha;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import cn.tealc.fxplugin.FxPlugin;
import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.plugin.FxPluginManager;
import cn.tealc.wutheringwavestool.ui.component.BaseDialog;
import cn.tealc.wutheringwavestool.util.DialogBuilder;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class CardAnalysisBaseView implements FxmlView<CardAnalysisBaseViewModel>, Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(CardAnalysisBaseView.class);
    @InjectViewModel
    private CardAnalysisBaseViewModel viewModel;
    @FXML
    private StackPane content;
    @FXML
    private ComboBox<String> playerComboBox;
    @FXML
    private ToggleSwitch skipFirstSSRSwitch;

    private Parent commonChild;
    private Parent detailChild;
    private Parent tableChild;
    private Parent statChild;
    private CardStatViewModel statViewModel;
    private Parent cloudChild;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        skipFirstSSRSwitch.selectedProperty().bindBidirectional(viewModel.skipFirstSSRProperty());
        playerComboBox.setItems(viewModel.getPlayerList());

        playerComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 != null) {
                viewModel.updatePlayer(t1);
            }
        });

        viewModel.subscribe(CardAnalysisBaseViewModel.EVENT_SELECTED_PLAYER, (s, objects) -> {
            playerComboBox.getSelectionModel().select(viewModel.getPlayerList().indexOf(viewModel.getPlayer()));
        });

        createCommonChild();
        viewModel.subscribe("upload",(s, objects) -> {
            showUploadDialog();
        });


        viewModel.init();
    }

    private void showUploadDialog() {
        String title = String.format("游戏账号 %s 有最新的数据，是否进行云备份？", viewModel.getPlayer());
        String message = "由于服务器开销限制，每一个游戏玩家账号60分钟内只允许上传一次（多账号单独计算），请最好在无新数据后上传";

        JFXDialogLayout build = DialogBuilder
                .create()
                .title(title)
                .message(message)
                .button("上传", null,true,event -> {
                    viewModel.uploadGachaFile();
                })
                .cancel("取消").build();

        NotificationManager.dialog(build);

    }


    @FXML
    void load(ActionEvent event) {
        viewModel.loadFromNet();
    }

    @FXML
    void export(ActionEvent event) {
        FxPluginManager instance = FxPluginManager.getInstance();
        Optional<FxPlugin> plugin = instance.loadPlugins(1001);
        if (plugin.isPresent()) { //插件存在
            FxPlugin fxPlugin = plugin.get();
            Map<String, Object> params = new HashMap<>();
            fxPlugin.setOnFinished(o -> {
                viewModel.loadFile((String) o);
            });
            Optional<Object> result = fxPlugin.run(params);
            result.ifPresent(o -> {
                MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, o);
            });
        } else { //不存在
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.analysis.message.type07")));
        }
    }

    @FXML
    void delete(ActionEvent event) {
        if (!playerComboBox.getItems().isEmpty()) {
            JFXDialogLayout layout = new JFXDialogLayout();
            Label title = new Label(LanguageManager.getString("ui.common.warning"));
            title.getStyleClass().add(Styles.TITLE_2);
            layout.setHeading(title);
            Label tip = new Label(String.format(LanguageManager.getString("ui.analysis.delete.tip.content"), viewModel.getPlayer()));
            layout.setBody(tip);
            Button okBtn = new Button(LanguageManager.getString("ui.common.ok"));
            okBtn.getStyleClass().add(Styles.DANGER);
            Button cancelBtn = new Button(LanguageManager.getString("ui.common.cancel"));
            okBtn.setOnAction(event1 -> {
                viewModel.delete();
                cancelBtn.fireEvent(event1);
            });
            cancelBtn.setCancelButton(true);
            layout.setActions(okBtn, cancelBtn);
            MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, layout);
        }
    }

    @FXML
    void uploadGachaFile(ActionEvent event) {
        ViewTuple<CloudBackupView, CloudBackupViewModel> viewTuple = FluentViewLoader.fxmlView(CloudBackupView.class).load();
        NotificationManager.dialog((Pane) viewTuple.getView(),viewTuple.getCodeBehind());
    }

    @FXML
    void copyUrl(ActionEvent event) {
        viewModel.copyUrl();
    }



    @FXML
    void toCommonChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                createCommonChild();
                commonChild.setOpacity(0);
                Platform.runLater(() -> {
                    commonChild.setOpacity(1);
                    Animations.slideInUp(commonChild, Duration.millis(300)).play();
                });
                if (viewModel.getPoolData() != null) {
                    NotificationManager.publish(NotificationKey.CARD_POOL_USER_UPDATE, viewModel.getPoolData());
                }
            } else {
                toggleButton.setSelected(true);
            }
        }
    }
    @FXML
    void toCloudBackupChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                if (cloudChild == null) {
                    ViewTuple<CloudBackupView, CloudBackupViewModel> viewTuple = FluentViewLoader.fxmlView(CloudBackupView.class).load();
                    cloudChild = viewTuple.getView();
                }
                cloudChild.setOpacity(0);
                content.getChildren().setAll(cloudChild);
                Platform.runLater(() -> {
                    cloudChild.setOpacity(1);
                    Animations.slideInUp(cloudChild, Duration.millis(300)).play();
                });
            } else {
                toggleButton.setSelected(true);
            }
        }
    }

    @FXML
    void toTableChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                if (tableChild == null) {
                    ViewTuple<CardTableShowView, CardTableShowViewModel> viewTuple = FluentViewLoader.fxmlView(CardTableShowView.class).load();
                    viewTuple.getViewModel().loadData(viewModel.getPlayer());
                    tableChild = viewTuple.getView();
                }
                tableChild.setOpacity(0);
                content.getChildren().setAll(tableChild);
                Platform.runLater(() -> {
                    tableChild.setOpacity(1);
                    Animations.slideInUp(tableChild, Duration.millis(300)).play();
                });
            } else {
                toggleButton.setSelected(true);
            }
        }
    }


    @FXML
    void toStatChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton) {
            if (toggleButton.isSelected()) {
                if (statChild == null) {
                    statViewModel = new CardStatViewModel(viewModel.getPoolData() != null);
                    statViewModel.setStatService(AppInjector.getInstance(cn.tealc.wutheringwavestool.service.GachaStatService.class));
                    ViewTuple<CardStatView, CardStatViewModel> viewTuple =
                            FluentViewLoader
                                    .fxmlView(CardStatView.class)
                                    .viewModel(statViewModel)
                                    .load();
                    statChild = viewTuple.getView();
                }
                statChild.setOpacity(0);
                content.getChildren().setAll(statChild);
                // 统计界面直接读 pool.json，不依赖 AnalysisData
                if (viewModel.getPlayer() != null) {
                    statViewModel.loadData(viewModel.getPlayer());
                }
                Platform.runLater(() -> {
                    statChild.setOpacity(1);
                    Animations.slideInUp(statChild, Duration.millis(300)).play();
                });
            } else {
                toggleButton.setSelected(true);
            }
        }
    }


    private void createCommonChild(){
        if (commonChild == null) {
            Thread.startVirtualThread(()->{
                ViewTuple<CardCommonAnalysisView, CardCommonAnalysisViewModel> viewTuple =
                        FluentViewLoader
                                .fxmlView(CardCommonAnalysisView.class)
                                .viewModel(new CardCommonAnalysisViewModel(viewModel.getPoolData() != null))
                                .load();
                commonChild = viewTuple.getView();
                Platform.runLater(()->{
                    content.getChildren().setAll(commonChild);
                });
            });
        }else {
            content.getChildren().setAll(commonChild);
        }
    }

    @FXML
    void toDetailChild(ActionEvent event) {
        if (event.getSource() instanceof ToggleButton toggleButton){
            if (toggleButton.isSelected()) {
                if (detailChild == null) {
                    ViewTuple<CardDetailAnalysisView, CardDetailAnalysisViewModel> viewTuple =
                            FluentViewLoader
                                    .fxmlView(CardDetailAnalysisView.class)
                                    .viewModel(new CardDetailAnalysisViewModel(viewModel.getPoolData() != null))
                                    .load();
                    detailChild = viewTuple.getView();
                }

                detailChild.setOpacity(0);
                content.getChildren().setAll(detailChild);

                if (viewModel.getPoolData() != null) {
                    NotificationManager.publish(NotificationKey.CARD_POOL_USER_UPDATE, viewModel.getPoolData());
                }else{
                    NotificationManager.publish(NotificationKey.CARD_POOL_USER_EMPTY);
                }
                Platform.runLater(() -> {
                    detailChild.setOpacity(1);
                    Animations.slideInUp(detailChild, Duration.millis(300)).play();
                });
            } else {
                toggleButton.setSelected(true);
            }
        }
    }

    }