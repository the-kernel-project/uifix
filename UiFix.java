package cat.narezany.margyt.uifix;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cat.narezany.margyt.plugin.MargyPlugin;

public final class UiFix extends MargyPlugin {

    private final Handler ui = new Handler(Looper.getMainLooper());

    // ── 1. Волновая полоска: полный перерисов при паузе / скролле / поиске ──
    // Проблема: после текущей позиции оставшаяся часть была прямой.
    // Фикс: находим все WaveSeekBar/progress-view и форсируем полный invalidate()
    // + постим второй invalidate через 1 кадр, чтобы обойти пропуск первого.

    @Override
    public void onActivityResumed(Activity activity) {
        fixWave(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        fixWave(activity);
    }

    private void fixWave(Activity activity) {
        try {
            View root = activity.getWindow().getDecorView();
            forceWaveRedraw(root);
        } catch (Exception e) {
            margyt().log("UiFix wave: " + e.getMessage());
        }
    }

    private void forceWaveRedraw(View v) {
        String cls = v.getClass().getSimpleName().toLowerCase();
        if (cls.contains("wave") || cls.contains("seek") || cls.contains("waveform")) {
            v.setVisibility(View.VISIBLE);
            v.invalidate();
            // второй invalidate через следующий кадр — гарантирует перерисовку
            final View target = v;
            ui.post(() -> {
                target.invalidate();
                target.requestLayout();
            });
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) forceWaveRedraw(g.getChildAt(i));
        }
    }

    // ── 2. MD3: кнопка "Найти связанный контент" и поле комментария ──

    @Override
    public void onActivityCreated(Activity activity) {
        try {
            View root = activity.getWindow().getDecorView();
            applyMd3(root);
            hideUnwantedBanners(root);
        } catch (Exception e) {
            margyt().log("UiFix created: " + e.getMessage());
        }
    }

    private void applyMd3(View v) {
        String cls = v.getClass().getSimpleName().toLowerCase();
        CharSequence cd = v.getContentDescription();
        String desc = cd != null ? cd.toString().toLowerCase() : "";
        String txt = (v instanceof TextView) ?
                ((TextView) v).getText().toString().toLowerCase() : "";

        boolean isSearchBtn = (cls.contains("button") || cls.contains("textview")) &&
                (desc.contains("связан") || txt.contains("связан") || txt.contains("найти"));
        boolean isCommentBar = cls.contains("edittext") || cls.contains("input") ||
                desc.contains("коммент") || txt.contains("добавить комментарий");

        if (isSearchBtn) {
            v.setBackground(md3Filled(v.getContext()));
            v.setPadding(dp(v,20), dp(v,10), dp(v,20), dp(v,10));
        } else if (isCommentBar) {
            v.setBackground(md3Outline(v.getContext()));
            v.setPadding(dp(v,16), dp(v,10), dp(v,16), dp(v,10));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyMd3(g.getChildAt(i));
        }
    }

    private GradientDrawable md3Filled(Context ctx) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp2(ctx, 50));
        d.setColor(margyt().accentColour());
        return d;
    }

    private GradientDrawable md3Outline(Context ctx) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp2(ctx, 50));
        d.setColor(0x22FFFFFF);
        d.setStroke(dp2(ctx, 1), margyt().accentColour());
        return d;
    }

    // ── 3. Аватарки кликабельны ──

    private void fixAvatarClicks(View v) {
        String cls = v.getClass().getSimpleName().toLowerCase();
        if (cls.contains("avatar") || cls.contains("userpic") ||
                (cls.contains("image") && cls.contains("profile"))) {
            if (!v.hasOnClickListeners()) {
                v.setClickable(true);
                v.setFocusable(true);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) fixAvatarClicks(g.getChildAt(i));
        }
    }

    // ── 4. Скрываем плашки RootMyMargyT и "Значок за поддержку" ──

    private void hideUnwantedBanners(View v) {
        if (v instanceof TextView) {
            String t = ((TextView) v).getText().toString();
            if (t.contains("RootMyMargyT") || t.contains("rootmymargyt")
                    || t.contains("Значок за поддержку") || t.contains("Поддержать")
                    || t.contains("SuperUser ядро активно")) {
                View parent = (View) v.getParent();
                if (parent != null) parent.setVisibility(View.GONE);
                else v.setVisibility(View.GONE);
                return;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) hideUnwantedBanners(g.getChildAt(i));
        }
    }

    // ── onScreen: все фиксы при смене экрана ──

    @Override
    public void onScreen(Activity activity, String name) {
        try {
            View root = activity.getWindow().getDecorView();
            forceWaveRedraw(root);
            fixAvatarClicks(root);
            applyMd3(root);
            hideUnwantedBanners(root);
        } catch (Exception e) {
            margyt().log("UiFix screen: " + e.getMessage());
        }
    }

    @Override
    public void onStop() {
        margyt().log("UiFix v1.1: выключен");
    }

    private int dp(View v, int dp) { return dp2(v.getContext(), dp); }
    private int dp2(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
