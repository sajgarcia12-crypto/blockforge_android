
package com.blockforge.studio;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.*;
import android.graphics.Typeface;

public class BlockForgeActivity extends Activity {

    LinearLayout root, content, navigation;
    int background = Color.rgb(12,16,26);
    int panel = Color.rgb(24,32,47);
    int blue = Color.rgb(55,135,255);
    int white = Color.WHITE;
    int gray = Color.rgb(160,175,195);
    EditText codeEditor;
    TextView runtimeOutput;
    int runtimeBlockCount = 0;
    String runtimeWorldName = "Mi mundo";

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        buildInterface();
        home();
    }

    @Override
    protected void onPause() {
        saveCurrentScript();
        super.onPause();
    }

    GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(18);
        return d;
    }

    TextView label(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(1);
        return l;
    }

    TextView button(String value, Runnable action) {
        TextView b = label(value, 14, white);
        b.setGravity(Gravity.CENTER);
        b.setPadding(15,16,15,16);
        b.setBackground(shape(blue));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    void buildInterface() {
        root = vertical();
        root.setBackgroundColor(background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(18,18,18,18);

        TextView logo = label("⬡",34,blue);
        header.addView(logo);

        LinearLayout brand = vertical();
        brand.setPadding(12,0,0,0);
        brand.addView(label("BLOCKFORGE",20,white));
        brand.addView(label("STUDIO MOBILE",10,gray));
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(label("● ONLINE",11,Color.GREEN));

        root.addView(header);

        View separator = new View(this);
        separator.setBackgroundColor(panel);
        root.addView(separator,new LinearLayout.LayoutParams(-1,2));

        ScrollView scroll = new ScrollView(this);
        content = vertical();
        content.setPadding(18,20,18,25);
        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        navigation = new LinearLayout(this);
        navigation.setBackgroundColor(Color.rgb(18,24,36));

        String[] names = {"Inicio","Proyectos","Escenas","Grafisript","Recursos","Ajustes"};
        String[] icons = {"⌂","▦","◇","{ }","▤","⚙"};

        for(int i=0;i<names.length;i++) {
            final int page=i;
            LinearLayout item=vertical();
            item.setGravity(Gravity.CENTER);
            item.setPadding(3,12,3,12);
            item.addView(label(icons[i],21,blue));
            TextView name=label(names[i],10,gray);
            item.addView(name);
            item.setOnClickListener(v -> openPage(page));
            navigation.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }

        root.addView(navigation);
        setContentView(root);
    }

    void openPage(int page) {
        saveCurrentScript();
        content.removeAllViews();
        switch(page) {
            case 0: home(); break;
            case 1: projects(); break;
            case 2: scenes(); break;
            case 3: grafisript(); break;
            case 4: resources(); break;
            case 5: settings(); break;
        }
    }

    void heading(String title,String subtitle) {
        content.addView(label(title,27,white));
        TextView sub=label(subtitle,13,gray);
        sub.setPadding(0,7,0,20);
        content.addView(sub);
    }

    void card(String title,String description) {
        LinearLayout c=vertical();
        c.setPadding(17,17,17,17);
        c.setBackground(shape(panel));

        c.addView(label(title,17,white));
        TextView desc=label(description,13,gray);
        desc.setPadding(0,9,0,0);
        c.addView(desc);

        LinearLayout.LayoutParams lp=
            new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=13;
        content.addView(c,lp);
    }

    void home() {
        heading("Bienvenido a BlockForge",
                "Tu espacio de creación de videojuegos.");

        LinearLayout hero=vertical();
        hero.setPadding(20,24,20,24);
        hero.setBackground(shape(Color.rgb(25,57,100)));

        hero.addView(label("BLOCKFORGE STUDIO",13,
                Color.rgb(160,205,255)));
        TextView big=label("CREA. PROGRAMA.\nCONSTRUYE.",29,white);
        big.setPadding(0,14,0,14);
        hero.addView(big);

        hero.addView(label(
                "Desarrolla tus propios mundos y videojuegos.",
                14,white));

        TextView create=button("＋  NUEVO PROYECTO",
                () -> openPage(1));

        LinearLayout.LayoutParams cp=
            new LinearLayout.LayoutParams(-1,-2);
        cp.topMargin=22;
        hero.addView(create,cp);

        content.addView(hero);

        heading("ESPACIO DE TRABAJO",
                "Herramientas principales.");

        card("◇  Editor de escenas",
                "Organiza objetos y construye tus mundos 2D y 3D.");

        card("{ }  El Grafisript",
                "Programa la lógica de tus videojuegos.");

        card("▦  Constructor de bloques",
                "Diseña mundos mediante bloques reutilizables.");

        card("▤  Recursos",
                "Gestiona texturas, modelos y sonidos.");
    }

    void projects() {
        heading("Mis proyectos",
                "Tus proyectos guardados en este dispositivo.");

        content.addView(button("＋ CREAR PROYECTO",
                () -> newProject()));

        java.util.Set<String> saved = getPreferences(MODE_PRIVATE)
                .getStringSet("blockforge_projects", new java.util.HashSet<>());
        java.util.ArrayList<String> names = new java.util.ArrayList<>(saved);
        java.util.Collections.sort(names);

        if (names.isEmpty()) {
            card("Todavía no hay proyectos",
                    "Pulsa CREAR PROYECTO para comenzar. Tus proyectos se guardan en este dispositivo.");
        } else {
            for (String name : names) {
                content.addView(button("ABRIR  ·  " + name, () -> {
                    getPreferences(MODE_PRIVATE).edit()
                            .putString("active_project", name).apply();
                    Toast.makeText(this, "Proyecto activo: " + name,
                            Toast.LENGTH_SHORT).show();
                    openPage(3);
                }));
            }
        }
    }

    void newProject() {
        LinearLayout form = vertical();
        form.setPadding(20, 8, 20, 0);
        EditText input = new EditText(this);
        input.setHint("Nombre del proyecto");
        form.addView(input);

        TextView typeLabel = label("Tipo de proyecto", 13, gray);
        typeLabel.setPadding(0, 12, 0, 6);
        form.addView(typeLabel);
        Spinner type = new Spinner(this);
        String[] types = {"2D", "3D"};
        type.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, types));
        form.addView(type);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Nuevo proyecto")
            .setView(form)
            .setPositiveButton("Crear", (d, w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Escribe un nombre para el proyecto",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                String projectType = type.getSelectedItem().toString();
                android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                java.util.Set<String> names = new java.util.HashSet<>(
                        prefs.getStringSet("blockforge_projects", new java.util.HashSet<>()));
                names.add(name);
                prefs.edit()
                        .putStringSet("blockforge_projects", names)
                        .putString("project_type_" + name, projectType)
                        .putString("active_project", name)
                        .apply();
                Toast.makeText(this, "Proyecto " + projectType + " creado: " + name,
                        Toast.LENGTH_LONG).show();
                openPage(3);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    ScenePreview scenePreview;

    void scenes() {
        heading("Editor de escenas",
                "Editor táctil 2D experimental. Toca una celda para colocar un bloque.");
        scenePreview = new ScenePreview();
        content.addView(scenePreview, new LinearLayout.LayoutParams(-1, 390));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(button("＋ BLOQUE", () -> scenePreview.addBlock()),
                new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(button("BORRAR TODO", () -> scenePreview.clearBlocks()),
                new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(actions);

        content.addView(button("GUARDAR ESCENA", () -> {
            scenePreview.persist();
            Toast.makeText(this, "Escena guardada", Toast.LENGTH_SHORT).show();
        }));
        card("Cómo usar", "Toca cualquier celda de la cuadrícula para colocar un bloque. Los cambios se guardan automáticamente.");
        card("Estado", "Editor 2D por cuadrícula. El renderizado 3D nativo todavía no está conectado a esta interfaz.");
    }

    void grafisript() {
        heading("El Grafisript",
                "Lenguaje de programación de BlockForge.");

        codeEditor=new EditText(this);
        codeEditor.setTextColor(Color.rgb(180,220,255));
        codeEditor.setTextSize(14);
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setGravity(Gravity.TOP);
        codeEditor.setPadding(15,15,15,15);
        codeEditor.setMinLines(15);
        codeEditor.setBackground(shape(Color.rgb(5,10,18)));

        String project = getPreferences(MODE_PRIVATE)
                .getString("active_project", "Mi primer proyecto");
        codeEditor.setText(getPreferences(MODE_PRIVATE).getString(
            "grafisript_" + project,
            "# Proyecto: " + project + "\n"+
            "crear_mundo \"Mi mundo\"\n"+
            "crear_bloque piedra 1 1 1\n"+
            "mostrar_mensaje \"Hola BlockForge\"\n"+
            "ver_bloques\n"
        ));

        content.addView(codeEditor);

        TextView consoleTitle = label("CONSOLA DE EL GRAFISRIPT", 13, gray);
        consoleTitle.setPadding(0, 16, 0, 8);
        content.addView(consoleTitle);

        runtimeOutput = label("Listo. Pulsa EJECUTAR para interpretar tu código.", 13,
                Color.rgb(145, 230, 170));
        runtimeOutput.setTypeface(Typeface.MONOSPACE);
        runtimeOutput.setPadding(14, 14, 14, 14);
        runtimeOutput.setBackground(shape(Color.rgb(5, 10, 18)));
        content.addView(runtimeOutput);

        content.addView(button("GUARDAR SCRIPT", () -> {
            saveCurrentScript();
            Toast.makeText(this, "Script guardado en este dispositivo",
                    Toast.LENGTH_SHORT).show();
        }));

        content.addView(button("▶ EJECUTAR", () -> runGrafisript()));

        card("Comandos disponibles",
                "crear_mundo \"Nombre\"\n"+
                "crear_bloque tipo x y z\n"+
                "mostrar_mensaje \"texto\"\n"+
                "limpiar_mundo\n"+
                "ver_bloques\n"+
                "ayuda");
    }

    void saveCurrentScript() {
        if (codeEditor == null) return;
        String project = getPreferences(MODE_PRIVATE)
                .getString("active_project", "Mi primer proyecto");
        getPreferences(MODE_PRIVATE).edit()
                .putString("grafisript_" + project,
                        codeEditor.getText().toString()).apply();
    }

    void runGrafisript() {
        if (codeEditor == null || runtimeOutput == null) return;

        StringBuilder output = new StringBuilder();
        String[] lines = codeEditor.getText().toString().split("\\r?\n");
        int executed = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int space = line.indexOf(' ');
            String command = space < 0 ? line : line.substring(0, space);
            String args = space < 0 ? "" : line.substring(space + 1).trim();

            switch (command) {
                case "crear_mundo":
                    runtimeWorldName = unquote(args);
                    if (runtimeWorldName.isEmpty()) runtimeWorldName = "Mundo Grafisript";
                    runtimeBlockCount = 0;
                    clearProjectScene();
                    output.append("Mundo creado: ").append(runtimeWorldName).append('\n');
                    executed++;
                    break;

                case "crear_bloque": {
                    String[] parts = args.split("\\s+");
                    if (parts.length != 4) {
                        output.append("Línea ").append(i + 1)
                              .append(": uso: crear_bloque tipo x y z\n");
                        break;
                    }
                    try {
                        int bx = Integer.parseInt(parts[1]);
                        Integer.parseInt(parts[2]);
                        int bz = Integer.parseInt(parts[3]);
                        runtimeBlockCount++;
                        addSceneBlock(bx, bz);
                        output.append("Bloque ").append(parts[0])
                              .append(" agregado en (").append(parts[1]).append(", ")
                              .append(parts[2]).append(", ").append(parts[3]).append(")\n");
                        executed++;
                    } catch (NumberFormatException ex) {
                        output.append("Línea ").append(i + 1)
                              .append(": las coordenadas deben ser números enteros\n");
                    }
                    break;
                }

                case "mostrar_mensaje":
                    output.append(unquote(args)).append('\n');
                    executed++;
                    break;

                case "limpiar_mundo":
                    runtimeBlockCount = 0;
                    output.append("Mundo limpiado\n");
                    executed++;
                    break;

                case "ver_bloques":
                    output.append("Bloques registrados: ").append(runtimeBlockCount).append('\n');
                    executed++;
                    break;

                case "ayuda":
                    output.append("Comandos: crear_mundo, crear_bloque, mostrar_mensaje, ")
                          .append("limpiar_mundo, ver_bloques, ayuda\n");
                    executed++;
                    break;

                default:
                    output.append("Línea ").append(i + 1)
                          .append(": comando desconocido: ").append(command).append('\n');
                    break;
            }
        }

        output.append("\nPrograma finalizado. Instrucciones ejecutadas: ").append(executed);
        runtimeOutput.setText(output.toString());
    }

    void addSceneBlock(int x, int z) {
        String project = getPreferences(MODE_PRIVATE)
                .getString("active_project", "Mi primer proyecto");
        String key = "scene_blocks_" + project;
        String saved = getPreferences(MODE_PRIVATE).getString(key, "");
        String entry = Math.max(0, Math.min(9, x)) + "," +
                Math.max(0, Math.min(9, z));
        String updated = saved.isEmpty() ? entry : saved + ";" + entry;
        getPreferences(MODE_PRIVATE).edit().putString(key, updated).apply();
    }

    void clearProjectScene() {
        String project = getPreferences(MODE_PRIVATE)
                .getString("active_project", "Mi primer proyecto");
        getPreferences(MODE_PRIVATE).edit()
                .putString("scene_blocks_" + project, "").apply();
    }

    String unquote(String value) {
        String result = value.trim();
        if (result.length() >= 2 && result.charAt(0) == '"' && result.charAt(result.length() - 1) == '"') {
        class ScenePreview extends View {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        java.util.ArrayList<Point> blocks = new java.util.ArrayList<>();

        ScenePreview() {
            super(BlockForgeActivity.this);
            setBackground(shape(Color.rgb(7,13,23)));
            String active = getPreferences(MODE_PRIVATE).getString("active_project", "Mi primer proyecto");
            String saved = getPreferences(MODE_PRIVATE).getString("scene_blocks_" + active, "");
            if (!saved.isEmpty()) {
                for (String pair : saved.split(";")) {
                    String[] xy = pair.split(",");
                    if (xy.length == 2) {
                        try {
                            blocks.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
        }

        void persist() {
            StringBuilder data = new StringBuilder();
            for (Point p : blocks) {
                if (data.length() > 0) data.append(';');
                data.append(p.x).append(',').append(p.y);
            }
            String active = getPreferences(MODE_PRIVATE).getString("active_project", "Mi primer proyecto");
            getPreferences(MODE_PRIVATE).edit().putString("scene_blocks_" + active, data.toString()).apply();
        }

        void addBlock() {
            int index = blocks.size();
            blocks.add(new Point(index % 10, (index / 10) % 10));
            persist();
            invalidate();
        }

        void clearBlocks() {
            blocks.clear();
            persist();
            invalidate();
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event) {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                float cellW = Math.max(1f, getWidth() / 10f);
                float cellH = Math.max(1f, getHeight() / 10f);
                int col = Math.max(0, Math.min(9, (int)(event.getX() / cellW)));
                int row = Math.max(0, Math.min(9, (int)(event.getY() / cellH)));
                blocks.add(new Point(col, row));
                persist();
                invalidate();
                performClick();
                return true;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cellW = getWidth() / 10f;
            float cellH = getHeight() / 10f;

            paint.setColor(Color.rgb(32,45,65));
            paint.setStrokeWidth(1f);
            for (int x = 0; x <= 10; x++)
                canvas.drawLine(x * cellW, 0, x * cellW, getHeight(), paint);
            for (int y = 0; y <= 10; y++)
                canvas.drawLine(0, y * cellH, getWidth(), y * cellH, paint);

            for (Point p : blocks) {
                float left = p.x * cellW + 3;
                float top = p.y * cellH + 3;
                float right = left + cellW - 6;
                float bottom = top + cellH - 6;
                paint.setColor(Color.rgb(55,135,90));
                canvas.drawRect(left, top, right, bottom, paint);
                paint.setColor(Color.rgb(115,210,145));
                canvas.drawRect(left, top, right, top + 5, paint);
                paint.setColor(Color.rgb(25,75,50));
                canvas.drawRect(left, bottom - 5, right, bottom, paint);
            }

            paint.setColor(white);
            paint.setTextSize(12);
            canvas.drawText("ESCENA · " + blocks.size() + " BLOQUES", 12, 20, paint);
        }
    }


    static final int PICK_RESOURCE_REQUEST = 7421;

    void resources() {
        heading("Biblioteca de recursos",
                "Importa imágenes, texturas, audio y archivos al almacenamiento privado de BlockForge.");
        content.addView(button("＋ IMPORTAR ARCHIVO", () -> {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_RESOURCE_REQUEST);
        }));
        java.io.File folder = new java.io.File(getFilesDir(), "resources");
        if (!folder.exists()) folder.mkdirs();
        java.io.File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            card("Sin recursos todavía", "Pulsa IMPORTAR ARCHIVO para añadir recursos desde tu dispositivo.");
        } else {
            for (java.io.File file : files) {
                card(file.getName(), (file.length() / 1024) + " KB · Guardado en BlockForge.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_RESOURCE_REQUEST || resultCode != RESULT_OK ||
                data == null || data.getData() == null) return;
        try {
            android.net.Uri uri = data.getData();
            String name = uri.getLastPathSegment();
            if (name == null || name.trim().isEmpty())
                name = "recurso_" + System.currentTimeMillis();
            name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
            java.io.File folder = new java.io.File(getFilesDir(), "resources");
            if (!folder.exists()) folder.mkdirs();
            java.io.File destination = new java.io.File(folder, name);
            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(destination)) {
                if (in == null) throw new java.io.IOException("No se pudo abrir el archivo");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            Toast.makeText(this, "Recurso importado: " + name, Toast.LENGTH_LONG).show();
            openPage(4);
        } catch (Exception ex) {
            Toast.makeText(this, "No se pudo importar: " + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    void settings() {
        heading("Configuración", "Preferencias de BlockForge Studio.");
        card("Apariencia", "Tema oscuro experimental.");
        card("Idioma", "Interfaz actual: español.");
        card("Motor gráfico", "C++ / SDL2. La conexión completa con la interfaz Java sigue pendiente.");
        card("Lenguaje", "El Grafisript · intérprete inicial.");
        card("Información", "BlockForge Studio Mobile · Versión experimental.");
    }

}