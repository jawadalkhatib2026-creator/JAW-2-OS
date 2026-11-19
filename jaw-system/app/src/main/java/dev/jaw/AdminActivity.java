package dev.jaw;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import org.json.JSONArray;

public class AdminActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Accounts.adminOk) { finish(); return; }
        render();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        Accounts.adminOk = false;
    }

    private String[] emails() {
        JSONArray a = Accounts.all(this);
        String[] out = new String[a.length()];
        for (int i = 0; i < out.length; i++) out[i] = a.optJSONObject(i).optString("email");
        return out;
    }

    private void confirm(Runnable action) {
        new AlertDialog.Builder(this).setMessage(R.string.admin_sure)
            .setPositiveButton(R.string.ok, (d, w) -> {
                action.run();
                Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
                render();
            })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void form(int title, int[] hints, boolean[] secret, java.util.function.Consumer<String[]> ok) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 24), Ui.dp(this, 12), Ui.dp(this, 24), 0);
        final EditText[] f = new EditText[hints.length];
        for (int i = 0; i < hints.length; i++) {
            f[i] = new EditText(this);
            f[i].setHint(hints[i]);
            f[i].setSingleLine(true);
            f[i].setInputType(InputType.TYPE_CLASS_TEXT | (secret[i] ? InputType.TYPE_TEXT_VARIATION_PASSWORD : 0));
            box.addView(f[i]);
        }
        new AlertDialog.Builder(this).setTitle(title).setView(box)
            .setPositiveButton(R.string.ok, (d, w) -> {
                String[] v = new String[f.length];
                for (int i = 0; i < v.length; i++) v[i] = f[i].getText().toString().trim();
                ok.accept(v);
            })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void toast(String m) { Toast.makeText(this, m == null ? getString(R.string.done) : m, Toast.LENGTH_LONG).show(); }

    private void assistant() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 24), Ui.dp(this, 12), Ui.dp(this, 24), 0);
        final EditText in = new EditText(this);
        in.setHint("help");
        in.setSingleLine(true);
        final TextView out = new TextView(this);
        out.setText(Assistant.HELP);
        out.setTextSize(13);
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(out);
        box.addView(in);
        box.addView(sv, new LinearLayout.LayoutParams(-1, Ui.dp(this, 220)));
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(R.string.admin_assistant).setView(box)
            .setPositiveButton(R.string.run, null)
            .setNegativeButton(R.string.close, null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            out.setText(Assistant.run(this, in.getText().toString()));
            in.setText("");
        }));
        dlg.show();
    }

    private void render() {
        LinearLayout[] pg = new LinearLayout[1];
        ScrollView sv = Ui.screen(this, getString(R.string.admin_panel), pg);
        LinearLayout page = pg[0];
        View first = Ui.row(this, page, getString(R.string.admin_accounts, Accounts.count(this)), null, v -> {
            String[] e = emails();
            new AlertDialog.Builder(this).setTitle(R.string.jaw_accounts)
                .setMessage(e.length == 0 ? getString(R.string.no_accounts) : android.text.TextUtils.join("\n", e))
                .setPositiveButton(R.string.close, null).show();
        });
        Ui.row(this, page, getString(R.string.admin_create), null, v -> form(R.string.admin_create,
            new int[]{R.string.email, R.string.password_hint}, new boolean[]{false, true}, x -> {
                String err = Accounts.create(this, x[0], x[1]);
                toast(err);
                render();
            }));
        Ui.row(this, page, getString(R.string.admin_reset_pw), null, v -> form(R.string.admin_reset_pw,
            new int[]{R.string.email, R.string.password_hint}, new boolean[]{false, true}, x -> toast(Accounts.setPassword(this, x[0], x[1]))));
        Ui.row(this, page, getString(R.string.admin_assistant), null, v -> assistant());
        Ui.row(this, page, getString(R.string.admin_tv_name), Store.tvName(this), v -> form(R.string.admin_tv_name,
            new int[]{R.string.tv_name}, new boolean[]{false}, x -> { if (!x[0].isEmpty()) Store.setTvName(this, x[0]); render(); }));
        Ui.row(this, page, getString(R.string.admin_remote), getString(Store.remoteOn(this) ? R.string.on : R.string.off), v -> {
            Store.setRemoteOn(this, !Store.remoteOn(this));
            JawApp.applyRemote(this);
            render();
        });
        Ui.row(this, page, getString(R.string.admin_add_app), null, v -> form(R.string.admin_add_app,
            new int[]{R.string.app_name_hint, R.string.pkg_hint, R.string.apk_hint}, new boolean[]{false, false, false},
            x -> toast(Catalog.add(this, 2, x[0], x[1], x[2]))));
        Ui.row(this, page, getString(R.string.admin_remove_app), null, v -> form(R.string.admin_remove_app,
            new int[]{R.string.app_name_hint}, new boolean[]{false}, x -> toast(Catalog.remove(this, x[0]) + " removed")));
        Ui.row(this, page, getString(R.string.admin_refresh_store), null, v -> new Thread(() -> {
            String r = Catalog.refresh(getApplicationContext());
            runOnUiThread(() -> Toast.makeText(this, "ok".equals(r) ? R.string.catalog_updated : R.string.catalog_none, Toast.LENGTH_LONG).show());
        }).start());
        Ui.row(this, page, getString(R.string.admin_delete_one), null, v -> {
            String[] e = emails();
            if (e.length == 0) { Toast.makeText(this, R.string.no_accounts, Toast.LENGTH_SHORT).show(); return; }
            new AlertDialog.Builder(this).setTitle(R.string.admin_delete_one)
                .setItems(e, (d, which) -> { Accounts.delete(this, e[which]); render(); })
                .setNegativeButton(R.string.cancel, null).show();
        });
        Ui.row(this, page, getString(R.string.admin_delete_all), null, v -> confirm(() -> Accounts.clear(this)));
        String mode = Store.accountMode(this);
        Ui.row(this, page, getString(R.string.account_type), getString(Store.modeLabel(mode)), v -> {
            Store.setAccountMode(this, Store.nextMode(mode));
            render();
        });
        Ui.row(this, page, getString(R.string.admin_reset_home), null, v -> confirm(() -> Store.resetApps(this)));
        Ui.row(this, page, getString(R.string.admin_reset_wall), null, v -> confirm(() -> Store.wallpaperFile(this).delete()));
        Ui.row(this, page, getString(R.string.admin_clear_channels), null, v -> confirm(() -> {
            JSONArray a = Store.channels(this);
            for (int i = 0; i < a.length(); i++) Store.removeChannel(this, a.optJSONObject(i).optString("id"));
        }));
        Ui.row(this, page, getString(R.string.admin_wipe), null, v -> confirm(() -> {
            JSONArray a = Store.channels(this);
            for (int i = 0; i < a.length(); i++) Store.removeChannel(this, a.optJSONObject(i).optString("id"));
            Store.wallpaperFile(this).delete();
            Accounts.clear(this);
            Store.sp(this).edit().clear().apply();
        }));
        setContentView(sv);
        first.requestFocus();
    }
}
