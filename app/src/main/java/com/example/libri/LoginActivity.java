package com.example.libri;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.libri.util.SessionManager;
import com.example.libri.util.UiUtils;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Pantalla de inicio de sesión. Hay dos usuarios de prueba (ver SessionManager).
 * Si ya hay una sesión abierta, pasa directo a la pantalla principal.
 */
public class LoginActivity extends AppCompatActivity {

    // Campos y mensaje de error de la pantalla
    private TextInputEditText etUser;
    private TextInputEditText etPassword;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si ya hay sesión iniciada se salta el login
        if (SessionManager.isLoggedIn(this)) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_login);
        UiUtils.applyWindowInsets(findViewById(R.id.loginRoot), true);

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvLoginError);
        Button btnLogin = findViewById(R.id.btnLogin);

        // Al tocar Ingresar se validan los datos
        btnLogin.setOnClickListener(v -> attemptLogin());
        // El botón "Listo" del teclado también inicia sesión
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    /** Valida los campos y revisa el usuario y la contraseña. */
    private void attemptLogin() {
        String user = etUser.getText() == null ? "" : etUser.getText().toString().trim();
        String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();

        if (user.isEmpty() || pass.isEmpty()) {
            showError(getString(R.string.login_error_empty));
            return;
        }
        if (SessionManager.login(this, user, pass)) {
            openMain();
        } else {
            showError(getString(R.string.login_error_wrong));
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    // Abre la pantalla principal y cierra el login para que "atrás" no vuelva aquí
    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
