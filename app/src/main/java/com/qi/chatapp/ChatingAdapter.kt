package com.example.bluetoothmessaging

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.webianks.bluechat.Message
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatingAdapter(private val context: Context,private var chatData: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val SENT = 0
    val RECEIVED = 1
    var df: SimpleDateFormat = SimpleDateFormat("hh:mm a",Locale.getDefault())

//    var chatData =  ArrayList<Message>()
//
//    fun setMessage(chatData: ArrayList<Message>){
//        Log.e("All data","$chatData")
//        this.chatData = chatData
//    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when(holder.itemViewType){

            SENT -> {
                val holder: SentHolder = holder as SentHolder
                holder.sentTV.text = chatData[position].message



                val timeMilliSeconds = chatData[position].time
                val resultdate = Date(timeMilliSeconds)

                holder.timeStamp.text = df.format(resultdate)

            }
            RECEIVED -> {
                val holder: ReceivedHolder = holder as ReceivedHolder
                holder.receivedTV.text = chatData[position].message

                Log.d("all text"," "+chatData[position].message)

                val timeMilliSeconds = chatData[position].time
                val resultdate = Date(timeMilliSeconds)
                holder.timeStamp.text = df.format(resultdate)
            }

        }
    }

    override fun getItemViewType(position: Int): Int {

        when(chatData[position].type){
            Constants.MESSAGE_TYPE_SENT -> return SENT
            Constants.MESSAGE_TYPE_RECEIVED -> return RECEIVED
        }

        return -1
    }

    override fun getItemCount(): Int {
        return chatData.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when(viewType){
            SENT -> {
                val view = LayoutInflater.from(context).inflate(R.layout.sent_layout,parent,false)
                return SentHolder(view)
            }
            RECEIVED -> {
                val view = LayoutInflater.from(context).inflate(R.layout.received_layout,parent,false)
                return ReceivedHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context).inflate(R.layout.sent_layout,parent,false)
                return SentHolder(view)
            }
        }
    }

    inner class SentHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var sentTV = itemView.findViewById<TextView>(R.id.sentMessage)
        var timeStamp = itemView.findViewById<TextView>(R.id.timeStamp)
    }

    inner class ReceivedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var receivedTV = itemView.findViewById<TextView>(R.id.receivedMessage)
        var timeStamp = itemView.findViewById<TextView>(R.id.timeStamp)
    }

}