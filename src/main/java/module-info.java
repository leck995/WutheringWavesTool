open module cn.tealc.wutheringwavestool {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires MaterialFX ;
    requires java.net.http;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires de.saxsys.mvvmfx;
    requires cn.tealc.teafx ;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.material2;


    exports cn.tealc.wutheringwavestool;
    exports cn.tealc.wutheringwavestool.model to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.ui;
    exports cn.tealc.wutheringwavestool.model.analysis to com.fasterxml.jackson.databind;

}