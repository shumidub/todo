package com.shumidub.todoapprealm.ui.theme;

import android.content.Context;
import androidx.core.content.ContextCompat;

import com.shumidub.todoapprealm.R;

/**
 * Canary palette used by the Tasks3 tab. Mirror of {@link CornflowerPalette}.
 * Parallel class by design — see task-003 R16. A shared TabPalette interface
 * will be introduced in a future refactor.
 */
public final class CanaryPalette {

    public final int bg;
    public final int surface;
    public final int surfaceMuted;
    public final int text;
    public final int textSoft;
    public final int inputText;
    public final int counter;
    public final int accent;
    public final int divider;

    public CanaryPalette(Context ctx) {
        bg = ContextCompat.getColor(ctx, R.color.canaryBg);
        surface = ContextCompat.getColor(ctx, R.color.canarySurface);
        surfaceMuted = ContextCompat.getColor(ctx, R.color.canarySurfaceMuted);
        text = ContextCompat.getColor(ctx, R.color.canaryText);
        textSoft = ContextCompat.getColor(ctx, R.color.canaryTextSoft);
        inputText = ContextCompat.getColor(ctx, R.color.canaryInputText);
        counter = ContextCompat.getColor(ctx, R.color.canaryCounter);
        accent = ContextCompat.getColor(ctx, R.color.canaryAccent);
        divider = ContextCompat.getColor(ctx, R.color.canaryDivider);
    }
}
