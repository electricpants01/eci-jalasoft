package eci.technician.activities.repairCode

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import eci.technician.databinding.ActivityRepairCodeSearchBinding

class RepairCodeSearchActivity : AppCompatActivity(){

    private lateinit var binding: ActivityRepairCodeSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepairCodeSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
    }

    private fun initToolbar(){
        setSupportActionBar(binding.appbarIncluded.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
