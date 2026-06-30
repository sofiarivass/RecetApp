package com.example.recetapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class DetalleActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    // Variables de UI para el detalle
    ImageView imgRecetaCard;
    TextView tvNombreCard, tvTiempoCard, tvDescripcion, tvIngredientesCard, tvPasosCard;
    LinearLayout layoutCategoriasCard;
    LinearLayout containerIngredientes;
    LinearLayout containerPasos;

    String recetaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle);

        db = FirebaseFirestore.getInstance();

        ImageButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        Button btnEditar = findViewById(R.id.btnEditar);
        Button btnEliminar = findViewById(R.id.btnEliminar);

        imgRecetaCard = findViewById(R.id.imgRecetaCard);
        layoutCategoriasCard = findViewById(R.id.layoutCategoriasCard);
        tvNombreCard = findViewById(R.id.tvNombreCard);
        tvTiempoCard = findViewById(R.id.tvTiempoCard);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvIngredientesCard = findViewById(R.id.tvIngredientesCard);
        tvPasosCard = findViewById(R.id.tvPasosCard);

        containerIngredientes = findViewById(R.id.containerIngredientes);
        containerPasos = findViewById(R.id.containerPasos);

        // id enviado por InicioActivity
        recetaId = getIntent().getStringExtra("idReceta");

        if (recetaId != null) {
            cargarDetalleReceta(recetaId);
        }

        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(DetalleActivity.this, PublicarActivity.class);
            intent.putExtra("idReceta", recetaId);
            startActivity(intent);
        });

        btnEliminar.setOnClickListener(v -> {
            db.collection("recetas").document(recetaId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(DetalleActivity.this, "Receta eliminada", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(DetalleActivity.this, "No se pudo eliminar", Toast.LENGTH_SHORT).show());
        });
    }

    private void cargarDetalleReceta(String id) {
        db.collection("recetas").document(id).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                tvNombreCard.setText(document.getString("nombre"));
                tvTiempoCard.setText(document.getString("tiempo"));
                tvDescripcion.setText(document.getString("descripcion"));

                String imagenStr = document.getString("img");
                if (imagenStr != null && !imagenStr.isEmpty() && !imagenStr.equals("placeholder_receta.png")) {
                    com.bumptech.glide.Glide.with(this)
                            .load(imagenStr)
                            .placeholder(R.drawable.placeholder_receta) // se muestra mientras carga
                            .into(imgRecetaCard);
                } else {
                    imgRecetaCard.setImageResource(R.drawable.placeholder_receta);
                }

                // carga dinamica de multiples categorias
                List<String> categorias = (List<String>) document.get("categorias");
                if (categorias != null && !categorias.isEmpty()) {
                    layoutCategoriasCard.removeAllViews();
                    for (String nombreCategoria : categorias) {
                        TextView chip = (TextView) getLayoutInflater()
                                .inflate(R.layout.item_chip_receta, layoutCategoriasCard, false);
                        chip.setText(nombreCategoria);
                        chip.setTextSize(12);
                        layoutCategoriasCard.addView(chip);

                    }
                    layoutCategoriasCard.setVisibility(View.VISIBLE);
                } else {
                    layoutCategoriasCard.setVisibility(View.GONE);
                }

                List<Map<String, String>> ingredientes = (List<Map<String, String>>) document.get("ingredientes");
                List<String> preparacion = (List<String>) document.get("preparacion");

                tvIngredientesCard.setText((ingredientes != null ? ingredientes.size() : 0) + " ingr.");
                tvPasosCard.setText((preparacion != null ? preparacion.size() : 0) + " pasos");

                if (ingredientes != null) {
                    for (Map<String, String> ing : ingredientes) {
                        View viewIngrediente = getLayoutInflater().inflate(R.layout.item_lista_ingredientes, containerIngredientes, false);

                        TextView tvIngrediente = viewIngrediente.findViewById(R.id.tvIngrediente);
                        TextView tvCantidad = viewIngrediente.findViewById(R.id.tvCantidad);

                        tvIngrediente.setText(ing.get("nombre"));
                        tvCantidad.setText(ing.get("cantidad"));

                        containerIngredientes.addView(viewIngrediente);
                    }
                }

                if (preparacion != null) {
                    for (int i = 0; i < preparacion.size(); i++) {
                        View viewPaso = getLayoutInflater().inflate(R.layout.item_lista_pasos, containerPasos, false);

                        TextView numPaso = viewPaso.findViewById(R.id.numPaso);
                        TextView textPaso = viewPaso.findViewById(R.id.textView);

                        numPaso.setText(String.valueOf(i + 1)); // i empieza en 0, le sumamos 1
                        textPaso.setText(preparacion.get(i));

                        containerPasos.addView(viewPaso);
                    }
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show());
    }
}