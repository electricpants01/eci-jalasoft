package eci.technician.activities;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;

import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.DividerItemDecoration;

import android.view.Menu;
import android.view.MenuItem;

import eci.signalr.messenger.ConversationUser;
import eci.technician.MainApplication;
import eci.technician.R;
import eci.technician.adapters.ChatUsersAdapter;
import eci.technician.databinding.ActivityTechniciansBinding;
import eci.technician.helpers.sortList.TechnicianConversationSortByName;
import eci.technician.tools.Constants;
import eci.technician.tools.SafeLinearLayoutManager;

import java.util.ArrayList;

class TechniciansActivity : AppCompatActivity(), ChatUsersAdapter.IChatUserSelectedListener {
    private lateinit var binding: ActivityTechniciansBinding
    private val messagingUsers: List<ConversationUser> = ArrayList()
    private lateinit var adapter: ChatUsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_technicians)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.recUsers.layoutManager = SafeLinearLayoutManager(this)
        binding.recUsers.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        refillAdapter(messagingUsers)
        retrieveUsers()
    }

    private fun retrieveUsers() {
        MainApplication.connection?.messagingProxy?.invoke(Array<ConversationUser>::class.java, "GetChatTechnicians")?.let { future ->
            future.done { users ->
                users?.let {
                    val usersSorted = it.sortedWith(TechnicianConversationSortByName())
                    runOnUiThread {
                        refillAdapter(usersSorted.toList())
                    }
                }
            }
        }
    }

    private fun refillAdapter(usersList: List<ConversationUser>) {
        adapter = ChatUsersAdapter(usersList, this)
        binding.recUsers.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_technicians, menu)
        val searchMenuItem = menu.findItem(R.id.search)
        val searchView = MenuItemCompat.getActionView(searchMenuItem) as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                adapter.filter?.filter(newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.refresh -> {
                retrieveUsers()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSelect(user: ConversationUser) {
        val data = Intent()
        data.putExtra(Constants.EXTRA_IDENT, user.chatIdent)
        data.putExtra(Constants.EXTRA_TECH_NAME_FOR_TECH_LIST, user.chatName)
        setResult(RESULT_OK, data)
        finish()
    }
}