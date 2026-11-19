package dev.jaw;

import android.content.Context;
import java.util.Locale;
import org.json.JSONArray;

/** Admin helper: understands short typed commands (English or Arabic keywords). It is not a cloud AI. */
final class Assistant {
    static final String HELP =
        "create account EMAIL PASSWORD  |  انشاء حساب EMAIL PASSWORD\n"
        + "delete account EMAIL  |  حذف حساب EMAIL\n"
        + "reset password EMAIL NEWPASSWORD  |  كلمة سر EMAIL NEWPASSWORD\n"
        + "list accounts  |  الحسابات\n"
        + "rename tv NAME  |  اسم التلفاز NAME\n"
        + "remote on / remote off  |  ريموت تشغيل / ريموت ايقاف\n"
        + "add app NAME PACKAGE APK_LINK [video|music|tools|games]\n"
        + "remove app NAME  |  حذف تطبيق NAME\n"
        + "theme 0-9  |  clock 24 / clock 12  |  glass on / glass off\n"
        + "reset home  |  reset wallpaper";

    private static String rest(String[] w, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < w.length; i++) sb.append(i > from ? " " : "").append(w[i]);
        return sb.toString();
    }

    static String run(Context c, String raw) {
        if (raw == null) return HELP;
        String s = raw.trim();
        if (s.isEmpty()) return HELP;
        String[] w = s.split("\\s+");
        String l = s.toLowerCase(Locale.US);
        try {
            if (l.startsWith("create account") || s.startsWith("انشاء حساب") || s.startsWith("إنشاء حساب")) {
                String e = w[w.length - 2], p = w[w.length - 1];
                String err = Accounts.create(c, e, p);
                return err == null ? "Account created: " + e : err;
            }
            if (l.startsWith("delete account") || s.startsWith("حذف حساب")) {
                Accounts.delete(c, w[w.length - 1].toLowerCase(Locale.US));
                return "Deleted (if it existed): " + w[w.length - 1];
            }
            if (l.startsWith("reset password") || s.startsWith("كلمة سر")) {
                String err = Accounts.setPassword(c, w[w.length - 2], w[w.length - 1]);
                return err == null ? "Password changed" : err;
            }
            if (l.startsWith("list accounts") || s.startsWith("الحسابات")) {
                JSONArray a = Accounts.all(c);
                if (a.length() == 0) return "No accounts";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < a.length(); i++) sb.append(a.getJSONObject(i).optString("email")).append('\n');
                return sb.toString().trim();
            }
            if (l.startsWith("rename tv") || s.startsWith("اسم التلفاز")) {
                String n = rest(w, 2);
                if (n.isEmpty()) return "Give a name";
                Store.setTvName(c, n);
                return "TV name: " + n;
            }
            if (l.startsWith("remote on") || s.startsWith("ريموت تشغيل")) { Store.setRemoteOn(c, true); JawApp.applyRemote(c); return "Phone remote is on"; }
            if (l.startsWith("remote off") || s.startsWith("ريموت ايقاف") || s.startsWith("ريموت إيقاف")) { Store.setRemoteOn(c, false); JawApp.applyRemote(c); return "Phone remote is off"; }
            if (l.startsWith("add app")) {
                int cat = 2;
                String last = w[w.length - 1].toLowerCase(Locale.US);
                int end = w.length;
                String[] cats = {"video", "music", "tools", "games"};
                for (int i = 0; i < 4; i++) if (cats[i].equals(last)) { cat = i; end = w.length - 1; }
                if (end < 5) return "Use: add app NAME PACKAGE APK_LINK [video|music|tools|games]";
                String apk = w[end - 1], pkg = w[end - 2];
                StringBuilder name = new StringBuilder();
                for (int i = 2; i < end - 2; i++) name.append(i > 2 ? " " : "").append(w[i]);
                String err = Catalog.add(c, cat, name.toString(), pkg, apk);
                return err == null ? "Added to Jaw AppStore: " + name : err;
            }
            if (l.startsWith("remove app") || s.startsWith("حذف تطبيق")) {
                String k = rest(w, 2);
                int n = Catalog.remove(c, k);
                return n > 0 ? "Removed " + n : "No custom store app named " + k;
            }
            if (l.startsWith("theme ")) {
                int t = Integer.parseInt(w[1]);
                if (t < 0 || t >= Theme.GRADIENTS.length) return "Theme 0-" + (Theme.GRADIENTS.length - 1);
                Store.setTheme(c, t);
                return "Theme set";
            }
            if (l.equals("clock 24")) { Store.setH24(c, true); return "24-hour clock"; }
            if (l.equals("clock 12")) { Store.setH24(c, false); return "12-hour clock"; }
            if (l.equals("glass on")) { Store.setGlass(c, true); return "Liquid Glass on"; }
            if (l.equals("glass off")) { Store.setGlass(c, false); return "Liquid Glass off"; }
            if (l.equals("reset home")) { Store.resetApps(c); return "Home apps reset"; }
            if (l.equals("reset wallpaper")) { Store.wallpaperFile(c).delete(); return "Wallpaper removed"; }
        } catch (Exception e) { return "I could not understand. Type help.\n\n" + HELP; }
        return "I could not understand. Type help.\n\n" + HELP;
    }
}
