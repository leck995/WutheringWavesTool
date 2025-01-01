open module cn.tealc.wutheringwavestool {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.swing;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.antdesignicons;
    requires java.net.http;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires de.saxsys.mvvmfx;
    requires cn.tealc.teafx;
    requires jdk.crypto.cryptoki;
    requires org.slf4j;
    requires com.sun.jna.platform;
    requires org.apache.commons.dbutils;
    requires org.apache.commons.codec;
    requires org.xerial.sqlitejdbc;
    requires com.sun.jna;
    requires filters;
    requires net.coobird.thumbnailator;
    requires com.github.kwhat.jnativehook;
    requires nfx.core;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;
    requires cn.tealc.fxplugin;

    exports cn.tealc.wutheringwavestool;
    exports cn.tealc.wutheringwavestool.model;
    exports cn.tealc.wutheringwavestool.model.analysis to com.fasterxml.jackson.databind;
    exports cn.tealc.wutheringwavestool.base;
    exports cn.tealc.wutheringwavestool.jna;
    exports cn.tealc.wutheringwavestool.ui;
    exports cn.tealc.wutheringwavestool.ui.kujiequ;
    exports cn.tealc.wutheringwavestool.ui.game;
    exports cn.tealc.wutheringwavestool.dao;
    exports cn.tealc.wutheringwavestool.util;
    exports cn.tealc.wutheringwavestool.model.message;
    exports cn.tealc.wutheringwavestool.plugin;


    exports com.kuro.kujiequ.model.towerData;
    exports com.kuro.kujiequ.model.roleData to com.fasterxml.jackson.databind;
    exports com.kuro.kujiequ.model.roleData.user to com.fasterxml.jackson.databind;
    exports com.kuro.kujiequ.model.sign to com.fasterxml.jackson.databind, WutheringWavesTool, WutheringWavesTool_test;
    exports com.kuro.kujiequ.model.roleData.weight;
    exports com.kuro.kujiequ.thread;
    exports com.kuro.game.model;
    exports com.kuro.kujiequ;


}