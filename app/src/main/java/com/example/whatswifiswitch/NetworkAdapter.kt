package com.example.whatswifiswitch

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NetworkAdapter(
    private val onEdit: (WifiNetwork) -> Unit,
    private val onDelete: (WifiNetwork) -> Unit
) : RecyclerView.Adapter<NetworkAdapter.ViewHolder>() {

    private var networks: List<WifiNetwork> = emptyList()

    fun submitList(list: List<WifiNetwork>) {
        networks = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val ssidText: TextView = view.findViewById(R.id.ssidText)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_network, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val network = networks[position]
        holder.ssidText.text = network.ssid
        holder.editButton.setOnClickListener { onEdit(network) }
        holder.deleteButton.setOnClickListener { onDelete(network) }
    }

    override fun getItemCount() = networks.size
}
