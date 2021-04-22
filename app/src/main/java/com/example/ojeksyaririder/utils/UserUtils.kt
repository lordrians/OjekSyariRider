package com.example.ojeksyaririder.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.example.ojeksyaririder.model.TokenModel
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

    fun updateToken(mContext: Context, token: String) {

        val tokenModel = TokenModel()

        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser.uid)
            .setValue(tokenModel)
            .addOnFailureListener { e ->
                Toast.makeText(mContext, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { aVoid ->

            }

    }


}