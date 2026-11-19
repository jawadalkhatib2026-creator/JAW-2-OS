package dev.jaw;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

/** Lets the phone remote control other apps: Back/Home, arrow focus moves, OK, taps and swipes. */
public class JawAccessibilityService extends AccessibilityService {
    static volatile JawAccessibilityService inst;
    static final String NEED = "Turn on Jaw System in Accessibility first / فعّل جو سيستم من إعدادات إمكانية الوصول";

    @Override protected void onServiceConnected() { inst = this; }

    @Override public boolean onUnbind(android.content.Intent i) {
        inst = null;
        return super.onUnbind(i);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }

    @Override public void onInterrupt() { }

    static boolean act(int action) {
        JawAccessibilityService s = inst;
        return s != null && s.performGlobalAction(action);
    }

    // ---- focus navigation (dir: 0 up, 1 down, 2 left, 3 right) ----

    private static void collect(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out, int depth) {
        if (n == null || depth > 40) return;
        if (n.isVisibleToUser() && n.isEnabled() && (n.isFocusable() || n.isClickable())) out.add(n);
        for (int i = 0; i < n.getChildCount(); i++) collect(n.getChild(i), out, depth + 1);
    }

    private static AccessibilityNodeInfo scrollable(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 40) return null;
        if (n.isScrollable() && n.isVisibleToUser()) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo r = scrollable(n.getChild(i), depth + 1);
            if (r != null) return r;
        }
        return null;
    }

    static String move(int dir) {
        JawAccessibilityService s = inst;
        if (s == null) return NEED;
        AccessibilityNodeInfo root = s.getRootInActiveWindow();
        if (root == null) return "No window to control / لا توجد نافذة";
        List<AccessibilityNodeInfo> all = new ArrayList<>();
        collect(root, all, 0);
        AccessibilityNodeInfo cur = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (cur == null) cur = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        Rect cr = new Rect(), r = new Rect();
        AccessibilityNodeInfo best = null;
        long bestScore = Long.MAX_VALUE;
        if (cur == null) {
            for (AccessibilityNodeInfo n : all) {
                n.getBoundsInScreen(r);
                long sc = (long) r.top * 10000L + r.left;
                if (sc < bestScore) { bestScore = sc; best = n; }
            }
        } else {
            cur.getBoundsInScreen(cr);
            int cx = cr.centerX(), cy = cr.centerY();
            for (AccessibilityNodeInfo n : all) {
                if (n.equals(cur)) continue;
                n.getBoundsInScreen(r);
                int nx = r.centerX(), ny = r.centerY();
                long main, cross;
                if (dir == 0) { main = cy - ny; cross = Math.abs(nx - cx); }
                else if (dir == 1) { main = ny - cy; cross = Math.abs(nx - cx); }
                else if (dir == 2) { main = cx - nx; cross = Math.abs(ny - cy); }
                else { main = nx - cx; cross = Math.abs(ny - cy); }
                if (main <= 4) continue;
                long sc = main + cross * 3;
                if (sc < bestScore) { bestScore = sc; best = n; }
            }
        }
        if (best != null) {
            boolean ok = best.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            best.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            return ok ? "ok" : "ok";
        }
        AccessibilityNodeInfo sc = scrollable(root, 0);
        if (sc != null) {
            boolean fwd = dir == 1 || dir == 3;
            sc.performAction(fwd ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            return "ok";
        }
        return "Nothing to move to here. Use the touch pad / لا شيء للتحرك إليه، استخدم لوحة اللمس";
    }

    static String click() {
        JawAccessibilityService s = inst;
        if (s == null) return NEED;
        AccessibilityNodeInfo root = s.getRootInActiveWindow();
        if (root == null) return "No window to control / لا توجد نافذة";
        AccessibilityNodeInfo cur = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (cur == null) cur = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        while (cur != null && !cur.isClickable()) cur = cur.getParent();
        if (cur != null && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return "ok";
        return "Nothing selected. Move first or use the touch pad / لا شيء محدد";
    }

    // ---- touch (x, y as 0..1000 of the screen) ----

    private static String gesture(float x1, float y1, float x2, float y2, long ms) {
        JawAccessibilityService s = inst;
        if (s == null) return NEED;
        DisplayMetrics dm = s.getResources().getDisplayMetrics();
        Path p = new Path();
        p.moveTo(x1 / 1000f * dm.widthPixels, y1 / 1000f * dm.heightPixels);
        p.lineTo(x2 / 1000f * dm.widthPixels, y2 / 1000f * dm.heightPixels);
        try {
            GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, ms)).build();
            return s.dispatchGesture(g, null, null) ? "ok" : "Touch was refused / تم رفض اللمس";
        } catch (Exception e) { return "Touch was refused / تم رفض اللمس"; }
    }

    static String tap(float x, float y) { return gesture(x, y, x + 1, y + 1, 60); }

    static String swipe(float x1, float y1, float x2, float y2) { return gesture(x1, y1, x2, y2, 350); }
}
