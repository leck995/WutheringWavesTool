package cn.tealc.wutheringwavestool.ui.component;

import cn.tealc.wutheringwavestool.base.Config;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

import java.util.Locale;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-11 20:16
 */
public class PoolNameCell extends ListCell<String> {
    private Label label;
    private StackPane stackPane;
    public PoolNameCell() {
        label=new Label();
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setPrefWidth(Label.USE_COMPUTED_SIZE);


    }

    @Override
    protected void updateItem(String s, boolean b) {
        super.updateItem(s, b);
        if (!b){
            if (Config.setting.getLanguage() == Locale.ENGLISH){
                label.setPrefHeight(90);
            }else {
                label.setPrefHeight(50);
            }

            String replace = s.replace("唤取", "").replace(" Pull","");

            if (replace.contains("感恩定向")){
                label.setText("感恩定向");
            }else if (replace.contains("Gratitude Directional Pull")){
                label.setText("Gratitude Directional Pull");
            }else {
                label.setText(replace);
            }
            setGraphic(label);
        }else {
            setGraphic(null);
        }
    }


}