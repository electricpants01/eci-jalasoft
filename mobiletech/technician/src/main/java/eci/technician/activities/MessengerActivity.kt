package eci.technician.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eci.signalr.messenger.*
import eci.technician.BaseActivity
import eci.technician.MainApplication
import eci.technician.R
import eci.technician.adapters.MessagesAdapter
import eci.technician.databinding.ActivityMessengerBinding
import eci.technician.helpers.AppAuth
import eci.technician.helpers.NetworkConnection
import eci.technician.helpers.ServiceHelper
import eci.technician.helpers.firebaseAnalytics.FBAnalyticsConstants
import eci.technician.repository.DatabaseRepository
import eci.technician.service.ChatService
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import eci.technician.viewmodels.MessengerViewModel
import io.realm.Realm
import microsoft.aspnet.signalr.client.ConnectionState
import java.util.*

class MessengerActivity : BaseActivity(), MessengerEventListener {
    companion object {
        const val TAG = "MessengerActivity"
        const val EXCEPTION = "Exception"
    }

    private lateinit var binding: ActivityMessengerBinding
    private var conversationId: String? = null
    private var chatIdent: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideTypingRunnable = Runnable { binding.txtTyping.visibility = View.INVISIBLE }
    private var lastTypingSent: Long = 0
    private var hasText = false
    private val layoutManager = SafeLinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
    private var techNameReceivedFromDatabase: String? = null
    val viewModel: MessengerViewModel by viewModels()

    private val sendTypingRunnable = Runnable {
        if (hasText) {
            sendTypingNotification(binding.txtMessage.text!!.toString())
        }
    }

    private val connection by lazy {
        NetworkConnection(this)
    }
    private var isConnected = true

    private fun showTyping(typingMessage: TypingModel) {
        handler.removeCallbacks(hideTypingRunnable)
        handler.postDelayed(hideTypingRunnable, 2000)

        binding.txtTyping.visibility = View.VISIBLE
        binding.txtTyping.text = typingMessage.typingMessage
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_messenger)
        setSupportActionBar(binding.toolbar)
        FBAnalyticsConstants.logEvent(this,FBAnalyticsConstants.MESSENGER_ACTIVITY)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        MainApplication.connection?.addMessengerEventListener(this)
        connection.observe(this) { t ->
            t?.let {
                isConnected = it
            }
        }

        conversationId = intent.getStringExtra(Constants.EXTRA_CONVERSATION_ID)
        chatIdent = intent.getStringExtra(Constants.EXTRA_IDENT)

        if (conversationId == null) {
            val realm = Realm.getDefaultInstance()
            try {
                conversationId =
                    realm.where(Conversation::class.java).equalTo("userIdent", chatIdent)
                        .findFirst()?.conversationId
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
            } finally {
                realm.close()
            }
        }
        conversationId?.let {
            viewModel.handleNotificationIfExists(it, this)
        }

        val techNameReceivedFromRequestPart =
            intent.getStringExtra(Constants.EXTRA_TECH_NAME_FOR_REQUEST_PART_CHAT)

        val realm = Realm.getDefaultInstance()
        try {
            techNameReceivedFromDatabase =
                realm.where(Conversation::class.java).equalTo("conversationId", conversationId)
                    .findFirst()?.userName
        } catch (e: Exception) {
            Log.e(TAG, EXCEPTION, e)
        } finally {
            realm.close()
        }

        val techNameReceivedFromTechList =
            intent.getStringExtra(Constants.EXTRA_TECH_NAME_FOR_TECH_LIST)
        title = techNameReceivedFromRequestPart ?: techNameReceivedFromDatabase
                ?: techNameReceivedFromTechList ?: ""

        setUpRecycler()
        observeMessages()


        binding.txtMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //Log.i(TAG, "beforeTextChanged")
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty() || before == 1) {
                    sendTypingNotification(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable) {
                //Log.i(TAG, "afterTextChanged")
            }
        })

        binding.btnSend.setOnClickListener {
            sendMessage()
            hideKeyboard(this)
        }


    }

    private fun observeMessages() {
        conversationId?.let {
            DatabaseRepository.getInstance().getMessages(it)
                .observe(this, { messages ->
                    binding.recMessages.adapter = MessagesAdapter(messages, applicationContext)
                    val adapter = binding.recMessages.adapter as MessagesAdapter
                    adapter.submitList(messages)
                })
        }
    }

    private fun observeMessages(withConversationId: String) {
        DatabaseRepository.getInstance().getMessages(withConversationId)
            .observe(this, { messages ->
                binding.recMessages.adapter = MessagesAdapter(messages, applicationContext)
                val adapter = binding.recMessages.adapter as MessagesAdapter
                adapter.submitList(messages)
            })

    }

    private fun setUpRecycler() {
        binding.recMessages.layoutManager = layoutManager
    }

    override fun onStart() {
        super.onStart()
        conversationId?.let {
            viewModel.sendSeenMessagesReadUpdatePeriodically(it)
        }
        viewModel.startCheckingMessages()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            conversationId?.let {
                viewModel.sendMessagesReadUpdate(it)
            }
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        conversationId?.let {
            viewModel.sendMessagesReadUpdate(it)
        }

    }

    private fun sendMessage() {
        if (!isConnected) {
            showUnavailableWhenOfflineMessage()
            return
        }

        if (MainApplication.connection?.connectionState != ConnectionState.Connected) {
            showMessageBox(getString(R.string.somethingWentWrong), "Try again later")
            return
        }

        val messageText = binding.txtMessage.text.toString().trim { it <= ' ' }
        if (messageText.isEmpty()) {
            return
        }

        if (conversationId != null) {
            MainApplication.connection?.messagingProxy?.invoke(
                UUID::class.java,
                "SendMessageToConversation",
                messageText,
                conversationId
            )?.let {
                it.done {
                    runOnUiThread {
                        binding.txtMessage.setText("")
                        hasText = false
                    }
                }
            } ?: showUnavailableWhenOfflineMessage()
        } else {
            if (chatIdent != null) {
                MainApplication.connection?.messagingProxy?.invoke(
                    UUID::class.java,
                    "SendPrivateMessageToUser",
                    messageText,
                    chatIdent
                )?.let {
                    it.done { uuid ->
                        conversationId = uuid.toString()
                        runOnUiThread {
                            binding.txtMessage.setText("")
                            hasText = false
                            observeMessages(withConversationId = uuid.toString())
                        }
                    }
                } ?: showUnavailableWhenOfflineMessage()
            }
        }

    }

    private fun sendTypingNotification(text: String) {
        if (conversationId == null) {
            return
        }

        if (lastTypingSent + 3000 > Date().time) {
            hasText = true
            return
        }

        MainApplication.connection?.messagingProxy?.invoke(
            "TypingMessage",
            conversationId,
            text
        )?.let {
            it.done {
                lastTypingSent = Date().time
                hasText = false
                handler.removeCallbacks(sendTypingRunnable)
                handler.postDelayed(sendTypingRunnable, 3000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MainApplication.connection?.removeMessengerEventListener(this)
    }

    override fun updateAll() {
        Log.i(TAG, "updateAll")
    }

    override fun messageReceived(message: Message) {
        conversationId?.let {
            if (!viewModel.isInBackground) {
                viewModel.saveMessageReceived(it, message)
            } else {
                viewModel.saveMessageReceived(null, message)
            }
            if (message.conversationId ?: "" == it && message.senderId != AppAuth.getInstance().technicianUser.id && !viewModel.isInBackground) {
                viewModel.sendSeenStatusForMessage(message)
            }
            viewModel.handleNotificationIfExists(it, this)
        }
    }

    override fun messageStatusChanged(messageStatusChangeModel: MessageStatusChangeModel) {
        Log.d(TAG, "ON messageStatusChanged  for ${messageStatusChangeModel.status}")
        conversationId?.let {
            viewModel.updateStatusChangedForMessage(it, messageStatusChangeModel)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.isInBackground = true
    }

    override fun typingMessage(typingModel: TypingModel) {
        if (typingModel.conversationId == conversationId) {
            showTyping(typingModel)
        }
    }

    override fun userStatusChanged(userStatusChangeModel: UserStatusChangeModel) {
        //Log.i(TAG, "userStatusChanged")
    }

    override fun conversationUsersAdded(conversation: Conversation) {
        Log.i(TAG, "conversationUsersAdded")
    }

    override fun conversationUsersRemoved(conversation: Conversation) {
        Log.i(TAG, "conversationUsersRemoved")

    }

    override fun conversationNameUpdated(conversation: Conversation) {
        Log.i(TAG, "conversationNameUpdated")

    }

    override fun hideConversation(conversation: Conversation) {
        Log.i(TAG, "hideConversation")

    }

    override fun unHideConversation(conversation: Conversation) {
        Log.i(TAG, "unHideConversation")

    }

    override fun updateConversation(conversation: Conversation) {
        Log.i(TAG, "updateConversation")

    }

    override fun onConversationPressed(item: Conversation?) {
        Log.i(TAG, "onConversationPressed")

    }

    override fun updateStatusMessage() {
        Log.i(TAG, "updateStatusMessage  UPDATE STATUS MESSAGE ")
    }

    override fun onResume() {
        super.onResume()
        viewModel.isInBackground = false
        conversationId?.let {
            viewModel.sendSeenMessagesReadUpdatePeriodically(it)
        }
        observeMessages()
        if (!ServiceHelper.isServiceRunning(ChatService::class.java, this)) {
            try {
                val chatServiceIntent = Intent(this, ChatService::class.java)
                chatServiceIntent.putExtra(ChatService.REFRESH_SERVICE_TAG, conversationId)
                startService(chatServiceIntent)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Log.e(TAG, EXCEPTION, e)
            }
        }
    }
}
