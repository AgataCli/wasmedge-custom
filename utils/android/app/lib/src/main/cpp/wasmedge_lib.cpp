#include <jni.h>
#include <string>
#include <array>
#include <wasmedge/wasmedge.h>

extern "C"
JNIEXPORT jint JNICALL
Java_org_wasmedge_native_1lib_NativeLib_nativeRunWasm(JNIEnv *env, jobject, jbyteArray image_bytes,
                                                      jstring func_name_jstr) {
    // Lê os bytes do wasm
    jsize buffer_size = env->GetArrayLength(image_bytes);
    jbyte *buffer = env->GetByteArrayElements(image_bytes, nullptr);

    // Lê a string do nome da função
    const char *func_name_cstr = env->GetStringUTFChars(func_name_jstr, nullptr);
    WasmEdge_String func_name = WasmEdge_StringCreateByCString(func_name_cstr);

    // Configura a execução com WASI
    WasmEdge_ConfigureContext *conf = WasmEdge_ConfigureCreate();
    WasmEdge_ConfigureAddHostRegistration(conf, WasmEdge_HostRegistration_Wasi);

    WasmEdge_VMContext *vm_ctx = WasmEdge_VMCreate(conf, nullptr);

    std::array<WasmEdge_Value, 0> params{};
    std::array<WasmEdge_Value, 1> ret_val{};

    WasmEdge_Result res = WasmEdge_VMRunWasmFromBuffer(
            vm_ctx, reinterpret_cast<const uint8_t *>(buffer), buffer_size,
            func_name, params.data(), params.size(),
            ret_val.data(), ret_val.size()
    );

    // Liberação
    WasmEdge_VMDelete(vm_ctx);
    WasmEdge_ConfigureDelete(conf);
    WasmEdge_StringDelete(func_name);
    env->ReleaseByteArrayElements(image_bytes, buffer, 0);
    env->ReleaseStringUTFChars(func_name_jstr, func_name_cstr);

    // Se a função tiver retorno (ex: main() ou write_file() não retornam), pode ignorar `ret_val`
    return WasmEdge_ResultOK(res) ? 0 : -1;
}

