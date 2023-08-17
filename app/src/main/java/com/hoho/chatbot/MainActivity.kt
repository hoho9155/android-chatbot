package com.hoho.chatbot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoho.chatbot.adapter.MessageAdapter
import com.hoho.chatbot.databinding.ActivityMainBinding
import com.hoho.chatbot.models.MessageTurbo
import com.hoho.chatbot.models.TextCompletionsParam
import com.hoho.chatbot.remote.OpenAIRepositoryImpl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var list = ArrayList<MessageTurbo>()
    private lateinit var mLayoutManager:LinearLayoutManager
    private lateinit var adaptor: MessageAdapter
    private lateinit var openAIRepo: OpenAIRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = mLayoutManager
        mLayoutManager.stackFromEnd  = true
        adaptor = MessageAdapter(list)
        binding.recyclerView.adapter = adaptor

        openAIRepo = OpenAIRepositoryImpl()

        binding.sendbtn.setOnClickListener {
            if (binding.userMsg.text!!.isEmpty()){
                Toast.makeText(this, "Please ask your question", Toast.LENGTH_SHORT).show()
            } else {
                callApi()
            }
        }
    }

    private fun callApi() {
        list.add(MessageTurbo(binding.userMsg.text.toString(), "user"))

        adaptor.notifyItemInserted(list.size - 1)
        binding.recyclerView.recycledViewPool.clear()
        binding.recyclerView.smoothScrollToPosition(list.size-1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Execute API OpenAI
                val flow: Flow<String> = openAIRepo.textCompletionsWithStream(
                    TextCompletionsParam(
                        messagesTurbo = list
                    )
                )
                var answerFromGPT: String = ""

                list.add(MessageTurbo(answerFromGPT, "assistant"))

                withContext(Dispatchers.Main) {
                    // adaptor.notifyItemInserted(list.size - 1)
                    binding.recyclerView.recycledViewPool.clear()
                    binding.recyclerView.smoothScrollToPosition(list.size - 1)
                }

                // When flow collecting updateLocalAnswer including FAB behavior expanded.
                // On completion FAB == false
                flow.onCompletion {

                }.collect { value ->
                    answerFromGPT += value
                    list.removeLast()
                    list.add(MessageTurbo(answerFromGPT, "assistant"))
                    // Switch back to the main thread before modifying the RecyclerView
                    withContext(Dispatchers.Main) {
                        adaptor.notifyItemChanged(list.size - 1)
                    }
                }

                binding.userMsg.text!!.clear()
            } catch (e:Exception) {
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}