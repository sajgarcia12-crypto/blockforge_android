
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
    String codeEditorProjectName = null;
    TextView runtimeOutput;
    int runtimeBlockCount = 0;
    // Datos generales de El Grafisript: texto, listas y categorías.
    java.util.Map<String, String> grafisriptTextVariables = new java.util.HashMap<>();
    java.util.Map<String, java.util.List<String>> grafisriptLists = new java.util.HashMap<>();
    java.util.Map<String, java.util.List<String>> grafisriptCategories = new java.util.HashMap<>();
    int sceneBrushColor = Color.rgb(55, 135, 90);
    boolean sceneEraseMode = false;
    boolean sceneGridVisible = true;
    boolean sceneCollisionsEnabled = true;
    int sceneToolMode = 0; // 0 lápiz, 1 rectángulo, 2 relleno

    boolean physicsRunning = false;
    int targetAnimationFps = 30;
    int sceneLayer = 1;
    boolean cameraPanMode = false;
    boolean scenePlayMode = false;
    android.media.MediaPlayer sceneAudioPlayer;
    boolean sceneAudioPrepared = false;
    boolean activityResumed = false;
    float physicsGravity = 24f;
    float physicsBounce = 0.15f;
    String runtimeWorldName = "Mi mundo";

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        targetAnimationFps = getPreferences(MODE_PRIVATE).getInt("sprite_fps", 30);
        physicsGravity=getPreferences(MODE_PRIVATE).getFloat("physics_gravity",24f);
        physicsBounce=getPreferences(MODE_PRIVATE).getFloat("physics_bounce",0.15f);
        sceneCollisionsEnabled=getPreferences(MODE_PRIVATE).getBoolean("scene_collisions",true);
        buildInterface();
        home();
    }

    @Override
    protected void onPause() {
        activityResumed = false;
        if (sceneAudioPlayer != null && sceneAudioPrepared && sceneAudioPlayer.isPlaying()) sceneAudioPlayer.pause();
        saveCurrentScript();
        if (scene3DView != null) scene3DView.onPause();
        if (scenePreview != null) scenePreview.stopPhysics();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        if (sceneAudioPlayer != null && sceneAudioPrepared && !sceneAudioPlayer.isPlaying()) {
            try { sceneAudioPlayer.start(); } catch (Exception ignored) { }
        }
        if (scene3DView != null) scene3DView.onResume();
        if (scenePreview != null && physicsRunning) scenePreview.startPhysics();
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

    String uiText(String spanish, String english) {
        String lang = getPreferences(MODE_PRIVATE).getString("ui_language", "es");
        return "en".equals(lang) ? english : spanish;
    }

    void showLanguagePicker() {
        String[] labels = {"Español", "English"};
        new android.app.AlertDialog.Builder(this)
            .setTitle("Idioma / Language")
            .setSingleChoiceItems(labels, "en".equals(getPreferences(MODE_PRIVATE).getString("ui_language","es")) ? 1 : 0,
                (dialog, which) -> {
                    getPreferences(MODE_PRIVATE).edit().putString("ui_language", which == 1 ? "en" : "es").apply();
                    dialog.dismiss();
                    buildInterface();
                    openPage(5);
                }).setNegativeButton("Cancelar", null).show();
    }

    android.app.Dialog editorPanel(String title, int gravity, int widthPercent) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout panelView = vertical();
        panelView.setPadding(16, 18, 16, 18);
        panelView.setBackground(shape(Color.rgb(17, 25, 39)));
        TextView heading = label(title, 18, white);
        heading.setPadding(0, 0, 0, 14);
        panelView.addView(heading);
        dialog.setContentView(panelView);
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.max(260, getResources().getDisplayMetrics().widthPixels * widthPercent / 100), -1);
            window.setGravity(gravity);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(Math.max(260, getResources().getDisplayMetrics().widthPixels * widthPercent / 100), -1);
            dialog.getWindow().setGravity(gravity);
        }
        return dialog;
    }

    void showSceneTreePanel() {
        if (scenePreview == null) return;
        android.app.Dialog dialog = editorPanel(uiText("ESCENA · ÁRBOL","SCENE · TREE"), Gravity.LEFT, 86);
        LinearLayout panelView = (LinearLayout)((android.view.ViewGroup)dialog.findViewById(android.R.id.content)).getChildAt(0);
        if (panelView == null) return;
        panelView.addView(label("NODOS Y OBJETOS", 11, Color.rgb(135,180,235)));
        panelView.addView(editorButton("＋ BLOQUE", () -> { scenePreview.addBlock(); dialog.dismiss(); }));
        panelView.addView(editorButton("＋ SPRITE", () -> { dialog.dismiss(); openSpriteDrawer(); }));
        panelView.addView(label("Bloques: " + scenePreview.blocks.size(), 13, gray));
        panelView.addView(label("Sprites: " + scenePreview.sprites.size(), 13, gray));
        int visibleBlocks = Math.min(scenePreview.blocks.size(), 20);
        for (int i = 0; i < visibleBlocks; i++) {
            Point block = scenePreview.blocks.get(i);
            int layer = i < scenePreview.blockLayers.size() ? scenePreview.blockLayers.get(i) : 1;
            panelView.addView(label("▦ Bloque (" + block.x + ", " + block.y + ") · capa " + layer, 12, gray));
        }
        if (scenePreview.blocks.size() > visibleBlocks)
            panelView.addView(label("… y " + (scenePreview.blocks.size() - visibleBlocks) + " bloques más", 11, gray));
        for (int i=0;i<scenePreview.sprites.size();i++) {
            final int index=i;
            SceneSprite sprite=scenePreview.sprites.get(i);
            TextView item=editorButton((scenePreview.selectedSprite==i?"● ":"○ ") + sprite.fileName, () -> {
                scenePreview.selectedSprite=index;
                scenePreview.invalidate();
                dialog.dismiss();
                showInspectorPanel();
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.bottomMargin=6;
            panelView.addView(item,lp);
        }
        panelView.addView(editorButton("CERRAR", dialog::dismiss));
    }

    void showInspectorPanel() {
        if (scenePreview == null) return;
        android.app.Dialog dialog = editorPanel(uiText("INSPECTOR","INSPECTOR"), Gravity.RIGHT, 88);
        LinearLayout panelView = (LinearLayout)((android.view.ViewGroup)dialog.findViewById(android.R.id.content)).getChildAt(0);
        if (panelView == null) return;
        SceneSprite sprite=scenePreview.selectedSpriteObject();
        if(sprite==null) {
            panelView.addView(label("Selecciona un sprite desde el árbol o tocándolo en el lienzo.",13,gray));
        } else {
            panelView.addView(label("OBJETO",11,Color.rgb(135,180,235)));
            panelView.addView(label(sprite.fileName,17,white));
            panelView.addView(label("X: "+sprite.x+" · Y: "+sprite.y+"\nTamaño: "+sprite.width+" × "+sprite.height+" · Giro: "+sprite.rotation+"°\nSólido: "+(sprite.solid?"Sí":"No"),13,gray));
            panelView.addView(editorButton("TRANSFORMAR · POSICIÓN / TAMAÑO / GIRO", () -> {
                android.widget.EditText x=new android.widget.EditText(this); x.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); x.setHint("X"); x.setText(String.valueOf(sprite.x));
                android.widget.EditText y=new android.widget.EditText(this); y.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); y.setHint("Y"); y.setText(String.valueOf(sprite.y));
                android.widget.EditText width=new android.widget.EditText(this); width.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); width.setHint("Ancho (0.25–8)"); width.setText(String.valueOf(sprite.width));
                android.widget.EditText height=new android.widget.EditText(this); height.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); height.setHint("Alto (0.25–8)"); height.setText(String.valueOf(sprite.height));
                android.widget.EditText rotation=new android.widget.EditText(this); rotation.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); rotation.setHint("Rotación en grados"); rotation.setText(String.valueOf(sprite.rotation));
                LinearLayout form=vertical();form.addView(x);form.addView(y);form.addView(width);form.addView(height);form.addView(rotation);
                new android.app.AlertDialog.Builder(this).setTitle("Transformación 2D").setView(form)
                    .setPositiveButton("Guardar",(d,w)->{try{
                        float nx=Float.parseFloat(x.getText().toString()),ny=Float.parseFloat(y.getText().toString());
                        float nw=Float.parseFloat(width.getText().toString()),nh=Float.parseFloat(height.getText().toString());
                        float nr=Float.parseFloat(rotation.getText().toString());
                        if(Float.isNaN(nx)||Float.isNaN(ny)||Float.isNaN(nw)||Float.isNaN(nh)||Float.isNaN(nr)||Float.isInfinite(nx)||Float.isInfinite(ny)||Float.isInfinite(nw)||Float.isInfinite(nh)||Float.isInfinite(nr)||nw<0.25f||nw>Math.min(8f,scenePreview.mapColumns)||nh<0.25f||nh>Math.min(8f,scenePreview.mapRows)||nr<-360f||nr>360f)throw new NumberFormatException();
                        sprite.x=Math.max(0f,Math.min(scenePreview.mapColumns-nw,nx));sprite.y=Math.max(0f,Math.min(scenePreview.mapRows-nh,ny));
                        sprite.width=nw;sprite.height=nh;sprite.rotation=nr;
                        scenePreview.persistSprites();scenePreview.invalidate();dialog.dismiss();
                    }catch(Exception e){Toast.makeText(this,"Valores inválidos. Tamaño: 0.25–8; giro: −360 a 360°.",Toast.LENGTH_LONG).show();}})
                    .setNegativeButton("Cancelar",null).show();
            }));
            panelView.addView(editorButton(sprite.solid?"HACER TRANSPASABLE":"HACER SÓLIDO", () -> { scenePreview.toggleSelectedSpriteSolid(); dialog.dismiss(); }));
            panelView.addView(editorButton("ELIMINAR OBJETO", () -> { scenePreview.deleteSelectedSprite(); dialog.dismiss(); }));
            panelView.addView(editorButton("DUPLICAR OBJETO", () -> { scenePreview.duplicateSelectedSprite(); dialog.dismiss(); }));
            panelView.addView(editorButton("EDITAR CÓDIGO", () -> { dialog.dismiss(); showCodePanel(); }));
        }
        panelView.addView(editorButton("CERRAR", dialog::dismiss));
    }

    void showCodePanel() {
        String project=activeProjectName();
        android.app.Dialog dialog=editorPanel("EL GRAFISRIPT",Gravity.BOTTOM,96);
        if(dialog.getWindow()!=null) dialog.getWindow().setLayout(-1,Math.max(420,getResources().getDisplayMetrics().heightPixels*72/100));
        LinearLayout panelView=(LinearLayout)((android.view.ViewGroup)dialog.findViewById(android.R.id.content)).getChildAt(0);
        if(panelView==null)return;
        panelView.addView(label("Código del proyecto · se guarda en el editor principal",12,gray));
        EditText editor=new EditText(this);
        editor.setTextColor(Color.rgb(180,220,255));editor.setTypeface(Typeface.MONOSPACE);editor.setTextSize(13);
        editor.setGravity(Gravity.TOP);editor.setMinLines(10);editor.setBackground(shape(Color.rgb(5,10,18)));
        editor.setText(getPreferences(MODE_PRIVATE).getString("grafisript_"+project,"# Escribe aquí tu código de El Grafisript\nmostrar_mensaje \"Hola, BlockForge\"\n"));
        panelView.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
        panelView.addView(editorButton("GUARDAR CÓDIGO",()->{
            getPreferences(MODE_PRIVATE).edit().putString("grafisript_"+project,editor.getText().toString()).apply();
            Toast.makeText(this,"Código guardado",Toast.LENGTH_SHORT).show();
        }));
        panelView.addView(editorButton("ABRIR EDITOR Y EJECUTAR",()->{
            getPreferences(MODE_PRIVATE).edit().putString("grafisript_"+project,editor.getText().toString()).apply();
            if (codeEditor != null) { codeEditorProjectName = project; codeEditor.setText(editor.getText().toString()); }
            dialog.dismiss();openPage(3);
        }));
        panelView.addView(editorButton("CERRAR",dialog::dismiss));
    }

    void showToolsPanel() {
        android.app.Dialog dialog=editorPanel("HERRAMIENTAS DE ESCENA",Gravity.RIGHT,88);
        LinearLayout panelView=(LinearLayout)((android.view.ViewGroup)dialog.findViewById(android.R.id.content)).getChildAt(0);
        if(panelView==null)return;
        panelView.addView(label("FÍSICA Y COLISIONES",11,Color.rgb(135,180,235)));
        panelView.addView(editorButton("CONFIGURAR FÍSICA",()->showPhysicsSettingsDialog()));
        panelView.addView(editorButton("CAPAS, CÁMARA Y PACKS",()->showEditorExtrasDialog()));
        panelView.addView(editorButton("AÑADIR BOTÓN MÓVIL",()->createMobileButtonDialog()));
        panelView.addView(editorButton("ADMINISTRAR BOTONES",()->manageMobileButtons()));
        panelView.addView(editorButton("BOTONES PREDETERMINADOS",()->{scenePreview.installDefaultMobileButtons();dialog.dismiss();}));
        panelView.addView(label("AUDIO Y ARCHIVOS",11,Color.rgb(135,180,235)));
        panelView.addView(editorButton("ELEGIR AUDIO",()->showSceneAudioPicker()));
        panelView.addView(editorButton("DETENER AUDIO",()->stopSceneAudio()));
        panelView.addView(editorButton("EXPORTAR NIVEL",()->exportScenePicker()));
        panelView.addView(editorButton("IMPORTAR NIVEL",()->importScenePicker()));
        panelView.addView(editorButton("GUARDAR ESCENA",()->{scenePreview.persist();Toast.makeText(this,"Escena guardada",Toast.LENGTH_SHORT).show();}));
        panelView.addView(editorButton("CERRAR",dialog::dismiss));
    }

    void showTimelinePanel() {
        if(scenePreview==null)return;
        android.app.Dialog dialog=editorPanel("ANIMACIÓN · TIMELINE",Gravity.BOTTOM,96);
        if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,Math.max(320,getResources().getDisplayMetrics().heightPixels*55/100));
        LinearLayout panelView=(LinearLayout)((android.view.ViewGroup)dialog.findViewById(android.R.id.content)).getChildAt(0);
        if(panelView==null)return;
        panelView.addView(label(scenePreview.animationStatus(),13,gray));
        LinearLayout row=new LinearLayout(this);
        row.addView(editorButton("＋ FRAME",()->pickAnimationFrame()),new LinearLayout.LayoutParams(0,48,1));
        row.addView(editorButton("◀",()->scenePreview.selectAnimationFrame(-1)),new LinearLayout.LayoutParams(0,48,1));
        row.addView(editorButton("▶",()->scenePreview.selectAnimationFrame(1)),new LinearLayout.LayoutParams(0,48,1));
        row.addView(editorButton("PLAY",()->scenePreview.toggleAnimationPlayback()),new LinearLayout.LayoutParams(0,48,1));
        panelView.addView(row);
        panelView.addView(editorButton("FPS",()->showAnimationFpsDialog()));
        panelView.addView(editorButton("BUCLE: " + (scenePreview.selectedSpriteObject() != null && scenePreview.selectedSpriteObject().animationLoop ? "ON" : "OFF"),()->scenePreview.toggleAnimationLoop()));
        panelView.addView(editorButton("ELIMINAR FRAME",()->scenePreview.deleteAnimationFrame()));
        panelView.addView(editorButton("CERRAR",dialog::dismiss));
    }


    int uiInk = Color.rgb(232, 236, 242);
    int uiMuted = Color.rgb(151, 161, 174);
    int uiSurface = Color.rgb(29, 34, 42);
    int uiSurfaceRaised = Color.rgb(36, 42, 51);
    int uiLine = Color.rgb(53, 61, 72);
    int uiAccent = Color.rgb(74, 145, 230);

    GradientDrawable uiPanel(int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(16f);
        d.setStroke(1, uiLine);
        return d;
    }

    TextView uiLabel(String value, int size, int color, boolean bold) {
        TextView t = label(value, size, color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    void uiGap(LinearLayout parent, int height) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, height));
    }

    TextView uiPrimary(String value, Runnable action) {
        TextView b = uiLabel(value, 13, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(16, 13, 16, 13);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(54, 112, 184));
        bg.setCornerRadius(12f);
        b.setBackground(bg);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    TextView uiSecondary(String value, Runnable action) {
        TextView b = uiLabel(value, 12, uiInk, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(11, 10, 11, 10);
        b.setMinHeight(42);
        b.setBackground(uiPanel(uiSurfaceRaised));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    void uiSection(String title, String detail) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = uiLabel(title, 12, uiInk, true);
        heading.setLetterSpacing(0.06f);
        row.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        if (detail != null && !detail.isEmpty())
            row.addView(uiLabel(detail, 10, uiMuted, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = 20;
        lp.bottomMargin = 9;
        content.addView(row, lp);
    }

    void openProject(String name) {
        getPreferences(MODE_PRIVATE).edit().putString("active_project", name).apply();
        openPage(2);
    }

    void projectTile(String name) {
        LinearLayout tile = new LinearLayout(this);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(14, 13, 14, 13);
        tile.setBackground(uiPanel(uiSurface));
        LinearLayout iconBox = vertical();
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackground(uiPanel(Color.rgb(39, 58, 78)));
        String type = getPreferences(MODE_PRIVATE).getString("project_type_" + name, "2D");
        iconBox.addView(uiLabel(type, 13, Color.rgb(144, 194, 245), true));
        tile.addView(iconBox, new LinearLayout.LayoutParams(46, 46));
        LinearLayout details = vertical();
        details.setPadding(12, 0, 0, 0);
        details.addView(uiLabel(name, 14, uiInk, true));
        details.addView(uiLabel(type + "  ·  En este dispositivo", 11, uiMuted, false));
        tile.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        tile.addView(uiLabel("›", 24, uiMuted, false));
        tile.setOnClickListener(v -> openProject(name));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 9;
        content.addView(tile, lp);
    }


    void buildInterface() {
        root = vertical();
        root.setBackgroundColor(Color.rgb(17, 20, 26));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(18, 13, 18, 13);
        header.setBackgroundColor(Color.rgb(22, 26, 33));

        TextView mark = uiLabel("BF", 16, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        GradientDrawable markBg = new GradientDrawable();
        markBg.setColor(Color.rgb(53, 104, 164));
        markBg.setCornerRadius(10f);
        mark.setBackground(markBg);
        header.addView(mark, new LinearLayout.LayoutParams(38, 38));

        LinearLayout brand = vertical();
        brand.setPadding(11, 0, 0, 0);
        brand.addView(uiLabel("BLOCKFORGE", 15, uiInk, true));
        brand.addView(uiLabel("STUDIO  /  MOBILE", 9, uiMuted, true));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(uiLabel("●", 10, Color.rgb(105, 185, 135), true));
        header.addView(uiLabel(" LOCAL", 9, uiMuted, true));
        root.addView(header);

        View separator = new View(this);
        separator.setBackgroundColor(uiLine);
        root.addView(separator, new LinearLayout.LayoutParams(-1, 1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = vertical();
        content.setPadding(18, 18, 18, 24);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        navigation = new LinearLayout(this);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(12, 8, 12, 8);
        navigation.setBackgroundColor(Color.rgb(37, 40, 46));
        TextView marketplaceButton = uiSecondary("▦   MARKETPLACE", () -> openPage(6));
        navigation.addView(marketplaceButton,
                new LinearLayout.LayoutParams(-1, -2));
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
        if (navigation != null) navigation.setVisibility(page == 2 ? View.GONE : View.VISIBLE);
        switch(page) {
            case 0: home(); break;
            case 1: projects(); break;
            case 2: scenes(); break;
            case 3: grafisript(); break;
            case 4: resources(); break;
            case 5: settings(); break;
            case 6: marketplace(); break;
        }
    }


    void marketplace() {
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 22, 24, 24);
        heading("Marketplace", "Recursos y contenido de la comunidad.");
        LinearLayout empty = vertical();
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(20, 36, 20, 36);
        empty.setBackground(uiPanel(uiSurface));
        empty.addView(uiLabel("Todavía no hay contenido", 16, uiInk, true));
        TextView note = uiLabel("El Marketplace aparecerá aquí cuando haya recursos disponibles.",
                12, uiMuted, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 8, 0, 0);
        empty.addView(note);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1);
        lp.topMargin = 18;
        content.addView(empty, lp);
        content.addView(uiSecondary("←  VOLVER A PROYECTOS", () -> openPage(0)));
    }


    void heading(String title, String subtitle) {
        TextView titleView = uiLabel(title, 25, uiInk, true);
        titleView.setLetterSpacing(-0.02f);
        content.addView(titleView);
        TextView sub = uiLabel(subtitle, 13, uiMuted, false);
        sub.setPadding(0, 6, 0, 0);
        content.addView(sub);
    }


    void card(String title, String description) {
        LinearLayout c = vertical();
        c.setPadding(16, 15, 16, 15);
        c.setBackground(uiPanel(uiSurface));
        c.addView(uiLabel(title, 14, uiInk, true));
        TextView desc = uiLabel(description, 12, uiMuted, false);
        desc.setPadding(0, 7, 0, 0);
        c.addView(desc);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 9;
        content.addView(c, lp);
    }


    void home() {
        content.setPadding(0, 0, 0, 0);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setBackgroundColor(Color.rgb(29, 32, 37));

        // Panel lateral: biblioteca de proyectos, al estilo de un gestor de proyectos.
        LinearLayout sidebar = vertical();
        sidebar.setPadding(16, 20, 16, 16);
        sidebar.setBackgroundColor(Color.rgb(37, 40, 46));
        TextView projectsTitle = uiLabel("PROYECTOS", 11, uiMuted, true);
        projectsTitle.setLetterSpacing(0.12f);
        sidebar.addView(projectsTitle);
        uiGap(sidebar, 12);

        java.util.Set<String> saved = getPreferences(MODE_PRIVATE)
                .getStringSet("blockforge_projects", new java.util.HashSet<>());
        java.util.ArrayList<String> names = new java.util.ArrayList<>(saved);
        java.util.Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        if (names.isEmpty()) {
            TextView empty = uiLabel("Todavía no hay proyectos", 12, uiMuted, false);
            empty.setPadding(0, 8, 0, 8);
            sidebar.addView(empty);
        } else {
            for (int i = names.size() - 1; i >= 0; i--) {
                final String projectName = names.get(i);
                String projectType = getPreferences(MODE_PRIVATE)
                        .getString("project_type_" + projectName, "2D");
                TextView item = uiLabel("▣  " + projectName + "   " + projectType,
                        12, uiInk, true);
                item.setSingleLine(true);
                item.setEllipsize(android.text.TextUtils.TruncateAt.END);
                item.setPadding(10, 13, 8, 13);
                item.setBackground(uiPanel(Color.rgb(48, 52, 59)));
                item.setOnClickListener(v -> openProject(projectName));
                LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(-1, -2);
                itemLp.bottomMargin = 6;
                sidebar.addView(item, itemLp);
            }
        }
        LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.31f), -1);
        content.addView(sidebar, sideLp);

        // Área principal: crear proyectos y acceso único al Marketplace.
        LinearLayout main = vertical();
        main.setGravity(Gravity.CENTER);
        main.setPadding(28, 22, 28, 22);
        main.setBackgroundColor(Color.rgb(29, 32, 37));
        TextView eyebrow = uiLabel("BLOCKFORGE STUDIO", 11, uiMuted, true);
        eyebrow.setLetterSpacing(0.16f);
        main.addView(eyebrow);
        TextView title = uiLabel("Administrador de proyectos", 25, uiInk, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 6);
        main.addView(title);
        TextView subtitle = uiLabel("Crea un proyecto nuevo o abre uno de tu biblioteca.",
                13, uiMuted, false);
        subtitle.setGravity(Gravity.CENTER);
        main.addView(subtitle);

        LinearLayout createCard = vertical();
        createCard.setPadding(22, 20, 22, 20);
        createCard.setBackground(uiPanel(Color.rgb(43, 47, 54)));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                Math.min((int)(getResources().getDisplayMetrics().widthPixels * 0.52f), 560), -2);
        cardLp.topMargin = 24;
        createCard.addView(uiLabel("NUEVO PROYECTO", 10, uiMuted, true));
        TextView createTitle = uiLabel("Empieza algo nuevo", 20, uiInk, true);
        createTitle.setPadding(0, 8, 0, 6);
        createCard.addView(createTitle);
        createCard.addView(uiLabel("Elige un nombre y decide si tu proyecto será 2D o 3D.",
                12, uiMuted, false));
        TextView create = uiPrimary("＋  CREAR PROYECTO", () -> newProject());
        LinearLayout.LayoutParams createLp = new LinearLayout.LayoutParams(-1, -2);
        createLp.topMargin = 16;
        createCard.addView(create, createLp);
        main.addView(createCard, cardLp);

        TextView marketplace = uiSecondary("▦   IR A MARKETPLACE", () -> openPage(6));
        LinearLayout.LayoutParams marketLp = new LinearLayout.LayoutParams(
                Math.min((int)(getResources().getDisplayMetrics().widthPixels * 0.52f), 560), -2);
        marketLp.topMargin = 14;
        main.addView(marketplace, marketLp);
        content.addView(main, new LinearLayout.LayoutParams(0, -1, 1));
    }


    void projects() {
        content.setPadding(20, 20, 20, 24);
        heading("Proyectos", "Tus juegos y escenas guardados localmente.");
        TextView create = uiPrimary("＋  NUEVO PROYECTO", () -> newProject());
        LinearLayout.LayoutParams createLp = new LinearLayout.LayoutParams(-1, -2);
        createLp.topMargin = 18;
        content.addView(create, createLp);

        uiSection("BIBLIOTECA", "PROYECTOS LOCALES");
        java.util.Set<String> saved = getPreferences(MODE_PRIVATE).getStringSet("blockforge_projects", new java.util.HashSet<>());
        java.util.ArrayList<String> names = new java.util.ArrayList<>(saved);
        java.util.Collections.sort(names);
        if (names.isEmpty()) {
            LinearLayout empty = vertical();
            empty.setPadding(18, 22, 18, 22);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(uiPanel(uiSurface));
            empty.addView(uiLabel("No hay proyectos todavía", 15, uiInk, true));
            TextView message = uiLabel("Crea tu primer proyecto para abrir el editor.", 12, uiMuted, false);
            message.setPadding(0, 7, 0, 0);
            empty.addView(message);
            content.addView(empty);
        } else {
            for (int i = names.size() - 1; i >= 0; i--) projectTile(names.get(i));
        }
    }

    void newProject() {
        LinearLayout form = vertical();
        form.setPadding(20, 8, 20, 0);
        EditText input = new EditText(this);
        input.setSingleLine(true);
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
                String safeName = name.replaceAll("[^a-zA-Z0-9._ -]", "_").trim();
                if (safeName.isEmpty() || ".".equals(safeName) || "..".equals(safeName)) {
                    Toast.makeText(this, "Ese nombre no es válido", Toast.LENGTH_SHORT).show();
                    return;
                }
                String projectType = type.getSelectedItem().toString();
                android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                java.util.Set<String> names = new java.util.HashSet<>(
                        prefs.getStringSet("blockforge_projects", new java.util.HashSet<>()));
                if (names.contains(name)) {
                    Toast.makeText(this, "Ya existe un proyecto con ese nombre",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                java.io.File projectsRoot = new java.io.File(getFilesDir(), "BlockForgeProjects");
                java.io.File projectFolder = new java.io.File(projectsRoot, safeName);
                if ((!projectsRoot.exists() && !projectsRoot.mkdirs())
                        || (!projectFolder.exists() && !projectFolder.mkdirs())) {
                    Toast.makeText(this, "No se pudo crear la carpeta del proyecto",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                names.add(name);
                prefs.edit()
                        .putStringSet("blockforge_projects", names)
                        .putString("project_type_" + name, projectType)
                        .putString("project_folder_" + name, projectFolder.getAbsolutePath())
                        .apply();
                Toast.makeText(this, "Proyecto " + projectType + " creado: " + name,
                        Toast.LENGTH_LONG).show();
                // Abrir directamente el editor correspondiente al tipo elegido.
                openProject(name);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    ScenePreview scenePreview;
    Scene3DView scene3DView;

    void showEditorExtrasDialog() {
        String[] options = {"Cambiar capa", "Mover cámara: " + (cameraPanMode ? "activado" : "desactivado"), "Tamaño del mapa", "Centrar cámara", "Instalar pack 30 FPS", "Instalar pack 60 FPS"};
        new android.app.AlertDialog.Builder(this)
            .setTitle("Herramientas de escena")
            .setItems(options, (dialog, which) -> {
                switch(which) {
                    case 0: sceneLayer=(sceneLayer+1)%3; openPage(2); break;
                    case 1: cameraPanMode=!cameraPanMode; openPage(2); break;
                    case 2: showMapSizeDialog(); break;
                    case 3: if(scenePreview!=null) scenePreview.resetCamera(); break;
                    case 4: installSpritePack(30); break;
                    case 5: installSpritePack(60); break;
                }
            }).show();
    }


    TextView editorButton(String value, Runnable action) {
        TextView b = uiLabel(value, 11, uiInk, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(6, 5, 6, 5);
        b.setMinHeight(42);
        b.setBackground(uiPanel(uiSurfaceRaised));
        b.setOnClickListener(v -> action.run());
        return b;
    }


    void scenes() {
        String project = activeProjectName();
        String type = getPreferences(MODE_PRIVATE).getString("project_type_" + project, "2D");
        if ("3D".equals(type)) {
            content.setPadding(12, 10, 12, 16);
            content.addView(editorButton("←  VOLVER A PROYECTOS", () -> openPage(1)));
            heading("Editor 3D", project);
            scene3DView = new Scene3DView();
            content.addView(scene3DView, new LinearLayout.LayoutParams(-1, 390));
            scene3DView.onResume();
            LinearLayout actions = new LinearLayout(this);
            actions.addView(editorButton("＋ CUBO", () -> scene3DView.addCube()), new LinearLayout.LayoutParams(0, 48, 1));
            actions.addView(editorButton("BORRAR", () -> scene3DView.clearCubes()), new LinearLayout.LayoutParams(0, 48, 1));
            content.addView(actions);
            content.addView(editorButton("GUARDAR ESCENA", () -> { scene3DView.persist(); Toast.makeText(this, "Escena 3D guardada", Toast.LENGTH_SHORT).show(); }));
            return;
        }

        content.setPadding(10, 8, 10, 16);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(7, 6, 7, 6);
        top.setBackground(uiPanel(uiSurface));
        top.addView(editorButton("‹", () -> openPage(1)), new LinearLayout.LayoutParams(42, 44));
        LinearLayout titleBox = vertical();
        titleBox.setPadding(10, 0, 4, 0);
        titleBox.addView(uiLabel(project, 14, uiInk, true));
        titleBox.addView(uiLabel("ESCENA 2D  ·  EDITOR", 9, uiMuted, true));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(editorButton("▶", () -> {
            scenePlayMode = !scenePlayMode; physicsRunning = scenePlayMode;
            if (physicsRunning && scenePreview != null) scenePreview.startPhysics();
            else if (scenePreview != null) scenePreview.stopPhysics();
            openPage(2);
        }), new LinearLayout.LayoutParams(44, 44));
        top.addView(editorButton("▣", () -> {
            if (scenePreview != null) scenePreview.persist();
            Toast.makeText(this, "Escena guardada", Toast.LENGTH_SHORT).show();
        }), new LinearLayout.LayoutParams(44, 44));
        content.addView(top);

        LinearLayout panels = new LinearLayout(this);
        panels.setPadding(0, 7, 0, 5);
        panels.addView(editorButton("ESCENA", () -> showSceneTreePanel()), new LinearLayout.LayoutParams(0, 42, 1));
        panels.addView(editorButton("INSPECTOR", () -> showInspectorPanel()), new LinearLayout.LayoutParams(0, 42, 1));
        panels.addView(editorButton("CÓDIGO", () -> showCodePanel()), new LinearLayout.LayoutParams(0, 42, 1));
        panels.addView(editorButton("ANIMAR", () -> showTimelinePanel()), new LinearLayout.LayoutParams(0, 42, 1));
        panels.addView(editorButton("MÁS", () -> showToolsPanel()), new LinearLayout.LayoutParams(0, 42, 1));
        content.addView(panels);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(4, 5, 4, 5);
        toolbar.setBackground(uiPanel(Color.rgb(24, 29, 37)));
        toolbar.addView(editorButton(sceneToolMode == 0 ? "● LÁPIZ" : "LÁPIZ", () -> { sceneToolMode = 0; openPage(2); }), new LinearLayout.LayoutParams(0, 44, 1));
        toolbar.addView(editorButton(sceneToolMode == 1 ? "● RECT." : "RECT.", () -> { sceneToolMode = 1; openPage(2); }), new LinearLayout.LayoutParams(0, 44, 1));
        toolbar.addView(editorButton(sceneToolMode == 2 ? "● CUBETA" : "CUBETA", () -> { sceneToolMode = 2; openPage(2); }), new LinearLayout.LayoutParams(0, 44, 1));
        toolbar.addView(editorButton("DESHACER", () -> scenePreview.undo()), new LinearLayout.LayoutParams(0, 44, 1));
        toolbar.addView(editorButton("REHACER", () -> scenePreview.redo()), new LinearLayout.LayoutParams(0, 44, 1));
        content.addView(toolbar);

        LinearLayout viewportFrame = vertical();
        viewportFrame.setPadding(3, 3, 3, 3);
        viewportFrame.setBackground(uiPanel(Color.rgb(13, 17, 23)));
        LinearLayout viewportHeader = new LinearLayout(this);
        viewportHeader.setGravity(Gravity.CENTER_VERTICAL);
        viewportHeader.setPadding(9, 7, 9, 7);
        viewportHeader.addView(uiLabel("VISTA DE ESCENA", 10, uiInk, true), new LinearLayout.LayoutParams(0, -2, 1));
        viewportHeader.addView(uiLabel("2D  ·  " + scenePreviewStatus(), 9, uiMuted, false));
        viewportFrame.addView(viewportHeader);
        scenePreview = new ScenePreview();
        String savedSceneAudio = getPreferences(MODE_PRIVATE).getString("scene_audio_" + project, "");
        if (!savedSceneAudio.isEmpty() && sceneAudioPlayer == null) playSceneAudio(savedSceneAudio);
        int viewportHeight = Math.max(340, Math.min(600, (int)(getResources().getDisplayMetrics().heightPixels * 0.38f)));
        viewportFrame.addView(scenePreview, new LinearLayout.LayoutParams(-1, viewportHeight));
        content.addView(viewportFrame);
        if (physicsRunning) scenePreview.startPhysics();

        uiSection("PINCEL Y VISTA", "HERRAMIENTAS DEL LIENZO");
        LinearLayout palette = new LinearLayout(this);
        palette.addView(editorButton("VERDE", () -> setSceneBrush(Color.rgb(55, 135, 90), false)), new LinearLayout.LayoutParams(0, 44, 1));
        palette.addView(editorButton("AZUL", () -> setSceneBrush(Color.rgb(55, 115, 210), false)), new LinearLayout.LayoutParams(0, 44, 1));
        palette.addView(editorButton("ROJO", () -> setSceneBrush(Color.rgb(205, 75, 75), false)), new LinearLayout.LayoutParams(0, 44, 1));
        palette.addView(editorButton("BORRAR", () -> setSceneBrush(sceneBrushColor, true)), new LinearLayout.LayoutParams(0, 44, 1));
        palette.addView(editorButton("HEX", () -> showSceneColorDialog()), new LinearLayout.LayoutParams(0, 44, 1));
        palette.addView(editorButton(sceneGridVisible ? "CUADRÍCULA ON" : "CUADRÍCULA OFF", () -> { sceneGridVisible = !sceneGridVisible; if (scenePreview != null) scenePreview.invalidate(); openPage(2); }), new LinearLayout.LayoutParams(0, 44, 1));
        content.addView(palette);

        uiSection("TRANSFORMACIÓN", "CÁMARA Y CAPAS");
        LinearLayout camera = new LinearLayout(this);
        camera.addView(editorButton("ZOOM +", () -> scenePreview.changeZoom(1.2f)), new LinearLayout.LayoutParams(0, 44, 1));
        camera.addView(editorButton("ZOOM −", () -> scenePreview.changeZoom(0.83f)), new LinearLayout.LayoutParams(0, 44, 1));
        camera.addView(editorButton("CENTRAR", () -> scenePreview.resetCamera()), new LinearLayout.LayoutParams(0, 44, 1));
        camera.addView(editorButton("CAPA " + new String[]{"FONDO", "MUNDO", "FRENTE"}[sceneLayer], () -> { sceneLayer = (sceneLayer + 1) % 3; openPage(2); }), new LinearLayout.LayoutParams(0, 44, 1));
        content.addView(camera);

        uiSection("RECURSOS DE ESCENA", "SPRITES Y MAPA");
        LinearLayout assets = new LinearLayout(this);
        assets.addView(editorButton("＋ SPRITE", () -> openSpriteDrawer()), new LinearLayout.LayoutParams(0, 46, 1));
        assets.addView(editorButton("IMPORTAR PNG", () -> pickSpriteImage()), new LinearLayout.LayoutParams(0, 46, 1));
        assets.addView(editorButton("TAMAÑO MAPA", () -> showMapSizeDialog()), new LinearLayout.LayoutParams(0, 46, 1));
        content.addView(assets);

        uiSection("ANIMACIÓN", "FOTOGRAMAS");
        LinearLayout animation = new LinearLayout(this);
        animation.addView(editorButton("＋ FRAME", () -> pickAnimationFrame()), new LinearLayout.LayoutParams(0, 46, 1));
        animation.addView(editorButton("ANTERIOR", () -> scenePreview.selectAnimationFrame(-1)), new LinearLayout.LayoutParams(0, 46, 1));
        animation.addView(editorButton("SIGUIENTE", () -> scenePreview.selectAnimationFrame(1)), new LinearLayout.LayoutParams(0, 46, 1));
        animation.addView(editorButton("PLAY / PAUSA", () -> scenePreview.toggleAnimationPlayback()), new LinearLayout.LayoutParams(0, 46, 1));
        animation.addView(editorButton("FPS", () -> showAnimationFpsDialog()), new LinearLayout.LayoutParams(0, 46, 1));
        content.addView(animation);
        TextView timeline = uiLabel(scenePreview.animationStatus(), 10, Color.rgb(145, 184, 224), false);
        timeline.setPadding(11, 10, 11, 10);
        timeline.setBackground(uiPanel(uiSurface));
        content.addView(timeline);

        uiSection("EJECUCIÓN", "FÍSICA Y COLISIONES");
        LinearLayout runtime = new LinearLayout(this);
        runtime.addView(editorButton(scenePlayMode ? "DETENER" : "PROBAR", () -> {
            scenePlayMode = !scenePlayMode; physicsRunning = scenePlayMode;
            if (physicsRunning) scenePreview.startPhysics(); else scenePreview.stopPhysics();
            openPage(2);
        }), new LinearLayout.LayoutParams(0, 46, 1));
        runtime.addView(editorButton(physicsRunning ? "PAUSAR FÍSICA" : "ACTIVAR FÍSICA", () -> {
            physicsRunning = !physicsRunning;
            if (physicsRunning) scenePreview.startPhysics(); else scenePreview.stopPhysics();
            openPage(2);
        }), new LinearLayout.LayoutParams(0, 46, 1));
        runtime.addView(editorButton("COLISIONES " + (sceneCollisionsEnabled ? "ON" : "OFF"), () -> {
            sceneCollisionsEnabled = !sceneCollisionsEnabled; openPage(2);
        }), new LinearLayout.LayoutParams(0, 46, 1));
        content.addView(runtime);
    }

    void showSceneAudioPicker() {
        java.io.File folder=new java.io.File(getFilesDir(),"resources");
        java.io.File[] all=folder.listFiles();java.util.ArrayList<java.io.File> audio=new java.util.ArrayList<>();
        if(all!=null)for(java.io.File file:all){String n=file.getName().toLowerCase(java.util.Locale.ROOT);if(n.endsWith(".mp3")||n.endsWith(".wav")||n.endsWith(".ogg")||n.endsWith(".m4a")||n.endsWith(".aac"))audio.add(file);}
        if(audio.isEmpty()){Toast.makeText(this,"Importa primero un audio desde Recursos",Toast.LENGTH_LONG).show();return;}
        String[] names=new String[audio.size()];for(int i=0;i<audio.size();i++)names[i]=audio.get(i).getName();
        new android.app.AlertDialog.Builder(this).setTitle("Audio de la escena").setItems(names,(dialog,which)->playSceneAudio(names[which])).setNegativeButton("Cancelar",null).show();
    }

    void showPhysicsSettingsDialog(){
        LinearLayout form=vertical();form.setPadding(18,8,18,0);
        EditText gravity=new EditText(this);gravity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);gravity.setHint("Gravedad (0–80)");gravity.setText(String.valueOf(physicsGravity));form.addView(gravity);
        EditText bounce=new EditText(this);bounce.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);bounce.setHint("Rebote (0–1)");bounce.setText(String.valueOf(physicsBounce));form.addView(bounce);
        android.widget.CheckBox collisions=new android.widget.CheckBox(this);collisions.setText("Activar colisiones entre bloques y sprites");collisions.setChecked(sceneCollisionsEnabled);form.addView(collisions);
        new android.app.AlertDialog.Builder(this).setTitle("Física 2D").setView(form)
            .setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{
                try{
                    float g=Float.parseFloat(gravity.getText().toString().trim());
                    float b=Float.parseFloat(bounce.getText().toString().trim());
                    if(Float.isNaN(g)||Float.isInfinite(g)||g<0f||g>80f||Float.isNaN(b)||Float.isInfinite(b)||b<0f||b>1f)throw new NumberFormatException();
                    physicsGravity=g;physicsBounce=b;sceneCollisionsEnabled=collisions.isChecked();
                    getPreferences(MODE_PRIVATE).edit().putFloat("physics_gravity",g).putFloat("physics_bounce",b).putBoolean("scene_collisions",sceneCollisionsEnabled).apply();
                    if(scenePreview!=null)scenePreview.invalidate();
                    Toast.makeText(this,"Física guardada en este dispositivo",Toast.LENGTH_SHORT).show();
                }catch(Exception ex){Toast.makeText(this,"Valores inválidos. Gravedad: 0–80; rebote: 0–1.",Toast.LENGTH_LONG).show();}
            }).show();
    }

    void playSceneAudio(String name) {
        stopSceneAudio();
        try {
            sceneAudioPlayer=new android.media.MediaPlayer();sceneAudioPrepared=false;
            java.io.File file=new java.io.File(new java.io.File(getFilesDir(),"resources"),name);
            if(!file.isFile())throw new java.io.IOException("El audio no existe en Recursos");
            sceneAudioPlayer.setDataSource(file.getAbsolutePath());sceneAudioPlayer.setLooping(true);
            sceneAudioPlayer.setOnPreparedListener(mp->{sceneAudioPrepared=true;if(activityResumed)mp.start();});
            sceneAudioPlayer.setOnErrorListener((mp,what,extra)->{Toast.makeText(this,"No se pudo reproducir el audio",Toast.LENGTH_SHORT).show();stopSceneAudio();return true;});
            sceneAudioPlayer.prepareAsync();getPreferences(MODE_PRIVATE).edit().putString("scene_audio_"+activeProjectName(),name).apply();
            Toast.makeText(this,"Preparando audio: "+name,Toast.LENGTH_SHORT).show();
        } catch(Exception ex){stopSceneAudio();Toast.makeText(this,"Error de audio: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }

    void stopSceneAudio() {
        if(sceneAudioPlayer!=null){try{sceneAudioPlayer.stop();}catch(Exception ignored){}try{sceneAudioPlayer.release();}catch(Exception ignored){}sceneAudioPlayer=null;}
        sceneAudioPrepared=false;
        getPreferences(MODE_PRIVATE).edit().remove("scene_audio_"+activeProjectName()).apply();
    }

    @Override protected void onDestroy(){
        if(sceneAudioPlayer!=null){try{sceneAudioPlayer.stop();}catch(Exception ignored){}try{sceneAudioPlayer.release();}catch(Exception ignored){}sceneAudioPlayer=null;}
        sceneAudioPrepared=false;super.onDestroy();
    }

    void showSceneColorDialog() {
        EditText hex = new EditText(this);
        hex.setSingleLine(true);
        hex.setHint("#RRGGBB o #AARRGGBB");
        hex.setText(String.format(java.util.Locale.ROOT, "#%06X", 0xFFFFFF & sceneBrushColor));
        new android.app.AlertDialog.Builder(this)
                .setTitle("Color personalizado del bloque")
                .setView(hex)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("APLICAR", (dialog, which) -> {
                    try {
                        int color = Color.parseColor(hex.getText().toString().trim());
                        setSceneBrush(color, false);
                        openPage(2);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Color inválido. Usa #RRGGBB.", Toast.LENGTH_LONG).show();
                    }
                }).show();
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
        codeEditorProjectName = project;
        codeEditor.setText(getPreferences(MODE_PRIVATE).getString(
            "grafisript_" + project,
            "# Grafisript · ejemplo general en español\n"+
            "Crear variable Nombre = \"Ari\"\n"+
            "Crear variable Puntos = 2 + 3\n"+
            "Poner_texto: (\"Hola, {Nombre}\")\n"+
            "Si Puntos > 3 {\n"+
            "    Poner_texto: (\"Puntos: {Puntos} · ¡Muy bien!\")\n"+
            "} Sino {\n"+
            "    Poner_texto: (\"Sigue intentando\")\n"+
            "}\n"+
            "Crear lista Inventario = [\"Llave\", \"Mapa\"]\n"+
            "Añadir \"Poción\" a Inventario\n"+
            "Mostrar Inventario\n"+
            "Repetir 2 {\n"+
            "    Poner_texto: (\"Una acción repetida\")\n"+
            "}\n"+
            "Funcion Saludar {\n"+
            "    Poner_texto: (\"¡Hola desde una función!\")\n"+
            "}\n"+
            "Llamar Saludar\n"
        ));

        content.addView(button("＋ CREAR CÓDIGO", () -> showGrafisriptCreator()));
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

        card("Sintaxis fácil · programación general",
                "Crear variable Nombre = \"Ari\"\n"+
                "Poner_texto: (\"Hola, {Nombre}\")\n"+
                "Si Edad >= 18 {\n"+
                "    Mostrar \"Acceso permitido\"\n"+
                "} Sino {\n"+
                "    Mostrar \"Acceso restringido\"\n"+
                "}\n"+
                "Crear lista Tareas = [\"Estudiar\", \"Crear\"]\n"+
                "Añadir \"Probar\" a Tareas\n"+
                "Repetir 3 {\n"+
                "    Mostrar \"¡Hola!\"\n"+
                "}\n"+
                "Mientras Puntos > 0 {\n"+
                "    Restar 1 a Puntos\n"+
                "}");
    }

    void showGrafisriptCreator() {
        final String[] names = {
            "Crear mundo", "Crear bloque", "Crear personaje",
            "Mover personaje", "Hacer saltar", "Configurar vida",
            "Aplicar daño", "Mostrar mensaje", "Repetir acciones"
        };
        final String[] snippets = {
            "crear_mundo \"Mi mundo\"",
            "crear_bloque piedra 4 0 4",
            "crear_personaje Jugador",
            "jugador.mover(1, 0)",
            "jugador.saltar(5)",
            "jugador.vida(100)",
            "jugador.recibir_danio(10)",
            "mostrar_mensaje \"¡Hola, mundo!\"",
            "repetir 3 {\n    crear_bloque piedra 4 0 4\n}"
        };
        LinearLayout creator = vertical();
        creator.setPadding(18, 8, 18, 0);
        Spinner chooser = new Spinner(this);
        chooser.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, names));
        creator.addView(chooser);
        TextView preview = label(snippets[0], 13, Color.rgb(180,220,255));
        preview.setTypeface(Typeface.MONOSPACE);
        preview.setPadding(12, 14, 12, 14);
        preview.setBackground(shape(Color.rgb(5,10,18)));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, -2);
        previewParams.topMargin = 12;
        creator.addView(preview, previewParams);
        chooser.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                    int position, long id) {
                preview.setText(snippets[position]);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        new android.app.AlertDialog.Builder(this)
            .setTitle("Crear código")
            .setMessage("Elige una acción. BlockForge escribirá el ejemplo por ti.")
            .setView(creator)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("INSERTAR", (dialog, which) -> {
                int start = Math.max(0, codeEditor.getSelectionStart());
                int end = Math.max(start, codeEditor.getSelectionEnd());
                codeEditor.getText().replace(start, end,
                        snippets[chooser.getSelectedItemPosition()] + "\n");
                Toast.makeText(this, "Código insertado", Toast.LENGTH_SHORT).show();
            }).show();
    }

    void saveCurrentScript() {
        if (codeEditor == null) return;
        String project = codeEditorProjectName != null ? codeEditorProjectName : activeProjectName();
        getPreferences(MODE_PRIVATE).edit()
                .putString("grafisript_" + project,
                        codeEditor.getText().toString()).apply();
    }

    String[] normalizeGrafisriptLines(String source) {
        String[] lines = source.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.isEmpty() || raw.startsWith("#")) { lines[i] = raw; continue; }
            String lower = raw.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("crear variable ")) raw = "variable " + raw.substring(15).trim();
            else if (lower.startsWith("crear lista ")) raw = "lista " + raw.substring(12).trim();
            else if (lower.startsWith("cambiar ")) raw = raw.substring(8).trim();
            else if (lower.startsWith("poner_texto:")) {
                String value = raw.substring(raw.indexOf(':') + 1).trim();
                if (value.startsWith("(") && value.endsWith(")")) value = value.substring(1, value.length() - 1).trim();
                raw = "mostrar_mensaje " + value;
            } else if (lower.startsWith("mostrar ")) raw = "mostrar_mensaje " + raw.substring(8).trim();
            else if (lower.startsWith("si ")) raw = "si " + raw.substring(3).trim();
            else if (lower.equals("sino")) raw = "sino {";
            else if (lower.startsWith("sino ")) raw = "sino " + raw.substring(5).trim();
            else if (lower.startsWith("repetir ")) raw = "repetir " + raw.substring(8).trim();
            else if (lower.startsWith("mientras ")) raw = "mientras " + raw.substring(9).trim();
            else if (lower.startsWith("funcion ")) raw = "funcion " + raw.substring(8).trim();
            else if (lower.startsWith("llamar ")) raw = "llamar " + raw.substring(7).trim();
            else if (lower.startsWith("sumar ")) {
                String rest = raw.substring(6).trim(); int at = rest.toLowerCase(java.util.Locale.ROOT).lastIndexOf(" a ");
                if (at > 0) raw = "sumar " + rest.substring(at + 3).trim() + " " + rest.substring(0, at).trim();
            } else if (lower.startsWith("restar ")) {
                String rest = raw.substring(7).trim(); int at = rest.toLowerCase(java.util.Locale.ROOT).lastIndexOf(" a ");
                if (at > 0) raw = "restar " + rest.substring(at + 3).trim() + " " + rest.substring(0, at).trim();
            } else if (lower.startsWith("añadir ") || lower.startsWith("anadir ")) raw = "anadir " + raw.substring(raw.indexOf(' ') + 1).trim();
            else if (lower.startsWith("quitar ")) raw = "quitar " + raw.substring(7).trim();
            String normalized = raw.toLowerCase(java.util.Locale.ROOT);
            if (normalized.equals("fin") || normalized.equals("fin_si") || normalized.equals("fin_repetir") || normalized.equals("fin_mientras") || normalized.equals("fin_funcion") || normalized.equals("fin_evento")) raw = "}";
            lines[i] = raw;
        }
        return lines;
    }

    java.util.List<String> parseGrafisriptList(String source) {
        java.util.List<String> values = new java.util.ArrayList<>();
        String body = source.trim();
        if (body.startsWith("[") && body.endsWith("]")) body = body.substring(1, body.length() - 1);
        StringBuilder item = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"') quoted = !quoted;
            if (c == ',' && !quoted) { String value = unquote(item.toString().trim()); if (!value.isEmpty()) values.add(value); item.setLength(0); }
            else item.append(c);
        }
        String last = unquote(item.toString().trim()); if (!last.isEmpty()) values.add(last);
        return values;
    }

    String grafisriptDisplayValue(String source, java.util.Map<String, Double> variables) {
        String value = source.trim();
        if (grafisriptTextVariables.containsKey(value)) return grafisriptTextVariables.get(value);
        if (grafisriptLists.containsKey(value)) return grafisriptLists.get(value).toString();
        if (variables.containsKey(value)) return String.valueOf(variables.get(value));
        value = unquote(value);
        for (java.util.Map.Entry<String, String> entry : grafisriptTextVariables.entrySet()) {
            value = value.replace("${" + entry.getKey() + "}", entry.getValue());
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        for (java.util.Map.Entry<String, Double> entry : variables.entrySet()) {
            value = value.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            value = value.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return value;
    }

    void runGrafisript() {
        if (codeEditor == null || runtimeOutput == null) return;
        String[] lines = normalizeGrafisriptLines(codeEditor.getText().toString());
        grafisriptTextVariables.clear(); grafisriptLists.clear(); grafisriptCategories.clear();
        StringBuilder output = new StringBuilder();
        java.util.Map<String, Double> variables = new java.util.HashMap<>();
        java.util.Map<String, String[]> functions = new java.util.HashMap<>();
        int[] cursor = new int[]{0};
        int executed = executeGrafisriptBlock(lines, cursor, variables, functions, output, 0);
        output.append("\nPrograma finalizado. Instrucciones ejecutadas: ").append(executed);
        runtimeOutput.setText(output.toString());
    }

    int executeGrafisriptBlock(String[] lines, int[] cursor,
            java.util.Map<String, Double> variables,
            java.util.Map<String, String[]> functions, StringBuilder output, int depth) {
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

            // Categorías originales: /Enemigo/ { ... }, /Dinero/ { ... }, /Interfas/ { ... }.
            if (line.startsWith("/") && line.endsWith("/") && cursor[0] < lines.length && lines[cursor[0]].trim().equals("{")) {
                String category = line.substring(1, line.length() - 1).trim();
                int bodyStart = cursor[0] + 1; int bodyEnd = findGrafisriptBlockEnd(lines, bodyStart);
                if (bodyEnd < 0) { output.append("Línea ").append(lineNumber).append(": falta cerrar la categoría ").append(category).append(".\n"); return executed; }
                java.util.List<String> entries = new java.util.ArrayList<>();
                for (int k = bodyStart; k < bodyEnd; k++) { String entry = lines[k].trim(); if (!entry.isEmpty() && !entry.startsWith("#")) entries.add(entry); }
                grafisriptCategories.put(category, entries);
                output.append("Categoría registrada: /").append(category).append("/ · elementos: ").append(entries.size()).append('\n');
                cursor[0] = bodyEnd + 1; executed++; continue;
            }

            if (line.startsWith("funcion ") && line.endsWith("{")) {
                String name = line.substring(8, line.length() - 1).trim();
                if (!name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    output.append("Línea ").append(lineNumber).append(": nombre de función inválido.\n");
                    continue;
                }
                int bodyStart = cursor[0];
                int bodyEnd = findGrafisriptBlockEnd(lines, bodyStart);
                if (bodyEnd < 0) {
                    output.append("Línea ").append(lineNumber).append(": falta cerrar la función.\n");
                    return executed;
                }
                functions.put(name, java.util.Arrays.copyOfRange(lines, bodyStart, bodyEnd));
                cursor[0] = bodyEnd + 1;
                output.append("Función definida: ").append(name).append('\n');
                continue;
            }

            if (line.startsWith("llamar ")) {
                String name = line.substring(7).trim();
                String[] body = functions.get(name);
                if (body == null) {
                    output.append("Línea ").append(lineNumber).append(": función no definida: ")
                            .append(name).append('\n');
                } else {
                    int[] functionCursor = new int[]{0};
                    executed += executeGrafisriptBlock(body, functionCursor, variables,
                            functions, output, depth + 1);
                    executed++;
                }
                continue;
            }

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
                    executed += executeGrafisriptBlock(lines, cursor, variables, functions, output, depth + 1);
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
                        executed += executeGrafisriptBlock(lines, cursor, variables, functions, output, depth + 1);
                    } else {
                        cursor[0] = elseEnd + 1;
                    }
                }
                continue;
            }

            if (line.startsWith("mientras ") && line.endsWith("{")) {
                String condition = line.substring(9, line.length() - 1).trim();
                int bodyStart = cursor[0]; int bodyEnd = findGrafisriptBlockEnd(lines, bodyStart);
                if (bodyEnd < 0) { output.append("Línea ").append(lineNumber).append(": falta cerrar mientras.\n"); return executed; }
                String[] body = java.util.Arrays.copyOfRange(lines, bodyStart, bodyEnd); int cycles = 0;
                while (evaluateGrafisriptCondition(condition, variables) && cycles < 1000) {
                    int[] nestedCursor = new int[]{0};
                    executed += executeGrafisriptBlock(body, nestedCursor, variables, functions, output, depth + 1); cycles++;
                }
                if (cycles == 1000 && evaluateGrafisriptCondition(condition, variables)) output.append("Línea ").append(lineNumber).append(": se detuvo después de 1000 repeticiones para evitar un bucle infinito.\n");
                cursor[0] = bodyEnd + 1; continue;
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
                    executed += executeGrafisriptBlock(body, nestedCursor, variables, functions, output, depth + 1);
                }
                cursor[0] = bodyEnd + 1;
                continue;
            }

            if (line.startsWith("lista ")) {
                int listAssignment = line.indexOf('=');
                if (listAssignment > 0) {
                    String name = line.substring(6, listAssignment).trim(); String value = line.substring(listAssignment + 1).trim();
                    if (name.matches("[a-zA-Z_][a-zA-Z0-9_]*") && value.startsWith("[") && value.endsWith("]")) {
                        grafisriptLists.put(name, parseGrafisriptList(value));
                        output.append("Lista creada: ").append(name).append(" · elementos: ").append(grafisriptLists.get(name).size()).append('\n'); executed++;
                    } else output.append("Línea ").append(lineNumber).append(": usa Crear lista Nombre = [\\\"uno\\\", \\\"dos\\\"].\n");
                }
                continue;
            }
            if (line.startsWith("anadir ")) {
                int at = line.toLowerCase(java.util.Locale.ROOT).lastIndexOf(" a ");
                if (at > 0) {
                    String item = grafisriptDisplayValue(line.substring(7, at), variables); String name = line.substring(at + 3).trim();
                    java.util.List<String> list = grafisriptLists.get(name);
                    if (list == null) output.append("Línea ").append(lineNumber).append(": no existe la lista ").append(name).append(".\n");
                    else { list.add(item); output.append("Añadido a ").append(name).append(": ").append(item).append('\n'); executed++; }
                } else output.append("Línea ").append(lineNumber).append(": usa Añadir \\\"elemento\\\" a Nombre_lista.\n");
                continue;
            }
            if (line.startsWith("quitar ")) {
                int at = line.toLowerCase(java.util.Locale.ROOT).lastIndexOf(" de ");
                if (at > 0) {
                    String item = grafisriptDisplayValue(line.substring(7, at), variables); String name = line.substring(at + 4).trim();
                    java.util.List<String> list = grafisriptLists.get(name);
                    if (list == null) output.append("Línea ").append(lineNumber).append(": no existe la lista ").append(name).append(".\n");
                    else { list.remove(item); output.append("Quitado de ").append(name).append(": ").append(item).append('\n'); executed++; }
                } else output.append("Línea ").append(lineNumber).append(": usa Quitar \\\"elemento\\\" de Nombre_lista.\n");
                continue;
            }
            if (line.startsWith("sumar ") || line.startsWith("restar ")) {
                boolean add = line.startsWith("sumar "); String[] parts = line.split("\\s+", 3);
                if (parts.length == 3) {
                    try { String name = parts[1]; double current = variables.containsKey(name) ? variables.get(name) : 0.0;
                        double amount = evaluateGrafisriptExpression(parts[2], variables); double result = add ? current + amount : current - amount;
                        variables.put(name, result); output.append(name).append(" = ").append(result).append('\n'); executed++;
                    } catch (Exception ex) { output.append("Línea ").append(lineNumber).append(": cantidad inválida para ").append(parts[1]).append(".\n"); }
                } else output.append("Línea ").append(lineNumber).append(": faltan datos en la operación.\n");
                continue;
            }

            if (line.startsWith("variable ")) line = line.substring(9).trim();
            int assignment = line.indexOf('=');
            if (assignment > 0 && !line.contains("==") && !line.contains(">=") &&
                    !line.contains("<=") && !line.contains("!=")) {
                String name = line.substring(0, assignment).trim();
                if (name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    String assigned = line.substring(assignment + 1).trim();
                    if (assigned.length() >= 2 && assigned.startsWith("\"") && assigned.endsWith("\"")) {
                        String textValue = unquote(assigned); grafisriptTextVariables.put(name, textValue);
                        output.append(name).append(" = ").append(textValue).append('\n'); executed++; continue;
                    }
                    if (assigned.equalsIgnoreCase("verdadero") || assigned.equalsIgnoreCase("falso")) {
                        double boolValue = assigned.equalsIgnoreCase("verdadero") ? 1.0 : 0.0;
                        variables.put(name, boolValue); grafisriptTextVariables.put(name, boolValue == 1.0 ? "Verdadero" : "Falso");
                        output.append(name).append(" = ").append(grafisriptTextVariables.get(name)).append('\n'); executed++; continue;
                    }
                    try {
                        double value = evaluateGrafisriptExpression(assigned, variables);
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
                String message = grafisriptDisplayValue(line.substring(cut).trim(), variables);
                output.append(message).append('\n');
                executed++;
                continue;
            }

            if (executeCharacterCommand(line, output, lineNumber - 1)) { executed++; continue; }
            if (execute2DCommand(line, output, lineNumber - 1)) { executed++; continue; }

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
                            .append("jugador.saltar, crear_bloque2d, mover_sprite, saltar_sprite, capa, camara.zoom, camara.centrar, fisica.activar, fisica.pausar, mostrar_mensaje, variable, si, sino, repetir, ver_bloques.\n");
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
            if ((line.startsWith("si ") || line.startsWith("repetir ") || line.startsWith("mientras ") || line.startsWith("funcion ") || line.equals("sino {")) && line.endsWith("{")) nesting++;
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
            if (name.equalsIgnoreCase("verdadero")) return 1.0;
            if (name.equalsIgnoreCase("falso")) return 0.0;
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


    String scenePreviewStatus() {
        return scenePreview == null ? "LISTO" : (scenePreview.blocks.size() + " BLOQUES · " + scenePreview.sprites.size() + " SPRITES");
    }

    String activeProjectName() {
        return getPreferences(MODE_PRIVATE).getString("active_project", "Mi primer proyecto");
    }

    boolean execute2DCommand(String line, StringBuilder output, int lineIndex) {
        try {
            ScenePreview target = scenePreview != null ? scenePreview : new ScenePreview();
            if (line.startsWith("crear_bloque2d ")) {
                String[] v = line.substring(15).trim().split("\\s+");
                if (v.length < 2 || v.length > 3) throw new IllegalArgumentException("Uso: crear_bloque2d x y [capa]");
                int x = Integer.parseInt(v[0]), y = Integer.parseInt(v[1]);
                int layer = v.length == 3 ? Integer.parseInt(v[2]) : sceneLayer;
                if (x < 0 || x >= target.mapColumns || y < 0 || y >= target.mapRows || layer < 0 || layer > 2)
                    throw new IllegalArgumentException("x/y fuera del mapa; capa: 0 fondo, 1 mundo, 2 frente");
                target.rememberState(); target.blocks.add(new Point(x,y));
                target.blockColors.add(sceneBrushColor); target.blockLayers.add(layer);
                target.persist(); target.invalidate();
                output.append("Bloque 2D creado en (").append(x).append(", ").append(y).append("), capa ").append(layer).append('\n');
                return true;
            }
            if (line.startsWith("capa ")) {
                int layer = Integer.parseInt(line.substring(5).trim());
                if (layer < 0 || layer > 2) throw new IllegalArgumentException("Usa 0, 1 o 2");
                sceneLayer=layer; output.append("Capa activa: ").append(layer).append('\n');
                if(scenePreview!=null)scenePreview.invalidate(); return true;
            }
            if (line.startsWith("mover_sprite(") && line.endsWith(")")) {
                String[] v=line.substring(13,line.length()-1).split(",");
                if(v.length!=2)throw new IllegalArgumentException("Uso: mover_sprite(dx, dy)");
                if(target.sprites.isEmpty())throw new IllegalArgumentException("No hay sprites en la escena");
                int index=target.selectedSprite>=0?target.selectedSprite:0; SceneSprite sp=target.sprites.get(index);
                sp.x=Math.max(0f,Math.min(target.mapColumns-1f,sp.x+Float.parseFloat(v[0].trim())));
                sp.y=Math.max(0f,Math.min(target.mapRows-1f,sp.y+Float.parseFloat(v[1].trim())));
                target.persistSprites();target.invalidate();output.append("Sprite movido a (").append(sp.x).append(", ").append(sp.y).append(")\n");return true;
            }
            if (line.startsWith("saltar_sprite(") && line.endsWith(")")) {
                float force=Float.parseFloat(line.substring(14,line.length()-1).trim());
                if(force<=0||force>20)throw new IllegalArgumentException("Fuerza permitida: mayor que 0 y hasta 20");
                if(target.sprites.isEmpty())throw new IllegalArgumentException("No hay sprites en la escena");
                int index=target.selectedSprite>=0?target.selectedSprite:0;target.sprites.get(index).vy=-force;
                target.persistSprites();physicsRunning=true;if(scenePreview!=null)scenePreview.startPhysics();output.append("Salto 2D iniciado\n");return true;
            }
            if (line.startsWith("camara.zoom(") && line.endsWith(")")) {
                float factor=Float.parseFloat(line.substring(12,line.length()-1).trim());
                if(factor<0.5f||factor>2f)throw new IllegalArgumentException("Zoom permitido: 0.5 a 2");
                target.changeZoom(factor);output.append("Zoom de cámara actualizado\n");return true;
            }
            if (line.equals("camara.centrar()")) {target.resetCamera();output.append("Cámara centrada\n");return true;}
            if (line.equals("fisica.activar()")) {physicsRunning=true;if(scenePreview!=null)scenePreview.startPhysics();output.append("Física 2D activada\n");return true;}
            if (line.equals("fisica.pausar()")) {physicsRunning=false;if(scenePreview!=null)scenePreview.stopPhysics();output.append("Física 2D pausada\n");return true;}
        } catch(Exception ex) {
            output.append("Línea ").append(lineIndex+1).append(": ").append(ex.getMessage()==null?"parámetro 2D inválido":ex.getMessage()).append('\n');
            return true;
        }
        return false;
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
            if (line.startsWith("jugador.recibir_danio(") && line.endsWith(")")) {
                int damage = Integer.parseInt(line.substring(22, line.length() - 1).trim());
                if (damage < 0 || damage > 1000) throw new IllegalArgumentException("Daño permitido: 0 a 1000");
                int health = prefs.getInt("character_health_" + project, 100);
                health = Math.max(0, health - damage);
                prefs.edit().putInt("character_health_" + project, health).apply();
                output.append("Daño aplicado: ").append(damage)
                        .append(" · vida restante: ").append(health).append("\n");
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
        String project = activeProjectName();
        int columns = scenePreview != null ? scenePreview.mapColumns :
                Math.max(5, Math.min(64, getPreferences(MODE_PRIVATE).getInt("scene_map_w_" + project, 10)));
        int rows = scenePreview != null ? scenePreview.mapRows :
                Math.max(5, Math.min(64, getPreferences(MODE_PRIVATE).getInt("scene_map_h_" + project, 10)));
        int bx = Math.max(0, Math.min(columns - 1, x));
        int by = Math.max(0, Math.min(rows - 1, z));
        if (scenePreview != null) {
            scenePreview.blocks.add(new Point(bx, by));
            scenePreview.blockColors.add(sceneBrushColor);
            scenePreview.blockLayers.add(sceneLayer);
            scenePreview.persist();
            scenePreview.invalidate();
            return;
        }
        String key = "scene_blocks_" + project;
        String saved = getPreferences(MODE_PRIVATE).getString(key, "");
        String entry = bx + "," + by + "," + sceneBrushColor + "," + sceneLayer;
        String updated = saved.isEmpty() ? entry : saved + ";" + entry;
        getPreferences(MODE_PRIVATE).edit().putString(key, updated).apply();
    }

    void clearProjectScene() {
        String project = activeProjectName();
        getPreferences(MODE_PRIVATE).edit()
                .putString("scene_blocks_" + project, "")
                .putString("scene_sprites_" + project, "")
                .remove("character_name_" + project)
                .remove("character_x_" + project)
                .remove("character_z_" + project)
                .apply();
        if (scenePreview != null) {
            scenePreview.blocks.clear();
            scenePreview.blockColors.clear();
            scenePreview.blockLayers.clear();
            for (SceneSprite sprite : scenePreview.sprites) {
                if (sprite.bitmap != null && !sprite.bitmap.isRecycled()) sprite.bitmap.recycle();
                for (Bitmap frame : sprite.animationFrames)
                    if (frame != null && !frame.isRecycled()) frame.recycle();
            }
            scenePreview.sprites.clear();
            scenePreview.selectedSprite = -1;
            scenePreview.persist();
            scenePreview.persistSprites();
            scenePreview.invalidate();
        }
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

    class MobileButton {
        String id, title, icon, action, iconFile="", script="";
        Bitmap iconBitmap;
        MobileButton(String i,String t,String ic,String a){this(i,t,ic,a,"","");}
        MobileButton(String i,String t,String ic,String a,String file){this(i,t,ic,a,file,"");}
        MobileButton(String i,String t,String ic,String a,String file,String code){
            id=i;title=t;icon=ic;action=a;iconFile=file==null?"":file;script=code==null?"":code;
            if(!iconFile.isEmpty())iconBitmap=BitmapFactory.decodeFile(new java.io.File(new java.io.File(getFilesDir(),"sprites"),iconFile).getAbsolutePath());
        }
    }

    void showMapSizeDialog(){
        LinearLayout form=vertical();form.setPadding(16,8,16,0);
        EditText width=new EditText(this);width.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);width.setHint("Ancho (5–64)");width.setText(String.valueOf(scenePreview.mapColumns));form.addView(width);
        EditText height=new EditText(this);height.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);height.setHint("Alto (5–64)");height.setText(String.valueOf(scenePreview.mapRows));form.addView(height);
        new android.app.AlertDialog.Builder(this).setTitle("Tamaño del mapa").setMessage("Configura las dimensiones de tu mundo 2D.").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("APLICAR",(d,w)->{try{int c=Integer.parseInt(width.getText().toString()),r=Integer.parseInt(height.getText().toString());if(c<5||c>64||r<5||r>64)throw new NumberFormatException();scenePreview.setMapSize(c,r);openPage(2);}catch(Exception ex){Toast.makeText(this,"Usa valores entre 5 y 64",Toast.LENGTH_LONG).show();}}).show();
    }

    void manageMobileButtons(){
        if(scenePreview==null||scenePreview.mobileButtons.isEmpty()){
            Toast.makeText(this,"Todavía no hay botones móviles creados",Toast.LENGTH_SHORT).show();return;
        }
        String[] names=new String[scenePreview.mobileButtons.size()];
        for(int i=0;i<names.length;i++){MobileButton b=scenePreview.mobileButtons.get(i);names[i]=b.title+"  ·  "+b.action;}
        new android.app.AlertDialog.Builder(this).setTitle("Administrar botones")
            .setItems(names,(dialog,which)->{
                MobileButton selected=scenePreview.mobileButtons.get(which);
                new android.app.AlertDialog.Builder(this).setTitle(selected.title)
                    .setItems(new String[]{"EDITAR","ELIMINAR"},(d,choice)->{
                        if(choice==0){editMobileButtonDialog(which);return;}
                        new android.app.AlertDialog.Builder(this).setTitle("Eliminar botón")
                            .setMessage("¿Quieres eliminar \""+selected.title+"\" del proyecto?")
                            .setNegativeButton("Cancelar",null)
                            .setPositiveButton("ELIMINAR",(confirm,w)->{
                                MobileButton removed=scenePreview.mobileButtons.remove(which);
                                if(removed.iconBitmap!=null&&!removed.iconBitmap.isRecycled())removed.iconBitmap.recycle();
                                scenePreview.persistMobileButtons();scenePreview.invalidate();openPage(2);
                                Toast.makeText(this,"Botón eliminado",Toast.LENGTH_SHORT).show();
                            }).show();
                    }).setNegativeButton("Cancelar",null).show();
            }).setNegativeButton("Cerrar",null).show();
    }

    void editMobileButtonDialog(int index){
        if(scenePreview==null||index<0||index>=scenePreview.mobileButtons.size())return;
        MobileButton existing=scenePreview.mobileButtons.get(index);
        LinearLayout form=vertical();form.setPadding(16,8,16,0);
        EditText title=new EditText(this);title.setHint("Nombre del botón");title.setText(existing.title);form.addView(title);
        EditText icon=new EditText(this);icon.setHint("Icono o emoji");icon.setText(existing.icon);form.addView(icon);
        Spinner action=new Spinner(this);
        String[] actions={"Mover izquierda","Mover derecha","Mover arriba","Mover abajo","Saltar","Acción: activar evento","Programar con El Grafisript"};
        String[] keys={"left","right","up","down","jump","event","script"};
        action.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,actions));
        for(int i=0;i<keys.length;i++)if(keys[i].equals(existing.action)){action.setSelection(i);break;}
        form.addView(action);
        TextView codeLabel=label("Código de El Grafisript (para evento o script)",12,gray);codeLabel.setPadding(0,10,0,4);form.addView(codeLabel);
        EditText script=new EditText(this);script.setText(existing.script);script.setHint("Escribe instrucciones de El Grafisript aquí");script.setMinLines(3);script.setGravity(Gravity.TOP|Gravity.START);script.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);form.addView(script);
        new android.app.AlertDialog.Builder(this).setTitle("Editar botón móvil")
            .setView(form).setNegativeButton("Cancelar",null)
            .setPositiveButton("GUARDAR",(d,w)->{
                String n=title.getText().toString().trim();String ic=icon.getText().toString().trim();
                if(n.isEmpty())n="Botón personalizado";if(ic.isEmpty())ic=n.substring(0,1).toUpperCase(java.util.Locale.ROOT);
                String code=script.getText().toString().trim();
                if(code.length()>4000){Toast.makeText(this,"El código no puede superar 4000 caracteres",Toast.LENGTH_LONG).show();return;}
                if(action.getSelectedItemPosition()==6&&code.isEmpty()){Toast.makeText(this,"Escribe el código de El Grafisript",Toast.LENGTH_LONG).show();return;}
                existing.title=n;existing.icon=ic;existing.action=keys[Math.max(0,Math.min(keys.length-1,action.getSelectedItemPosition()))];existing.script=code;
                scenePreview.persistMobileButtons();scenePreview.invalidate();openPage(2);
                Toast.makeText(this,"Botón actualizado",Toast.LENGTH_SHORT).show();
            }).show();
    }

    void createMobileButtonDialog() { createMobileButtonDialog(""); }
    void createMobileButtonDialog(String iconFile) {
        LinearLayout form=vertical(); form.setPadding(16,8,16,0);
        EditText title=new EditText(this); title.setHint("Nombre del botón (ej. Disparar)"); form.addView(title);
        EditText icon=new EditText(this); icon.setHint("Icono o emoji (ej. ★, A, ⚔)"); form.addView(icon);
        Spinner action=new Spinner(this);
        String[] actions={"Mover izquierda","Mover derecha","Mover arriba","Mover abajo","Saltar","Acción: activar evento","Programar con El Grafisript"};
        action.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,actions));form.addView(action);
        TextView codeLabel=label("Código que se ejecutará al tocar el botón",12,gray);codeLabel.setPadding(0,10,0,4);form.addView(codeLabel);
        EditText script=new EditText(this);script.setHint("mostrar_mensaje \"¡Botón pulsado!\"\ncamara.zoom(1.2)");script.setMinLines(3);script.setGravity(Gravity.TOP|Gravity.START);script.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);form.addView(script);
        new android.app.AlertDialog.Builder(this).setTitle("Crear botón para móviles")
            .setMessage("Diseña el nombre y el icono, y elige qué hará al tocarlo.")
            .setView(form).setNegativeButton("Cancelar",null)
            .setPositiveButton("Crear",(d,w)->{
                String n=title.getText().toString().trim();String ic=icon.getText().toString().trim();
                if(n.isEmpty())n="Botón personalizado";if(ic.isEmpty())ic=n.substring(0,1).toUpperCase(java.util.Locale.ROOT);
                if(scenePreview==null)scenePreview=new ScenePreview();
                String code=script.getText().toString().trim();
                if(code.length()>4000){Toast.makeText(this,"El código no puede superar 4000 caracteres",Toast.LENGTH_LONG).show();return;}
                if(action.getSelectedItemPosition()==6&&code.isEmpty()){Toast.makeText(this,"Escribe el código de El Grafisript para este botón",Toast.LENGTH_LONG).show();return;}
                scenePreview.addMobileButton(n,ic,action.getSelectedItemPosition(),iconFile,code);
                Toast.makeText(this,"Botón móvil creado: "+n,Toast.LENGTH_SHORT).show();openPage(2);
            }).show();
    }

    class SceneSprite {
        String fileName;
        float x, y;
        float width = 1f, height = 1f, rotation = 0f;
        boolean solid = true;
        float vx = 0f, vy = 0f;
        Bitmap bitmap;
        java.util.ArrayList<Bitmap> animationFrames=new java.util.ArrayList<>();
        java.util.ArrayList<String> animationFrameNames=new java.util.ArrayList<>();
        int currentFrameIndex = 0;
        int animationFps = 8;
        boolean animationPlaying = true;
        boolean animationLoop = true;
        long lastFrameAdvance = 0L;
        SceneSprite(String name, float px, float py, boolean isSolid) {
            fileName = name; x = px; y = py; solid = isSolid;
            java.io.File file = new java.io.File(new java.io.File(getFilesDir(), "sprites"), name);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            int sample = 1;
            while (options.outWidth / sample > 256 || options.outHeight / sample > 256)
                sample *= 2;
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        }
    }

    class ScenePreview extends View {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        java.util.ArrayList<Point> blocks = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> blockColors = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> blockLayers = new java.util.ArrayList<>();
        java.util.ArrayList<SceneSprite> sprites = new java.util.ArrayList<>();
        java.util.ArrayList<MobileButton> mobileButtons = new java.util.ArrayList<>();
        int activeMobileButtonPointer=-1;
        int activeMobileTouchId=-1;
        int mapColumns=10,mapRows=10;
        java.util.ArrayList<String> undoHistory = new java.util.ArrayList<>();
        java.util.ArrayList<String> redoHistory = new java.util.ArrayList<>();
        int draggingSprite = -1;
        float spriteDragOffsetX = 0f, spriteDragOffsetY = 0f;
        int selectedSprite=-1;
        boolean paintingBlocks=false;
        int gestureStartCol=-1,gestureStartRow=-1;
        int lastPaintedCol=-1,lastPaintedRow=-1;
        float cameraZoom=1f,cameraOffsetX=0f,cameraOffsetY=0f,lastPanX,lastPanY;
        boolean multiTouchGesture=false;
        android.view.ScaleGestureDetector scaleDetector;

        ScenePreview() {
            super(BlockForgeActivity.this);
            setBackground(shape(Color.rgb(7,13,23)));
            cameraZoom=getPreferences(MODE_PRIVATE).getFloat("scene_camera_zoom_"+activeProjectName(),getPreferences(MODE_PRIVATE).getFloat("scene_camera_zoom",1f));
            cameraOffsetX=getPreferences(MODE_PRIVATE).getFloat("scene_camera_x_"+activeProjectName(),getPreferences(MODE_PRIVATE).getFloat("scene_camera_x",0f));
            cameraOffsetY=getPreferences(MODE_PRIVATE).getFloat("scene_camera_y_"+activeProjectName(),getPreferences(MODE_PRIVATE).getFloat("scene_camera_y",0f));
            scaleDetector=new android.view.ScaleGestureDetector(BlockForgeActivity.this,new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override public boolean onScale(android.view.ScaleGestureDetector d){float old=cameraZoom;cameraZoom=Math.max(0.65f,Math.min(2.5f,cameraZoom*d.getScaleFactor()));float q=cameraZoom/old;cameraOffsetX=d.getFocusX()-(d.getFocusX()-cameraOffsetX)*q;cameraOffsetY=d.getFocusY()-(d.getFocusY()-cameraOffsetY)*q;saveCamera();invalidate();return true;}});
            String active = getPreferences(MODE_PRIVATE).getString("active_project", "Mi primer proyecto");
            loadMobileButtons();
            mapColumns=Math.max(5,Math.min(64,getPreferences(MODE_PRIVATE).getInt("scene_map_w_"+active,10)));
            mapRows=Math.max(5,Math.min(64,getPreferences(MODE_PRIVATE).getInt("scene_map_h_"+active,10)));
            String saved = getPreferences(MODE_PRIVATE).getString("scene_blocks_" + active, "");
            if (!saved.isEmpty()) {
                for (String pair : saved.split(";")) {
                    String[] xy = pair.split(",");
                    if (xy.length >= 2) {
                        try {
                            blocks.add(new Point(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
                            blockColors.add(xy.length>=3?Integer.parseInt(xy[2]):Color.rgb(55,135,90));
                            blockLayers.add(xy.length>=4?Math.max(0,Math.min(2,Integer.parseInt(xy[3]))):1);
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
            String spriteData = getPreferences(MODE_PRIVATE)
                    .getString("scene_sprites_" + active, "");
            if (!spriteData.isEmpty()) for (String item : spriteData.split(";")) {
                String[] parts = item.split("\\|");
                if (parts.length >= 3) try {
                    SceneSprite loaded = new SceneSprite(parts[0], Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]), parts.length < 4 || Boolean.parseBoolean(parts[3]));
                    if (parts.length > 4) loaded.vx = Float.parseFloat(parts[4]);
                    if(parts.length>5)loaded.vy=Float.parseFloat(parts[5]);
                    if(parts.length>6&&!parts[6].isEmpty())for(String fn:parts[6].split(",")){java.io.File ff=new java.io.File(new java.io.File(getFilesDir(),"sprites"),fn);Bitmap fb=BitmapFactory.decodeFile(ff.getAbsolutePath());if(fb!=null){loaded.animationFrames.add(fb);loaded.animationFrameNames.add(fn);}}
                    if(parts.length>7)loaded.currentFrameIndex=Math.max(0,Math.min(loaded.animationFrames.size()-1,Integer.parseInt(parts[7])));
                    if(parts.length>8)loaded.animationFps=Math.max(1,Math.min(60,Integer.parseInt(parts[8])));
                    if(parts.length>9)loaded.animationPlaying=Boolean.parseBoolean(parts[9]);
                    if(parts.length>10)loaded.animationLoop=Boolean.parseBoolean(parts[10]);
                    if(parts.length>11)loaded.width=Math.max(0.25f,Math.min((float)mapColumns,Float.parseFloat(parts[11])));
                    if(parts.length>12)loaded.height=Math.max(0.25f,Math.min((float)mapRows,Float.parseFloat(parts[12])));
                    if(parts.length>13)loaded.rotation=Math.max(-360f,Math.min(360f,Float.parseFloat(parts[13])));
                    loaded.x=Math.max(0f,Math.min(mapColumns-loaded.width,loaded.x));
                    loaded.y=Math.max(0f,Math.min(mapRows-loaded.height,loaded.y));
                    sprites.add(loaded);
                } catch (Exception ignored) { }
            }
        }

        void loadMobileButtons(){
            mobileButtons.clear();String raw=getPreferences(MODE_PRIVATE).getString("mobile_buttons_"+activeProjectName(),"");
            try{org.json.JSONArray arr=new org.json.JSONArray(raw);for(int i=0;i<arr.length();i++){org.json.JSONObject o=arr.getJSONObject(i);mobileButtons.add(new MobileButton(o.optString("id"),o.optString("title"),o.optString("icon"),o.optString("action","left"),o.optString("iconFile",""),o.optString("script","")));}}catch(Exception ignored){}
        }
        void persistMobileButtons(){
            org.json.JSONArray arr=new org.json.JSONArray();for(MobileButton b:mobileButtons){org.json.JSONObject o=new org.json.JSONObject();try{o.put("id",b.id);o.put("title",b.title);o.put("icon",b.icon);o.put("action",b.action);o.put("iconFile",b.iconFile);o.put("script",b.script);arr.put(o);}catch(Exception ignored){}}
            getPreferences(MODE_PRIVATE).edit().putString("mobile_buttons_"+activeProjectName(),arr.toString()).apply();
        }
        void addMobileButton(String title,String icon,int actionIndex,String iconFile,String script){
            String[] actions={"left","right","up","down","jump","event","script"};
            mobileButtons.add(new MobileButton("btn_"+System.currentTimeMillis(),title,icon,actions[Math.max(0,Math.min(actions.length-1,actionIndex))],iconFile,script));
            persistMobileButtons();invalidate();
        }
        void installDefaultMobileButtons(){
            mobileButtons.clear();mobileButtons.add(new MobileButton("left","Izquierda","◀","left"));
            mobileButtons.add(new MobileButton("right","Derecha","▶","right"));mobileButtons.add(new MobileButton("jump","Saltar","▲","jump"));
            persistMobileButtons();invalidate();Toast.makeText(BlockForgeActivity.this,"Controles predeterminados instalados",Toast.LENGTH_SHORT).show();
        }
        void runMobileButton(MobileButton b){
            if("script".equals(b.action)){
                StringBuilder output=new StringBuilder();
                String[] lines=b.script.split("\\r?\\n");
                executeGrafisriptBlock(lines,new int[]{0},new java.util.HashMap<String,Double>(),new java.util.HashMap<String,String[]>(),output,0);
                if(output.length()>0)Toast.makeText(BlockForgeActivity.this,output.toString().trim(),Toast.LENGTH_LONG).show();
                refreshScenePreview();persistSprites();invalidate();return;
            }
            if("event".equals(b.action)){
                if(!b.script.trim().isEmpty()){
                    StringBuilder output=new StringBuilder();
                    executeGrafisriptBlock(b.script.split("\\r?\\n"),new int[]{0},
                            new java.util.HashMap<String,Double>(),
                            new java.util.HashMap<String,String[]>(),output,0);
                    if(output.length()>0)Toast.makeText(BlockForgeActivity.this,output.toString().trim(),Toast.LENGTH_LONG).show();
                    refreshScenePreview();persistSprites();invalidate();
                }else{
                    Toast.makeText(BlockForgeActivity.this,"Evento activado: "+b.title,Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if(selectedSprite<0||selectedSprite>=sprites.size()){Toast.makeText(BlockForgeActivity.this,"Selecciona un sprite para controlar",Toast.LENGTH_SHORT).show();return;}
            SceneSprite sp=sprites.get(selectedSprite);
            switch(b.action){
                case "left":sp.vx=-5f;break;case "right":sp.vx=5f;break;
                case "up":sp.vy=-5f;break;case "down":sp.vy=5f;break;
                case "jump":sp.vy=-9f;if(!physicsRunning){physicsRunning=true;startPhysics();}break;
            }
            persistSprites();invalidate();
        }
        void drawMobileButtons(Canvas canvas){
            if(!scenePlayMode)return;float size=Math.max(48f,Math.min(68f,getWidth()*0.14f));float gap=10f;
            float right=getWidth()-18f, bottom=getHeight()-18f;
            for(int i=0;i<mobileButtons.size();i++){MobileButton b=mobileButtons.get(i);float cx=right-(i%3)*(size+gap)-size/2f;float cy=bottom-(i/3)*(size+gap)-size/2f;
                paint.setColor(Color.argb(175,25,45,72));canvas.drawCircle(cx,cy,size/2f,paint);
                if(b.iconBitmap!=null&&!b.iconBitmap.isRecycled()){paint.setColor(Color.WHITE);canvas.drawBitmap(b.iconBitmap,null,new RectF(cx-size*.32f,cy-size*.32f,cx+size*.32f,cy+size*.32f),paint);}else{paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(size*.42f);canvas.drawText(b.icon,cx,cy+size*.14f,paint);}
            }
        }
        int hitMobileButton(float x,float y){
            float size=Math.max(48f,Math.min(68f,getWidth()*0.14f));float gap=10f,right=getWidth()-18f,bottom=getHeight()-18f;
            for(int i=0;i<mobileButtons.size();i++){float cx=right-(i%3)*(size+gap)-size/2f;float cy=bottom-(i/3)*(size+gap)-size/2f;if(Math.hypot(x-cx,y-cy)<=size/2f)return i;}return -1;
        }

        void setMapSize(int columns,int rows){mapColumns=columns;mapRows=rows;for(SceneSprite sp:sprites){sp.width=Math.min(sp.width,mapColumns);sp.height=Math.min(sp.height,mapRows);sp.x=Math.max(0,Math.min(mapColumns-sp.width,sp.x));sp.y=Math.max(0,Math.min(mapRows-sp.height,sp.y));}for(int i=blocks.size()-1;i>=0;i--){Point p=blocks.get(i);if(p.x>=mapColumns||p.y>=mapRows){blocks.remove(i);if(i<blockColors.size())blockColors.remove(i);if(i<blockLayers.size())blockLayers.remove(i);}}getPreferences(MODE_PRIVATE).edit().putInt("scene_map_w_"+activeProjectName(),mapColumns).putInt("scene_map_h_"+activeProjectName(),mapRows).apply();persist();persistSprites();invalidate();Toast.makeText(BlockForgeActivity.this,"Mapa: "+mapColumns+" × "+mapRows,Toast.LENGTH_SHORT).show();}

        void persistSprites() {
            StringBuilder data = new StringBuilder();
            for (SceneSprite sprite : sprites) {
                if (data.length() > 0) data.append(';');
                data.append(sprite.fileName).append('|').append(sprite.x).append('|')
                        .append(sprite.y).append('|').append(sprite.solid).append('|')
                        .append(sprite.vx).append('|').append(sprite.vy).append('|');
                for(int fi=0;fi<sprite.animationFrameNames.size();fi++){if(fi>0)data.append(',');data.append(sprite.animationFrameNames.get(fi));}
                data.append('|').append(sprite.currentFrameIndex).append('|').append(sprite.animationFps).append('|').append(sprite.animationPlaying).append('|').append(sprite.animationLoop)
                        .append('|').append(sprite.width).append('|').append(sprite.height).append('|').append(sprite.rotation);
            }
            getPreferences(MODE_PRIVATE).edit()
                    .putString("scene_sprites_" + activeProjectName(), data.toString()).apply();
        }

        void addSprite(String name) {
            SceneSprite created = new SceneSprite(name, 4, 4, true);
            created.vy = 0f;
            sprites.add(created);
            selectedSprite = sprites.size() - 1;
            persistSprites();
            invalidate();
        }

        SceneSprite selectedSpriteObject(){return selectedSprite>=0&&selectedSprite<sprites.size()?sprites.get(selectedSprite):null;}
        void addAnimationFrame(String name){SceneSprite sp=selectedSpriteObject();if(sp==null)return;java.io.File f=new java.io.File(new java.io.File(getFilesDir(),"sprites"),name);Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());if(b==null)return;sp.animationFrames.add(b);sp.animationFrameNames.add(name);sp.currentFrameIndex=sp.animationFrames.size()-1;sp.animationPlaying=false;persistSprites();invalidate();startAnimationTicker();}
        void selectAnimationFrame(int delta){SceneSprite sp=selectedSpriteObject();if(sp==null||sp.animationFrames.isEmpty()){Toast.makeText(BlockForgeActivity.this,"Añade fotogramas primero",Toast.LENGTH_SHORT).show();return;}sp.currentFrameIndex=(sp.currentFrameIndex+delta+sp.animationFrames.size())%sp.animationFrames.size();sp.animationPlaying=false;persistSprites();invalidate();}
        void toggleAnimationPlayback(){SceneSprite sp=selectedSpriteObject();if(sp==null||sp.animationFrames.size()<2){Toast.makeText(BlockForgeActivity.this,"Añade al menos 2 fotogramas",Toast.LENGTH_SHORT).show();return;}sp.animationPlaying=!sp.animationPlaying;if(sp.animationPlaying&&sp.currentFrameIndex>=sp.animationFrames.size()-1&&!sp.animationLoop)sp.currentFrameIndex=0;sp.lastFrameAdvance=android.os.SystemClock.uptimeMillis();persistSprites();startAnimationTicker();invalidate();}
        void toggleAnimationLoop(){SceneSprite sp=selectedSpriteObject();if(sp==null||sp.animationFrames.size()<2){Toast.makeText(BlockForgeActivity.this,"Añade al menos 2 fotogramas",Toast.LENGTH_SHORT).show();return;}sp.animationLoop=!sp.animationLoop;persistSprites();invalidate();Toast.makeText(BlockForgeActivity.this,sp.animationLoop?"Animación en bucle":"Animación de una sola pasada",Toast.LENGTH_SHORT).show();}
        void deleteAnimationFrame(){SceneSprite sp=selectedSpriteObject();if(sp==null||sp.animationFrames.isEmpty()){Toast.makeText(BlockForgeActivity.this,"No hay fotogramas para eliminar",Toast.LENGTH_SHORT).show();return;}int index=Math.max(0,Math.min(sp.currentFrameIndex,sp.animationFrames.size()-1));Bitmap removed=sp.animationFrames.remove(index);if(removed!=null&&!removed.isRecycled())removed.recycle();sp.animationFrameNames.remove(index);sp.currentFrameIndex=sp.animationFrames.isEmpty()?0:Math.min(index,sp.animationFrames.size()-1);persistSprites();invalidate();}
        String animationStatus(){SceneSprite sp=selectedSpriteObject();return sp==null?"ANIMACIÓN · selecciona un sprite":"FOTOGRAMA "+(sp.animationFrames.isEmpty()?0:sp.currentFrameIndex+1)+"/"+sp.animationFrames.size()+" · "+sp.animationFps+" FPS · "+(sp.animationLoop?"BUCLE":"UNA PASADA");}
        void moveSelectedSprite(float dx,float dy){if(selectedSprite<0||selectedSprite>=sprites.size()){Toast.makeText(BlockForgeActivity.this,"Selecciona un sprite en la escena",Toast.LENGTH_SHORT).show();return;}SceneSprite sp=sprites.get(selectedSprite);sp.x=Math.max(0,Math.min(mapColumns-sp.width,sp.x+dx));sp.y=Math.max(0,Math.min(mapRows-sp.height,sp.y+dy));persistSprites();invalidate();}
        void jumpSelectedSprite(){if(selectedSprite<0||selectedSprite>=sprites.size())return;sprites.get(selectedSprite).vy=-9f;if(!physicsRunning){physicsRunning=true;startPhysics();}}
        void saveCamera(){String project=activeProjectName();getPreferences(MODE_PRIVATE).edit().putFloat("scene_camera_zoom_"+project,cameraZoom).putFloat("scene_camera_x_"+project,cameraOffsetX).putFloat("scene_camera_y_"+project,cameraOffsetY).apply();}
        void changeZoom(float f){float old=cameraZoom;cameraZoom=Math.max(.65f,Math.min(2.5f,cameraZoom*f));float cx=getWidth()/2f,cy=getHeight()/2f;cameraOffsetX=cx-(cx-cameraOffsetX)*(cameraZoom/old);cameraOffsetY=cy-(cy-cameraOffsetY)*(cameraZoom/old);saveCamera();invalidate();}
        void resetCamera(){cameraZoom=1f;cameraOffsetX=0f;cameraOffsetY=0f;cameraPanMode=false;saveCamera();invalidate();}
        void toggleSelectedSpriteSolid() {
            if (selectedSprite < 0 || selectedSprite >= sprites.size()) {
                Toast.makeText(BlockForgeActivity.this, "Primero toca un sprite para seleccionarlo",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            SceneSprite sprite = sprites.get(selectedSprite);
            sprite.solid = !sprite.solid;
            persistSprites();
            invalidate();
            Toast.makeText(BlockForgeActivity.this,
                    sprite.solid ? "Sprite sólido: bloquea colisiones" : "Sprite atravesable",
                    Toast.LENGTH_SHORT).show();
        }

        void deleteSelectedSprite() {
            if (selectedSprite < 0 || selectedSprite >= sprites.size()) {
                Toast.makeText(BlockForgeActivity.this, "No hay ningún sprite seleccionado",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            SceneSprite removed = sprites.remove(selectedSprite);
            if(removed.bitmap!=null)removed.bitmap.recycle();for(Bitmap b:removed.animationFrames)if(b!=null)b.recycle();
            selectedSprite = -1;
            draggingSprite = -1;
            persistSprites();
            invalidate();
        }

        long lastPhysicsNanos = 0L;
        float accumulator = 0f;
        final float fixedStep = 1f / 60f;
        final Runnable physicsLoop = new Runnable() {
            @Override public void run() {
                if (!physicsRunning || !isAttachedToWindow()) return;
                long now = System.nanoTime();
                if (lastPhysicsNanos == 0L) lastPhysicsNanos = now;
                float frameTime = Math.min(0.05f, (now - lastPhysicsNanos) / 1_000_000_000f);
                lastPhysicsNanos = now;
                accumulator += frameTime;
                int steps = 0;
                while (accumulator >= fixedStep && steps < 3) {
                    physicsStep(fixedStep);
                    accumulator -= fixedStep;
                    steps++;
                }
                invalidate();
                postDelayed(this, Math.max(8, 1000 / Math.max(30, targetAnimationFps)));
            }
        };

        void startPhysics() {
            lastPhysicsNanos = 0L;
            accumulator = 0f;
            removeCallbacks(physicsLoop);
            postOnAnimation(physicsLoop);
        }

        void stopPhysics() {
            removeCallbacks(physicsLoop);
            lastPhysicsNanos = 0L;
            persistSprites();
        }

        void physicsStep(float dt) {
            for (SceneSprite sprite : sprites) {
                float oldX = sprite.x, oldY = sprite.y;
                sprite.vx = Math.max(-8f, Math.min(8f, sprite.vx));
                sprite.vy = Math.max(-18f, Math.min(18f, sprite.vy + physicsGravity * dt));

                float nextX = Math.max(0f, Math.min(mapColumns - sprite.width, oldX + sprite.vx * dt));
                boolean horizontalHit = false;
                if (sceneCollisionsEnabled && sprite.solid) {
                    for (int i = 0; i < blocks.size(); i++) {
                        int layer = i < blockLayers.size() ? blockLayers.get(i) : 1;
                        if (layer == 0) continue;
                        Point block = blocks.get(i);
                        boolean overlapX = nextX < block.x + 1f && nextX + sprite.width > block.x;
                        boolean overlapY = oldY < block.y + 1f && oldY + sprite.height > block.y;
                        if (overlapX && overlapY) { horizontalHit = true; break; }
                    }
                    if (!horizontalHit) for (SceneSprite other : sprites) {
                        if (other != sprite && other.solid &&
                                nextX < other.x + other.width && nextX + sprite.width > other.x &&
                                oldY < other.y + other.height && oldY + sprite.height > other.y) {
                            horizontalHit = true;
                            break;
                        }
                    }
                }
                sprite.x = horizontalHit ? oldX : nextX;
                if (horizontalHit || nextX <= 0f || nextX >= mapColumns - sprite.width) sprite.vx = 0f;

                float nextY = oldY + sprite.vy * dt;
                boolean landed = false;
                if (sceneCollisionsEnabled && sprite.solid) {
                    for (int i = 0; i < blocks.size(); i++) {
                        int layer = i < blockLayers.size() ? blockLayers.get(i) : 1;
                        if (layer == 0) continue;
                        Point block = blocks.get(i);
                        boolean overlapX = sprite.x < block.x + 1f && sprite.x + sprite.width > block.x;
                        if (!overlapX) continue;
                        float top = block.y - sprite.height;
                        float bottom = block.y + 1f;
                        if (sprite.vy >= 0f && oldY + sprite.height <= block.y && nextY + sprite.height >= block.y) {
                            nextY = top;
                            landed = true;
                        } else if (sprite.vy < 0f && oldY >= bottom && nextY <= bottom) {
                            nextY = bottom;
                            sprite.vy = 0f;
                        }
                    }
                    for (SceneSprite other : sprites) {
                        if (other == sprite || !other.solid ||
                                sprite.x >= other.x + other.width || sprite.x + sprite.width <= other.x) continue;
                        float platformTop = other.y - sprite.height;
                        if (sprite.vy >= 0f && oldY + sprite.height <= other.y && nextY + sprite.height >= other.y) {
                            nextY = platformTop;
                            landed = true;
                        } else if (sprite.vy < 0f && oldY >= other.y + other.height &&
                                nextY <= other.y + other.height) {
                            nextY = other.y + other.height;
                            sprite.vy = 0f;
                        }
                    }
                }

                if (nextY >= mapRows - sprite.height) {
                    nextY = mapRows - sprite.height;
                    landed = true;
                }
                if (nextY <= 0f) {
                    nextY = 0f;
                    if (sprite.vy < 0f) sprite.vy = 0f;
                }
                sprite.y = Math.max(0f, Math.min(mapRows - 1f, nextY));
                if (landed) sprite.vy = sprite.vy > 3f ? -sprite.vy * physicsBounce : 0f;
                sprite.vx *= 0.88f;
                if (Math.abs(sprite.vx) < 0.025f) sprite.vx = 0f;
                if (Math.abs(sprite.vy) < 0.025f) sprite.vy = 0f;
            }
        }

        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();startAnimationTicker();}
        @Override protected void onDetachedFromWindow(){removeCallbacks(animationTicker);removeCallbacks(physicsLoop);persistSprites();persist();saveCamera();super.onDetachedFromWindow();}
        final Runnable animationTicker=new Runnable(){@Override public void run(){if(!isAttachedToWindow())return;boolean active=false,stopped=false;long delay=1000;for(SceneSprite sp:sprites){if(sp.animationPlaying&&sp.animationFrames.size()>1){long frameDelay=Math.max(16,1000/Math.max(1,sp.animationFps));delay=Math.min(delay,frameDelay);long now=android.os.SystemClock.uptimeMillis();if(now-sp.lastFrameAdvance>=frameDelay){if(sp.currentFrameIndex>=sp.animationFrames.size()-1&&!sp.animationLoop){sp.currentFrameIndex=sp.animationFrames.size()-1;sp.animationPlaying=false;stopped=true;}else{sp.currentFrameIndex=(sp.currentFrameIndex+1)%sp.animationFrames.size();sp.lastFrameAdvance=now;}}if(sp.animationPlaying)active=true;}}if(stopped)persistSprites();if(active||stopped)invalidate();if(active)postDelayed(this,Math.max(8,delay));}};
        void startAnimationTicker(){removeCallbacks(animationTicker);postDelayed(animationTicker,100);}

        void persist() {
            StringBuilder data = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                Point p = blocks.get(i);
                if (data.length() > 0) data.append(';');
                int color = i < blockColors.size() ? blockColors.get(i) : sceneBrushColor;
                int layer=i<blockLayers.size()?blockLayers.get(i):1;
                data.append(p.x).append(',').append(p.y).append(',').append(color).append(',').append(layer);
            }
            String active = getPreferences(MODE_PRIVATE).getString("active_project", "Mi primer proyecto");
            getPreferences(MODE_PRIVATE).edit().putString("scene_blocks_" + active, data.toString()).apply();
        }

        String captureState() {
            StringBuilder state = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                if (state.length() > 0) state.append(';');
                Point point = blocks.get(i);
                int color = i < blockColors.size() ? blockColors.get(i) : sceneBrushColor;
                int layer=i<blockLayers.size()?blockLayers.get(i):1;
                state.append(point.x).append(',').append(point.y).append(',').append(color).append(',').append(layer);
            }
            return state.toString();
        }

        void rememberState() {
            undoHistory.add(captureState());
            if (undoHistory.size() > 30) undoHistory.remove(0);
            redoHistory.clear();
        }

        void restoreBlockState(String state) {
            blocks.clear();
            blockColors.clear();
            blockLayers.clear();
            if (!state.isEmpty()) {
                for (String entry : state.split(";")) {
                    String[] values = entry.split(",");
                    try {
                        blocks.add(new Point(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
                        blockColors.add(values.length > 2 ? Integer.parseInt(values[2]) : sceneBrushColor);
                        blockLayers.add(values.length > 3 ? Math.max(0, Math.min(2, Integer.parseInt(values[3]))) : 1);
                    } catch (Exception ignored) { }
                }
            }
            persist();
            invalidate();
        }

        void undo() {
            if (undoHistory.isEmpty()) {
                Toast.makeText(BlockForgeActivity.this, "No hay cambios para deshacer", Toast.LENGTH_SHORT).show();
                return;
            }
            redoHistory.add(captureState());
            String previous = undoHistory.remove(undoHistory.size() - 1);
            restoreBlockState(previous);
        }

        void redo() {
            if (redoHistory.isEmpty()) {
                Toast.makeText(BlockForgeActivity.this, "No hay cambios para rehacer", Toast.LENGTH_SHORT).show();
                return;
            }
            undoHistory.add(captureState());
            String next = redoHistory.remove(redoHistory.size() - 1);
            restoreBlockState(next);
        }

        void duplicateSelectedSprite() {
            SceneSprite original = selectedSpriteObject();
            if (original == null) {
                Toast.makeText(BlockForgeActivity.this, "Selecciona un sprite primero", Toast.LENGTH_SHORT).show();
                return;
            }
            SceneSprite copy = new SceneSprite(original.fileName,
                    Math.min(mapColumns - original.width, original.x + 1f),
                    Math.min(mapRows - original.height, original.y + 1f), original.solid);
            copy.vx = original.vx;
            copy.vy = original.vy;
            copy.width = original.width;
            copy.height = original.height;
            copy.rotation = original.rotation;
            copy.animationFps = original.animationFps;
            copy.animationLoop = original.animationLoop;
            copy.animationPlaying = original.animationPlaying;
            for (String frameName : original.animationFrameNames) {
                java.io.File frameFile = new java.io.File(new java.io.File(getFilesDir(), "sprites"), frameName);
                Bitmap frame = BitmapFactory.decodeFile(frameFile.getAbsolutePath());
                if (frame != null) {
                    copy.animationFrames.add(frame);
                    copy.animationFrameNames.add(frameName);
                }
            }
            copy.currentFrameIndex = copy.animationFrames.isEmpty() ? 0 :
                    Math.min(original.currentFrameIndex, copy.animationFrames.size() - 1);
            sprites.add(copy);
            selectedSprite = sprites.size() - 1;
            persistSprites();
            invalidate();
            Toast.makeText(BlockForgeActivity.this, "Sprite duplicado", Toast.LENGTH_SHORT).show();
        }

        void applyRectangle(int x1,int y1,int x2,int y2){
            rememberState();
            int left=Math.max(0,Math.min(x1,x2)),right=Math.min(mapColumns-1,Math.max(x1,x2));
            int top=Math.max(0,Math.min(y1,y2)),bottom=Math.min(mapRows-1,Math.max(y1,y2));
            for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)applyCellDirect(x,y);
            persist();invalidate();
        }
        void applyCellDirect(int col,int row){
            int found=-1;
            for(int i=0;i<blocks.size();i++){Point p=blocks.get(i);int layer=i<blockLayers.size()?blockLayers.get(i):1;if(p.x==col&&p.y==row&&layer==sceneLayer){found=i;break;}}
            if(sceneEraseMode){if(found>=0){blocks.remove(found);if(found<blockColors.size())blockColors.remove(found);if(found<blockLayers.size())blockLayers.remove(found);}return;}
            if(found<0){blocks.add(new Point(col,row));blockColors.add(sceneBrushColor);blockLayers.add(sceneLayer);}
        }
        void floodFill(int startX,int startY){
            int targetColor=0;boolean targetExists=false;
            for(int i=0;i<blocks.size();i++){Point p=blocks.get(i);int layer=i<blockLayers.size()?blockLayers.get(i):1;if(p.x==startX&&p.y==startY&&layer==sceneLayer){targetExists=true;targetColor=i<blockColors.size()?blockColors.get(i):sceneBrushColor;break;}}
            if(sceneEraseMode&&!targetExists)return;
            if(!sceneEraseMode&&targetExists&&targetColor==sceneBrushColor)return;
            java.util.ArrayDeque<Point> queue=new java.util.ArrayDeque<>();
            java.util.HashSet<Integer> visited=new java.util.HashSet<>();
            java.util.ArrayList<Point> region=new java.util.ArrayList<>();
            queue.add(new Point(startX,startY));
            while(!queue.isEmpty()&&visited.size()<mapColumns*mapRows){
                Point p=queue.removeFirst();
                if(p.x<0||p.y<0||p.x>=mapColumns||p.y>=mapRows)continue;
                int key=p.y*mapColumns+p.x;if(!visited.add(key))continue;
                int found=-1;
                for(int i=0;i<blocks.size();i++){Point q=blocks.get(i);int layer=i<blockLayers.size()?blockLayers.get(i):1;if(q.x==p.x&&q.y==p.y&&layer==sceneLayer){found=i;break;}}
                boolean matches=targetExists?(found>=0&&(found>=blockColors.size()?sceneBrushColor:blockColors.get(found))==targetColor):(found<0);
                if(!matches)continue;
                region.add(p);
                queue.add(new Point(p.x+1,p.y));queue.add(new Point(p.x-1,p.y));
                queue.add(new Point(p.x,p.y+1));queue.add(new Point(p.x,p.y-1));
            }
            if(region.isEmpty())return;
            rememberState();
            for(Point p:region)applyCellDirect(p.x,p.y);
            persist();invalidate();
        }

        void paintCell(int col,int row){
            if(col==lastPaintedCol&&row==lastPaintedRow)return;
            lastPaintedCol=col;lastPaintedRow=row;
            if(sceneEraseMode){
                for(int i=blocks.size()-1;i>=0;i--){Point p=blocks.get(i);if(p.x==col&&p.y==row&&(i>=blockLayers.size()||blockLayers.get(i)==sceneLayer)){blocks.remove(i);if(i<blockColors.size())blockColors.remove(i);if(i<blockLayers.size())blockLayers.remove(i);break;}}
            }else{
                boolean exists=false;
                for(int i=0;i<blocks.size();i++){Point p=blocks.get(i);int layer=i<blockLayers.size()?blockLayers.get(i):1;if(p.x==col&&p.y==row&&layer==sceneLayer){exists=true;break;}}
                if(!exists){blocks.add(new Point(col,row));blockColors.add(sceneBrushColor);blockLayers.add(sceneLayer);}
            }
        }
        void addBlock() {
            rememberState();
            int index = blocks.size();
            blocks.add(new Point(index % mapColumns, (index / mapColumns) % mapRows));
            blockColors.add(sceneBrushColor);blockLayers.add(sceneLayer);
            persist();
            invalidate();
        }

        void clearBlocks() {
            rememberState();
            blocks.clear();
            blockColors.clear();blockLayers.clear();persist();invalidate();
        }

        void releaseHeldMobileButton(){
            if(activeMobileButtonPointer>=0&&activeMobileButtonPointer<mobileButtons.size()
                    &&selectedSprite>=0&&selectedSprite<sprites.size()){
                String action=mobileButtons.get(activeMobileButtonPointer).action;
                SceneSprite controlled=sprites.get(selectedSprite);
                if("left".equals(action)||"right".equals(action))controlled.vx=0f;
                if("up".equals(action)||"down".equals(action))controlled.vy=0f;
                persistSprites();
            }
            activeMobileButtonPointer=-1;
            activeMobileTouchId=-1;
            invalidate();
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event){
            scaleDetector.onTouchEvent(event);
            int action=event.getActionMasked();
            if(scenePlayMode){
                if(action==android.view.MotionEvent.ACTION_DOWN){
                    int hit=hitMobileButton(event.getX(0),event.getY(0));
                    if(hit>=0){
                        activeMobileButtonPointer=hit;
                        activeMobileTouchId=event.getPointerId(0);
                        runMobileButton(mobileButtons.get(hit));
                        return true;
                    }
                    float wx=(event.getX(0)-cameraOffsetX)/cameraZoom;
                    float wy=(event.getY(0)-cameraOffsetY)/cameraZoom;
                    int col=Math.max(0,Math.min(mapColumns-1,(int)(wx/Math.max(1f,getWidth()/(float)mapColumns))));
                    int row=Math.max(0,Math.min(mapRows-1,(int)(wy/Math.max(1f,getHeight()/(float)mapRows))));
                    for(int i=sprites.size()-1;i>=0;i--){
                        SceneSprite sp=sprites.get(i);
                        if(col>=sp.x-0.25f&&col<=sp.x+sp.width+0.25f&&row>=sp.y-0.25f&&row<=sp.y+sp.height+0.25f){selectedSprite=i;invalidate();break;}
                    }
                    return true;
                }
                if(action==android.view.MotionEvent.ACTION_POINTER_UP
                        &&event.getPointerId(event.getActionIndex())==activeMobileTouchId){
                    releaseHeldMobileButton();
                    return true;
                }
                if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL){
                    releaseHeldMobileButton();
                    multiTouchGesture=false;
                    return true;
                }
                if(action==android.view.MotionEvent.ACTION_MOVE && activeMobileTouchId>=0
                        && event.findPointerIndex(activeMobileTouchId)<0){
                    releaseHeldMobileButton();
                    return true;
                }
                if(event.getPointerCount()>1||scaleDetector.isInProgress()){
                    multiTouchGesture=true;
                    return true;
                }
                return true;
            }
            if(event.getPointerCount()>1 || scaleDetector.isInProgress()){multiTouchGesture=true;return true;}
            if(action==android.view.MotionEvent.ACTION_UP && multiTouchGesture){multiTouchGesture=false;return true;}
            if(cameraPanMode){if(event.getAction()==0){lastPanX=event.getX();lastPanY=event.getY();draggingSprite=-2;return true;}if(event.getAction()==2&&draggingSprite==-2){cameraOffsetX+=event.getX()-lastPanX;cameraOffsetY+=event.getY()-lastPanY;lastPanX=event.getX();lastPanY=event.getY();saveCamera();invalidate();return true;}if(event.getAction()==1){draggingSprite=-1;return true;}}
            float wx=(event.getX()-cameraOffsetX)/cameraZoom,wy=(event.getY()-cameraOffsetY)/cameraZoom;
            float cellW=Math.max(1f,getWidth()/(float)mapColumns),cellH=Math.max(1f,getHeight()/(float)mapRows);
            int col=Math.max(0,Math.min(mapColumns-1,(int)(wx/cellW))),row=Math.max(0,Math.min(mapRows-1,(int)(wy/cellH)));
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if(sceneToolMode==2){floodFill(col,row);return true;}
                if(sceneToolMode==1){gestureStartCol=col;gestureStartRow=row;paintingBlocks=false;draggingSprite=-1;return true;}
                draggingSprite = -1;
                for (int i = sprites.size() - 1; i >= 0; i--) {
                    SceneSprite sprite = sprites.get(i);
                    if (col >= sprite.x - 0.25f && col <= sprite.x + sprite.width + 0.25f &&
                            row >= sprite.y - 0.25f && row <= sprite.y + sprite.height + 0.25f) {
                        if (sceneEraseMode) {
                            if (sprite.bitmap != null) sprite.bitmap.recycle();
                            for (Bitmap frame : sprite.animationFrames) if (frame != null) frame.recycle();
                            sprites.remove(i);
                            selectedSprite = -1;
                            persistSprites();
                        } else {
                            draggingSprite = i;
                            selectedSprite = i;
                            spriteDragOffsetX = col - sprite.x;
                            spriteDragOffsetY = row - sprite.y;
                        }
                        break;
                    }
                }
                if(draggingSprite<0){paintingBlocks=true;lastPaintedCol=-1;lastPaintedRow=-1;rememberState();paintCell(col,row);invalidate();}
                return true;
            }
            if(sceneToolMode==1 && event.getAction()==android.view.MotionEvent.ACTION_UP && gestureStartCol>=0){
                applyRectangle(gestureStartCol,gestureStartRow,col,row);gestureStartCol=-1;gestureStartRow=-1;performClick();return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && paintingBlocks) {
                paintCell(col,row);invalidate();return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && draggingSprite >= 0) {
                SceneSprite sprite = sprites.get(draggingSprite);
                sprite.x = Math.max(0f, Math.min(mapColumns - sprite.width, col - spriteDragOffsetX));
                sprite.y = Math.max(0f, Math.min(mapRows - sprite.height, row - spriteDragOffsetY));
                invalidate();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if(paintingBlocks){paintingBlocks=false;lastPaintedCol=-1;lastPaintedRow=-1;persist();invalidate();performClick();return true;}
                if (draggingSprite >= 0) {
                    SceneSprite sprite = sprites.get(draggingSprite);
                    sprite.x = Math.max(0f, Math.min(mapColumns - sprite.width, col - spriteDragOffsetX));
                    sprite.y = Math.max(0f, Math.min(mapRows - sprite.height, row - spriteDragOffsetY));
                    draggingSprite = -1;
                    persistSprites();
                } else {
                    rememberState();
                    if (sceneEraseMode) {
                        for (int i = blocks.size() - 1; i >= 0; i--) {
                            Point block = blocks.get(i);
                            if (block.x == col && block.y == row) {
                                blocks.remove(i);
                                if(i<blockColors.size())blockColors.remove(i);if(i<blockLayers.size())blockLayers.remove(i);
                                break;
                            }
                        }
                    } else {
                        blocks.add(new Point(col, row));
                        blockColors.add(sceneBrushColor);blockLayers.add(sceneLayer);
                    }
                    persist();
                }
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

        void drawBlockLayer(Canvas canvas,int layer,float cellW,float cellH){
            for(int i=0;i<blocks.size();i++){
                if((i<blockLayers.size()?blockLayers.get(i):1)!=layer)continue;
                Point p=blocks.get(i);float left=p.x*cellW+3,top=p.y*cellH+3,right=left+cellW-6,bottom=top+cellH-6;
                paint.setColor(i<blockColors.size()?blockColors.get(i):sceneBrushColor);canvas.drawRect(left,top,right,bottom,paint);
                paint.setColor(Color.rgb(175,220,255));canvas.drawRect(left,top,right,top+5,paint);
                paint.setColor(Color.rgb(20,35,55));canvas.drawRect(left,bottom-5,right,bottom,paint);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();canvas.translate(cameraOffsetX,cameraOffsetY);canvas.scale(cameraZoom,cameraZoom);
            float cellW=getWidth()/(float)mapColumns,cellH=getHeight()/(float)mapRows;
            if (sceneGridVisible) {
                paint.setColor(Color.rgb(32,45,65));
                paint.setStrokeWidth(1f);
                for (int x = 0; x <= mapColumns; x++)
                    canvas.drawLine(x * cellW, 0, x * cellW, mapRows * cellH, paint);
                for (int y = 0; y <= mapRows; y++)
                    canvas.drawLine(0, y * cellH, mapColumns * cellW, y * cellH, paint);
            }

            drawBlockLayer(canvas,0,cellW,cellH);
            drawBlockLayer(canvas,1,cellW,cellH);

            for (SceneSprite sprite : sprites) {
                float left = sprite.x * cellW + 2;
                float top = sprite.y * cellH + 2;
                float right = left + cellW * sprite.width - 4;
                float bottom = top + cellH * sprite.height - 4;
                float centerX = (left + right) * 0.5f, centerY = (top + bottom) * 0.5f;
                canvas.save();
                canvas.rotate(sprite.rotation, centerX, centerY);
                Bitmap shown=sprite.bitmap;if(!sprite.animationFrames.isEmpty()){int frame=Math.max(0,Math.min(sprite.currentFrameIndex,sprite.animationFrames.size()-1));shown=sprite.animationFrames.get(frame);}
                if(shown!=null){paint.setFilterBitmap(false);canvas.drawBitmap(shown,null,new RectF(left,top,right,bottom),paint);
                } else {
                    paint.setColor(Color.MAGENTA);
                    canvas.drawRect(left, top, right, bottom, paint);
                }
                if (sprites.indexOf(sprite)==selectedSprite) {paint.setColor(Color.YELLOW);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3f);canvas.drawRect(left-2,top-2,right+2,bottom+2,paint);paint.setStyle(Paint.Style.FILL);}
                if (sprite.solid) {
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(left, top, right, bottom, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                canvas.restore();
            }

            drawBlockLayer(canvas,2,cellW,cellH);
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

            canvas.drawText("ESCENA · "+blocks.size()+" BLOQUES · CAPA "+(sceneLayer+1)+" · "+Math.round(cameraZoom*100)+"%",12,20,paint);
            if(selectedSprite>=0&&selectedSprite<sprites.size())canvas.drawText("SELECCIONADO · "+sprites.get(selectedSprite).fileName,12,38,paint);
            canvas.restore();
            drawMobileButtons(canvas);
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
    static final int PICK_SPRITE_REQUEST = 7422;
    static final int PICK_ANIMATION_FRAME_REQUEST = 7423;
    static final int EXPORT_2D_SCENE_REQUEST = 7424;
    static final int IMPORT_2D_SCENE_REQUEST = 7425;

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
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode == EXPORT_2D_SCENE_REQUEST) {
            try { writeSceneJson(data.getData()); Toast.makeText(this,"Nivel 2D exportado",Toast.LENGTH_LONG).show(); }
            catch(Exception ex){Toast.makeText(this,"Error al exportar: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
            return;
        }
        if (requestCode == IMPORT_2D_SCENE_REQUEST) {
            try { readSceneJson(data.getData()); Toast.makeText(this,"Nivel 2D importado",Toast.LENGTH_LONG).show(); openPage(2); }
            catch(Exception ex){Toast.makeText(this,"Archivo de nivel inválido: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
            return;
        }
        if (requestCode == PICK_ANIMATION_FRAME_REQUEST) {
            try { String name=copyUriToFolder(data.getData(),"sprites","frame"); if(scenePreview!=null)scenePreview.addAnimationFrame(name); Toast.makeText(this,"Fotograma añadido",Toast.LENGTH_LONG).show(); openPage(2); }
            catch(Exception ex){Toast.makeText(this,"No se pudo añadir: "+ex.getMessage(),Toast.LENGTH_LONG).show();} return;
        }
        if (requestCode == PICK_SPRITE_REQUEST) {
            try {
                String name = copyUriToFolder(data.getData(), "sprites", "sprite");
                if (scenePreview != null) scenePreview.addSprite(name);
                Toast.makeText(this, "Sprite importado y añadido a la escena", Toast.LENGTH_LONG).show();
                openPage(2);
            } catch (Exception ex) {
                Toast.makeText(this, "No se pudo importar la imagen: " + ex.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode != PICK_RESOURCE_REQUEST) return;
        try {
            String name = copyUriToFolder(data.getData(), "resources", "recurso");
            Toast.makeText(this, "Recurso importado: " + name, Toast.LENGTH_LONG).show();
            openPage(4);
        } catch (Exception ex) {
            Toast.makeText(this, "No se pudo importar: " + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    String copyUriToFolder(android.net.Uri uri, String folderName, String prefix)
            throws java.io.IOException {
        String name = uri.getLastPathSegment();
        if (name == null || name.trim().isEmpty()) name = prefix + "_" + System.currentTimeMillis();
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!name.contains(".")) name += ".png";
        java.io.File folder = new java.io.File(getFilesDir(), folderName);
        if (!folder.exists() && !folder.mkdirs())
            throw new java.io.IOException("No se pudo crear la carpeta");
        java.io.File destination = new java.io.File(folder, System.currentTimeMillis() + "_" + name);
        try (java.io.InputStream in = getContentResolver().openInputStream(uri);
             java.io.FileOutputStream out = new java.io.FileOutputStream(destination)) {
            if (in == null) throw new java.io.IOException("No se pudo abrir el archivo");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }
        return destination.getName();
    }

    void exportScenePicker() {
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("application/json");
        intent.putExtra(android.content.Intent.EXTRA_TITLE,"BlockForge-Scene.json");
        startActivityForResult(intent,EXPORT_2D_SCENE_REQUEST);
    }

    void importScenePicker() {
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("application/json");
        startActivityForResult(intent,IMPORT_2D_SCENE_REQUEST);
    }

    String encodeSceneSpriteFile(String name) throws Exception {
        if(name==null||name.isEmpty()||name.contains("/")||name.contains("\\"))throw new java.io.IOException("Nombre de sprite no válido");
        java.io.File file=new java.io.File(new java.io.File(getFilesDir(),"sprites"),name);
        if(!file.isFile()||file.length()<=0||file.length()>2*1024*1024)throw new java.io.IOException("Sprite ausente o demasiado grande: "+name);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        try(java.io.InputStream in=new java.io.FileInputStream(file)){
            byte[] buffer=new byte[8192];int count,total=0;
            while((count=in.read(buffer))!=-1){total+=count;if(total>2*1024*1024)throw new java.io.IOException("Sprite supera 2 MB: "+name);bytes.write(buffer,0,count);}
        }
        return android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP);
    }

    String importSceneSpriteFile(String encoded,String fallbackName,int index) throws Exception {
        if(encoded==null||encoded.isEmpty())return fallbackName;
        byte[] data;
        try{data=android.util.Base64.decode(encoded,android.util.Base64.NO_WRAP);}
        catch(IllegalArgumentException ex){throw new java.io.IOException("Imagen de sprite dañada en el archivo");}
        if(data.length==0||data.length>2*1024*1024)throw new java.io.IOException("Imagen importada vacía o demasiado grande");
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(data,0,data.length,bounds);
        if(bounds.outWidth<=0||bounds.outHeight<=0||(long)bounds.outWidth*bounds.outHeight>4_194_304L)
            throw new java.io.IOException("Dimensiones de sprite no válidas o demasiado grandes");
        String extension=".png";
        if(fallbackName!=null){int dot=fallbackName.lastIndexOf('.');if(dot>=0&&dot<fallbackName.length()-1){String ext=fallbackName.substring(dot).toLowerCase(java.util.Locale.ROOT);if(".png".equals(ext)||".jpg".equals(ext)||".jpeg".equals(ext)||".webp".equals(ext))extension=ext;}}
        String name="scene_import_"+System.currentTimeMillis()+"_"+index+extension;
        java.io.File folder=new java.io.File(getFilesDir(),"sprites");if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException("No se pudo crear la carpeta de sprites");
        java.io.File destination=new java.io.File(folder,name);
        try(java.io.OutputStream out=new java.io.FileOutputStream(destination)){out.write(data);}
        return name;
    }

    org.json.JSONObject buildSceneJson() throws Exception {
        ScenePreview target=scenePreview!=null?scenePreview:new ScenePreview();
        org.json.JSONObject rootJson=new org.json.JSONObject();
        rootJson.put("format","BlockForge2D");rootJson.put("version",5);rootJson.put("mapWidth",target.mapColumns);rootJson.put("mapHeight",target.mapRows);
        org.json.JSONArray blockArray=new org.json.JSONArray();
        for(int i=0;i<target.blocks.size();i++){
            Point p=target.blocks.get(i);org.json.JSONObject item=new org.json.JSONObject();
            item.put("x",p.x);item.put("y",p.y);
            item.put("color",i<target.blockColors.size()?target.blockColors.get(i):sceneBrushColor);
            item.put("layer",i<target.blockLayers.size()?target.blockLayers.get(i):1);
            blockArray.put(item);
        }
        rootJson.put("blocks",blockArray);
        org.json.JSONArray spriteArray=new org.json.JSONArray();
        for(SceneSprite sp:target.sprites){
            org.json.JSONObject item=new org.json.JSONObject();
            item.put("file",sp.fileName);item.put("x",sp.x);item.put("y",sp.y);
            item.put("width",sp.width);item.put("height",sp.height);item.put("rotation",sp.rotation);
            item.put("solid",sp.solid);item.put("vx",sp.vx);item.put("vy",sp.vy);
            item.put("animationFps",sp.animationFps);item.put("animationLoop",sp.animationLoop);
            item.put("animationPlaying",sp.animationPlaying);item.put("currentFrameIndex",sp.currentFrameIndex);
            item.put("imageBase64",encodeSceneSpriteFile(sp.fileName));
            org.json.JSONArray frames=new org.json.JSONArray(),frameImages=new org.json.JSONArray();
            for(String frame:sp.animationFrameNames){frames.put(frame);frameImages.put(encodeSceneSpriteFile(frame));}
            item.put("frames",frames);item.put("frameImages",frameImages);spriteArray.put(item);
        }
        rootJson.put("sprites",spriteArray);
        org.json.JSONArray buttons=new org.json.JSONArray();for(MobileButton b:target.mobileButtons){org.json.JSONObject item=new org.json.JSONObject();item.put("id",b.id);item.put("title",b.title);item.put("icon",b.icon);item.put("action",b.action);item.put("iconFile",b.iconFile);item.put("iconBase64",b.iconFile==null||b.iconFile.isEmpty()?"":encodeSceneSpriteFile(b.iconFile));item.put("script",b.script);buttons.put(item);}rootJson.put("mobileButtons",buttons);
        org.json.JSONObject camera=new org.json.JSONObject();
        camera.put("zoom",target.cameraZoom);camera.put("x",target.cameraOffsetX);camera.put("y",target.cameraOffsetY);
        rootJson.put("camera",camera);
        rootJson.put("audio",getPreferences(MODE_PRIVATE).getString("scene_audio_"+activeProjectName(),""));
        return rootJson;
    }

    void writeSceneJson(android.net.Uri uri) throws Exception {
        org.json.JSONObject json=buildSceneJson();
        byte[] payload=json.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(payload.length>8*1024*1024)throw new java.io.IOException("El nivel exportado supera 8 MB; reduce el tamaño o cantidad de imágenes.");
        try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){
            if(out==null)throw new java.io.IOException("No se pudo crear el archivo");
            out.write(payload);
        }
    }

    void readSceneJson(android.net.Uri uri) throws Exception {
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
            if(in==null)throw new java.io.IOException("No se pudo abrir el archivo");
            byte[] buffer=new byte[8192];int n,total=0;
            while((n=in.read(buffer))!=-1){total+=n;if(total>8*1024*1024)throw new java.io.IOException("El nivel supera 8 MB");bytes.write(buffer,0,n);}
        }
        org.json.JSONObject json=new org.json.JSONObject(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
        int formatVersion=json.optInt("version",1);
        if(!"BlockForge2D".equals(json.optString("format"))||(formatVersion<1||formatVersion>5))
            throw new java.io.IOException("Formato o versión no compatible");
        ScenePreview target=scenePreview!=null?scenePreview:new ScenePreview();
        target.setMapSize(Math.max(5,Math.min(64,json.optInt("mapWidth",10))),Math.max(5,Math.min(64,json.optInt("mapHeight",10))));
        target.blocks.clear();target.blockColors.clear();target.blockLayers.clear();
        org.json.JSONArray blockArray=json.optJSONArray("blocks");
        if(blockArray!=null)for(int i=0;i<blockArray.length();i++){
            org.json.JSONObject item=blockArray.getJSONObject(i);
            int x=item.getInt("x"),y=item.getInt("y"),layer=item.optInt("layer",1);
            if(x<0||x>=target.mapColumns||y<0||y>=target.mapRows||layer<0||layer>2)continue;
            target.blocks.add(new Point(x,y));target.blockColors.add(item.optInt("color",sceneBrushColor));target.blockLayers.add(layer);
        }
        for(SceneSprite old:target.sprites){if(old.bitmap!=null)old.bitmap.recycle();for(Bitmap frame:old.animationFrames)if(frame!=null)frame.recycle();}
        target.sprites.clear();target.selectedSprite=-1;
        target.mobileButtons.clear();org.json.JSONArray savedButtons=json.optJSONArray("mobileButtons");if(savedButtons!=null)for(int i=0;i<savedButtons.length();i++){org.json.JSONObject b=savedButtons.getJSONObject(i);String iconName=b.optString("iconFile","");String iconData=b.optString("iconBase64","");if(!iconData.isEmpty())iconName=importSceneSpriteFile(iconData,iconName.isEmpty()?"mobile_icon.png":iconName,50000+i);target.mobileButtons.add(new MobileButton(b.optString("id"),b.optString("title"),b.optString("icon"),b.optString("action","left"),iconName,b.optString("script","")));}target.persistMobileButtons();
        org.json.JSONArray spriteArray=json.optJSONArray("sprites");
        if(spriteArray!=null)for(int i=0;i<spriteArray.length();i++){
            org.json.JSONObject item=spriteArray.getJSONObject(i);String name=item.optString("file","");
            if(name.isEmpty()||name.contains("/")||name.contains("\\"))continue;
            String importedMain=importSceneSpriteFile(item.optString("imageBase64",""),name,i);
            SceneSprite sp=new SceneSprite(importedMain,(float)item.optDouble("x",0),(float)item.optDouble("y",0),item.optBoolean("solid",true));
            if(sp.bitmap==null)continue;
            sp.vx=(float)item.optDouble("vx",0);sp.vy=(float)item.optDouble("vy",0);
            sp.width=(float)Math.max(0.25,Math.min(Math.min(8,target.mapColumns),item.optDouble("width",1)));
            sp.height=(float)Math.max(0.25,Math.min(Math.min(8,target.mapRows),item.optDouble("height",1)));
            sp.x=Math.max(0f,Math.min(target.mapColumns-sp.width,sp.x));
            sp.y=Math.max(0f,Math.min(target.mapRows-sp.height,sp.y));
            sp.rotation=(float)Math.max(-360,Math.min(360,item.optDouble("rotation",0)));
            org.json.JSONArray frames=item.optJSONArray("frames"),frameImages=item.optJSONArray("frameImages");
            if(frames!=null)for(int j=0;j<frames.length();j++){
                String fn=frames.optString(j,"");if(fn.isEmpty()||fn.contains("/")||fn.contains("\\"))continue;
                String encoded=frameImages!=null&&j<frameImages.length()?frameImages.optString(j,""):"";
                String importedFrame=importSceneSpriteFile(encoded,fn,i*1000+j);
                java.io.File file=new java.io.File(new java.io.File(getFilesDir(),"sprites"),importedFrame);
                Bitmap frame=BitmapFactory.decodeFile(file.getAbsolutePath());
                if(frame!=null){sp.animationFrames.add(frame);sp.animationFrameNames.add(importedFrame);}
            }
            sp.animationFps=Math.max(1,Math.min(60,item.optInt("animationFps",targetAnimationFps)));
            sp.animationLoop=item.optBoolean("animationLoop",true);
            sp.currentFrameIndex=sp.animationFrames.isEmpty()?0:Math.max(0,Math.min(sp.animationFrames.size()-1,item.optInt("currentFrameIndex",0)));
            sp.animationPlaying=item.optBoolean("animationPlaying",false)&&sp.animationFrames.size()>1;
            target.sprites.add(sp);
        }
        org.json.JSONObject camera=json.optJSONObject("camera");
        if(camera!=null){target.cameraZoom=(float)Math.max(.65,Math.min(2.5,camera.optDouble("zoom",1)));target.cameraOffsetX=(float)camera.optDouble("x",0);target.cameraOffsetY=(float)camera.optDouble("y",0);}
        String audio=json.optString("audio","");
        if(audio.isEmpty())getPreferences(MODE_PRIVATE).edit().remove("scene_audio_"+activeProjectName()).apply();
        else getPreferences(MODE_PRIVATE).edit().putString("scene_audio_"+activeProjectName(),audio).apply();
        target.persist();target.persistSprites();target.saveCamera();target.invalidate();target.startAnimationTicker();
    }

    void showAnimationFpsDialog(){
        if(scenePreview==null||scenePreview.selectedSpriteObject()==null){Toast.makeText(this,"Selecciona un sprite primero",Toast.LENGTH_SHORT).show();return;}
        SceneSprite sprite=scenePreview.selectedSpriteObject();
        EditText fps=new EditText(this);fps.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);fps.setText(String.valueOf(sprite.animationFps));fps.setHint("FPS entre 1 y 60");
        new android.app.AlertDialog.Builder(this).setTitle("Velocidad de animación")
            .setMessage("Elige cuántos fotogramas se muestran por segundo.")
            .setView(fps).setNegativeButton("Cancelar",null)
            .setPositiveButton("APLICAR",(d,w)->{
                try{int value=Integer.parseInt(fps.getText().toString().trim());if(value<1||value>60)throw new NumberFormatException();
                    sprite.animationFps=value;targetAnimationFps=value;
                    getPreferences(MODE_PRIVATE).edit().putInt("sprite_fps",value).apply();
                    scenePreview.persistSprites();scenePreview.startAnimationTicker();openPage(2);
                }catch(Exception ex){Toast.makeText(this,"Usa un valor entre 1 y 60 FPS",Toast.LENGTH_LONG).show();}
            }).show();
    }

    void pickAnimationFrame() {
        if(scenePreview==null||scenePreview.selectedSprite<0){Toast.makeText(this,"Selecciona primero un sprite",Toast.LENGTH_SHORT).show();return;}
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("image/*");startActivityForResult(intent,PICK_ANIMATION_FRAME_REQUEST);
    }
    void pickSpriteImage() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_SPRITE_REQUEST);
    }


    void openSpriteDrawer() {
        LinearLayout wrap = vertical();
        wrap.setPadding(14, 12, 14, 12);
        wrap.setBackgroundColor(Color.rgb(20, 24, 31));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView sizeLabel = uiLabel("TAMAÑO DEL LIENZO", 10, uiMuted, true);
        top.addView(sizeLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Spinner sizePicker = new Spinner(this);
        String[] sizes = {"16 × 16", "32 × 32", "64 × 64"};
        sizePicker.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, sizes));
        top.addView(sizePicker, new LinearLayout.LayoutParams(130, 44));
        wrap.addView(top);

        SpriteDrawView drawing = new SpriteDrawView();
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(-1, 0, 1);
        canvasParams.topMargin = 10;
        canvasParams.bottomMargin = 10;
        wrap.addView(drawing, new LinearLayout.LayoutParams(-1, (int)(getResources().getDisplayMetrics().density * 340)));
        sizePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                drawing.setSize(position == 0 ? 16 : position == 1 ? 32 : 64);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER);
        tools.addView(editorButton("LÁPIZ", () -> drawing.setTool(0)), new LinearLayout.LayoutParams(0, 44, 1));
        tools.addView(editorButton("BORRADOR", () -> drawing.setTool(1)), new LinearLayout.LayoutParams(0, 44, 1));
        tools.addView(editorButton("RELLENO", () -> drawing.setTool(2)), new LinearLayout.LayoutParams(0, 44, 1));
        tools.addView(editorButton("DESHACER", () -> drawing.undo()), new LinearLayout.LayoutParams(0, 44, 1));
        tools.addView(editorButton("REHACER", () -> drawing.redo()), new LinearLayout.LayoutParams(0, 44, 1));
        wrap.addView(tools);

        HorizontalScrollView paletteScroll = new HorizontalScrollView(this);
        paletteScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colors = new LinearLayout(this);
        colors.setPadding(0, 9, 0, 9);
        int[] palette = {Color.BLACK, Color.WHITE, Color.rgb(230,75,75), Color.rgb(70,180,110),
                Color.rgb(70,130,230), Color.YELLOW, Color.MAGENTA, Color.CYAN,
                Color.rgb(150,95,55), Color.rgb(120,120,130), Color.rgb(255,150,65)};
        String[] labels = {"NEGRO","BLANCO","ROJO","VERDE","AZUL","AMARILLO","ROSA","CIAN","TIERRA","GRIS","NARANJA"};
        for (int i = 0; i < palette.length; i++) {
            final int color = palette[i];
            TextView swatch = editorButton("●", () -> drawing.setColor(color));
            swatch.setTextColor(color);
            swatch.setContentDescription(labels[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(48, 42);
            lp.rightMargin = 5;
            colors.addView(swatch, lp);
        }
        paletteScroll.addView(colors);
        wrap.addView(paletteScroll);
        LinearLayout lower = new LinearLayout(this);
        lower.addView(editorButton("COLOR HEX", () -> {
            EditText hex = new EditText(this);
            hex.setSingleLine(true);
            hex.setHint("#RRGGBB");
            hex.setText("#FFFFFF");
            new android.app.AlertDialog.Builder(this).setTitle("Color personalizado")
                .setView(hex).setPositiveButton("APLICAR", (d,w) -> {
                    try { drawing.setColor(Color.parseColor(hex.getText().toString().trim())); }
                    catch (Exception e) { Toast.makeText(this, "Color HEX no válido", Toast.LENGTH_SHORT).show(); }
                }).setNegativeButton("CANCELAR", null).show();
        }), new LinearLayout.LayoutParams(0, 44, 1));
        lower.addView(editorButton("LIMPIAR", () -> new android.app.AlertDialog.Builder(this)
                .setTitle("Limpiar lienzo").setMessage("¿Borrar todos los píxeles?")
                .setPositiveButton("BORRAR", (d,w) -> drawing.clear()).setNegativeButton("CANCELAR", null).show()),
                new LinearLayout.LayoutParams(0, 44, 1));
        wrap.addView(lower);

        android.widget.CheckBox asMobileButton = new android.widget.CheckBox(this);
        asMobileButton.setText("Usar también como botón móvil");
        asMobileButton.setTextColor(uiMuted);
        wrap.addView(asMobileButton);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("PIXEL STUDIO  /  EDITOR DE SPRITES")
                .setView(wrap)
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("GUARDAR PNG", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getWindow().setLayout(-1, (int)(getResources().getDisplayMetrics().heightPixels * 0.88f));
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    String name = saveDrawnSprite(drawing.exportBitmap());
                    if (scenePreview != null) scenePreview.addSprite(name);
                    if (asMobileButton.isChecked()) createMobileButtonDialog(name);
                    Toast.makeText(this, "Sprite PNG guardado: " + name, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (!asMobileButton.isChecked()) openPage(2);
                } catch (Exception ex) {
                    Toast.makeText(this, "No se pudo guardar el sprite: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        dialog.show();
    }

    String saveDrawnSprite(Bitmap bitmap) throws java.io.IOException {
        java.io.File folder = new java.io.File(getFilesDir(), "sprites");
        if (!folder.exists()) folder.mkdirs();
        String name = "dibujado_" + System.currentTimeMillis() + ".png";
        java.io.File file = new java.io.File(folder, name);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
        return name;
    }


    class SpriteDrawView extends View {
        int size = 16;
        int[] pixels = new int[size * size];
        final Paint brush = new Paint(Paint.ANTI_ALIAS_FLAG);
        final java.util.ArrayList<int[]> undo = new java.util.ArrayList<>();
        final java.util.ArrayList<int[]> redo = new java.util.ArrayList<>();
        int currentColor = Color.WHITE;
        int tool = 0;
        boolean showGrid = true;
        float cellSize, gridLeft, gridTop;

        SpriteDrawView() {
            super(BlockForgeActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setBackgroundColor(Color.rgb(15, 19, 25));
        }

        void setSize(int value) {
            if (value == size) return;
            size = value;
            pixels = new int[size * size];
            undo.clear();
            redo.clear();
            invalidate();
        }
        void setColor(int color) { currentColor = color; tool = 0; invalidate(); }
        void setTool(int value) { tool = value; invalidate(); }
        void snapshot() {
            undo.add(pixels.clone());
            if (undo.size() > 40) undo.remove(0);
            redo.clear();
        }
        void undo() {
            if (undo.isEmpty()) return;
            redo.add(pixels.clone());
            pixels = undo.remove(undo.size() - 1);
            invalidate();
        }
        void redo() {
            if (redo.isEmpty()) return;
            undo.add(pixels.clone());
            pixels = redo.remove(redo.size() - 1);
            invalidate();
        }
        void clear() { snapshot(); java.util.Arrays.fill(pixels, Color.TRANSPARENT); invalidate(); }
        Bitmap exportBitmap() {
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        }
        int pixelAt(float x, float y) {
            if (cellSize <= 0 || x < gridLeft || y < gridTop ||
                    x >= gridLeft + cellSize * size || y >= gridTop + cellSize * size) return -1;
            int px = (int)((x - gridLeft) / cellSize);
            int py = (int)((y - gridTop) / cellSize);
            return py * size + px;
        }
        void paintAt(int index) {
            if (index < 0 || index >= pixels.length) return;
            if (tool == 2) { floodFill(index, pixels[index], currentColor); return; }
            pixels[index] = tool == 1 ? Color.TRANSPARENT : currentColor;
        }
        void floodFill(int start, int target, int replacement) {
            if (target == replacement) return;
            java.util.ArrayList<Integer> queue = new java.util.ArrayList<>();
            boolean[] seen = new boolean[pixels.length];
            queue.add(start);
            seen[start] = true;
            for (int q = 0; q < queue.size(); q++) {
                int at = queue.get(q);
                if (pixels[at] != target) continue;
                pixels[at] = replacement;
                int x = at % size, y = at / size;
                int[] next = {x > 0 ? at - 1 : -1, x < size - 1 ? at + 1 : -1,
                        y > 0 ? at - size : -1, y < size - 1 ? at + size : -1};
                for (int n : next) if (n >= 0 && !seen[n] && pixels[n] == target) {
                    seen[n] = true; queue.add(n);
                }
            }
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent event) {
            int index = pixelAt(event.getX(), event.getY());
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if (index < 0) return false;
                snapshot();
                paintAt(index);
                invalidate();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                if (index >= 0 && tool != 2) { paintAt(index); invalidate(); }
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_UP;
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth() - 24f, getHeight() - 24f);
            cellSize = Math.max(1f, side / size);
            float gridSide = cellSize * size;
            gridLeft = (getWidth() - gridSide) / 2f;
            gridTop = (getHeight() - gridSide) / 2f;
            brush.setStyle(Paint.Style.FILL);
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                brush.setColor(((x + y) & 1) == 0 ? Color.rgb(53, 60, 70) : Color.rgb(39, 46, 56));
                float l = gridLeft + x * cellSize, t = gridTop + y * cellSize;
                canvas.drawRect(l, t, l + cellSize, t + cellSize, brush);
                int color = pixels[y * size + x];
                if (color != Color.TRANSPARENT) {
                    brush.setColor(color);
                    canvas.drawRect(l, t, l + cellSize, t + cellSize, brush);
                }
            }
            if (showGrid && cellSize >= 5f) {
                brush.setColor(Color.argb(65, 5, 9, 14));
                brush.setStrokeWidth(1f);
                for (int i = 0; i <= size; i++) {
                    float p = gridLeft + i * cellSize;
                    canvas.drawLine(p, gridTop, p, gridTop + gridSide, brush);
                    p = gridTop + i * cellSize;
                    canvas.drawLine(gridLeft, p, gridLeft + gridSide, p, brush);
                }
            }
            brush.setStyle(Paint.Style.STROKE);
            brush.setStrokeWidth(2f);
            brush.setColor(Color.rgb(93, 151, 213));
            canvas.drawRect(gridLeft, gridTop, gridLeft + gridSide, gridTop + gridSide, brush);
            brush.setStyle(Paint.Style.FILL);
        }
    }

    void savePackPixelSprite(java.io.File folder, String name, int color, int type)
            throws java.io.IOException {
        Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setColor(color);
        if (type == 0) {
            canvas.drawRect(5,2,11,5,p); canvas.drawRect(3,5,13,11,p);
            canvas.drawRect(4,11,7,15,p); canvas.drawRect(9,11,12,15,p);
            p.setColor(Color.WHITE); canvas.drawPoint(6,7,p); canvas.drawPoint(10,7,p);
        } else if (type == 1) {
            canvas.drawCircle(8,8,6,p); p.setColor(Color.rgb(255,245,170));
            canvas.drawCircle(8,8,3,p);
        } else {
            canvas.drawRect(0,5,16,12,p);
            p.setColor(Color.rgb(35,80,55)); canvas.drawRect(0,12,16,15,p);
        }
        java.io.File file = new java.io.File(folder, name);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally { bitmap.recycle(); }
    }

    void installSpritePack(final int fps) {
        if(fps!=30 && fps!=60){Toast.makeText(this,"FPS no compatible",Toast.LENGTH_SHORT).show();return;}
        targetAnimationFps=fps;
        getPreferences(MODE_PRIVATE).edit().putInt("sprite_fps",fps).apply();
        try {
            java.io.File packFolder=new java.io.File(getFilesDir(),"sprite-packs");
            java.io.File spriteFolder=new java.io.File(getFilesDir(),"sprites");
            if(!packFolder.exists()&&!packFolder.mkdirs())throw new java.io.IOException("No se pudo crear la carpeta de paquetes");
            if(!spriteFolder.exists()&&!spriteFolder.mkdirs())throw new java.io.IOException("No se pudo crear la carpeta de sprites");
            org.json.JSONObject pack=new org.json.JSONObject();
            pack.put("name","BlockForge Pixel Pack "+fps);pack.put("version",1);pack.put("fps",fps);
            pack.put("description","Paquete pixel art integrado, instalable sin conexión.");
            org.json.JSONArray palette=new org.json.JSONArray();
            if(fps==30){palette.put("#4D9BFF");palette.put("#FFD34D");palette.put("#54C878");}
            else{palette.put("#9B7BFF");palette.put("#FFF078");palette.put("#49C9C0");}
            pack.put("palette",palette);
            java.io.File descriptor=new java.io.File(packFolder,"pack-"+fps+".json");
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(descriptor)){
                out.write(pack.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            savePackPixelSprite(spriteFolder,"pack_hero_"+fps+".png",Color.parseColor(palette.getString(0)),0);
            savePackPixelSprite(spriteFolder,"pack_coin_"+fps+".png",Color.parseColor(palette.getString(1)),1);
            savePackPixelSprite(spriteFolder,"pack_platform_"+fps+".png",Color.parseColor(palette.getString(2)),2);
            if(scenePreview!=null){
                scenePreview.addSprite("pack_hero_"+fps+".png");
                scenePreview.addSprite("pack_coin_"+fps+".png");
                scenePreview.addSprite("pack_platform_"+fps+".png");
            }
            Toast.makeText(this,"Pack "+fps+" FPS instalado sin conexión: 3 sprites pixel art.",Toast.LENGTH_LONG).show();
        } catch(Exception ex) {
            Toast.makeText(this,"No se pudo instalar el pack: "+ex.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    void settings() {
        heading(uiText("Configuración","Settings"), uiText("Preferencias de BlockForge Studio.","BlockForge Studio preferences."));
        content.addView(editorButton(uiText("🌐  CAMBIAR IDIOMA","🌐  CHANGE LANGUAGE"), () -> showLanguagePicker()));
        String lang=getPreferences(MODE_PRIVATE).getString("ui_language","es");
        card(uiText("Idioma activo","Active language"),"en".equals(lang)?"English":"Español");
        card(uiText("Apariencia","Appearance"),uiText("Tema oscuro de BlockForge.","BlockForge dark theme."));
        card(uiText("Lenguaje de programación","Programming language"),"El Grafisript");
        card(uiText("Motor gráfico","Graphics engine"),"C++ / SDL2 · OpenGL ES");
        card(uiText("Información","About"),"BlockForge Studio Mobile · Experimental");
    }

}