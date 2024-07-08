package cn.tealc.wutheringwavestool.ui.component;

import cn.tealc.wutheringwavestool.model.sign.SignGood;
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
    private static final ImageManager imageManager = new ImageManager();
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

    }

    @Override
    protected void updateItem(SignGood signGood, boolean b) {
        super.updateItem(signGood, b);
        if (!b){
            name.setVisible(signGood.getSign());
            imageManager.setImage(signGood.getGoodsUrl(),imageView);
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

class ImageManager{
    private final Map<String, Image> imageMap=new LinkedHashMap<>();
    public void setImage(String url, ImageView imageView){
        Image pair = imageMap.get(url);
        if (pair == null){//不存在缓存，虚拟线程加载图片
            Image image1 = new Image(url,60,60,true,true,true);
            imageMap.put(url,image1);
            imageView.setImage(image1);
        }else { //存在缓存
                imageView.setImage(pair);
        }
        //限制最大容量为200
        if (imageMap.size() > 10){
            imageMap.remove(imageMap.keySet().iterator().next());
        }
    }


    public void clear(){
        imageMap.clear();
    }
}