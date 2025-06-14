package cn.tealc.wutheringwavestool.ui.component;

import com.jfoenixN.controls.JFXDialog;

/**
 * @description: 弹窗的基本类，使用前必须设置dialog
 * @author: Leck
 * @create: 2025-06-11 16:55
 */
public class BaseDialog {
    protected JFXDialog dialog;

    public void setDialog(JFXDialog dialog) {
        this.dialog = dialog;
    }


    public void closeDialog(){
        dialog.close();
    }




}