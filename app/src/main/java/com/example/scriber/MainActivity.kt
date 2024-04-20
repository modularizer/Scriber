package com.example.scriber
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.ui.AppBarConfiguration
import com.example.scriber.databinding.ActivityMainBinding

import android.text.Editable
import android.text.TextWatcher

import android.content.Context
import android.content.SharedPreferences
import android.widget.Button
import android.widget.EditText
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

fun getSecureSharedPreferences(context: Context): SharedPreferences {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    return EncryptedSharedPreferences.create(
        "my_secrets", // The name of the shared preferences file
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

fun addSecretToSharedPreferences(context: Context, secretKey: String, secretValue: String) {
    val sharedPreferences = getSecureSharedPreferences(context)
    val editor = sharedPreferences.edit()
    editor.putString(secretKey, secretValue)
    editor.apply()
}

fun getSecretFromSharedPreferences(context: Context, secretKey: String, defValue: String = ""): String {
    val sharedPreferences = getSecureSharedPreferences(context)
    return sharedPreferences.getString(secretKey, defValue)!!
}

class PhoneNumberTextWatcher : TextWatcher {
    private var isFormatting = false

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // No action needed here
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // No action needed here
    }

    override fun afterTextChanged(s: Editable) {
        if (isFormatting) return
        isFormatting = true

        // Remove all non-digit characters
        val digitsOnly = s.toString().filter { it.isDigit() }

        // Break the digits into the parts of a phone number
        val phoneNumber = StringBuilder(digitsOnly)
        when {
            phoneNumber.length > 6 -> phoneNumber.insert(6, "-").insert(3, ") ").insert(0, "(")
            phoneNumber.length > 3 -> phoneNumber.insert(3, ") ").insert(0, "(")
            else -> phoneNumber.insert(0, "(")
        }

        s.replace(0, s.length, phoneNumber)
        isFormatting = false
    }
}


class MainActivity : AppCompatActivity() {
    // TODO: Replace with the actual phone number

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val phoneNumberInput = findViewById<EditText>(R.id.phoneNumberInput)
        phoneNumberInput.addTextChangedListener(PhoneNumberTextWatcher())
        var phoneNumber = getSecretFromSharedPreferences(this, "phoneNumber")

        // set the phone number input to the saved phone number
        phoneNumberInput.setText(phoneNumber)

        val saveButton = findViewById<Button>(R.id.saveButton)

        // Set up click listener for the save button to save the input to SharedPreferences
        saveButton.setOnClickListener {
            phoneNumber = phoneNumberInput.text.toString()
            addSecretToSharedPreferences(this, "phoneNumber", phoneNumber)

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 1)
            } else {
                makePhoneCall(phoneNumber)
            }
        }

        setSupportActionBar(binding.toolbar)



    }

    private fun makePhoneCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Snackbar.make(binding.root, "Unable to make call", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            var phoneNumber = getSecretFromSharedPreferences(this, "phoneNumber")
            makePhoneCall(phoneNumber) // TODO: Replace with the actual phone number
        } else {
            Snackbar.make(binding.root,
                "Permission denied, cannot make call",
                Snackbar.LENGTH_LONG).show()
        }
    }
}