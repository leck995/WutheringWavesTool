package cn.tealc.wutheringwavestool.ui.component;

import atlantafx.base.controls.Spacer;
import atlantafx.base.controls.Tile;
import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.model.analysis.SsrData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.io.File;
import java.util.Random;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-03 15:44
 */
public class SsrCell extends ListCell<SsrData> {
    public static final String[] COLORS= {"#66cccc","#ff99cc","#1a7f37","#66cc99","#99cc00","#cccccc"};
    private final HBox root;
    private ImageView iv;
    private Label name;
    private Label date;
    private Label count;
    private ProgressBar progressBar;
    private Label desc;

    public SsrCell() {
        iv = new ImageView();
        iv.setFitHeight(60);
        iv.setFitWidth(60);
        iv.setSmooth(true);
        Circle circle = new Circle(30,30,30);
        iv.setClip(circle);

        name = new Label();
        name.getStyleClass().add("role-name");
        date=new Label();
        date.getStyleClass().add("role-date");
        Spacer spacer=new Spacer();
        HBox top = new HBox(5.0,name,spacer,date);
        top.setAlignment(Pos.CENTER_LEFT);

        count=new Label();
        count.getStyleClass().add("role-date");
        progressBar = new ProgressBar();
        progressBar.setProgress(0);
        progressBar.setPrefWidth(200);
        HBox progressHBox = new HBox(5.0,progressBar,count);
        progressHBox.setAlignment(Pos.CENTER);
        VBox parent = new VBox(5.0,top,progressHBox);
        parent.setAlignment(Pos.CENTER_LEFT);

        desc=new Label();
        desc.getStyleClass().add("role-desc");
        root = new HBox(15.0,iv,parent,desc);
        root.setAlignment(Pos.CENTER_LEFT);
        Random random=new Random();
        int i = random.nextInt(5)+1;
        root.getStyleClass().addAll("role-pane",String.format("role-pane-%d",i));


    }

    @Override
    protected void updateItem(SsrData ssrData, boolean b) {
        super.updateItem(ssrData, b);
        if (!b){

            String img = Config.headers.get(ssrData.getName());
            if (img!=null){
                iv.setImage(new Image(img,60,60,true,true,true));
            }else {
                File file=new File("assets/thumb/今汐.png");
                if (file.exists()){
                    iv.setImage(new Image(file.toURI().toString(),60,60,true,true,true));
                }else {
                    iv.setImage(null);
                }

            }

            name.setText(ssrData.getName());
            date.setText(ssrData.getDate());
            count.setText(String.valueOf(ssrData.getCount()));
            progressBar.setProgress(ssrData.getCount()/80.0);

            if (ssrData.isEvent()){
                desc.setText("限定");
                desc.setTextFill(Color.web("#ffd000"));
                //desc.setFont(Font.font());
                //desc.getStyleClass().remove("role-desc-2");
                //desc.getStyleClass().add("role-desc-1");
            }else {
                desc.setText("常驻");
                desc.setTextFill(Color.web("#787878"));
                //desc.getStyleClass().remove("role-desc-1");
                //desc.getStyleClass().add("role-desc-2");
            }

            setGraphic(root);
        }else {
            setGraphic(null);
        }
    }
}