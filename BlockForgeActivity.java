
package com.blockforge.studio;

// Compilación: los métodos de interfaz deben permanecer en líneas Java reales, no como texto escapado.

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
    static final int PICK_BACKGROUND_REQUEST = 7123;
    int sceneBrushColor = Color.rgb(55, 135, 90);
    boolean sceneEraseMode = false;
    boolean sceneGridVisible = true;
    boolean sceneCollisionsEnabled = true;
    int sceneToolMode = 0; // 0 lápiz, 1 rectángulo, 2 relleno
    final java.util.ArrayList<GraphNode> grafisriptNodes = new java.util.ArrayList<>();
    int selectedGraphNode = -1;
    static final class GraphNode {
        String type;
        float value;
        GraphNode(String t, float v) { type=t; value=v; }
    }

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
    // Estado de la sintaxis declarativa sencilla de Grafisript.
    String grafisriptEasyContext = "";
    int grafisriptEnemyDamage = 20;
    String grafisriptEnemyName = "Enemigo";


    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        applyUiTheme();
        targetAnimationFps = getPreferences(MODE_PRIVATE).getInt("sprite_fps", 30);
        physicsGravity=getPreferences(MODE_PRIVATE).getFloat("physics_gravity",24f);
        physicsBounce=getPreferences(MODE_PRIVATE).getFloat("physics_bounce",0.15f);
        sceneCollisionsEnabled=getPreferences(MODE_PRIVATE).getBoolean("scene_collisions",true);
        buildInterface();
        home();
        ensureOriginalSoundPackInstalled();
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

    void applyUiTheme() {
        boolean light = getPreferences(MODE_PRIVATE).getBoolean("ui_light_theme", false);
        if (light) {
            background = Color.rgb(242, 244, 247); panel = Color.WHITE; white = Color.rgb(28, 32, 38); gray = Color.rgb(92, 101, 113);
            uiInk = Color.rgb(28, 32, 38); uiMuted = Color.rgb(94, 103, 115); uiSurface = Color.WHITE; uiSurfaceRaised = Color.rgb(235, 238, 242); uiLine = Color.rgb(211, 216, 223);
        } else {
            background = Color.rgb(12,16,26); panel = Color.rgb(24,32,47); white = Color.WHITE; gray = Color.rgb(160,175,195);
            uiInk = Color.rgb(232,236,242); uiMuted = Color.rgb(151,161,174); uiSurface = Color.rgb(29,34,42); uiSurfaceRaised = Color.rgb(36,42,51); uiLine = Color.rgb(53,61,72);
        }
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

    boolean isLikelyPlayerSprite(String name) {
        String n=name==null?"":name.toLowerCase(java.util.Locale.ROOT);
        return n.contains("hero")||n.contains("player")||n.contains("personaje")||n.contains("astronaut")||n.contains("paladin")||n.contains("knight")||n.contains("adventur")||n.contains("builtin_0_")||n.contains("builtin_1_")||n.contains("builtin_7_")||n.contains("builtin_8_")||n.contains("builtin_9_");
    }
    boolean isStaticSceneAsset(String name) {
        String n=name==null?"":name.toLowerCase(java.util.Locale.ROOT);
        return n.contains("coin")||n.contains("platform")||n.contains("plataforma")||n.contains("builtin_2_")||n.contains("builtin_3_")||n.contains("builtin_4_")||n.contains("builtin_5_")||n.contains("builtin_6_")||n.contains("builtin_10_")||n.contains("builtin_11_")||n.contains("builtin_12_")||n.contains("builtin_13_")||n.contains("builtin_14_")||n.contains("builtin_15_");
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
        panelView.setId(0x00bf0011);
        panelView.setPadding(16, 18, 16, 18);
        panelView.setBackground(shape(Color.rgb(17, 25, 39)));
        TextView heading = label(title, 18, white);
        heading.setPadding(0, 0, 0, 14);
        panelView.addView(heading);
        ScrollView panelScroll = new ScrollView(this);
        panelScroll.setFillViewport(true);
        panelScroll.setVerticalScrollBarEnabled(true);
        panelScroll.addView(panelView);
        dialog.setContentView(panelScroll);
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
        LinearLayout panelView = (LinearLayout)dialog.findViewById(0x00bf0011);
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
            panelView.addView(label("X: "+sprite.x+" · Y: "+sprite.y+"\nTamaño: "+sprite.width+" × "+sprite.height+" · Giro: "+sprite.rotation+"°\nSólido: "+(sprite.solid?"Sí":"No")+" · "+(sprite.isPlayer?"PERSONAJE":"OBJETO FIJO"),13,gray));
            panelView.addView(editorButton(sprite.isPlayer?"✓ PERSONAJE DEL JUEGO":"DESIGNAR COMO PERSONAJE",()->{scenePreview.designateSelectedAsPlayer();dialog.dismiss();}));
            panelView.addView(editorButton("TRANSFORMAR · POSICIÓN / TAMAÑO / GIRO", () -> {
                android.widget.EditText x=new android.widget.EditText(this); x.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); x.setHint("X"); x.setText(String.valueOf(sprite.x));
                android.widget.EditText y=new android.widget.EditText(this); y.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); y.setHint("Y"); y.setText(String.valueOf(sprite.y));
                int maxWidthSteps=Math.max(1,(int)Math.floor((Math.min(8f,scenePreview.mapColumns)-0.25f)*4f));
                int maxHeightSteps=Math.max(1,(int)Math.floor((Math.min(8f,scenePreview.mapRows)-0.25f)*4f));
                android.widget.SeekBar width=new android.widget.SeekBar(this);width.setMax(maxWidthSteps);width.setProgress(Math.max(0,Math.min(maxWidthSteps,Math.round((sprite.width-0.25f)*4f))));
                android.widget.SeekBar height=new android.widget.SeekBar(this);height.setMax(maxHeightSteps);height.setProgress(Math.max(0,Math.min(maxHeightSteps,Math.round((sprite.height-0.25f)*4f))));
                TextView widthValue=label("Ancho: "+sprite.width+" unidades",13,gray);
                TextView heightValue=label("Alto: "+sprite.height+" unidades",13,gray);
                width.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int p,boolean u){widthValue.setText("Ancho: "+(0.25f+p/4f)+" unidades");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
                height.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int p,boolean u){heightValue.setText("Alto: "+(0.25f+p/4f)+" unidades");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
                android.widget.EditText rotation=new android.widget.EditText(this); rotation.setInputType(8194 | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED); rotation.setHint("Rotación en grados"); rotation.setText(String.valueOf(sprite.rotation));
                LinearLayout form=vertical();form.addView(x);form.addView(y);form.addView(widthValue);form.addView(width);form.addView(heightValue);form.addView(height);form.addView(rotation);
                new android.app.AlertDialog.Builder(this).setTitle("Transformación 2D · Tamaño preciso").setView(form)
                    .setPositiveButton("Guardar",(d,w)->{try{
                        float nx=Float.parseFloat(x.getText().toString()),ny=Float.parseFloat(y.getText().toString());
                        float nw=0.25f+width.getProgress()/4f,nh=0.25f+height.getProgress()/4f;
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
            saveProjectFile("scripts/main.bf",editor.getText().toString());
            Toast.makeText(this,"Código guardado",Toast.LENGTH_SHORT).show();
        }));
        panelView.addView(editorButton("ABRIR EDITOR Y EJECUTAR",()->{
            getPreferences(MODE_PRIVATE).edit().putString("grafisript_"+project,editor.getText().toString()).apply();
            saveProjectFile("scripts/main.bf",editor.getText().toString());
            if (codeEditor != null) { codeEditorProjectName = project; codeEditor.setText(editor.getText().toString()); }
            dialog.dismiss();openPage(3);
        }));
        panelView.addView(editorButton("CERRAR",dialog::dismiss));
    }

    void showSceneLightingDialog() {
        boolean enabled=getPreferences(MODE_PRIVATE).getBoolean("scene_shadows_enabled_"+activeProjectName(),true);
        int opacity=getPreferences(MODE_PRIVATE).getInt("scene_shadow_opacity_"+activeProjectName(),90);
        LinearLayout form=vertical();
        TextView state=label("Sombras 2D: "+(enabled?"ACTIVADAS":"DESACTIVADAS"),14,white);
        form.addView(state);
        android.widget.SeekBar strength=new android.widget.SeekBar(this);
        strength.setMax(180); strength.setProgress(opacity);
        TextView strengthLabel=label("Opacidad: "+opacity+" / 180",13,gray);
        strength.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(android.widget.SeekBar b,int p,boolean u){strengthLabel.setText("Opacidad: "+p+" / 180");}
            public void onStartTrackingTouch(android.widget.SeekBar b){}
            public void onStopTrackingTouch(android.widget.SeekBar b){}
        });
        form.addView(strengthLabel);form.addView(strength);
        new android.app.AlertDialog.Builder(this).setTitle("ILUMINACIÓN Y SOMBRAS 2D")
            .setMessage("Sombras proyectadas sencillas debajo de los sprites. Ajusta su intensidad para dar profundidad a la escena.")
            .setView(form)
            .setPositiveButton("GUARDAR",(d,w)->{
                getPreferences(MODE_PRIVATE).edit()
                    .putBoolean("scene_shadows_enabled_"+activeProjectName(),enabled)
                    .putInt("scene_shadow_opacity_"+activeProjectName(),strength.getProgress()).apply();
                if(scenePreview!=null)scenePreview.invalidate();
                Toast.makeText(this,"Iluminación 2D guardada",Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton(enabled?"DESACTIVAR":"ACTIVAR",(d,w)->{
                getPreferences(MODE_PRIVATE).edit()
                    .putBoolean("scene_shadows_enabled_"+activeProjectName(),!enabled)
                    .putInt("scene_shadow_opacity_"+activeProjectName(),strength.getProgress()).apply();
                if(scenePreview!=null)scenePreview.invalidate();
                showSceneLightingDialog();
            }).setNegativeButton("CERRAR",null).show();
    }

    void showToolsPanel() {
        android.app.Dialog dialog=editorPanel("HERRAMIENTAS DE ESCENA",Gravity.RIGHT,88);
        LinearLayout panelView=(LinearLayout)((android.view.ViewGroup)dialog.findViewById(android.R.id.content)).getChildAt(0);
        if(panelView==null)return;
        panelView.addView(label("FÍSICA Y COLISIONES",11,Color.rgb(135,180,235)));
        panelView.addView(label("ILUMINACIÓN Y RENDER 2D",11,Color.rgb(135,180,235)));
        panelView.addView(editorButton("SOMBRAS 2D · INTENSIDAD",()->showSceneLightingDialog()));
        panelView.addView(editorButton("EFECTOS DE COLOR · SPRITES",()->openSpritePixelEditor()));
        panelView.addView(editorButton("CONFIGURAR FÍSICA",()->showPhysicsSettingsDialog()));
        panelView.addView(editorButton("CAPAS, CÁMARA Y PACKS",()->showEditorExtrasDialog()));
        panelView.addView(editorButton("AÑADIR BOTÓN MÓVIL",()->createMobileButtonDialog()));
        panelView.addView(label("RECURSOS EXTERNOS · SPRITECOOK",11,Color.rgb(135,180,235)));
        panelView.addView(editorButton("ABRIR SPRITECOOK · DESCARGAR SPRITES",()->showSpriteCookPanel()));
        panelView.addView(editorButton("ADMINISTRAR BOTONES",()->manageMobileButtons()));
        panelView.addView(editorButton("BOTONES PREDETERMINADOS",()->{scenePreview.installDefaultMobileButtons();dialog.dismiss();}));
        panelView.addView(label("AUDIO Y ARCHIVOS",11,Color.rgb(135,180,235)));
        panelView.addView(editorButton("SONIDOS PREDETERMINADOS",()->installOriginalSoundPack()));
        panelView.addView(editorButton("ELEGIR AUDIO / ARCHIVO",()->showSceneAudioPicker()));
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
        b.setOnClickListener(v -> { if (getPreferences(MODE_PRIVATE).getBoolean("ui_haptics", true)) v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); action.run(); });
        return b;
    }

    TextView uiSecondary(String value, Runnable action) {
        TextView b = uiLabel(value, 12, uiInk, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(11, 10, 11, 10);
        b.setMinHeight(42);
        b.setBackground(uiPanel(uiSurfaceRaised));
        b.setOnClickListener(v -> { if (getPreferences(MODE_PRIVATE).getBoolean("ui_haptics", true)) v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); action.run(); });
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

    void saveProjectFile(String relativePath,String value) {
        try {
            String project=activeProjectName();
            String folderPath=getPreferences(MODE_PRIVATE).getString("project_folder_"+project,"");
            if(folderPath.isEmpty())return;
            java.io.File root=new java.io.File(folderPath).getCanonicalFile();
            java.io.File target=new java.io.File(root,relativePath).getCanonicalFile();
            if(!target.getPath().startsWith(root.getPath()+java.io.File.separator))return;
            java.io.File parent=target.getParentFile();
            if(parent!=null&&!parent.exists()&&!parent.mkdirs())return;
            java.io.FileOutputStream out=new java.io.FileOutputStream(target);
            out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.close();
        } catch(Exception ignored) { }
    }

    void copyProjectAsset(String sourceFolder,String fileName,String targetFolder) {
        try {
            if(fileName==null||fileName.isEmpty())return;
            String folderPath=getPreferences(MODE_PRIVATE).getString("project_folder_"+activeProjectName(),"");
            if(folderPath.isEmpty())return;
            java.io.File root=new java.io.File(folderPath).getCanonicalFile();
            java.io.File targetDir=new java.io.File(root,targetFolder).getCanonicalFile();
            if(!targetDir.getPath().startsWith(root.getPath()+java.io.File.separator))return;
            if(!targetDir.exists()&&!targetDir.mkdirs())return;
            java.io.File source=new java.io.File(new java.io.File(getFilesDir(),sourceFolder),fileName);
            if(!source.isFile())return;
            java.io.InputStream in=new java.io.FileInputStream(source);
            java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(targetDir,fileName));
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            in.close();out.close();
        } catch(Exception ignored) { }
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
        root.setBackgroundColor(getPreferences(MODE_PRIVATE).getBoolean("ui_light_theme",false) ? Color.rgb(242,244,247) : Color.rgb(17, 20, 26));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(18, 13, 18, 13);
        header.setBackgroundColor(getPreferences(MODE_PRIVATE).getBoolean("ui_light_theme",false) ? Color.WHITE : Color.rgb(22, 26, 33));

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
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        navigation.setPadding(6, 6, 6, 6);
        navigation.setBackgroundColor(getPreferences(MODE_PRIVATE).getBoolean("ui_light_theme",false) ? Color.rgb(225,229,235) : Color.rgb(37, 40, 46));

        HorizontalScrollView menuScroll = new HorizontalScrollView(this);
        menuScroll.setHorizontalScrollBarEnabled(false);
        menuScroll.setFillViewport(false);
        LinearLayout menuItems = new LinearLayout(this);
        menuItems.setGravity(Gravity.CENTER_VERTICAL);
        addMenuItem(menuItems, "⌂  INICIO", () -> openPage(0));
        addMenuItem(menuItems, "▣  PROYECTOS", () -> openPage(1));
        addMenuItem(menuItems, "2D  EDITOR", () -> open2DWorkspace());
        addMenuItem(menuItems, "{ }  GRAFISRIPT", () -> openPage(3));
        addMenuItem(menuItems, "▤  RECURSOS", () -> openPage(4));
        addMenuItem(menuItems, "⚙  AJUSTES", () -> openPage(5));
        addMenuItem(menuItems, "▦  MARKET", () -> openPage(6));
        menuScroll.addView(menuItems);
        navigation.addView(menuScroll, new LinearLayout.LayoutParams(-1, -2));
        root.addView(navigation);
        setContentView(root);
    }

    void addMenuItem(LinearLayout menu, String title, Runnable action) {
        TextView item = uiSecondary(title, action);
        item.setSingleLine(true);
        item.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(3, 0, 3, 0);
        menu.addView(item, lp);
    }

    void open2DWorkspace() {
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String active = prefs.getString("active_project", "");
        if (!active.isEmpty() && "2D".equals(prefs.getString("project_type_" + active, "2D"))) {
            openProject(active);
            return;
        }

        java.util.Set<String> saved = prefs.getStringSet("blockforge_projects", new java.util.HashSet<>());
        for (String name : saved) {
            if ("2D".equals(prefs.getString("project_type_" + name, "2D"))) {
                openProject(name);
                return;
            }
        }
        create2DProject();
    }

    void create2DProject() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Nombre del juego 2D");
        new android.app.AlertDialog.Builder(this)
            .setTitle("Nuevo proyecto 2D")
            .setMessage("Crea tu juego y entra directamente al editor de escenas 2D.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("CREAR Y ABRIR", (dialog, which) -> {
                String name = input.getText().toString().trim();
                String safeName = name.replaceAll("[^a-zA-Z0-9._ -]", "_").trim();
                if (safeName.isEmpty() || ".".equals(safeName) || "..".equals(safeName)) {
                    Toast.makeText(this, "Escribe un nombre válido para el juego.", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                java.util.Set<String> names = new java.util.HashSet<>(prefs.getStringSet("blockforge_projects", new java.util.HashSet<>()));
                if (names.contains(name)) {
                    Toast.makeText(this, "Ya existe un proyecto con ese nombre.", Toast.LENGTH_SHORT).show();
                    return;
                }
                java.io.File projectsRoot = new java.io.File(getFilesDir(), "BlockForgeProjects");
                java.io.File folder = new java.io.File(projectsRoot, safeName);
                if ((!projectsRoot.exists() && !projectsRoot.mkdirs()) || (!folder.exists() && !folder.mkdirs())) {
                    Toast.makeText(this, "No se pudo crear la carpeta del proyecto.", Toast.LENGTH_LONG).show();
                    return;
                }
                names.add(name);
                prefs.edit().putStringSet("blockforge_projects", names)
                    .putString("project_type_" + name, "2D")
                    .putString("project_folder_" + name, folder.getAbsolutePath())
                    .putString("active_project", name).apply();
                openPage(2);
            }).show();
    }

    // Una misma vista Android no puede estar conectada a dos contenedores.
    // La movemos explícitamente entre el editor y el modo de juego.
    void detachScenePreviewFromParent() {
        if (scenePreview == null) return;
        android.view.ViewParent parent = scenePreview.getParent();
        if (parent instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) parent).removeView(scenePreview);
        }
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
        content.setPadding(18, 18, 18, 24);
        heading("Marketplace", "Biblioteca de recursos para crear tus juegos.");
        card("SPRITES Y PIXEL ART", "Personajes, enemigos, espadas, monedas, objetos, animaciones y tilesets. Descarga el recurso y luego impórtalo desde Recursos o desde el editor 2D.");
        content.addView(uiPrimary("🎨  SPRITECOOK · CREAR Y DESCARGAR", () -> openResourceUrl("https://www.spritecook.ai/es")));
        content.addView(uiSecondary("KENNEY · ASSETS GRATUITOS", () -> openResourceUrl("https://kenney.nl/assets")));
        content.addView(uiSecondary("OPEN GAME ART · SPRITES Y TEXTURAS", () -> openResourceUrl("https://opengameart.org/")));
        content.addView(uiSecondary("ITCH.IO · GAME ASSETS", () -> openResourceUrl("https://itch.io/game-assets")));
        content.addView(uiSecondary("CRAFTPPIX · PIXEL ART", () -> openResourceUrl("https://craftpix.net/freebies/")));
        uiSection("CATEGORÍAS", "RECURSOS PARA TU PROYECTO");
        card("🧍 PERSONAJES Y ENEMIGOS", "Sprites individuales, hojas de animación y personajes para plataformas, RPG y aventuras.");
        card("⚔️ ARMAS Y OBJETOS", "Espadas, escudos, pociones, monedas, cofres y objetos coleccionables.");
        card("🌄 FONDOS Y ESCENARIOS", "Fondos PNG/JPG, parallax, tilesets, suelos, paredes y plataformas.");
        card("✨ EFECTOS Y UI", "Explosiones, partículas, iconos, botones y elementos de interfaz.");
        card("Cómo instalar un recurso", "1. Abre una biblioteca.  2. Descarga el archivo PNG o ZIP.  3. Vuelve a BlockForge y entra en Recursos → Importar, o usa Añadir Sprite en el editor 2D.  4. Selecciona el archivo descargado.");
        card("Licencias", "Cada página tiene sus propias licencias. Revisa si permite uso comercial, modificaciones y redistribución antes de publicar tu juego. BlockForge no descarga ni incluye automáticamente archivos de terceros.");
        content.addView(uiPrimary("＋  IMPORTAR UN SPRITE O FONDO", () -> openSpriteDrawer()));
        content.addView(uiSecondary("←  VOLVER A PROYECTOS", () -> openPage(0)));
    }

    void openResourceUrl(String url) {
        try {
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception ex) {
            Toast.makeText(this, "No se pudo abrir la página. Comprueba tu conexión.", Toast.LENGTH_LONG).show();
        }
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
        ScrollView sidebarScroll = new ScrollView(this);
        sidebarScroll.setFillViewport(true);
        sidebarScroll.setVerticalScrollBarEnabled(true);
        sidebarScroll.addView(sidebar);
        LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.31f), -1);
        content.addView(sidebarScroll, sideLp);

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
        LinearLayout support = vertical();
        support.setPadding(12,12,12,12);
        support.setBackgroundColor(Color.rgb(25,28,34));
        support.addView(uiLabel("MOTOR GRÁFICO INDIE",11,Color.rgb(135,180,235),true));
        TextView supportText=uiLabel("Apoya al creador con donaciones, promocionando tus juegos o compartiendo ideas.",10,uiMuted,false);
        supportText.setPadding(0,5,0,7); support.addView(supportText);
        support.addView(editorButton("♡  APOYAR / GITHUB",()->{
            try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/sajgarcia12-crypto/blockforge_android"))); }
            catch(Exception ignored) { Toast.makeText(this,"GitHub: sajgarcia12-crypto/blockforge_android",Toast.LENGTH_LONG).show(); }
        }));
        support.addView(uiLabel("Discord: katorne1234  ·  Ideas y comunidad",10,uiMuted,false));
        content.addView(support,new LinearLayout.LayoutParams(-1,-2));
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

        EditText folderInput = new EditText(this);
        folderInput.setSingleLine(true);
        folderInput.setHint("Nombre de la carpeta (por defecto: proyecto)");
        form.addView(folderInput);
        TextView typeLabel = label("Tipo de proyecto", 13, gray);
        typeLabel.setPadding(0, 12, 0, 6);
        form.addView(typeLabel);
        Spinner type = new Spinner(this);
        String[] types = {"2D", "3D"};
        type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,types));
        form.addView(type);
        TextView resolutionLabel=label("Resolución del mundo 2D (píxeles)",13,gray);
        resolutionLabel.setPadding(0,12,0,6);form.addView(resolutionLabel);
        Spinner resolution=new Spinner(this);
        String[] resolutions={"320 × 180","640 × 360","1280 × 720","320 × 320","512 × 512","1920 × 1080"};
        resolution.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,resolutions));
        form.addView(resolution);

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
                String folderName=folderInput.getText().toString().trim();if(folderName.isEmpty())folderName=name;
                String safeFolder=folderName.replaceAll("[^a-zA-Z0-9._ -]","_").trim();
                if(safeFolder.isEmpty()||".".equals(safeFolder)||"..".equals(safeFolder)){Toast.makeText(this,"Nombre de carpeta no válido",Toast.LENGTH_SHORT).show();return;}
                String[] dims=resolution.getSelectedItem().toString().split("×");int projectWidth=320,projectHeight=180;
                try{projectWidth=Integer.parseInt(dims[0].trim());projectHeight=Integer.parseInt(dims[1].trim());}catch(Exception ignored){}
                android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                java.util.Set<String> names = new java.util.HashSet<>(
                        prefs.getStringSet("blockforge_projects", new java.util.HashSet<>()));
                if (names.contains(name)) {
                    Toast.makeText(this, "Ya existe un proyecto con ese nombre",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                java.io.File projectsRoot = new java.io.File(getFilesDir(), "BlockForgeProjects");
                java.io.File projectFolder = new java.io.File(projectsRoot, safeFolder);
                if ((!projectsRoot.exists() && !projectsRoot.mkdirs())
                        || (!projectFolder.exists() && !projectFolder.mkdirs())) {
                    Toast.makeText(this, "No se pudo crear la carpeta del proyecto",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String[] projectDirs = {"assets","scenes","scripts","textures","audio","backgrounds","exports"};
                for (String dirName : projectDirs) {
                    java.io.File dir = new java.io.File(projectFolder, dirName);
                    if (!dir.exists() && !dir.mkdirs()) {
                        Toast.makeText(this, "No se pudo crear la carpeta " + dirName, Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                try {
                    org.json.JSONObject manifest = new org.json.JSONObject();
                    manifest.put("name", name); manifest.put("type", projectType);
                    manifest.put("format", "blockforge-project"); manifest.put("version", 1);
                    manifest.put("folder",safeFolder);manifest.put("width",projectWidth);manifest.put("height",projectHeight);manifest.put("unit","pixels");
                    manifest.put("createdAt", System.currentTimeMillis());
                    java.io.FileOutputStream manifestOut = new java.io.FileOutputStream(new java.io.File(projectFolder, "project.json"));
                    manifestOut.write(manifest.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    manifestOut.close();
                } catch (Exception ex) {
                    Toast.makeText(this, "No se pudo crear project.json", Toast.LENGTH_LONG).show(); return;
                }
                names.add(name);
                prefs.edit().putStringSet("blockforge_projects", names)
                    .putString("project_type_" + name, projectType)
                    .putString("project_folder_" + name, projectFolder.getAbsolutePath())
                    .putInt("scene_map_w_"+name,Math.max(5,Math.min(64,projectWidth/32)))
                    .putInt("scene_map_h_"+name,Math.max(5,Math.min(64,projectHeight/32)))
                    .putInt("scene_bg_color_" + name, Color.rgb(24,32,43)).apply();
                Toast.makeText(this, "Proyecto " + projectType + " creado: " + name + " · carpeta " + safeFolder + (projectType.equals("2D")?" · "+projectWidth+"×"+projectHeight+" px":""),
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

        // Editor 2D de escritorio adaptado a móvil horizontal: árbol / viewport / inspector.
        content.setPadding(0, 0, 0, 0);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(Color.rgb(30, 32, 36));
        if (scenePlayMode) {
            content.setPadding(0,0,0,0);
            LinearLayout gameBar = new LinearLayout(this);
            gameBar.setGravity(Gravity.CENTER_VERTICAL);
            gameBar.setPadding(10,4,10,4);
            gameBar.setBackgroundColor(Color.rgb(16,19,25));
            gameBar.addView(uiLabel("▶  JUEGO · "+project,12,uiInk,true),new LinearLayout.LayoutParams(0,44,1));
            gameBar.addView(editorButton("Ⅱ PAUSAR",()->{ if(physicsRunning){physicsRunning=false;if(scenePreview!=null)scenePreview.stopPhysics();}else{physicsRunning=true;if(scenePreview!=null)scenePreview.startPhysics();} }),new LinearLayout.LayoutParams(90,42));
            gameBar.addView(editorButton("■ EDITOR",()->stopSceneGame()),new LinearLayout.LayoutParams(90,42));
            content.addView(gameBar,new LinearLayout.LayoutParams(-1,50));
            if(scenePreview==null)scenePreview=new ScenePreview();
            detachScenePreviewFromParent();
            scenePreview.setBackgroundColor(Color.rgb(24,31,43));
            if(scenePreview.sprites.size()>0 && (scenePreview.selectedSprite<0||scenePreview.selectedSprite>=scenePreview.sprites.size()))scenePreview.selectedSprite=0;
            gameBar.setElevation(12f);
            scenePreview.setElevation(1f);
            content.addView(scenePreview,new LinearLayout.LayoutParams(-1,0,1));
            // Arrancar la física cuando el viewport ya esté unido a la ventana.
            if(physicsRunning) scenePreview.post(() -> {
                if(scenePlayMode && physicsRunning && scenePreview != null
                        && scenePreview.isAttachedToWindow()) scenePreview.startPhysics();
            });
            return;
        }

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(8, 4, 8, 4);
        top.setBackgroundColor(Color.rgb(22, 24, 28));
        top.addView(editorButton("☰", () -> openPage(1)), new LinearLayout.LayoutParams(42, 42));
        LinearLayout brand = vertical();
        brand.setPadding(8, 0, 8, 0);
        brand.addView(uiLabel("BLOCKFORGE", 12, uiInk, true));
        brand.addView(uiLabel(project + " · 2D", 9, uiMuted, false));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        HorizontalScrollView workspaceNav = new HorizontalScrollView(this);
        workspaceNav.setHorizontalScrollBarEnabled(false);
        workspaceNav.setFillViewport(false);
        LinearLayout workspaceTabs = new LinearLayout(this);
        workspaceTabs.setGravity(Gravity.CENTER_VERTICAL);
        addWorkspaceTab(workspaceTabs, "2D", true, () -> openPage(2));
        addWorkspaceTab(workspaceTabs, "SCRIPT", false, () -> openPage(3));
        addWorkspaceTab(workspaceTabs, "JUEGO", false, () -> { startSceneGame(); });
        addWorkspaceTab(workspaceTabs, "ASSETS", false, () -> openPage(4));
        addWorkspaceTab(workspaceTabs, "AJUSTES", false, () -> openPage(5));
        workspaceNav.addView(workspaceTabs);
        top.addView(workspaceNav, new LinearLayout.LayoutParams(0, 46, 1));
        top.addView(editorButton("▶", () -> startSceneGame()), new LinearLayout.LayoutParams(42,42));
        top.addView(editorButton("💾", () -> { if(scenePreview!=null){scenePreview.persist();scenePreview.persistSprites();} Toast.makeText(this,"Escena guardada",Toast.LENGTH_SHORT).show(); }), new LinearLayout.LayoutParams(42,42));
        content.addView(top, new LinearLayout.LayoutParams(-1, 48));

        HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(5,3,5,3);
        tools.setBackgroundColor(Color.rgb(39,42,47));
        addWorkspaceTool(tools,"↖ Seleccionar / mover", () -> {sceneToolMode=0;});
        addWorkspaceTool(tools,"✋ Mano", () -> {cameraPanMode=!cameraPanMode;Toast.makeText(this,cameraPanMode?"Arrastra para mover la vista":"Selección activa",Toast.LENGTH_SHORT).show();});
        addWorkspaceTool(tools,sceneGridVisible?"▦ Cuadrícula":"□ Cuadrícula",()->{sceneGridVisible=!sceneGridVisible;if(scenePreview!=null)scenePreview.invalidate();});
        addWorkspaceTool(tools,"＋ Sprite",()->openSpriteDrawer());
        addWorkspaceTool(tools,"Nodos",()->showNodeGraphPanel());
        addWorkspaceTool(tools,"Animación",()->showTimelinePanel());
        addWorkspaceTool(tools,"Deshacer",()->{if(scenePreview!=null)scenePreview.undo();});
        addWorkspaceTool(tools,"Rehacer",()->{if(scenePreview!=null)scenePreview.redo();});
        toolsScroll.addView(tools);
        content.addView(toolsScroll,new LinearLayout.LayoutParams(-1,52));

        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);
        workspace.setBackgroundColor(Color.rgb(18,20,24));
        int screenWidth=getResources().getDisplayMetrics().widthPixels;
        boolean compactEditor=screenWidth<650;
        int sideWidth=compactEditor?0:Math.max(148,Math.min(205,screenWidth/5));
        LinearLayout left = vertical();
        left.setPadding(7,7,7,7);
        left.setBackgroundColor(Color.rgb(36,39,44));
        left.addView(panelHeading("ESCENA"));
        left.addView(editorButton("＋ Nodo raíz",()->showNodePalette()));
        left.addView(editorButton("＋ Sprite",()->openSpriteDrawer()));
        left.addView(editorButton("＋ Cámara 2D",()->{if(scenePreview!=null){scenePreview.cameraOffsetX=0;scenePreview.cameraOffsetY=0;scenePreview.cameraZoom=1f;scenePreview.saveCamera();scenePreview.invalidate();}}));
        left.addView(editorButton("Árbol de nodos",()->showSceneTreePanel()));
        left.addView(panelHeading("ARCHIVOS"));
        left.addView(editorButton("Importar PNG",()->pickSpriteImage()));
        left.addView(editorButton("Recursos",()->openPage(4)));
        if(!compactEditor) workspace.addView(left,new LinearLayout.LayoutParams(sideWidth,-1));

        LinearLayout center = vertical();
        center.setPadding(4,4,4,4);
        center.setBackgroundColor(Color.rgb(27,29,33));
        LinearLayout viewportBar = new LinearLayout(this);
        viewportBar.setGravity(Gravity.CENTER_VERTICAL);
        viewportBar.setPadding(8,4,8,4);
        viewportBar.addView(uiLabel("VISTA 2D",10,uiInk,true),new LinearLayout.LayoutParams(0,30,1));
        viewportBar.addView(uiLabel(scenePreviewStatus(),9,uiMuted,false));
        center.addView(viewportBar);
        if (scenePreview == null) scenePreview = new ScenePreview();
        detachScenePreviewFromParent();
        String savedSceneAudio=getPreferences(MODE_PRIVATE).getString("scene_audio_"+project,"");
        if(!savedSceneAudio.isEmpty()&&sceneAudioPlayer==null)playSceneAudio(savedSceneAudio);
        center.addView(scenePreview,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout zoom = new LinearLayout(this);
        zoom.setGravity(Gravity.CENTER_VERTICAL);
        zoom.addView(editorButton("−",()->scenePreview.changeZoom(0.83f)),new LinearLayout.LayoutParams(42,36));
        zoom.addView(editorButton("Centrar",()->scenePreview.resetCamera()),new LinearLayout.LayoutParams(75,36));
        zoom.addView(editorButton("+",()->scenePreview.changeZoom(1.2f)),new LinearLayout.LayoutParams(42,36));
        zoom.addView(editorButton("Cuadrícula",()->{sceneGridVisible=!sceneGridVisible;scenePreview.invalidate();}),new LinearLayout.LayoutParams(0,36,1));
        center.addView(zoom);
        workspace.addView(center,new LinearLayout.LayoutParams(0,-1,1));

        LinearLayout inspector = vertical();
        inspector.setPadding(7,7,7,7);
        inspector.setBackgroundColor(Color.rgb(36,39,44));
        inspector.addView(panelHeading("INSPECTOR"));
        inspector.addView(editorButton("Propiedades",()->showInspectorPanel()));
        inspector.addView(editorButton("Transformar",()->showInspectorPanel()));
        inspector.addView(editorButton("Físicas 2D",()->showPhysicsSettings()));
        inspector.addView(editorButton("Colisiones "+(sceneCollisionsEnabled?"ON":"OFF"),()->{sceneCollisionsEnabled=!sceneCollisionsEnabled;openPage(2);}));
        inspector.addView(panelHeading("CAPAS"));
        inspector.addView(editorButton("Fondo · opciones",()->showBackgroundPicker()));
        inspector.addView(editorButton("Mundo",()->{sceneLayer=1;Toast.makeText(this,"Capa Mundo",Toast.LENGTH_SHORT).show();}));
        inspector.addView(editorButton("Frente",()->{sceneLayer=2;Toast.makeText(this,"Capa Frente",Toast.LENGTH_SHORT).show();}));
        inspector.addView(panelHeading("PROYECTO"));
        inspector.addView(editorButton("Tamaño del mapa",()->showMapSizeDialog()));
        inspector.addView(editorButton("Exportar escena",()->exportScenePicker()));
        if(!compactEditor) workspace.addView(inspector,new LinearLayout.LayoutParams(sideWidth,-1));
        int workspaceHeight=compactEditor?Math.max(300,(int)(getResources().getDisplayMetrics().heightPixels*0.48f)):Math.max(420,(int)(getResources().getDisplayMetrics().heightPixels*0.58f));
        content.addView(workspace,new LinearLayout.LayoutParams(-1,workspaceHeight));
        if(compactEditor){
            HorizontalScrollView quickScroll=new HorizontalScrollView(this);
            quickScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout quick=new LinearLayout(this);quick.setPadding(6,5,6,5);quick.setGravity(Gravity.CENTER_VERTICAL);
            String[] names={"＋ Sprite","Fondo","Propiedades","Árbol","Física","Capas","Recursos","Nodos","Animación","Deshacer","Rehacer"};
            Runnable[] actions={
                ()->openSpriteDrawer(),()->showBackgroundPicker(),()->showInspectorPanel(),()->showSceneTreePanel(),
                ()->showPhysicsSettingsDialog(),()->showEditorExtrasDialog(),()->openPage(4),()->showNodeGraphPanel(),
                ()->showTimelinePanel(),()->{if(scenePreview!=null)scenePreview.undo();},()->{if(scenePreview!=null)scenePreview.redo();}
            };
            for(int i=0;i<names.length;i++){TextView b=editorButton(names[i],actions[i]);b.setTextSize(12);b.setMinHeight(54);b.setPadding(14,10,14,10);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,54);lp.setMargins(3,0,3,0);quick.addView(b,lp);}
            quickScroll.addView(quick);content.addView(quickScroll,new LinearLayout.LayoutParams(-1,64));
        }

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(7,4,7,5);
        bottom.setBackgroundColor(Color.rgb(28,30,34));
        LinearLayout bottomTabs = new LinearLayout(this);
        bottomTabs.setGravity(Gravity.CENTER_VERTICAL);
        addWorkspaceTool(bottomTabs,"Salida",()->showRuntimeOutput());
        addWorkspaceTool(bottomTabs,"Depurador",()->showRuntimeOutput());
        addWorkspaceTool(bottomTabs,"Animación",()->showTimelinePanel());
        addWorkspaceTool(bottomTabs,"Grafisript · Nodos",()->showNodeGraphPanel());
        bottom.addView(bottomTabs);
        bottom.addView(uiLabel(scenePreview==null?"LISTO":scenePreview.animationStatus(),9,uiMuted,false));
        content.addView(bottom,new LinearLayout.LayoutParams(-1,92));
        if(physicsRunning)scenePreview.startPhysics();
    }

    void addWorkspaceTab(LinearLayout row,String text,boolean active,Runnable action){
        TextView v=editorButton((active?"● ":"")+text,action);
        v.setTextSize(10);
        v.setSingleLine(true);
        v.setPadding(13,0,13,0);
        v.setTextColor(active?Color.rgb(105,170,245):uiInk);
        v.setBackground(uiPanel(active?Color.rgb(39,54,73):Color.rgb(30,34,41)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,38);
        lp.setMargins(2,0,2,0);
        row.addView(v,lp);
    }
    void addWorkspaceTool(LinearLayout row,String text,Runnable action){
        TextView v=editorButton(text,action);v.setTextSize(10);
        row.addView(v,new LinearLayout.LayoutParams(-2,38));
    }
    TextView panelHeading(String text){
        TextView v=uiLabel(text,10,Color.rgb(145,178,218),true);
        v.setPadding(5,10,3,7);return v;
    }

    static final String[][] GRAPH_NODE_CATALOG = {
        {"PERSONAJE","personaje","0"},
        {"Crear personaje","crear_personaje","0"},
        {"Mover jugador X/Z","jugador_mover","1"},
        {"Saltar jugador","jugador_saltar","5"},
        {"Girar jugador","jugador_girar","90"},
        {"Establecer vida","vida","100"},
        {"Aplicar daño","danio","10"},
        {"Mover sprite X/Y","mover","1"},
        {"Saltar sprite","saltar","6"},
        {"Crear bloque 2D","bloque2d","1"},
        {"Cambiar capa","capa","1"},
        {"Zoom de cámara","cam_zoom","1"},
        {"Centrar cámara","cam_center","0"},
        {"Activar física","fisica_on","0"},
        {"Pausar física","fisica_off","0"},
        {"Mostrar mensaje","mensaje","0"},
        {"Crear enemigo","obj_enemy","1"},
        {"Crear espada","obj_sword","1"},
        {"Crear moneda","obj_coin","1"},
        {"Crear piedra","obj_stone","1"}
    };

    String graphNodeTitle(String type){
        for(String[] item:GRAPH_NODE_CATALOG)if(item[1].equals(type))return item[0];
        return "Nodo personalizado";
    }
    float graphNodeDefault(String type){
        for(String[] item:GRAPH_NODE_CATALOG)if(item[1].equals(type))try{return Float.parseFloat(item[2]);}catch(Exception ignored){}
        return 0f;
    }
    void showNodePalette(){
        showNodeGraphPanel();
    }
    void persistGraphNodes(){
        org.json.JSONArray a=new org.json.JSONArray();
        for(GraphNode n:grafisriptNodes){
            org.json.JSONObject o=new org.json.JSONObject();
            try{o.put("type",n.type);o.put("value",n.value);a.put(o);}catch(Exception ignored){}
        }
        getPreferences(MODE_PRIVATE).edit().putString("grafisript_nodes_"+activeProjectName(),a.toString()).apply();
    }
    void loadGraphNodes(){
        grafisriptNodes.clear();
        try{
            org.json.JSONArray a=new org.json.JSONArray(getPreferences(MODE_PRIVATE).getString("grafisript_nodes_"+activeProjectName(),"[]"));
            for(int i=0;i<a.length();i++){
                org.json.JSONObject o=a.getJSONObject(i);
                grafisriptNodes.add(new GraphNode(o.optString("type","saltar"),(float)o.optDouble("value",6)));
            }
        }catch(Exception ignored){}
    }
    String graphToGrafisript(){
        StringBuilder b=new StringBuilder("# Código generado desde el grafo visual de BlockForge\n");
        for(GraphNode n:grafisriptNodes){
            String t=n.type;
            if("personaje".equals(t))b.append("# Usa el sprite marcado como personaje en el inspector\n");
            else if("crear_personaje".equals(t))b.append("crear_personaje Jugador\n");
            else if("jugador_mover".equals(t))b.append("jugador.mover(").append(n.value).append(", 0)\n");
            else if("jugador_saltar".equals(t))b.append("jugador.saltar(").append(Math.max(0.1f,Math.min(100f,n.value))).append(")\n");
            else if("jugador_girar".equals(t))b.append("jugador.girar(").append(n.value).append(")\n");
            else if("vida".equals(t))b.append("jugador.vida(").append(Math.max(0,Math.min(1000,Math.round(n.value)))).append(")\n");
            else if("danio".equals(t))b.append("jugador.recibir_danio(").append(Math.max(0,Math.min(1000,Math.round(n.value)))).append(")\n");
            else if("mover".equals(t))b.append("mover_sprite(").append(n.value).append(", 0)\n");
            else if("saltar".equals(t))b.append("saltar_sprite(").append(Math.max(0.1f,Math.min(20f,n.value))).append(")\n");
            else if("bloque2d".equals(t))b.append("crear_bloque2d ").append(Math.max(0,Math.round(n.value))).append(" 0 1\n");
            else if("capa".equals(t))b.append("capa ").append(Math.max(0,Math.min(2,Math.round(n.value)))).append("\n");
            else if("cam_zoom".equals(t))b.append("camara.zoom(").append(Math.max(0.5f,Math.min(2f,n.value))).append(")\n");
            else if("cam_center".equals(t))b.append("camara.centrar()\n");
            else if("fisica_on".equals(t))b.append("fisica.activar()\n");
            else if("fisica_off".equals(t))b.append("fisica.pausar()\n");
            else if("mensaje".equals(t))b.append("mostrar_mensaje \"Hola desde BlockForge\"\n");
            else if("obj_enemy".equals(t))b.append("crear_objeto2d enemy ").append(Math.max(0,Math.round(n.value))).append(" 1\n");
            else if("obj_sword".equals(t))b.append("crear_objeto2d sword ").append(Math.max(0,Math.round(n.value))).append(" 1\n");
            else if("obj_coin".equals(t))b.append("crear_objeto2d coin ").append(Math.max(0,Math.round(n.value))).append(" 1\n");
            else if("obj_stone".equals(t))b.append("crear_objeto2d stone ").append(Math.max(0,Math.round(n.value))).append(" 1\n");
        }
        return b.toString();
    }
    void showNodeGraphPanel(){
        loadGraphNodes();
        final android.app.Dialog dialog=new android.app.Dialog(this);
        LinearLayout page=vertical();
        page.setPadding(10,10,10,10);
        page.setBackgroundColor(Color.rgb(15,19,26));
        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=label("GRAFISRIPT · EDITOR DE NODOS",16,white);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(editorButton("✕",dialog::dismiss),new LinearLayout.LayoutParams(48,44));
        page.addView(header);
        page.addView(label("Elige un nodo para añadirlo. Edita sus valores y cambia el orden con ↑ ↓. Los cables curvos muestran la secuencia.",11,gray));
        LinearLayout workspace=new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);
        workspace.setBackgroundColor(Color.rgb(19,23,30));
        LinearLayout palette=vertical();
        palette.setPadding(5,5,5,5);
        palette.setBackgroundColor(Color.rgb(28,34,44));
        palette.addView(panelHeading("BIBLIOTECA"));
        ScrollView paletteScroll=new ScrollView(this);
        LinearLayout paletteItems=vertical();
        for(String[] item:GRAPH_NODE_CATALOG){
            final String type=item[1];
            TextView chip=editorButton("＋ "+item[0],()->{
                grafisriptNodes.add(new GraphNode(type,graphNodeDefault(type)));
                persistGraphNodes();dialog.dismiss();showNodeGraphPanel();
            });
            chip.setTextSize(10);
            chip.setMinHeight(48);
            paletteItems.addView(chip);
            chip.setOnLongClickListener(v->{
                android.content.ClipData data=android.content.ClipData.newPlainText("grafisript-node",type);
                if(android.os.Build.VERSION.SDK_INT>=24)v.startDragAndDrop(data,new View.DragShadowBuilder(v),type,0);
                else v.startDrag(data,new View.DragShadowBuilder(v),type,0);
                return true;
            });
        }
        paletteScroll.addView(paletteItems);
        palette.addView(paletteScroll,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView graphScroll=new ScrollView(this);
        graphScroll.setFillViewport(true);
        LinearLayout graphStack=vertical();
        graphStack.setPadding(10,10,10,20);
        graphStack.setOnDragListener((v,event)->{
            if(event.getAction()==android.view.DragEvent.ACTION_DRAG_STARTED)return event.getClipDescription()!=null;
            if(event.getAction()==android.view.DragEvent.ACTION_DROP){
                String type=String.valueOf(event.getClipData().getItemAt(0).getText());
                grafisriptNodes.add(new GraphNode(type,graphNodeDefault(type)));
                persistGraphNodes();dialog.dismiss();showNodeGraphPanel();return true;
            }
            return true;
        });
        if(grafisriptNodes.isEmpty()){
            TextView empty=label("TU GRAFO ESTÁ VACÍO\\n\\nAñade nodos desde la biblioteca.",13,gray);
            empty.setGravity(Gravity.CENTER);empty.setPadding(12,55,12,55);graphStack.addView(empty);
        }
        for(int i=0;i<grafisriptNodes.size();i++){
            final int index=i;
            GraphNode node=grafisriptNodes.get(i);
            LinearLayout card=vertical();
            card.setPadding(10,9,10,9);
            card.setBackground(uiPanel(Color.rgb(39,51,68)));
            TextView title=label((i==0?"● INICIO  ":"● SIGUIENTE  ")+graphNodeTitle(node.type).toUpperCase(java.util.Locale.ROOT),12,white);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(title);
            if(!"personaje".equals(node.type)&&!"fisica_on".equals(node.type)&&!"fisica_off".equals(node.type)&&!"cam_center".equals(node.type)&&!"mensaje".equals(node.type)){
                TextView value=editorButton("VALOR: "+node.value+"   ✎ EDITAR",()->{
                    EditText input=new EditText(this);
                    input.setSingleLine(true);
                    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                    input.setText(String.valueOf(node.value));
                    new android.app.AlertDialog.Builder(this).setTitle("Parámetro · "+graphNodeTitle(node.type)).setView(input)
                        .setPositiveButton("GUARDAR",(d,w)->{
                            try{float v=Float.parseFloat(input.getText().toString());if(Float.isNaN(v)||Float.isInfinite(v))throw new NumberFormatException();node.value=v;persistGraphNodes();dialog.dismiss();showNodeGraphPanel();}
                            catch(Exception ex){Toast.makeText(this,"Escribe un número válido",Toast.LENGTH_SHORT).show();}
                        }).setNegativeButton("Cancelar",null).show();
                });
                card.addView(value);
            }
            LinearLayout actions=new LinearLayout(this);
            actions.addView(editorButton("↑",()->{if(index>0){java.util.Collections.swap(grafisriptNodes,index,index-1);persistGraphNodes();dialog.dismiss();showNodeGraphPanel();}}),new LinearLayout.LayoutParams(0,44,1));
            actions.addView(editorButton("↓",()->{if(index<grafisriptNodes.size()-1){java.util.Collections.swap(grafisriptNodes,index,index+1);persistGraphNodes();dialog.dismiss();showNodeGraphPanel();}}),new LinearLayout.LayoutParams(0,44,1));
            actions.addView(editorButton("ELIMINAR",()->{grafisriptNodes.remove(index);persistGraphNodes();dialog.dismiss();showNodeGraphPanel();}),new LinearLayout.LayoutParams(0,44,2));
            card.addView(actions);
            graphStack.addView(card);
            if(i<grafisriptNodes.size()-1){
                View wire=new View(this){
                    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
                    @Override protected void onDraw(Canvas canvas){
                        super.onDraw(canvas);
                        float x=getWidth()/2f;
                        p.setColor(Color.rgb(76,170,235));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3f);
                        Path path=new Path();path.moveTo(x,0);path.cubicTo(x,14,x,25,x,40);
                        canvas.drawPath(path,p);
                        p.setStyle(Paint.Style.FILL);canvas.drawCircle(x,40,4,p);
                    }
                };
                graphStack.addView(wire,new LinearLayout.LayoutParams(-1,44));
            }
        }
        graphScroll.addView(graphStack);
        workspace.addView(palette,new LinearLayout.LayoutParams(Math.max(138,getResources().getDisplayMetrics().widthPixels*36/100),-1));
        workspace.addView(graphScroll,new LinearLayout.LayoutParams(0,-1,1));
        page.addView(workspace,new LinearLayout.LayoutParams(-1,0,1));
        page.addView(editorButton("GENERAR CÓDIGO GRAFISRIPT",()->{
            String code=graphToGrafisript();
            getPreferences(MODE_PRIVATE).edit().putString("grafisript_"+activeProjectName(),code).apply();
            saveProjectFile("scripts/main.bf",code);
            Toast.makeText(this,"Código generado y guardado",Toast.LENGTH_LONG).show();
            dialog.dismiss();showCodePanel();
        }));
        page.addView(editorButton("CERRAR",dialog::dismiss));
        dialog.setContentView(page);
        android.view.Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(-1,-1);
            window.setGravity(Gravity.CENTER);
        }
        dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);
    }

    void showPhysicsSettings(){
        EditText gravity=new EditText(this);gravity.setSingleLine(true);gravity.setInputType(8194|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);gravity.setText(String.valueOf(physicsGravity));
        EditText bounce=new EditText(this);bounce.setSingleLine(true);bounce.setInputType(8194|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);bounce.setText(String.valueOf(physicsBounce));
        LinearLayout form=vertical();form.addView(label("Gravedad",12,gray));form.addView(gravity);form.addView(label("Rebote",12,gray));form.addView(bounce);
        new android.app.AlertDialog.Builder(this).setTitle("Físicas 2D").setView(form).setPositiveButton("Guardar",(d,w)->{try{physicsGravity=Math.max(0,Math.min(100,Float.parseFloat(gravity.getText().toString())));physicsBounce=Math.max(0,Math.min(1,Float.parseFloat(bounce.getText().toString())));getPreferences(MODE_PRIVATE).edit().putFloat("physics_gravity",physicsGravity).putFloat("physics_bounce",physicsBounce).apply();}catch(Exception e){Toast.makeText(this,"Valores no válidos",Toast.LENGTH_SHORT).show();}}).setNegativeButton("Cancelar",null).show();
    }
    void showRuntimeOutput(){
        new android.app.AlertDialog.Builder(this).setTitle("Salida · BlockForge")
            .setMessage("Grafisript · "+grafisriptNodes.size()+" nodos visuales\nEscena: "+scenePreviewStatus()+"\nFísica: "+(physicsRunning?"activa":"en pausa"))
            .setPositiveButton("Aceptar",null).show();
    }

    void showSpriteCookPanel() {
        LinearLayout box=vertical();box.setPadding(18,12,18,12);
        box.addView(label("Descarga personajes, enemigos, espadas, monedas, fondos o tilesets en PNG desde SpriteCook. Después vuelve aquí e impórtalos a tu proyecto.",14,gray));
        box.addView(editorButton("1 · ABRIR SPRITECOOK",()->{
            try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://www.spritecook.ai/es")));}
            catch(Exception ex){Toast.makeText(this,"No se pudo abrir el navegador",Toast.LENGTH_SHORT).show();}
        }));
        box.addView(editorButton("2 · IMPORTAR LO DESCARGADO",()->openSpriteDrawer()));
        box.addView(label("Consejo: descarga PNG con fondo transparente para personajes y objetos. Para fondos, usa PNG o JPG.",12,gray));
        new android.app.AlertDialog.Builder(this).setTitle("SpriteCook · Recursos para tu juego").setView(box).setNegativeButton("Cerrar",null).show();
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

    void ensureOriginalSoundPackInstalled() {
        try {
            java.io.File folder=new java.io.File(getFilesDir(),"resources");
            if(!folder.exists()&&!folder.mkdirs())return;
            java.io.File click=new java.io.File(folder,"bf_click_original.wav");
            java.io.File jump=new java.io.File(folder,"bf_jump_original.wav");
            java.io.File coin=new java.io.File(folder,"bf_coin_original.wav");
            java.io.File ambient=new java.io.File(folder,"bf_ambient_original.wav");
            if(!click.exists())createToneWav(click,660,90);
            if(!jump.exists())createToneWav(jump,520,180);
            if(!coin.exists())createToneWav(coin,990,220);
            if(!ambient.exists())createToneWav(ambient,220,900);
        } catch(Exception ex) { android.util.Log.w("BlockForge","No se pudo preparar el pack de audio",ex); }
    }

    void installOriginalSoundPack() {
        try {
            ensureOriginalSoundPackInstalled();
            java.io.File folder=new java.io.File(getFilesDir(),"resources");
            if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException("No se pudo crear Recursos");
            createToneWav(new java.io.File(folder,"bf_click_original.wav"),660,90);
            createToneWav(new java.io.File(folder,"bf_jump_original.wav"),520,180);
            createToneWav(new java.io.File(folder,"bf_coin_original.wav"),990,220);
            createToneWav(new java.io.File(folder,"bf_ambient_original.wav"),220,900);
            final String[] files={"bf_click_original.wav","bf_jump_original.wav","bf_coin_original.wav","bf_ambient_original.wav"};
            new android.app.AlertDialog.Builder(this).setTitle("Sonidos sintéticos originales").setItems(new String[]{"Click","Salto","Moneda","Ambiente"},(d,w)->playSceneAudio(files[w])).setNegativeButton("Cerrar",null).show();
            Toast.makeText(this,"Pack de sonidos originales instalado",Toast.LENGTH_LONG).show();
        } catch(Exception ex){Toast.makeText(this,"Error creando sonidos: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void createToneWav(java.io.File file,int frequency,int durationMs) throws java.io.IOException {
        int rate=22050,count=rate*durationMs/1000;
        java.io.DataOutputStream out=new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(file)));
        out.writeBytes("RIFF");writeLE(out,36+count*2);out.writeBytes("WAVEfmt ");writeLE(out,16);writeLE(out,1);writeLE(out,1);writeLE(out,rate);writeLE(out,rate*2);writeLE(out,2);writeLE(out,16);out.writeBytes("data");writeLE(out,count*2);
        for(int i=0;i<count;i++){double fade=Math.min(1.0,Math.min(i/500.0,(count-i)/1200.0));short v=(short)(Math.sin(2*Math.PI*frequency*i/rate)*5000*fade);out.write(v&255);out.write((v>>8)&255);}out.close();
    }
    void writeLE(java.io.DataOutputStream out,int v)throws java.io.IOException{out.write(v&255);out.write((v>>8)&255);out.write((v>>16)&255);out.write((v>>24)&255);}

    void showBackgroundPicker(){
        String[] options={"Crear fondo de color","Fondos predeterminados","Añadir imagen desde archivos","Quitar imagen"};
        new android.app.AlertDialog.Builder(this).setTitle("FONDO DE LA ESCENA").setItems(options,(d,w)->{
            if(w==0||w==1){
                final int[] colors=w==0?new int[]{Color.rgb(24,32,43),Color.rgb(36,54,82),Color.rgb(45,67,48),Color.rgb(78,48,70),Color.BLACK,Color.WHITE}:new int[]{Color.rgb(100,170,225),Color.rgb(47,91,58),Color.rgb(190,105,65),Color.rgb(10,17,35)};
                final String[] labels=w==0?new String[]{"Azul noche","Azul profundo","Bosque","Atardecer","Negro","Blanco"}:new String[]{"Cielo","Bosque","Atardecer","Noche"};
                new android.app.AlertDialog.Builder(this).setTitle("Seleccionar fondo").setItems(labels,(dd,k)->{getPreferences(MODE_PRIVATE).edit().putInt("scene_bg_color_"+activeProjectName(),colors[k]).remove("scene_bg_image_"+activeProjectName()).apply();if(scenePreview!=null)scenePreview.invalidate();}).show();
            }else if(w==2){android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("image/*");startActivityForResult(intent,PICK_BACKGROUND_REQUEST);}
            else{getPreferences(MODE_PRIVATE).edit().remove("scene_bg_image_"+activeProjectName()).apply();if(scenePreview!=null)scenePreview.invalidate();}
        }).show();
    }

    void playSceneAudio(String name) {
        stopSceneAudio();
        try {
            sceneAudioPlayer=new android.media.MediaPlayer();sceneAudioPrepared=false;
            java.io.File file=new java.io.File(new java.io.File(getFilesDir(),"resources"),name);
            if(!file.isFile())throw new java.io.IOException("El audio no existe en Recursos");
            copyProjectAsset("resources",name,"audio");
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
        codeEditor.setTextSize(getPreferences(MODE_PRIVATE).getInt("editor_font_size", 14));
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
            "funcion saludar {\n"+
            "    mostrar_mensaje \"Hola desde una función\"\n"+
            "}\n"+
            "llamar saludar\n"+
            "ver_bloques\n"
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

        card("Sintaxis del lenguaje",
                "variable puntos = 2 + 3\n"+
                "si puntos > 3 {\n"+
                "    mostrar_mensaje \"Ganaste: ${puntos}\"\n"+
                "} sino {\n"+
                "    mostrar_mensaje \"Sigue intentando\"\n"+
                "}\n"+
                "repetir 3 {\n"+
                "    crear_bloque piedra 1 0 1\n"+
                "}\n"+
                "crear_personaje Jugador\n"+
                "jugador.mover(2, 1)\n"+
                "mostrar_mensaje \"Listo\"");
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

    void runGrafisript() {
        if (codeEditor == null || runtimeOutput == null) return;
        String[] lines = codeEditor.getText().toString().split("\\r?\\n");
        grafisriptEasyContext = "";
        grafisriptEnemyDamage = 20;

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

            if (line.equals("{")) {
                int bodyStart = cursor[0];
                int bodyEnd = findGrafisriptBlockEnd(lines, bodyStart);
                if (bodyEnd < 0) {
                    output.append("Línea ").append(lineNumber).append(": falta cerrar el bloque con }\\n");
                    return executed;
                }
                String[] definition = java.util.Arrays.copyOfRange(lines, bodyStart, bodyEnd);
                executeEasyGrafisriptDefinition(definition, output, lineNumber - 1);
                cursor[0] = bodyEnd + 1;
                executed++;
                continue;
            }

            if (executeEasyGrafisriptLine(line, output, lineNumber - 1)) { executed++; continue; }

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
            if ((line.startsWith("si ") || line.startsWith("repetir ") || line.startsWith("funcion ") || line.equals("sino {")) && line.endsWith("{")) nesting++;
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


    String scenePreviewStatus() {
        return scenePreview == null ? "LISTO" : (scenePreview.blocks.size() + " BLOQUES · " + scenePreview.sprites.size() + " SPRITES");
    }

    String activeProjectName() {
        return getPreferences(MODE_PRIVATE).getString("active_project", "Mi primer proyecto");
    }

    String ensureBuiltinObjectSprite(String type) throws java.io.IOException {
        String safe=type.toLowerCase(java.util.Locale.ROOT);
        String fileName="blockforge_"+safe+".png";
        java.io.File folder=new java.io.File(getFilesDir(),"sprites");
        if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException("No se pudo crear la carpeta de sprites");
        java.io.File file=new java.io.File(folder,fileName);
        if(file.exists()&&file.length()>0)return fileName;
        Bitmap bitmap=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        if("coin".equals(safe)){
            p.setColor(Color.rgb(255,205,45));canvas.drawCircle(8,8,6.5f,p);
            p.setColor(Color.rgb(255,244,150));canvas.drawCircle(8,8,3.5f,p);
        }else if("sword".equals(safe)){
            p.setColor(Color.rgb(210,225,240));canvas.drawRect(7,1,10,11,p);
            canvas.drawRect(5,3,12,5,p);p.setColor(Color.rgb(145,85,48));canvas.drawRect(7,11,10,15,p);
        }else if("stone".equals(safe)){
            p.setColor(Color.rgb(115,126,140));canvas.drawRect(3,5,13,12,p);
            canvas.drawRect(5,3,11,5,p);p.setColor(Color.rgb(80,91,105));canvas.drawRect(5,9,8,11,p);
        }else{
            p.setColor(Color.rgb(180,65,65));canvas.drawRect(4,4,12,14,p);
            canvas.drawRect(5,2,11,4,p);p.setColor(Color.WHITE);
            canvas.drawRect(6,6,7,8,p);canvas.drawRect(10,6,11,8,p);
        }
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){
            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new java.io.IOException("No se pudo guardar el sprite");
        }finally{bitmap.recycle();}
        return fileName;
    }

    boolean execute2DCommand(String line, StringBuilder output, int lineIndex) {
        try {
            ScenePreview target = scenePreview != null ? scenePreview : new ScenePreview();
            if(line.startsWith("crear_objeto2d ")){
                String[] v=line.substring(15).trim().split("\\s+");
                if(v.length!=3)throw new IllegalArgumentException("Uso: crear_objeto2d enemy|sword|coin|stone x y");
                String type=v[0].toLowerCase(java.util.Locale.ROOT);
                if(!"enemy".equals(type)&&!"sword".equals(type)&&!"coin".equals(type)&&!"stone".equals(type))
                    throw new IllegalArgumentException("Tipo de objeto no compatible");
                int x=Integer.parseInt(v[1]),y=Integer.parseInt(v[2]);
                if(x<0||x>=target.mapColumns||y<0||y>=target.mapRows)throw new IllegalArgumentException("Posición fuera del mapa");
                String fileName=ensureBuiltinObjectSprite(type);
                target.addSprite(fileName);
                SceneSprite created=target.selectedSpriteObject();
                if(created!=null){
                    created.x=x;created.y=y;created.width=1f;created.height=1f;
                    created.isPlayer=false;
                    target.persistSprites();target.invalidate();
                }
                output.append("Objeto ").append(type).append(" creado en (").append(x).append(", ").append(y).append(")\n");
                return true;
            }
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


    // Ejecuta los eventos hijos registrados por los bloques declarativos de Grafisript.
    void dispatchGrafisriptEvent(String objectType, String objectName, String eventName, int damage) {
        String project = activeProjectName();
        String prefix = "grafisript_" + objectType + "_" + project + "_" + objectName;
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String propertiesRaw = prefs.getString(prefix + "_properties", "{}");
        if (eventName.equalsIgnoreCase("al_morir") || eventName.equalsIgnoreCase("al_abrir") || eventName.equalsIgnoreCase("al_romper")) grantGrafisriptDropsFromProperties(propertiesRaw);
        String raw = prefs.getString(prefix + "_events", "{}");
        try {
            org.json.JSONObject all = new org.json.JSONObject(raw);
            org.json.JSONObject actions = all.optJSONObject(eventName.toLowerCase(java.util.Locale.ROOT));
            if (actions == null) return;
            android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            android.content.SharedPreferences.Editor edit = prefs.edit();
            for (java.util.Iterator<String> it = actions.keys(); it.hasNext();) {
                String key = it.next().toLowerCase(java.util.Locale.ROOT);
                String value = actions.optString(key, "").trim();
                if (key.equals("vida") || key.equals("salud")) {
                    int hp = prefs.getInt("character_health_" + project, 100);
                    int amount = 0;
                    try { amount = (int)evaluateGrafisriptExpression(value.replace("(", "").replace(")", ""), new java.util.HashMap<String,Double>()); }
                    catch (Exception ignored) { amount = damage; }
                    if (value.startsWith("-")) hp -= Math.abs(amount);
                    else if (value.startsWith("+")) hp += Math.abs(amount);
                    else hp = amount;
                    edit.putInt("character_health_" + project, Math.max(0, Math.min(100000, hp)));
                } else if (eventName.equals("al_saltar") && (key.equals("fuerza") || key.equals("potencia"))) {
                    try {
                        float force = Float.parseFloat(value);
                        if (scenePreview != null) {
                            SceneSprite player = scenePreview.findPlayerSprite();
                            if (player != null) player.vy = -Math.max(0.5f, Math.min(30f, force));
                        }
                    } catch (Exception ignored) { }
                } else if (key.equals("reproducir_animacion") || key.equals("animacion")) {
                    if (scenePreview != null) {
                        SceneSprite player = scenePreview.findPlayerSprite();
                        if (player != null && !player.animationFrames.isEmpty()) {
                            player.currentFrameIndex = 0;
                            player.animationPlaying = true;
                            player.lastFrameAdvance = android.os.SystemClock.uptimeMillis();
                            scenePreview.startAnimationTicker();
                        }
                    }
                }
            }
            edit.apply();
            if (scenePreview != null) { scenePreview.persistSprites(); scenePreview.invalidate(); }
        } catch (Exception ex) {
            android.util.Log.e("BlockForge", "Evento Grafisript inválido: " + eventName, ex);
        }
    }

    // Sintaxis amigable: Crear_mundo: / fondo_color = negro_con_rojo, etc.
    boolean executeEasyGrafisriptDefinition(String[] body, StringBuilder output, int lineIndex) {
        String parent = "", name = "", event = "";
        boolean readingDrops = false, dropsClosed = false;
        java.util.LinkedHashMap<String,String> props = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String,java.util.LinkedHashMap<String,String>> events = new java.util.LinkedHashMap<>();
        try {
            for (String raw : body) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.equals("?")) { if (!readingDrops) throw new IllegalArgumentException("El símbolo ? solo puede cerrar ¿Que_suelta:"); readingDrops = false; dropsClosed = true; continue; }
                if (line.startsWith("¿Que_suelta:") || line.startsWith("¿que_suelta:")) { if (readingDrops) throw new IllegalArgumentException("La tabla de botín ya está abierta"); readingDrops = true; dropsClosed = false; continue; }
                if (readingDrops) {
                    int dropEq = line.indexOf("=");
                    if (dropEq <= 0) throw new IllegalArgumentException("Usa (Objeto) = 10% dentro de ¿Que_suelta");
                    String dropName = line.substring(0, dropEq).trim().replace("(", "").replace(")", "").trim();
                    int chance = Integer.parseInt(line.substring(dropEq + 1).trim().replace("%", ""));
                    if (dropName.isEmpty() || chance < 0 || chance > 100) throw new IllegalArgumentException("Objeto o porcentaje de botín no válido (0–100%)");
                    props.put("drop:" + dropName, String.valueOf(chance));
                    continue;
                }
                int colon = line.indexOf(':');
                if (colon > 0) {
                    String h = line.substring(0,colon).trim().toLowerCase(java.util.Locale.ROOT).replace(" ","_")
                        .replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u").replace("ñ","n");
                    String v = line.substring(colon+1).trim().replace("(","").replace(")","");
                    if (h.equals("colision") || h.equals("collision")) { if (parent.isEmpty()) throw new IllegalArgumentException("Colisión necesita un objeto padre"); props.put("colision", v); continue; }
                    if (h.equals("ambiente") || h.equals("crear_ambiente")) { parent="ambiente"; name=v.isEmpty()?"Ambiente":v; event=""; }
                    else if (h.startsWith("crear_personaje") || h.equals("personaje")) { parent="personaje"; name=v.isEmpty()?"Jugador":v; event=""; }
                    else if (h.equals("enemigo") || h.startsWith("crear_enemigo")) { parent="enemigo"; name=v.isEmpty()?"Enemigo":v; event=""; }
                    else if (h.startsWith("crear_moneda") || h.startsWith("crear_dinero") || h.equals("moneda")) { parent="moneda"; name=v.isEmpty()?"Moneda":v; event=""; }
                    else if (h.startsWith("crear_cofre") || h.equals("cofre")) { parent="cofre"; name=v.isEmpty()?"Cofre":v; event=""; }
                    else if (h.startsWith("crear_arma") || h.equals("arma") || h.equals("espada")) { parent="arma"; name=v.isEmpty()?"Arma":v; event=""; }
                    else if (h.equals("crear_mundo") || h.equals("mundo")) { parent="mundo"; name="Mundo"; event=""; }
                    else if (h.startsWith("al_")) { if(parent.isEmpty()) throw new IllegalArgumentException("El evento necesita un objeto padre"); event=h; events.put(event,new java.util.LinkedHashMap<String,String>()); }
                    else throw new IllegalArgumentException("Bloque no reconocido: "+h);
                    continue;
                }
                int op=line.indexOf("+="); boolean minus=false;
                if(op<0){op=line.indexOf("-=");minus=op>=0;}
                int eq=op>=0?op:line.indexOf('=');
                if(eq<=0) throw new IllegalArgumentException("Escribe propiedad = valor: "+line);
                String key=line.substring(0,eq).trim().toLowerCase(java.util.Locale.ROOT).replace(" ","_")
                    .replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u").replace("ñ","n");
                String value=unquote(line.substring(eq+(op>=0?2:1)).trim());
                if(minus)value="-("+value+")";
                if(parent.isEmpty()) throw new IllegalArgumentException("Primero crea un objeto");
                if(event.isEmpty())props.put(key,value);else events.get(event).put(key,value);
            }
            if (readingDrops) throw new IllegalArgumentException("Falta el símbolo ? para cerrar ¿Que_suelta:");
            if(parent.isEmpty()) throw new IllegalArgumentException("Falta el comando principal, por ejemplo Crear_personaje: (Santiago)");
            String project=activeProjectName();
            String prefix="grafisript_"+parent+"_"+project+"_"+name;
            org.json.JSONObject eventJson=new org.json.JSONObject();
            for(java.util.Map.Entry<String,java.util.LinkedHashMap<String,String>> e:events.entrySet())
                eventJson.put(e.getKey(),new org.json.JSONObject(e.getValue()));
            android.content.SharedPreferences.Editor ed=getPreferences(MODE_PRIVATE).edit()
                .putString(prefix+"_properties",new org.json.JSONObject(props).toString())
                .putString(prefix+"_events",eventJson.toString()).putString(prefix+"_name",name);
            for(java.util.Map.Entry<String,String> p:props.entrySet()){
                String k=p.getKey(),v=p.getValue();
                if(parent.equals("personaje")){
                    if(k.equals("vida")||k.equals("salud"))ed.putInt("character_health_"+project,Integer.parseInt(v));
                    if(k.equals("velocidad"))ed.putFloat("character_speed_"+project,Float.parseFloat(v)*10f);
                    if(k.equals("que_tan_rapido_cae")||k.equals("velocidad_de_caida"))ed.putFloat("character_fall_speed_"+project,Float.parseFloat(v)*10f);
                    ed.putString("character_name_"+project,name);
                } else if(parent.equals("enemigo")&&(k.equals("daño")||k.equals("dano")||k.equals("danio"))) {
                    ed.putInt("enemy_damage_"+project,Integer.parseInt(v)).putString("enemy_name_"+project,name);
                } else if(parent.equals("moneda")){
                    if(k.equals("valor")||k.equals("cantidad"))ed.putInt("currency_value_"+project+"_"+name,Integer.parseInt(v));
                    if(k.equals("probabilidad_de_que_suelten")||k.equals("probabilidad")){
                        int chance=Integer.parseInt(v.replace("%","").trim());
                        if(chance<0||chance>100)throw new IllegalArgumentException("La probabilidad debe ser de 0% a 100%");
                        ed.putInt("currency_chance_"+project+"_"+name,chance);
                    }
                    if(k.equals("soltado_por")||k.equals("enemigo_que_lo_suelta")||k.equals("origen"))
                        ed.putString("currency_drop_source_"+project+"_"+name,v);
                }
            }
            ed.apply();
            if (parent.equals("ambiente")) {
                int hp = Math.max(1, Integer.parseInt(props.getOrDefault("vida", "5")));
                String collisionValue = props.getOrDefault("colision", "no");
                boolean collision = "si".equalsIgnoreCase(collisionValue) || "true".equalsIgnoreCase(collisionValue);
                java.io.File spriteFolder = new java.io.File(getFilesDir(), "sprites");
                java.io.File spriteFile = new java.io.File(spriteFolder, name);
                if (!spriteFile.isFile()) { java.io.File[] candidates=spriteFolder.listFiles(); if(candidates!=null) for(java.io.File candidate:candidates){String base=candidate.getName();int dot=base.lastIndexOf('.');if(dot>0)base=base.substring(0,dot);if(base.equalsIgnoreCase(name)){spriteFile=candidate;break;}} }
                if (scenePreview != null && spriteFile.isFile()) {
                    SceneSprite object = new SceneSprite(spriteFile.getName(), Math.max(0, scenePreview.mapColumns * 0.5f), Math.max(0, scenePreview.mapRows * 0.5f), collision);
                    object.environmentObject = true; object.environmentHealth = hp;
                    for (java.util.Map.Entry<String,String> p : props.entrySet()) if (p.getKey().startsWith("drop:")) object.environmentDrops.put(p.getKey().substring(5), Integer.parseInt(p.getValue()));
                    scenePreview.sprites.add(object); scenePreview.selectedSprite = scenePreview.sprites.size() - 1; scenePreview.persistSprites(); scenePreview.invalidate();
                    output.append("Sprite '").append(name).append("' colocado en el mapa.\\n");
                } else {
                    output.append("Código guardado; falta importar el sprite '").append(name).append("' para mostrarlo.\\n");
                }
            }
            output.append(parent).append(" '").append(name).append("' configurado: ")
                .append(props.size()).append(" propiedades, ").append(events.size()).append(" eventos hijos.\\n");
            return true;
        } catch(Exception ex) {
            output.append("Bloque desde línea ").append(lineIndex+1).append(": ")
                .append(ex.getMessage()==null?"valor no válido":ex.getMessage()).append("\\n");
            return true;
        }
    }

    boolean executeEasyGrafisriptLine(String rawLine, StringBuilder output, int lineIndex) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) return true;
        try {
            int colon = line.indexOf(':');
            if (colon >= 0) {
                String header = line.substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT)
                        .replace(" ", "_").replace("(", "").replace(")", "");
                String remainder = line.substring(colon + 1).trim();
                if (header.equals("crear_mundo")) {
                    grafisriptEasyContext = "mundo";
                    runtimeWorldName = "Mundo Grafisript";
                    if (remainder.isEmpty()) {
                        clearProjectScene();
                        output.append("Mundo creado. Configura sus propiedades debajo.\\n");
                        return true;
                    }
                } else if (header.equals("crear_personaje") || header.equals("personaje")) {
                    grafisriptEasyContext = "personaje";
                    String name = remainder.isEmpty() ? "Jugador" : unquote(remainder);
                    executeCharacterCommand("crear_personaje " + name, output, lineIndex);
                    return true;
                } else if (header.equals("enemigo")) {
                    grafisriptEasyContext = "enemigo";
                    grafisriptEnemyName = remainder.isEmpty() ? "Enemigo" : unquote(remainder);
                    output.append("Enemigo configurado: ").append(grafisriptEnemyName).append('\\n');
                    return true;
                } else if (header.equals("ese_enemigo") || header.equals("eseenemigo")) {
                    grafisriptEasyContext = "objetivo_enemigo";
                    if (remainder.isEmpty()) return true;
                } else {
                    return false;
                }
                line = remainder;
                if (line.isEmpty()) return true;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) return false;
            String key = line.substring(0, equals).trim().toLowerCase(java.util.Locale.ROOT)
                    .replace("(", "").replace(")", "").replace(" ", "_");
            String value = unquote(line.substring(equals + 1).trim());
            android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            String project = activeProjectName();
            if ("mundo".equals(grafisriptEasyContext) && (key.equals("fondo_color") || key.equals("color_fondo"))) {
                String color = value.toLowerCase(java.util.Locale.ROOT);
                int parsed;
                if (color.contains("negro") && color.contains("rojo")) parsed = Color.rgb(76, 12, 24);
                else if (color.equals("negro")) parsed = Color.BLACK;
                else if (color.equals("rojo")) parsed = Color.rgb(190, 28, 42);
                else if (color.equals("azul")) parsed = Color.rgb(25, 70, 145);
                else if (color.equals("verde")) parsed = Color.rgb(25, 105, 65);
                else if (color.equals("blanco")) parsed = Color.WHITE;
                else if (color.matches("#[0-9a-fA-F]{6}")) parsed = Color.parseColor(color);
                else throw new IllegalArgumentException("Color no reconocido. Usa negro, rojo, negro_con_rojo, azul, verde, blanco o #RRGGBB");
                prefs.edit().putInt("scene_bg_color_" + project, parsed).apply();
                if (scenePreview != null) scenePreview.invalidate();
                output.append("Fondo del mundo actualizado: ").append(value).append('\\n');
                return true;
            }
            if ("personaje".equals(grafisriptEasyContext)) {
                if (key.equals("vida") || key.equals("salud")) {
                    int hp = Integer.parseInt(value);
                    if (hp < 0 || hp > 100000) throw new IllegalArgumentException("La vida debe estar entre 0 y 100000");
                    prefs.edit().putInt("character_health_" + project, hp).apply();
                    output.append("Vida del jugador = ").append(hp).append('\\n');
                    return true;
                }
                if (key.equals("velocidad")) {
                    float speed = Float.parseFloat(value);
                    if (speed < 0 || speed > 1000) throw new IllegalArgumentException("Velocidad permitida: 0 a 1000");
                    prefs.edit().putFloat("character_speed_" + project, speed).apply();
                    output.append("Velocidad del jugador = ").append(speed).append('\\n');
                    return true;
                }
            }
            if ("enemigo".equals(grafisriptEasyContext) && (key.equals("daño") || key.equals("dano"))) {
                grafisriptEnemyDamage = Integer.parseInt(value);
                if (grafisriptEnemyDamage < 0 || grafisriptEnemyDamage > 100000)
                    throw new IllegalArgumentException("Daño permitido: 0 a 100000");
                prefs.edit().putInt("enemy_damage_" + project, grafisriptEnemyDamage)
                        .putString("enemy_name_" + project, grafisriptEnemyName).apply();
                output.append(grafisriptEnemyName).append(" · daño = ").append(grafisriptEnemyDamage).append('\\n');
                return true;
            }
            if ("objetivo_enemigo".equals(grafisriptEasyContext) &&
                    (key.equals("quitar_vida_al_jugador") || key.equals("dañar_jugador") || key.equals("danar_jugador"))) {
                int damage = Integer.parseInt(value);
                if (damage < 0 || damage > 100000) throw new IllegalArgumentException("Daño permitido: 0 a 100000");
                int hp = prefs.getInt("character_health_" + project, 100);
                hp = Math.max(0, hp - damage);
                prefs.edit().putInt("character_health_" + project, hp).apply();
                output.append(grafisriptEnemyName).append(" golpeó al jugador por ").append(damage)
                        .append(" · vida restante: ").append(hp).append('\\n');
                return true;
            }
        } catch (Exception ex) {
            output.append("Línea ").append(lineIndex + 1).append(": ")
                    .append(ex.getMessage() == null ? "valor no válido" : ex.getMessage()).append('\\n');
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
                String playerName = prefs.getString("character_name_" + project, "Jugador");
                String eventPrefix = "grafisript_personaje_" + project + "_" + playerName + "_events";
                boolean hasDamageEvent = false;
                try { hasDamageEvent = new org.json.JSONObject(prefs.getString(eventPrefix, "{}"))
                        .has("al_recibir_dano"); } catch (Exception ignored) { }
                if (!hasDamageEvent) {
                    health = Math.max(0, health - damage);
                    prefs.edit().putInt("character_health_" + project, health).apply();
                }
                dispatchGrafisriptEvent("personaje", playerName, "al_recibir_dano", damage);
                health = prefs.getInt("character_health_" + project, health);
                if (health == 0) dispatchGrafisriptEvent("personaje", playerName, "al_morir", 0);
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

    void grantGrafisriptDropsFromProperties(String rawProperties) {
        try { org.json.JSONObject props=new org.json.JSONObject(rawProperties); java.util.ArrayList<String> got=new java.util.ArrayList<>(); for(java.util.Iterator<String> it=props.keys();it.hasNext();){String key=it.next();if(!key.startsWith("drop:"))continue;int chance=Math.max(0,Math.min(100,props.optInt(key,0)));if(java.util.concurrent.ThreadLocalRandom.current().nextInt(100)<chance){String item=key.substring(5);grantGrafisriptInventoryItem(item,1);got.add(item);}} if(!got.isEmpty())Toast.makeText(this,"Botín: + "+android.text.TextUtils.join(", ",got),Toast.LENGTH_LONG).show(); } catch(Exception ignored) { }
    }

    void grantGrafisriptInventoryItem(String item, int amount) {
        if(item==null||item.trim().isEmpty()||amount<=0)return;
        String project=activeProjectName();String key="grafisript_inventory_"+project;
        try { org.json.JSONObject inventory=new org.json.JSONObject(getPreferences(MODE_PRIVATE).getString(key,"{}"));inventory.put(item,inventory.optInt(item,0)+amount);getPreferences(MODE_PRIVATE).edit().putString(key,inventory.toString()).apply(); }
        catch(Exception ignored) { }
    }

    class SceneSprite {
        String fileName;
        float x, y;
        float width = 1f, height = 1f, rotation = 0f;
        boolean solid = true;
        boolean isPlayer = false;
        boolean environmentObject = false;
        int environmentHealth = 5;
        java.util.LinkedHashMap<String,Integer> environmentDrops = new java.util.LinkedHashMap<>();
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
        Bitmap sceneBackgroundBitmap=null;
        String sceneBackgroundLoadedName="";
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
        String heldMobileAction="";
        boolean multiTouchGesture=false;
        android.view.ScaleGestureDetector scaleDetector;

        ScenePreview() {
            super(BlockForgeActivity.this);
            setBackground(shape(Color.rgb(7,13,23)));
            cameraZoom=getPreferences(MODE_PRIVATE).getFloat("scene_camera_zoom_"+activeProjectName(),getPreferences(MODE_PRIVATE).getFloat("scene_camera_zoom",1f));
            cameraOffsetX=getPreferences(MODE_PRIVATE).getFloat("scene_camera_x_"+activeProjectName(),getPreferences(MODE_PRIVATE).getFloat("scene_camera_x",0f));
            cameraOffsetY=getPreferences(MODE_PRIVATE).getFloat("scene_camera_y_"+activeProjectName(),getPreferences(MODE_PRIVATE).getFloat("scene_camera_y",0f));
            scaleDetector=new android.view.ScaleGestureDetector(BlockForgeActivity.this,new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override public boolean onScale(android.view.ScaleGestureDetector d){float old=cameraZoom;cameraZoom=Math.max(0.12f,Math.min(8f,cameraZoom*d.getScaleFactor()));float q=cameraZoom/old;cameraOffsetX=d.getFocusX()-(d.getFocusX()-cameraOffsetX)*q;cameraOffsetY=d.getFocusY()-(d.getFocusY()-cameraOffsetY)*q;saveCamera();invalidate();return true;}});
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
                    if(parts.length>14)loaded.isPlayer=Boolean.parseBoolean(parts[14]);
                    if(parts.length>15)loaded.environmentObject=Boolean.parseBoolean(parts[15]);
                    if(parts.length>16)loaded.environmentHealth=Math.max(1,Integer.parseInt(parts[16]));
                    if(parts.length>17&&!parts[17].isEmpty())for(String drop:parts[17].split(",")){int cut=drop.lastIndexOf(':');if(cut>0)loaded.environmentDrops.put(drop.substring(0,cut),Math.max(0,Math.min(100,Integer.parseInt(drop.substring(cut+1)))));}
                    loaded.x=Math.max(0f,Math.min(mapColumns-loaded.width,loaded.x));
                    loaded.y=Math.max(0f,Math.min(mapRows-loaded.height,loaded.y));
                    sprites.add(loaded);
                } catch (Exception ignored) { }
            }
            boolean hasPlayer=false;
            for(SceneSprite sprite:sprites) if(sprite.isPlayer) { hasPlayer=true; break; }
            if(!hasPlayer) for(SceneSprite sprite:sprites) {
                String n=sprite.fileName.toLowerCase(java.util.Locale.ROOT);
                if(isLikelyPlayerSprite(n)) {
                    sprite.isPlayer=true; break;
                }
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
        void triggerMobileJump(){
            int playerIndex=-1;
            for(int i=0;i<sprites.size();i++)if(sprites.get(i).isPlayer){playerIndex=i;break;}
            if(playerIndex<0){Toast.makeText(BlockForgeActivity.this,"Designa un personaje en el inspector",Toast.LENGTH_SHORT).show();return;}
            selectedSprite=playerIndex;
            SceneSprite player=sprites.get(playerIndex);
            player.vy=-9f;
            String project = activeProjectName();
            String playerName = getPreferences(MODE_PRIVATE).getString("character_name_" + project, player.fileName);
            dispatchGrafisriptEvent("personaje", playerName, "al_saltar", 0);
            if(!physicsRunning){physicsRunning=true;startPhysics();}
            invalidate();
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
            int playerIndex=-1;for(int i=0;i<sprites.size();i++)if(sprites.get(i).isPlayer){playerIndex=i;break;}
            if(playerIndex<0){Toast.makeText(BlockForgeActivity.this,"Designa un personaje en el inspector",Toast.LENGTH_SHORT).show();return;}
            selectedSprite=playerIndex;SceneSprite sp=sprites.get(playerIndex);
            switch(b.action){
                case "left":heldMobileAction="left";sp.vx=-6.5f;break;
                case "right":heldMobileAction="right";sp.vx=6.5f;break;
                case "up":heldMobileAction="up";sp.vy=-6.5f;break;
                case "down":heldMobileAction="down";sp.vy=6.5f;break;
                case "jump":heldMobileAction="";sp.vy=-9f;if(!physicsRunning){physicsRunning=true;startPhysics();}break;
            }
            if(!physicsRunning){physicsRunning=true;startPhysics();}
            invalidate();
        }
        float[] mobileButtonCenter(int index,float size){
            MobileButton b=mobileButtons.get(index);float gap=12f,bottom=getHeight()-Math.max(24f,size*.55f);
            boolean leftSide="left".equals(b.action)||"right".equals(b.action)||"up".equals(b.action)||"down".equals(b.action);
            int slot=0;for(int i=0;i<index;i++){String a=mobileButtons.get(i).action;if(("left".equals(a)||"right".equals(a)||"up".equals(a)||"down".equals(a))==leftSide)slot++;}
            float cx=leftSide?Math.max(size*.65f+12f,getWidth()*.12f+(slot%2)*(size+gap)):getWidth()-Math.max(size*.65f+12f,getWidth()*.12f)-(slot%2)*(size+gap);
            float cy=bottom-(slot/2)*(size+gap);
            if(!leftSide&&"jump".equals(b.action))cy=bottom;
            return new float[]{cx,cy};
        }
        void drawMobileButtons(Canvas canvas){
            if(!scenePlayMode)return;float size=Math.max(148f,Math.min(184f,getWidth()*0.115f));
            for(int i=0;i<mobileButtons.size();i++){MobileButton b=mobileButtons.get(i);float[] pt=mobileButtonCenter(i,size);float cx=pt[0],cy=pt[1];
                paint.setColor(Color.argb(190,25,45,72));canvas.drawCircle(cx,cy,size/2f,paint);
                paint.setColor(Color.argb(220,210,225,245));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f);canvas.drawCircle(cx,cy,size/2f,paint);paint.setStyle(Paint.Style.FILL);
                if(b.iconBitmap!=null&&!b.iconBitmap.isRecycled()){paint.setColor(Color.WHITE);canvas.drawBitmap(b.iconBitmap,null,new RectF(cx-size*.32f,cy-size*.32f,cx+size*.32f,cy+size*.32f),paint);}else{paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(size*.42f);canvas.drawText(b.icon,cx,cy+size*.14f,paint);}
            }
        }
        int hitMobileButton(float x,float y){
            float size=Math.max(148f,Math.min(184f,getWidth()*0.115f));
            for(int i=0;i<mobileButtons.size();i++){float[] pt=mobileButtonCenter(i,size);if(Math.hypot(x-pt[0],y-pt[1])<=size*0.68f)return i;}return -1;
        }

        void damageEnvironmentAt(float screenX, float screenY) {
            float wx=(screenX-cameraOffsetX)/(cameraZoom*64f);
            float wy=(screenY-cameraOffsetY)/(cameraZoom*64f);
            for(int i=sprites.size()-1;i>=0;i--){
                SceneSprite target=sprites.get(i);
                if(!target.environmentObject) continue;
                if(wx<target.x || wx>target.x+target.width || wy<target.y || wy>target.y+target.height) continue;
                target.environmentHealth=Math.max(0,target.environmentHealth-1);
                if(target.environmentHealth==0){
                    java.util.ArrayList<String> obtained=new java.util.ArrayList<>();
                    for(java.util.Map.Entry<String,Integer> drop:target.environmentDrops.entrySet()) if(java.util.concurrent.ThreadLocalRandom.current().nextInt(100)<drop.getValue()){ grantGrafisriptInventoryItem(drop.getKey(),1); obtained.add(drop.getKey()); }
                    if(target.bitmap!=null&&!target.bitmap.isRecycled())target.bitmap.recycle();
                    for(Bitmap frame:target.animationFrames)if(frame!=null&&!frame.isRecycled())frame.recycle();
                    sprites.remove(i);selectedSprite=-1;
                    Toast.makeText(BlockForgeActivity.this,obtained.isEmpty()?"Objeto roto · no soltó objetos":"Objeto roto · + "+android.text.TextUtils.join(", ",obtained),Toast.LENGTH_LONG).show();
                } else Toast.makeText(BlockForgeActivity.this,"Vida del objeto: "+target.environmentHealth,Toast.LENGTH_SHORT).show();
                persistSprites();invalidate();return;
            }
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
                        .append('|').append(sprite.width).append('|').append(sprite.height).append('|').append(sprite.rotation).append('|').append(sprite.isPlayer)
                        .append('|').append(sprite.environmentObject).append('|').append(sprite.environmentHealth).append('|');
                boolean firstDrop = true;
                for (java.util.Map.Entry<String,Integer> drop : sprite.environmentDrops.entrySet()) { if (!firstDrop) data.append(','); firstDrop = false; data.append(drop.getKey()).append(':').append(drop.getValue()); }
            }
            getPreferences(MODE_PRIVATE).edit()
                    .putString("scene_sprites_" + activeProjectName(), data.toString()).apply();
            saveProjectFile("scenes/sprites.json",data.toString());
            for(SceneSprite sprite:sprites){
                if (sprite.environmentObject) continue;
                copyProjectAsset("sprites",sprite.fileName,"assets");
                for(String frameName:sprite.animationFrameNames)copyProjectAsset("sprites",frameName,"assets");
            }
        }

        void addSprite(String name) {
            float centerX=(getWidth()*0.5f-cameraOffsetX)/(cameraZoom*64f);
            float centerY=(getHeight()*0.5f-cameraOffsetY)/(cameraZoom*64f);
            SceneSprite created = new SceneSprite(name, centerX, centerY, true);
            String project=activeProjectName();
            String targetBase=name.contains(".")?name.substring(0,name.lastIndexOf('.')):name;
            java.util.Map<String,?> saved=getPreferences(MODE_PRIVATE).getAll();
            for(String savedKey:saved.keySet()) if(savedKey.startsWith("grafisript_ambiente_"+project+"_")&&savedKey.endsWith("_name")) {
                String savedName=String.valueOf(saved.get(savedKey)); String savedBase=savedName.contains(".")?savedName.substring(0,savedName.lastIndexOf('.')):savedName;
                if(savedName.equals(name)||savedBase.equalsIgnoreCase(targetBase)) {
                    String cfgPrefix=savedKey.substring(0,savedKey.length()-5);
                    try { org.json.JSONObject cfg=new org.json.JSONObject(getPreferences(MODE_PRIVATE).getString(cfgPrefix+"_properties","{}")); created.environmentObject=true;created.environmentHealth=Math.max(1,cfg.optInt("vida",5));String col=cfg.optString("colision","no");created.solid="si".equalsIgnoreCase(col)||"true".equalsIgnoreCase(col);for(java.util.Iterator<String> it=cfg.keys();it.hasNext();){String k=it.next();if(k.startsWith("drop:"))created.environmentDrops.put(k.substring(5),Math.max(0,Math.min(100,cfg.optInt(k,0))));} } catch(Exception ignored) {}
                    break;
                }
            }
            created.vy = 0f;
            created.isPlayer = isLikelyPlayerSprite(name) || (sprites.isEmpty() && !isStaticSceneAsset(name));
            sprites.add(created);
            selectedSprite = sprites.size() - 1;
            persistSprites();
            invalidate();
        }

        SceneSprite selectedSpriteObject(){return selectedSprite>=0&&selectedSprite<sprites.size()?sprites.get(selectedSprite):null;}
        SceneSprite findPlayerSprite(){for(SceneSprite sp:sprites)if(sp.isPlayer)return sp;return null;}
        void designateSelectedAsPlayer(){
            SceneSprite chosen=selectedSpriteObject();
            if(chosen==null){Toast.makeText(BlockForgeActivity.this,"Selecciona primero el personaje",Toast.LENGTH_SHORT).show();return;}
            for(SceneSprite sprite:sprites)sprite.isPlayer=false;
            chosen.isPlayer=true;chosen.vx=0f;chosen.vy=0f;persistSprites();invalidate();
            Toast.makeText(BlockForgeActivity.this,"Personaje del juego asignado",Toast.LENGTH_SHORT).show();
        }
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
                if (!sprite.isPlayer) continue; // Plataformas, monedas y decoraciones permanecen fijas.
                float oldX = sprite.x, oldY = sprite.y;
                if(scenePlayMode && sprite==selectedSpriteObject()){
                    if("left".equals(heldMobileAction))sprite.vx=-6.5f;
                    else if("right".equals(heldMobileAction))sprite.vx=6.5f;
                    else if("up".equals(heldMobileAction))sprite.vy=-6.5f;
                    else if("down".equals(heldMobileAction))sprite.vy=6.5f;
                }
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
                if(!(scenePlayMode && sprite==selectedSpriteObject() &&
                        ("left".equals(heldMobileAction)||"right".equals(heldMobileAction)))) sprite.vx *= 0.88f;
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
            saveProjectFile("scenes/main.blocks",data.toString());
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
            copy.vx = 0f;
            copy.vy = 0f;
            copy.isPlayer = false;
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
            int playerIndex=-1;for(int i=0;i<sprites.size();i++)if(sprites.get(i).isPlayer){playerIndex=i;break;}
            if(playerIndex>=0&&playerIndex<sprites.size()){
                SceneSprite controlled=sprites.get(playerIndex);selectedSprite=playerIndex;
                if("left".equals(heldMobileAction)||"right".equals(heldMobileAction))controlled.vx=0f;
                if("up".equals(heldMobileAction)||"down".equals(heldMobileAction))controlled.vy=0f;
            }
            heldMobileAction="";
            activeMobileButtonPointer=-1;
            activeMobileTouchId=-1;
            invalidate();
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event){
            scaleDetector.onTouchEvent(event);
            int action=event.getActionMasked();
            if(scenePlayMode){
                if(action==android.view.MotionEvent.ACTION_DOWN || action==android.view.MotionEvent.ACTION_POINTER_DOWN){
                    int pointerIndex=action==android.view.MotionEvent.ACTION_DOWN?0:event.getActionIndex();
                    int hit=hitMobileButton(event.getX(pointerIndex),event.getY(pointerIndex));
                    if(hit>=0){
                        MobileButton pressed=mobileButtons.get(hit);
                        // Segundo dedo: permite saltar mientras el primero mantiene correr.
                        if("jump".equals(pressed.action) && activeMobileTouchId>=0){triggerMobileJump();return true;}
                        if(activeMobileTouchId<0 || action==android.view.MotionEvent.ACTION_DOWN){
                            activeMobileButtonPointer=hit;activeMobileTouchId=event.getPointerId(pointerIndex);runMobileButton(pressed);
                        } else if("jump".equals(pressed.action)){runMobileButton(pressed);}
                        return true;
                    }
                    if(action==android.view.MotionEvent.ACTION_DOWN) damageEnvironmentAt(event.getX(pointerIndex),event.getY(pointerIndex));
                    return true;
                }
                if(action==android.view.MotionEvent.ACTION_POINTER_UP
                        &&event.getPointerId(event.getActionIndex())==activeMobileTouchId){releaseHeldMobileButton();return true;}
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
            final float worldUnit=64f;
            float wx=(event.getX()-cameraOffsetX)/cameraZoom,wy=(event.getY()-cameraOffsetY)/cameraZoom;
            float worldX=wx/worldUnit,worldY=wy/worldUnit;
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                draggingSprite=-1;
                for(int i=sprites.size()-1;i>=0;i--){
                    SceneSprite sprite=sprites.get(i);
                    if(worldX>=sprite.x&&worldX<=sprite.x+sprite.width&&worldY>=sprite.y&&worldY<=sprite.y+sprite.height){
                        if(sceneEraseMode){
                            if(sprite.bitmap!=null)sprite.bitmap.recycle();
                            for(Bitmap frame:sprite.animationFrames)if(frame!=null)frame.recycle();
                            sprites.remove(i);selectedSprite=-1;persistSprites();
                        }else{
                            draggingSprite=i;selectedSprite=i;
                            spriteDragOffsetX=worldX-sprite.x;spriteDragOffsetY=worldY-sprite.y;
                        }
                        break;
                    }
                }
                if(draggingSprite<0){selectedSprite=-1;lastPanX=event.getX();lastPanY=event.getY();}
                invalidate();return true;
            }
            if(event.getAction()==android.view.MotionEvent.ACTION_MOVE&&draggingSprite>=0){
                SceneSprite sprite=sprites.get(draggingSprite);
                sprite.x=worldX-spriteDragOffsetX;sprite.y=worldY-spriteDragOffsetY;
                invalidate();return true;
            }
            if(event.getAction()==android.view.MotionEvent.ACTION_MOVE&&draggingSprite<0){
                cameraOffsetX+=event.getX()-lastPanX;cameraOffsetY+=event.getY()-lastPanY;
                lastPanX=event.getX();lastPanY=event.getY();saveCamera();invalidate();return true;
            }
            if(event.getAction()==android.view.MotionEvent.ACTION_UP){
                if(draggingSprite>=0)persistSprites();
                draggingSprite=-1;performClick();return true;
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
            canvas.drawColor(getPreferences(MODE_PRIVATE).getInt("scene_bg_color_"+activeProjectName(),Color.rgb(24,32,43)));
            String bgName=getPreferences(MODE_PRIVATE).getString("scene_bg_image_"+activeProjectName(),"");
            if(!bgName.equals(sceneBackgroundLoadedName)){
                if(sceneBackgroundBitmap!=null&&!sceneBackgroundBitmap.isRecycled())sceneBackgroundBitmap.recycle();
                sceneBackgroundBitmap=null;sceneBackgroundLoadedName=bgName;
                if(!bgName.isEmpty()){
                    java.io.File bgFile=new java.io.File(new java.io.File(getFilesDir(),"backgrounds"),bgName);
                    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(bgFile.getAbsolutePath(),bounds);
                    int sample=1,targetW=Math.max(1,getWidth()*2),targetH=Math.max(1,getHeight()*2);
                    while(bounds.outWidth>0&&bounds.outHeight>0&&(bounds.outWidth/sample>targetW*2||bounds.outHeight/sample>targetH*2))sample*=2;
                    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=sample;opts.inPreferredConfig=Bitmap.Config.RGB_565;
                    try{sceneBackgroundBitmap=BitmapFactory.decodeFile(bgFile.getAbsolutePath(),opts);}catch(OutOfMemoryError oom){opts.inSampleSize=Math.max(2,sample*2);try{sceneBackgroundBitmap=BitmapFactory.decodeFile(bgFile.getAbsolutePath(),opts);}catch(OutOfMemoryError ignored){}}
                }
            }
            if(sceneBackgroundBitmap!=null&&!sceneBackgroundBitmap.isRecycled()){paint.setFilterBitmap(false);canvas.drawBitmap(sceneBackgroundBitmap,null,new RectF(0,0,getWidth(),getHeight()),paint);}
            canvas.save();canvas.translate(cameraOffsetX,cameraOffsetY);canvas.scale(cameraZoom,cameraZoom);
            final float worldUnit=64f;
            float visibleLeft=-cameraOffsetX/(cameraZoom*worldUnit);
            float visibleTop=-cameraOffsetY/(cameraZoom*worldUnit);
            float visibleRight=visibleLeft+getWidth()/(cameraZoom*worldUnit);
            float visibleBottom=visibleTop+getHeight()/(cameraZoom*worldUnit);
            if(sceneGridVisible&&!scenePlayMode){
                paint.setColor(Color.rgb(31,43,60));paint.setStrokeWidth(1f);
                int startX=(int)Math.floor(visibleLeft),endX=(int)Math.ceil(visibleRight);
                int startY=(int)Math.floor(visibleTop),endY=(int)Math.ceil(visibleBottom);
                for(int x=startX;x<=endX;x++)canvas.drawLine(x*worldUnit,visibleTop*worldUnit,x*worldUnit,visibleBottom*worldUnit,paint);
                for(int y=startY;y<=endY;y++)canvas.drawLine(visibleLeft*worldUnit,y*worldUnit,visibleRight*worldUnit,y*worldUnit,paint);
                paint.setColor(Color.rgb(95,65,80));paint.setStrokeWidth(2f);canvas.drawLine(visibleLeft*worldUnit,0,visibleRight*worldUnit,0,paint);
                paint.setColor(Color.rgb(65,100,75));canvas.drawLine(0,visibleTop*worldUnit,0,visibleBottom*worldUnit,paint);
            }
            paint.setColor(Color.rgb(115,130,150));paint.setTextSize(12f);
            canvas.drawText("WORLD 2D  ·  "+(int)(cameraZoom*100)+"%",12-visibleLeft*worldUnit,24-visibleTop*worldUnit,paint);
            for(SceneSprite sprite:sprites){
                float left=sprite.x*worldUnit,top=sprite.y*worldUnit;
                float right=left+worldUnit*sprite.width,bottom=top+worldUnit*sprite.height;
                float centerX = (left + right) * 0.5f, centerY = (top + bottom) * 0.5f;
                boolean shadows=getPreferences(MODE_PRIVATE).getBoolean("scene_shadows_enabled_"+activeProjectName(),true);
                if(shadows){
                    int alpha=Math.max(0,Math.min(180,getPreferences(MODE_PRIVATE).getInt("scene_shadow_opacity_"+activeProjectName(),90)));
                    paint.setColor(Color.argb(alpha,0,0,0));
                    paint.setStyle(Paint.Style.FILL);
                    float shadowY=bottom-worldUnit*0.025f;
                    float shadowWidth=Math.max(worldUnit*0.18f,(right-left)*0.72f);
                    float shadowHeight=Math.max(3f,worldUnit*0.10f);
                    canvas.drawOval(centerX-shadowWidth/2f,shadowY-shadowHeight/2f,centerX+shadowWidth/2f,shadowY+shadowHeight/2f,paint);
                }
                canvas.save();
                canvas.rotate(sprite.rotation, centerX, centerY);
                Bitmap shown=sprite.bitmap;if(!sprite.animationFrames.isEmpty()){int frame=Math.max(0,Math.min(sprite.currentFrameIndex,sprite.animationFrames.size()-1));shown=sprite.animationFrames.get(frame);}
                if(sprite.environmentObject && shown!=null){paint.setFilterBitmap(false);canvas.drawBitmap(shown,null,new RectF(left,top,right,bottom),paint);
                } else if(sprite.environmentObject){
                    paint.setColor(Color.rgb(105,68,38));canvas.drawRoundRect(new RectF(centerX-worldUnit*.10f,top+worldUnit*.42f,centerX+worldUnit*.10f,bottom),worldUnit*.04f,worldUnit*.04f,paint);
                    paint.setColor(Color.rgb(42,125,62));canvas.drawCircle(centerX,top+worldUnit*.28f,worldUnit*.30f,paint);
                    paint.setColor(Color.rgb(64,155,72));canvas.drawCircle(centerX-worldUnit*.13f,top+worldUnit*.34f,worldUnit*.19f,paint);
                    paint.setColor(Color.rgb(80,170,78));canvas.drawCircle(centerX+worldUnit*.13f,top+worldUnit*.34f,worldUnit*.18f,paint);
                } else if(shown!=null){paint.setFilterBitmap(false);canvas.drawBitmap(shown,null,new RectF(left,top,right,bottom),paint);
                } else {
                    paint.setColor(Color.MAGENTA);
                    canvas.drawRect(left, top, right, bottom, paint);
                }
                if (sprite.environmentObject && scenePlayMode) {paint.setColor(Color.WHITE);paint.setTextSize(10f);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("HP "+sprite.environmentHealth,centerX,top-5f,paint);}
                if (!scenePlayMode && sprites.indexOf(sprite)==selectedSprite) {paint.setColor(Color.YELLOW);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3f);canvas.drawRect(left-2,top-2,right+2,bottom+2,paint);paint.setStyle(Paint.Style.FILL);}
                if (sprite.solid) {
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(left, top, right, bottom, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                canvas.restore();
            }


            paint.setColor(white);
            paint.setTextSize(12);
            String active = activeProjectName();
            String character = getPreferences(MODE_PRIVATE)
                    .getString("character_name_" + active, "");
            if (!character.isEmpty()) {
                float px = getPreferences(MODE_PRIVATE).getFloat("character_x_" + active, 0f);
                float pz = getPreferences(MODE_PRIVATE).getFloat("character_z_" + active, 0f);
                float centerX = px * worldUnit;
                float centerY = pz * worldUnit;
                paint.setColor(Color.rgb(65, 145, 255));
                canvas.drawCircle(centerX, centerY, worldUnit * 0.32f, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(10);
                canvas.drawText(character, centerX - 18, centerY - 12, paint);
            }

            canvas.drawText("ESCENA 2D · "+sprites.size()+" OBJETOS · "+Math.round(cameraZoom*100)+"%",12,20,paint);
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
        if(requestCode==PICK_BACKGROUND_REQUEST){
            try{String name=copyUriToFolder(data.getData(),"backgrounds","background");getPreferences(MODE_PRIVATE).edit().putString("scene_bg_image_"+activeProjectName(),name).apply();copyProjectAsset("backgrounds",name,"backgrounds");if(scenePreview!=null){if(scenePreview.sceneBackgroundBitmap!=null&&!scenePreview.sceneBackgroundBitmap.isRecycled())scenePreview.sceneBackgroundBitmap.recycle();scenePreview.sceneBackgroundBitmap=null;scenePreview.sceneBackgroundLoadedName="";scenePreview.invalidate();}Toast.makeText(this,"Fondo importado",Toast.LENGTH_SHORT).show();openPage(2);}
            catch(Exception ex){Toast.makeText(this,"No se pudo importar fondo: "+ex.getMessage(),Toast.LENGTH_LONG).show();}return;
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


    void startSceneGame() {
        try {
            if (scenePreview == null) scenePreview = new ScenePreview();
            if (scenePreview.sprites.isEmpty() && scenePreview.blocks.isEmpty()) {
                Toast.makeText(this,"Añade al menos un sprite u objeto antes de ejecutar la escena",Toast.LENGTH_LONG).show();
                return;
            }
            boolean hasPlayer=false;
            for(SceneSprite sprite:scenePreview.sprites) if(sprite.isPlayer){hasPlayer=true;break;}
            if(!hasPlayer){
                Toast.makeText(this,"Designa el personaje desde Inspector → DESIGNAR COMO PERSONAJE. Los demás sprites quedarán fijos.",Toast.LENGTH_LONG).show();
                return;
            }
            for(int i=0;i<scenePreview.sprites.size();i++)if(scenePreview.sprites.get(i).isPlayer){scenePreview.selectedSprite=i;break;}
            // Cambiar primero al viewport de juego; no ejecutar física sobre una vista aún desmontada.
            scenePlayMode = true;
            if (scenePreview.selectedSprite < 0 && !scenePreview.sprites.isEmpty())
                scenePreview.selectedSprite = 0;
            if (scenePreview.mobileButtons.isEmpty())
                scenePreview.installDefaultMobileButtons();
            physicsRunning = true;
            openPage(2);
        } catch (RuntimeException ex) {
            scenePlayMode = false;
            physicsRunning = false;
            if (scenePreview != null) {
                try { scenePreview.stopPhysics(); } catch (RuntimeException ignored) { }
            }
            android.util.Log.e("BlockForge", "No se pudo iniciar Play Mode", ex);
            Toast.makeText(this, "No se pudo iniciar el juego: " +
                    (ex.getMessage() == null ? "error interno" : ex.getMessage()),
                    Toast.LENGTH_LONG).show();
            try { openPage(2); } catch (RuntimeException ignored) { }
        }
    }

    void stopSceneGame() {
        scenePlayMode=false;
        physicsRunning=false;
        if(scenePreview!=null){scenePreview.stopPhysics();scenePreview.persistSprites();}
        openPage(2);
    }

    void openSpriteDrawer() {
        final String[] options={"🎮  Sprites predeterminados","🎨  Dibujar mi sprite","📁  Importar desde archivos","🧪  Crear sprite con efectos"};
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle("CREAR SPRITE · PIXEL STUDIO")
            .setItems(options,(d,which)->{
                if(which==0)createBuiltinSpritePicker();
                else if(which==1)openSpritePixelEditor();
                else if(which==2)pickSpriteImage();
                else openSpritePixelEditor();
            }).setNegativeButton("CANCELAR",null).create();
        dialog.setOnShowListener(v->{if(dialog.getWindow()!=null){dialog.getWindow().setLayout(-1,-1);dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);}});
        dialog.show();
    }

    String importSpriteCookAsset(String assetName) throws java.io.IOException {
        java.io.File dir = new java.io.File(getFilesDir(), "sprites");
        if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("No se pudo crear la carpeta de sprites");
        String fileName = "spritecook_" + assetName + ".webp";
        java.io.File target = new java.io.File(dir, fileName);
        if (!target.exists()) {
            try (java.io.InputStream in = getAssets().open("spritecook/" + assetName + ".webp");
                 java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                out.flush();
            }
        }
        return fileName;
    }

    void createBuiltinSpritePicker(){
        final String[] names={"Personaje aventurero","Slime","Moneda","Plataforma","Árbol","Corazón","Caja","Robot","Murciélago","Esqueleto","Gema","Estrella","Poción","Cofre","Portal","Flor",
            "⭐ SpriteCook · Astronauta ardilla","SpriteCook · Hámster","SpriteCook · Loro","SpriteCook · Pingüino",
            "SpriteCook · Perro","SpriteCook · Gato naranja","SpriteCook · Goblin","SpriteCook · Caballero"};
        final String[] spriteCookFiles={"astronaut-squirrel","chunky-hamster","chunky-parrot","cute-penguin",
            "friendly-brown-dog","ginger-cat","royal-goblin","paladin-knight"};
        new android.app.AlertDialog.Builder(this).setTitle("BIBLIOTECA DE SPRITES · 24 ELEMENTOS")
            .setItems(names,(d,which)->{
                try{
                    String file = which < 16 ? createBuiltinSprite(which) : importSpriteCookAsset(spriteCookFiles[which-16]);
                    if(scenePreview==null)scenePreview=new ScenePreview();
                    scenePreview.addSprite(file);
                    Toast.makeText(this,"Sprite añadido: "+names[which],Toast.LENGTH_SHORT).show();
                    openPage(2);
                } catch(Exception e){
                    android.util.Log.e("BlockForge","No se pudo importar sprite",e);
                    Toast.makeText(this,"No se pudo importar el sprite",Toast.LENGTH_LONG).show();
                }
            }).setNegativeButton("VOLVER",null).show();
    }

    String createBuiltinSprite(int kind) throws java.io.IOException {
        int n=32;Bitmap bitmap=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);Paint p=new Paint();
        c.drawColor(Color.TRANSPARENT,android.graphics.PorterDuff.Mode.CLEAR);
        int[] palette={Color.rgb(65,145,255),Color.rgb(80,205,125),Color.rgb(255,205,45),Color.rgb(105,115,135),Color.rgb(55,155,75),Color.rgb(235,65,85),Color.rgb(175,115,65),Color.rgb(45,190,205),Color.rgb(105,65,170),Color.rgb(220,220,225),Color.rgb(55,210,220),Color.rgb(255,190,45),Color.rgb(180,70,210),Color.rgb(180,110,55),Color.rgb(85,75,220),Color.rgb(245,110,160)};
        int main=palette[Math.max(0,Math.min(palette.length-1,kind))];
        p.setColor(main);
        if(kind==2){c.drawCircle(16,16,12,p);p.setColor(Color.rgb(255,245,160));c.drawCircle(16,16,8,p);}
        else if(kind==5){c.drawCircle(10,12,7,p);c.drawCircle(22,12,7,p);c.drawRect(4,12,28,25,p);}
        else if(kind==3||kind==6||kind==13){c.drawRect(2,8,30,30,p);p.setColor(Color.rgb(35,45,58));c.drawRect(5,11,27,27,p);p.setColor(main);c.drawRect(8,14,24,24,p);if(kind==13){p.setColor(Color.rgb(255,210,80));c.drawRect(13,14,19,23,p);}}
        else if(kind==4||kind==15){if(kind==4){c.drawRect(13,17,19,31,p);c.drawCircle(16,11,11,p);}else{c.drawRect(14,12,18,31,p);c.drawCircle(16,9,7,p);c.drawCircle(8,17,6,p);c.drawCircle(24,17,6,p);}}
        else if(kind==8){c.drawOval(new RectF(5,8,27,25),p);p.setColor(Color.WHITE);c.drawCircle(12,15,2,p);c.drawCircle(21,15,2,p);p.setColor(main);c.drawRect(2,4,9,12,p);c.drawRect(23,4,30,12,p);}
        else if(kind==9){c.drawRect(7,5,25,29,p);p.setColor(Color.WHITE);c.drawRect(10,11,13,14,p);c.drawRect(19,11,22,14,p);c.drawRect(12,22,20,24,p);}
        else if(kind==10){Path path=new Path();path.moveTo(16,2);path.lineTo(29,16);path.lineTo(16,30);path.lineTo(3,16);path.close();c.drawPath(path,p);p.setColor(Color.WHITE);c.drawLine(16,5,16,27,p);c.drawLine(5,16,27,16,p);}
        else if(kind==11){Path star=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float radius=i%2==0?14:6;float x=16+(float)Math.cos(angle)*radius,y=16+(float)Math.sin(angle)*radius;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();c.drawPath(star,p);}
        else if(kind==12){c.drawRoundRect(new RectF(10,8,22,29),4,4,p);p.setColor(Color.rgb(245,235,255));c.drawRect(12,4,20,9,p);p.setColor(Color.WHITE);c.drawRect(13,17,19,22,p);}
        else if(kind==14){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);c.drawCircle(16,16,11,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawCircle(16,16,4,p);}
        else if(kind==7){c.drawRoundRect(new RectF(5,5,27,27),4,4,p);p.setColor(Color.WHITE);c.drawRect(9,11,13,15,p);c.drawRect(19,11,23,15,p);p.setColor(Color.DKGRAY);c.drawRect(12,21,20,23,p);}
        else {c.drawRoundRect(new RectF(5,4,27,30),5,5,p);p.setColor(Color.WHITE);c.drawRect(10,12,13,15,p);c.drawRect(20,12,23,15,p);p.setColor(Color.rgb(35,35,45));c.drawRect(12,22,20,24,p);}
        java.io.File folder=new java.io.File(getFilesDir(),"sprites");if(!folder.exists())folder.mkdirs();
        String file="builtin_"+kind+"_"+System.currentTimeMillis()+".png";
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,file))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}
        return file;
    }

    void openSpritePixelEditor() {
        LinearLayout wrap = vertical();
        wrap.setPadding(14, 12, 14, 12);
        wrap.setBackgroundColor(Color.rgb(20, 24, 31));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView sizeLabel = uiLabel("TAMAÑO DEL LIENZO", 10, uiMuted, true);
        top.addView(sizeLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Spinner sizePicker = new Spinner(this);
        String[] sizes = {"16 × 16", "32 × 32", "64 × 64", "128 × 128"};
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
                drawing.setSize(position == 0 ? 16 : position == 1 ? 32 : position == 2 ? 64 : 128);
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
        tools.addView(editorButton("SHADERS", () -> new android.app.AlertDialog.Builder(this).setTitle("Efectos de color").setItems(new String[]{"Original","Escala de grises","Sepia","Invertir colores","Neón"},(d,w)->drawing.applyEffect(w)).show()), new LinearLayout.LayoutParams(0,44,1));
        wrap.addView(tools);

        HorizontalScrollView paletteScroll = new HorizontalScrollView(this);
        paletteScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colors = new LinearLayout(this);
        colors.setPadding(0, 9, 0, 9);
        int[] palette = {Color.BLACK,Color.WHITE,Color.RED,Color.rgb(255,110,110),Color.rgb(145,25,45),Color.rgb(255,125,35),Color.rgb(255,195,45),Color.YELLOW,Color.rgb(155,210,55),Color.GREEN,Color.rgb(40,175,130),Color.CYAN,Color.rgb(50,145,220),Color.BLUE,Color.rgb(90,80,190),Color.MAGENTA,Color.rgb(225,125,190),Color.rgb(150,95,55),Color.rgb(90,55,35),Color.LTGRAY,Color.GRAY,Color.DKGRAY};
        String[] labels = {"NEGRO","BLANCO","ROJO","CORAL","VINO","NARANJA","AMBAR","AMARILLO","LIMA","VERDE","MENTA","CIAN","CELESTE","AZUL","VIOLETA","MAGENTA","ROSA","TIERRA","MARRON","PLATA","GRIS","CARBON"};
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
            dialog.getWindow().setLayout(-1, -1);
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
        void applyEffect(int effect){
            snapshot();
            for(int i=0;i<pixels.length;i++){
                int c=pixels[i];if(Color.alpha(c)==0)continue;
                int r=Color.red(c),g=Color.green(c),b=Color.blue(c);
                if(effect==1){int v=(int)(.299*r+.587*g+.114*b);r=g=b=v;}
                else if(effect==2){int nr=(int)(.393*r+.769*g+.189*b),ng=(int)(.349*r+.686*g+.168*b),nb=(int)(.272*r+.534*g+.131*b);r=Math.min(255,nr);g=Math.min(255,ng);b=Math.min(255,nb);}
                else if(effect==3){r=255-r;g=255-g;b=255-b;}
                else if(effect==4){r=Math.min(255,(int)(r*1.35+35));g=Math.min(255,(int)(g*1.25+20));b=Math.min(255,(int)(b*1.45+45));}
                pixels[i]=Color.argb(Color.alpha(c),r,g,b);
            }
            invalidate();
        }
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

    long folderBytes(java.io.File folder) {
        if (folder == null || !folder.exists()) return 0L;
        if (folder.isFile()) return folder.length();
        long total = 0L;
        java.io.File[] children = folder.listFiles();
        if (children != null) for (java.io.File child : children) total += folderBytes(child);
        return total;
    }

    String readableBytes(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    void showMemorySettings() {
        final int[] choices = {128, 256, 384, 512, 768};
        String[] labels = {"128 MB", "256 MB", "384 MB", "512 MB", "768 MB"};
        int current = getPreferences(MODE_PRIVATE).getInt("memory_budget_mb", 256);
        int selected = 1;
        for (int i=0;i<choices.length;i++) if (choices[i] == current) selected = i;
        new android.app.AlertDialog.Builder(this)
            .setTitle(uiText("Presupuesto de memoria","Memory budget"))
            .setMessage(uiText("Preferencia para limitar cachés y recursos temporales. Android sigue administrando la RAM total del proceso; esto no fija un límite real del sistema.","Preference for cache and temporary resources. Android still manages total process RAM; this does not set a system-enforced limit."))
            .setSingleChoiceItems(labels, selected, (d, which) -> {
                getPreferences(MODE_PRIVATE).edit().putInt("memory_budget_mb", choices[which]).apply();
                d.dismiss(); settings();
            }).setNegativeButton(uiText("Cerrar","Close"), null).show();
    }

    void showStorageManager(java.io.File folder) {
        try {
            if (folder == null || !folder.getCanonicalPath().startsWith(getFilesDir().getCanonicalPath() + java.io.File.separator) && !folder.equals(getFilesDir())) {
                Toast.makeText(this, uiText("Carpeta no permitida","Folder not allowed"), Toast.LENGTH_SHORT).show(); return;
            }
        } catch (java.io.IOException ex) {
            Toast.makeText(this, uiText("No se pudo abrir la carpeta","Could not open folder"), Toast.LENGTH_SHORT).show(); return;
        }
        java.io.File[] entries = folder.listFiles();
        java.util.Arrays.sort(entries == null ? new java.io.File[0] : entries, (a,b) -> {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        java.util.ArrayList<java.io.File> files = new java.util.ArrayList<>();
        if (!folder.equals(getFilesDir())) files.add(folder.getParentFile());
        if (entries != null) for (java.io.File entry : entries) files.add(entry);
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (java.io.File entry : files) names.add(entry == folder.getParentFile() && !folder.equals(getFilesDir()) ? "⬆  .." :
            (entry.isDirectory() ? "📁  " : "📄  ") + entry.getName() + (entry.isDirectory() ? "" : "  ·  " + readableBytes(entry.length())));
        String path = folder.getAbsolutePath().replace(getFilesDir().getAbsolutePath(), "BlockForge");
        new android.app.AlertDialog.Builder(this).setTitle(uiText("Archivos · ","Files · ") + path)
            .setItems(names.toArray(new String[0]), (d, which) -> {
                java.io.File chosen = files.get(which);
                if (chosen.isDirectory()) showStorageManager(chosen);
                else new android.app.AlertDialog.Builder(this).setTitle(chosen.getName())
                    .setMessage(readableBytes(chosen.length()))
                    .setPositiveButton(uiText("Eliminar","Delete"), (dd,ww) -> {
                        if (chosen.delete()) Toast.makeText(this,uiText("Archivo eliminado","File deleted"),Toast.LENGTH_SHORT).show();
                        else Toast.makeText(this,uiText("No se pudo eliminar","Could not delete file"),Toast.LENGTH_SHORT).show();
                        showStorageManager(folder);
                    }).setNegativeButton(uiText("Cerrar","Close"),null).show();
            })
            .setNeutralButton(uiText("Nueva carpeta","New folder"), (d,w) -> {
                EditText name = new EditText(this); name.setSingleLine(true);
                new android.app.AlertDialog.Builder(this).setTitle(uiText("Nombre de carpeta","Folder name")).setView(name)
                    .setPositiveButton(uiText("Crear","Create"),(dd,ww)->{
                        String value=name.getText().toString().trim();
                        if (!value.isEmpty() && value.matches("[a-zA-Z0-9 _-]{1,48}")) {
                            java.io.File created=new java.io.File(folder,value);
                            if(created.mkdir()) Toast.makeText(this,uiText("Carpeta creada","Folder created"),Toast.LENGTH_SHORT).show();
                        } else Toast.makeText(this,uiText("Nombre no válido","Invalid name"),Toast.LENGTH_SHORT).show();
                        showStorageManager(folder);
                    }).setNegativeButton(uiText("Cancelar","Cancel"),null).show();
            }).setNegativeButton(uiText("Cerrar","Close"),null).show();
    }

    void showEditorFontSizeDialog() {
        final int[] sizes = {12, 14, 16, 18, 20};
        String[] labels = {"12 sp", "14 sp", "16 sp", "18 sp", "20 sp"};
        int current = getPreferences(MODE_PRIVATE).getInt("editor_font_size", 14);
        int selected = 1;
        for (int i = 0; i < sizes.length; i++) if (sizes[i] == current) selected = i;
        new android.app.AlertDialog.Builder(this)
            .setTitle(uiText("Tamaño del texto del editor", "Editor text size"))
            .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                getPreferences(MODE_PRIVATE).edit().putInt("editor_font_size", sizes[which]).apply();
                dialog.dismiss();
                Toast.makeText(this, uiText("Tamaño guardado. Se aplicará al abrir el editor.", "Size saved. It will apply when the editor opens."), Toast.LENGTH_SHORT).show();
                settings();
            }).setNegativeButton(uiText("Cancelar", "Cancel"), null).show();
    }

    void clearTemporaryCache() {
        new android.app.AlertDialog.Builder(this)
            .setTitle(uiText("Limpiar caché temporal", "Clear temporary cache"))
            .setMessage(uiText("Se eliminarán archivos temporales y cachés. Tus proyectos y scripts no se borrarán.", "Temporary files and cache will be deleted. Your projects and scripts will not be removed."))
            .setNegativeButton(uiText("Cancelar", "Cancel"), null)
            .setPositiveButton(uiText("Limpiar", "Clear"), (dialog, which) -> {
                long before = folderBytes(getCacheDir());
                java.io.File[] children = getCacheDir().listFiles();
                if (children != null) for (java.io.File child : children) deleteCacheEntry(child);
                Toast.makeText(this, uiText("Caché limpiada: ", "Cache cleared: ") + readableBytes(before), Toast.LENGTH_LONG).show();
                settings();
            }).show();
    }

    boolean deleteCacheEntry(java.io.File entry) {
        if (entry == null || !entry.exists()) return true;
        if (entry.isDirectory()) {
            java.io.File[] children = entry.listFiles();
            if (children != null) for (java.io.File child : children) deleteCacheEntry(child);
        }
        return entry.delete();
    }

    void settings() {
        content.setPadding(20, 20, 20, 24);
        content.setOrientation(LinearLayout.VERTICAL);
        heading(uiText("Configuración","Settings"), uiText("Personaliza BlockForge Studio.","Customize BlockForge Studio."));
        uiSection(uiText("APLICACIÓN","APP"), uiText("PREFERENCIAS","PREFERENCES"));
        content.addView(uiSecondary(uiText("🌐  Idioma · Español / English","🌐  Language · Spanish / English"), () -> showLanguagePicker()));
        String lang=getPreferences(MODE_PRIVATE).getString("ui_language","es");
        card(uiText("Idioma activo","Active language"),"en".equals(lang)?"English":"Español");
        content.addView(uiSecondary(uiText("🎨  Cambiar tema claro / oscuro","🎨  Switch light / dark theme"), () -> {
            boolean light=getPreferences(MODE_PRIVATE).getBoolean("ui_light_theme",false);
            getPreferences(MODE_PRIVATE).edit().putBoolean("ui_light_theme",!light).apply();
            applyUiTheme(); buildInterface(); openPage(5);
        }));
        card(uiText("Tema actual","Current theme"),getPreferences(MODE_PRIVATE).getBoolean("ui_light_theme",false)?uiText("Claro","Light"):uiText("Oscuro","Dark"));
        uiSection(uiText("EDITOR Y COMODIDAD","EDITOR & COMFORT"), uiText("PREFERENCIAS DE EDICIÓN","EDITING PREFERENCES"));
        int fontSize=getPreferences(MODE_PRIVATE).getInt("editor_font_size",14);
        content.addView(uiSecondary(uiText("Aa  Tamaño del texto · ","Aa  Editor text size · ")+fontSize+" sp", () -> showEditorFontSizeDialog()));
        boolean haptics=getPreferences(MODE_PRIVATE).getBoolean("ui_haptics",true);
        content.addView(uiSecondary(uiText("Vibración al tocar · ","Touch vibration · ")+(haptics?uiText("Activada","On"):uiText("Desactivada","Off")), () -> {
            boolean next=!getPreferences(MODE_PRIVATE).getBoolean("ui_haptics",true);
            getPreferences(MODE_PRIVATE).edit().putBoolean("ui_haptics",next).apply();
            settings();
        }));
        card(uiText("Guardado de scripts","Script saving"),uiText("El editor guarda al cambiar de pantalla y al salir de la app.","The editor saves when changing screens and when leaving the app."));

        uiSection(uiText("RENDIMIENTO","PERFORMANCE"), uiText("MEMORIA","MEMORY"));
        android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo memoryInfo=new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        long usedHeap=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long maxHeap=Runtime.getRuntime().maxMemory();
        int budget=getPreferences(MODE_PRIVATE).getInt("memory_budget_mb",256);
        card(uiText("RAM de la app","App RAM"),uiText("Montón usado: ","Heap used: ")+readableBytes(usedHeap)+" · "+uiText("máximo Java: ","Java max: ")+readableBytes(maxHeap));
        card(uiText("RAM del dispositivo","Device memory"),uiText("Disponible: ","Available: ")+readableBytes(memoryInfo.availMem)+" · "+uiText("presupuesto elegido: ","Selected budget: ")+budget+" MB");
        content.addView(uiSecondary(uiText("Ajustar presupuesto de caché","Adjust cache budget"), () -> showMemorySettings()));

        uiSection(uiText("ALMACENAMIENTO","STORAGE"), uiText("DATOS LOCALES","LOCAL DATA"));
        long appBytes=folderBytes(getFilesDir())+folderBytes(getCacheDir());
        java.io.File storageRoot=getFilesDir();
        card(uiText("Espacio usado por BlockForge","BlockForge storage used"),readableBytes(appBytes));
        card(uiText("Almacenamiento del dispositivo","Device storage"),uiText("Libre: ","Free: ")+readableBytes(storageRoot.getUsableSpace())+" · "+uiText("Total: ","Total: ")+readableBytes(storageRoot.getTotalSpace()));
        content.addView(uiPrimary(uiText("📁  ADMINISTRAR CARPETAS","📁  MANAGE FOLDERS"), () -> showStorageManager(getFilesDir())));
        content.addView(uiSecondary(uiText("🧹  Limpiar caché temporal","🧹  Clear temporary cache"), () -> clearTemporaryCache()));

        uiSection(uiText("ACERCA DE","ABOUT"), uiText("DETALLES","DETAILS"));
        card(uiText("Lenguaje de programación","Programming language"),"El Grafisript");
        card(uiText("Motor gráfico","Graphics engine"),"C++ / SDL2 · OpenGL ES");
        String version="Experimental";
        try { version=getPackageManager().getPackageInfo(getPackageName(),0).versionName; } catch(Exception ignored) { }
        card(uiText("Aplicación","Application"),"BlockForge Studio Mobile · "+version);
        card(uiText("Directorio privado","Private app folder"),getFilesDir().getAbsolutePath());
    }

}