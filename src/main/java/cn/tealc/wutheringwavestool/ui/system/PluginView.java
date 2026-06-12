package cn.tealc.wutheringwavestool.ui.system;

import cn.tealc.fxplugin.FxPlugin;
import cn.tealc.wutheringwavestool.FXResourcesLoader;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.teafx.utils.message.MessageInfo;
import cn.tealc.wutheringwavestool.plugin.FxPluginConfig;
import cn.tealc.wutheringwavestool.plugin.FxPluginManager;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2025-01-01 01:31
 */
public class PluginView implements FxmlView<PluginViewModel>, Initializable {
    @InjectViewModel
    private PluginViewModel viewModel;
    @FXML
    private StackPane root;

    @FXML
    private FlowPane localPluginPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        ObservableList<FxPluginConfig> plugins = viewModel.getPlugins();
        initCell(plugins);
        plugins.addListener((ListChangeListener<? super FxPluginConfig>) change -> {
            localPluginPane.getChildren().clear();
            initCell(change.getList());
        });
    }

    private void initCell(List<? extends FxPluginConfig> plugins) {
        plugins.forEach(plugin -> {
            localPluginPane.getChildren().add(new PluginCell(plugin));
        });
    }



    private void runPlugin(FxPluginConfig config) {
        FxPluginManager instance = FxPluginManager.getInstance();
        Optional<FxPlugin> plugin = instance.loadPlugins(config.getId());
        if (plugin.isPresent()){ //插件存在
            FxPlugin fxPlugin = plugin.get();
            Map<String,Object> params = new HashMap<>();
            switch (config.getPluginType()){
                case DEFAULT -> {
                    Optional<Object> result = fxPlugin.run(params);
                }
                case VIEW -> {
                    Optional<Object> result = fxPlugin.run(params);
                    result.ifPresent(o -> {
                        root.getChildren().add(localPluginPane);
                    });
                }
                case STAGE -> {
                    Optional<Object> result = fxPlugin.run(params);
                    result.ifPresent(o -> {
                        Stage stage = (Stage) o;
                        stage.show();
                    });
                }
                case DIALOG -> {
                    Optional<Object> result = fxPlugin.run(params);
                    result.ifPresent(o -> {
                        MvvmFX.getNotificationCenter().publish(NotificationKey.DIALOG,o);
                    });
                }
            }

        }else { //不存在
            NotificationManager.message(MessageInfo.warning(LanguageManager.getString("ui.analysis.message.type07")));
        }
    }

    class PluginCell extends VBox{
        private ImageView icon;
        private Label title;
        private FxPluginConfig config;
        public PluginCell(FxPluginConfig config) {
            this.config = config;
            setAlignment(Pos.BOTTOM_CENTER);
            getStyleClass().add("plugin-cell");
            File file = config.getIconFile();
            if (file.exists()){
                icon = new ImageView(new Image(file.toURI().toString(),45,45,true,true,true));
            }else {
                icon = new ImageView(new Image(FXResourcesLoader.load("image/icon.png"),45,45,true,true,true));
            }
            title = new Label(config.getTitle());
            setSpacing(5.0);
            getChildren().addAll(icon,title);

            ContextMenu contextMenu = new ContextMenu();
            MenuItem runItem=new MenuItem(LanguageManager.getString("ui.plugin.contextmenu.run"));
            MenuItem deleteItem=new MenuItem(LanguageManager.getString("ui.plugin.contextmenu.delete"));
            runItem.setOnAction(e -> runPlugin(config));
            deleteItem.setOnAction(event -> delete());

            if (config.getLevel() == FxPluginConfig.Level.SYSTEM){
                deleteItem.setDisable(true);
            }
            contextMenu.getItems().addAll(runItem,deleteItem);
            setOnContextMenuRequested(event -> {
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
            });
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY)
                    runPlugin(config);
            });
        }


        private void delete(){
            File file1 = new File("plugins/" + config.getPath());
            boolean b = deleteDirectory(file1);
            if (b){
                viewModel.getPlugins().remove(config);
            }else {
                NotificationManager.message(MessageInfo.error(LanguageManager.getString("ui.plugin.message01")));
            }

        }
        public boolean deleteDirectory(File directory) {
            if (!directory.exists()) {
                return false;
            }
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        deleteDirectory(file); // 递归删除
                    }
                }
            }
            return directory.delete();
        }
    }
}