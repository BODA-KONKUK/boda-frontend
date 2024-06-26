import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.boda_v1.R
import com.example.boda_v1.Retrofit.Message

class ChatAdapter(private val messages: List<Message>, private val onMessageClick: (Message) -> Unit) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textUser: TextView = itemView.findViewById(R.id.textUser)
        val textServer: TextView = itemView.findViewById(R.id.textServer)
        val textLabelUser: TextView = itemView.findViewById(R.id.textLabelUser)
        val textLabelServer: TextView = itemView.findViewById(R.id.textLabelServer)

        init {
            itemView.setOnClickListener {
                onMessageClick(messages[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message    , parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            holder.textUser.visibility = View.VISIBLE
            holder.textServer.visibility = View.GONE
            holder.textLabelUser.visibility = View.VISIBLE
            holder.textLabelServer.visibility = View.GONE
            holder.textUser.text = message.text
        } else {
            holder.textUser.visibility = View.GONE
            holder.textServer.visibility = View.VISIBLE
            holder.textLabelUser.visibility = View.GONE
            holder.textLabelServer.visibility = View.VISIBLE
            holder.textServer.text = message.text
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}
