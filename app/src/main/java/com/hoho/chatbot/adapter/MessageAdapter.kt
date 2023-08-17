package com.hoho.chatbot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.GONE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.hoho.chatbot.R
import com.hoho.chatbot.models.MessageTurbo

class MessageAdapter(val list: ArrayList<MessageTurbo>): Adapter<MessageAdapter.viewHolder>() {

    inner class viewHolder(view: View):ViewHolder(view) {
        val txtmsg = view.findViewById<TextView>(R.id.show_message)
        val imgContainer = view.findViewById<LinearLayout>(R.id.imageCard)
        val img = view.findViewById<ImageView>(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        var view:View? = null
        val from = LayoutInflater.from(parent.context)
        view = if (viewType==0){
            from.inflate(R.layout.chatrightitem,parent,false)
        } else {
            from.inflate(R.layout.chatleftitem,parent,false)
        }
        return viewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        val message = list[position]
        return if (message.role == "user") 0 else 1
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        val message = list[position]

        holder.txtmsg.text = message.content
    }
}