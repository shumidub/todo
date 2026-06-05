package com.shumidub.todoapprealm.ui.theme;

import android.content.Context;
import androidx.core.content.ContextCompat;

import com.shumidub.todoapprealm.R;

/**
 * Indigo palette used by the Notes tab (taskGroup 3) — a Tasks-style list whose
 * cards drop the checkbox and the points/cycling/priority params. Mirror of
 * {@link CornflowerPalette} / {@link CanaryPalette}.
 */
public final class IndigoPalette {

    public final int bg;
    public final int surface;
    public final int surfaceMuted;
    public final int text;
    public final int textSoft;
    public final int inputText;
    public final int counter;
    public final int accent;
    public final int divider;

    public IndigoPalette(Context ctx) {
        bg = ContextCompat.getColor(ctx, R.color.indigoBg);
        surface = ContextCompat.getColor(ctx, R.color.indigoSurface);
        surfaceMuted = ContextCompat.getColor(ctx, R.color.indigoSurfaceMuted);
        text = ContextCompat.getColor(ctx, R.color.indigoText);
        textSoft = ContextCompat.getColor(ctx, R.color.indigoTextSoft);
        inputText = ContextCompat.getColor(ctx, R.color.indigoInputText);
        counter = ContextCompat.getColor(ctx, R.color.indigoCounter);
        accent = ContextCompat.getColor(ctx, R.color.indigoAccent);
        divider = ContextCompat.getColor(ctx, R.color.indigoDivider);
    }
}
