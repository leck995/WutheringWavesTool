package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.model.system.NavData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.saxsys.mvvmfx.*;

import java.io.InputStream;
import java.util.List;

public class NavLoader {
    public static ViewTuple<?, ?> load(NavData navData) {
        try {
            Class<?> viewClass =  Class.forName(navData.getViewClass());
            if (navData.isFxml()){
                @SuppressWarnings("unchecked")
                ViewTuple<?, ?> viewTuple = FluentViewLoader
                        .fxmlView((Class<? extends FxmlView<?>>) viewClass)
                        .load();
                return viewTuple;
            }else {
                @SuppressWarnings("unchecked")
                ViewTuple<?, ?> viewTuple = FluentViewLoader
                        .javaView((Class<? extends JavaView<?>>) viewClass)
                        .load();
                return viewTuple;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}