package com.example.recetapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import com.cloudinary.android.MediaManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PublicarActivity extends AppCompatActivity {
    TextView txtTitulo;

    ImageButton btnVolver;
    TextView btnDescartar;
    Button btnGuardar;

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    FrameLayout subirFoto;
    ImageView imgReceta;
    LinearLayout layoutPlaceholder;
    LinearLayout layoutBotonesFoto;
    ImageButton btnEditarFoto;
    ImageButton btnEliminarFoto;
    Uri imagenSeleccionada = null;

    LinearLayout layoutCategorias;
    TextView chipAgregar;
    ArrayList<String> categoriasSeleccionadas = new ArrayList<>();
    ArrayList<String> categorias = new ArrayList<>();

    LinearLayout layoutIngredientes;
    LinearLayout btnAnadirIngrediente;

    LinearLayout layoutPasos;
    LinearLayout layoutPreparacion;
    LinearLayout btnAnadirPaso;

    EditText inputTiempo;
    EditText inputNombre;
    EditText inputDescripcion;

    String recetaIdEditar = null;
    String imagenActual = "";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_publicar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Cloudinary
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "doz0zsjzr");
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Ya estaba inicializado, no hacemos nada
        }

        txtTitulo = findViewById(R.id.txtTitulo);

        btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());

        btnDescartar = findViewById(R.id.btnDescartar);
        btnDescartar.setOnClickListener(v -> finish());

        btnGuardar = findViewById(R.id.btnGuardar);
        btnGuardar.setOnClickListener(v -> guardarReceta());

        subirFoto = findViewById(R.id.subirFoto);
        imgReceta = findViewById(R.id.imgReceta);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);
        layoutBotonesFoto = findViewById(R.id.layoutBotonesFoto);
        btnEditarFoto = findViewById(R.id.btnEditarFoto);
        btnEliminarFoto = findViewById(R.id.btnEliminarFoto);

        layoutCategorias = findViewById(R.id.layoutCategorias);
        chipAgregar = findViewById(R.id.chipAgregar);

        inputTiempo = findViewById(R.id.inputTiempo);
        inputNombre = findViewById(R.id.inputNombre);
        inputDescripcion = findViewById(R.id.inputDescripcion);

        imgReceta.setVisibility(View.GONE);
        layoutPlaceholder.setVisibility(View.VISIBLE);
        layoutBotonesFoto.setVisibility(View.GONE);

        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                imagenSeleccionada = uri;
                imgReceta.setImageURI(uri);
                imgReceta.setVisibility(View.VISIBLE);
                layoutPlaceholder.setVisibility(View.GONE);
                layoutBotonesFoto.setVisibility(View.VISIBLE);
            }
        });


        subirFoto.setOnClickListener(v -> abrirGaleria());

        btnEditarFoto.setOnClickListener(v -> abrirGaleria());

        btnEliminarFoto.setOnClickListener(v -> {
            imagenSeleccionada = null;
            imgReceta.setImageURI(null);
            imgReceta.setVisibility(View.GONE);
            layoutPlaceholder.setVisibility(View.VISIBLE);
            layoutBotonesFoto.setVisibility(View.GONE);
        });

        cargarCategorias();
        chipAgregar.setOnClickListener(v -> crearCategoria());

        layoutIngredientes = findViewById(R.id.layoutIngredientes);
        btnAnadirIngrediente = findViewById(R.id.btnAnadirIngrediente);
        agregarIngrediente();
        btnAnadirIngrediente.setOnClickListener(v -> agregarIngrediente());

        layoutPasos = findViewById(R.id.layoutPasos);
        layoutPreparacion = findViewById(R.id.layoutPreparacion);
        btnAnadirPaso = findViewById(R.id.btnAnadirPaso);
        agregarPaso();
        btnAnadirPaso.setOnClickListener(v -> agregarPaso());

        recetaIdEditar = getIntent().getStringExtra("idReceta");

        if (recetaIdEditar != null) {
            txtTitulo.setText("Editar Receta");
            datosEditar(recetaIdEditar);
        }
    }

    private void abrirGaleria() {
        pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    private void cargarCategorias() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("usuarios").document(user.getUid()).get().addOnSuccessListener(document -> {
            List<String> categoriasFirestore = (List<String>) document.get("categorias");
            if (categoriasFirestore != null) {
                for (String nombre : categoriasFirestore) {
                    agregarChip(nombre);
                }
            }
        });
    }

    private void agregarChip(String nombre) {
        View vista = getLayoutInflater().inflate(R.layout.item_categoria, layoutCategorias, false);
        TextView chip = vista.findViewById(R.id.chipCategoria);
        chip.setText(nombre);

        if (categoriasSeleccionadas.contains(nombre)) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(getColor(R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_outline);
            chip.setTextColor(getColor(R.color.black));
        }

        chip.setOnClickListener(v -> {
            if (categoriasSeleccionadas.contains(nombre)) {
                categoriasSeleccionadas.remove(nombre);
                chip.setBackgroundResource(R.drawable.bg_chip_outline);
                chip.setTextColor(getColor(R.color.black));
            } else {
                categoriasSeleccionadas.add(nombre);
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(getColor(R.color.white));
            }
        });
        layoutCategorias.addView(vista);
    }

    private void crearCategoria() {
        EditText input = new EditText(this);
        new AlertDialog.Builder(this).setTitle("Nueva categoría").setView(input).setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = input.getText().toString().trim();
            if (nombre.isEmpty()) return;
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;
            categorias.add(nombre);
            agregarChip(nombre);
            db.collection("usuarios").document(user.getUid()).update("categorias", FieldValue.arrayUnion(nombre)).addOnFailureListener(e -> {
                Toast.makeText(this, "Error al guardar categoría", Toast.LENGTH_SHORT).show();
            });
        }).setNegativeButton("Cancelar", null).show();
    }

    private void agregarIngrediente() {
        View fila = getLayoutInflater().inflate(R.layout.item_ingrediente, layoutIngredientes, false);
        ImageButton btnEliminarIngrediente = fila.findViewById(R.id.btnEliminarIngrediente);
        btnEliminarIngrediente.setOnClickListener(v -> {
            layoutIngredientes.removeView(fila);
        });
        layoutIngredientes.addView(fila);
    }

    private void agregarPaso() {
        View fila = getLayoutInflater().inflate(R.layout.item_preparacion, layoutPasos, false);

        ImageButton btnEliminarPaso = fila.findViewById(R.id.btnEliminarPaso);

        btnEliminarPaso.setOnClickListener(v -> {
            layoutPasos.removeView(fila);
            actualizarNumerosPasos();
        });

        layoutPasos.addView(fila);

        actualizarNumerosPasos();
    }

    private void actualizarNumerosPasos() {
        for (int i = 0; i < layoutPasos.getChildCount(); i++) {
            View fila = layoutPasos.getChildAt(i);
            TextView numPaso = fila.findViewById(R.id.numPaso);
            numPaso.setText(String.valueOf(i + 1));
        }
    }

    private boolean validarDatos() {
        if (inputNombre == null || inputNombre.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "El nombre de la receta es obligatorio", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (inputTiempo == null || inputTiempo.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "El tiempo de preparación es obligatorio", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (categoriasSeleccionadas == null || categoriasSeleccionadas.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una categoría", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (layoutIngredientes == null || layoutIngredientes.getChildCount() == 0) {
            Toast.makeText(this, "Debes agregar al menos un ingrediente", Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean encontroIngredientes = false;
        for (int i = 0; i < layoutIngredientes.getChildCount(); i++) {
            View fila = layoutIngredientes.getChildAt(i);
            if (fila == null) continue;

            EditText ingrediente = fila.findViewById(R.id.inputIngrediente);
            EditText cantidad = fila.findViewById(R.id.inputCantidad);

            if (ingrediente == null || cantidad == null) continue;

            encontroIngredientes = true;

            if (ingrediente.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Falta el nombre de un ingrediente", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (cantidad.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Falta la cantidad en un ingrediente", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (!encontroIngredientes) {
            Toast.makeText(this, "Debes agregar al menos un ingrediente", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (layoutPasos == null || layoutPasos.getChildCount() == 0) {
            Toast.makeText(this, "Debes agregar al menos un paso de preparación", Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean encontroPasos = false;
        for (int i = 0; i < layoutPasos.getChildCount(); i++) {
            View fila = layoutPasos.getChildAt(i);
            if (fila == null) continue;

            EditText descPaso = fila.findViewById(R.id.inputPaso);

            if (descPaso == null) continue;

            encontroPasos = true;

            if (descPaso.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Un paso de preparación está vacío", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (!encontroPasos) {
            Toast.makeText(this, "Debes agregar al menos un paso de preparación", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void guardarReceta() {
        if (!validarDatos()) {
            return;
        }

        Toast.makeText(this, "Subiendo receta...", Toast.LENGTH_LONG).show();

        if (imagenSeleccionada != null) {
            // subir a cloudinary
            MediaManager.get().upload(imagenSeleccionada).unsigned("recetapp_preset")
                    .callback(new com.cloudinary.android.callback.UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imgCloudinary = (String) resultData.get("secure_url");
                            guardarEnFirestore(imgCloudinary);
                        }

                        @Override
                        public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                            Toast.makeText(PublicarActivity.this, "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        }
                    }).dispatch();
        } else {
            // por si el usuario no sube foto
            guardarEnFirestore(imagenActual);
        }
    }


    private void guardarEnFirestore(String imgCloudinary) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error al obtener usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombreText = inputNombre.getText().toString();
        String tiempoText = inputTiempo.getText().toString();

        String descText;
        if (inputDescripcion != null) {
            descText = inputDescripcion.getText().toString();
        } else {
            descText = "";
        }

        List<String[]> ingredientesCrudos = new ArrayList<>();
        for (int i = 0; i < layoutIngredientes.getChildCount(); i++) {
            View fila = layoutIngredientes.getChildAt(i);
            if (fila == null) {
                continue;
            }

            EditText ing = fila.findViewById(R.id.inputIngrediente);
            EditText cant = fila.findViewById(R.id.inputCantidad);

            if (ing != null && cant != null) {
                ingredientesCrudos.add(new String[]{ing.getText().toString(), cant.getText().toString()});
            }
        }

        List<String> pasosCrudos = new ArrayList<>();
        for (int i = 0; i < layoutPasos.getChildCount(); i++) {
            View fila = layoutPasos.getChildAt(i);
            if (fila == null) {
                continue;
            }

            EditText descPaso = fila.findViewById(R.id.inputPaso);
            if (descPaso != null) {
                pasosCrudos.add(descPaso.getText().toString());
            }
        }

        // pool de hilos
        ExecutorService executor = Executors.newFixedThreadPool(3);

        executor.execute(() -> {
            // armado de objeto completo en 2ndo plano
            Map<String, Object> receta = new HashMap<>();
            receta.put("usuario", user.getUid());
            receta.put("img", imgCloudinary);
            receta.put("categorias", new ArrayList<>(categoriasSeleccionadas));
            receta.put("nombre", nombreText);
            receta.put("tiempo", tiempoText);
            receta.put("descripcion", descText);
            receta.put("preparacion", pasosCrudos);

            // mapeo de ingredientes para firebase
            List<Map<String, String>> ingredientes = new ArrayList<>();
            for (String[] itemCrudo : ingredientesCrudos) {
                Map<String, String> item = new HashMap<>();
                item.put("nombre", itemCrudo[0]);
                item.put("cantidad", itemCrudo[1]);
                ingredientes.add(item);
            }
            receta.put("ingredientes", ingredientes);

            // hilo principal para bd y pantalla
            runOnUiThread(() -> {
                if (recetaIdEditar != null) {
                    // actualizar receta
                    db.collection("recetas").document(recetaIdEditar).set(receta)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Receta actualizada", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(PublicarActivity.this, InicioActivity.class));
                                finish();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al actualizar receta :(", Toast.LENGTH_LONG).show();
                            });
                } else {
                    // nueva receta
                    db.collection("recetas").add(receta)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Receta creada :)", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(PublicarActivity.this, InicioActivity.class));
                                finish();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al crear receta :(", Toast.LENGTH_LONG).show();
                            });
                }
            });
        });

        // apago executor
        executor.shutdown();
    }

    private void datosEditar(String id) {
        db.collection("recetas").document(id).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                inputNombre.setText(document.getString("nombre"));
                inputTiempo.setText(document.getString("tiempo"));
                inputDescripcion.setText(document.getString("descripcion"));

                // cargar img con glide
                imagenActual = document.getString("img");
                if (imagenActual != null && !imagenActual.isEmpty() && !imagenActual.equals("placeholder_receta.png")) {
                    com.bumptech.glide.Glide.with(this)
                            .load(imagenActual)
                            .into(imgReceta);

                    imgReceta.setVisibility(View.VISIBLE);
                    layoutPlaceholder.setVisibility(View.GONE);
                    layoutBotonesFoto.setVisibility(View.VISIBLE);
                }

                // limpiar layouts
                layoutIngredientes.removeAllViews();
                layoutPasos.removeAllViews();

                // reconstruir ingredientes
                List<Map<String, String>> ingredientes = (List<Map<String, String>>) document.get("ingredientes");
                if (ingredientes != null) {
                    for (Map<String, String> ing : ingredientes) {
                        View fila = getLayoutInflater().inflate(R.layout.item_ingrediente, layoutIngredientes, false);

                        EditText inputIngrediente = fila.findViewById(R.id.inputIngrediente);
                        EditText inputCantidad = fila.findViewById(R.id.inputCantidad);
                        ImageButton btnEliminarIngrediente = fila.findViewById(R.id.btnEliminarIngrediente);

                        inputIngrediente.setText(ing.get("nombre"));
                        inputCantidad.setText(ing.get("cantidad"));

                        btnEliminarIngrediente.setOnClickListener(v -> layoutIngredientes.removeView(fila));
                        layoutIngredientes.addView(fila);
                    }
                }

                // reconstruir pasos
                List<String> preparacion = (List<String>) document.get("preparacion");
                if (preparacion != null) {
                    for (String paso : preparacion) {
                        View fila = getLayoutInflater().inflate(R.layout.item_preparacion, layoutPasos, false);

                        EditText descPaso = fila.findViewById(R.id.inputPaso);
                        ImageButton btnEliminarPaso = fila.findViewById(R.id.btnEliminarPaso);

                        descPaso.setText(paso);

                        btnEliminarPaso.setOnClickListener(v -> {
                            layoutPasos.removeView(fila);
                            actualizarNumerosPasos();
                        });

                        layoutPasos.addView(fila);
                    }
                    actualizarNumerosPasos();
                }

                // cargar categorías
                List<String> categorias = (List<String>) document.get("categorias");
                if (categorias != null) {
                    categoriasSeleccionadas.clear();
                    categoriasSeleccionadas.addAll(categorias);
                    actualizarChipsUI();
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al cargar la receta", Toast.LENGTH_SHORT).show();
        });
    }

    private void actualizarChipsUI() {
        for (int i = 0; i < layoutCategorias.getChildCount(); i++) {
            View vista = layoutCategorias.getChildAt(i);
            TextView chip = vista.findViewById(R.id.chipCategoria);

            if (chip != null) {
                String nombre = chip.getText().toString();

                if (categoriasSeleccionadas.contains(nombre)) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected);
                    chip.setTextColor(getColor(R.color.white));
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_outline);
                    chip.setTextColor(getColor(R.color.black));
                }
            }
        }
    }
}

