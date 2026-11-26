
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
    int sceneBrushColor = Color.rgb(55, 135, 90);
    boolean sceneEraseMode = false;
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
        if (scene3DView != null) scene3DView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scene3DView != null) scene3DView.onResume();
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
        if (scene3DView != null) {
            scene3DView.onPause();
            scene3DView = null;
        }
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
    Scene3DView scene3DView;

    void scenes() {
        String project = activeProjectName();
        String type = getPreferences(MODE_PRIVATE)
                .getString("project_type_" + project, "2D");
        if ("3D".equals(type)) {
            heading("Editor de escenas 3D",
                    "Vista 3D OpenGL ES: coloca cubos y observa la escena desde una cámara en perspectiva.");
            scene3DView = new Scene3DView();
            content.addView(scene3DView, new LinearLayout.LayoutParams(-1, 390));
            scene3DView.onResume();
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.addView(button("＋ CUBO", () -> scene3DView.addCube()),
                    new LinearLayout.LayoutParams(0, -2, 1));
            actions.addView(button("BORRAR TODO", () -> scene3DView.clearCubes()),
                    new LinearLayout.LayoutParams(0, -2, 1));
            content.addView(actions);
            content.addView(button("GUARDAR ESCENA", () -> {
                scene3DView.persist();
                Toast.makeText(this, "Escena 3D guardada", Toast.LENGTH_SHORT).show();
            }));
            card("Controles", "Usa ＋ CUBO para añadir geometría cúbica. La escena se guarda en este proyecto.");
            card("Estado del motor", "Renderizador 3D inicial con OpenGL ES 2.0. Todavía faltan materiales, iluminación avanzada, físicas, modelos y exportación.");
            return;
        }

        heading("Editor de escenas 2D",
                "Editor táctil por cuadrícula. Toca una celda para colocar un bloque.");
        scenePreview = new ScenePreview();
        content.addView(scenePreview, new LinearLayout.LayoutParams(-1, 390));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(button("＋ BLOQUE", () -> scenePreview.addBlock()),
                new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(button("BORRAR TODO", () -> scenePreview.clearBlocks()),
                new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(actions);
        LinearLayout palette = new LinearLayout(this);
        palette.setOrientation(LinearLayout.HORIZONTAL);
        palette.addView(button("VERDE", () -> setSceneBrush(Color.rgb(55, 135, 90), false)),
                new LinearLayout.LayoutParams(0, -2, 1));
        palette.addView(button("AZUL", () -> setSceneBrush(Color.rgb(55, 115, 210), false)),
                new LinearLayout.LayoutParams(0, -2, 1));
        palette.addView(button("ROJO", () -> setSceneBrush(Color.rgb(205, 75, 75), false)),
                new LinearLayout.LayoutParams(0, -2, 1));
        palette.addView(button("BORRAR", () -> setSceneBrush(sceneBrushColor, true)),
                new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(palette);
        content.addView(button("GUARDAR ESCENA", () -> {
            scenePreview.persist();
            Toast.makeText(this, "Escena guardada", Toast.LENGTH_SHORT).show();
        }));
        card("Cómo usar", "Toca cualquier celda de la cuadrícula para colocar un bloque. Los cambios se guardan automáticamente.");
        card("Estado", "Editor 2D por cuadrícula. El modo 3D usa un renderizador OpenGL ES independiente.");
    }

    void setSceneBrush(int color, boolean erase) {
        sceneBrushColor = color;
        sceneEraseMode = erase;
        if (scenePreview != null) scenePreview.invalidate();
        Toast.makeText(this, erase ? "Modo borrador activado" : "Pincel seleccionado",
                Toast.LENGTH_SHORT).show();
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
            "# Demo de El Grafisript · variables, condiciones y bucles\n"+
            "crear_mundo \"Mi mundo\"\n"+
            "variable puntos = 2 + 3\n"+
            "si puntos > 3 {\n"+
            "    mostrar_mensaje \"¡Ganaste!\"\n"+
            "} sino {\n"+
            "    mostrar_mensaje \"Sigue intentando\"\n"+
            "}\n"+
            "repetir 3 {\n"+
            "    crear_bloque piedra 1 0 1\n"+
            "}\n"+
            "crear_personaje Jugador\n"+
            "jugador.mover(2, 1)\n"+
            "mostrar_mensaje \"¡Jugador listo!\"\n"+
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

        card("Sintaxis del lenguaje",
                "variable puntos = 2 + 3\\n"+
                "si puntos > 3 {\\n"+
                "    mostrar_mensaje \\"Ganaste: ${puntos}\\"\\n"+
                "} sino {\\n"+
                "    mostrar_mensaje \\"Sigue intentando\\"\\n"+
                "}\\n"+
                "repetir 3 {\\n"+
                "    crear_bloque piedra 1 0 1\\n"+
                "}\\n"+
                "crear_personaje Jugador\\n"+
                "jugador.mover(2, 1)\\n"+
                "mostrar_mensaje \\"Listo\\"");
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
        String[] lines = codeEditor.getText().toString().split("\\r?\\n");
        StringBuilder output = new StringBuilder();
        java.util.Map<String, Double> variables = new java.util.HashMap<>();
        int[] cursor = new int[]{0};
        int executed = executeGrafisriptBlock(lines, cursor, variables, output, 0);
        output.append("\nPrograma finalizado. Instrucciones ejecutadas: ").append(executed);
        runtimeOutput.setText(output.toString());
    }

    int executeGrafisriptBlock(String[] lines, int[] cursor,
            java.util.Map<String, Double> variables, StringBuilder output, int depth) {
        if (depth > 12) {
            output.append("Error: demasiados bloques anidados (máximo 12).\n");
            return 0;
        }
        int executed = 0;
        while (cursor[0] < lines.length) {
            int lineNumber = cursor[0] + 1;
            String line = lines[cursor[0]++].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.equals("}") || line.equals("fin")) return executed;

            if (line.startsWith("si ") && line.endsWith("{")) {
                String condition = line.substring(3, line.length() - 1).trim();
                int bodyStart = cursor[0];
                int bodyEnd = findGrafisriptBlockEnd(lines, bodyStart);
                if (bodyEnd < 0) {
                    output.append("Línea ").append(lineNumber).append(": falta cerrar el bloque si.\n");
                    return executed;
                }
                boolean result = evaluateGrafisriptCondition(condition, variables);
                if (result) {
                    executed += executeGrafisriptBlock(lines, cursor, variables, output, depth + 1);
                } else {
                    cursor[0] = bodyEnd + 1;
                }
                if (cursor[0] < lines.length && lines[cursor[0]].trim().startsWith("sino")) {
                    String otherwise = lines[cursor[0]++].trim();
                    if (!otherwise.equals("sino {")) {
                        output.append("Línea ").append(cursor[0]).append(": usa sino {.\n");
                        continue;
                    }
                    int elseEnd = findGrafisriptBlockEnd(lines, cursor[0]);
                    if (elseEnd < 0) {
                        output.append("Error: falta cerrar el bloque sino.\n");
                        return executed;
                    }
                    if (!result) {
                        executed += executeGrafisriptBlock(lines, cursor, variables, output, depth + 1);
                    } else {
                        cursor[0] = elseEnd + 1;
                    }
                }
                continue;
            }

            if (line.startsWith("repetir ") && line.endsWith("{")) {
                String amountText = line.substring(8, line.length() - 1).trim();
                int bodyStart = cursor[0];
                int bodyEnd = findGrafisriptBlockEnd(lines, bodyStart);
                if (bodyEnd < 0) {
                    output.append("Línea ").append(lineNumber).append(": falta cerrar repetir.\n");
                    return executed;
                }
                int count;
                try {
                    count = (int)evaluateGrafisriptExpression(amountText, variables);
                } catch (Exception ex) {
                    output.append("Línea ").append(lineNumber).append(": repetición inválida.\n");
                    cursor[0] = bodyEnd + 1;
                    continue;
                }
                if (count < 0 || count > 1000) {
                    output.append("Línea ").append(lineNumber).append(": repetir acepta de 0 a 1000.\n");
                    cursor[0] = bodyEnd + 1;
                    continue;
                }
                String[] body = java.util.Arrays.copyOfRange(lines, bodyStart, bodyEnd);
                for (int n = 0; n < count; n++) {
                    int[] nestedCursor = new int[]{0};
                    executed += executeGrafisriptBlock(body, nestedCursor, variables, output, depth + 1);
                }
                cursor[0] = bodyEnd + 1;
                continue;
            }

            if (line.startsWith("variable ")) line = line.substring(9).trim();
            int assignment = line.indexOf('=');
            if (assignment > 0 && !line.contains("==") && !line.contains(">=") &&
                    !line.contains("<=") && !line.contains("!=")) {
                String name = line.substring(0, assignment).trim();
                if (name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    try {
                        double value = evaluateGrafisriptExpression(
                                line.substring(assignment + 1).trim(), variables);
                        variables.put(name, value);
                        output.append(name).append(" = ").append(value).append('\n');
                        executed++;
                    } catch (Exception ex) {
                        output.append("Línea ").append(lineNumber)
                                .append(": expresión inválida: ").append(ex.getMessage()).append('\n');
                    }
                    continue;
                }
            }

            if (line.startsWith("mostrar_mensaje ") || line.startsWith("imprimir ")) {
                int cut = line.startsWith("imprimir ") ? 9 : 16;
                String message = unquote(line.substring(cut).trim());
                for (java.util.Map.Entry<String, Double> entry : variables.entrySet()) {
                    message = message.replace("${" + entry.getKey() + "}",
                            String.valueOf(entry.getValue()));
                }
                output.append(message).append('\n');
                executed++;
                continue;
            }

            if (executeCharacterCommand(line, output, lineNumber - 1)) {
                executed++;
                continue;
            }

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
                        output.append("Línea ").append(lineNumber)
                                .append(": uso: crear_bloque tipo x y z\n");
                        break;
                    }
                    try {
                        int bx = (int)evaluateGrafisriptExpression(parts[1], variables);
                        int by = (int)evaluateGrafisriptExpression(parts[2], variables);
                        int bz = (int)evaluateGrafisriptExpression(parts[3], variables);
                        runtimeBlockCount++;
                        addSceneBlock(bx, bz);
                        output.append("Bloque ").append(parts[0]).append(" en (")
                                .append(bx).append(", ").append(by).append(", ").append(bz).append(")\n");
                        refreshScenePreview();
                        executed++;
                    } catch (Exception ex) {
                        output.append("Línea ").append(lineNumber)
                                .append(": coordenadas inválidas.\n");
                    }
                    break;
                }
                case "limpiar_mundo":
                    runtimeBlockCount = 0;
                    clearProjectScene();
                    refreshScenePreview();
                    output.append("Mundo limpiado.\n");
                    executed++;
                    break;
                case "ver_bloques":
                    output.append("Bloques registrados: ").append(runtimeBlockCount).append('\n');
                    executed++;
                    break;
                case "ayuda":
                    output.append("Comandos: crear_mundo, crear_bloque, crear_personaje, jugador.mover, ")
                            .append("jugador.saltar, mostrar_mensaje, variable, si, sino, repetir, ver_bloques.\n");
                    executed++;
                    break;
                default:
                    output.append("Línea ").append(lineNumber)
                            .append(": comando desconocido: ").append(command).append('\n');
                    break;
            }
        }
        return executed;
    }

    int findGrafisriptBlockEnd(String[] lines, int start) {
        int nesting = 1;
        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if ((line.startsWith("si ") || line.startsWith("repetir ") || line.equals("sino {")) && line.endsWith("{")) nesting++;
            if (line.equals("}") || line.equals("fin")) {
                nesting--;
                if (nesting == 0) return i;
            }
        }
        return -1;
    }

    boolean evaluateGrafisriptCondition(String condition, java.util.Map<String, Double> variables) {
        String[] operators = {">=", "<=", "==", "!=", ">", "<"};
        for (String operator : operators) {
            int at = condition.indexOf(operator);
            if (at > 0) {
                double left = evaluateGrafisriptExpression(condition.substring(0, at).trim(), variables);
                double right = evaluateGrafisriptExpression(condition.substring(at + operator.length()).trim(), variables);
                switch (operator) {
                    case ">=": return left >= right;
                    case "<=": return left <= right;
                    case "==": return left == right;
                    case "!=": return left != right;
                    case ">": return left > right;
                    case "<": return left < right;
                }
            }
        }
        return evaluateGrafisriptExpression(condition, variables) != 0;
    }

    double evaluateGrafisriptExpression(String expression, java.util.Map<String, Double> variables) {
        return new GrafisriptExpression(expression, variables).parse();
    }

    class GrafisriptExpression {
        final String source;
        final java.util.Map<String, Double> variables;
        int position = 0;
        GrafisriptExpression(String source, java.util.Map<String, Double> variables) {
            this.source = source;
            this.variables = variables;
        }
        double parse() {
            double value = addSubtract();
            skipSpaces();
            if (position != source.length()) throw new IllegalArgumentException("símbolo inesperado");
            return value;
        }
        double addSubtract() {
            double value = multiplyDivide();
            while (true) {
                skipSpaces();
                if (take('+')) value += multiplyDivide();
                else if (take('-')) value -= multiplyDivide();
                else return value;
            }
        }
        double multiplyDivide() {
            double value = atom();
            while (true) {
                skipSpaces();
                if (take('*')) value *= atom();
                else if (take('/')) {
                    double divisor = atom();
                    if (divisor == 0) throw new IllegalArgumentException("división por cero");
                    value /= divisor;
                } else return value;
            }
        }
        double atom() {
            skipSpaces();
            if (take('(')) {
                double value = addSubtract();
                skipSpaces();
                if (!take(')')) throw new IllegalArgumentException("falta )");
                return value;
            }
            int begin = position;
            if (position < source.length() && (source.charAt(position) == '-' || source.charAt(position) == '+')) position++;
            while (position < source.length() &&
                    (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) position++;
            if (position > begin) return Double.parseDouble(source.substring(begin, position));
            while (position < source.length() &&
                    (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_')) position++;
            if (position == begin) throw new IllegalArgumentException("se esperaba un número o variable");
            String name = source.substring(begin, position);
            if (!variables.containsKey(name)) throw new IllegalArgumentException("variable no definida: " + name);
            return variables.get(name);
        }
        void skipSpaces() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
        }
        boolean take(char expected) {
            if (position < source.length() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }
    }

    String activeProjectName() {
        return getPreferences(MODE_PRIVATE)
                .getString("active_project", "Mi primer proyecto");
    }

    boolean executeCharacterCommand(String line, StringBuilder output, int lineIndex) {
        String project = activeProjectName();
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        try {
            if (line.startsWith("crear_personaje ")) {
                String name = line.substring("crear_personaje ".length()).trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Falta el nombre del personaje");
                prefs.edit().putString("character_name_" + project, name)
                        .putFloat("character_x_" + project, 0f)
                        .putFloat("character_z_" + project, 0f)
                        .putInt("character_health_" + project, 100).apply();
                output.append("Personaje creado: ").append(name).append(" · vida 100\n");
                refreshScenePreview();
                return true;
            }
            if (line.startsWith("jugador.mover(") && line.endsWith(")")) {
                String[] values = line.substring(14, line.length() - 1).split(",");
                if (values.length != 2) throw new IllegalArgumentException("Uso: jugador.mover(x, z)");
                float dx = Float.parseFloat(values[0].trim());
                float dz = Float.parseFloat(values[1].trim());
                float x = prefs.getFloat("character_x_" + project, 0f) + dx;
                float z = prefs.getFloat("character_z_" + project, 0f) + dz;
                prefs.edit().putFloat("character_x_" + project, x)
                        .putFloat("character_z_" + project, z).apply();
                output.append("Jugador movido a (").append(x).append(", ").append(z).append(")\n");
                refreshScenePreview();
                return true;
            }
            if (line.startsWith("jugador.saltar(") && line.endsWith(")")) {
                float height = Float.parseFloat(line.substring(15, line.length() - 1).trim());
                if (height <= 0 || height > 100) throw new IllegalArgumentException("La altura debe ser mayor que 0 y hasta 100");
                prefs.edit().putFloat("character_last_jump_" + project, height).apply();
                output.append("Salto programado: altura ").append(height).append("\n");
                return true;
            }
            if (line.startsWith("jugador.girar(") && line.endsWith(")")) {
                float degrees = Float.parseFloat(line.substring(14, line.length() - 1).trim());
                prefs.edit().putFloat("character_rotation_" + project, degrees).apply();
                output.append("Jugador girado: ").append(degrees).append(" grados\n");
                refreshScenePreview();
                return true;
            }
            if (line.startsWith("jugador.vida(") && line.endsWith(")")) {
                int health = Integer.parseInt(line.substring(13, line.length() - 1).trim());
                if (health < 0 || health > 1000) throw new IllegalArgumentException("Vida permitida: 0 a 1000");
                prefs.edit().putInt("character_health_" + project, health).apply();
                output.append("Vida del jugador: ").append(health).append("\n");
                return true;
            }
        } catch (Exception ex) {
            output.append("Línea ").append(lineIndex + 1).append(": ")
                    .append(ex.getMessage() == null ? "parámetro inválido" : ex.getMessage())
                    .append("\n");
            return true;
        }
        return false;
    }

    void refreshScenePreview() {
        if (scenePreview != null) scenePreview.invalidate();
        if (scene3DView != null) scene3DView.refresh();
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
        if (result.length() >= 2 && result.charAt(0) == '"' &&
                result.charAt(result.length() - 1) == '"') {
            return result.substring(1, result.length() - 1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
        }
        return result;
    }

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
                if (sceneEraseMode) {
                    for (int i = blocks.size() - 1; i >= 0; i--) {
                        Point block = blocks.get(i);
                        if (block.x == col && block.y == row) {
                            blocks.remove(i);
                            break;
                        }
                    }
                } else {
                    blocks.add(new Point(col, row));
                }
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
                paint.setColor(sceneBrushColor);
                canvas.drawRect(left, top, right, bottom, paint);
                paint.setColor(Color.rgb(175, 220, 255));
                canvas.drawRect(left, top, right, top + 5, paint);
                paint.setColor(Color.rgb(20, 35, 55));
                canvas.drawRect(left, bottom - 5, right, bottom, paint);
            }

            paint.setColor(white);
            paint.setTextSize(12);
            String active = activeProjectName();
            String character = getPreferences(MODE_PRIVATE)
                    .getString("character_name_" + active, "");
            if (!character.isEmpty()) {
                float px = getPreferences(MODE_PRIVATE).getFloat("character_x_" + active, 0f);
                float pz = getPreferences(MODE_PRIVATE).getFloat("character_z_" + active, 0f);
                int cx = Math.max(0, Math.min(9, Math.round(px)));
                int cz = Math.max(0, Math.min(9, Math.round(pz)));
                float centerX = (cx + 0.5f) * cellW;
                float centerY = (cz + 0.5f) * cellH;
                paint.setColor(Color.rgb(65, 145, 255));
                canvas.drawCircle(centerX, centerY, Math.min(cellW, cellH) * 0.32f, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(10);
                canvas.drawText(character, centerX - 18, centerY - 12, paint);
            }

            canvas.drawText("ESCENA · " + blocks.size() + " BLOQUES", 12, 20, paint);
        }
    }



    class Scene3DView extends android.opengl.GLSurfaceView {
        final Scene3DRenderer renderer;
        final java.util.ArrayList<Point> cubes = new java.util.ArrayList<>();

        Scene3DView() {
            super(BlockForgeActivity.this);
            setEGLContextClientVersion(2);
            renderer = new Scene3DRenderer();
            setRenderer(renderer);
            setRenderMode(android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            setBackgroundColor(Color.rgb(7, 13, 23));
            String saved = getPreferences(MODE_PRIVATE)
                    .getString("scene_blocks_" + activeProjectName(), "");
            if (!saved.isEmpty()) {
                for (String pair : saved.split(";")) {
                    String[] xy = pair.split(",");
                    if (xy.length == 2) {
                        try {
                            cubes.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
            refresh();
        }

        void refresh() {
            float[] positions = new float[cubes.size() * 2];
            for (int i = 0; i < cubes.size(); i++) {
                positions[i * 2] = cubes.get(i).x;
                positions[i * 2 + 1] = cubes.get(i).y;
            }
            renderer.setBlocks(positions);
        }

        void persist() {
            StringBuilder data = new StringBuilder();
            for (Point p : cubes) {
                if (data.length() > 0) data.append(';');
                data.append(p.x).append(',').append(p.y);
            }
            getPreferences(MODE_PRIVATE).edit()
                    .putString("scene_blocks_" + activeProjectName(), data.toString()).apply();
        }

        void addCube() {
            int index = cubes.size();
            cubes.add(new Point(index % 10, (index / 10) % 10));
            persist();
            refresh();
        }

        void clearCubes() {
            cubes.clear();
            persist();
            refresh();
        }
    }

    class Scene3DRenderer implements android.opengl.GLSurfaceView.Renderer {
        private final String vertexShader =
                "attribute vec4 aPosition; uniform mat4 uMvp; void main(){ gl_Position=uMvp*aPosition; }";
        private final String fragmentShader =
                "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }";
        private java.nio.FloatBuffer cubeBuffer;
        private int program;
        private int positionHandle;
        private int matrixHandle;
        private int colorHandle;
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] viewProjection = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];
        private volatile float[] blocks = new float[0];
        private final float[] vertices = {
            -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
             0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f
        };

        Scene3DRenderer() {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(java.nio.ByteOrder.nativeOrder());
            cubeBuffer = bb.asFloatBuffer();
            cubeBuffer.put(vertices).position(0);
        }

        void setBlocks(float[] value) { blocks = value; }

        int compileShader(int type, String source) {
            int shader = android.opengl.GLES20.glCreateShader(type);
            android.opengl.GLES20.glShaderSource(shader, source);
            android.opengl.GLES20.glCompileShader(shader);
            int[] status = new int[1];
            android.opengl.GLES20.glGetShaderiv(shader, android.opengl.GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == 0) {
                String error = android.opengl.GLES20.glGetShaderInfoLog(shader);
                android.opengl.GLES20.glDeleteShader(shader);
                throw new RuntimeException("Shader 3D inválido: " + error);
            }
            return shader;
        }

        @Override
        public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl,
                                      javax.microedition.khronos.egl.EGLConfig config) {
            android.opengl.GLES20.glClearColor(0.03f, 0.05f, 0.09f, 1f);
            android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_DEPTH_TEST);
            int vs = compileShader(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShader);
            int fs = compileShader(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShader);
            program = android.opengl.GLES20.glCreateProgram();
            android.opengl.GLES20.glAttachShader(program, vs);
            android.opengl.GLES20.glAttachShader(program, fs);
            android.opengl.GLES20.glLinkProgram(program);
            android.opengl.GLES20.glDeleteShader(vs);
            android.opengl.GLES20.glDeleteShader(fs);
            int[] status = new int[1];
            android.opengl.GLES20.glGetProgramiv(program, android.opengl.GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] == 0) throw new RuntimeException("No se pudo enlazar el motor 3D.");
            positionHandle = android.opengl.GLES20.glGetAttribLocation(program, "aPosition");
            matrixHandle = android.opengl.GLES20.glGetUniformLocation(program, "uMvp");
            colorHandle = android.opengl.GLES20.glGetUniformLocation(program, "uColor");
        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            android.opengl.GLES20.glViewport(0, 0, width, height);
            float ratio = (float) width / Math.max(1, height);
            android.opengl.Matrix.perspectiveM(projection, 0, 48f, ratio, 0.1f, 100f);
            android.opengl.Matrix.setLookAtM(view, 0, 9f, 11f, 13f, 0f, 0f, 0f, 0f, 1f, 0f);
            android.opengl.Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT |
                    android.opengl.GLES20.GL_DEPTH_BUFFER_BIT);
            android.opengl.GLES20.glUseProgram(program);
            android.opengl.GLES20.glEnableVertexAttribArray(positionHandle);
            android.opengl.GLES20.glVertexAttribPointer(positionHandle, 3,
                    android.opengl.GLES20.GL_FLOAT, false, 0, cubeBuffer);
            float[] current = blocks;
            for (int i = 0; i < current.length; i += 2) {
                android.opengl.Matrix.setIdentityM(model, 0);
                android.opengl.Matrix.translateM(model, 0,
                        current[i] - 4.5f, 0.5f, current[i + 1] - 4.5f);
                android.opengl.Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0);
                android.opengl.GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvp, 0);
                android.opengl.GLES20.glUniform4f(colorHandle, 0.20f, 0.68f, 0.42f, 1f);
                cubeBuffer.position(0);
                android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLES, 0, 36);
            }
            android.opengl.GLES20.glDisableVertexAttribArray(positionHandle);
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