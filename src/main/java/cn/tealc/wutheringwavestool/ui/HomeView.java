package cn.tealc.wutheringwavestool.ui;

import de.saxsys.mvvmfx.FxmlView;
import de.saxsys.mvvmfx.InjectViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 19:57
 */
public class HomeView implements Initializable, FxmlView<HomeViewModel> {
    @InjectViewModel
    private HomeViewModel viewModel;

    @FXML
    private AnchorPane root;
    @FXML
    private ImageView roleIV;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        File file=new File("assets/image/home-bg.png");

   /*     root.setBackground(new Background(
                new BackgroundImage(
                        new Image(file.toURI().toString()),
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, true))));*/
        File file2=new File("assets/image/home-role.png");
      /*  roleIV.setFitHeight(500);
        roleIV.setFitWidth(900);*/
        roleIV.setPreserveRatio(true);
        roleIV.setSmooth(true);
        roleIV.fitWidthProperty().bind(root.widthProperty().multiply(0.7));
        roleIV.fitHeightProperty().bind(root.heightProperty().multiply(0.7));
        roleIV.setImage(new Image(file2.toURI().toString(),true));
    }

    @FXML
    void startGame(ActionEvent event) {
        viewModel.startGame();
    }

    @FXML
    void startUpdate(ActionEvent event) {
        viewModel.startUpdate();
    }
}