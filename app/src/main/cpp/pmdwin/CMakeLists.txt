cmake_minimum_required(VERSION 3.4.1)

#[[
add_library( # Sets the name of the library.
        pmdwin

        # Sets the library as a shared library.
        STATIC

        # Provides a relative path to your source file(s).
        pmdwin.cpp
)

target_include_directories(
        pmdwin
        ${CMAKE_CURRENT_SOURCE_DIR}/pmdwincore
        ${CMAKE_CURRENT_SOURCE_DIR}/fmgen
)
]]

add_subdirectory(pmdwincore)
#add_subdirectory(fmgen)
#add_subdirectory(ymfm)

