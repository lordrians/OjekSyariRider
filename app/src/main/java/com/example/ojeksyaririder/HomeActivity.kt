package com.example.ojeksyaririder

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.ojeksyaririder.databinding.ActivityHomeBinding
import com.example.ojeksyaririder.utils.Common
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding

    private lateinit var navController: NavController

    private lateinit var avatarUri: Uri

    private lateinit var ivAvatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        init()
    }

    private fun init() {
        binding.navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_sign_out) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.menu_sign_out)
                    .setMessage(R.string.confirm_signout)
                    .setNegativeButton(R.string.cancel) { dialog: DialogInterface?, i_: Int ->
                        dialog?.dismiss()
                    }
                    .setPositiveButton(R.string.signout) { dialog, i ->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(applicationContext, SplashScreenActiviy::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .setCancelable(false)
                val dialog: AlertDialog = builder.create()
                dialog.setOnShowListener { dialogInterface ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                android.R.color.holo_red_dark
                            )
                        )
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                R.color.colorAccent
                            )
                        )
                }
                dialog.show()
            }
            true
        }


        val headerView = binding.navView.getHeaderView(0)
        val tvName: TextView = headerView.findViewById(R.id.tv_navuser_name)
        val tvPhone: TextView = headerView.findViewById(R.id.tv_navuser_phone)
        ivAvatar = headerView.findViewById(R.id.iv_navuser_avatar)

        tvName.text = Common.buildWelcomeMessage()
        tvPhone.text = if (Common.currentRider != null) Common.currentRider.phoneNumber else ""
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}