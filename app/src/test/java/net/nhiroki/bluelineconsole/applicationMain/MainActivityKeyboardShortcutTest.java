package net.nhiroki.bluelineconsole.applicationMain;

import android.view.KeyEvent;

import org.junit.Assert;
import org.junit.Test;

public class MainActivityKeyboardShortcutTest {
    @Test
    public void escapeKeyClosesWindow() {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE);
        Assert.assertTrue(MainActivity.isCloseShortcutEvent(event));
    }

    @Test
    public void ctrlWClosesWindow() {
        KeyEvent event = new KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_W, 0, KeyEvent.META_CTRL_ON);
        Assert.assertTrue(MainActivity.isCloseShortcutEvent(event));
    }


    @Test
    public void ctrlQClosesWindow() {
        KeyEvent event = new KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Q, 0, KeyEvent.META_CTRL_ON);
        Assert.assertTrue(MainActivity.isCloseShortcutEvent(event));
    }

    @Test
    public void altF4ClosesWindow() {
        KeyEvent event = new KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_F4, 0, KeyEvent.META_ALT_ON);
        Assert.assertTrue(MainActivity.isCloseShortcutEvent(event));
    }
    @Test
    public void keyDownEventDoesNotCloseWindow() {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE);
        Assert.assertFalse(MainActivity.isCloseShortcutEvent(event));
    }
}
