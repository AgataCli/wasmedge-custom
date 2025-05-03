package org.wasmedge.example_app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.wasmedge.native_lib.NativeLib
import java.util.*
import android.widget.*

class MainActivity : AppCompatActivity() {

    lateinit var lib: NativeLib
    var lastLogTimestamp: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinner = findViewById<Spinner>(R.id.wasm_selector)
        val button = findViewById<Button>(R.id.run_button)
        val tv = findViewById<TextView>(R.id.tv_text)

        lib = NativeLib(this)

        // Lista dos módulos disponíveis: nome visível -> (arquivo.wasm, função)
        val wasmModules = mapOf(
            "N1 - Escrever arquivo" to ("n1_write_file.wasm" to "write_file"),
            "N1 - Ler arquivo" to ("n1_read_file.wasm" to "read_file"),
            "N1 - Obter timestamp" to ("n1_get_timestamp.wasm" to "get_timestamp"),

            "N2 - Criar múltiplos arquivos" to ("n2_create_multiple_files.wasm" to "create_multiple_files"),
            "N2 - Acessar diretório pai" to ("n2_access_parent_directory.wasm" to "access_parent_directory"),
            "N2 - Alocar 100MB" to ("n2_allocate_large_memory.wasm" to "allocate_large_memory"),
            "N2 - Abrir /etc/passwd" to ("n2_open_restricted_file.wasm" to "open_restricted_file"),
            "N2 - Abrir socket localhost" to ("n2_open_network_socket.wasm" to "open_network_socket"),

            "N3 - Executar processo externo" to ("n3_execute_external_process.wasm" to "execute_external_process"),
            "N3 - Loop infinito" to ("n3_infinite_loop.wasm" to "infinite_loop"),
            "N3 - Checar variáveis ambiente" to ("n3_check_env_vars.wasm" to "check_env_vars"),
            "N3 - Escrever fora do sandbox" to ("n3_write_outside_sandbox.wasm" to "write_outside_sandbox")
        )


        val labels = wasmModules.keys.toList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        button.setOnClickListener {
            val label = spinner.selectedItem as String
            val (wasmFile, funcName) = wasmModules[label]!!

            tv.text = "Executando $funcName em $wasmFile..."

            Thread {
                // Limpa o log para evitar repetições
                Runtime.getRuntime().exec("logcat -c").waitFor()

                // Executa o módulo
                val result = lib.runWasmFunction(wasmFile, funcName)

                // Lê os logs após a execução
                val process = Runtime.getRuntime().exec("logcat -d WasmImportLogger:I WasmDinamicLogger:I *:S")
                val reader = process.inputStream.bufferedReader()

                val staticLog = StringBuilder()
                val dynamicLog = StringBuilder()

                reader.forEachLine { line ->
                    when {
                        line.contains("WasmImportLogger") && line.contains("Importando") -> {
                            val index = line.indexOf("Importando")
                            if (index >= 0) staticLog.appendLine(line.substring(index))
                        }
                        line.contains("WasmDinamicLogger") -> {
                            val index = line.indexOf("Funcao") // ou "Arg[" se quiser ser mais amplo
                            val msg = if (index >= 0) line.substring(index) else line
                            dynamicLog.appendLine(msg)
                        }
                    }
                }

                val logOutput = StringBuilder()
                logOutput.appendLine("Captura Estática:")
                logOutput.appendLine(staticLog.toString().trim())
                logOutput.appendLine()
                logOutput.appendLine("Captura Dinâmica:")
                logOutput.appendLine(dynamicLog.toString().trim())

                runOnUiThread {
                    tv.text = logOutput.toString()
                }
            }.start()


        }
    }
}

