package cat.narezany.margyt.uifix;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cat.narezany.margyt.plugin.MargyPlugin;
import java.lang.reflect.Method;

public final class UiFix extends MargyPlugin {

    private final Handler ui = new Handler(Looper.getMainLooper());

    // ── margyt() через рефлексию: не зависим от точного типа PluginContext ──

    private Object ctx() {
        try {
            Class<?> c = getClass();
            while (c != null) {
                try {
                    Method m = c.getDeclaredMethod("margyt");
                    m.setAccessible(true);
                    return m.invoke(this);
                } catch (NoSuchMethodException ignore) {
                    c = c.getSuperclass();
                }
            }
        } catch (Throwable ignore) {}
        return null;
    }

    private void log(String s) {
        try {
            Object c = ctx();
            if (c != null) c.getClass().getMethod("log", String.class).invoke(c, s);
        } catch (Throwable ignore) {}
    }

    private int accent() {
        try {
            Object c = ctx();
            if (c != null) return (Integer) c.getClass().getMethod("accentColour").invoke(c);
        } catch (Throwable ignore) {}
        return 0xFFA855F7;
    }

    // ── 1. Волновая полоска ──

    @Override
    public void onActivityResumed(Activity a) { run(a); }

    @Override
    public void onActivityPaused(Activity a) { run(a); }

    @Override
    public void onScreen(Activity a, String name) { run(a); }

    @Override
    public void onActivityCreated(Activity a) { run(a); }

    private void run(final Activity a) {
        try {
            final View root = a.getWindow().getDecorView();
            fix(root);
            // второй проход после отрисовки: ловит то, что создалось позже
            ui.postDelayed(new Runnable() {
                @Override public void run() {
                    try { fix(root); } catch (Throwable t) { log("UiFix delayed: " + t); }
                }
            }, 400);
        } catch (Throwable t) {
            log("UiFix run: " + t);
        }
    }

    private void fix(View v) {
        String cls = v.getClass().getSimpleName().toLowerCase();

        // волна: полный перерисов
        if (cls.contains("wave") || cls.contains("seek")) {
            v.setVisibility(View.VISIBLE);
            v.invalidate();
            final View t = v;
            ui.post(new Runnable() {
                @Override public void run() { t.invalidate(); t.requestLayout(); }
            });
        }

        // аватарки: кликабельные
        if (cls.contains("avatar") || cls.contains("userpic")) {
            if (!v.hasOnClickListeners()) {
                v.setClickable(true);
                v.setFocusable(true);
            }
        }

        String txt = "";
        if (v instanceof TextView) {
            CharSequence cs = ((TextView) v).getText();
            if (cs != null) txt = cs.toString();
        }
        CharSequence cd = v.getContentDescription();
        String desc = cd != null ? cd.toString().toLowerCase() : "";
        String low = txt.toLowerCase();

        // плашки: RootMyMargyT и «Значок за поддержку»
        if (txt.length() > 0 && (low.contains("rooted margyt") || low.contains("rootmymargyt")
                || low.contains("superuser ядро") || low.contains("значок за поддержку")
                || low.equals("поддержать"))) {
            View hide = v;
            if (v.getParent() instanceof View) hide = (View) v.getParent();
            hide.setVisibility(View.GONE);
            return;
        }

        // MD3: «Найти связанный контент» и «Добавить комментарий»
        boolean related = low.contains("найти связанный") || desc.contains("связанный");
        boolean comment = low.contains("добавить комментарий") || desc.contains("добавить комментарий");
        if (related) {
            v.setBackground(shape(v.getContext(), true));
            v.setPadding(dp(v, 20), dp(v, 10), dp(v, 20), dp(v, 10));
        } else if (comment) {
            v.setBackground(shape(v.getContext(), false));
            v.setPadding(dp(v, 16), dp(v, 10), dp(v, 16), dp(v, 10));
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) fix(g.getChildAt(i));
        }
    }

    private GradientDrawable shape(Context c, boolean filled) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp2(c, 50));
        if (filled) {
            d.setColor(accent());
        } else {
            d.setColor(0x22FFFFFF);
            d.setStroke(dp2(c, 1), accent());
        }
        return d;
    }

    @Override
    public void onStop() { log("UiFix: выключен"); }

    private int dp(View v, int x) { return dp2(v.getContext(), x); }
    private int dp2(Context c, int x) {
        return Math.round(x * c.getResources().getDisplayMetrics().density);
    }
}
