#include <SDL2/SDL.h>
#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include <fstream>
#include <algorithm>
#include <cmath>

namespace BlockForge {

const int SCREEN_W = 960;
const int SCREEN_H = 640;
const float TILE_W = 64.0f;
const float TILE_H = 32.0f;
const float BLOCK_H = 28.0f;

struct Block {
    int x, y, z;
    std::string type;
};

struct World {
    std::vector<Block> blocks;
    std::string name;

    bool occupied(int x, int y, int z) const {
        for (const auto& b : blocks)
            if (b.x == x && b.y == y && b.z == z) return true;
        return false;
    }

    void add(const std::string& type, int x, int y, int z) {
        if (!occupied(x, y, z))
            blocks.push_back({x, y, z, type});
    }

    void generate() {
        blocks.clear();
        name = "Mi primer mundo";
        for (int x = -5; x <= 5; ++x)
            for (int z = -5; z <= 5; ++z)
                add("pasto", x, 0, z);

        add("piedra", 1, 1, 1);
        add("piedra", 1, 2, 1);
        add("madera", -2, 1, 2);
        add("arena", 3, 1, -2);
    }
};

class Grafisript {
public:
    explicit Grafisript(World& w) : world(w) {}

    // Comandos compatibles:
    // crear_mundo "Nombre"
    // crear_bloque pasto 2 1 3
    // mostrar_mensaje "Hola"
    // limpiar_mundo
    // ver_bloques
    bool execute(const std::string& line) {
        std::istringstream in(line);
        std::string cmd;
        in >> cmd;
        if (cmd.empty() || cmd[0] == '#') return true;

        if (cmd == "crear_mundo") {
            std::string rest;
            std::getline(in, rest);
            rest = trim(rest);
            if (rest.size() >= 2 && rest.front() == '"' && rest.back() == '"')
                rest = rest.substr(1, rest.size() - 2);
            world.blocks.clear();
            world.name = rest.empty() ? "Mundo Grafisript" : rest;
            for (int x = -5; x <= 5; ++x)
                for (int z = -5; z <= 5; ++z)
                    world.add("pasto", x, 0, z);
            message = "Mundo creado: " + world.name;
            return true;
        }

        if (cmd == "crear_bloque") {
            std::string type;
            int x, y, z;
            if (!(in >> type >> x >> y >> z)) {
                message = "Error: crear_bloque tipo x y z";
                return false;
            }
            world.add(type, x, y, z);
            message = "Bloque agregado: " + type;
            return true;
        }

        if (cmd == "mostrar_mensaje") {
            std::string rest;
            std::getline(in, rest);
            message = trim(rest);
            if (message.size() >= 2 && message.front() == '"' && message.back() == '"')
                message = message.substr(1, message.size() - 2);
            return true;
        }

        if (cmd == "limpiar_mundo") {
            world.blocks.clear();
            message = "Mundo limpiado";
            return true;
        }

        if (cmd == "ver_bloques") {
            message = "Bloques en el mundo: " + std::to_string(world.blocks.size());
            return true;
        }

        if (cmd == "ayuda") {
            message = "Comandos: crear_mundo, crear_bloque, mostrar_mensaje, limpiar_mundo, ver_bloques";
            return true;
        }

        message = "Comando desconocido: " + cmd;
        return false;
    }

    const std::string& lastMessage() const { return message; }

    void loadScript(const std::string& path) {
        std::ifstream file(path.c_str());
        if (!file) return;
        std::string line;
        while (std::getline(file, line)) execute(line);
    }

private:
    World& world;
    std::string message = "El Grafisript esta conectado al motor.";

    static std::string trim(const std::string& s) {
        const std::string ws = " \t\r\n";
        size_t a = s.find_first_not_of(ws);
        if (a == std::string::npos) return "";
        size_t b = s.find_last_not_of(ws);
        return s.substr(a, b - a + 1);
    }
};

class Engine {
public:
    Engine() : running(false), playerX(0.0f), playerZ(0.0f),
               cameraX(0.0f), cameraZ(0.0f), moveX(0.0f), moveZ(0.0f) {}

    bool init() {
        if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS) != 0) {
            std::cerr << "SDL_Init fallo: " << SDL_GetError() << std::endl;
            return false;
        }

        window = SDL_CreateWindow(
            "BlockForge Studio - Motor + El Grafisript",
            SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED,
            SCREEN_W, SCREEN_H,
            SDL_WINDOW_SHOWN | SDL_WINDOW_RESIZABLE
        );

        if (!window) {
            std::cerr << "No se pudo crear la ventana: " << SDL_GetError() << std::endl;
            SDL_Quit();
            return false;
        }

        renderer = SDL_CreateRenderer(window, -1,
            SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
        if (!renderer)
            renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_SOFTWARE);

        if (!renderer) {
            std::cerr << "No se pudo crear el renderizador: " << SDL_GetError() << std::endl;
            SDL_DestroyWindow(window);
            SDL_Quit();
            return false;
        }

        SDL_SetRenderDrawBlendMode(renderer, SDL_BLENDMODE_BLEND);
        SDL_StartTextInput();
        running = true;
        return true;
    }

    void run() {
        Uint32 previous = SDL_GetTicks();
        while (running) {
            Uint32 now = SDL_GetTicks();
            float dt = (now - previous) / 1000.0f;
            previous = now;
            if (dt > 0.05f) dt = 0.05f;

            handleEvents();
            update(dt);
            draw();
            SDL_Delay(1);
        }
    }

    void shutdown() {
        SDL_StopTextInput();
        if (renderer) SDL_DestroyRenderer(renderer);
        if (window) SDL_DestroyWindow(window);
        SDL_Quit();
    }

    World& getWorld() { return world; }
    void setMessage(const std::string& m) { message = m; }

private:
    SDL_Window* window = nullptr;
    SDL_Renderer* renderer = nullptr;
    bool running;
    World world;
    float playerX, playerZ, cameraX, cameraZ, moveX, moveZ;
    std::string message = "BlockForge listo. Toca los controles para moverte.";
    bool draggingLeft = false;
    int fingerId = -1;
    int screenW = SCREEN_W, screenH = SCREEN_H;

    void handleEvents() {
        SDL_Event e;
        while (SDL_PollEvent(&e)) {
            if (e.type == SDL_QUIT) running = false;

            if (e.type == SDL_WINDOWEVENT && e.window.event == SDL_WINDOWEVENT_SIZE_CHANGED) {
                screenW = e.window.data1;
                screenH = e.window.data2;
            }

            if (e.type == SDL_KEYDOWN) {
                if (e.key.keysym.sym == SDLK_ESCAPE) running = false;
                if (e.key.keysym.sym == SDLK_w || e.key.keysym.sym == SDLK_UP) moveZ = -1;
                if (e.key.keysym.sym == SDLK_s || e.key.keysym.sym == SDLK_DOWN) moveZ = 1;
                if (e.key.keysym.sym == SDLK_a || e.key.keysym.sym == SDLK_LEFT) moveX = -1;
                if (e.key.keysym.sym == SDLK_d || e.key.keysym.sym == SDLK_RIGHT) moveX = 1;
            }
            if (e.type == SDL_KEYUP) {
                if (e.key.keysym.sym == SDLK_w || e.key.keysym.sym == SDLK_UP ||
                    e.key.keysym.sym == SDLK_s || e.key.keysym.sym == SDLK_DOWN) moveZ = 0;
                if (e.key.keysym.sym == SDLK_a || e.key.keysym.sym == SDLK_LEFT ||
                    e.key.keysym.sym == SDLK_d || e.key.keysym.sym == SDLK_RIGHT) moveX = 0;
            }

            if (e.type == SDL_FINGERDOWN) {
                int x = (int)(e.tfinger.x * screenW);
                int y = (int)(e.tfinger.y * screenH);
                if (x < screenW / 2) {
                    draggingLeft = true;
                    fingerId = (int)e.tfinger.fingerId;
                    setTouchDirection(x, y);
                }
            }
            if (e.type == SDL_FINGERMOTION && draggingLeft &&
                (int)e.tfinger.fingerId == fingerId) {
                setTouchDirection((int)(e.tfinger.x * screenW),
                                  (int)(e.tfinger.y * screenH));
            }
            if (e.type == SDL_FINGERUP && draggingLeft &&
                (int)e.tfinger.fingerId == fingerId) {
                draggingLeft = false;
                moveX = moveZ = 0;
            }

            if (e.type == SDL_MOUSEBUTTONDOWN && e.button.button == SDL_BUTTON_LEFT) {
                if (e.button.x < screenW / 2) setTouchDirection(e.button.x, e.button.y);
            }
            if (e.type == SDL_MOUSEBUTTONUP && e.button.button == SDL_BUTTON_LEFT) {
                moveX = moveZ = 0;
            }
        }
    }

    void setTouchDirection(int x, int y) {
        int cx = screenW / 4;
        int cy = screenH - 105;
        int dx = x - cx;
        int dy = y - cy;
        moveX = (std::abs(dx) < 18) ? 0.0f : (dx > 0 ? 1.0f : -1.0f);
        moveZ = (std::abs(dy) < 18) ? 0.0f : (dy > 0 ? 1.0f : -1.0f);
    }

    void update(float dt) {
        float speed = 3.2f * dt;
        playerX += moveX * speed;
        playerZ += moveZ * speed;
        playerX = std::max(-5.0f, std::min(5.0f, playerX));
        playerZ = std::max(-5.0f, std::min(5.0f, playerZ));
        cameraX += (playerX - cameraX) * 3.0f * dt;
        cameraZ += (playerZ - cameraZ) * 3.0f * dt;
    }

    SDL_Point project(float x, float y, float z) const {
        float sx = screenW * 0.5f + (x - cameraX - z + cameraZ) * TILE_W * 0.5f;
        float sy = screenH * 0.43f + (x - cameraX + z - cameraZ) * TILE_H * 0.5f
                   - y * BLOCK_H;
        return {(int)sx, (int)sy};
    }

    void polygon(const SDL_Point* pts, int count, SDL_Color c) {
        // Triangulacion sencilla para SDL2, sin dependencias adicionales.
        for (int i = 1; i < count - 1; ++i) {
            SDL_SetRenderDrawColor(renderer, c.r, c.g, c.b, c.a);
            SDL_RenderDrawLine(renderer, pts[0].x, pts[0].y, pts[i].x, pts[i].y);
            SDL_RenderDrawLine(renderer, pts[i].x, pts[i].y, pts[i+1].x, pts[i+1].y);
            SDL_RenderDrawLine(renderer, pts[i+1].x, pts[i+1].y, pts[0].x, pts[0].y);
        }
        // Relleno por scanlines.
        int minY = pts[0].y, maxY = pts[0].y;
        for (int i = 1; i < count; ++i) {
            minY = std::min(minY, pts[i].y);
            maxY = std::max(maxY, pts[i].y);
        }
        for (int y = minY; y <= maxY; ++y) {
            std::vector<int> xs;
            for (int i = 0; i < count; ++i) {
                SDL_Point a = pts[i], b = pts[(i+1)%count];
                if ((a.y <= y && b.y > y) || (b.y <= y && a.y > y)) {
                    float t = (float)(y-a.y) / (float)(b.y-a.y);
                    xs.push_back((int)(a.x + t*(b.x-a.x)));
                }
            }
            if (xs.size() >= 2) {
                std::sort(xs.begin(), xs.end());
                SDL_SetRenderDrawColor(renderer, c.r, c.g, c.b, c.a);
                SDL_RenderDrawLine(renderer, xs.front(), y, xs.back(), y);
            }
        }
    }

    SDL_Color colorFor(const std::string& type) const {
        if (type == "pasto") return {76, 166, 75, 255};
        if (type == "tierra") return {139, 91, 55, 255};
        if (type == "piedra") return {135, 143, 151, 255};
        if (type == "madera") return {156, 105, 58, 255};
        if (type == "arena") return {218, 195, 126, 255};
        return {180, 90, 190, 255};
    }

    void drawBlock(const Block& b) {
        SDL_Point top = project((float)b.x, (float)b.y, (float)b.z);
        SDL_Point p[4] = {
            {top.x, top.y},
            {top.x + (int)(TILE_W/2), top.y + (int)(TILE_H/2)},
            {top.x, top.y + (int)TILE_H},
            {top.x - (int)(TILE_W/2), top.y + (int)(TILE_H/2)}
        };
        SDL_Color base = colorFor(b.type);
        SDL_Point left[4] = {p[3], p[2], {p[2].x, p[2].y+(int)BLOCK_H},
                             {p[3].x, p[3].y+(int)BLOCK_H}};
        SDL_Point right[4] = {p[1], p[2], {p[2].x, p[2].y+(int)BLOCK_H},
                              {p[1].x, p[1].y+(int)BLOCK_H}};
        SDL_Point topFace[4] = {p[0], p[1], p[2], p[3]};
        polygon(left, 4, { (Uint8)(base.r*0.68f), (Uint8)(base.g*0.68f),
                          (Uint8)(base.b*0.68f), 255});
        polygon(right, 4, { (Uint8)(base.r*0.82f), (Uint8)(base.g*0.82f),
                           (Uint8)(base.b*0.82f), 255});
        polygon(topFace, 4, base);
        SDL_SetRenderDrawColor(renderer, 35, 42, 35, 180);
        SDL_RenderDrawLine(renderer, p[0].x, p[0].y, p[1].x, p[1].y);
        SDL_RenderDrawLine(renderer, p[1].x, p[1].y, p[2].x, p[2].y);
        SDL_RenderDrawLine(renderer, p[2].x, p[2].y, p[3].x, p[3].y);
        SDL_RenderDrawLine(renderer, p[3].x, p[3].y, p[0].x, p[0].y);
    }

    void fillRect(int x, int y, int w, int h, SDL_Color c) {
        SDL_Rect r = {x,y,w,h};
        SDL_SetRenderDrawColor(renderer,c.r,c.g,c.b,c.a);
        SDL_RenderFillRect(renderer,&r);
    }

    void drawTextHint() {
        // SDL2 sin SDL_ttf: interfaz legible mediante paneles y etiquetas de texto en consola.
        fillRect(12, 12, screenW-24, 46, {12,18,28,205});
        fillRect(20, 20, 5, 30, {70,190,120,255});
        fillRect(34, 22, 170, 5, {230,238,245,255});
        fillRect(34, 34, 260, 4, {115,145,170,255});
        fillRect(12, screenH-62, screenW-24, 50, {12,18,28,205});
        fillRect(22, screenH-48, 28, 22, {70,190,120,255});
        fillRect(58, screenH-48, 28, 22, {70,190,120,255});
        fillRect(94, screenH-48, 28, 22, {70,190,120,255});
        fillRect(screenW-110, screenH-48, 82, 22, {75,110,170,255});
    }

    void draw() {
        SDL_SetRenderDrawColor(renderer, 116, 174, 215, 255);
        SDL_RenderClear(renderer);

        // Orden por profundidad para dibujar primero lo que esta mas lejos.
        std::vector<Block> sorted = world.blocks;
        std::sort(sorted.begin(), sorted.end(), [](const Block& a, const Block& b) {
            return (a.x + a.z + a.y) < (b.x + b.z + b.y);
        });
        for (const auto& b : sorted) drawBlock(b);

        // Personaje sencillo en el plano del mundo.
        SDL_Point pp = project(playerX, 0.15f, playerZ);
        fillRect(pp.x-9, pp.y-27, 18, 26, {45,65,190,255});
        fillRect(pp.x-7, pp.y-38, 14, 13, {235,190,145,255});
        fillRect(pp.x-10, pp.y-25, 20, 4, {25,35,100,255});

        drawTextHint();
        SDL_RenderPresent(renderer);
    }
};

} // namespace BlockForge

int main(int argc, char** argv) {
    (void)argc; (void)argv;

    BlockForge::Engine motor;
    BlockForge::World& mundo = motor.getWorld();
    mundo.generate();

    BlockForge::Grafisript lenguaje(mundo);

    // El juego SIEMPRE abre primero. No espera entrada por consola.
    // Si existe mundo.grafi junto al ejecutable, ejecuta sus comandos.
    lenguaje.loadScript("mundo.grafi");
    motor.setMessage(lenguaje.lastMessage());

    if (!motor.init()) {
        std::cerr << "BlockForge no pudo iniciar SDL2. Revisa la configuracion SDL2 de Cxxdroid."
                  << std::endl;
        return 1;
    }

    std::cout << "BlockForge iniciado. El Grafisript esta integrado." << std::endl;
    std::cout << "Crea un archivo mundo.grafi con comandos como:" << std::endl;
    std::cout << "crear_mundo \"Mi mundo\"" << std::endl;
    std::cout << "crear_bloque piedra 2 1 2" << std::endl;
    std::cout << "mostrar_mensaje \"Hola desde El Grafisript\"" << std::endl;
    motor.run();
    motor.shutdown();
    return 0;
}
