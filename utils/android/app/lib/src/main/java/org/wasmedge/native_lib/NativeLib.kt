package org.wasmedge.native_lib

import android.content.Context

class NativeLib(private val ctx : Context) {
    private external fun nativeRunWasm(imageBytes : ByteArray, funcName: String) : Int

    companion object {
        init {
            System.loadLibrary("wasmedge_lib")
        }
    }

    fun runWasmFunction(wasmFile: String, funcName: String): Int {
        val wasmBytes = ctx.assets.open(wasmFile).readBytes()
        return nativeRunWasm(wasmBytes, funcName)
    }
}
