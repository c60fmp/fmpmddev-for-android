cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        mxdrvcore

        # Sets the library as a shared library.
        STATIC

        # Provides a relative path to your source file(s).
        mxdrv.cpp
        mxdrv_context.cpp
        sound_iocs.cpp
)

target_include_directories(
        mxdrvcore
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../include
)
