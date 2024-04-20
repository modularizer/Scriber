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
import android.media.MediaRecorder


import android.text.Editable
import android.text.TextWatcher

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.widget.Button
import android.widget.EditText
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File

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
    private lateinit var binding: ActivityMainBinding
    private lateinit var recorder: MediaRecorder

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
            makePhoneCall(phoneNumber)
        }

        setSupportActionBar(binding.toolbar)
    }

    private fun setupMediaRecorder() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            val outputFile = File(getExternalFilesDir(null), "recorded_call.3gp")

            setOutputFile(outputFile.absolutePath)
            prepare()
            start()  // Start recording
        }
    }

    private fun makePhoneCall(number: String) {
        if (!hasPermissions()) {
            requestPermissions()
            return
        }
        setupMediaRecorder()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = true


        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            recorder.stop()
            recorder.release() // Release the recorder resources

        } else {
            Snackbar.make(binding.root, "Unable to make call", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.apply {
            stop()
            release()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    var permissionsRequired = arrayOf(
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    fun hasPermissions(): Boolean {
        return permissionsRequired.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(this, permissionsRequired, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (hasPermissions()){
            makePhoneCall(getSecretFromSharedPreferences(this, "phoneNumber"))
        } else {
            Snackbar.make(binding.root, "Permission denied, cannot make call", Snackbar.LENGTH_LONG).show()
        }
    }
}