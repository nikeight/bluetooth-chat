package com.example.bluetoothmessaging

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class DevicesRecyclerViewAdapter(val mAllDeviceList: List<AllDeviceData>, val context: Context) :
        RecyclerView.Adapter<DevicesRecyclerViewAdapter.VH>() {


    private var listener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context).inflate(R.layout.recyclerview_single_item, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.label?.text = mAllDeviceList[position].deviceName ?: mAllDeviceList[position].deviceHardwareAddress
    }

    override fun getItemCount(): Int {
        return mAllDeviceList.size
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView){

        var label: TextView? = itemView.findViewById(R.id.largeLabel)

        init {
            itemView.setOnClickListener{
                listener?.itemClicked(mAllDeviceList[adapterPosition])
            }
        }
    }

    fun setItemClickListener(listener: ItemClickListener){
        this.listener = listener
    }

    interface ItemClickListener{
        fun itemClicked(allDeviceData: AllDeviceData)
    }
}