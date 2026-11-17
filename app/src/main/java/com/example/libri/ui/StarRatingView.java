package com.example.libri.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.libri.R;
import com.example.libri.util.UiUtils;

/**
 * Fila de 5 estrellas para calificar un libro.
 * Al tocar una estrella se pone esa calificación; si se toca la misma otra vez, se quita.
 */
public class StarRatingView extends LinearLayout {

    /** Se avisa cuando el usuario cambia la calificación. */
    public interface OnRatingChangeListener {
        void onRatingChanged(int rating);
    }

    // Cantidad de estrellas
    private static final int STARS = 5;

    // Las vistas de cada estrella
    private final ImageView[] stars = new ImageView[STARS];
    // Calificación actual (0 = sin calificar)
    private int rating = 0;
    private OnRatingChangeListener listener;

    public StarRatingView(Context context) {
        super(context);
        init(context);
    }

    public StarRatingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        // Tamaño de cada estrella y su espacio interior, en píxeles
        int size = UiUtils.dp(context, 40);
        int pad = UiUtils.dp(context, 6);
        for (int i = 0; i < STARS; i++) {
            // Se crea cada estrella por código y se agrega a la fila
            ImageView star = new ImageView(context);
            star.setLayoutParams(new LayoutParams(size, size));
            star.setPadding(pad, pad, pad, pad);
            star.setContentDescription(context.getString(R.string.cd_star, i + 1));
            final int value = i + 1;
            star.setOnClickListener(v -> {
                // Tocar la misma estrella otra vez quita la calificación
                int newRating = (value == rating) ? 0 : value;
                setRating(newRating);
                if (listener != null) {
                    listener.onRatingChanged(newRating);
                }
            });
            stars[i] = star;
            addView(star);
        }
        refresh();
    }

    public void setOnRatingChangeListener(OnRatingChangeListener listener) {
        this.listener = listener;
    }

    /** Pone la calificación (0 a 5) sin avisar al listener. */
    public void setRating(int rating) {
        this.rating = Math.max(0, Math.min(STARS, rating));
        refresh();
    }

    public int getRating() {
        return rating;
    }

    // Pinta las estrellas llenas hasta la calificación y el resto vacías
    private void refresh() {
        for (int i = 0; i < STARS; i++) {
            stars[i].setImageResource(i < rating ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        }
    }
}
