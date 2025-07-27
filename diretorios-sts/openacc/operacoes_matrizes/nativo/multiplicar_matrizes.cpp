#include <iostream>
#include <cstdlib>

extern "C" void multiplicar_matrizes(float* A, float* B, float* C, int N) {
    #pragma acc data copyin(A[0:N*N], B[0:N*N]) copyout(C[0:N*N])
    {
        #pragma acc parallel loop collapse(2)
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                float sum = 0.0f;
                for (int k = 0; k < N; k++) {
                    sum += A[i * N + k] * B[k * N + j];
                }
                C[i * N + j] = sum;
            }
        }
    }
}
