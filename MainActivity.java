package com.ontik.screenai;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.media.AudioManager;
import android.provider.Settings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    WebView ontikWebView;

    private String lastCommand = null;
    private boolean cancelRequested = false;
    private final ArrayList<String> commandHistory =
            new ArrayList<>();

    private SharedPreferences memoryPrefs;
    private boolean repeatingCommand = false;
    private String pendingSafetyCommand = null;
    private boolean safetyConfirmedExecution = false;


    static final int COLOR_BG_TOP     = Color.rgb(7, 10, 18);
    static final int COLOR_BG_BOTTOM  = Color.rgb(2, 3, 6);
    static final int COLOR_CARD       = Color.rgb(12, 18, 27);
    static final int COLOR_CARD_EDGE  = Color.rgb(26, 36, 50);
    static final int COLOR_CYAN       = Color.rgb(0, 229, 255);
    static final int COLOR_CYAN_DEEP  = Color.rgb(0, 140, 220);
    static final int COLOR_DIM        = Color.rgb(80, 100, 125);
    static final int COLOR_GREEN      = Color.rgb(0, 224, 130);
    static final int COLOR_RED        = Color.rgb(255, 82, 82);

    LinearLayout layout;
    TextView status;
    EditText command;
    Button run;
    Button voice;
    TextView activity;

    int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle b) {
        memoryPrefs =
                getSharedPreferences(
                        "ontik_memory",
                        MODE_PRIVATE
                );

        loadCommandHistory();

        super.onCreate(b);

        showPremiumLoading();

        new android.os.Handler(
                android.os.Looper.getMainLooper()
        ).postDelayed(() -> {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        1001
                );
            }
            buildUI();
        }, 1800);
    }

    void showPremiumLoading() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(5, 7, 14));

        TextView logo = new TextView(this);
        logo.setText("『ONTIK』");
        logo.setTextColor(COLOR_CYAN);
        logo.setTextSize(34);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView sub = new TextView(this);
        sub.setText("SCREEN AI");
        sub.setTextColor(Color.LTGRAY);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progress.setMax(100);
        progress.setProgress(0);

        LinearLayout.LayoutParams barParams =
                new LinearLayout.LayoutParams(
                        dp(190),
                        dp(4)
                );
        barParams.topMargin = dp(28);

        TextView loading = new TextView(this);
        loading.setText("INITIALIZING AI...");
        loading.setTextColor(Color.GRAY);
        loading.setTextSize(11);
        loading.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        -2,
                        -2
                );
        textParams.topMargin = dp(14);

        root.addView(logo);
        root.addView(sub);
        root.addView(progress, barParams);
        root.addView(loading, textParams);

        setContentView(root);

        logo.setScaleX(0.75f);
        logo.setScaleY(0.75f);
        logo.setAlpha(0f);

        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setInterpolator(
                        new OvershootInterpolator(1.2f)
                )
                .start();

        ObjectAnimator pulse =
                ObjectAnimator.ofFloat(
                        logo,
                        "alpha",
                        0.55f,
                        1f
                );

        pulse.setDuration(700);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();

        ValueAnimator bar =
                ValueAnimator.ofInt(0, 100);

        bar.setDuration(1600);

        bar.addUpdateListener(animation -> {
            progress.setProgress(
                    (Integer) animation.getAnimatedValue()
            );
        });

        bar.start();

        ObjectAnimator loadingPulse =
                ObjectAnimator.ofFloat(
                        loading,
                        "alpha",
                        0.4f,
                        1f
                );

        loadingPulse.setDuration(700);
        loadingPulse.setRepeatMode(ValueAnimator.REVERSE);
        loadingPulse.setRepeatCount(ValueAnimator.INFINITE);
        loadingPulse.start();
    }

    GradientDrawable roundedDrawable(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    GradientDrawable roundedStrokeDrawable(int color, int strokeColor, float radiusDp, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(strokeDp), strokeColor);
        return d;
    }

    GradientDrawable accentGradient(float radiusDp) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{COLOR_CYAN, COLOR_CYAN_DEEP}
        );
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    Drawable withRipple(Drawable base, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), base, base);
    }

    void applyPressFeedback(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new OvershootInterpolator(1.4f)).start();
                    break;
            }
            return false;
        });
    }

    void fadeSlideIn(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(22));
        v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(480).setInterpolator(new DecelerateInterpolator(1.4f)).start();
    }

    void setTextAnimated(TextView tv, String text) {
        tv.animate().alpha(0f).setDuration(140).withEndAction(() -> {
            tv.setText(text);
            tv.animate().alpha(1f).setDuration(220).start();
        }).start();
    }

    void startPulse(View v) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.35f);
        alpha.setDuration(700);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.REVERSE);
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.start();
        v.setTag(alpha);
    }

    void stopPulse(View v) {
        Object tag = v.getTag();
        if (tag instanceof ObjectAnimator) ((ObjectAnimator) tag).cancel();
        v.setAlpha(1f);
    }

    void startBreathing(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.04f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.04f);
        scaleX.setDuration(1600);
        scaleY.setDuration(1600);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();
        v.setTag(set);
    }

    public class WebBridge {

        @JavascriptInterface
        public void executeCommand(String command) {
            MainActivity.this.runOnUiThread(() ->
                    MainActivity.this.executeCommand(command)
            );
        }

        @JavascriptInterface
        public boolean isAccessibilityEnabled() {
            return ONTIKAccessibilityService.isRunning();
        }

        @JavascriptInterface
        public void openAccessibilitySettings() {
            try {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                showOutput("Unable to open Accessibility Settings");
            }
        }

        @JavascriptInterface
        public void startVoiceCommand() {
            MainActivity.this.runOnUiThread(() ->
                    MainActivity.this.startVoice()
            );
        }

        @JavascriptInterface
        public void cancelMultiStep() {
            MainActivity.this.runOnUiThread(() -> {
                cancelRequested = true;
                pendingSafetyCommand = null;
                safetyConfirmedExecution = false;

                showOutput("🛑 Multi-Step task cancelled.");

                if (ontikWebView != null) {
                    ontikWebView.evaluateJavascript(
                            "setMultiStepRunning(false)",
                            null
                    );
                }
            });
        }
    }

    void buildUI() {
        WebView web = new WebView(this);
        ontikWebView = web;

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        web.setBackgroundColor(Color.BLACK);
        web.addJavascriptInterface(new WebBridge(), "ONTIK");

        web.loadUrl("file:///android_asset/index.html");

        setContentView(web);
    }

    void updateStatus() {
        if (status == null) return;
        if (ONTIKAccessibilityService.isRunning()) {
            status.setText("🟢 Accessibility Service: ON");
            status.setTextColor(COLOR_GREEN);
        } else {
            status.setText("🔴 Accessibility Service: OFF");
            status.setTextColor(COLOR_RED);
        }
    }


    private void saveCommandToHistory(
            String command) {

        if (memoryPrefs == null ||
                command == null ||
                command.trim().isEmpty()) {
            return;
        }

        commandHistory.add(command.trim());

        while (commandHistory.size() > 30) {
            commandHistory.remove(0);
        }

        StringBuilder data =
                new StringBuilder();

        for (String item : commandHistory) {
            if (data.length() > 0) {
                data.append("\n");
            }

            data.append(
                    item.replace("\n", " ")
            );
        }

        memoryPrefs.edit()
                .putString(
                        "history",
                        data.toString()
                )
                .apply();
    }

    private void loadCommandHistory() {

        if (memoryPrefs == null) {
            return;
        }

        String data =
                memoryPrefs.getString(
                        "history",
                        ""
                );

        if (data == null ||
                data.trim().isEmpty()) {
            return;
        }

        commandHistory.clear();

        String[] items =
                data.split("\\n");

        for (String item : items) {

            if (item != null &&
                    !item.trim().isEmpty()) {

                commandHistory.add(
                        item.trim()
                );
            }
        }

        if (!commandHistory.isEmpty()) {
            lastCommand =
                    commandHistory.get(
                            commandHistory.size() - 1
                    );
        }
    }

    private String getLastCommand() {

        if (lastCommand != null &&
                !lastCommand.trim().isEmpty()) {
            return lastCommand;
        }

        if (!commandHistory.isEmpty()) {
            return commandHistory.get(
                    commandHistory.size() - 1
            );
        }

        return null;
    }

    private String getCommandHistoryText() {

        if (commandHistory.isEmpty()) {
            return "📜 No command history.";
        }

        StringBuilder out =
                new StringBuilder();

        int start =
                Math.max(
                        0,
                        commandHistory.size() - 10
                );

        for (int i = start;
                i < commandHistory.size();
                i++) {

            out.append(i + 1)
               .append(". ")
               .append(commandHistory.get(i))
               .append("\n");
        }

        return out.toString().trim();
    }

    private boolean isRiskyCommand(String command) {
        if (command == null) {
            return false;
        }

        String x =
                command.toLowerCase(Locale.US);

        return x.contains("delete") ||
               x.contains("remove") ||
               x.contains("send") ||
               x.contains("send message") ||
               x.contains("send email") ||
               x.contains("post") ||
               x.contains("publish") ||
               x.contains("buy") ||
               x.contains("purchase") ||
               x.contains("pay") ||
               x.contains("checkout") ||
               x.contains("transfer") ||
               x.contains("withdraw") ||
               x.contains("মুছে") ||
               x.contains("ডিলিট") ||
               x.contains("পাঠাও") ||
               x.contains("পাঠিয়ে") ||
               x.contains("পোস্ট") ||
               x.contains("কিন") ||
               x.contains("পেমেন্ট") ||
               x.contains("উইথড্র");
    }

    void executeCommand(String text) {
        if (text == null) return;

        text = text.trim();

        if (text.isEmpty()) {
            showOutput("⌨️ Write a command first.");
            return;
        }

        String lower =
                text.toLowerCase(Locale.US).trim();

        // Command History
        if (lower.equals("history") ||
                lower.equals("command history") ||
                lower.equals("commands") ||
                lower.contains("হিস্টোরি") ||
                lower.contains("কমান্ড হিস্টোরি")) {

            showOutput(
                    getCommandHistoryText()
            );

            return;
        }

        // Repeat last command
        if (lower.equals("repeat") ||
                lower.equals("repeat last") ||
                lower.equals("again") ||
                lower.contains("আবার কর") ||
                lower.contains("শেষ কমান্ড")) {

            String remembered =
                    getLastCommand();

            if (remembered == null ||
                    remembered.trim().isEmpty()) {

                showOutput(
                        "ℹ️ No previous command."
                );

                return;
            }

            showOutput(
                    "🔁 Repeating: " +
                    remembered
            );

            repeatingCommand = true;

            try {
                executeCommand(remembered);
            } finally {
                repeatingCommand = false;
            }

            return;
        }

        // Save normal commands to memory
        if (!repeatingCommand) {
            lastCommand = text;
            saveCommandToHistory(text);
        }

        if (text.contains(",") ||
                text.matches("(?i).*\\s+then\\s+.*") ||
                text.contains(" তারপর ") ||
                text.contains(" এরপর ")) {

            executeMultiStep(text);
            return;
        }

        if (ONTIKAccessibilityService.isRunning()) {
            AnalystIntent analystIntent =
                    analyzeCommand(text);

            if (executeAnalystIntent(analystIntent)) {
                return;
            }
        }

        if (text.isEmpty()) {
            showOutput("⌨️ Write a command first.");
            return;
        }

        if (!ONTIKAccessibilityService.isRunning()) {
            showOutput("⚠️ Enable ONTIK Screen AI in Accessibility Settings first.");
            return;
        }

        lower = text.toLowerCase(Locale.US).trim();

        // CANCEL
        if (lower.equals("cancel") ||
                lower.equals("stop") ||
                lower.equals("abort") ||
                lower.equals("no") ||
                lower.equals("না") ||
                lower.equals("বাতিল")) {

            cancelRequested = true;
            pendingSafetyCommand = null;
            safetyConfirmedExecution = false;

            showOutput(
                    "🛑 CANCELLED"
            );

            return;
        }

        // SAFETY CONFIRMATION
        if (!safetyConfirmedExecution &&
                isRiskyCommand(lower)) {

            pendingSafetyCommand = text;

            showOutput(
                    "⚠️ CONFIRMATION REQUIRED\n" +
                    "This action may send, post, delete or purchase.\n" +
                    "Type CONFIRM to continue or CANCEL to stop."
            );

            return;
        }

        // CONFIRM
        if (lower.equals("confirm") ||
                lower.equals("yes") ||
                lower.equals("হ্যাঁ") ||
                lower.equals("ঠিক আছে") ||
                lower.equals("চালাও")) {

            if (pendingSafetyCommand == null ||
                    pendingSafetyCommand.trim().isEmpty()) {

                showOutput(
                        "ℹ️ Nothing is waiting for confirmation."
                );

                return;
            }

            String confirmed =
                    pendingSafetyCommand;

            pendingSafetyCommand = null;
            safetyConfirmedExecution = true;

            try {
                executeCommand(confirmed);
            } finally {
                safetyConfirmedExecution = false;
            }

            return;
        }


        // HOME
        if (lower.equals("home") ||
                lower.equals("go home") ||
                lower.contains("go to home") ||
                lower.contains("open home") ||
                lower.contains("হোম")) {

            ONTIKAccessibilityService.home();
            showOutput("🏠 Home");
            return;
        }

        // BACK
        if (lower.equals("back") ||
                lower.equals("go back") ||
                lower.contains("back যাও") ||
                lower.contains("পিছনে")) {

            ONTIKAccessibilityService.back();
            showOutput("↩️ Back");
            return;
        }

        // SCROLL DOWN
        if (lower.contains("scroll down") ||
                lower.contains("swipe down") ||
                lower.contains("scroll নিচে") ||
                lower.contains("নিচে scroll") ||
                lower.contains("নিচে স্ক্রল")) {

            ONTIKAccessibilityService.scrollDown();
            showOutput("⬇️ Scrolling down");
            return;
        }

        // SCROLL UP
        if (lower.contains("scroll up") ||
                lower.contains("swipe up") ||
                lower.contains("scroll উপরে") ||
                lower.contains("উপরে scroll") ||
                lower.contains("উপরে স্ক্রল")) {

            ONTIKAccessibilityService.scrollUp();
            showOutput("⬆️ Scrolling up");
            return;
        }

        // ALARM
        if (lower.contains("alarm") ||
                lower.contains("set alarm") ||
                lower.contains("wake me") ||
                lower.contains("অ্যালার্ম")) {

            String time = extractAlarmTime(text);

            if (time != null) {
                setAlarm(time);
            } else {
                showOutput("⏰ Example: Set alarm 6:00 AM");
            }
            return;
        }

        // OPEN + SEARCH
        ParsedCommand parsed = parseOpenSearch(text);

        if (parsed.app != null && !parsed.app.trim().isEmpty()) {
            openAppByName(parsed.app, parsed.search);
            return;
        }

        // NATURAL APP COMMANDS
        String command = text;

        command = command.replaceAll(
                "(?i)\\b(please|pls|kindly|can you|could you)\\b",
                " "
        );

        command = command.replaceAll(
                "(?i)\\b(open|launch|start|run|চালু করো|চালু কর|খুলে দাও|খুলো|খুলুন|চালাও)\\b",
                " "
        );

        command = command.replaceAll(
                "(?i)\\b(app|application|অ্যাপ)\\b",
                " "
        );

        command = command.replaceAll(
                "(?i)\\s+(এ|তে|এতে|কে|টা|টি|দাও|দে|করো|কর|চাই)\\s*$",
                " "
        );

        command = command.trim();

        if (!command.isEmpty() &&
                command.length() >= 2 &&
                !command.contains("search")) {

            openAppByName(command, null);
            return;
        }

        showOutput("🤖 I couldn't understand that command.");
    }

    String extractAlarmTime(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)?",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            String minuteText = matcher.group(2);
            int minute = minuteText == null ? 0 : Integer.parseInt(minuteText);
            String ampm = matcher.group(3);
            if (hour >= 1 && hour <= 12 && minute >= 0 && minute <= 59) {
                if (ampm != null) {
                    ampm = ampm.replace(".", "").toLowerCase(Locale.US);
                    if (ampm.equals("pm") && hour < 12) hour += 12;
                    if (ampm.equals("am") && hour == 12) hour = 0;
                }
                return String.format(Locale.US, "%02d:%02d", hour, minute);
            }
        }
        return null;
    }

    void setAlarm(String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour);
            intent.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute);
            intent.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "ONTIK Screen AI");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                showOutput("⏰ Opening alarm for " + time);
                return;
            }
            String[] clockPackages = {"com.google.android.deskclock", "com.android.deskclock", "com.android.clock"};
            PackageManager pm = getPackageManager();
            for (String pkg : clockPackages) {
                Intent clock = pm.getLaunchIntentForPackage(pkg);
                if (clock != null) {
                    clock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(clock);
                    showOutput("⏰ Clock opened. Please set alarm for " + time);
                    return;
                }
            }
            showOutput("❌ No compatible Clock app found.");
        } catch (Exception e) {
            showOutput("❌ Alarm error: " + e.getClass().getSimpleName());
        }
    }

    ParsedCommand parseOpenSearch(String text) {
        if (text == null) return new ParsedCommand(null, null);

        String original = text.trim();
        String lower = original.toLowerCase(Locale.US);

        String[] searchPatterns = {
                " and search ",
                " then search ",
                " search for ",
                " search ",
                " এ search ",
                " এ খুঁজে ",
                " এ খোজ ",
                " এ সার্চ ",
                "তে search ",
                "তে সার্চ "
        };

        for (String pattern : searchPatterns) {
            int pos = lower.indexOf(pattern);

            if (pos > 0) {
                String appPart = original.substring(0, pos).trim();
                String searchPart =
                        original.substring(pos + pattern.length()).trim();

                appPart = appPart.replaceAll(
                        "(?i)^(open|launch|start|run|চালু কর|খুলে দাও|খুলো)\\s+",
                        ""
                ).trim();

                if (!appPart.isEmpty() && !searchPart.isEmpty()) {
                    return new ParsedCommand(appPart, searchPart);
                }
            }
        }

        String[] openWords = {
                "open ",
                "launch ",
                "start ",
                "run ",
                "খুলে দাও ",
                "খুলো ",
                "চালাও "
        };

        for (String prefix : openWords) {
            if (lower.startsWith(prefix)) {
                String app = original.substring(prefix.length()).trim();

                if (!app.isEmpty()) {
                    return new ParsedCommand(app, null);
                }
            }
        }

        return new ParsedCommand(null, null);
    }

    String normalizeAppName(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
    }

    int levenshtein(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[a.length()][b.length()];
    }

    double appMatchScore(String wanted, String label) {
        String a = normalizeAppName(wanted);
        String b = normalizeAppName(label);
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        if (a.equals(b)) return 1.0;
        if (b.contains(a) || a.contains(b)) return 0.92;
        int distance = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 0.0 : 1.0 - ((double) distance / max);
    }

    void openAppByName(String name, String search) {
        PackageManager pm = getPackageManager();

        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);

        ArrayList<ResolveInfo> apps =
                new ArrayList<>(pm.queryIntentActivities(launcher, 0));

        ResolveInfo bestMatch = null;
        String bestLabel = "";
        String bestPackage = "";
        double bestScore = 0.0;

        String wanted = normalizeAppName(name);

        for (ResolveInfo info : apps) {
            String label = info.loadLabel(pm).toString();
            String pkg = info.activityInfo.packageName;

            String normalizedLabel = normalizeAppName(label);
            String normalizedPkg = normalizeAppName(pkg);

            double score = appMatchScore(wanted, label);

            if (normalizedLabel.equals(wanted)) {
                score = 1.0;
            }

            if (normalizedLabel.contains(wanted) && wanted.length() >= 3) {
                score = Math.max(score, 0.90);
            }

            if (normalizedPkg.contains(wanted) && wanted.length() >= 3) {
                score = Math.max(score, 0.88);
            }

            if (wanted.equals("youtube") &&
                    (normalizedLabel.contains("youtube") ||
                     normalizedPkg.contains("youtube"))) {
                score = 1.0;
            }

            if (wanted.equals("yt") &&
                    (normalizedLabel.contains("youtube") ||
                     normalizedPkg.contains("youtube"))) {
                score = 1.0;
            }

            if (wanted.equals("chrome") &&
                    normalizedLabel.contains("chrome")) {
                score = Math.max(score, 0.98);
            }

            if (wanted.equals("whatsapp") &&
                    (normalizedLabel.contains("whatsapp") ||
                     normalizedPkg.contains("whatsapp"))) {
                score = 1.0;
            }

            if (wanted.equals("facebook") &&
                    (normalizedLabel.contains("facebook") ||
                     normalizedPkg.contains("facebook"))) {
                score = 1.0;
            }

            if (wanted.equals("messenger") &&
                    (normalizedLabel.contains("messenger") ||
                     normalizedPkg.contains("messenger"))) {
                score = 1.0;
            }

            if (wanted.equals("instagram") &&
                    (normalizedLabel.contains("instagram") ||
                     normalizedPkg.contains("instagram"))) {
                score = 1.0;
            }

            if (wanted.equals("gallery") &&
                    (normalizedLabel.contains("gallery") ||
                     normalizedLabel.contains("photos") ||
                     normalizedPkg.contains("gallery") ||
                     normalizedPkg.contains("photos"))) {
                score = Math.max(score, 0.95);
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = info;
                bestLabel = label;
                bestPackage = pkg;
            }
        }

        if (bestMatch == null || bestScore < 0.50) {
            showOutput("❌ App not found: " + name);
            return;
        }

        Intent intent = pm.getLaunchIntentForPackage(bestPackage);

        if (intent == null) {
            showOutput("❌ Unable to launch: " + bestLabel);
            return;
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        );

        try {
            startActivity(intent);

            showOutput("🚀 Opening " + bestLabel);

            if (search != null && !search.isEmpty()) {
                android.os.Handler h =
                        new android.os.Handler(
                                android.os.Looper.getMainLooper()
                        );

                h.postDelayed(() -> {
                    ONTIKAccessibilityService.searchCurrentApp(search);
                }, 2500);
            }

        } catch (Exception e) {
            showOutput("❌ Unable to open: " + bestLabel);
        }
    }

    void performSearch(String text) {
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.postDelayed(() -> {
            ONTIKAccessibilityService.clickFirstMatching("Search", "Search YouTube", "Search videos", "Search");
            h.postDelayed(() -> {
                boolean typed = ONTIKAccessibilityService.setText(text);
                if (!typed) {
                    ONTIKAccessibilityService.clickFirstMatching("Search", "Search YouTube", "Search videos");
                    h.postDelayed(() -> ONTIKAccessibilityService.setText(text), 800);
                }
                h.postDelayed(() -> ONTIKAccessibilityService.clickFirstMatching("Search", "Go", "Enter", "ic_search"), 1200);
            }, 1100);
        }, 2300);
    }

    void startVoice() {
        try {
            Intent i = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            i.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            i.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "bn-BD"
            );

            i.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    "bn-BD"
            );

            i.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak Bangla, English or Banglish"
            );

            startActivityForResult(i, 101);

        } catch (Exception e) {
            stopPulse(voice);

            showOutput(
                    "🎙️ Voice recognition is not available."
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            stopPulse(voice);
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String spoken = result.get(0);

                    showOutput(
                            "🎙️ VOICE RECEIVED: " +
                            spoken
                    );

                    showOutput(
                            "🧠 UNDERSTANDING..."
                    );

                    if (ontikWebView != null) {
                        ontikWebView.evaluateJavascript(
                            "setVoiceCommand(" +
                            org.json.JSONObject.quote(spoken) +
                            ")",
                            null
                        );
                    } else {
                        executeCommand(spoken);
                    }
                }
            }
        }
    }

    void showOutput(String text) {
        if (text == null) {
            text = "";
        }

        final String message = text;

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();

        if (ontikWebView != null) {
            runOnUiThread(() -> {
                try {
                    String js =
                            "if(window.setAIActivityStatus){" +
                            "setAIActivityStatus(" +
                            org.json.JSONObject.quote(message) +
                            ");}";

                    ontikWebView.evaluateJavascript(
                            js,
                            null
                    );
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void hideMultiStepCancel() {
        if (ontikWebView != null) {
            ontikWebView.evaluateJavascript(
                    "setMultiStepRunning(false)",
                    null
            );
        }
    }

    void executeMultiStep(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String[] steps = text.trim().split(
                "(?i)\\s*(?:,|\\bthen\\b|\\bnext\\b|\\bafter that\\b|তারপর|এরপর)\\s*"
        );

        if (steps.length < 2) {
            executeCommand(text.trim());
            return;
        }

        cancelRequested = false;

        if (ontikWebView != null) {
            ontikWebView.evaluateJavascript(
                    "setMultiStepRunning(true)",
                    null
            );
        }

        showOutput(
                "🧠 AI Analyst: " +
                steps.length +
                " steps detected."
        );

        executeStepSequence(steps, 0);
    }

    void executeStepSequence(
            String[] steps,
            int index) {

        if (cancelRequested) {
            hideMultiStepCancel();
            showOutput("🛑 Multi-Step task cancelled.");
            return;
        }

        if (steps == null || index >= steps.length) {
            hideMultiStepCancel();
            showOutput("✓ Multi-Step task completed.");
            return;
        }

        String step = steps[index].trim();

        if (step.isEmpty()) {
            executeStepSequence(steps, index + 1);
            return;
        }

        showOutput(
                "⚡ STEP " +
                (index + 1) +
                "/" +
                steps.length +
                "\n› " +
                step
        );

        executeStepWithRetry(
                steps,
                index,
                step,
                0
        );
    }

    void executeStepWithRetry(
            String[] steps,
            int index,
            String step,
            int attempt) {

        if (cancelRequested) {
            hideMultiStepCancel();
            showOutput("❌ Multi-Step task cancelled.");
            return;
        }

        if (attempt >= 3) {
            hideMultiStepCancel();
            showOutput(
                    "❌ STEP " +
                    (index + 1) +
                    " failed after retries."
            );
            return;
        }

        showOutput(
                "🤖 Executing STEP " +
                (index + 1) +
                " • Attempt " +
                (attempt + 1)
        );

        String before =
                ONTIKAccessibilityService.getScreenSignature();

        executeCommand(step);

        new android.os.Handler(
                android.os.Looper.getMainLooper()
        ).postDelayed(() -> {

            if (cancelRequested) {
                hideMultiStepCancel();
                showOutput("🛑 Multi-Step task cancelled.");
                return;
            }

            if (!ONTIKAccessibilityService.isRunning()) {
                showOutput("❌ Accessibility Service stopped.");
                return;
            }

            String after =
                    ONTIKAccessibilityService.getScreenSignature();

            if (!before.equals(after)) {

                showOutput(
                        "✓ STEP " +
                        (index + 1) +
                        " verified."
                );

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() ->
                        executeStepSequence(
                                steps,
                                index + 1
                        ),
                        700
                );

            } else {

                showOutput(
                        "⚠️ STEP " +
                        (index + 1) +
                        " not verified. Retrying..."
                );

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() ->
                        executeStepWithRetry(
                                steps,
                                index,
                                step,
                                attempt + 1
                        ),
                        900
                );
            }

        }, 1200);
    }

    static class ParsedCommand {
        String app;
        String search;
        ParsedCommand(String a, String s) {
            app = a;
            search = s;
        }
    }
    // AI_ANALYST_PARSER_V1_1

    // AI_ANALYST_PARSER_V1_1

    static class AnalystIntent {
        String type;
        String target;
        String value;

        AnalystIntent(String type, String target, String value) {
            this.type = type;
            this.target = target;
            this.value = value;
        }
    }

    AnalystIntent analyzeCommand(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new AnalystIntent("UNKNOWN", null, null);
        }

        String original = text.trim();
        String lower = original.toLowerCase(Locale.US);

        // SCREEN ANALYSIS
        if (lower.contains("analyze screen") ||
                lower.contains("analyse screen") ||
                lower.contains("screen analysis") ||
                lower.contains("screen analyze") ||
                lower.contains("স্ক্রিন দেখ") ||
                lower.contains("স্ক্রিন বিশ্লেষণ") ||
                lower.contains("স্ক্রিন এনালাইস")) {
            return new AnalystIntent("ANALYZE", null, null);
        }

        // SEARCH
        if (lower.contains("search ") ||
                lower.contains("search for ") ||
                lower.contains("সার্চ ") ||
                lower.contains("খুঁজে ") ||
                lower.contains("খোজ ")) {

            String value = original
                    .replaceFirst("(?i).*?(search for|search|সার্চ|খুঁজে|খোজ)\\s*", "")
                    .trim();

            if (!value.isEmpty()) {
                return new AnalystIntent("SEARCH", null, value);
            }
        }

        // CLICK / TAP
        if (lower.contains("click ") ||
                lower.contains("tap ") ||
                lower.contains("press ") ||
                lower.contains("touch ") ||
                lower.contains("ক্লিক ") ||
                lower.contains("চাপো ") ||
                lower.contains("চাপ ")) {

            String target = original
                    .replaceFirst(
                            "(?i).*?(click|tap|press|touch|ক্লিক|চাপো|চাপ)\\s*",
                            ""
                    )
                    .trim();

            if (!target.isEmpty()) {
                return new AnalystIntent("CLICK", target, null);
            }
        }

        // TYPE / WRITE
        if (lower.contains("type ") ||
                lower.contains("write ") ||
                lower.contains("enter ") ||
                lower.contains("লিখে ") ||
                lower.contains("লিখ ") ||
                lower.contains("টাইপ ")) {

            String value = original
                    .replaceFirst(
                            "(?i).*?(type|write|enter|লিখে|লিখ|টাইপ)\\s*",
                            ""
                    )
                    .trim();

            if (!value.isEmpty()) {
                return new AnalystIntent("TYPE", null, value);
            }
        }

        return new AnalystIntent("UNKNOWN", null, null);
    }

    boolean executeAnalystIntent(AnalystIntent intent) {
        if (intent == null) {
            return false;
        }

        if ("CLICK".equals(intent.type)) {
            showOutput(
                    "👆 CLICKING: " +
                    intent.target
            );

            boolean ok =
                    ONTIKAccessibilityService.smartClick(
                            intent.target
                    );

            if (ok) {
                showOutput(
                        "🔍 VERIFYING CLICK..."
                );

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() -> {
                    showOutput(
                            "✅ CLICK COMPLETED: " +
                            intent.target
                    );
                }, 500);

            } else {
                showOutput(
                        "⚠️ CLICK FAILED — RETRYING..."
                );

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() -> {

                    boolean retry =
                            ONTIKAccessibilityService.smartClick(
                                    intent.target
                            );

                    showOutput(
                            retry
                                    ? "✅ CLICK VERIFIED: " +
                                      intent.target
                                    : "❌ CLICK FAILED: " +
                                      intent.target
                    );

                }, 900);
            }

            return true;
        }

        if ("TYPE".equals(intent.type)) {
            showOutput(
                    "⌨️ TYPING..."
            );

            boolean ok =
                    ONTIKAccessibilityService.setTextByHint(
                            null,
                            intent.value
                    );

            if (ok) {
                showOutput(
                        "🔍 VERIFYING TEXT INPUT..."
                );

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() -> {
                    showOutput(
                            "✅ TEXT INPUT COMPLETED"
                    );
                }, 500);

            } else {
                showOutput(
                        "⚠️ TEXT INPUT FAILED — RETRYING..."
                );

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() -> {

                    boolean retry =
                            ONTIKAccessibilityService.setTextByHint(
                                    null,
                                    intent.value
                            );

                    showOutput(
                            retry
                                    ? "✅ TEXT INPUT VERIFIED"
                                    : "❌ NO EDITABLE FIELD FOUND"
                    );

                }, 900);
            }

            return true;
        }

        if ("SEARCH".equals(intent.type)) {
            showOutput(
                    "🔎 SEARCHING: " +
                    intent.value
            );

            ONTIKAccessibilityService.searchCurrentApp(
                    intent.value
            );

            showOutput(
                    "🔍 VERIFYING SEARCH..."
            );

            return true;
        }

        if ("PLAY_VIDEO".equals(intent.type)) {
            showOutput("▶️ PLAYING FIRST VIDEO...");

            boolean ok =
                    ONTIKAccessibilityService.playFirstVideo();

            if (ok) {
                showOutput("🔍 VERIFYING VIDEO...");

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() ->
                        showOutput("✅ VIDEO PLAY STARTED"),
                        900
                );

            } else {
                showOutput(
                        "⚠️ VIDEO NOT FOUND — RETRYING..."
                );

                new android.os.Handler(
                        android.os.Looper.getMainLooper()
                ).postDelayed(() -> {

                    boolean retry =
                            ONTIKAccessibilityService.playFirstVideo();

                    showOutput(
                            retry
                                    ? "✅ VIDEO PLAY VERIFIED"
                                    : "❌ VIDEO NOT FOUND"
                    );

                }, 1200);
            }

            return true;
        }

        if ("ANALYZE".equals(intent.type)) {
            showOutput(
                    "🧠 ANALYZING SCREEN..."
            );

            String result =
                    ONTIKAccessibilityService.analyzeCurrentScreen();

            showOutput(result);

            return true;
        }

        return false;
    }


}
