package com.example.libri.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.libri.LoginActivity;
import com.example.libri.R;
import com.example.libri.data.local.BookEntity;
import com.example.libri.util.SessionManager;
import com.example.libri.util.ThemePrefs;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

/**
 * Perfil: datos del usuario, resumen de su biblioteca, selector de tema y cierre de sesión.
 */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    // Crea la pantalla a partir de fragment_profile.xml
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // ViewModel compartido para contar los libros del usuario
        LibraryViewModel viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);

        String name = SessionManager.getDisplayName(requireContext());
        TextView tvAvatar = view.findViewById(R.id.tvAvatar);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvUser = view.findViewById(R.id.tvUser);
        TextView tvTotals = view.findViewById(R.id.tvTotals);

        // El avatar muestra la inicial del nombre
        tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        tvName.setText(name);
        tvUser.setText(getString(R.string.profile_user, SessionManager.getUserId(requireContext())));

        // Resumen: cuántos libros hay y cuántos están terminados
        viewModel.getBooks().observe(getViewLifecycleOwner(), (List<BookEntity> list) -> {
            int total = list == null ? 0 : list.size();
            int done = 0;
            if (list != null) {
                for (BookEntity b : list) {
                    if (BookEntity.STATUS_DONE.equals(b.status)) {
                        done++;
                    }
                }
            }
            tvTotals.setText(getString(R.string.profile_totals, total, done));
        });

        // Selector de tema: automático (según el sistema), claro u oscuro
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.themeToggle);
        int mode = ThemePrefs.getMode(requireContext());
        toggle.check(mode == ThemePrefs.MODE_LIGHT ? R.id.btnThemeLight
                : mode == ThemePrefs.MODE_DARK ? R.id.btnThemeDark : R.id.btnThemeAuto);
        // Al elegir un modo se guarda y se aplica al instante
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int newMode = checkedId == R.id.btnThemeLight ? ThemePrefs.MODE_LIGHT
                    : checkedId == R.id.btnThemeDark ? ThemePrefs.MODE_DARK : ThemePrefs.MODE_AUTO;
            if (newMode != ThemePrefs.getMode(requireContext())) {
                // Guarda y aplica; Android recrea la pantalla con los nuevos colores
                ThemePrefs.setMode(requireContext(), newMode);
            }
        });

        // Cerrar sesión: se borra la sesión y se vuelve al login sin poder regresar con "atrás"
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            SessionManager.logout(requireContext());
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
