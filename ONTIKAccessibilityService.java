package com.ontik.screenai;

import android.accessibilityservice.AccessibilityService;
import android.media.AudioManager;
import java.util.Locale;
import android.accessibilityservice.GestureDescription;
import java.util.Locale;
import android.graphics.Path;
import java.util.Locale;
import android.os.Bundle;
import java.util.Locale;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;
import android.view.accessibility.AccessibilityEvent;
import java.util.Locale;
import java.util.List;

public class ONTIKAccessibilityService extends AccessibilityService {

    private static ONTIKAccessibilityService instance;

    public static void recents() {
        if (instance != null) {
            instance.performGlobalAction(
                    AccessibilityService.GLOBAL_ACTION_RECENTS
            );
        }
    }

    public static void volumeUp() {
        if (instance == null) return;

        AudioManager audio =
                (AudioManager) instance.getSystemService(
                        android.content.Context.AUDIO_SERVICE
                );

        if (audio != null) {
            audio.adjustVolume(
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
            );
        }
    }

    public static void volumeDown() {
        if (instance == null) return;

        AudioManager audio =
                (AudioManager) instance.getSystemService(
                        android.content.Context.AUDIO_SERVICE
                );

        if (audio != null) {
            audio.adjustVolume(
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
            );
        }
    }

    public static void muteVolume() {
        if (instance == null) return;

        AudioManager audio =
                (AudioManager) instance.getSystemService(
                        android.content.Context.AUDIO_SERVICE
                );

        if (audio != null) {
            audio.adjustVolume(
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
            );
        }
    }

    public static String getScreenSignature() {
        if (instance == null) {
            return "OFF";
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) {
            return "NO_SCREEN";
        }

        StringBuilder out =
                new StringBuilder();

        CharSequence pkg =
                root.getPackageName();

        if (pkg != null) {
            out.append(pkg);
        }

        collectSignature(root, out, 0);

        root.recycle();

        return Integer.toHexString(
                out.toString().hashCode()
        );
    }

    private static void collectSignature(
            AccessibilityNodeInfo node,
            StringBuilder out,
            int depth) {

        if (node == null || depth > 12) {
            return;
        }

        CharSequence text =
                node.getText();

        CharSequence desc =
                node.getContentDescription();

        if (text != null && text.length() > 0) {
            out.append("|T:").append(text);
        }

        if (desc != null && desc.length() > 0) {
            out.append("|D:").append(desc);
        }

        for (int i = 0;
                i < node.getChildCount();
                i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child != null) {
                collectSignature(
                        child,
                        out,
                        depth + 1
                );

                child.recycle();
            }
        }
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    public static void home() {
        if (instance != null) instance.performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public static void back() {
        if (instance != null) instance.performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public static void scrollDown() {
        if (instance == null) return;
        GestureDescription.Builder b = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(500, 1400);
        p.lineTo(500, 400);
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 400));
        instance.dispatchGesture(b.build(), null, null);
    }

    public static void scrollUp() {
        if (instance == null) return;
        GestureDescription.Builder b = new GestureDescription.Builder();
        Path p = new Path();
        p.moveTo(500, 400);
        p.lineTo(500, 1400);
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 400));
        instance.dispatchGesture(b.build(), null, null);
    }

    public static void searchCurrentApp(String query) {
        if (instance == null || query == null ||
                query.trim().isEmpty()) return;

        searchRetry(query.trim(), 0);
    }

    private static void searchRetry(String query, int attempt) {
        if (instance == null || query == null ||
                query.trim().isEmpty()) return;

        if (attempt >= 10) {
            return;
        }

        if (setText(query)) {
            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).postDelayed(() -> {

                if (!submitSearch()) {
                    clickFirstMatching(
                            "Search",
                            "Search videos",
                            "Search YouTube",
                            "Find",
                            "ic_search",
                            "search"
                    );
                }

            }, 500);

            return;
        }

        clickFirstMatching(
                "Search",
                "Search videos",
                "Search YouTube",
                "Find",
                "ic_search",
                "search"
        );

        new android.os.Handler(
                android.os.Looper.getMainLooper()
        ).postDelayed(() ->
                searchRetry(query, attempt + 1), 800);
    }

    private static boolean submitSearch() {
        if (instance == null) return false;

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) return false;

        AccessibilityNodeInfo field =
                findBestEditable(root);

        boolean ok = false;

        if (field != null) {
            try {
                field.performAction(
                        AccessibilityNodeInfo.ACTION_FOCUS
                );

                ok = field.performAction(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId()
                );

                if (!ok) {
                    Bundle args = new Bundle();
                    args.putInt(
                            AccessibilityNodeInfo
                                    .ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                            0
                    );
                }

                field.recycle();

            } catch (Exception ignored) {
            }
        }

        root.recycle();

        if (ok) return true;

        clickFirstMatching(
                "Search",
                "Search videos",
                "Search YouTube",
                "Find",
                "ic_search",
                "search"
        );

        return true;
    }

    public static boolean playFirstVideo() {
    if (instance == null) return false;

    new android.os.Handler(
            android.os.Looper.getMainLooper()
    ).postDelayed(() -> {
        if (instance == null) return;

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) return;

        AccessibilityNodeInfo target =
                findFirstRealVideo(root);

        if (target != null) {
            clickOnlyVideo(target);
            target.recycle();
        }

        root.recycle();
    }, 1800);

    return true;
}

private static AccessibilityNodeInfo findFirstRealVideo(
        AccessibilityNodeInfo root) {

    java.util.ArrayList<AccessibilityNodeInfo> list =
            new java.util.ArrayList<>();

    collectRealVideos(root, list);

    AccessibilityNodeInfo best = null;
    int bestScore = -1;

    for (AccessibilityNodeInfo n : list) {

        int score = videoScore(n);

        if (score > bestScore) {

            if (best != null) {
                best.recycle();
            }

            best =
                    AccessibilityNodeInfo.obtain(n);

            bestScore = score;
        }

        n.recycle();
    }

    list.clear();

    return bestScore >= 30 ? best : null;
}

private static void collectRealVideos(
        AccessibilityNodeInfo node,
        java.util.ArrayList<AccessibilityNodeInfo> list) {

    if (node == null) return;

    if (node.isVisibleToUser()) {

        android.graphics.Rect r =
                new android.graphics.Rect();

        node.getBoundsInScreen(r);

        String text =
                node.getText() == null
                        ? ""
                        : node.getText().toString();

        String desc =
                node.getContentDescription() == null
                        ? ""
                        : node.getContentDescription().toString();

        String id =
                node.getViewIdResourceName() == null
                        ? ""
                        : node.getViewIdResourceName();

        String all =
                (text + " " + desc + " " + id)
                        .toLowerCase(Locale.US);

        boolean forbidden =
                all.contains("back") ||
                all.contains("navigate up") ||
                all.contains("search") ||
                all.contains("microphone") ||
                all.contains("keyboard") ||
                all.contains("home") ||
                all.contains("menu") ||
                all.contains("settings") ||
                all.contains("shorts") ||
                all.contains("subscribe");

        boolean video =
                all.contains("thumbnail") ||
                all.contains("video") ||
                all.contains("views") ||
                all.contains("watch") ||
                all.contains("ago") ||
                all.contains("channel") ||
                all.contains("ভিডিও");

        if (!forbidden &&
                video &&
                r.top > 180 &&
                r.width() > 150 &&
                r.height() > 50) {

            list.add(
                    AccessibilityNodeInfo.obtain(node)
            );
        }
    }

    for (int i = 0;
            i < node.getChildCount();
            i++) {

        AccessibilityNodeInfo child =
                node.getChild(i);

        if (child != null) {

            collectRealVideos(
                    child,
                    list
            );

            child.recycle();
        }
    }
}

private static int videoScore(
        AccessibilityNodeInfo node) {

    if (node == null) return -1000;

    android.graphics.Rect r =
            new android.graphics.Rect();

    node.getBoundsInScreen(r);

    String text =
            node.getText() == null
                    ? ""
                    : node.getText().toString();

    String desc =
            node.getContentDescription() == null
                    ? ""
                    : node.getContentDescription().toString();

    String id =
            node.getViewIdResourceName() == null
                    ? ""
                    : node.getViewIdResourceName();

    String all =
            (text + " " + desc + " " + id)
                    .toLowerCase(Locale.US);

    int score = 0;

    if (node.isClickable())
        score += 25;

    if (all.contains("thumbnail"))
        score += 40;

    if (all.contains("video"))
        score += 35;

    if (all.contains("views"))
        score += 30;

    if (all.contains("watch"))
        score += 25;

    if (all.contains("ago"))
        score += 20;

    if (all.contains("channel"))
        score += 15;

    if (!text.trim().isEmpty())
        score += 10;

    if (!desc.trim().isEmpty())
        score += 10;

    if (r.top > 180)
        score += 15;

    if (r.top > 250)
        score += 10;

    if (r.width() > 300)
        score += 10;

    return score;
}

private static boolean clickOnlyVideo(
        AccessibilityNodeInfo node) {

    if (node == null) return false;

    AccessibilityNodeInfo current =
            AccessibilityNodeInfo.obtain(node);

    for (int i = 0;
            i < 6 &&
            current != null;
            i++) {

        if (current.isVisibleToUser() &&
                current.isClickable()) {

            boolean ok =
                    current.performAction(
                            AccessibilityNodeInfo.ACTION_CLICK
                    );

            current.recycle();

            return ok;
        }

        AccessibilityNodeInfo parent =
                current.getParent();

        current.recycle();
        current = parent;
    }

    return false;
}

public static boolean setText(String text) {
        if (instance == null || text == null) {
            return false;
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo field =
                findBestEditable(root);

        boolean ok = false;

        if (field != null) {
            Bundle args = new Bundle();

            args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
            );

            ok = field.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    args
            );

            field.recycle();
        }

        root.recycle();
        return ok;
    }

    private static AccessibilityNodeInfo findBestEditable(
            AccessibilityNodeInfo node) {

        if (node == null) {
            return null;
        }

        if (node.isEditable() &&
                node.isVisibleToUser()) {

            String id =
                    node.getViewIdResourceName();

            String hint =
                    node.getHintText() == null
                            ? ""
                            : node.getHintText().toString();

            String desc =
                    node.getContentDescription() == null
                            ? ""
                            : node.getContentDescription().toString();

            String all =
                    ((id == null ? "" : id) + " " +
                     hint + " " +
                     desc).toLowerCase(Locale.US);

            if (all.contains("search") ||
                    all.contains("query") ||
                    all.contains("find")) {

                return AccessibilityNodeInfo.obtain(node);
            }
        }

        AccessibilityNodeInfo fallback = null;

        for (int i = 0;
                i < node.getChildCount();
                i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            AccessibilityNodeInfo result =
                    findBestEditable(child);

            child.recycle();

            if (result != null) {
                if (fallback != null) {
                    fallback.recycle();
                }
                return result;
            }
        }

        if (node.isEditable() &&
                node.isVisibleToUser()) {

            fallback =
                    AccessibilityNodeInfo.obtain(node);
        }

        return fallback;
    }

    private static AccessibilityNodeInfo findEditable(
            AccessibilityNodeInfo node) {

        if (node == null) return null;

        if (node.isEditable() &&
                node.isVisibleToUser()) {
            return AccessibilityNodeInfo.obtain(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) continue;

            AccessibilityNodeInfo result =
                    findEditable(child);

            child.recycle();

            if (result != null) return result;
        }

        return null;
    }

    public static void clickFirstMatching(
            String... texts) {

        if (instance == null ||
                texts == null) return;

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) return;

        AccessibilityNodeInfo target =
                findClickable(root, texts);

        if (target != null) {
            target.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
            );
            target.recycle();
        }

        root.recycle();
    }

    private static AccessibilityNodeInfo findClickable(
            AccessibilityNodeInfo node,
            String[] texts) {

        if (node == null) return null;

        String text = node.getText() == null
                ? ""
                : node.getText().toString();

        String desc = node.getContentDescription() == null
                ? ""
                : node.getContentDescription().toString();

        String id = node.getViewIdResourceName() == null
                ? ""
                : node.getViewIdResourceName();

        for (String wanted : texts) {
            if (wanted == null) continue;

            String w =
                    wanted.toLowerCase(Locale.US);

            if (text.toLowerCase(Locale.US).contains(w) ||
                    desc.toLowerCase(Locale.US).contains(w) ||
                    id.toLowerCase(Locale.US).contains(w)) {

                if (node.isClickable()) {
                    return AccessibilityNodeInfo.obtain(node);
                }

                AccessibilityNodeInfo parent =
                        node.getParent();

                if (parent != null &&
                        parent.isClickable()) {

                    AccessibilityNodeInfo result =
                            AccessibilityNodeInfo.obtain(parent);

                    parent.recycle();
                    return result;
                }

                if (parent != null) {
                    parent.recycle();
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) continue;

            AccessibilityNodeInfo result =
                    findClickable(child, texts);

            child.recycle();

            if (result != null) return result;
        }

        return null;
    }

    // SCREEN_ANALYST_V1_1

    // SCREEN_ANALYST_V1_1

    public static String analyzeCurrentScreen() {
        if (instance == null) {
            return "Accessibility Service is OFF";
        }

        AccessibilityNodeInfo r = instance.getRootInActiveWindow();

        if (r == null) {
            return "No active screen detected";
        }

        StringBuilder out = new StringBuilder();

        out.append("APP: ")
           .append(r.getPackageName() == null ? "unknown" : r.getPackageName())
           .append("\n");

        analyzeNode(r, out, 0);

        r.recycle();

        return out.toString();
    }

    private static void analyzeNode(
            AccessibilityNodeInfo node,
            StringBuilder out,
            int depth) {

        if (node == null || depth > 20) {
            return;
        }

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String viewId = node.getViewIdResourceName();

        boolean clickable = node.isClickable();
        boolean editable = node.isEditable();
        boolean focusable = node.isFocusable();

        if ((text != null && text.length() > 0) ||
                (desc != null && desc.length() > 0) ||
                clickable ||
                editable ||
                focusable) {

            out.append("\n");

            if (text != null && text.length() > 0) {
                out.append("TEXT: ")
                   .append(text)
                   .append("\n");
            }

            if (desc != null && desc.length() > 0) {
                out.append("DESC: ")
                   .append(desc)
                   .append("\n");
            }

            if (clickable) {
                out.append("CLICKABLE: true\n");
            }

            if (editable) {
                out.append("EDITABLE: true\n");
            }

            if (focusable) {
                out.append("FOCUSABLE: true\n");
            }

            if (viewId != null && !viewId.isEmpty()) {
                out.append("ID: ")
                   .append(viewId)
                   .append("\n");
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);

            if (child != null) {
                analyzeNode(child, out, depth + 1);
                child.recycle();
            }
        }
    }

    // ANALYST_ACTIONS_V1_1

    // ANALYST_ACTIONS_V1_1

    public static boolean clickText(String target) {
        if (instance == null || target == null || target.trim().isEmpty()) {
            return false;
        }

        AccessibilityNodeInfo root = instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        boolean result = clickNodeByText(root, target.trim());

        root.recycle();

        return result;
    }

    private static boolean clickNodeByText(
            AccessibilityNodeInfo node,
            String target) {

        if (node == null) {
            return false;
        }

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if (matchesTarget(text, target) ||
                matchesTarget(desc, target)) {

            if (node.isClickable()) {
                return node.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                );
            }

            AccessibilityNodeInfo parent = node.getParent();

            if (parent != null) {
                boolean clicked = parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                );

                parent.recycle();

                if (clicked) {
                    return true;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo child = node.getChild(i);

            if (child != null) {

                if (clickNodeByText(child, target)) {
                    child.recycle();
                    return true;
                }

                child.recycle();
            }
        }

        return false;
    }

    private static boolean matchesTarget(
            CharSequence value,
            String target) {

        if (value == null || target == null) {
            return false;
        }

        String a = value.toString()
                .trim()
                .toLowerCase(Locale.US);

        String b = target.trim()
                .toLowerCase(Locale.US);

        return a.equals(b) ||
                a.contains(b) ||
                b.contains(a);
    }

    public static boolean setTextByHint(
            String hint,
            String value) {

        if (instance == null ||
                value == null ||
                value.isEmpty()) {
            return false;
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo field =
                findEditable(root, hint);

        boolean result = false;

        if (field != null) {

            Bundle args = new Bundle();

            args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    value
            );

            result = field.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    args
            );

            field.recycle();
        }

        root.recycle();

        return result;
    }

    private static AccessibilityNodeInfo findEditable(
            AccessibilityNodeInfo node,
            String hint) {

        if (node == null) {
            return null;
        }

        if (node.isEditable()) {

            if (hint == null ||
                    hint.trim().isEmpty()) {
                return AccessibilityNodeInfo.obtain(node);
            }

            CharSequence text = node.getText();
            CharSequence desc = node.getContentDescription();

            if (matchesTarget(text, hint) ||
                    matchesTarget(desc, hint)) {
                return AccessibilityNodeInfo.obtain(node);
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {

            AccessibilityNodeInfo child = node.getChild(i);

            if (child != null) {

                AccessibilityNodeInfo result =
                        findEditable(child, hint);

                child.recycle();

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    // SMART_SCREEN_SEARCH_V1_1

    // SMART_SCREEN_SEARCH_V1_1

    public static boolean smartClick(String target) {
        if (instance == null ||
                target == null ||
                target.trim().isEmpty()) {
            return false;
        }

        AccessibilityNodeInfo root =
                instance.getRootInActiveWindow();

        if (root == null) {
            return false;
        }

        AccessibilityNodeInfo node =
                findBestNode(
                        root,
                        target.trim()
                );

        boolean ok = false;

        if (node != null) {
            ok = performSmartClick(node);
            node.recycle();
        }

        root.recycle();
        return ok;
    }

    private static AccessibilityNodeInfo findBestNode(
            AccessibilityNodeInfo node,
            String target) {

        if (node == null ||
                target == null) {
            return null;
        }

        AccessibilityNodeInfo best = null;
        int bestScore = 0;

        String wanted =
                target.trim().toLowerCase(Locale.US);

        String text =
                node.getText() == null
                        ? ""
                        : node.getText().toString();

        String desc =
                node.getContentDescription() == null
                        ? ""
                        : node.getContentDescription().toString();

        String id =
                node.getViewIdResourceName() == null
                        ? ""
                        : node.getViewIdResourceName();

        text = text.toLowerCase(Locale.US);
        desc = desc.toLowerCase(Locale.US);
        id = id.toLowerCase(Locale.US);

        if (node.isVisibleToUser()) {

            int score = 0;

            if (text.equals(wanted)) {
                score += 100;
            } else if (text.contains(wanted)) {
                score += 75;
            }

            if (desc.equals(wanted)) {
                score += 95;
            } else if (desc.contains(wanted)) {
                score += 70;
            }

            if (id.equals(wanted)) {
                score += 90;
            } else if (id.contains(wanted)) {
                score += 60;
            }

            if (node.isClickable()) {
                score += 20;
            }

            if (score > bestScore) {
                bestScore = score;
                best =
                        AccessibilityNodeInfo.obtain(node);
            }
        }

        for (int i = 0;
                i < node.getChildCount();
                i++) {

            AccessibilityNodeInfo child =
                    node.getChild(i);

            if (child == null) {
                continue;
            }

            AccessibilityNodeInfo result =
                    findBestNode(
                            child,
                            target
                    );

            child.recycle();

            if (result != null) {

                int score =
                        scoreNode(
                                result,
                                target
                        );

                if (score > bestScore) {

                    if (best != null) {
                        best.recycle();
                    }

                    bestScore = score;
                    best = result;

                } else {
                    result.recycle();
                }
            }
        }

        return best;
    }

    private static int scoreNode(
            AccessibilityNodeInfo node,
            String target) {

        if (node == null ||
                target == null) {
            return 0;
        }

        String wanted =
                target.toLowerCase(Locale.US);

        String text =
                node.getText() == null
                        ? ""
                        : node.getText().toString()
                                .toLowerCase(Locale.US);

        String desc =
                node.getContentDescription() == null
                        ? ""
                        : node.getContentDescription().toString()
                                .toLowerCase(Locale.US);

        String id =
                node.getViewIdResourceName() == null
                        ? ""
                        : node.getViewIdResourceName()
                                .toLowerCase(Locale.US);

        int score = 0;

        if (text.equals(wanted)) {
            score += 100;
        } else if (text.contains(wanted)) {
            score += 75;
        }

        if (desc.equals(wanted)) {
            score += 95;
        } else if (desc.contains(wanted)) {
            score += 70;
        }

        if (id.equals(wanted)) {
            score += 90;
        } else if (id.contains(wanted)) {
            score += 60;
        }

        if (node.isClickable()) {
            score += 20;
        }

        return score;
    }





    private static boolean performSmartClick(
            AccessibilityNodeInfo node) {

        if (node.isClickable()) {
            return node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
            );
        }

        AccessibilityNodeInfo parent = node.getParent();

        if (parent != null) {

            boolean result = false;

            if (parent.isClickable()) {
                result = parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                );
            }

            parent.recycle();

            if (result) {
                return true;
            }
        }

        return false;
    }

}
