package com.example.recetapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PerfilActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    TextView txtNombre, txtMail;
    TextView totalRecetas, totalFavoritas;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        txtNombre = findViewById(R.id.txtNombre);
        txtMail = findViewById(R.id.txtMail);
        totalRecetas = findViewById(R.id.totalRecetas);
        totalFavoritas = findViewById(R.id.totalFavoritas);

        cargarDatosDelPerfil();
    }

    private void cargarDatosDelPerfil() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // obtener nombre, correo y total de favoritas desde el documento del usuario
        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String nombre = document.getString("usuario");
                        String correo = document.getString("correo");

                        if (nombre != null) txtNombre.setText(nombre);
                        if (correo != null) txtMail.setText(correo);

                        List<String> favoritos = (List<String>) document.get("favoritos");
                        int cantidadFavoritos = (favoritos != null) ? favoritos.size() : 0;
                        totalFavoritas.setText(String.valueOf(cantidadFavoritos));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar el perfil", Toast.LENGTH_SHORT).show());

        // obtener el total de recetas creadas buscando en la colección recetas
        db.collection("recetas")
                .whereEqualTo("usuario", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // queryDocumentSnapshots.size() nos da el número exacto de documentos que encontró
                    int cantidadRecetas = queryDocumentSnapshots.size();
                    totalRecetas.setText(String.valueOf(cantidadRecetas));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar recetas", Toast.LENGTH_SHORT).show());
    }
}