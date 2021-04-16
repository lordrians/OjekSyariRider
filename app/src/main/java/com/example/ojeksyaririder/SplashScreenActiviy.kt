package com.example.ojeksyaririder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.ojeksyaririder.databinding.ActivitySplashScreenBinding
import com.example.ojeksyaririder.databinding.LayoutRegisterBinding
import com.example.ojeksyaririder.model.RiderModel
import com.example.ojeksyaririder.utils.Common
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class SplashScreenActiviy : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var bindingRegister: LayoutRegisterBinding
    private lateinit var providers: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var riderInfoRef: DatabaseReference

    companion object {
        const val LOGIN_REQUEST_CODE = 1212
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        database = FirebaseDatabase.getInstance()
        riderInfoRef = database.getReference(Common.RIDER_INFO_REFERENCE)

        providers = mutableListOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            var user: FirebaseUser? = myFirebaseAuth.currentUser
            if (user != null){
                checkUserFromFirebase()
            } else {
                showLoginLayout()
            }
        }
    }

    private fun checkUserFromFirebase() {
        riderInfoRef.child(FirebaseAuth.getInstance().currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val riderModel = snapshot.getValue(RiderModel::class.java)
                        if (riderModel != null) {
                            goToHomeActivity(riderModel)
                        }
                    } else {
                        showRegisterLayout()
                    }
                }
            })
    }

    private fun goToHomeActivity(riderModel: RiderModel) {
        Common.currentRider = riderModel
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun showLoginLayout() {
        var authMethodPickerLayout: AuthMethodPickerLayout = AuthMethodPickerLayout
            .Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()

        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMethodPickerLayout)
            .setIsSmartLockEnabled(false)
            .setTheme(R.style.LoginTheme)
            .setAvailableProviders(providers)
            .build(), LOGIN_REQUEST_CODE)
    }

    private fun showRegisterLayout() {
        var alertBuilder = AlertDialog.Builder(this,R.style.DialogTheme)
        bindingRegister = LayoutRegisterBinding.inflate(layoutInflater)

        //Set Data
        if (FirebaseAuth.getInstance().currentUser?.phoneNumber != null &&
            !TextUtils.isEmpty(FirebaseAuth.getInstance().currentUser?.phoneNumber)){
            bindingRegister.edtPhoneNumber.setText(FirebaseAuth.getInstance().currentUser?.phoneNumber)
        }

        //Set View
        alertBuilder.setView(bindingRegister.root)
        val dialog = alertBuilder.create()
        dialog.show()

        bindingRegister.btnRegister.setOnClickListener {
            if (TextUtils.isEmpty(bindingRegister.edtFirstName.text.toString())){
                Toast.makeText(applicationContext, R.string.please_enter_your_first_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (TextUtils.isEmpty(bindingRegister.edtLastName.text.toString())){
                Toast.makeText(applicationContext, R.string.please_enter_your_last_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (TextUtils.isEmpty(bindingRegister.edtPhoneNumber.text.toString())){
                Toast.makeText(applicationContext, R.string.please_enter_your_phone, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                val driver = RiderModel()
                driver.firstName = bindingRegister.edtFirstName.text.toString()
                driver.lastName = bindingRegister.edtLastName.text.toString()
                driver.phoneNumber = bindingRegister.edtPhoneNumber.text.toString()

                riderInfoRef.child(FirebaseAuth.getInstance().currentUser.uid)
                    .setValue(driver)
                    .addOnFailureListener{exception ->
                        dialog.dismiss()
                        Toast.makeText(applicationContext, "exception.message.toString()", Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener { void ->
                        Toast.makeText(applicationContext, R.string.register_success, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        goToHomeActivity(driver)
                    }
            }
        }

    }

    @SuppressLint("CheckResult")
    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var response = IdpResponse.fromResultIntent(data)
        if (requestCode == LOGIN_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                var user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(applicationContext, response?.error?.message.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }
}