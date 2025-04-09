package com.achelmas.numart

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var alertDialogBuilder : AlertDialog.Builder
    private lateinit var logOutBtn: CardView
    private lateinit var appLanguageBtn: CardView
    private lateinit var premiumBtn: CardView

    private var fullnameTextView: TextView? = null
    private lateinit var fullname: String
    private var ageTextView: TextView? = null
    private lateinit var age: String
    private var mAuth: FirebaseAuth? = null

    private var targetsUnlockedTextView: TextView? = null

    private lateinit var isPremiumTxtView: TextView
    private lateinit var userRef: DatabaseReference
    private var isPremiumUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mAuth = FirebaseAuth.getInstance()
        // Initialize user reference
        userRef = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(mAuth!!.currentUser!!.uid)

        // Initialize premium TextView
        isPremiumTxtView = findViewById(R.id.settingsActivity_isPremiumId)

        // Add premium status check
        checkPremiumStatus()

        toolbar = findViewById(R.id.settingsActivity_toolBarId)
        // Set arrow back button to Toolbar
        toolbar.setNavigationIcon(R.drawable.arrow_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        fullnameTextView = findViewById(R.id.settingsActivity_fullnameId)
        ageTextView = findViewById(R.id.settingsActivity_ageId)
        targetsUnlockedTextView = findViewById(R.id.settingsActivity_targetsUnlockedId) // Yeni TextView

        var bundle: Bundle = intent.extras!!
        if(bundle != null) {
            fullname = bundle.getString("fullname")!!.toString()
            age = bundle.getString("age")!!.toString()
        }
        fullnameTextView!!.text = fullname
        ageTextView!!.text = age

        getTargetsUnlocked()


        appLanguageProcess()
        logOutProcess()
        premiumProcess()
    }

    private fun checkPremiumStatus() {
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isPremiumUser = snapshot.child("premium").getValue(Boolean::class.java) ?: false
                updatePremiumUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updatePremiumUI() {
        if (isPremiumUser) {
            isPremiumTxtView.text = "NumARt Premium"
        } else {
            isPremiumTxtView.text = resources.getString(R.string.premium_title)
        }
    }
    private fun premiumProcess() {
        premiumBtn = findViewById(R.id.settingsActivity_premiumBtnId)
        premiumBtn.setOnClickListener {
            if (isPremiumUser) {
                showPremiumStatusMessage()
            } else {
                showPremiumDialog()
            }
        }
    }
    private fun showPremiumStatusMessage() {
        Snackbar.make(
            findViewById(android.R.id.content),
            resources.getString(R.string.premium_status_message),
            Snackbar.LENGTH_SHORT
        ).setBackgroundTint(ContextCompat.getColor(this, R.color.gold))
            .setTextColor(Color.BLACK)
            .show()
    }
    private fun showPremiumDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.premium_alert_dialog, null)
        view.background = ContextCompat.getDrawable(this, R.drawable.background_of_alert_dialog)

        val startBtn: CardView = view.findViewById(R.id.premiumPage_startBtn)
        val closeBtn: ImageView = view.findViewById(R.id.premiumPage_closeBtn)

        builder.setView(view)
        val dialog = builder.create().apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setDimAmount(0.7f)
            setCancelable(false)
        }
        dialog.show()

        startBtn.setOnClickListener {
            unlockAllTargets()
            dialog.dismiss()
        }
        closeBtn.setOnClickListener { dialog.dismiss() }
    }
    private fun unlockAllTargets() {
        val updates = HashMap<String, Any>().apply {
            put("premium", true)
            val targets = HashMap<String, Boolean>().apply {
                for (i in 1..30) put(i.toString(), true)
            }
            put("UserProgress", targets)
        }

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                setResult(RESULT_OK)
                isPremiumUser = true
                updatePremiumUI()
                getTargetsUnlocked() // Refresh unlocked targets count
                Snackbar.make(
                    findViewById(android.R.id.content),
                    resources.getString(R.string.premium_activated),
                    Snackbar.LENGTH_SHORT
                ).setBackgroundTint(ContextCompat.getColor(this, R.color.gold))
                    .setTextColor(Color.BLACK)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, resources.getString(R.string.premium_activation_failed), Toast.LENGTH_SHORT).show()
            }
    }
    private fun appLanguageProcess() {
        appLanguageBtn = findViewById(R.id.settingsActivity_appLanguageBtnId)
        appLanguageBtn.setOnClickListener {
            var bottomSheetView: View = LayoutInflater.from(this).inflate( R.layout.bottom_sheet_layout_of_languages ,
                findViewById(R.id.bottomSheetLayoutOfLanguages_container) )

            var bottomSheetDialog: BottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)

            bottomSheetDialog.setContentView(bottomSheetView)
            bottomSheetDialog.show()

            val englishBtn: RadioButton = bottomSheetView.findViewById(R.id.bottomSheetLayoutOfLanguages_EnglishBtn)
            val turkishBtn: RadioButton = bottomSheetView.findViewById(R.id.bottomSheetLayoutOfLanguages_TurkishBtn)

            // Set current language selection
            when (LanguageManager.loadSelectedLanguage(this)) {
                "en" -> englishBtn.isChecked = true
                "tr" -> turkishBtn.isChecked = true
            }

            englishBtn.setOnClickListener {
                // Save the selected language to SharedPreferences and change app language
                saveAndChangeLanguage("en")
            }

            turkishBtn.setOnClickListener {
                saveAndChangeLanguage("tr")
            }
        }
    }
    private fun saveAndChangeLanguage(language: String) {
        LanguageManager.setLocaleLanguage(this, language)

        // Restart the app to apply the new language
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
    private fun getTargetsUnlocked() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val userProgressRef = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(userId!!)
            .child("UserProgress")

        userProgressRef.get().addOnSuccessListener { snapshot ->
            var unlockedTargets = 0
            snapshot.children.forEach { child ->
                if (child.value == true) {
                    unlockedTargets++
                }
            }

            // Hedeflerin sayısını TextView'de göster
            targetsUnlockedTextView?.text = "$unlockedTargets"
        }.addOnFailureListener {
            Toast.makeText(this, resources.getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
        }
    }

    private fun logOutProcess() {
        // Initialize the variables
        logOutBtn = findViewById(R.id.settingsActivity_logOutBtnId)
        logOutBtn.setOnClickListener {

            alertDialogBuilder  = AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(resources.getString(R.string.log_out))
                .setMessage(resources.getString(R.string.log_out_message))
                .setPositiveButton(resources.getString(R.string.log_out)) { dialogInterface, i ->

                    mAuth!!.signOut()

                    var intent = Intent(this, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                    Toast.makeText(this, resources.getString(R.string.log_out_success), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, i ->
                    dialogInterface.cancel()
                }

            var alertDialog : AlertDialog = alertDialogBuilder.create()

            //Change The Color of LOG OUT Btn
            alertDialog.setOnShowListener {
                var positiveButton: Button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.primaryColor))

                var negativeButton: Button = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(Color.parseColor("#F0F3F8"))
            }
            alertDialog.show()

            // for when the user log out he can again signup but he can't enter to mainAc until login
            clearLoginFlag()
        }
    }

    private fun clearLoginFlag() {
        val prefs: SharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.remove("loginCompleted")
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        // Set Language
        LanguageManager.loadLocale(this)
    }
}