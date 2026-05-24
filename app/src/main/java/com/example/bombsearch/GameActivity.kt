package com.example.bombsearch // Ajuste para o seu pacote

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class GameActivity : AppCompatActivity() {

    // Configurações do tabuleiro
    private val LINHAS = 8
    private val COLUNAS = 8
    private val NUM_MINAS = 10

    // Matriz para guardar as referências na tela (AGORA COMO TEXTVIEW)
    private lateinit var botoes: Array<Array<TextView>>

    private lateinit var btnVoltar: Button

    private var matrizLogica = Array(LINHAS) { IntArray(COLUNAS) }

    private var celulasReveladas = 0
    private var pontuacaoAtual = 1000
    private var minasRestantes = NUM_MINAS
    private var timerJob: Job? = null
    private lateinit var tvPontuacao: TextView
    private lateinit var tvMinasRestantes: TextView
    private lateinit var btnReiniciar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        tvPontuacao = findViewById(R.id.tvPontuacao)
        tvMinasRestantes = findViewById(R.id.tvMinasRestantes)
        btnReiniciar = findViewById(R.id.btnReiniciar)

        // Configura o botão de reiniciar
        btnReiniciar.setOnClickListener {
            reiniciarJogo()
        }

        btnVoltar = findViewById(R.id.btnVoltar)

        // Configura o botão de voltar à tela inicial
        btnVoltar.setOnClickListener {
            // 1. Para o cronômetro para evitar vazamento de memória
            pararCronometro()

            // 2. Cria a intenção de voltar para a tela principal
            val intent = android.content.Intent(this, MainActivity::class.java)

            // 3. Limpa a pilha de telas para não acumular Activities abertas
            intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            // 4. Encerra a tela atual do jogo
            finish()
        }

        carregarDadosUsuario()
        gerarTabuleiro()
        sortearMinas()
        calcularNumerosAoRedor()
        atualizarPlacar()
        iniciarCronometro()
    }

    private fun carregarDadosUsuario() {
        val sharedPreferences = getSharedPreferences("PreferenciasJogo", Context.MODE_PRIVATE)
        // Busca o nome, se não achar, usa "Desconhecido" por padrão
        val nome = sharedPreferences.getString("NOME_JOGADOR", "Desconhecido")

        val tvNomeJogador = findViewById<TextView>(R.id.tvNomeJogador)
        tvNomeJogador.text = "Jogador: $nome"
    }

    private fun gerarTabuleiro() {
        val gridLayout = findViewById<GridLayout>(R.id.gridTabuleiro)
        gridLayout.rowCount = LINHAS
        gridLayout.columnCount = COLUNAS

        // Inicializa a matriz de botões como TextView
        botoes = Array(LINHAS) { Array(COLUNAS) { TextView(this) } }

        // 1. Cálculo Perfeito de Tamanho (Convertendo DP para Pixels)
        val displayMetrics = resources.displayMetrics
        val densidade = displayMetrics.density

        // O layout activity_game.xml tem 16dp de padding em cada lado (32dp total).
        val paddingTelaPx = (32 * densidade).toInt()

        // Nossas margens do código são 2px de cada lado do botão = 4px. Vezes o número de colunas.
        val margensBotoesPx = COLUNAS * 4

        // Agora sim, a matemática bate 100% com a tela física do aparelho
        val tamanhoBotao = (displayMetrics.widthPixels - paddingTelaPx - margensBotoesPx) / COLUNAS

        for (linha in 0 until LINHAS) {
            for (coluna in 0 until COLUNAS) {

                // CRIAÇÃO COMO TEXTVIEW PARA EVITAR DISTORÇÕES
                val botao = TextView(this).apply {

                    // Mantém o conteúdo centralizado (números e emojis) e ajusta fonte
                    gravity = android.view.Gravity.CENTER
                    textSize = 18f

                    // Configuração visual inicial
                    setBackgroundColor(Color.LTGRAY)

                    // Parâmetros de layout
                    val params = GridLayout.LayoutParams().apply {
                        width = tamanhoBotao
                        height = tamanhoBotao
                        rowSpec = GridLayout.spec(linha)
                        columnSpec = GridLayout.spec(coluna)
                        setMargins(2, 2, 2, 2) // O mesmo espaçamento usado no cálculo acima
                    }
                    layoutParams = params

                    // Eventos de clique
                    setOnClickListener {
                        cliqueBotao(linha, coluna)
                    }

                    setOnLongClickListener {
                        marcarBandeira(linha, coluna)
                        true
                    }
                }

                // Salva na matriz e adiciona ao layout
                botoes[linha][coluna] = botao
                gridLayout.addView(botao)
            }
        }
    }

    private fun cliqueBotao(linha: Int, coluna: Int) {
        val botao = botoes[linha][coluna]

        // Trocamos isEnabled por isClickable para não perder a cor ao bloquear
        if (!botao.isClickable || botao.text == "🚩") return

        val valor = matrizLogica[linha][coluna]

        if (valor == -1) {
            botao.setBackgroundColor(Color.RED)
            botao.text = "💣"
            gameOver(linha, coluna) // Enviamos exatamente qual bomba causou a explosão
        } else {
            revelarCelula(linha, coluna)
            verificarVitoria()
        }
    }

    private fun revelarCelula(linha: Int, coluna: Int) {
        if (linha !in 0 until LINHAS || coluna !in 0 until COLUNAS) return

        val botao = botoes[linha][coluna]

        // Verifica se é clicável em vez de usar isEnabled
        if (!botao.isClickable || botao.text == "🚩") return

        // Revela o botão visualmente mantendo a vibração das cores
        botao.isClickable = false
        botao.isLongClickable = false
        botao.setBackgroundColor(Color.WHITE)

        // Incrementa o contador de células seguras abertas
        celulasReveladas++

        tvPontuacao.text = "Pontos: $pontuacaoAtual"

        val valor = matrizLogica[linha][coluna]

        if (valor > 0) {
            botao.text = valor.toString()
            botao.setTextColor(Color.BLUE)
        } else if (valor == 0) {
            // Efeito cascata para células vazias
            for (i in -1..1) {
                for (j in -1..1) {
                    revelarCelula(linha + i, coluna + j)
                }
            }
        }
    }

    private fun verificarVitoria() {
        val totalCelulasSeguras = (LINHAS * COLUNAS) - NUM_MINAS

        if (celulasReveladas == totalCelulasSeguras) {
            vitoria()
        }
    }

    private fun vitoria() {
        tvPontuacao.text = "Pontos: $pontuacaoAtual"
        pararCronometro()

        // Bloquear o tabuleiro para o utilizador não clicar em mais nada
        for (l in 0 until LINHAS) {
            for (c in 0 until COLUNAS) {
                botoes[l][c].isClickable = false
                botoes[l][c].isLongClickable = false

                // Opcional: mostrar onde estavam as bandeiras/minas restantes de forma verde
                if (matrizLogica[l][c] == -1) {
                    botoes[l][c].text = "🚩"
                    botoes[l][c].setBackgroundColor(Color.GREEN)
                }
            }
        }

        // Criar um aviso visual de Vitória
        Toast.makeText(this, "Parabéns! Ganhou o jogo!", Toast.LENGTH_LONG).show()

        salvarPartidaNaNuvem(pontuacaoAtual)
    }

    // Definimos -1 como valor padrão para quando o tempo acabar e não houver bomba clicada
    private fun gameOver(linhaExplodida: Int = -1, colunaExplodida: Int = -1) {
        pararCronometro()

        // CORREÇÃO: A pontuação de derrota agora é baseada nas células que ele abriu.
        // Se abrir 20 células antes de explodir, ganha 200 pontos.
        // Se perder por tempo sem abrir nada, ganha 0.
        pontuacaoAtual = celulasReveladas * 10

        // Atualiza a tela para mostrar a pontuação final real da derrota
        tvPontuacao.text = "Pontos: $pontuacaoAtual"

        // Revela todas as bombas para o jogador ver onde elas estavam
        for (l in 0 until LINHAS) {
            for (c in 0 until COLUNAS) {
                val botao = botoes[l][c]

                if (matrizLogica[l][c] == -1) {
                    botao.text = "💣"
                    // Destaca de vermelho APENAS a bomba que matou o jogador
                    if (l == linhaExplodida && c == colunaExplodida) {
                        botao.setBackgroundColor(Color.RED)
                    } else {
                        botao.setBackgroundColor(Color.LTGRAY)
                    }
                }
                // Trava o clique sem deixar transparente!
                botao.isClickable = false
                botao.isLongClickable = false
            }
        }

        Toast.makeText(this, "Game Over! Você explodiu.", Toast.LENGTH_LONG).show()

        // Salva a pontuação corrigida na nuvem
        salvarPartidaNaNuvem(pontuacaoAtual)
    }

    private fun marcarBandeira(linha: Int, coluna: Int) {
        val botao = botoes[linha][coluna]

        // Se o botão já foi aberto (fundo branco), não podemos colocar bandeira
        if (!botao.isClickable && botao.text != "🚩") return

        if (botao.text == "🚩") {
            // RETIRAR BANDEIRA: O jogador mudou de ideia
            botao.text = ""
            minasRestantes++
        } else {
            // COLOCAR BANDEIRA: Verifica se ainda tem bandeiras disponíveis
            if (minasRestantes > 0) {
                botao.text = "🚩"
                minasRestantes--
            }
        }
        atualizarPlacar()
    }

    private fun atualizarPlacar() {
        tvMinasRestantes.text = "Minas: $minasRestantes"
        tvPontuacao.text = "Pontos: $pontuacaoAtual"
    }

    private fun sortearMinas() {
        var minasPlantadas = 0

        while (minasPlantadas < NUM_MINAS) {
            // Sorteia uma linha e coluna aleatória
            val linha = (0 until LINHAS).random()
            val coluna = (0 until COLUNAS).random()

            // Se ainda não tiver mina nesse local, planta uma!
            if (matrizLogica[linha][coluna] != -1) {
                matrizLogica[linha][coluna] = -1
                minasPlantadas++
            }
        }
    }

    private fun calcularNumerosAoRedor() {
        for (l in 0 until LINHAS) {
            for (c in 0 until COLUNAS) {

                // Se a célula atual já for uma mina, não precisa calcular nada
                if (matrizLogica[l][c] == -1) continue

                var totalMinasAoRedor = 0

                // Olha para os 8 vizinhos (acima, abaixo, lados e diagonais)
                for (i in -1..1) {
                    for (j in -1..1) {
                        val vizinhoLinha = l + i
                        val vizinhoColuna = c + j

                        // Verifica se o vizinho não está "fora do tabuleiro" (ex: linha -1)
                        if (vizinhoLinha in 0 until LINHAS && vizinhoColuna in 0 until COLUNAS) {
                            // Se o vizinho for uma mina, aumenta a contagem
                            if (matrizLogica[vizinhoLinha][vizinhoColuna] == -1) {
                                totalMinasAoRedor++
                            }
                        }
                    }
                }

                // Salva o número final na matriz invisível
                matrizLogica[l][c] = totalMinasAoRedor
            }
        }
    }

    private fun reiniciarJogo() {
        // 1. Zera as variáveis de controle
        celulasReveladas = 0
        pontuacaoAtual = 1000
        minasRestantes = NUM_MINAS
        atualizarPlacar()
        iniciarCronometro()

        // 2. Limpa a matriz lógica (zera tudo)
        for (l in 0 until LINHAS) {
            for (c in 0 until COLUNAS) {
                matrizLogica[l][c] = 0

                // 3. Restaura o visual dos botões
                val botao = botoes[l][c]
                botao.text = ""
                botao.setBackgroundColor(Color.LTGRAY)
                // Restaura o poder de clique
                botao.isClickable = true
                botao.isLongClickable = true
            }
        }

        // 4. Sorteia novas minas e recalcula os números
        sortearMinas()
        calcularNumerosAoRedor()
    }

    private fun iniciarCronometro() {
        timerJob?.cancel() // Garante que não teremos dois cronômetros rodando juntos

        // Usamos a Thread Principal (Main) porque vamos alterar a tela (tvPontuacao)
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (pontuacaoAtual > 0) {
                delay(1000) // Espera exatos 1 segundo
                pontuacaoAtual -= 10 // Perde 10 pontos por segundo (ajuste como preferir)
                atualizarPlacar()
            }

            // Opcional: Se a pontuação chegar a zero, o jogador perde por tempo
            if (pontuacaoAtual <= 0) {
                pontuacaoAtual = 0
                atualizarPlacar()
                gameOver() // Chama sem parâmetros para o tempo esgotado
            }
        }
    }

    private fun pararCronometro() {
        timerJob?.cancel() // Interrompe o loop
    }

    private fun salvarPartidaNaNuvem(pontuacaoFinal: Int) {
        // Puxa o nome do jogador salvo localmente
        val sharedPreferences = getSharedPreferences("PreferenciasJogo", Context.MODE_PRIVATE)
        val nome = sharedPreferences.getString("NOME_JOGADOR", "Desconhecido") ?: "Desconhecido"

        // Inicia a tarefa em segundo plano (Thread de I/O)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Monta o objeto que a sua API espera receber
                val partida = PartidaModel(nomeJogador = nome, pontuacao = pontuacaoFinal)

                // Faz a chamada para a rota "/bombsearch/salvar_partida"
                val response = RetrofitClient.apiService.salvarPontuacao(partida)

                // Volta para a Thread Principal para mostrar mensagens na tela
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@GameActivity, "Pontuação salva na nuvem com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("API_ERRO", "Erro da API: ${response.code()}")
                        Toast.makeText(this@GameActivity, "Erro ao salvar no servidor.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Se cair aqui, é porque o servidor Ktor está desligado ou o celular sem internet
                withContext(Dispatchers.Main) {
                    Log.e("API_FALHA", "Falha na conexão: ${e.message}")
                    Toast.makeText(this@GameActivity, "Falha de conexão com a API.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}