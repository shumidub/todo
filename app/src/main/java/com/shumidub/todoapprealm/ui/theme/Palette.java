package com.shumidub.todoapprealm.ui.theme;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.shumidub.todoapprealm.R;

/**
 * Unified per-tab colour set. Task groups map to palettes:
 * 1 = Cornflower (Tasks2), 2 = Canary (Tasks3), 3 = Indigo (Notes).
 * Callers hold a single Palette instead of branching on three palette classes.
 */
public final class Palette {

    public final int bg;
    public final int surface;
    public final int surfaceMuted;
    public final int text;
    public final int textSoft;
    public final int inputText;
    public final int counter;
    public final int accent;
    public final int divider;

    private Palette(int bg, int surface, int surfaceMuted, int text, int textSoft,
                    int inputText, int counter, int accent, int divider) {
        this.bg = bg;
        this.surface = surface;
        this.surfaceMuted = surfaceMuted;
        this.text = text;
        this.textSoft = textSoft;
        this.inputText = inputText;
        this.counter = counter;
        this.accent = accent;
        this.divider = divider;
    }

    /** Palette for a task group, or null when the group has no themed palette (default chrome). */
    @Nullable
    public static Palette forGroup(Context ctx, int group) {
        switch (group) {
            case 1: return fromRes(ctx,
                    R.color.cornflowerBg, R.color.cornflowerSurface, R.color.cornflowerSurfaceMuted,
                    R.color.cornflowerText, R.color.cornflowerTextSoft, R.color.cornflowerInputText,
                    R.color.cornflowerCounter, R.color.cornflowerAccent, R.color.cornflowerDivider);
            case 2: return fromRes(ctx,
                    R.color.canaryBg, R.color.canarySurface, R.color.canarySurfaceMuted,
                    R.color.canaryText, R.color.canaryTextSoft, R.color.canaryInputText,
                    R.color.canaryCounter, R.color.canaryAccent, R.color.canaryDivider);
            case 3: return fromRes(ctx,
                    R.color.indigoBg, R.color.indigoSurface, R.color.indigoSurfaceMuted,
                    R.color.indigoText, R.color.indigoTextSoft, R.color.indigoInputText,
                    R.color.indigoCounter, R.color.indigoAccent, R.color.indigoDivider);
            default: return null;
        }
    }

    /** Default dialog chrome used where no tab palette applies (e.g. Tasks1 bottom sheet). */
    public static Palette dialogDefault(Context ctx) {
        int accent = ContextCompat.getColor(ctx, R.color.colorAccent);
        int onSurface = ContextCompat.getColor(ctx, R.color.colorDialogOnSurface);
        int onSurfaceVariant = ContextCompat.getColor(ctx, R.color.colorDialogOnSurfaceVariant);
        int white = ContextCompat.getColor(ctx, R.color.colorWhite);
        int dialogSurface = ContextCompat.getColor(ctx, R.color.colorDialogSurface);
        return new Palette(dialogSurface, dialogSurface, dialogSurface,
                onSurface, onSurfaceVariant, white, white, accent, onSurfaceVariant);
    }

    private static Palette fromRes(Context ctx,
                                   @ColorRes int bg, @ColorRes int surface, @ColorRes int surfaceMuted,
                                   @ColorRes int text, @ColorRes int textSoft, @ColorRes int inputText,
                                   @ColorRes int counter, @ColorRes int accent, @ColorRes int divider) {
        return new Palette(
                ContextCompat.getColor(ctx, bg),
                ContextCompat.getColor(ctx, surface),
                ContextCompat.getColor(ctx, surfaceMuted),
                ContextCompat.getColor(ctx, text),
                ContextCompat.getColor(ctx, textSoft),
                ContextCompat.getColor(ctx, inputText),
                ContextCompat.getColor(ctx, counter),
                ContextCompat.getColor(ctx, accent),
                ContextCompat.getColor(ctx, divider));
    }
}
