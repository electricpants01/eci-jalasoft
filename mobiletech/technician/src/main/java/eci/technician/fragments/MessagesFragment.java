package eci.technician.fragments;


import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;

import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import eci.signalr.messenger.Conversation;
import eci.signalr.messenger.Message;
import eci.signalr.messenger.MessageStatusChangeModel;
import eci.signalr.messenger.MessengerEventListener;
import eci.signalr.messenger.TypingModel;
import eci.signalr.messenger.UserStatusChangeModel;
import eci.technician.MainApplication;
import eci.technician.R;
import eci.technician.activities.MessengerActivity;
import eci.technician.activities.TechniciansActivity;
import eci.technician.adapters.ConversationsAdapter;
import eci.technician.databinding.FragmentMessagesBinding;
import eci.technician.helpers.AppAuth;
import eci.technician.helpers.chat.ChatHandler;
import eci.technician.interfaces.UpdateChatIconListener;
import eci.technician.tools.Constants;
import eci.technician.tools.SafeLinearLayoutManager;

import java.util.Collections;
import java.util.List;

import eci.technician.viewmodels.OrderFragmentViewModel;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessagesFragment extends Fragment implements MessengerEventListener {
    private FragmentMessagesBinding binding;
    private String conversationId = null;
    private ConversationsAdapter adapter;
    private OrderFragmentViewModel viewModel;
    private Boolean doAdapterUpdate = true;
    private UpdateChatIconListener listener;

    public MessagesFragment() {
        this.listener = ChatHandler.INSTANCE.getListener();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        try {
            if (MainApplication.getConnection() != null) {
                MainApplication.getConnection().addMessengerEventListener(this);
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e("MessagesFragment", "Exception", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_messages, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OrderFragmentViewModel.class);
        LinearLayoutManager layoutManager = new SafeLinearLayoutManager(getActivity());
        binding.recMessages.setLayoutManager(layoutManager);
        adapter = new ConversationsAdapter(Collections.emptyList(), this);
        binding.recMessages.setAdapter(adapter);

        observeConversationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        doAdapterUpdate = true;
        observeConversationUpdates();
    }

    private void observeConversationUpdates() {
        if (viewModel != null) {
            viewModel.getConversations().observe(getViewLifecycleOwner(), conversations -> {
                if (doAdapterUpdate) {
                    initAdapter2(conversations);
                    initNotification(conversations);
                }
            });
        }
    }

    private void initNotification(RealmResults<Conversation> conversations) {
        int count = 0;
        for(Conversation conversation: conversations) {
            if(conversation.getUnreadMessageCount() > 0) {
                count ++;
            }
        }
        
        if(listener != null)
            listener.updateChatIcon(count);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (MainApplication.getConnection() != null) {
                MainApplication.getConnection().removeMessengerEventListener(this);
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e("MessagesFragment", "Exception", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTIVITY_TECHNICIANS && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(getActivity(), MessengerActivity.class);
            String chatIdent = data.getStringExtra(Constants.EXTRA_IDENT);
            String techName = data.getStringExtra(Constants.EXTRA_TECH_NAME_FOR_TECH_LIST);
            intent.putExtra(Constants.EXTRA_IDENT, chatIdent);
            intent.putExtra(Constants.EXTRA_TECH_NAME_FOR_TECH_LIST, techName);
            startActivity(intent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Realm realm = Realm.getDefaultInstance();
        MessengerEventListener listener = this;
        inflater.inflate(R.menu.menu_messenger, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String text = newText == null ? null : newText.toLowerCase().trim();

                if (!(text == null || "".equals(text))) {
                    RealmResults<Conversation> conversations = realm.where(Conversation.class).contains("userName", text, Case.INSENSITIVE)
                            .sort("updateTime", Sort.DESCENDING).findAll();
                    adapter = new ConversationsAdapter(conversations, listener);
                    binding.recMessages.setAdapter(adapter);
                    AppAuth.getInstance().setConversationsAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    return true;

                } else {
                    RealmResults<Conversation> conversations = realm.where(Conversation.class).sort("updateTime", Sort.DESCENDING).findAll();
                    adapter = new ConversationsAdapter(conversations, listener);
                    binding.recMessages.setAdapter(adapter);
                    AppAuth.getInstance().setConversationsAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    return false;
                }
            }
        });
        super.onCreateOptionsMenu(menu, inflater);

        realm.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.technicians) {
            openTechnicians();
        }
        return super.onOptionsItemSelected(item);
    }

    private void openTechnicians() {
        Intent intent = new Intent(getActivity(), TechniciansActivity.class);
        startActivityForResult(intent, Constants.ACTIVITY_TECHNICIANS);
    }

    public void initAdapter2(List<Conversation> conversationList) {
        ConversationsAdapter adapter = new ConversationsAdapter(conversationList, this);
        binding.recMessages.setAdapter(adapter);
    }

    @Override
    public void updateAll() {
    }

    @Override
    public void messageReceived(Message message) {
        if (conversationId != null && conversationId.equals(message.getConversationId())) {
            Intent messengerIntent = new Intent(getActivity(), MessengerActivity.class);
            messengerIntent.putExtra(Constants.EXTRA_CONVERSATION_ID, conversationId);
            startActivity(messengerIntent);
            conversationId = null;
        }
    }

    @Override
    public void messageStatusChanged(MessageStatusChangeModel messageStatusChangeModel) {
    }

    @Override
    public void typingMessage(TypingModel typingModel) {
    }

    @Override
    public void userStatusChanged(UserStatusChangeModel userStatusChangeModel) {
    }

    @Override
    public void conversationUsersAdded(Conversation conversation) {
    }

    @Override
    public void conversationUsersRemoved(Conversation conversation) {
    }

    @Override
    public void conversationNameUpdated(Conversation conversation) {
    }

    @Override
    public void hideConversation(Conversation conversation) {
    }

    @Override
    public void unHideConversation(Conversation conversation) {
    }

    @Override
    public void updateConversation(Conversation conversation) {

    }

    @Override
    public void onConversationPressed(Conversation item) {
        doAdapterUpdate = false;
        Intent intent = new Intent(getContext(), MessengerActivity.class);
        intent.putExtra(Constants.EXTRA_CONVERSATION_ID, item.getConversationId());
        this.startActivity(intent);
    }

    @Override
    public void updateStatusMessage() {

    }
}