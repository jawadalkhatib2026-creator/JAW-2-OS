package dev.jaw;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The Jaw AppStore catalog. "Our servers" = your GitHub repo: store/catalog.json is downloaded from it,
 * and each entry points to an APK file you host (for example in a GitHub release).
 * Entry: {"cat":0-3,"name":"App","pkg":"com.example.app","apk":"https://..."}  cat: 0 video, 1 music, 2 tools, 3 games
 */
final class Catalog {
    static final String URL = "https://raw.githubusercontent.com/" + Updater.REPO + "/main/store/catalog.json";

    private static File cache(Context c) { return new File(c.getFilesDir(), "catalog.json"); }

    private static JSONArray read(File f) {
        try { return new JSONArray(LocalServer.readText(f)); } catch (Exception e) { return new JSONArray(); }
    }

    /** Downloads the newest catalog in the background. Returns "ok" or "none". */
    static String refresh(Context c) {
        try {
            HttpURLConnection h = (HttpURLConnection) new URL(URL).openConnection();
            h.setConnectTimeout(8000);
            h.setReadTimeout(8000);
            if (h.getResponseCode() != 200) return "none";
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (InputStream in = h.getInputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    bo.write(buf, 0, r);
                    if (bo.size() > 800000) return "none";
                }
            }
            new JSONArray(bo.toString("UTF-8"));
            try (FileOutputStream fo = new FileOutputStream(cache(c))) { fo.write(bo.toByteArray()); }
            return "ok";
        } catch (Exception e) { return "none"; }
    }

    private static JSONArray custom(Context c) {
        try { return new JSONArray(Store.sp(c).getString("store_custom", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static List<String[]> items(Context c) {
        List<String[]> out = new ArrayList<>();
        JSONArray[] src = {custom(c), read(cache(c))};
        for (JSONArray a : src) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                String pkg = o.optString("pkg"), name = o.optString("name"), apk = o.optString("apk");
                if (pkg.isEmpty() || name.isEmpty()) continue;
                boolean dup = false;
                for (String[] e : out) if (e[2].equals(pkg)) dup = true;
                if (!dup) out.add(new String[]{String.valueOf(Math.max(0, Math.min(3, o.optInt("cat", 2)))), name, pkg, apk.startsWith("https://") ? apk : ""});
            }
        }
        return out;
    }

    static String add(Context c, int cat, String name, String pkg, String apk) {
        if (name.trim().isEmpty() || !pkg.matches("[A-Za-z0-9_.]+\\.[A-Za-z0-9_.]+")) return "Bad name or package / اسم أو حزمة غير صالحة";
        if (!apk.startsWith("https://")) return "The APK link must start with https:// / يجب أن يبدأ الرابط بـ https://";
        try {
            JSONArray a = custom(c), out = new JSONArray();
            for (int i = 0; i < a.length(); i++) if (!pkg.equals(a.getJSONObject(i).optString("pkg"))) out.put(a.getJSONObject(i));
            JSONObject o = new JSONObject();
            o.put("cat", cat);
            o.put("name", name.trim());
            o.put("pkg", pkg.trim());
            o.put("apk", apk.trim());
            out.put(o);
            Store.sp(c).edit().putString("store_custom", out.toString()).apply();
            return null;
        } catch (Exception e) { return "Error / خطأ"; }
    }

    /** Removes custom apps whose name or package matches. Returns how many were removed. */
    static int remove(Context c, String key) {
        int n = 0;
        try {
            JSONArray a = custom(c), out = new JSONArray();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (key.equalsIgnoreCase(o.optString("name")) || key.equals(o.optString("pkg"))) n++;
                else out.put(o);
            }
            Store.sp(c).edit().putString("store_custom", out.toString()).apply();
        } catch (Exception e) { }
        return n;
    }

    static String exportJson(Context c) { return custom(c).toString(); }
}
