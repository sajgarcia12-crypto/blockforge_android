cmake_minimum_required(VERSION 3.24)
project(BlockForge_PC VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(BlockForge_PC src/main.cpp)
target_compile_definitions(BlockForge_PC PRIVATE BLOCKFORGE_PC_BOOTSTRAP=1)
if(MSVC)
  target_compile_options(BlockForge_PC PRIVATE /W4 /permissive-)
else()
  target_compile_options(BlockForge_PC PRIVATE -Wall -Wextra -Wpedantic)
endif()
