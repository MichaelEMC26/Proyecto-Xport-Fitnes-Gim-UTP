package com.registro.huella;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Funciones de libzkfp.dll (ZKFinger SDK 5.x de ZKTeco, el mismo que usa el lector ZK9500).
 * La DLL la instala el driver/SDK del lector en C:\Windows\System32 (64 bits).
 */
interface ZKFinger extends Library {

    ZKFinger LIB = Native.load("libzkfp", ZKFinger.class);

    // Códigos de retorno más comunes
    int OK = 0;
    int YA_INICIADO = 1;
    int SIN_DISPOSITIVO = -3;
    int CAPTURA = -8;          // no hay dedo en el lector (se vuelve a intentar)

    // Parámetros de ZKFPM_GetParameters
    int PARAM_ANCHO = 1;
    int PARAM_ALTO = 2;

    int ZKFPM_Init();
    int ZKFPM_Terminate();
    int ZKFPM_GetDeviceCount();

    Pointer ZKFPM_OpenDevice(int indice);
    int ZKFPM_CloseDevice(Pointer dispositivo);
    int ZKFPM_GetParameters(Pointer dispositivo, int codigo, byte[] valor, IntByReference tamano);

    /** Lee una huella: imagen (ancho*alto bytes, escala de grises) y plantilla (hasta 2048 bytes). */
    int ZKFPM_AcquireFingerprint(Pointer dispositivo, byte[] imagen, int tamImagen,
                                 byte[] plantilla, IntByReference tamPlantilla);

    // Base de huellas en memoria (1:N)
    Pointer ZKFPM_DBInit();
    int ZKFPM_DBFree(Pointer bd);
    int ZKFPM_DBClear(Pointer bd);
    int ZKFPM_DBAdd(Pointer bd, int fid, byte[] plantilla, int tamPlantilla);
    int ZKFPM_DBCount(Pointer bd, IntByReference cantidad);
    int ZKFPM_DBIdentify(Pointer bd, byte[] plantilla, int tamPlantilla, IntByReference fid, IntByReference puntaje);

    /** Une 3 lecturas del mismo dedo en la plantilla de registro. */
    int ZKFPM_DBMerge(Pointer bd, byte[] p1, byte[] p2, byte[] p3, byte[] registro, IntByReference tamRegistro);

    /** Compara dos plantillas: devuelve el puntaje (mayor a 0 = coinciden). */
    int ZKFPM_DBMatch(Pointer bd, byte[] p1, int tam1, byte[] p2, int tam2);
}
