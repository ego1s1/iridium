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

# 16 KB page-size support (Android 15+): align LOAD segments to 16 KB so the
# loader can mmap the library on 16 KB-page devices. Without this the OS
# reports "LOAD segment not aligned" for this library. Both flags are needed:
# max-page-size sets the alignment ceiling, common-page-size keeps shared
# segments packed at the same granularity (per Android 16 KB migration guide).
target_link_options(
    iridium_epub PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)
