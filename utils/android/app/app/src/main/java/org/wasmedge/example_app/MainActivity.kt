package org.wasmedge.example_app

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.wasmedge.native_lib.NativeLib
import android.widget.*
import androidx.annotation.RequiresApi
import java.io.File

class MainActivity : AppCompatActivity() {

    lateinit var lib: NativeLib

    private var currentModuleName: String? = null
    private val moduleStaticMap = mutableMapOf<String, MutableSet<String>>()
    private val moduleDynamicMap = mutableMapOf<String, MutableSet<String>>()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinner = findViewById<Spinner>(R.id.wasm_selector)
        val runButton = findViewById<Button>(R.id.run_button)
        val csvButton = findViewById<Button>(R.id.btn_generate_csv)
        val tv = findViewById<TextView>(R.id.tv_text)

        lib = NativeLib(this)

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

        runButton.setOnClickListener {
            val label = spinner.selectedItem as String
            val (wasmFile, funcName) = wasmModules[label]!!

            currentModuleName = wasmFile
            moduleStaticMap.putIfAbsent(wasmFile, mutableSetOf())
            moduleDynamicMap.putIfAbsent(wasmFile, mutableSetOf())

            tv.text = "Executando $funcName em $wasmFile..."

            Thread {
                Runtime.getRuntime().exec("logcat -c").waitFor()

                lib.runWasmFunction(wasmFile, funcName)

                val process = Runtime.getRuntime().exec("logcat -d WasmImportLogger:I WasmDinamicLogger:I *:S")
                val reader = process.inputStream.bufferedReader()

                val staticLog = StringBuilder()
                val dynamicLog = StringBuilder()

                reader.forEachLine { line ->
                    if (line.contains("WasmImportLogger") && line.contains("Importando")) {
                        val idx = line.indexOf("Importando")
                        if (idx >= 0) {
                            val staticLine = line.substring(idx)
                            staticLog.appendLine(staticLine)

                            val funcMatch = Regex("Função: ([a-zA-Z0-9_]+)").find(staticLine)
                            val func = funcMatch?.groupValues?.get(1)
                            if (func != null && currentModuleName != null) {
                                moduleStaticMap[currentModuleName!!]?.add(func)
                            }
                        }
                    }

                    if (line.contains("WasmDinamicLogger") && line.contains("Funcao WASI Executada: Nome")) {
                        val match = Regex("Nome = ([^,\\s]+)").find(line)
                        val funcName = match?.groupValues?.get(1)?.substringAfterLast("::")
                        if (funcName != null && currentModuleName != null) {
                            dynamicLog.appendLine("Função Executada: $funcName")
                            moduleDynamicMap[currentModuleName!!]?.add(funcName)
                        }
                    }
                }

                val logOutput = StringBuilder()
                logOutput.appendLine("📌 Captura Estática:")
                logOutput.appendLine(staticLog.toString().trim())
                logOutput.appendLine()
                logOutput.appendLine("📌 Captura Dinâmica:")
                logOutput.appendLine(dynamicLog.toString().trim())

                runOnUiThread {
                    tv.text = logOutput.toString()
                }

            }.start()
        }

        csvButton.setOnClickListener {
            val filename = "captura_wasi_por_modulo.csv"

            val downloadsDir = getExternalFilesDir(null)?.let {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            }

            if (downloadsDir != null) {
                val file = File(downloadsDir, filename)

                try {
                    file.bufferedWriter().use { writer ->
                        writer.write("ModuloWasm,Captura,FuncaoWASI\n")

                        moduleStaticMap.forEach { (mod, funcs) ->
                            funcs.forEach { writer.write("$mod,Estatica,$it\n") }
                        }

                        moduleDynamicMap.forEach { (mod, funcs) ->
                            funcs.forEach { writer.write("$mod,Dinamica,$it\n") }
                        }
                    }

                    Toast.makeText(this, "CSV salvo em Downloads: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao salvar CSV: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Pasta Downloads não acessivel", Toast.LENGTH_LONG).show()
            }
        }

    }
}
