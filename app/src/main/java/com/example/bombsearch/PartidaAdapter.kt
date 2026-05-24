package com.example.bombsearch // Ajuste para o seu pacote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PartidaAdapter(private val partidas: List<PartidaResponseModel>) :
    RecyclerView.Adapter<PartidaAdapter.PartidaViewHolder>() {

    // Mapeia os elementos do item_partida.xml
    class PartidaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNome: TextView = itemView.findViewById(R.id.tvItemNome)
        val tvPontos: TextView = itemView.findViewById(R.id.tvItemPontos)
        val tvData: TextView = itemView.findViewById(R.id.tvItemData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartidaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_partida, parent, false)
        return PartidaViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartidaViewHolder, position: Int) {
        val partida = partidas[position]

        holder.tvNome.text = partida.nomeJogador
        holder.tvPontos.text = "Pontos: ${partida.pontuacao}"

        // Se a data vier nula da API, colocamos um texto padrão
        val dataOriginal = partida.dataPartida ?: ""
        if (dataOriginal.isNotEmpty()) {
            holder.tvData.text = formatarData(dataOriginal)
        } else {
            holder.tvData.text = "Data não registrada"
        }
    }

    override fun getItemCount(): Int = partidas.size


    private fun formatarData(dataBanco: String): String {
        return try {
            // Formato que vem do MySQL (ex: "2026-05-23 23:15:00")
            val formatoEntrada = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            formatoEntrada.timeZone = java.util.TimeZone.getTimeZone("UTC") // Avisa o app que a data original é UTC

            val dataConvertida = formatoEntrada.parse(dataBanco)

            // Formato bonito para o usuário ver (ex: "23/05/2026 às 20:15")
            val formatoSaida = java.text.SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", java.util.Locale.getDefault())
            formatoSaida.timeZone = java.util.TimeZone.getDefault() // Usa o fuso horário atual do celular (UTC-3)

            if (dataConvertida != null) {
                formatoSaida.format(dataConvertida)
            } else {
                dataBanco
            }
        } catch (e: Exception) {
            dataBanco // Em caso de erro, apenas mostra o que veio do banco
        }
    }
}