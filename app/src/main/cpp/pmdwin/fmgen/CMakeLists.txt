cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        fmgen

        STATIC

        # Provides a relative path to your source file(s).
        file_opna.cpp
        fmgen.cpp
        fmtimer.cpp
        opna.cpp
        opm.cpp
        psg.cpp
        sjis2utf.cpp
)

target_include_directories(
        fmgen

        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/..
)