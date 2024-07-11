package cn.tealc.wutheringwavestool.ui.component;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-11 20:16
 */
public class PoolNameCell extends ListCell<String> {
    private Label label;
    public PoolNameCell() {
        label=new Label();
    }

    @Override
    protected void updateItem(String s, boolean b) {
        super.updateItem(s, b);
        if (!b){
            String replace = s.replace("唤取", "");
            if (replace.contains("感恩定向")){
                label.setText("感恩定向");
            }else {
                label.setText(replace);
            }
            setGraphic(label);
        }else {
            setGraphic(null);
        }
    }
}