cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        pmdwincore

        STATIC

        # Provides a relative path to your source file(s).
        pmdwincore.cpp
        opnaw.cpp
        p86drv.cpp
        ppsdrv.cpp
        ppz8l.cpp
        table.cpp
        util.cpp
)

target_include_directories(
        pmdwincore

        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/..
        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../fmgen
#        PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/../ymfm
)

target_link_libraries( # Specifies the target library.
        pmdwincore
        fmgen
#        ymfm
)

