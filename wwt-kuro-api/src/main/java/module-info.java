open module cn.tealc.wwt.kuro.api {
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires org.apache.commons.codec;
    requires java.net.http;

    exports com.kuro.model;
    exports com.kuro.kujiequ;
    exports com.kuro.kujiequ.api;
    exports com.kuro.kujiequ.model.sign;
    exports com.kuro.kujiequ.model.roleData;
    exports com.kuro.kujiequ.model.roleData.user;
    exports com.kuro.kujiequ.model.roleData.weight;
    exports com.kuro.kujiequ.model.towerData;
    exports com.kuro.kujiequ.model.newTowerData;
    exports com.kuro.kujiequ.model.slash;
    exports com.kuro.kujiequ.model.calculator.exist;
    exports com.kuro.kujiequ.model.calculator.list;
    exports com.kuro.kujiequ.model.calculator.result;
    exports com.kuro.kujiequ.model.resourcebriefing;
    exports com.kuro.game;
    exports com.kuro.game.model;
    exports com.kuro.game.model.game;
    exports com.kuro.game.model.launcher;
    exports com.kuro.game.model.launcher.item;
    exports com.kuro.launcher;
    exports com.kuro.launcher.model;
    exports com.kuro.launcher.model.api;
    exports com.kuro.util;
}
