package com.kuro.kujiequ.thread.base.sign;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.service.SignService;
import javafx.concurrent.Task;

public class SignTask extends Task<String> {

    @Override
    protected String call() {
        SignService signService = AppInjector.getInstance(SignService.class);
        return signService.signAll();
    }
}
