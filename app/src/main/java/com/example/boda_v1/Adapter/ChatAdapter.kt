package com.example.boda_v1.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.boda_v1.R
import com.example.boda_v1.Retrofit.Message

class ChatAdapter(
    private val messages: List<Message>,
    private val onMessageClick: (Message) -> Unit // 클릭 리스너 추가
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    // ViewHolder 클래스 정의
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textUser: TextView = view.findViewById(R.id.textUser)
        val textServer: TextView = view.findViewById(R.id.textServer)
    }

    // ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    // ViewHolder에 데이터 바인딩
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            holder.textUser.visibility = View.VISIBLE
            holder.textServer.visibility = View.GONE
            holder.textUser.text = message.text
        } else {
            holder.textServer.visibility = View.VISIBLE
            holder.textUser.visibility = View.GONE
            holder.textServer.text = message.text
        }

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            onMessageClick(message)
        }
    }

    // 아이템 개수 반환
    override fun getItemCount(): Int {
        return messages.size
    }
}
