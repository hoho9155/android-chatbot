package com.hoho.chatbot.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.hoho.chatbot.R
import com.hoho.chatbot.models.MessageTurbo

class MessageAdapter(val list: ArrayList<MessageTurbo>, val tts: TextToSpeech): Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View):ViewHolder(view) {
        val txtmsg = view.findViewById<TextView>(R.id.idTV)
        val clipboardBtn = view.findViewById<ImageView>(R.id.idIBClipboard)
        val speakerBtn = view.findViewById<ImageView>(R.id.idIBSpeaker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        var view:View? = null
        val from = LayoutInflater.from(parent.context)
        view = if (viewType==0){
            from.inflate(R.layout.message_item,parent,false)
        } else {
            from.inflate(R.layout.bot_item,parent,false)
        }
        return MessageViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        val message = list[position]
        return if (message.role == "user") 0 else 1
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = list[position]

        holder.txtmsg.text = message.content.trim()
            .replace("\\n", System.getProperty("line.separator"))
            .replace("\\", "")

        if (holder.itemViewType == 1) {
            holder.clipboardBtn.setOnClickListener {
                val clipboardManager = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("text", holder.txtmsg.text)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(holder.itemView.context, "Text copied to clipboard", Toast.LENGTH_LONG).show()
            }
            holder.speakerBtn.setOnClickListener {
                val content = holder.txtmsg.text
                tts.speak(holder.txtmsg.text, TextToSpeech.QUEUE_FLUSH, null, content.toString())
            }
        }

    }
}