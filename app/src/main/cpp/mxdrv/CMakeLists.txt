cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        mxdrv

        # Sets the library as a shared library.
        STATIC

        # Provides a relative path to your source file(s).
        mxdrvwrapper.cpp
        mxdrvinterface.cpp
        mdx_util.c
        file_opna.cpp
        sjis2utf.cpp
        util.cpp
)

target_include_directories(
        mxdrv
        
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/mxdrv
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/x68sound
)

add_subdirectory(mxdrvcore)
add_subdirectory(x68sound)

target_link_libraries( # Specifies the target library.
        mxdrv
        
        mxdrvcore
        x68sound
)