#include "../src/core/grafisript.hpp"
#include <cassert>
#include <vector>
int main(){
 using namespace blockforge::grafisript;
 std::vector<std::string> out;
 Runtime vm("test-project",[&](const std::string&s){out.push_back(s);});
 vm.ejecutar(R"(
Crear variable Vida = 10
Sumar 5 a Vida
Poner_texto: ("Vida: {Vida}")
Crear lista Inventario = ["Llave", "Mapa"]
Añadir "Poción" a Inventario
Quitar "Mapa" de Inventario
Mostrar "OK"
)");
 assert(out.size()==2&&out[0]=="Vida: 15"&&out[1]=="OK");
 assert(std::get<double>(vm.variables().at("Vida"))==15);
 assert(std::get<Lista>(vm.variables().at("Inventario")).size()==2);
 bool bad=false;try{vm.ejecutar("Sumar 1 a NoExiste");}catch(const Error&e){bad=e.linea==1;}assert(bad);
 Runtime juego("test-project");
 juego.ejecutar(R"(
Crear escena "Prueba 2D"
Crear objeto Jugador
Crear objeto Suelo
Posicion Jugador = 10, 10
Tamaño Jugador = 20, 20
Color Jugador = #33AAFF
Fisica Jugador = Verdadero
Posicion Suelo = 0, 25
Tamaño Suelo = 200, 20
Solido Suelo = Verdadero
Gravedad = 0
Camara seguir Jugador
)");
 const auto&m=juego.mundo_2d();
 assert(m.nombre=="Prueba 2D");
 assert(m.objetos.size()==2);
 assert(m.buscar("Jugador")!=nullptr&&m.buscar("Jugador")->fisica);
 assert(m.buscar("Jugador")->color.r==0x33&&m.buscar("Jugador")->color.g==0xAA);
 assert(m.objetivo_camara()=="Jugador");
 assert(m.detectar_colisiones().size()==1);
}
