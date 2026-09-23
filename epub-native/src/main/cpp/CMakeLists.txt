cmake_minimum_required(VERSION 3.22.1)
project(iridium_epub CXX)

add_library(iridium_epub SHARED iridium_epub.cpp)

target_compile_options(iridium_epub PRIVATE
    -std=c++17
    -fexceptions
    -fno-rtti
    -Wall
    -Wextra
    -O2
)

find_library(log-lib log)
find_library(z-lib z)

target_link_libraries(iridium_epub ${log-lib} ${z-lib})
