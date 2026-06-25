package com.example.recetapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    TextView linkLogin;
    Button btnComenzar;
    Button btnGoogle;

    EditText inputUsuario;
    EditText inputCorreo;
    EditText inputContrasenia;
    EditText inputContraseniaR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        inputUsuario = findViewById(R.id.inputUsuario);
        inputCorreo = findViewById(R.id.inputCorreo);
        inputContrasenia = findViewById(R.id.inputContrasenia);
        inputContraseniaR = findViewById(R.id.inputContraseniaR);

        linkLogin = findViewById(R.id.linkLogin);
        linkLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
            finish();
        });

        btnComenzar = findViewById(R.id.btnComenzar);
        btnComenzar.setOnClickListener(v -> validarRegistro());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult();
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (Exception e) {
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnGoogle = findViewById(R.id.btnGoogle);
        btnGoogle.setOnClickListener(v -> {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleLauncher.launch(signInIntent);
            });
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleLauncher.launch(signInIntent);
        });

    }

    private void validarRegistro() {
        String nombre = inputUsuario.getText().toString();
        String correo = inputCorreo.getText().toString();
        String contrasenia = inputContrasenia.getText().toString();
        String contraseniaR = inputContraseniaR.getText().toString();

        if (nombre.isEmpty() || correo.isEmpty() || contrasenia.isEmpty()) {
            Toast.makeText(this, "Completá todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!correo.contains("@") || !correo.contains(".") || correo.startsWith("@") || correo.endsWith(".") || correo.substring(correo.lastIndexOf(".") + 1).length() < 2) {
            Toast.makeText(this, "Ingresá un correo válido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (contrasenia.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!contrasenia.equals(contraseniaR) || !contrasenia.contentEquals(contraseniaR)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }


        db.collection("usuarios").whereEqualTo("usuario", nombre).get().addOnSuccessListener(query -> {
            if (!query.isEmpty()) {
                Toast.makeText(this, "Ese usuario ya existe", Toast.LENGTH_SHORT).show();
            } else {
                registrarUsuario(nombre, correo, contrasenia);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(
                    this,
                    "Error al registrarte",
                    Toast.LENGTH_SHORT
            ).show();
        });;

    }

    private void registrarUsuario(String nombre, String correo, String contrasenia) {
        mAuth.createUserWithEmailAndPassword(correo, contrasenia).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                guardarUsuario(nombre, correo);
            } else {
                Toast.makeText(this, "Ese correo ya está registrado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // registrar con google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(
                                    this,
                                    "Error al obtener usuario",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        String uid = user.getUid();

                        db.collection("usuarios")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(document -> {
                                    if (!document.exists()) {

                                        Map<String, Object> usuario = new HashMap<>();
                                        usuario.put("usuario", user.getDisplayName());
                                        usuario.put("correo", user.getEmail());

                                        db.collection("usuarios").document(uid).set(usuario);
                                    }

                                    Toast.makeText(this,"Bienvenido " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

                                    startActivity(new Intent(RegistroActivity.this, InicioActivity.class));
                                    finish();
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(
                                            this,
                                            "Error al guardar usuario",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });;
                    } else {
                        Toast.makeText(this,"Error de autenticación", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void guardarUsuario(String nombre, String correo) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Error al obtener usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("usuario", nombre);
        usuario.put("correo", correo);

        db.collection("usuarios").document(uid).set(usuario).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegistroActivity.this, InicioActivity.class));
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al registrarte", Toast.LENGTH_SHORT).show();
        });

    }
}