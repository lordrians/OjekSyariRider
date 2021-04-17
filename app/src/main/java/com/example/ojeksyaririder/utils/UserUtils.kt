package com.example.ojeksyaririder.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UserUtils {

    fun updateUser(view: View, updateData: HashMap<String, Any>) {

        FirebaseAuth.getInstance().currentUser?.uid?.let {
            FirebaseDatabase.getInstance()
                .getReference(Common.RIDER_INFO_REFERENCE)
                .child(it)
                .updateChildren(updateData)
                .addOnFailureListener { err ->
                    Snackbar.make(view, err.message.toString(), Snackbar.LENGTH_SHORT).show()
                }
                .addOnSuccessListener {
                    Snackbar.make(view, "Update information successfully!", Snackbar.LENGTH_SHORT).show()
                }
        }

    }


}