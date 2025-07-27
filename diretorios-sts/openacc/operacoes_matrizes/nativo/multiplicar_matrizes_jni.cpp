#include <jni.h>
#include "Multiplicacao_Matrizes.h"  // Header com a função C++
#include <iostream>

extern "C" JNIEXPORT void JNICALL Java_operacoes_1matrizes_multiplicacao_Multiplicacao_1Matrizes_multiplicarMatrizes
(JNIEnv* env, jobject obj, jfloatArray jA, jfloatArray jB, jfloatArray jC, jint N) {
    
    float* A = env->GetFloatArrayElements(jA, 0);
    float* B = env->GetFloatArrayElements(jB, 0);
    float* C = env->GetFloatArrayElements(jC, 0);

    multiplicar_matrizes(A, B, C, N);  // chama o código com OpenACC

    env->ReleaseFloatArrayElements(jA, A, 0);
    env->ReleaseFloatArrayElements(jB, B, 0);
    env->ReleaseFloatArrayElements(jC, C, 0);
}
