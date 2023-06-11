#ifndef HEADERS_FMGEN_H
#define HEADERS_FMGEN_H


#ifndef STRICT
#define STRICT
#endif

#define WIN32_LEAN_AND_MEAN

#ifdef _WIN32
	#include <windows.h>
	#include <objbase.h>
	#include <tchar.h>
	
#else
	#include <unistd.h>
	#include <fcntl.h>
	#include <linux/stat.h>
	#include <sys/stat.h>
	#include <jni.h>
#endif



#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <assert.h>
#include <limits.h>

/*
#ifdef _MSC_VER
	#undef max
	#define max _MAX
	#undef min
	#define min _MIN
#endif
*/

#endif	// HEADERS_FMGEN_H
