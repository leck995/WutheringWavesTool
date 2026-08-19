module cn.tealc.wwt.game.resource.core {
    requires com.fasterxml.jackson.databind;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires java.net.http;
    requires org.slf4j;

    opens cn.tealc.wwt.game.resource to com.fasterxml.jackson.databind;
    opens cn.tealc.wwt.game.resource.internal to com.fasterxml.jackson.databind;
    opens cn.tealc.wwt.game.resource.internal.legacy.model to com.fasterxml.jackson.databind;

    exports cn.tealc.wwt.game.resource;
    exports cn.tealc.wwt.game.resource.model;
    exports cn.tealc.wwt.game.resource.util;

}
