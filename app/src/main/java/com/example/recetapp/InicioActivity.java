package com.example.recetapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InicioActivity extends AppCompatActivity {

    LinearLayout layoutCategorias;
    LinearLayout containerRecetas;
    TextView msjVacio;

    private String categoriaActual = "Todas";

    ImageView btnPerfil;
    Button btnNuevaReceta;

    EditText etBuscar;
    private List<DocumentSnapshot> recetasCategoriaActual = new ArrayList<>();
    private List<String> favoritosActuales = new ArrayList<>();

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

        btnPerfil = findViewById(R.id.btnPerfil);
        btnPerfil.setOnClickListener(v -> {
            startActivity(new Intent(InicioActivity.this, PerfilActivity.class));
        });

        btnNuevaReceta = findViewById(R.id.btnNuevaReceta);
        btnNuevaReceta.setOnClickListener(v -> {
            startActivity(new Intent(InicioActivity.this, PublicarActivity.class));
        });

        etBuscar = findViewById(R.id.etBuscar);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                mostrarRecetas(filtrarPorTexto(recetasCategoriaActual, s.toString()), favoritosActuales, categoriaActual);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        cargarCategoriasUsuario();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // cada vez que vuelve al inicio (ej: despues de eliminar una receta), recarga la lista con la categoria que el usuario tenia seleccionada
        cargarRecetas(categoriaActual);
    }

    private List<DocumentSnapshot> filtrarPorTexto(List<DocumentSnapshot> lista, String texto) {
        if (texto == null || texto.trim().isEmpty()) return lista;
        String textoLower = texto.toLowerCase().trim();
        List<DocumentSnapshot> resultado = new ArrayList<>();
        for (DocumentSnapshot doc : lista) {
            String nombre = doc.getString("nombre");
            if (nombre != null && nombre.toLowerCase().contains(textoLower)) {
                resultado.add(doc);
            }
        }
        return resultado;
    }

    private void cargarCategoriasUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(document -> {
            List<String> cats = (List<String>) document.get("categorias");
            layoutCategorias.removeAllViews();

            agregarChip("", false, R.drawable.icono_corazon, false);
            agregarChip("Todas", true, 0, true);

            agregarSeparador();

            if (cats != null) {
                for (String cat : cats) {
                    agregarChip(cat, false, 0, false);
                }
            }
        });
    }

    private void agregarSeparador() {
        View separador = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (1.5f * getResources().getDisplayMetrics().density),
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(6, 16, 28, 16);
        separador.setLayoutParams(params);
        separador.setBackgroundColor(Color.parseColor("#D3C1B3"));
        layoutCategorias.addView(separador);
    }

    private void agregarChip(String nombre, boolean esSeleccionado, int iconoResId, boolean esNegrita) {
        TextView chip = (TextView) getLayoutInflater().inflate(R.layout.item_categoria, layoutCategorias, false);

        chip.setText(nombre);

        if (esNegrita) {
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        if (iconoResId != 0) {
            android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(this, iconoResId);
            if (drawable != null) {
                int size = (int) (18 * getResources().getDisplayMetrics().density);
                drawable.setBounds(0, 0, size, size);
                chip.setCompoundDrawables(drawable, null, null, null);
                chip.setCompoundDrawablePadding(8);
            }
        } else {
            chip.setCompoundDrawables(null, null, null, null);
        }

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

    private void mostrarRecetas(List<DocumentSnapshot> recetas, List<String> listaFavoritos, String filtroCategoria) {
        containerRecetas.removeAllViews();

        for (DocumentSnapshot document : recetas) {
            View card = getLayoutInflater().inflate(R.layout.item_receta, containerRecetas, false);

            ImageView imgRecetaCard = card.findViewById(R.id.imgRecetaCard);
            String imagenStr = document.getString("img");

            if (imagenStr != null && !imagenStr.isEmpty() && !imagenStr.equals("placeholder_receta.png")) {
                com.bumptech.glide.Glide.with(card.getContext())
                        .load(imagenStr)
                        .placeholder(R.drawable.placeholder_receta)
                        .into(imgRecetaCard);
            } else {
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


            ImageButton btnFavorito = card.findViewById(R.id.btnFavorito);
            String recetaId = document.getId();

            final boolean[] esFavorito = {listaFavoritos.contains(recetaId)};

            if (esFavorito[0]) {
                btnFavorito.setImageResource(R.drawable.icono_corazon_relleno);
            } else {
                btnFavorito.setImageResource(R.drawable.icono_corazon_vacio);
            }

            FirebaseUser user = mAuth.getCurrentUser();
            btnFavorito.setOnClickListener(v -> {
                if (esFavorito[0]) {
                    db.collection("usuarios").document(user.getUid())
                            .update("favoritos", FieldValue.arrayRemove(recetaId))
                            .addOnSuccessListener(aVoid -> {
                                esFavorito[0] = false;
                                btnFavorito.setImageResource(R.drawable.icono_corazon_vacio);

                                // Extra: Si el usuario saca el corazón MIENTRAS está en la pestaña favoritos,
                                // recargamos la lista para que la tarjeta desaparezca dinámicamente.
                                if (categoriaActual.equals("Favoritos") || categoriaActual.equals("")) {
                                    cargarRecetas(categoriaActual);
                                }
                            });
                } else {
                    db.collection("usuarios").document(user.getUid())
                            .update("favoritos", FieldValue.arrayUnion(recetaId))
                            .addOnSuccessListener(aVoid -> {
                                esFavorito[0] = true;
                                btnFavorito.setImageResource(R.drawable.icono_corazon_relleno);
                            });
                }
            });

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, DetalleActivity.class);
                intent.putExtra("idReceta", recetaId);
                startActivity(intent);
            });

            containerRecetas.addView(card);
        }

        // mensajes de estado vacío
        if (containerRecetas.getChildCount() == 0) {
            if (filtroCategoria.equals("Todas")) {
                msjVacio.setText("Todavía no cargaste ninguna receta");
            } else if (filtroCategoria.equals("Favoritos")) {
                msjVacio.setText("Todavía no agregaste recetas a favoritos");
            } else {
                msjVacio.setText("No tenés recetas en la categoría " + filtroCategoria);
            }
            msjVacio.setVisibility(View.VISIBLE);
        } else {
            msjVacio.setVisibility(View.GONE);
        }
    }

    private void cargarRecetas(String filtroCategoria) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(userDoc -> {
            List<String> favoritosUser = (List<String>) userDoc.get("favoritos");
            final List<String> listaFavoritos = (favoritosUser != null) ? favoritosUser : new ArrayList<>();

            db.collection("recetas").whereEqualTo("usuario", user.getUid()).get().addOnSuccessListener(querySnapshot -> {

                // pool de hilos
                ExecutorService executor = Executors.newFixedThreadPool(3);

                executor.execute(() -> {
                    List<DocumentSnapshot> recetasFiltradas = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        if (filtroCategoria.equals("Favoritos") || filtroCategoria.equals("")) {
                            if (listaFavoritos.contains(document.getId())) {
                                recetasFiltradas.add(document);
                            }
                        } else if (filtroCategoria.equals("Todas")) {
                            recetasFiltradas.add(document);
                        } else {
                            List<String> cats = (List<String>) document.get("categorias");
                            if (cats != null && cats.contains(filtroCategoria)) {
                                recetasFiltradas.add(document);
                            }
                        }
                    }

                    // hilo principal para la ui
                    runOnUiThread(() -> {
                        recetasCategoriaActual = recetasFiltradas;
                        favoritosActuales = listaFavoritos;
                        mostrarRecetas(filtrarPorTexto(recetasFiltradas, etBuscar.getText().toString()), listaFavoritos, filtroCategoria);
                    });


                });

                executor.shutdown();

            }).addOnFailureListener(e -> Toast.makeText(this, "Error cargando recetas", Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> Toast.makeText(this, "Error cargando usuario", Toast.LENGTH_SHORT).show());
    }
}