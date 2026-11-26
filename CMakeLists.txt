cmake_minimum_required(VERSION 3.22.1)
project(BlockForgeStudio LANGUAGES C CXX)

# Add the official SDL2 source tree at app/src/main/cpp/SDL.
# It must contain SDL/CMakeLists.txt and SDL/src/core/android/...
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/SDL/CMakeLists.txt")
    message(FATAL_ERROR
        "SDL2 source not found. Put the official SDL2 source folder at app/src/main/cpp/SDL")
endif()

set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
add_subdirectory(SDL)

add_library(main SHARED BlockForge_Studio.cpp)
target_compile_features(main PRIVATE cxx_std_17)
target_link_libraries(main PRIVATE SDL2 SDL2main)
