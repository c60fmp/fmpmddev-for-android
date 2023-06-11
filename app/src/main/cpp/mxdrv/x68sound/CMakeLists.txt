cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        x68sound

        # Sets the library as a shared library.
        STATIC

        # Provides a relative path to your source file(s).
        x68sound.cpp
        x68sound_adpcm.cpp
        x68sound_context.cpp
        x68sound_lfo.cpp
        x68sound_op.cpp
        x68sound_opm.cpp
        x68sound_pcm8.cpp
)

target_include_directories(
        x68sound
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../include
)
