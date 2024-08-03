package cn.tealc.wutheringwavestool.ui.component;

import cn.tealc.wutheringwavestool.model.sign.SignGood;
import cn.tealc.wutheringwavestool.util.LocalResourcesManager;
import de.saxsys.mvvmfx.MvvmFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.controlsfx.control.GridCell;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-07 22:27
 */
public class SignGoodCell extends GridCell<SignGood> {
    private ImageView imageView=new ImageView();
    private Label name=new Label("已签");
    private Label num=new Label();
    private Label index=new Label();

    public SignGoodCell() {
        imageView.setFitHeight(60);
        imageView.setFitWidth(60);
        name.getStyleClass().add("name");
        index.getStyleClass().add("index");
        num.getStyleClass().add("num");
        StackPane stackPane=new StackPane(imageView,num,index,name);
        StackPane.setAlignment(num, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(index, Pos.TOP_LEFT);
        stackPane.setPrefSize(70,90);
        stackPane.setPadding(new Insets(3));
        stackPane.getStyleClass().add("goods");
        setGraphic(stackPane);
        updateSelected(false);

    }


    @Override
    protected void updateItem(SignGood signGood, boolean b) {
        super.updateItem(signGood, b);
        if (!b){
            name.setVisible(signGood.getSign());
            imageView.setImage(LocalResourcesManager.imageBuffer(signGood.getGoodsUrl(),60,60,true,true));
            num.setText(String.format("x%d",signGood.getGoodsNum()));
            index.setText(String.format("%02d",signGood.getSerialNum()+1));
        }else {
            imageView.setImage(null);
            num.setText(null);
            index.setText(null);
            name.setVisible(false);
        }
    }

    public void done(){
        name.setVisible(true);
    }

}