
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

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        buildInterface();
        home();
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

        String[] names = {"Inicio","Proyectos","Escenas","Grafisript","Ajustes"};
        String[] icons = {"⌂","▦","◇","{ }","⚙"};

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
        content.removeAllViews();
        switch(page) {
            case 0: home(); break;
            case 1: projects(); break;
            case 2: scenes(); break;
            case 3: grafisript(); break;
            case 4: settings(); break;
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
                "Gestiona tus creaciones.");

        content.addView(button("＋ CREAR PROYECTO",
                () -> newProject()));

        card("Mi primer videojuego",
                "Proyecto de demostración.");

        card("Mundo de bloques",
                "Proyecto de construcción experimental.");

        card("Proyecto vacío",
                "Comienza una creación desde cero.");
    }

    void newProject() {
        EditText input=new EditText(this);
        input.setHint("Nombre del proyecto");
        new android.app.AlertDialog.Builder(this)
            .setTitle("Nuevo proyecto")
            .setView(input)
            .setPositiveButton("Crear",(d,w)->{
                Toast.makeText(this,
                    "Proyecto preparado: "+input.getText(),
                    Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Cancelar",null)
            .show();
    }

    void scenes() {
        heading("Editor de escenas",
                "Construye y organiza tu mundo.");

        ScenePreview preview=new ScenePreview();
        content.addView(preview,
                new LinearLayout.LayoutParams(-1,430));

        content.addView(button("＋ AÑADIR BLOQUE",
                () -> Toast.makeText(this,
                    "Herramienta experimental",
                    Toast.LENGTH_SHORT).show()));

        card("Jerarquía",
                "Mundo\n  ├ Cámara\n  ├ Luz\n  └ Bloque");

        card("Inspector",
                "Propiedades del objeto seleccionado.");

        card("Herramientas",
                "Movimiento · Rotación · Escala");
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

        codeEditor.setText(
            "# Mi primer programa\n"+
            "crear_mundo \"Mi mundo\"\n"+
            "crear_bloque piedra 1 1 1\n"+
            "mostrar_mensaje \"Hola BlockForge\"\n"+
            "ver_bloques\n"
        );

        content.addView(codeEditor);

        content.addView(button("▶ EJECUTAR",
                () -> Toast.makeText(this,
                    "El editor está preparado. El intérprete completo aún debe integrarse.",
                    Toast.LENGTH_LONG).show()));

        card("Comandos disponibles como ejemplo",
                "crear_mundo nombre\n"+
                "crear_bloque tipo x y z\n"+
                "mostrar_mensaje texto\n"+
                "ver_bloques");
    }

    void settings() {
        heading("Configuración",
                "Personaliza BlockForge.");

        card("Apariencia","Tema oscuro.");
        card("Idioma","Español.");
        card("Motor gráfico","C++ / SDL2.");
        card("Lenguaje","El Grafisript.");
        card("Información",
                "BlockForge Studio Mobile\nVersión 0.2 experimental.");
    }

    class ScenePreview extends View {
        Paint paint=new Paint(3);

        ScenePreview() {
            super(BlockForgeActivity.this);
            setBackground(shape(Color.rgb(7,13,23)));
        }

        void polygon(Canvas c,float[] points,int color) {
            Path p=new Path();
            p.moveTo(points[0],points[1]);
            for(int i=2;i<points.length;i+=2)
                p.lineTo(points[i],points[i+1]);
            p.close();
            paint.setColor(color);
            c.drawPath(p,paint);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            float w=getWidth(),h=getHeight();
            paint.setColor(Color.rgb(32,45,65));
            paint.setStrokeWidth(1);

            for(int x=0;x<w;x+=35)
                c.drawLine(x,0,x,h,paint);
            for(int y=0;y<h;y+=35)
                c.drawLine(0,y,w,y,paint);

            float x=w/2,y=h/2;

            polygon(c,new float[]{
                x,y-65,x+65,y-32,x,y,x-65,y-32
            },Color.rgb(78,165,105));

            polygon(c,new float[]{
                x-65,y-32,x,y,x,y+65,x-65,y+32
            },Color.rgb(48,112,77));

            polygon(c,new float[]{
                x+65,y-32,x,y,x,y+65,x+65,y+32
            },Color.rgb(34,83,65));

            paint.setColor(white);
            paint.setTextSize(12);
            c.drawText("VISTA DE ESCENA",15,25,paint);
        }
    }
    }
