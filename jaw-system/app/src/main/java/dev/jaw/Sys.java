package dev.jaw;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

final class Sys {
    private static boolean open(Context c, String action) {
        try {
            c.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void tvSettings(Context c) {
        if (!open(c, Settings.ACTION_SETTINGS)) Toast.makeText(c, R.string.cant_open, Toast.LENGTH_SHORT).show();
    }

    static void dateSettings(Context c) {
        if (!open(c, Settings.ACTION_DATE_SETTINGS)) tvSettings(c);
    }

    /** Asks Android to make Jaw the home app. Many TVs hide this, so the last fallback shows the adb command. */
    static void homeSettings(Context c) {
        if (c instanceof android.app.Activity && android.os.Build.VERSION.SDK_INT >= 29) {
            try {
                android.app.role.RoleManager rm = (android.app.role.RoleManager) c.getSystemService(Context.ROLE_SERVICE);
                if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)
                        && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    ((android.app.Activity) c).startActivityForResult(
                        rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), 77);
                    return;
                }
            } catch (Exception e) { /* try the next way */ }
        }
        if (open(c, Settings.ACTION_HOME_SETTINGS)) return;
        if (open(c, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) return;
        if (c instanceof android.app.Activity) {
            new android.app.AlertDialog.Builder(c).setTitle(R.string.home_launcher)
                .setMessage(R.string.home_adb).setPositiveButton(R.string.ok, null).show();
        } else Toast.makeText(c, R.string.home_fallback, Toast.LENGTH_LONG).show();
    }
}
