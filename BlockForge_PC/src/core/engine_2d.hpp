#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>

namespace blockforge::dosd {
struct Vector2 { double x=0, y=0; };
struct Color { std::uint8_t r=80,g=160,b=240,a=255; };
struct Objeto {
 std::string nombre, sprite;
 Vector2 posicion{}, tamano{32,32}, velocidad{};
 Color color{};
 bool fisica=false, solido=true, visible=true, suelo=false;
};
struct Colision { std::string a,b; };
class Mundo {
public:
 std::string nombre="Escena";
 Vector2 camara{};
 double gravedad=980.0, escala=1.0;
 std::vector<Objeto> objetos;
 std::vector<Colision> colisiones;
 Objeto& crear(const std::string& objectName){if(objectName.empty())throw std::runtime_error("Nombre de objeto vacío");if(buscar(nombre))throw std::runtime_error("Ya existe el objeto: "+objectName);objetos.push_back({});objetos.back().nombre=objectName;return objetos.back();}
 Objeto* buscar(const std::string& objectName){auto i=std::find_if(objetos.begin(),objetos.end(),[&](const Objeto&o){return o.nombre==objectName;});return i==objetos.end()?nullptr:&*i;}
 const Objeto* buscar(const std::string& objectName)const{auto i=std::find_if(objetos.begin(),objetos.end(),[&](const Objeto&o){return o.nombre==objectName;});return i==objetos.end()?nullptr:&*i;}
 void posicion(const std::string& n,double x,double y){exigir(n).posicion={x,y};}
 void tamano(const std::string& n,double w,double h){if(w<=0||h<=0||w>8192||h>8192)throw std::runtime_error("Tamaño 2D fuera de rango");exigir(n).tamano={w,h};}
 void velocidad(const std::string& n,double x,double y){exigir(n).velocidad={x,y};}
 void color(const std::string& n,Color c){exigir(n).color=c;}
 void sprite(const std::string& n,const std::string& ruta){exigir(n).sprite=ruta;}
 void activar_fisica(const std::string& n,bool v=true){exigir(n).fisica=v;}
 void activar_solido(const std::string& n,bool v=true){exigir(n).solido=v;}
 void seguir(const std::string& n){if(!buscar(n))throw std::runtime_error("No existe el objeto de cámara: "+n);objetivoCamara=n;}
 const std::string& objetivo_camara()const{return objetivoCamara;}
 bool colisionan(const Objeto&a,const Objeto&b)const{return a.visible&&b.visible&&a.posicion.x<b.posicion.x+b.tamano.x&&a.posicion.x+a.tamano.x>b.posicion.x&&a.posicion.y<b.posicion.y+b.tamano.y&&a.posicion.y+a.tamano.y>b.posicion.y;}
 std::vector<Colision> detectar_colisiones()const{std::vector<Colision> r;for(size_t i=0;i<objetos.size();++i)for(size_t j=i+1;j<objetos.size();++j)if(objetos[i].solido&&objetos[j].solido&&colisionan(objetos[i],objetos[j]))r.push_back({objetos[i].nombre,objetos[j].nombre});return r;}
 void paso(double dt){dt=std::clamp(dt,0.0,0.05);for(auto&o:objetos)if(o.visible&&o.fisica){o.velocidad.y+=gravedad*dt;o.posicion.x+=o.velocidad.x*dt;o.posicion.y+=o.velocidad.y*dt;}
  for(auto&o:objetos)if(o.visible&&o.fisica)for(const auto&p:objetos)if(&o!=&p&&p.visible&&p.solido&&o.solido&&colisionan(o,p)){double prevBottom=o.posicion.y+o.tamano.y-o.velocidad.y*dt;if(o.velocidad.y>=0&&prevBottom<=p.posicion.y+2){o.posicion.y=p.posicion.y-o.tamano.y;o.velocidad.y=0;}}
  if(!objetivoCamara.empty())if(auto*p=buscar(objetivoCamara))camara={p->posicion.x+p->tamano.x/2,p->posicion.y+p->tamano.y/2};
 }
private:
 std::string objetivoCamara;
 Objeto& exigir(const std::string&n){auto*p=buscar(n);if(!p)throw std::runtime_error("Objeto no definido: "+n);return*p;}
};
}
