package cn.tealc.wutheringwavestool.ui;

import atlantafx.base.controls.Message;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.model.message.MessageInfo;

import cn.tealc.wutheringwavestool.model.message.MessageType;
import cn.tealc.wutheringwavestool.thread.MainBackgroundTask;
import cn.tealc.wutheringwavestool.ui.kujiequ.*;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import com.jfoenixN.controls.JFXDialog;
import com.jfoenixN.controls.JFXDialogLayout;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.MvvmFX;
import de.saxsys.mvvmfx.ViewTuple;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 18:59
 */
public class MainView implements Initializable,FxmlView<MainViewModel> {
    private static final Logger LOG= LoggerFactory.getLogger(MainView.class);
    @FXML
    private StackPane child;
    @FXML
    private Button minBtn;
    @FXML
    private Button maxBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private ToggleGroup navToggleGroup;

    @FXML
    private StackPane root;

    @FXML
    private VBox messagePane;
    @FXML
    private ToggleButton analysisBtn;
    @FXML
    private ToggleButton homeBtn;


    @FXML
    private ToggleButton supportBtn;

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
    private Region navBg;
    @FXML
    private ImageView icon;

    @FXML
    private HBox titlebar;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

 /*       FontIcon fontIcon;
        if (Config.setting.isTheme()){
            fontIcon = new FontIcon(Material2AL.BEDTIME);
        }else {
            fontIcon = new FontIcon(Material2MZ.WB_SUNNY);
        }


        ToggleButton skinBtn=new ToggleButton(null,fontIcon);
        skinBtn.selectedProperty().bindBidirectional(Config.setting.themeProperty());
        skinBtn.setOnAction(event -> {
            if (skinBtn.isSelected()) {
                fontIcon.setIconCode(Material2AL.BEDTIME);
                Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            }else {
                fontIcon.setIconCode(Material2MZ.WB_SUNNY);
                Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            }
        });*/

        Circle circle=new Circle(18,18,18);
        icon.setClip(circle);
        icon.setImage(new Image(FXResourcesLoader.load("image/icon.png"),45,45,true,true));


        //禁用库街区，系统语言为英文也会默认禁用库街区
        if (Config.setting.isNoKuJieQu()){
            Iterator<Node> iterator = nav.getChildren().iterator();
            while (iterator.hasNext()) {
                Node next = iterator.next();
                if (next instanceof ToggleButton button) {
                    if (button.getAccessibleText() != null && button.getAccessibleText().equals("kujiequ")){
                        iterator.remove();
                    }
                }
            }
        }


        navBtn.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (!t1){
                for (Toggle toggle : navToggleGroup.getToggles()) {
                    ToggleButton toggleButton = (ToggleButton) toggle;
                    toggleButton.getStyleClass().remove("icon-only");
                }
                supportBtn.getStyleClass().remove("icon-only");
            }else {
                for (Toggle toggle : navToggleGroup.getToggles()) {
                    ToggleButton toggleButton = (ToggleButton) toggle;
                    toggleButton.getStyleClass().add("icon-only");
                }
                supportBtn.getStyleClass().add("icon-only");
            }
        });

        navBtn.selectedProperty().bindBidirectional(Config.setting.leftBarShowProperty());

        supportBtn.visibleProperty().bind(Config.setting.supportProperty().not());





        if (Config.setting.isFirstViewWithPoolAnalysis()){
            ViewTuple<AnalysisPoolView, AnalysisPoolViewModel> viewTuple = FluentViewLoader.fxmlView(AnalysisPoolView.class).load();
            child.getChildren().setAll(viewTuple.getView());
            navToggleGroup.selectToggle(analysisBtn);
        }else {
            ViewTuple<HomeView, HomeViewModel> viewTuple = FluentViewLoader.fxmlView(HomeView.class).load();
            child.getChildren().setAll(viewTuple.getView());

        }

        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(bgPane.widthProperty());
        rectangle.heightProperty().bind(bgPane.heightProperty());
        rectangle.setArcWidth(10);
        rectangle.setArcHeight(10);
        bgPane.setClip(rectangle);
        bgPane02.visibleProperty().bind(bgPane.visibleProperty().not());
        bgPane03.visibleProperty().bind(bgPane.visibleProperty().not());

        updateBg();
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.MESSAGE,((s, objects) -> {
            showMessage((MessageInfo) objects[0]);
        }));
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.DIALOG,((s, objects) -> {
            if (objects[0] instanceof JFXDialogLayout node){
                showDialog(node);
            }else {
                Pane panes= (Pane) objects[0];
                showDialog(panes);
            }
        }));
        MvvmFX.getNotificationCenter().subscribe(NotificationKey.CHANGE_BG,((s, objects) -> {
            updateBg();
        }));
    }


    private void updateBg(){
        Image image=null;
        if(Config.setting.isDiyHomeBg()){
            image= LocalResourcesManager.getHomeBg(Config.setting.getDiyHomeBgName());
            if (image == null){
                image = new Image(FXResourcesLoader.load("image/bg.png"));
                Config.setting.setDiyHomeBg(false);
                Config.setting.setDiyHomeBgName(null);
                LOG.warn("自定义壁纸出现问题，取消自定义");
            }
        }else {
            image = new Image(FXResourcesLoader.load("image/bg.png"));
        }
        bgPane.setBackground(
                new Background(
                        new BackgroundImage(
                                image,
                                BackgroundRepeat.NO_REPEAT,
                                BackgroundRepeat.NO_REPEAT,
                                BackgroundPosition.CENTER,
                                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, true))));

        //bgPane02用于显示高斯模糊的背景
        MainBackgroundTask task = new MainBackgroundTask(image);
        task.setOnSucceeded(workerStateEvent -> {
            bgPane02.setBackground(task.getValue());
        });

        Thread.startVirtualThread(task);
    }

    private void showDialog(JFXDialogLayout container){
        JFXDialog dialog = new JFXDialog(root,container,JFXDialog.DialogTransition.CENTER);
        for (Node action : container.getActions()) {
            if (action instanceof Button button) {
                if (button.isCancelButton()){
                    button.setOnAction(event -> {
                        dialog.close();
                    });
                }
            }
        }
        dialog.show();
    }

    private void showDialog(Pane pane){
        JFXDialog dialog = new JFXDialog(root,pane,JFXDialog.DialogTransition.CENTER);
        dialog.show();
    }

    private void showMessage(MessageInfo info){
        if (messagePane.getChildren().size() > 7){
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
            Timeline timeline=new Timeline(new KeyFrame(Duration.millis(250),new KeyValue(message.translateXProperty(),0)));
            timeline.play();
        });
        if (info.getAutoClose()){
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


    public void close(){
        switch (Config.setting.getCloseEvent()) {
            case 0 -> showExitDialog();
            case 1 -> MainApplication.exit();
            case 2 -> MainApplication.window.hide();
        }
    }

    private void showExitDialog(){
        JFXDialogLayout dialogLayout = new JFXDialogLayout();
        Label title = new Label("关闭提示");
        title.getStyleClass().add("title-2");
        dialogLayout.setHeading(title);
        Label tip=new Label("确认退出吗？");
        dialogLayout.setBody(tip);

        Button exitBtn=new Button("退出");
        Button iconBtn=new Button("隐藏至托盘");
        Button cancelBtn=new Button("取消");

        dialogLayout.setActions(iconBtn,exitBtn,cancelBtn);
        JFXDialog jfxDialog = new JFXDialog(root,dialogLayout,JFXDialog.DialogTransition.CENTER);

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
    void toAnalysis(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<AnalysisPoolView, AnalysisPoolViewModel> viewTuple = FluentViewLoader.fxmlView(AnalysisPoolView.class).load();
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
            bgPane.setVisible(false);

        }else {
            toggleButton.setSelected(true);
        }
    }
    @FXML
    void toOwnRole(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<OwnRoleView, OwnRoleViewModel> viewTuple = FluentViewLoader.fxmlView(OwnRoleView.class).load();

            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
            bgPane.setVisible(false);
        }else {
            toggleButton.setSelected(true);
        }
    }
    @FXML
    void toSign(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<SignView, SignViewModel> viewTuple = FluentViewLoader.fxmlView(SignView.class).load();
            bgPane.setVisible(false);
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
        }else {
            toggleButton.setSelected(true);
        }

    }
    @FXML
    void toMain(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<HomeView, HomeViewModel> viewTuple = FluentViewLoader.fxmlView(HomeView.class).load();
            bgPane.setVisible(true);
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toSetting(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<SettingView,SettingViewModel> viewTuple = FluentViewLoader.fxmlView(SettingView.class).load();
            bgPane.setVisible(false);
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
        }else {
            toggleButton.setSelected(true);
        }
    }
    @FXML
    void toAccount(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<AccountView, AccountViewModel> viewTuple = FluentViewLoader.fxmlView(AccountView.class).load();
            bgPane.setVisible(false);
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toGameTime(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<GameTimeView, GameTimeViewModel> viewTuple = FluentViewLoader.fxmlView(GameTimeView.class).load();
            bgPane.setVisible(false);
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
        }else {
            toggleButton.setSelected(true);
        }
    }


    @FXML
    void toTower(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<TowerView, TowerViewModel> viewTuple = FluentViewLoader.fxmlView(TowerView.class).load();
            bgPane.setVisible(false);
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toStartAppSetting(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        if (toggleButton.isSelected()){
            ViewTuple<GameAppSettingView, GameAppSettingViewModel> viewTuple = FluentViewLoader.fxmlView(GameAppSettingView.class).load();
            bgPane.setVisible(false);
            child.getChildren().setAll(viewTuple.getView());
            startNavAnim();
        }else {
            toggleButton.setSelected(true);
        }
    }

    @FXML
    void toSupport(ActionEvent event) {
        ToggleButton toggleButton= (ToggleButton) event.getSource();
        toggleButton.setSelected(false);
        Label title = new Label(LanguageManager.getString("ui.setting.sponsor.dialog.title"));
        title.getStyleClass().add(Styles.TITLE_3);
        Label tip1 =new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip01"));
        tip1.setWrapText(true);
        tip1.setPrefWidth(450);
        tip1.setMinHeight(80);
        Image image =new Image(FXResourcesLoader.load("image/support.png"),350,320,true,true,true);
        ImageView iv = new ImageView(image);

        StackPane imagePane =new StackPane(iv);
        Label tip2 =new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip02"));
        Label tip3 =new Label(LanguageManager.getString("ui.setting.sponsor.dialog.tip03"));
        VBox center = new VBox(5.0,tip1,imagePane,tip2,tip3);

        Hyperlink browserBtn = new Hyperlink(LanguageManager.getString("ui.setting.sponsor.dialog.browser"));
        browserBtn.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().browse(new URI(Config.URL_SUPPORT_LIST));
            } catch (IOException | URISyntaxException e) {
                LOG.error("打开赞助名单失败{}",e.getMessage());
            }
        });
        Button okBtn = new Button(LanguageManager.getString("ui.setting.sponsor.dialog.ok"));
        Button cancelBtn = new Button(LanguageManager.getString("ui.common.cancel"));


        okBtn.setOnAction(actionEvent -> {
            MvvmFX.getNotificationCenter().publish(NotificationKey.MESSAGE,new MessageInfo(MessageType.SUCCESS,"感谢您的支持，谢谢",Duration.seconds(5)));
            Config.setting.setSupport(true);
            cancelBtn.fireEvent(actionEvent);
        });

        cancelBtn.setCancelButton(true);
        JFXDialogLayout dialogLayout=new JFXDialogLayout();
        dialogLayout.setHeading(title);
        dialogLayout.setBody(center);
        dialogLayout.setActions(browserBtn,okBtn,cancelBtn);
        dialogLayout.setPrefSize(500,500);
        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,dialogLayout);
    }


    private void startChangeAnim(){
        Animations.slideInLeft(child,Duration.millis(300)).play();
        var t = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(bgPane.scaleXProperty(), 1.2),
                        new KeyValue(bgPane.scaleYProperty(), 1.2),
                        new KeyValue(bgPane.scaleZProperty(), 1.2)
                ));
        t.play();
    }
    private void startBackAnim(){
        Animations.slideInLeft(child,Duration.millis(300)).play();
        var t = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(bgPane.scaleXProperty(), 1.2),
                        new KeyValue(bgPane.scaleYProperty(), 1.2),
                        new KeyValue(bgPane.scaleZProperty(), 1.2)
                ));
        t.play();
    }

    private Message createMessage(MessageInfo messageInfo){
        Message message = null;
        switch (messageInfo.getType()){
            case SUCCESS -> {
                message = new Message(
                        "Success",
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedAL.CHECK_CIRCLE_OUTLINE)
                );
                message.getStyleClass().add(Styles.SUCCESS);
            }
            case WARNING -> {
                message = new Message(
                        "Warning",
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedMZ.OUTLINED_FLAG)
                );
                message.getStyleClass().add(Styles.WARNING);
            }
            case INFO -> {
                message = new Message(
                        "Info",
                        messageInfo.getMessage(),

                        new FontIcon(Material2OutlinedMZ.TURNED_IN_NOT)
                );
                message.getStyleClass().add(Styles.ACCENT);
            }
            case ERROR -> {
                message = new Message(
                        "ERROR",
                        messageInfo.getMessage(),
                        new FontIcon(Material2OutlinedAL.ERROR_OUTLINE)
                );
                message.getStyleClass().add(Styles.DANGER);
            }
        }

        message.setPrefSize(300.0,60.0);
        message.setMaxSize(300.0,80.0);
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