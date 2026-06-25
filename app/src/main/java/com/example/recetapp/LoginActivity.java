package com.example.recetapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    Button btnGoogle;
    Button btnIngresar;
    TextView linkRegistro;

    EditText inputCorreo;
    EditText inputContrasenia;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        inputCorreo = findViewById(R.id.inputCorreo);
        inputContrasenia = findViewById(R.id.inputContrasenia);


        linkRegistro = findViewById(R.id.linkRegistro);
        linkRegistro.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
            finish();
        });

        btnIngresar = findViewById(R.id.btnIngresar);
        btnIngresar.setOnClickListener(v -> validarIngreso());


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

    private void validarIngreso() {
        String correo = inputCorreo.getText().toString();
        String contrasenia = inputContrasenia.getText().toString();

        if (correo.isEmpty() || contrasenia.isEmpty()) {
            Toast.makeText(this, "Completá todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(correo, contrasenia).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    db.collection("usuarios").document(user.getUid()).get()
                            .addOnSuccessListener(document -> {
                                String nombre = "";
                                if (document.exists() && document.contains("usuario")) {
                                    nombre = document.getString("usuario");
                                } else {
                                    nombre = user.getDisplayName() != null ? user.getDisplayName() : ":D";
                                }

                                Toast.makeText(this, "Bienvenido de nuevo " + nombre + "!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, InicioActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                startActivity(new Intent(LoginActivity.this, InicioActivity.class));
                                finish();
                            });
                }
            } else {
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(this, "Error al obtener usuario", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = user.getUid();

                db.collection("usuarios").document(uid).get().addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("usuario", user.getDisplayName());
                        usuario.put("correo", user.getEmail());

                        db.collection("usuarios").document(uid).set(usuario);
                    }
                    Toast.makeText(this, "Bienvenido " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, InicioActivity.class));
                    finish();
                });
            } else {
                Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show();
            }
        });
    }
}