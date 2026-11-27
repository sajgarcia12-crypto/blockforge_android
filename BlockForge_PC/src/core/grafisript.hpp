#pragma once
#include <algorithm>
#include <cctype>
#include <filesystem>
#include <fstream>
#include <functional>
#include <iostream>
#include <map>
#include <sstream>
#include <stdexcept>
#include <string>
#include <variant>
#include <vector>
namespace blockforge::grafisript {
using Lista=std::vector<std::string>;
using Valor=std::variant<double,std::string,bool,Lista>;
struct Error:std::runtime_error{std::size_t linea;Error(std::size_t n,const std::string&m):runtime_error(m),linea(n){}};
class Runtime {
public:
 using Salida=std::function<void(const std::string&)>;
 explicit Runtime(std::filesystem::path root="Proyecto",Salida out={}):root_(std::filesystem::weakly_canonical(std::filesystem::absolute(root))),out_(out?std::move(out):[](const std::string&s){std::cout<<s<<'\n';}){std::filesystem::create_directories(root_);}
 void set_entrada(std::function<std::string(const std::string&)> f){input_=std::move(f);}
 void ejecutar(const std::string&src){std::vector<std::string> l;std::istringstream in(src);std::string s;while(std::getline(in,s))l.push_back(s);paso(l,0,l.size(),0);}
 const std::map<std::string,Valor>& variables()const{return vars_;}
private:
 std::filesystem::path root_;Salida out_;std::function<std::string(const std::string&)> input_=[](const std::string&q){std::cout<<q<<' ';std::string s;std::getline(std::cin,s);return s;};std::map<std::string,Valor>vars_;std::size_t pasos_=0;
 static std::string trim(std::string s){auto a=s.find_first_not_of(" \t\r\n");if(a==s.npos)return{};return s.substr(a,s.find_last_not_of(" \t\r\n")-a+1);}
 static bool starts(const std::string&s,const std::string&p){return s.rfind(p,0)==0;}
 static std::string unq(std::string s){s=trim(s);if(s.size()>1&&s.front()=='"'&&s.back()=='"')return s.substr(1,s.size()-2);return s;}
 static std::string texto(const Valor&v){if(auto p=std::get_if<std::string>(&v))return*p;if(auto p=std::get_if<double>(&v)){std::ostringstream o;o<<*p;return o.str();}if(auto p=std::get_if<bool>(&v))return*p?"Verdadero":"Falso";std::string s;for(auto&x:std::get<Lista>(v)){if(!s.empty())s+=", ";s+=x;}return s;}
 static double num(const Valor&v){if(auto p=std::get_if<double>(&v))return*p;if(auto p=std::get_if<bool>(&v))return*p?1:0;if(auto p=std::get_if<std::string>(&v)){try{return std::stod(*p);}catch(...){}}throw std::runtime_error("Se esperaba un número");}
 Valor valor(const std::string&t){auto s=trim(t);if(s=="Verdadero")return true;if(s=="Falso")return false;if(s.size()>1&&s.front()=='"'&&s.back()=='"')return unq(s);try{std::size_t n;double d=std::stod(s,&n);if(n==s.size())return d;}catch(...){}auto i=vars_.find(s);if(i!=vars_.end())return i->second;throw std::runtime_error("Variable no definida: "+s);}
 double math(const std::string&s){struct P{const std::string&s;size_t i=0;void ws(){while(i<s.size()&&isspace((unsigned char)s[i]))++i;}double f(){ws();if(i<s.size()&&s[i]=='('){++i;double v=e();ws();if(i==s.size()||s[i++]!=')')throw std::runtime_error("Falta )");return v;}bool n=false;if(i<s.size()&&(s[i]=='-'||s[i]=='+'))n=s[i++]=='-';ws();auto b=i;while(i<s.size()&&(isdigit((unsigned char)s[i])||s[i]=='.'))++i;if(b==i)throw std::runtime_error("Expresión matemática inválida");double v=std::stod(s.substr(b,i-b));return n?-v:v;}double t(){double v=f();for(;;){ws();if(i==s.size()||(s[i]!='*'&&s[i]!='/'))return v;char c=s[i++];double r=f();if(c=='/'&&r==0)throw std::runtime_error("División entre cero");v=c=='*'?v*r:v/r;}}double e(){double v=t();for(;;){ws();if(i==s.size()||(s[i]!='+'&&s[i]!='-'))return v;char c=s[i++];double r=t();v=c=='+'?v+r:v-r;}}}p{s};double v=p.e();p.ws();if(p.i!=s.size())throw std::runtime_error("Expresión matemática inválida");return v;}
 bool cond(std::string s){s=trim(s);auto p=s.find(" o ");if(p!=s.npos)return cond(s.substr(0,p))||cond(s.substr(p+3));p=s.find(" y ");if(p!=s.npos)return cond(s.substr(0,p))&&cond(s.substr(p+3));if(starts(s,"no "))return!cond(s.substr(3));for(auto op:{" >= "," <= "," != "," == "," = "," > "," < "}){p=s.find(op);if(p==s.npos)continue;auto a=valor(s.substr(0,p)),b=valor(s.substr(p+std::string(op).size()));std::string o=op;if(o==" = "||o==" == ")return texto(a)==texto(b);if(o==" != ")return texto(a)!=texto(b);double x=num(a),y=num(b);if(o==" > ")return x>y;if(o==" < ")return x<y;if(o==" >= ")return x>=y;return x<=y;}auto v=valor(s);if(auto b=std::get_if<bool>(&v))return*b;return num(v)!=0;}
 std::string interp(std::string s){for(size_t p=0;(p=s.find('{',p))!=s.npos;){auto e=s.find('}',p);if(e==s.npos)break;auto i=vars_.find(s.substr(p+1,e-p-1));if(i==vars_.end())throw std::runtime_error("Variable no definida en texto");auto v=texto(i->second);s.replace(p,e-p+1,v);p+=v.size();}return s;}
 std::filesystem::path path(const std::string&n){std::filesystem::path p(n);if(p.is_absolute())throw std::runtime_error("No se permiten rutas absolutas");auto f=std::filesystem::weakly_canonical(root_/p);auto r=f.lexically_relative(root_);if(r.empty()||*r.begin()=="..")throw std::runtime_error("Ruta fuera del proyecto");return f;}
 size_t close(const std::vector<std::string>&a,size_t from,size_t end,const std::string&o,const std::string&c){int d=0;for(size_t i=from;i<end;++i){auto s=trim(a[i]);if(starts(s,o))++d;if(starts(s,c)&&--d==0)return i;}throw Error(from+1,"Falta "+c);}
 void paso(const std::vector<std::string>&a,size_t b,size_t e,int depth){if(depth>32)throw Error(b+1,"Demasiados bloques anidados");for(size_t i=b;i<e;++i){if(++pasos_>100000)throw Error(i+1,"Límite de seguridad de instrucciones alcanzado");auto s=trim(a[i]);if(s.empty()||s[0]=='#'||(s.front()=='/'&&s.back()=='/'))continue;if(s=="{"||s=="}")continue;try{
 if(starts(s,"Si ")&&s.find(" entonces")!=s.npos){auto p=s.find(" entonces");auto c=close(a,i+1,e,"Si ","Fin_si");size_t mid=c;for(size_t j=i+1;j<c;++j)if(trim(a[j])=="Si_no"){mid=j;break;}if(cond(s.substr(3,p-3)))paso(a,i+1,mid,depth+1);else if(mid<c)paso(a,mid+1,c,depth+1);i=c;continue;}
 if(starts(s,"Repetir ")&&s.find(" veces")!=s.npos){auto p=s.find(" veces");int n=(int)num(valor(s.substr(8,p-8)));if(n<0||n>10000)throw std::runtime_error("Repeticiones fuera de rango");auto c=close(a,i+1,e,"Repetir ","Fin_repetir");for(int k=0;k<n;++k)paso(a,i+1,c,depth+1);i=c;continue;}
 if(starts(s,"Mientras ")){auto c=close(a,i+1,e,"Mientras ","Fin_mientras");size_t n=0;while(cond(s.substr(9))){if(++n>10000)throw std::runtime_error("Bucle Mientras excedió el límite");paso(a,i+1,c,depth+1);}i=c;continue;}
 if(starts(s,"Crear variable ")||starts(s,"Cambiar ")){bool cr=starts(s,"Crear variable ");auto r=s.substr(cr?15:8);auto p=r.find('=');if(p==r.npos)throw std::runtime_error("Usa Nombre = valor");vars_[trim(r.substr(0,p))]=valor(r.substr(p+1));continue;}
 if(starts(s,"Sumar ")||starts(s,"Restar ")){bool add=starts(s,"Sumar ");auto r=s.substr(add?6:7);auto p=r.find(" a ");if(p==r.npos)throw std::runtime_error("Usa Sumar cantidad a Variable");auto k=trim(r.substr(p+3));auto it=vars_.find(k);if(it==vars_.end())throw std::runtime_error("Variable no definida: "+k);it->second=num(it->second)+(add?1:-1)*num(valor(r.substr(0,p)));continue;}
 if(starts(s,"Poner_texto:")){auto t=trim(s.substr(12));if(t.size()>=2&&t.front()=='('&&t.back()==')')t=t.substr(1,t.size()-2);out_(interp(unq(t)));continue;}if(starts(s,"Mostrar ")){auto r=trim(s.substr(8));out_(interp(unq(r.find('"')!=r.npos?r:texto(valor(r)))));continue;}
 if(starts(s,"Preguntar ")){auto p=s.find(" y guardar en ");if(p==s.npos)throw std::runtime_error("Falta 'y guardar en'");vars_[trim(s.substr(p+14))]=input_(interp(unq(s.substr(10,p-10))));continue;}
 if(starts(s,"Crear lista ")){auto r=s.substr(12);auto p=r.find('=');if(p==r.npos)throw std::runtime_error("Lista requiere =");auto v=trim(r.substr(p+1));if(v.size()<2||v.front()!='['||v.back()!=']')throw std::runtime_error("Lista debe usar [ ]");Lista l;std::stringstream ss(v.substr(1,v.size()-2));std::string x;while(std::getline(ss,x,','))l.push_back(unq(x));vars_[trim(r.substr(0,p))]=l;continue;}
 if(starts(s,"Añadir ")||starts(s,"Quitar ")){bool add=starts(s,"Añadir ");auto p=s.find(add?" a ":" de ");if(p==s.npos)throw std::runtime_error("Operación de lista inválida");auto it=vars_.find(trim(s.substr(p+(add?3:4))));if(it==vars_.end()||!std::holds_alternative<Lista>(it->second))throw std::runtime_error("Destino no es una lista");auto&l=std::get<Lista>(it->second);auto v=unq(s.substr(add?7:7,p-7));if(add)l.push_back(v);else{auto q=std::find(l.begin(),l.end(),v);if(q!=l.end())l.erase(q);}continue;}
 if(starts(s,"Guardar ")){auto p=s.find(" en ");if(p==s.npos)throw std::runtime_error("Guardar Variable en archivo");auto it=vars_.find(trim(s.substr(8,p-8)));if(it==vars_.end())throw std::runtime_error("Variable no definida");std::ofstream f(path(unq(s.substr(p+4))),std::ios::binary);if(!f)throw std::runtime_error("No se pudo guardar");f<<texto(it->second);continue;}
 if(starts(s,"Leer ")){auto p=s.find(" y guardar en ");if(p==s.npos)throw std::runtime_error("Leer archivo y guardar en Variable");std::ifstream f(path(unq(s.substr(5,p-5))),std::ios::binary);if(!f)throw std::runtime_error("No se pudo leer");std::ostringstream v;v<<f.rdbuf();vars_[trim(s.substr(p+14))]=v.str();continue;}
 if(starts(s,"Crear carpeta ")){std::filesystem::create_directories(path(unq(s.substr(14))));continue;}
 if(starts(s,"Usar modulo ")){auto m=trim(s.substr(12));if(m!="Matematicas"&&m!="Archivos"&&m!="Datos")throw std::runtime_error("Módulo no disponible: "+m);continue;}
 throw std::runtime_error("Instrucción aún no implementada o desconocida: "+s);
 }catch(const Error&){throw;}catch(const std::exception&x){throw Error(i+1,x.what());}}}
};
}
