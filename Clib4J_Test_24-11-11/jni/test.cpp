#include "org_clib4j_communicator_communicator.h"
#include <windows.h>
#include <stdio.h>

extern "C" JNIEXPORT auto JNICALL Java_org_clib4j_communicator_communicator_printnumber(
		JNIEnv *,
		jclass,
		jint number) -> void 
{
    char sz[16];
	sprintf (sz, "%ld", (long)number);
    MessageBoxA(NULL, sz, "A", MB_OK);
}