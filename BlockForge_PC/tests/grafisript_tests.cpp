#include "../src/core/grafisript.hpp"
#include <cassert>
#include <vector>
int main(){using namespace blockforge::grafisript;std::vector<std::string> out;Runtime vm("test-project",[&](const std::string&s){out.push_back(s);});vm.ejecutar(R"(
Crear variable Vida = 10
Sumar 5 a Vida
Poner_texto: ("Vida: {Vida}")
Crear lista Inventario = ["Llave", "Mapa"]
Añadir "Poción" a Inventario
Quitar "Mapa" de Inventario
Mostrar "OK"
)");assert(out.size()==2&&out[0]=="Vida: 15"&&out[1]=="OK");assert(std::get<double>(vm.variables().at("Vida"))==15);assert(std::get<Lista>(vm.variables().at("Inventario")).size()==2);bool bad=false;try{vm.ejecutar("Sumar 1 a NoExiste");}catch(const Error&e){bad=e.linea==1;}assert(bad);}
