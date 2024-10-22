open module cn.tealc.wutheringwavestool {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.antdesignicons;
    requires java.net.http;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires de.saxsys.mvvmfx;
    requires cn.tealc.teafx ;

    requires jdk.crypto.cryptoki;
    requires org.slf4j;
    requires com.sun.jna.platform;
    requires org.apache.commons.dbutils;
    requires org.apache.commons.codec;
    requires org.xerial.sqlitejdbc;
    requires com.sun.jna;
    requires filters;
    requires javafx.swing;
    requires net.coobird.thumbnailator;
    requires com.github.kwhat.jnativehook;
    requires nfx.core;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;


    exports cn.tealc.wutheringwavestool;
    exports cn.tealc.wutheringwavestool.model;
    exports cn.tealc.wutheringwavestool.model.kujiequ.towerData;
    exports cn.tealc.wutheringwavestool.model.kujiequ.roleData to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.model.kujiequ.roleData.user to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.model.analysis to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.model.kujiequ.sign to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.model.kujiequ.roleData.weight;
    exports cn.tealc.wutheringwavestool.ui;
    exports cn.tealc.wutheringwavestool.base;
    exports cn.tealc.wutheringwavestool.jna;
    exports cn.tealc.wutheringwavestool.ui.kujiequ;

}