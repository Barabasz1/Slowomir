cmake_minimum_required(VERSION 3.22.1)
project(microfrontend)

set(MF_DIR ${CMAKE_SOURCE_DIR}/tensorflow/lite/experimental/microfrontend/lib)
set(KISS_DIR ${CMAKE_SOURCE_DIR}/kissfft)

file(GLOB MF_SRC ${MF_DIR}/*.c ${MF_DIR}/*.cc)
list(FILTER MF_SRC EXCLUDE REGEX ".*(_test\\.cc|_main\\.c|memmap[^/]*\\.c)$")

add_library(microfrontend SHARED microfrontend_jni.cpp ${MF_SRC})


target_sources(microfrontend PRIVATE ${KISS_DIR}/kiss_fft.c ${KISS_DIR}/kiss_fftr.c)

target_compile_definitions(microfrontend PRIVATE FIXED_POINT=16)

target_include_directories(microfrontend PRIVATE ${CMAKE_SOURCE_DIR} ${KISS_DIR})
target_link_libraries(microfrontend log)