package net.nhiroki.bluelineconsole.applicationMain;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class SuperKeyAccessibilityService extends AccessibilityService {
    private boolean superKeyPressedAlone = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used. This service only listens for keyboard events.
    }

    @Override
    public void onInterrupt() {
        // No ongoing operation.
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!this.isPhysicalKeyboardEvent(event)) {
            this.superKeyPressedAlone = false;
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && MainActivity.isSuperKey(event.getKeyCode())) {
            this.superKeyPressedAlone = event.getRepeatCount() == 0;
            return false;
        }

        if (this.superKeyPressedAlone && event.getAction() == KeyEvent.ACTION_DOWN && !MainActivity.isSuperKey(event.getKeyCode())) {
            this.superKeyPressedAlone = false;
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_UP && MainActivity.isSuperKey(event.getKeyCode())) {
            final boolean shouldOpenOverview = this.superKeyPressedAlone;
            this.superKeyPressedAlone = false;
            if (shouldOpenOverview) {
                this.launchOverview();
                return true;
            }
        }

        return false;
    }

    private boolean isPhysicalKeyboardEvent(KeyEvent event) {
        if (event == null) {
            return false;
        }

        final InputDevice inputDevice = event.getDevice();
        return inputDevice != null
                && !inputDevice.isVirtual()
                && inputDevice.getKeyboardType() != InputDevice.KEYBOARD_TYPE_NONE;
    }

    private void launchOverview() {
        final Intent openHomeIntent = new Intent(this, HomeActivity.class);
        openHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openHomeIntent.putExtra(Intent.EXTRA_TEXT, "");
        startActivity(openHomeIntent);
    }
}
