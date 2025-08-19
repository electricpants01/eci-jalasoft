package eci.technician.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import eci.technician.R
import eci.technician.adapters.MissingPartsAdapter
import eci.technician.databinding.ActivityReassignMissingPartsBinding
import eci.technician.models.data.UnavailableParts
import eci.technician.tools.Constants
import eci.technician.tools.SafeLinearLayoutManager
import kotlinx.android.synthetic.main.activity_reassign_missing_parts.*

class ReassignMissingPartsActivity : AppCompatActivity() {
    private var unavailableParts: UnavailableParts? = null
    private var neededParts: UnavailableParts? = null
    lateinit var binding: ActivityReassignMissingPartsBinding
    private var serviceOrderId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reassign_missing_parts)
        unavailableParts = intent.getSerializableExtra(Constants.REASSIGN_UNAVAILABLE_PARTS) as UnavailableParts?
        neededParts = intent.getSerializableExtra(Constants.REASSIGN_NEEDED_PARTS) as UnavailableParts?
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        serviceOrderId = intent.getStringExtra(Constants.SERVICE_ORDER_ID)
        serviceOrderId?.let {
            title = getString(R.string.reassign_title, serviceOrderId)
        }
        initAdapter()

        unavailableParts?.let { unavailableParts ->
            binding.missingPartsHintText.text = if (unavailableParts.parts.size > 1) getString(R.string.unavailable_parts_hint) else getString(R.string.unavailable_part_hint)
        }

        neededParts?.let {
            binding.neededPartsHintText.setText(R.string.needed_parts_hint)
        }

        if (unavailableParts == null) {
            binding.missingPartsTxt.visibility = View.GONE
            binding.missingPartsHintText.visibility = View.GONE
        }
        if (neededParts == null) {
            binding.neededPartsTxt.visibility = View.GONE
            binding.neededPartsHintText.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initAdapter() {
        unavailableParts?.let { unavailableParts ->
            val missingPartsAdapter = MissingPartsAdapter(unavailableParts.parts)
            binding.recMissingParts.layoutManager = SafeLinearLayoutManager(this)
            binding.recMissingParts.adapter = missingPartsAdapter
        }
        neededParts?.let { neededParts ->
            val neededPartsAdapter = MissingPartsAdapter(neededParts.parts)
            binding.recNeededParts.layoutManager = SafeLinearLayoutManager(this)
            binding.recNeededParts.adapter = neededPartsAdapter
        }
    }
}