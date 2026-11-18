package com.leskur.blanklauncher;

import android.os.Bundle;
import android.transition.Slide;
import android.view.Gravity;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat;
import androidx.leanback.preference.LeanbackSettingsFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class SettingsActivity extends FragmentActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(.65f);
        if (state == null) getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsHost(), "settings").commitNow();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Fragment host = getSupportFragmentManager().findFragmentByTag("settings");
                if (host != null && host.getChildFragmentManager().getBackStackEntryCount() > 0)
                    host.getChildFragmentManager().popBackStackImmediate();
                else finish();
            }
        });
    }
    @Override public void finish() {
        super.finish();
        overridePendingTransition(R.anim.settings_stay, R.anim.settings_exit);
    }
    public static class SettingsHost extends LeanbackSettingsFragmentCompat {
        @Override public void onPreferenceStartInitialScreen() { showPage(null); }
        @Override public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference preference) { return false; }
        @Override public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen screen) {
            showPage(screen.getKey()); return true;
        }
        private void showPage(String key) {
            Preferences page = new Preferences();
            Bundle args = new Bundle();
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
            page.setArguments(args);
            page.setEnterTransition(new Slide(Gravity.END).setDuration(220));
            page.setExitTransition(new Slide(Gravity.START).setDuration(220));
            page.setReenterTransition(new Slide(Gravity.START).setDuration(220));
            page.setReturnTransition(new Slide(Gravity.END).setDuration(220));
            startPreferenceFragment(page);
        }
    }
    public static class Preferences extends LeanbackPreferenceFragmentCompat {
        @Override public void onCreatePreferences(Bundle state, String rootKey) {
            getPreferenceManager().setSharedPreferencesName("slate");
            setPreferencesFromResource(R.xml.tv_settings, rootKey);
        }
    }
}
