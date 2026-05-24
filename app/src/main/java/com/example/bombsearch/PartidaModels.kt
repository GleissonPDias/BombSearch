package com.example.bombsearch

import com.google.gson.annotations.SerializedName

// Usada para ENVIAR dados (POST) quando o jogo acaba
data class PartidaModel(
    @SerializedName("nomeJogador")
    val nomeJogador: String,

    @SerializedName("pontuacao")
    val pontuacao: Int
)

// Usada para RECEBER dados (GET) na tela de Histórico
data class PartidaResponseModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nomeJogador")
    val nomeJogador: String,

    @SerializedName("pontuacao")
    val pontuacao: Int,

    @SerializedName("dataPartida")
    val dataPartida: String? // Coloquei a interrogação para evitar erro caso a data venha nula do banco
)