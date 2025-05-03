package org.wasmedge.example_app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.wasmedge.native_lib.NativeLib
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var lib: NativeLib

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tv = findViewById<TextView>(R.id.tv_text)
        lib = NativeLib(this)

        Thread {
            val lines = mutableListOf<String>()
            val testModules = listOf(
                "n1_write_file.wasm" to "write_file",
                "n1_read_file.wasm" to "read_file",
                "n1_get_timestamp.wasm" to "get_timestamp",
                "n2_create_multiple_files.wasm" to "create_multiple_files",
                "n2_access_parent_directory.wasm" to "access_parent_directory",
                "n2_allocate_large_memory.wasm" to "allocate_large_memory",
                "n2_open_restricted_file.wasm" to "open_restricted_file",
                "n2_open_network_socket.wasm" to "open_network_socket",
                "n3_execute_external_process.wasm" to "execute_external_process",
                "n3_infinite_loop.wasm" to "infinite_loop",
                "n3_check_env_vars.wasm" to "check_env_vars",
                "n3_write_outside_sandbox.wasm" to "write_outside_sandbox"
            )

            for ((file, func) in testModules) {
                lines.add("Executando $func em $file")
                runOnUiThread { tv.text = lines.joinToString("\n") }
                val result = lib.runWasmFunction(file, func)
                lines.add("Resultado: $result")
                runOnUiThread { tv.text = lines.joinToString("\n") }
            }
        }.start()
    }
}
