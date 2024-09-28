open module cn.tealc.wutheringwavestool {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires java.net.http;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires de.saxsys.mvvmfx;
    requires cn.tealc.teafx ;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.material2;
    requires jdk.crypto.cryptoki;
    requires org.slf4j;
    requires com.sun.jna.platform;
    requires org.apache.commons.dbutils;
    requires org.xerial.sqlitejdbc;
    requires com.sun.jna;
    requires com.dustinredmond.fxtrayicon;
    requires filters;
    requires javafx.swing;
    requires net.coobird.thumbnailator;
    requires com.github.kwhat.jnativehook;
    requires nfx.core;

    exports cn.tealc.wutheringwavestool;
    exports cn.tealc.wutheringwavestool.model;
    exports cn.tealc.wutheringwavestool.model.roleData to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.model.roleData.user to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.model.analysis to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.model.sign to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.ui;


}