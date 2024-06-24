package com.example.boda_v1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.loader.content.CursorLoader
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.boda_v1.Adapter.ChatAdapter
import com.example.boda_v1.Retrofit.ClientMessage
import com.example.boda_v1.Retrofit.IRetrofit
import com.example.boda_v1.Retrofit.Message
import com.example.boda_v1.Retrofit.ServerMessage
import com.example.boda_v1.Retrofit.TextData
import com.example.boda_v1.databinding.ActivityChatBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation

class ChatActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityChatBinding
    private val messages = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var retrofitService: IRetrofit
    private lateinit var textToSpeech: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())
    private val pollingInterval = 5000L
    private var imgPath: String? = null
    private var imageUrl: String? = null
    private lateinit var gestureDetector: GestureDetector

    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    val logging = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textToSpeech = TextToSpeech(this, this)

        chatAdapter = ChatAdapter(messages) { message ->
            speakOut(message.text)
        }
        binding.recyclerView.adapter = chatAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val imageUri = intent.getStringExtra("imageUri")
        if (imageUri != null) {
            loadImageWithRotation(Uri.parse(imageUri), binding.displayImageView)
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                speakOut("질문하기")
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                startVoiceRecognition()
                return true
            }
        })

        binding.buttonMic.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        imgPath = Uri.parse(imageUri)?.let { getFilePathFromUri(it) }

        val gson: Gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://3.34.232.60:3000/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()

        retrofitService = retrofit.create(IRetrofit::class.java)

        clickUpload()
    }

    private fun loadImageWithRotation(imageUri: Uri, imageView: ImageView) {
        val rotation = getImageRotation(imageUri)
        Picasso.get()
            .load(imageUri)
            .rotate(rotation.toFloat())
            .fit()
            .centerCrop()
            .into(imageView)
    }

    private fun getImageRotation(imageUri: Uri): Int {
        var rotation = 0
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
                inputStream.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return rotation
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 10)
        } else {
            Toast.makeText(this, "Your device doesn't support speech input", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                val spokenText = result[0]
                messages.add(Message(spokenText, true))
                chatAdapter.notifyItemInserted(messages.size - 1)
                binding.recyclerView.scrollToPosition(messages.size - 1)
                translateAndSendTextToServer(spokenText)
            }
        }
    }

    private fun translateText(text: String, targetLang: String): String {
        return try {
            val translate = TranslateOptions.newBuilder().setApiKey(BuildConfig.GOOGLE_TRANSLATE_API_KEY).build().service
            val translation: Translation = translate.translate(
                text,
                Translate.TranslateOption.targetLanguage(targetLang)
            )
            translation.translatedText
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TranslateError", "Translation failed: ${e.message}")
            "Translation Error"
        }
    }

    private fun translateAndSendTextToServer(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val translatedText = translateText(text, "en")
                withContext(Dispatchers.Main) {
                    sendTextToServer(translatedText, imageUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "번역 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendTextToServer(text: String, imageUrl: String?) {
        val textData = TextData(text)
        val call = retrofitService.sendText(ClientMessage(text, imageUrl))

        call.enqueue(object : Callback<ServerMessage> {
            override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                if (response.isSuccessful) {
                    val serverMessage = response.body()
                    if (serverMessage != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val translatedMessage = translateText(serverMessage.message, "ko")
                            withContext(Dispatchers.Main) {
                                messages.add(Message(translatedMessage, false))
                                speakOut(translatedMessage)
                                chatAdapter.notifyItemInserted(messages.size - 1)
                                binding.recyclerView.scrollToPosition(messages.size - 1)
                            }
                        }
                    } else {
                        Toast.makeText(this@ChatActivity, "서버 응답이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send text", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                if (t is HttpException) {
                    val responseBody = t.response()?.errorBody()
                    responseBody?.let {
                        Log.e("서버에러", it.string())
                    }
                }
                Log.d("get message", "${t.message}")
                Toast.makeText(this@ChatActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clickUpload() {
        if (imgPath == null) {
            Toast.makeText(this, "이미지를 먼저 선택하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(imgPath!!)
        val body = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val part = MultipartBody.Part.createFormData("file", file.name, body)

        val call: Call<ServerMessage> = retrofitService.uploadImg(part)

        call.enqueue(object : Callback<ServerMessage> {
            override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                if (response.isSuccessful) {
                    val serverMessage = response.body()
                    if (serverMessage != null) {
                        imageUrl = serverMessage.imgUrl
                        CoroutineScope(Dispatchers.IO).launch {
                            val translatedMessage = translateText(serverMessage.message, "ko")
                            withContext(Dispatchers.Main) {
                                messages.add(Message(translatedMessage, false))
                                speakOut(translatedMessage)
                                chatAdapter.notifyItemInserted(messages.size - 1)
                                binding.recyclerView.scrollToPosition(messages.size - 1)
                            }
                        }
                    } else {
                        Toast.makeText(this@ChatActivity, "서버 응답이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "서버 요청 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                Log.e("Upload failed", t.toString())
                Toast.makeText(this@ChatActivity, "업로드 실패: $t", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFilePathFromUri(uri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val loader = CursorLoader(this, uri!!, proj, null, null, null)
        val cursor = loader.loadInBackground()
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        return cursor?.getString(columnIndex!!) ?: ""
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported")
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}
