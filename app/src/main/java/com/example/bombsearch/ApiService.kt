package com.example.bombsearch

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("bombsearch/salvar_partida")
    suspend fun salvarPontuacao(@Body partida: PartidaModel): Response<Void>

    @GET("bombsearch/historico") // A rota que criaremos no Ktor depois
    suspend fun buscarHistorico(): Response<List<PartidaResponseModel>>

}