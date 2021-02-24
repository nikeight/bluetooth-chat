package com.example.bluetoothmessaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.webianks.bluechat.Message

class ChatFragment : Fragment() {

    private lateinit var chatInput: EditText
    private lateinit var sendButton: FrameLayout
    private var communicationListener: CommunicationListener? = null
    private var chatingAdapter: ChatingAdapter? = null
    private lateinit var recyclerviewChat: RecyclerView
    private val messageList = arrayListOf<Message>()

    companion object {
        fun newInstance(): ChatFragment {
            val myFragment = ChatFragment()
            val args = Bundle()
            myFragment.arguments = args
            return myFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view: View  = LayoutInflater.from(activity).inflate(R.layout.chat_fragment, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {

        chatInput = view.findViewById(R.id.chatInput)
        val chatIcon: ImageView = view.findViewById(R.id.sendIcon)
        sendButton = view.findViewById(R.id.sendButton)
        recyclerviewChat = view.findViewById(R.id.chatRecyclerView)

        sendButton.isClickable = false
        sendButton.isEnabled = false

        val layoutManager = LinearLayoutManager(activity)
        layoutManager.reverseLayout = true
        recyclerviewChat.layoutManager = layoutManager

        chatInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(txt: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(txt: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(txt: Editable) {

                if (txt.isNotEmpty()) {
                    chatIcon.setImageDrawable(activity?.getDrawable(R.drawable.ic_send))
                    sendButton.isClickable = true
                    sendButton.isEnabled = true
                }else {
                    chatIcon.setImageDrawable(activity?.getDrawable(R.drawable.ic_send_depri))
                    sendButton.isClickable = false
                    sendButton.isEnabled = false
                }
            }
        })

        sendButton.setOnClickListener{

            if (chatInput.text.isNotEmpty()){
                communicationListener?.onCommunication(chatInput.text.toString())
                chatInput.setText("")
            }
        }

        //
        // chatingAdapter = ChatingAdapter(activity!!, messageList.reversed())
        //recyclerviewChat.adapter = chatingAdapter

        //chatingAdapter?.setMessage(messageList)
    }


    fun setCommunicationListener(communicationListener: CommunicationListener){
       this.communicationListener = communicationListener
   }

    interface CommunicationListener{
        fun onCommunication(message: String)
    }

    fun communicate(message: Message) {

        messageList.add(message)
        if (activity != null) {

            chatingAdapter = ChatingAdapter(activity!!, messageList.reversed())

            //chatingAdapter?.setMessage(messageList.reversed() as ArrayList<Message>)

            recyclerviewChat.adapter = chatingAdapter

            chatingAdapter?.notifyDataSetChanged()

            recyclerviewChat.scrollToPosition(0)
        }
    }
}