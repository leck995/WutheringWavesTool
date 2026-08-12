package cn.tealc.wutheringwavestool.base;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AppInjector {
    private static final Injector injector = Guice.createInjector(
            new DataSourceModule(),
            new AppModule()
    );

    public static Injector getInjector() {
        return injector;
    }

    public static <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }
}
