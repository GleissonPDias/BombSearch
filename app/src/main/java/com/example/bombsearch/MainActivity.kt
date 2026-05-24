package com.example.bombsearch // Ajuste para o nome exato do seu pacote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Mapeando os elementos do XML
        val etNomeJogador = findViewById<EditText>(R.id.etNomeJogador)
        val btnIniciarJogo = findViewById<Button>(R.id.btnIniciarJogo)
        val btnVerHistorico = findViewById<Button>(R.id.btnVerHistorico)

        btnVerHistorico.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        btnIniciarJogo.setOnClickListener {
            val nomeDigitado = etNomeJogador.text.toString().trim()

            if (nomeDigitado.isEmpty()) {
                // Validação de segurança para não aceitar nome vazio
                Toast.makeText(this, "Por favor, digite seu nome!", Toast.LENGTH_SHORT).show()
            } else {
                // Salvando o nome no SharedPreferences
                salvarNomeUsuario(nomeDigitado)

                // Navegando para a tela do Jogo
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)

                // Opcional: fechar a tela inicial para que o botão 'voltar' do Android não volte para cá
                finish()
            }
        }
    }

    private fun salvarNomeUsuario(nome: String) {
        // Cria ou acessa o arquivo "PreferenciasJogo" de forma privada
        val sharedPreferences = getSharedPreferences("PreferenciasJogo", Context.MODE_PRIVATE)

        // Salva a chave "NOME_JOGADOR" com o valor digitado
        sharedPreferences.edit().apply {
            putString("NOME_JOGADOR", nome)
            apply() // Salva de forma assíncrona (melhor performance)
        }
    }
}