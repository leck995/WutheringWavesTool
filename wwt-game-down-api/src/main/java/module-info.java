module cn.tealc.download {
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires org.slf4j;

    opens cn.tealc.download.internal to com.fasterxml.jackson.databind;

    exports cn.tealc.download;
    exports cn.tealc.download.model;
    exports cn.tealc.download.model.game;
    exports cn.tealc.download.model.launcher;
    exports cn.tealc.download.error;
    exports cn.tealc.download.util;
}
