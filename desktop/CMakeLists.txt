cmake_minimum_required(VERSION 3.22)
project(BlockForgeDesktop LANGUAGES C CXX)

# BlockForge usa el mismo motor C++/SDL2 que comparte la base del proyecto.
# En CI, SDL2_SOURCE_DIR apunta al código fuente oficial de SDL2.
if(NOT DEFINED SDL2_SOURCE_DIR OR NOT EXISTS "${SDL2_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Falta SDL2_SOURCE_DIR. Descarga SDL2 y pasa su carpeta a CMake.")
endif()

set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)

add_subdirectory("${SDL2_SOURCE_DIR}" "${CMAKE_BINARY_DIR}/SDL2")

add_executable(BlockForge ../BlockForge_Studio.cpp)
target_compile_features(BlockForge PRIVATE cxx_std_17)
target_link_libraries(BlockForge PRIVATE SDL2::SDL2)

if(TARGET SDL2::SDL2main)
    target_link_libraries(BlockForge PRIVATE SDL2::SDL2main)
elseif(TARGET SDL2main)
    target_link_libraries(BlockForge PRIVATE SDL2main)
endif()

if(WIN32)
    target_compile_definitions(BlockForge PRIVATE SDL_MAIN_HANDLED=0)
endif()

set_target_properties(BlockForge PROPERTIES
    OUTPUT_NAME "BlockForge"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/package"
    RUNTIME_OUTPUT_DIRECTORY_RELEASE "${CMAKE_BINARY_DIR}/package"
)
