package com.example.libri;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.libri.data.LibraryRepository;
import com.example.libri.ui.HomeFragment;
import com.example.libri.ui.LibraryFragment;
import com.example.libri.ui.ProfileFragment;
import com.example.libri.ui.StatsFragment;
import com.example.libri.util.SessionManager;
import com.example.libri.util.UiUtils;
import com.google.android.material.navigation.NavigationBarView;

/**
 * Pantalla principal: contiene las 4 secciones (Inicio, Biblioteca, Estadísticas, Perfil).
 * En celular la navegación va abajo; en tablet va a un lado (el mismo id "bottomNav").
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sin sesión, se vuelve al login
        if (!SessionManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Carga los libros de ejemplo la primera vez que entra cada usuario
        LibraryRepository.getInstance(this).seedIfNeeded();

        setContentView(R.layout.activity_main);
        // Evita que el contenido quede debajo de la barra de estado o de navegación
        UiUtils.applyWindowInsets(findViewById(R.id.mainRoot), false);

        // Barra inferior en celular o riel lateral en tablet (ambas son NavigationBarView)
        NavigationBarView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            // Según el botón tocado se muestra la sección correspondiente
            Fragment fragment;
            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_library) {
                fragment = new LibraryFragment();
            } else if (id == R.id.nav_stats) {
                fragment = new StatsFragment();
            } else {
                fragment = new ProfileFragment();
            }
            show(fragment);
            return true;
        });

        // Solo la primera vez; al girar la pantalla Android restaura el fragmento solo
        if (savedInstanceState == null) {
            nav.setSelectedItemId(R.id.nav_home);
        }
    }

    // Reemplaza el fragmento visible por el elegido
    private void show(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
