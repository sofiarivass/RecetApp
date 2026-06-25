package com.example.recetapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InicioActivity extends AppCompatActivity {

    LinearLayout layoutCategorias;
    LinearLayout containerRecetas;
    TextView msjVacio;

    private String categoriaActual = "Todas";

    android.widget.ImageButton btnEditarCategorias;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        layoutCategorias = findViewById(R.id.linearLayoutCategorias);
        containerRecetas = findViewById(R.id.containerRecetas);
        msjVacio = findViewById(R.id.msjVacio);

        Button btnNuevaReceta = findViewById(R.id.btnNuevaReceta);
        btnNuevaReceta.setOnClickListener(v -> {
            startActivity(new Intent(InicioActivity.this, PublicarActivity.class));
        });

        cargarCategoriasUsuario();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // cada vez que vuelve al inicio (ej: despues de eliminar una receta), recarga la lista con la categoria que el usuario tenia seleccionada
        cargarRecetas(categoriaActual);
    }

    private void cargarCategoriasUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(document -> {
            List<String> cats = (List<String>) document.get("categorias");
            layoutCategorias.removeAllViews();
            agregarChip("Todas", true);
            if (cats != null) {
                for (String cat : cats) {
                    agregarChip(cat, false);
                }
            }
        });
    }

    private void agregarChip(String nombre, boolean esSeleccionado) {
        TextView chip = (TextView) getLayoutInflater().inflate(R.layout.item_categoria, layoutCategorias, false);

        chip.setText(nombre);

        if (esSeleccionado) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setTextColor(Color.parseColor("#934B00"));
        }

        chip.setOnClickListener(v -> {
            categoriaActual = nombre;

            cargarRecetas(nombre);
            actualizarSeleccionChips(nombre);
        });

        layoutCategorias.addView(chip);
    }

    private void actualizarSeleccionChips(String nombreSeleccionado) {
        for (int i = 0; i < layoutCategorias.getChildCount(); i++) {
            View vista = layoutCategorias.getChildAt(i);

            if (vista instanceof TextView) {
                TextView chip = (TextView) vista;
                String nombreChip = chip.getText().toString();

                if (nombreChip.equals(nombreSeleccionado)) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected);
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip);
                    chip.setTextColor(Color.parseColor("#934B00"));
                }
            }
        }
    }

    private void cargarRecetas(String filtroCategoria) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        db.collection("recetas").whereEqualTo("usuario", user.getUid()).get().addOnSuccessListener(querySnapshot -> {

            // pool de hilos
            ExecutorService executor = Executors.newFixedThreadPool(3);

            executor.execute(() -> {
                List<DocumentSnapshot> recetasFiltradas = new ArrayList<>();

                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    List<String> cats = (List<String>) document.get("categorias");
                    if (filtroCategoria.equals("Todas") || (cats != null && cats.contains(filtroCategoria))) {
                        recetasFiltradas.add(document);
                    }
                }

                // hilo principal para la ui
                runOnUiThread(() -> {
                    containerRecetas.removeAllViews();

                    for (DocumentSnapshot document : recetasFiltradas) {
                        View card = getLayoutInflater().inflate(R.layout.item_receta, containerRecetas, false);

                        ImageView imgRecetaCard = card.findViewById(R.id.imgRecetaCard);
                        String imagenStr = document.getString("img");

                        if (imagenStr != null && !imagenStr.isEmpty() && !imagenStr.equals("placeholder_receta.png")) {
                            com.bumptech.glide.Glide.with(card.getContext())
                                    .load(imagenStr)
                                    .placeholder(R.drawable.placeholder_receta) // se muestra el placeholder mientras carga la img
                                    .into(imgRecetaCard);
                        } else {
                            // si no hay img muestra el placeholder
                            imgRecetaCard.setImageResource(R.drawable.placeholder_receta);
                        }

                        TextView tvNombre = card.findViewById(R.id.tvNombreCard);
                        TextView tvTiempo = card.findViewById(R.id.tvTiempoCard);

                        LinearLayout layoutCategoriasCard = card.findViewById(R.id.layoutCategoriasCard);

                        TextView tvIngredientes = card.findViewById(R.id.tvIngredientesCard);
                        TextView tvPasos = card.findViewById(R.id.tvPasosCard);

                        tvNombre.setText(document.getString("nombre"));
                        tvTiempo.setText(document.getString("tiempo"));

                        List<String> categorias = (List<String>) document.get("categorias");
                        List<?> ingredientes = (List<?>) document.get("ingredientes");
                        List<?> preparacion = (List<?>) document.get("preparacion");

                        // carga dinamica de multiples categorias
                        if (categorias != null && !categorias.isEmpty()) {
                            layoutCategoriasCard.removeAllViews();
                            for (String nombreCategoria : categorias) {
                                TextView chip = (TextView) LayoutInflater.from(card.getContext())
                                        .inflate(R.layout.item_chip_receta, layoutCategoriasCard, false);
                                chip.setText(nombreCategoria);
                                layoutCategoriasCard.addView(chip);
                            }
                        }

                        tvIngredientes.setText((ingredientes != null ? ingredientes.size() : 0) + " ingr.");
                        tvPasos.setText((preparacion != null ? preparacion.size() : 0) + " pasos");

                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(this, DetalleActivity.class);
                            intent.putExtra("idReceta", document.getId());
                            startActivity(intent);
                        });

                        containerRecetas.addView(card);
                    }

                    // mensajes de estado vacio
                    if (containerRecetas.getChildCount() == 0) {
                        if (filtroCategoria.equals("Todas")) {
                            msjVacio.setText("Todavía no cargaste ninguna receta");
                        } else {
                            msjVacio.setText("No tenés recetas en la categoría " + filtroCategoria);
                        }
                        msjVacio.setVisibility(View.VISIBLE);
                    } else {
                        msjVacio.setVisibility(View.GONE);
                    }
                });
            });

            // apagar executor
            executor.shutdown();

        }).addOnFailureListener(e -> Toast.makeText(this, "Error cargando recetas", Toast.LENGTH_SHORT).show());
    }
}