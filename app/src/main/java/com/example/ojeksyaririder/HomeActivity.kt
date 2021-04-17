package com.example.ojeksyaririder

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
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
import com.bumptech.glide.Glide
import com.example.ojeksyaririder.databinding.ActivityHomeBinding
import com.example.ojeksyaririder.utils.Common
import com.example.ojeksyaririder.utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class HomeActivity : AppCompatActivity() {

    companion object {
        const val PICK_IMAGE_REQUEST = 1717
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding

    private lateinit var navController: NavController

    private lateinit var avatarUri: Uri
    private lateinit var ivAvatar: ImageView

    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference

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

        storageReference = FirebaseStorage.getInstance().reference

        waitingDialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(R.string.waiting)
            .create()

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

        ivAvatar.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        if (Common.currentRider != null && Common.currentRider.avatar != null && !TextUtils.isEmpty(Common.currentRider.avatar)){
            Glide.with(this)
                .load(Common.currentRider.avatar)
                .into(ivAvatar)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            if (data != null && data.data != null){
                avatarUri = data.data!!
                ivAvatar.setImageURI(avatarUri)

                showDialogUpload()
            }
        }
    }

    private fun showDialogUpload() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.change_avatar)
            .setMessage(R.string.confirm_change_avatar)
            .setNegativeButton(R.string.cancel) { dialog: DialogInterface?, i_: Int ->
                dialog?.dismiss()
            }
            .setPositiveButton(R.string.upload){ dialog, i ->
                if (avatarUri != null){
                    waitingDialog.setMessage(resources.getString(R.string.uploading))
                    waitingDialog.show()

                    val uniqueName = FirebaseAuth.getInstance().currentUser?.uid
                    val avatarFolder = storageReference.child("avatars/"+uniqueName)

                    avatarFolder.putFile(avatarUri)
                        .addOnFailureListener {
                            waitingDialog.dismiss()
                            Snackbar.make(binding.drawerLayout, it.message.toString(), Snackbar.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful){
                                avatarFolder.downloadUrl.addOnSuccessListener { uri ->
                                    var updateData: HashMap<String, Any> = HashMap()
                                    updateData.put("avatar", uri.toString())

                                    UserUtils.updateUser(binding.drawerLayout, updateData)
                                }
                            }
                            waitingDialog.dismiss()
                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                            waitingDialog.setMessage(StringBuilder(resources.getString(R.string.uploading)).append(progress).append("%"))
                        }
                }
            }
            .setCancelable(false)
        val dialog: AlertDialog = builder.create()
        dialog.setOnShowListener { dialogInterface ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.colorAccent))
        }
        dialog.show()
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