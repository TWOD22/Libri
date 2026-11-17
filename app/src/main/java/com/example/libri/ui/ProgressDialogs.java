package com.example.libri.ui;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.libri.R;
import com.example.libri.data.local.BookEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Ventanas emergentes de avance: la de "Sumar avance" y la de felicitación al terminar un libro.
 * Se usan desde Inicio y desde el detalle del libro.
 */
public final class ProgressDialogs {

    private ProgressDialogs() {
        // Clase de utilidades: no se instancia
    }

    /** Pide en qué página (o porcentaje) va el lector y guarda el avance. */
    public static void showAddProgress(Context context, BookEntity book, LibraryViewModel viewModel) {
        // Contenido de la ventana, tomado de dialog_progress.xml
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
        TextView tvHint = content.findViewById(R.id.tvProgressHint);
        TextInputLayout layout = content.findViewById(R.id.inputProgressLayout);
        TextInputEditText input = content.findViewById(R.id.inputProgress);

        // Valor máximo permitido: total de páginas o 100%
        final int max = book.maxValue();
        if (book.isPages()) {
            tvHint.setText(context.getString(R.string.progress_hint_pages, book.current, book.totalPages));
            layout.setHint(context.getString(R.string.progress_field_pages));
        } else {
            tvHint.setText(context.getString(R.string.progress_hint_percent, book.current));
            layout.setHint(context.getString(R.string.progress_field_percent));
        }
        // Se muestra el avance actual para que el usuario solo lo cambie
        input.setText(String.valueOf(book.current));

        // Se crea la ventana con los botones Cancelar y Guardar
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(book.title)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        dialog.setOnShowListener(d -> {
            input.requestFocus();
            input.selectAll();
            // Se reemplaza el clic del botón para poder validar sin cerrar la ventana
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String text = input.getText() == null ? "" : input.getText().toString().trim();
                // Validaciones: que haya un número y que no pase del máximo
                if (TextUtils.isEmpty(text)) {
                    layout.setError(context.getString(R.string.error_number_required));
                    return;
                }
                int value;
                try {
                    value = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    layout.setError(context.getString(R.string.error_number_invalid));
                    return;
                }
                if (max > 0 && value > max) {
                    layout.setError(context.getString(
                            book.isPages() ? R.string.error_over_pages : R.string.error_over_percent, max));
                    return;
                }
                layout.setError(null);
                dialog.dismiss();
                // Se guarda el avance y según el resultado se muestra un mensaje distinto
                viewModel.addProgress(book.id, value, result -> {
                    if (context instanceof Activity
                            && (((Activity) context).isFinishing() || ((Activity) context).isDestroyed())) {
                        return;
                    }
                    // Si llegó al 100% se muestra el mensaje de logro
                    if (result.finished) {
                        showFinished(context, result.title);
                    } else if (result.corrected) {
                        Toast.makeText(context, R.string.toast_progress_corrected, Toast.LENGTH_SHORT).show();
                    } else if (result.changed) {
                        Toast.makeText(context, R.string.toast_progress_saved, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, R.string.toast_progress_same, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
        dialog.show();
    }

    /** Mensaje de logro cuando el libro llega al 100%. */
    public static void showFinished(Context context, String title) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.finished_title)
                .setMessage(context.getString(R.string.finished_message, title))
                .setPositiveButton(R.string.finished_button, null)
                .show();
    }
}
