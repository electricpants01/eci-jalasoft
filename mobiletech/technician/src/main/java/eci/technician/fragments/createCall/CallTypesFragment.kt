package eci.technician.fragments.createCall

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import eci.technician.R
import eci.technician.adapters.CallTypesAdapter
import eci.technician.databinding.FragmentCallTypesBinding
import eci.technician.interfaces.CallTypesAdapterTapListener
import eci.technician.interfaces.CreateCallInterface
import eci.technician.models.create_call.CallType
import eci.technician.viewmodels.CreateCallViewModel
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_call_types.*
import kotlinx.android.synthetic.main.fragment_call_types.toolbar

class CallTypesFragment : Fragment(), CallTypesAdapterTapListener {

    private val viewModel: CreateCallViewModel by activityViewModels()
    private var _binding: FragmentCallTypesBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callBack = requireActivity().onBackPressedDispatcher.addCallback(this){
            goToCreateCallFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentCallTypesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = getString(R.string.call_types)
        showProgressBar()
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        binding.recyclerCallTypes.setHasFixedSize(true)
        binding.recyclerCallTypes.layoutManager = LinearLayoutManager(context)
        viewModel.callTypesList_.observe(viewLifecycleOwner, Observer { callTypeList ->
            val activeCalTypeList = callTypeList.filter { it.active }
            setupRecycler(activeCalTypeList.toMutableList())
        })
    }

    private fun showProgressBar(){
        binding.progressBarContainer.visibility = View.VISIBLE
        binding.recyclerCallTypes.visibility = View.GONE
    }

    private fun hidProgressBar(){
        binding.progressBarContainer.visibility = View.GONE
        binding.recyclerCallTypes.visibility = View.VISIBLE
    }

    private fun setupRecycler(callTypes:MutableList<CallType>) {
        hidProgressBar()
        recyclerCallTypes.adapter = CallTypesAdapter(callTypes, this)
        (recyclerCallTypes.adapter as CallTypesAdapter).notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                goToCreateCallFragment()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onTapCallType(item: CallType) {
        viewModel.callTypeSelected = item
        viewModel.createSC.callTypeId = item.callTypeId
        goToCreateCallFragment()
    }

    fun goToCreateCallFragment(){
        (activity as CreateCallInterface).goToCreateCallFragment()
    }

}