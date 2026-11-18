package com.leskur.blanklauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.net.Uri;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import org.json.JSONArray;
import org.json.JSONException;
import android.view.*;
import android.widget.*;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(235, 235, 235);
    private final int muted = Color.rgb(160, 160, 160);
    private final int accent = Color.rgb(240, 240, 240);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<App> apps = new ArrayList<>();
    private LinearLayout content, grid;
    private TextView time, date, resultCount;
    private int sideMargin;
    private int gridRevision;
    private boolean appsLoaded, suppressConfirmUp;
    private EditText search;

    private ScrollView scroll;
    private LinearLayout allPage;
    private int cardWidth;
    private SharedPreferences prefs;
    private View pressedTile;
    private boolean held;
    private final Runnable holdAction = () -> {
        if (pressedTile != null && pressedTile.hasFocus() && !held) {
            held = true;
            pressedTile.performLongClick();
        }
    };
    private final Runnable clock = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
            date.setText(new SimpleDateFormat("M月d日  EEEE", Locale.CHINA).format(now));
            handler.postDelayed(this, 60000 - System.currentTimeMillis() % 60000);
        }
    };
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refreshApps(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("slate", MODE_PRIVATE);
        if (!prefs.contains("pinned_order")) {
            List<String> legacy = new ArrayList<>(prefs.getStringSet("favorites", Collections.emptySet()));
            Collections.sort(legacy);
            prefs.edit().putString("pinned_order", new JSONArray(legacy).toString()).apply();
        }
        getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildLayout();


        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::returnToTop);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(changes, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(changes, filter);
    }

    private void buildLayout() {
        content = column();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(18, 18, 18));
        sideMargin = getResources().getConfiguration().screenWidthDp >= 800 ? 48 : 24;
        content.setPadding(dp(sideMargin), dp(27), dp(sideMargin), dp(27));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        time = text("", 26, ink);
        time.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        time.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(time);
        date = text("", 16, muted);
        date.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-2, -2);
        dateParams.leftMargin = dp(12);
        header.addView(date, dateParams);
        TextView settings = headerAction("桌面设置", this::showSettings);
        settings.setTextSize(16);
        settings.setContentDescription("桌面设置");
        header.addView(settings);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, dp(48));
        headerParams.bottomMargin = dp(24);
        content.addView(header, headerParams);

        FrameLayout pages = new FrameLayout(this);
        content.addView(pages, new LinearLayout.LayoutParams(-1, 0, 1));


        allPage = column();
        allPage.setPadding(0, dp(4), 0, 0);
        LinearLayout searchBar = row();
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTextColor(ink);
        search.setHintTextColor(muted);
        search.setHint("搜索应用");
        search.setContentDescription("搜索应用");
        search.setPadding(dp(14), dp(8), dp(14), dp(8));
        search.setBackground(surface(false));
        search.setOnFocusChangeListener((v, focused) -> v.setBackground(surface(focused)));
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchBar.addView(search, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(new View(this), 2, new LinearLayout.LayoutParams(0, 1, 1));
        int searchWidth = Math.min(240, (int) (getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density * 0.36f));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(dp(searchWidth), dp(48));
        searchParams.leftMargin = dp(16);
        header.addView(searchBar, 3, searchParams);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { renderGrid(); }
            public void afterTextChanged(Editable value) { }
        });
        search.setOnEditorActionListener((view, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
                focusFirst(grid);
                return true;
            }
            return false;
        });
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("应用", 24, ink);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        resultCount = text("", 14, muted);
        heading.addView(resultCount);
        heading.setPadding(dp(10), 0, dp(10), dp(10));
        allPage.addView(heading);
        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(true);
        scroll.setPadding(0, 0, 0, dp(8));
        grid = column();
        grid.setPadding(0, dp(8), 0, dp(8));
        grid.setClipChildren(false);
        scroll.addView(grid);
        LinearLayout browser = row();
        browser.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
        allPage.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));
        pages.addView(allPage, new FrameLayout.LayoutParams(-1, -1));


    }

    private void refreshApps() {
        View previousFocus = appsLoaded ? getCurrentFocus() : null;
        String focused = previousFocus != null && previousFocus.getTag() instanceof String
                ? (String) previousFocus.getTag() : null;
        int previousScroll = scroll.getScrollY();
        apps.clear();
        PackageManager pm = getPackageManager();
        Set<String> seen = new HashSet<>();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                ComponentName component = new ComponentName(pkg, info.activityInfo.name);
                apps.add(new App(pkg, info.loadLabel(pm).toString(), component));
            }
        }
        Collator collator = Collator.getInstance(Locale.CHINA);
        apps.sort((a, b) -> {
            int group = a.letter.compareTo(b.letter);
            return group != 0 ? group : collator.compare(a.label, b.label);
        });
        renderApps(focused);
        appsLoaded = true;
        if (previousFocus == null) focusFirst(grid);
        if (previousFocus != null && previousFocus.isAttachedToWindow()) {
            previousFocus.requestFocus();
        }
        View restored = focused == null ? null : grid.findViewWithTag(focused);
        int revision = gridRevision;
        if (restored != null) scroll.post(() -> {
            // Ignore restoration after a new query, Home, or another list rebuild.
            if (revision == gridRevision && restored.hasFocus()) scroll.scrollTo(0, previousScroll);
        });
    }

    private void renderApps(String focused) {
        int available = getResources().getConfiguration().screenWidthDp - sideMargin * 2;
        cardWidth = available / columns() - 20;
        renderGrid();
        LinearLayout visible = grid;
        View restore = focused == null ? null : visible.findViewWithTag(focused);
        if (restore != null) restore.requestFocus();
        else if (focused != null || getCurrentFocus() == null || visible.hasFocus()) focusFirst(visible);
    }

    private int columns() {
        int available = getResources().getConfiguration().screenWidthDp - sideMargin * 2;
        return Math.max(1, Math.min(6, available / 144));
    }

    private void renderGrid() {
        if (grid == null) return;
        gridRevision++;
        cancelTilePress();
        grid.removeAllViews();

        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<App> filtered = new ArrayList<>();
        for (App app : apps) {
            if (!app.matches(query)) continue;
            filtered.add(app);
        }
        Set<String> pinned = new HashSet<>(readOrder("pinned_order"));
        // Stable partition keeps the existing alphabetic order in both groups.
        filtered.sort((a, b) -> Boolean.compare(pinned.contains(b.pkg), pinned.contains(a.pkg)));
        fillRows(grid, filtered, columns());
        int count = filtered.size();
        resultCount.setText(query.isEmpty() ? count + " 个应用" : "找到 " + count + " 个应用");
        if (count == 0) {
            TextView empty = text("没有找到应用，试试名称或拼音首字母", 15, muted);
            empty.setPadding(dp(6), dp(20), 0, dp(20));
            grid.addView(empty);
            if (!query.isEmpty()) grid.addView(action("清除搜索", this::returnToTop));
            else {
                empty.setText("还没有可启动的应用");
                grid.addView(action("打开设置", this::showSettings));
            }
        }
        scroll.scrollTo(0, 0);
    }

    private void focusFirst(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        if (view.isFocusable()) { view.requestFocus(); return; }
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                focusFirst(child);
                if (child.hasFocus()) return;
            }
        }
    }

    private List<String> readOrder(String key) {
        List<String> values = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(key, "[]"));
            for (int i = 0; i < array.length(); i++) {
                String value = array.getString(i);
                if (!values.contains(value)) values.add(value);
            }
        } catch (JSONException ignored) { }
        return values;
    }

    private void saveOrder(String key, List<String> order) {
        prefs.edit().putString(key, new JSONArray(order).toString()).apply();
    }

    private void launchApp(App app) {
        if (open(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))) {
            List<String> recent = readOrder("recent_order");
            recent.remove(app.pkg);
            recent.add(0, app.pkg);
            saveOrder("recent_order", recent.subList(0, Math.min(48, recent.size())));
        }
    }

    private void showAppMenu(App app) {
        List<String> pinned = readOrder("pinned_order");
        boolean isPinned = pinned.contains(app.pkg);
        List<String> options = new ArrayList<>();
        options.add(isPinned ? "取消置顶" : "置顶");
        if (app.component != null) {
            options.add("应用详情");
            if (canUninstall(app)) options.add("卸载");
        }
        new AlertDialog.Builder(this).setTitle(app.label)
            .setItems(options.toArray(new String[0]), (dialog, which) -> {
                if (which == 0) {
                    if (isPinned) pinned.remove(app.pkg); else pinned.add(app.pkg);
                    saveOrder("pinned_order", pinned);
                    renderApps(app.pkg);
                } else if (which == 1) {
                    open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", app.pkg, null)));
                } else if (canUninstall(app)) {
                    // The system owns confirmation and determines whether removal is allowed.
                    open(new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.pkg, null)));
                }
            }).setNegativeButton("取消", null).show();
    }

    private boolean canUninstall(App app) {
        if (app.component == null) return false;
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(app.pkg, 0);
            // Removing updates is not uninstalling the preinstalled application itself.
            if ((info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) return false;
            UserManager users = getSystemService(UserManager.class);
            return users == null || !users.hasUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void fillRows(LinearLayout parent, List<App> entries, int columns) {
        LinearLayout line = null;
        for (int i = 0; i < entries.size(); i++) {
            if (i % columns == 0) {
                line = row();
                line.setClipChildren(false);
                parent.addView(line);
            }
            line.addView(tile(entries.get(i)));
        }
    }

    private View tile(App app) {
        LinearLayout card = column();
        card.setTag(app.pkg);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(cardWidth), dp(116));
        params.setMargins(dp(10), dp(10), dp(10), dp(10));
        card.setLayoutParams(params);
        ImageView icon = new ImageView(this);
        try { icon.setImageDrawable(getPackageManager().getActivityIcon(app.component)); }
        catch (PackageManager.NameNotFoundException e) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
        card.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));
        boolean favorite = readOrder("pinned_order").contains(app.pkg);
        AppNameView label = new AppNameView();
        label.setText((favorite ? "★ " : "") + app.label);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(12);
        card.addView(label, nameParams);
        card.setContentDescription(app.label + (favorite ? "，已置顶" : "") + "，长按打开应用菜单");
        focusable(card, true);
        View.OnFocusChangeListener cardFocus = card.getOnFocusChangeListener();
        card.setOnFocusChangeListener((view, focused) -> {
            cardFocus.onFocusChange(view, focused);
            label.setCardFocused(focused);
            if (!focused && pressedTile == view) cancelTilePress();
        });
        card.setOnClickListener(v -> launchApp(app));
        card.setOnLongClickListener(v -> {
            showAppMenu(app);
            return true;
        });
        return card;
    }

    private void showSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
        overridePendingTransition(R.anim.settings_enter, R.anim.settings_stay);
    }
    private boolean open(Intent intent) {
        try { startActivity(intent); return true; }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "此设备不支持该入口，或应用无法打开", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) suppressConfirmUp = false;
            if (event.getAction() == KeyEvent.ACTION_UP && suppressConfirmUp) {
                suppressConfirmUp = false;
                return true;
            }
            View focused = getCurrentFocus();
            if (pressedTile != null || (focused != null && focused.getTag() instanceof String)) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) {
                        pressedTile = focused;
                        held = false;
                        pressedTile.setPressed(true);
                        pressedTile.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        handler.postDelayed(holdAction, ViewConfiguration.getLongPressTimeout());
                    } else if (event.isLongPress()) {
                        holdAction.run();
                    }
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    handler.removeCallbacks(holdAction);
                    View target = pressedTile;
                    pressedTile = null;
                    if (target != null) {
                        target.setPressed(false);
                        target.animate().scaleX(target.hasFocus() ? 1.05f : 1f).scaleY(target.hasFocus() ? 1.05f : 1f).setDuration(100).start();
                        if (!held && !event.isCanceled() && target.hasFocus() && target.isAttachedToWindow()) target.performClick();
                    }
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void cancelTilePress() {
        handler.removeCallbacks(holdAction);
        if (pressedTile != null) {
            suppressConfirmUp = true;
            pressedTile.setPressed(false);
            pressedTile.animate().cancel();
            pressedTile.setScaleX(1f);
            pressedTile.setScaleY(1f);
        }
        pressedTile = null;
        held = false;
    }

    private TextView action(String label, Runnable run) {
        TextView view = text(label, 14, ink);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(18), dp(12), dp(18), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(dp(6), dp(4), dp(6), dp(4));
        view.setLayoutParams(params);
        focusable(view);
        view.setOnClickListener(v -> run.run());
        return view;
    }
    private TextView headerAction(String label, Runnable run) {
        TextView view = action(label, run);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setMinWidth(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(48));
        params.leftMargin = dp(8);
        view.setLayoutParams(params);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setOnFocusChangeListener((v, focused) -> {
            if (focused) v.setBackground(surface(true));
            else v.setBackgroundColor(Color.TRANSPARENT);
        });
        return view;
    }
    private void focusable(View view) {
        focusable(view, false);
    }
    private void focusable(View view, boolean transparentIdle) {
        view.setFocusable(true);
        view.setClickable(true);
        if (transparentIdle) view.setBackgroundColor(Color.TRANSPARENT);
        else view.setBackground(surface(false));
        view.setOnFocusChangeListener((v, focused) -> {
            if (transparentIdle && !focused) v.setBackgroundColor(Color.TRANSPARENT);
            else v.setBackground(surface(focused));
            v.animate().scaleX(focused ? 1.05f : 1f).scaleY(focused ? 1.05f : 1f).setDuration(140).start();
        });
    }
    private GradientDrawable surface(boolean focused) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(focused ? Color.rgb(48, 48, 48) : Color.rgb(33, 33, 33));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(focused ? 2 : 0), focused ? accent : Color.TRANSPARENT);
        return bg;
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setFontFeatureSettings("tnum");
        return view;
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onResume() {
        super.onResume();
        refreshApps();
        handler.removeCallbacks(clock);
        clock.run();
    }
    @Override protected void onPause() {
        cancelTilePress();
        handler.removeCallbacks(clock);
        handler.removeCallbacks(holdAction);
        pressedTile = null;
        super.onPause();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        returnToTop();
    }
    private void returnToTop() {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
        if (search.length() > 0) search.setText("");
        gridRevision++;
        focusFirst(grid);
        scroll.scrollTo(0, 0);
    }
    // API 33+ uses OnBackInvokedDispatcher above; this override serves older TVs only.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { returnToTop(); }
    @Override protected void onDestroy() {

        unregisterReceiver(changes); handler.removeCallbacks(clock); super.onDestroy();
    }
    // The launcher retains its platform theme; AppCompat is used only by TV settings.
    @android.annotation.SuppressLint("AppCompatCustomView")
    private class AppNameView extends TextView {
        private boolean cardFocused;
        private final Runnable begin = () -> {
            if (!cardFocused || !isShown() || !hasWindowFocus()) return;
            if (getPaint().measureText(getText().toString()) <= getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight()) return;
            setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
            setMarqueeRepeatLimit(-1);
            setSelected(true);
        };
        AppNameView() {
            super(MainActivity.this);
            setTextSize(16);
            setTextColor(ink);
            setSingleLine(true);
            setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
        void setCardFocused(boolean focused) {
            cardFocused = focused;
            removeCallbacks(begin);
            setSelected(false);
            setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (focused) postDelayed(begin, 1000);
        }
        @Override protected void onDetachedFromWindow() {
            removeCallbacks(begin);
            setSelected(false);
            super.onDetachedFromWindow();
        }
    }
    private static class App {
        final String pkg, label;
        final String pinyin, initials, letter;
        final ComponentName component;
        App(String pkg, String label, ComponentName component) {
            this.pkg = pkg; this.label = label; this.component = component;
            String[] keys = AppSearch.keys(label);
            pinyin = keys[0]; initials = keys[1];
            String first = initials.isEmpty() ? label : initials;
            char c = first.isEmpty() ? '#' : Character.toUpperCase(first.charAt(0));
            letter = c >= 'A' && c <= 'Z' ? String.valueOf(c) : "#";
        }
        boolean matches(String query) {
            return query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query)
                    || pinyin.contains(query) || initials.contains(query);
        }
    }
}
