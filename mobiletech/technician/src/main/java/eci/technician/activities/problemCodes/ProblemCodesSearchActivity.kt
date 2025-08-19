package eci.technician.activities.problemCodes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import eci.technician.databinding.ActivityProblemCodesSearchBinding


class ProblemCodesSearchActivity : AppCompatActivity() {

    lateinit var binding: ActivityProblemCodesSearchBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemCodesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavHost()
        setupBar()
    }

    private fun setupNavHost() {
        setSupportActionBar(binding.appbarIncluded.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val navHostFragment =
            supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun setupBar() {
        appBarConfiguration = AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
            onBackPressed()
            true
        }.build()
        binding.appbarIncluded.toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
                || super.onSupportNavigateUp()
    }

}
