package cn.tealc.wutheringwavestool.jna;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class GlobalKeyListener implements NativeKeyListener {
  public void nativeKeyPressed(NativeKeyEvent e) {
/*    if (e.getKeyCode() == NativeKeyEvent.VC_1 &&
            e.getModifiers() == NativeKeyEvent.VC_CONTROL && e.getModifiers() == NativeKeyEvent.VC_SHIFT) {
      System.out.println("Ctrl + Shift + L was pressed!");
      // 在这里执行你想要的操作!!!
    }*/
    System.out.println(e.getKeyCode());
    System.out.println(e.getModifiers());
    System.out.println("========");
  }

  public void nativeKeyReleased(NativeKeyEvent e) {

  }

  public void nativeKeyTyped(NativeKeyEvent e) {

  }

}
