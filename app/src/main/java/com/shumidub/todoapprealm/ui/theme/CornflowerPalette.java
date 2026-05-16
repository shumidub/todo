package com.shumidub.todoapprealm.ui.theme;

import android.content.Context;
import androidx.core.content.ContextCompat;

import com.shumidub.todoapprealm.R;

/**
 * Cornflower palette used by the Tasks2 tab. Resolved once per Context.
 * Kept as a value-style holder so callers can pull a single set of ints
 * instead of repeating ContextCompat.getColor lookups inline.
 */
public final class CornflowerPalette {

    public final int bg;
    public final int surface;
    public final int surfaceMuted;
    public final int text;
    public final int textSoft;
    public final int inputText;
    public final int counter;
    public final int accent;
    public final int divider;

    public CornflowerPalette(Context ctx) {
        bg = ContextCompat.getColor(ctx, R.color.cornflowerBg);
        surface = ContextCompat.getColor(ctx, R.color.cornflowerSurface);
        surfaceMuted = ContextCompat.getColor(ctx, R.color.cornflowerSurfaceMuted);
        text = ContextCompat.getColor(ctx, R.color.cornflowerText);
        textSoft = ContextCompat.getColor(ctx, R.color.cornflowerTextSoft);
        inputText = ContextCompat.getColor(ctx, R.color.cornflowerInputText);
        counter = ContextCompat.getColor(ctx, R.color.cornflowerCounter);
        accent = ContextCompat.getColor(ctx, R.color.cornflowerAccent);
        divider = ContextCompat.getColor(ctx, R.color.cornflowerDivider);
    }
}
