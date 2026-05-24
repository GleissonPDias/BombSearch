package com.example.bombsearch // Ajuste para o seu pacote

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistorico: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistorico = findViewById(R.id.rvHistorico)
        rvHistorico.layoutManager = LinearLayoutManager(this) // Define que a lista será vertical

        buscarHistoricoNaNuvem()
    }

    private fun buscarHistoricoNaNuvem() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Faz o GET na rota do Ktor (certifique-se de que o backend já tem essa rota GET configurada!)
                val response = RetrofitClient.apiService.buscarHistorico()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val listaPartidas = response.body()!!

                        // Passa a lista para o Adapter atualizar a tela
                        val adapter = PartidaAdapter(listaPartidas)
                        rvHistorico.adapter = adapter
                    } else {
                        Toast.makeText(this@HistoryActivity, "Erro ao buscar histórico.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("API_ERRO", "Falha: ${e.message}")
                    Toast.makeText(this@HistoryActivity, "Sem conexão com o servidor.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}