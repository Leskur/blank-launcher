package com.leskur.blanklauncher;

import android.os.Build;
import java.nio.charset.Charset;
import java.util.Locale;

/** Search keys are local: no network requests or installed-package data leaves the device. */
final class AppSearch {
    private static final int[] BOUNDARIES = {-20319,-20284,-19776,-19219,-18711,-18527,-18240,-17923,-17418,-16475,-16213,-15641,-15166,-14923,-14915,-14631,-14150,-14091,-13319,-12839,-12557,-11848,-11056,-10247};
    private static final String INITIALS = "ABCDEFGHJKLMNOPQRSTWXYZ";
    private static final Charset GB = Charset.forName("GB2312");

    static String[] keys(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        if (Build.VERSION.SDK_INT >= 29) {
            try { return Modern.keys(lower); }
            catch (IllegalArgumentException ignored) { /* Vendor ICU may omit Han-Latin. */ }
        }
        StringBuilder initials = new StringBuilder();
        boolean wordStart = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                byte[] bytes = String.valueOf(c).getBytes(GB);
                char initial = c;
                if (bytes.length == 2) {
                    int value = (bytes[0] & 255) * 256 + (bytes[1] & 255) - 65536;
                    for (int j = 0; j < INITIALS.length(); j++) {
                        if (value >= BOUNDARIES[j] && value < BOUNDARIES[j + 1]) { initial = Character.toLowerCase(INITIALS.charAt(j)); break; }
                    }
                }
                initials.append(initial); wordStart = true;
            } else if (Character.isLetterOrDigit(c)) {
                if (wordStart) initials.append(c);
                wordStart = false;
            } else wordStart = true;
        }
        return new String[]{lower.replaceAll("\\s+", ""), initials.toString()};
    }

    @android.annotation.TargetApi(29)
    private static class Modern {
        private static android.icu.text.Transliterator transliterator;
        static String[] keys(String value) {
            if (transliterator == null) transliterator = android.icu.text.Transliterator.getInstance("Han-Latin; Latin-ASCII; Lower()");
            String latin = transliterator.transliterate(value);
            StringBuilder initials = new StringBuilder();
            for (String word : latin.split("[^a-z0-9]+")) if (!word.isEmpty()) initials.append(word.charAt(0));
            return new String[]{latin.replaceAll("[^a-z0-9]", ""), initials.toString()};
        }
    }
}
