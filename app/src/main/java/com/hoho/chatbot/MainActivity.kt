package com.hoho.chatbot

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoho.chatbot.adapter.MessageAdapter
import com.hoho.chatbot.databinding.ActivityMainBinding
import com.hoho.chatbot.models.MessageTurbo
import com.hoho.chatbot.models.TextCompletionsParam
import com.hoho.chatbot.remote.OpenAIRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var list = ArrayList<MessageTurbo>()
    private lateinit var mLayoutManager:LinearLayoutManager
    private lateinit var adaptor: MessageAdapter
    private lateinit var openAIRepo: OpenAIRepositoryImpl

    val RecordAudioRequestCode = 1
    private lateinit var speechRecognizer: SpeechRecognizer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mLayoutManager = LinearLayoutManager(this)
        binding.idRVChats.layoutManager = mLayoutManager
        mLayoutManager.stackFromEnd  = true
        val tts = TextToSpeech(this) {
            Log.i("MainActivity", "onCreate: $it")
        }
        tts.language = Locale.US
        adaptor = MessageAdapter(list, tts)
        binding.idRVChats.adapter = adaptor

        openAIRepo = OpenAIRepositoryImpl()

        renderSystemMsg()

        binding.idIBSend.setOnClickListener {
            if (binding.idEdtMessage.text!!.isEmpty()){
                Toast.makeText(this, "Please ask your question", Toast.LENGTH_SHORT).show()
            } else {
                callApi()
            }
        }

        // voice input
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        var intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {
                binding.idEdtMessage.setText("")
                binding.idEdtMessage.hint =  "Listening..."
            }

            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(i: Int) {}
            override fun onResults(bundle: Bundle) {
                binding.idIBMicrophone.setImageResource(R.drawable.microphone_off)
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                binding.idEdtMessage.setText(data!![0])
            }

            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {}
        })

        binding.idIBMicrophone.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                binding.idIBMicrophone.setImageResource(R.drawable.microphone_off)
                speechRecognizer.stopListening()
            }
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                binding.idIBMicrophone.setImageResource(R.drawable.microphone)
                speechRecognizer.startListening(intent)
            }
            true
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    private fun checkPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf<String>(android.Manifest.permission.RECORD_AUDIO),
            RecordAudioRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RecordAudioRequestCode && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(
                this,
                "Permission Granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun renderSystemMsg() {
        lifecycleScope.launch(Dispatchers.IO) {
            val systemMsg = "Hi, my name is Chloe. I am an expert content creator with artificial intelligence.\n" +
                    "I can help you write any script on any subject for your voice overs, whether for your audio books, youtube videos or any other contents."
            var currentMsg = ""
            var msg = MessageTurbo(currentMsg, "system")
            list.add(msg)

            for (word in systemMsg.split(" ")) {
                currentMsg += "$word "
                msg.content = currentMsg
                withContext(Dispatchers.Main) {
                    adaptor.notifyItemChanged(list.size - 1)
                }
                Thread.sleep(50)
            }
        }
    }

    private fun callApi() {
        list.add(MessageTurbo(binding.idEdtMessage.text.toString(), "user"))

        adaptor.notifyItemInserted(list.size - 1)
        binding.idRVChats.recycledViewPool.clear()
        binding.idRVChats.smoothScrollToPosition(list.size-1)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Execute API OpenAI
                val flow: Flow<String> = openAIRepo.textCompletionsWithStream(
                    TextCompletionsParam(
                        messagesTurbo = list
                    )
                )
                var answerFromGPT: String = ""

                var msg = MessageTurbo(answerFromGPT, "assistant")
                list.add(msg)

                withContext(Dispatchers.Main) {
                    // adaptor.notifyItemInserted(list.size - 1)
                    binding.idRVChats.recycledViewPool.clear()
                    binding.idRVChats.smoothScrollToPosition(list.size - 1)
                }

                // When flow collecting updateLocalAnswer including FAB behavior expanded.
                // On completion FAB == false
                flow.onCompletion {

                }.collect { value ->
                    answerFromGPT += value
                    msg.content = answerFromGPT
                    // Switch back to the main thread before modifying the RecyclerView
                    withContext(Dispatchers.Main) {
                        adaptor.notifyItemChanged(list.size - 1)
                    }
                }

                binding.idEdtMessage.text!!.clear()
            } catch (e:Exception) {
//                withContext(Dispatchers.Main){
//                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
//                }
            }
        }
    }

}