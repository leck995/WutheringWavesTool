package cn.tealc.wutheringwavestool.model.roleData;

/**
 * @program: WutheringWavesTool
 * @description: 用于角色详情类
 * @author: Leck
 * @create: 2024-10-03 20:22
 */
public class PhantomEx extends Phantom {
    private Status status;



     enum Status{
        SSS("完美"),
        SS("毕业"),
        S("小毕业"),
        N("普通");

        String text;
        Status(String text) {
        }
    }
}