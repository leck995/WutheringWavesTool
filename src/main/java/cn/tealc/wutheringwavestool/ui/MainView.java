package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.Message;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import cn.tealc.teafx.utils.AnchorPaneUtil;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;

import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.model.release.Release;
import cn.tealc.wutheringwavestool.model.system.NavData;
import cn.tealc.wutheringwavestool.thread.system.ui.MainBackgroundTask;
import cn.tealc.wutheringwavestool.ui.cardpool.CardAnalysisBaseView;
import cn.tealc.wutheringwavestool.ui.cardpool.CardAnalysisBaseViewModel;
import cn.tealc.wutheringwavestool.ui.component.BaseDialog;
import cn.tealc.wutheringwavestool.ui.game.*;
import cn.tealc.wutheringwavestool.ui.game.manage.GameManagerView;
import cn.tealc.wutheringwavestool.ui.game.manage.GameManagerViewModel;
import cn.tealc.wutheringwavestool.ui.kujiequ.*;
import cn.tealc.wutheringwavestool.ui.base.UpdateView;
import cn.tealc.wutheringwavestool.ui.base.UpdateViewModel;
import cn.tealc.wutheringwavestool.ui.kujiequ.calculator.CalculatorView;
import cn.tealc.wutheringwavestool.ui.kujiequ.calculator.CalculatorViewModel;
import cn.tealc.wutheringwavestool.ui.kujiequ.tower.TowerGroupView;
import cn.tealc.wutheringwavestool.ui.kujiequ.tower.TowerGroupViewModel;
import cn.tealc.wutheringwavestool.ui.kujiequ.tower.TowerView;
import cn.tealc.wutheringwavestool.ui.kujiequ.tower.TowerViewModel;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import cn.tealc.wutheringwavestool.util.NavLoader;
import com.jfoenixN.controls.JFXDialog;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 18:59
 */
public class MainView implements Initializable, FxmlView<MainViewModel> {
    private static final Logger LOG = LoggerFactory.getLogger(MainView.class);
    @InjectViewModel
    private MainViewModel viewModel;
    @FXML
    private AnchorPane content;
    @FXML
    private StackPane child;
    @FXML
    private Button minBtn;
    @FXML
    private Button maxBtn;
    @FXML
    private Button closeBtn;


    @FXML
    private StackPane root;

    @FXML
    private VBox messagePane;

    private GaussianBlur bgGaussianBlur;
    @FXML
    private Pane bgPane;
    @FXML
    private Pane bgPane02;
    @FXML
    private Pane bgPane03;
    @FXML
    private ToggleButton navBtn;
    @FXML
    private VBox nav;
    @FXML
    private VBox navTop, navBottom;
    @FXML
    private Region navBg;
    @FXML
    private ImageView icon;

    @FXML
    private HBox titlebar;
    private ToggleGroup navToggleGroup;
    private ToggleButton supportBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Circle circle = new Circle(18, 18, 18);
        icon.setClip(circle);
        icon.setImage(new Image(FXResourcesLoader.load("image/icon.png"), 45, 45, true, true));

        //禁用库街区，系统语言为英文也会默认禁用库街区
        if (Config.setting.isNoKuJieQu()) {
            Iterator<Node> iterator = nav.getChildren().iterator();
            while (iterator.hasNext()) {
                Node next = iterator.next();
                if (next instanceof ToggleButton button) {
                    if (button.getAccessibleText() != null && button.getAccessibleText().equals("kujiequ")) {
                        iterator.remove();
                    }
                }
            }
        }

        initBackground();
        initNav();
        initContent();
        initGlobalEvent();
    }

    private void initGlobalEvent() {
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.NOTIFICATION_SHOW_UPDATE, ((s, objects) -> {
            showUpdateView((Release) objects[0]);
        }));
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.MESSAGE, ((s, objects) -> {
            showMessage((MessageInfo) objects[0]);
        }));
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.DIALOG, ((s, objects) -> {
            if (objects[0] instanceof JFXDialogLayout node) {
                showDialog(node);
            } else {
                Pane panes = (Pane) objects[0];
                if (objects[1] != null && objects[1] instanceof BaseDialog dialog) {
                    showDialog(panes, dialog);
                } else {
                    showDialog(panes);
                }
            }
        }));
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.CHANGE_BG, ((s, objects) -> {
            updateBg();
        }));

        MvvmFX.getNotificationCenter().subscribe(NotificationKey.CHANGE_NAV, ((s, objects) -> {
            navTop.getChildren().clear();
            navBottom.getChildren().clear();
            initNav();
        }));
    }

    private void initBackground() {
        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(bgPane.widthProperty());
        rectangle.heightProperty().bind(bgPane.heightProperty());
        rectangle.setArcWidth(10);
        rectangle.setArcHeight(10);
        bgPane.setClip(rectangle);
        bgPane02.visibleProperty().bind(bgPane.visibleProperty().not());
        bgPane03.visibleProperty().bind(bgPane.visibleProperty().not());

        // 创建亚克力效果层
        Rectangle acrylicLayer = new Rectangle();
        acrylicLayer.widthProperty().bind(bgPane02.widthProperty());
        acrylicLayer.heightProperty().bind(bgPane02.heightProperty());
        acrylicLayer.setFill(Color.rgb(247, 249, 253, 0.5));// 半透明白色基底
        // 添加噪点纹理（可选）
        Rectangle noiseTexture = new Rectangle();
        noiseTexture.setFill(Color.rgb(0, 0, 0, 0.03)); // 黑色噪点
        noiseTexture.widthProperty().bind(bgPane02.widthProperty());
        noiseTexture.heightProperty().bind(bgPane02.heightProperty());
        bgPane02.getChildren().addAll(acrylicLayer, noiseTexture);

        updateBg();
    }

    private void initNav() {
        supportBtn = new ToggleButton(LanguageManager.getString("ui.main.button.nav.type09"),new FontIcon(Material2AL.LOCAL_CAFE));
        supportBtn.getStyleClass().add("icon-only");
        supportBtn.setOnAction(this::toSupport);
        navBottom.getChildren().addFirst(supportBtn);

        navToggleGroup = new ToggleGroup();
        List<NavData> navList = viewModel.getNavList();

        for (NavData navData : navList) {
            if (Config.setting.isNoKuJieQu() && navData.isKujiequ()){
                continue;
            }
            FontIcon fontIcon = new FontIcon(navData.getIcon());
            String title = LanguageManager.getString(navData.getTitle());
            ToggleButton toggleButton = new ToggleButton(title, fontIcon);
            toggleButton.setOnAction(actionEvent -> {
                ToggleButton source = (ToggleButton) actionEvent.getSource();
                if (source.isSelected()) {
                    ViewTuple<?, ?> load = NavLoader.load(navData);
                    bgPane.setVisible(navData.isShowBg());
                    child.getChildren().setAll(load.getView());
                    startNavAnim();
                } else {
                    source.setSelected(true);
                }
            });
            toggleButton.setToggleGroup(navToggleGroup);
            toggleButton.getStyleClass().add("icon-only");
            if (navData.isBottom()) {
                navBottom.getChildren().add(toggleButton);
            } else {
                navTop.getChildren().add(toggleButton);
            }
        }

        navBtn.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (!t1) {
                for (Toggle toggle : navToggleGroup.getToggles()) {
                    if (toggle == navBtn) {
                        return;
                    }
                    ToggleButton toggleButton = (ToggleButton) toggle;
                    toggleButton.getStyleClass().remove("icon-only");
                }
                supportBtn.getStyleClass().remove("icon-only");
            } else {
                for (Toggle toggle : navToggleGroup.getToggles()) {
                    if (toggle == navBtn) {
                        return;
                    }
                    ToggleButton toggleButton = (ToggleButton) toggle;
                    toggleButton.getStyleClass().add("icon-only");
                }
                supportBtn.getStyleClass().add("icon-only");
            }
        });
        navBtn.selectedProperty().bindBidirectional(Config.setting.leftBarShowProperty());
        supportBtn.visibleProperty().bind(Config.setting.supportProperty().not());
    }

    private void initContent(){
        if (Config.setting.isFirstViewWithPoolAnalysis()) {
            ViewTuple<CardAnalysisBaseView, CardAnalysisBaseViewModel> viewTuple = FluentViewLoader.fxmlView(CardAnalysisBaseView.class).load();
            child.getChildren().setAll(viewTuple.getView());
            //navToggleGroup.selectToggle(analysisBtn);
            bgPane.setVisible(false);
        } else {
            ViewTuple<HomeView, HomeViewModel> viewTuple = FluentViewLoader.fxmlView(HomeView.class).load();
            child.getChildren().setAll(viewTuple.getView());
        }
    }

    private void updateBg() {
        Image image = null;
        if (Config.setting.getDiyHomeBgType() == 0) {
            image = new Image(FXResourcesLoader.load("image/bg.png"));
        } else if (Config.setting.getDiyHomeBgType() == 1) {
            image = LocalResourcesManager.getHomeBg(Config.setting.getDiyHomeBgName());
            if (image == null) {
                image = new Image(FXResourcesLoader.load("image/bg.png"));
                Config.setting.setDiyHomeBg(false);
                Config.setting.setDiyHomeBgName(null);
                LOG.warn("自定义壁纸出现问题，取消自定义");
            }
        } else if (Config.setting.getDiyHomeBgType() == 2) {
            image = getImageFormBgDir();
            if (image == null) {
                image = new Image(FXResourcesLoader.load("image/bg.png"));
            }
        }
        if (image != null) {
            bgPane.setBackground(
                    new Background(
                            new BackgroundImage(
                                    image,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.CENTER,
                                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, true))));
            // bgPane02.setBackground(bgPane.getBackground());
            //bgPane02用于显示高斯模糊的背景
            MainBackgroundTask task = new MainBackgroundTask(image);
            task.setOnSucceeded(workerStateEvent -> {
                bgPane02.setBackground(task.getValue());

            });
            Thread.startVirtualThread(task);
        }
    }

    private Image getImageFormBgDir() {
        File bgDir = new File(Config.setting.getDiyHomeBgDir());
        if (bgDir.exists()) {
            File[] bgs = bgDir.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif"));
            if (bgs != null && bgs.length > 0) {
                Random random = new Random();
                int i = random.nextInt(bgs.length);
                return new Image(bgs[i].toURI().toString(), 2560, 1440, true, true, false);
            }
        }

        return null;
    }


    private void showDialog(JFXDialogLayout container) {
        JFXDialog dialog = new JFXDialog(root, container, JFXDialog.DialogTransition.CENTER);
        for (Node action : container.getActions()) {
            if (action instanceof Button button) {
                if (button.isCancelButton()) {
                    button.setOnAction(event -> {
                        dialog.close();
                    });
                }
            }
        }
        dialog.show();
    }

    private void showUpdateView(Release release) {
        ViewTuple<UpdateView, UpdateViewModel> viewTuple = FluentViewLoader.fxmlView(UpdateView.class).viewModel(new UpdateViewModel(release)).load();
        StackPane view = (StackPane) viewTuple.getView();
        view.setBackground(bgPane02.getBackground());

        //必须放在通知界面的后面
        content.getChildren().add(content.getChildren().size() - 1, view);
        AnchorPaneUtil.setPosition(view, 0, 0, 0, 0);
    }


    private void showDialog(Pane pane) {
        JFXDialog dialog = new JFXDialog(root, pane, JFXDialog.DialogTransition.CENTER);
        dialog.show();
    }

    private void showDialog(Pane pane, BaseDialog baseDialog) {
        JFXDialog dialog = new JFXDialog(root, pane, JFXDialog.DialogTransition.CENTER);
        baseDialog.setDialog(dialog);
        dialog.show();
    }

    private void showMessage(MessageInfo info) {
        if (messagePane.getChildren().size() > 7) {
            messagePane.getChildren().removeFirst();
        }
        Message message = createMessage(info);
        message.setOnClose(e -> {
            var out = Animations.slideOutRight(message, Duration.millis(250));
            out.setOnFinished(f -> messagePane.getChildren().remove(message));
            out.playFromStart();
        });
        Platform.runLater(() -> {
            messagePane.getChildren().add(message);
            message.setTranslateX(300);
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(250), new KeyValue(message.translateXProperty(), 0)));
            timeline.play();
        });
        if (info.getAutoClose()) {
            Timeline fiveSecondsWonder = new Timeline(new KeyFrame(info.getShowTime(), new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    var out = Animations.slideOutRight(message, Duration.millis(250));
                    out.setOnFinished(f -> messagePane.getChildren().remove(message));
                    out.playFromStart();
                }
            }));
            fiveSecondsWonder.play();
        }
    }


    public void close() {
        switch (Config.setting.getCloseEvent()) {
            case 0 -> showExitDialog();
            case 1 -> MainApplication.exit();
            case 2 -> MainApplication.window.hide();
        }
    }

    private void showExitDialog() {
        JFXDialogLayout dialogLayout = new JFXDialogLayout();
        Label title = new Label(LanguageManager.getString("ui.main.exit.header"));
        title.getStyleClass().add("title-2");
        dialogLayout.setHeading(title);
        Label tip = new Label(LanguageManager.getString("ui.main.exit.body"));
        dialogLayout.setBody(tip);

        Button exitBtn = new Button(LanguageManager.getString("ui.main.exit.btn01"));
        Button iconBtn = new Button(LanguageManager.getString("ui.main.exit.btn02"));
        Button cancelBtn = new Button(LanguageManager.getString("ui.main.exit.btn03"));

        dialogLayout.setActions(iconBtn, exitBtn, cancelBtn);
        JFXDialog jfxDialog = new JFXDialog(root, dialogLayout, JFXDialog.DialogTransition.CENTER);

        exitBtn.setOnAction(event -> {
            MainApplication.exit();
        });
        iconBtn.setOnAction(event -> {
            MainApplication.window.hide();
            jfxDialog.close();
        });

        cancelBtn.setOnAction(event -> jfxDialog.close());
        jfxDialog.show();
    }


    @FXML
    void toSupport(ActionEvent event) {
        ToggleButton toggleButton = (ToggleButton) event.getSource();
        toggleButton.setSelected(false);
        Label title = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.title"));
        title.getStyleClass().add(Styles.TITLE_3);
        Label tip1 = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip01"));
        tip1.setWrapText(true);
        tip1.setPrefWidth(450);
        tip1.setMinHeight(80);
        Image image = new Image(FXResourcesLoader.load("image/support.png"), 350, 320, true, true, true);
        ImageView iv = new ImageView(image);

        StackPane imagePane = new StackPane(iv);
        Label tip2 = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip02"));
        Label tip3 = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip03"));
        VBox center = new VBox(5.0, tip1, imagePane, tip2, tip3);

        Hyperlink browserBtn = new Hyperlink(LanguageManager.getString("ui.setting.sponsor.dialog.browser"));
        browserBtn.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI(Config.URL_SUPPORT_LIST));
            } catch (IOException | URISyntaxException e) {
                LOG.error("打开赞助名单失败{}", e.getMessage());
            }
        });
        Button okBtn = new Button(LanguageManager.getString("ui.setting.sponsor.dialog.ok"));
        Button cancelBtn = new Button(LanguageManager.getString("ui.common.cancel"));


        okBtn.setOnAction(actionEvent -> {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE, new MessageInfo(MessageType.SUCCESS, "感谢您的支持，谢谢", Duration.seconds(5)));
            Config.setting.setSupport(true);
            cancelBtn.fireEvent(actionEvent);
        });

        cancelBtn.setCancelButton(true);
        JFXDialogLayout dialogLayout = new JFXDialogLayout();
        dialogLayout.setHeading(title);
        dialogLayout.setBody(center);
        dialogLayout.setActions(browserBtn, okBtn, cancelBtn);
        dialogLayout.setPrefSize(500, 500);
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG, dialogLayout);
    }


    private void startChangeAnim() {
        Animations.slideInLeft(child, Duration.millis(300)).play();
        var t = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(bgPane.scaleXProperty(), 1.2),
                        new KeyValue(bgPane.scaleYProperty(), 1.2),
                        new KeyValue(bgPane.scaleZProperty(), 1.2)
                ));
        t.play();
    }

    private void startBackAnim() {
        Animations.slideInLeft(child, Duration.millis(300)).play();
        var t = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(bgPane.scaleXProperty(), 1.2),
                        new KeyValue(bgPane.scaleYProperty(), 1.2),
                        new KeyValue(bgPane.scaleZProperty(), 1.2)
                ));
        t.play();
    }

    private Message createMessage(MessageInfo messageInfo) {
        Message message = null;
        switch (messageInfo.getType()) {
            case SUCCESS -> {
                message = new Message(
                        null,
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedAL.CHECK_CIRCLE_OUTLINE)
                );
                message.getStyleClass().add(Styles.SUCCESS);
            }
            case WARNING -> {
                message = new Message(
                        null,
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedMZ.OUTLINED_FLAG)
                );
                message.getStyleClass().add(Styles.WARNING);
            }
            case INFO -> {
                message = new Message(
                        null,
                        messageInfo.getMessage(),

                        new FontIcon(Material2OutlinedMZ.TURNED_IN_NOT)
                );
                message.getStyleClass().add(Styles.ACCENT);
            }
            case ERROR -> {
                message = new Message(
                        null,
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedAL.ERROR_OUTLINE)
                );
                message.getStyleClass().add(Styles.DANGER);
            }
        }

        message.setPrefSize(300.0, 60.0);
        message.setMaxSize(300.0, 80.0);
        return message;
    }

    public void startNavAnim() {

        var t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(child.scaleXProperty(), 0.9, Animations.EASE),
                        new KeyValue(child.scaleYProperty(), 0.9, Animations.EASE)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(child.scaleXProperty(), 1, Animations.EASE),
                        new KeyValue(child.scaleYProperty(), 1, Animations.EASE)
                )
        );

        t.statusProperty().addListener((obs, old, val) -> {
            if (val == Animation.Status.STOPPED) {
                child.setScaleX(1);
                child.setScaleY(1);
            }
        });

        t.play();
    }


    public Button getMinBtn() {
        return minBtn;
    }

    public Button getMaxBtn() {
        return maxBtn;
    }

    public Button getCloseBtn() {
        return closeBtn;
    }

    public HBox getTitlebar() {
        return titlebar;
    }
}