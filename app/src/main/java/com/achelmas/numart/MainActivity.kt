package com.achelmas.numart

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.achelmas.numart.easyLevelMVC.EasyLevelActivity
import com.achelmas.numart.hardLevelMVC.HardLevelActivity
import com.achelmas.numart.mediumLevelMVC.MediumLevelActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_PREMIUM = 1001 // Can be any unique number
    }

    private lateinit var toolbar: Toolbar
    private lateinit var fullNameTxtView: TextView
    private lateinit var fullName: String
    private lateinit var age: String
    private lateinit var easyLevelBtn : RelativeLayout
    private lateinit var mediumLevelBtn : RelativeLayout
    private lateinit var hardLevelBtn : RelativeLayout

    //Firebase
    private var mAuth: FirebaseAuth? = null

    private lateinit var userRef: DatabaseReference
    private var isPremiumUser = false // Track premium status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set language
        LanguageManager.loadLocale(this)

        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(mAuth!!.currentUser!!.uid) // Initialize userRef here

        // Initialize variables
        toolbar = findViewById(R.id.mainActivity_toolBarId)
        fullNameTxtView = findViewById(R.id.mainActivity_fullnameId)
        easyLevelBtn = findViewById(R.id.mainActivity_easyLevelId)
        mediumLevelBtn = findViewById(R.id.mainActivity_mediumLevelId)
        hardLevelBtn = findViewById(R.id.mainActivity_hardLevelId)

        // Set items ( Profile and Settings ) in Toolbar
        toolbar.inflateMenu(R.menu.menu_off)
        // Handle menu item clicks
        itemsOfToolbar()

        // get fullname from firebase
        getFullNameProcess()

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isPremiumUser = snapshot.child("premium").getValue(Boolean::class.java) ?: false
                if (isPremiumUser) {
                    // Change menu icon color to gold
                    toolbar.menu.findItem(R.id.premiumId)?.icon?.setTint(
                        ContextCompat.getColor(this@MainActivity, R.color.gold)
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Handle button clicks
        easyLevelBtn.setOnClickListener {
            var intent = Intent(baseContext , EasyLevelActivity::class.java)
            startActivity(intent)
        }
        mediumLevelBtn.setOnClickListener {
            var intent = Intent(baseContext , MediumLevelActivity::class.java)
            startActivity(intent)
        }
        hardLevelBtn.setOnClickListener {
            var intent = Intent(baseContext , HardLevelActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getFullNameProcess() {

        var reference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users").child(mAuth!!.currentUser!!.uid)

        // Use ValueEventListener to get the value of the "fullname" child
        reference.child("fullname").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the value from the dataSnapshot
                fullName = snapshot.getValue(String::class.java)!!
                val text =  "${resources.getString(R.string.welcome)}, ${fullName}! 👋"

                // Create a SpannableString with the desired text
                val spannable = SpannableString(text)
                // Find the start and end index of the full name in the text
                val startIndex = text.indexOf(fullName)
                val endIndex = startIndex + fullName.length
                // Apply the color span to the full name
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(baseContext, R.color.primaryColor)), // Use a color of your choice
                    startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Set the SpannableString to the TextView
                fullNameTxtView.text = spannable
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })

        // Use ValueEventListener to get the value of the "age" child
        reference.child("age").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the value from the dataSnapshot
                age = snapshot.getValue(String::class.java)!!
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun itemsOfToolbar() {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.settingsId -> {
                    var intent = Intent(baseContext , SettingsActivity::class.java)
                    intent.putExtra("fullname",fullName)
                    intent.putExtra("age",age)
                    startActivityForResult(intent, REQUEST_CODE_PREMIUM)
                    true
                }
                R.id.premiumId -> {
                    if (isPremiumUser) {
                        showPremiumStatusMessage()
                    } else {
                        premiumFunction()
                    }
                    true
                    true
                }
                else -> false
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
    private fun premiumFunction() {
        var builder: AlertDialog.Builder = AlertDialog.Builder(this)
        var view: View = layoutInflater.inflate(R.layout.premium_alert_dialog , null)

        view.background = ContextCompat.getDrawable(this , R.drawable.background_of_alert_dialog)

        // Initialize the variables
        var startBtn: CardView = view.findViewById(R.id.premiumPage_startBtn)
        var closeBtn: ImageView = view.findViewById(R.id.premiumPage_closeBtn)

        builder.setView(view)
        var dialog: AlertDialog = builder.create()
        // Set the background of the entire dialog to transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        startBtn.setOnClickListener {
            unlockAllTarget()
            dialog.dismiss()
        }
        closeBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun unlockAllTarget() {
        val userId = mAuth!!.currentUser!!.uid
        val userRef = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(userId)

        // Create updates for both premium status and all targets
        val updates = HashMap<String, Any>().apply {
            // Set premium status to true
            put("premium", true)

            // Create map for all unlocked targets (assuming 30 targets)
            val targets = HashMap<String, Boolean>().apply {
                for (i in 1..30) {
                    put(i.toString(), true)
                }
            }
            put("UserProgress", targets)
        }

        // Update database in one transaction
        userRef.updateChildren(updates)
            .addOnSuccessListener {
                // Update UI state
                isPremiumUser = true
                toolbar.menu.findItem(R.id.premiumId)?.icon?.setTint(
                    ContextCompat.getColor(this, R.color.gold)
                )

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

    // Add this to refresh when returning from Settings
    override fun onResume() {
        super.onResume()
        checkPremiumStatus()
    }

    private fun checkPremiumStatus() {
        val userRef = FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(mAuth!!.currentUser!!.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isPremiumUser = snapshot.child("premium").getValue(Boolean::class.java) ?: false
                updatePremiumIcon()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun updatePremiumIcon() {
        val premiumMenuItem = toolbar.menu.findItem(R.id.premiumId)
        premiumMenuItem?.icon?.setTint(
            if (isPremiumUser) ContextCompat.getColor(this, R.color.gold)
            else ContextCompat.getColor(this, R.color.white) // Add this color
        )
    }
    // Update menu handling
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val premiumMenuItem = menu.findItem(R.id.premiumId)
        premiumMenuItem.isVisible = true // Always keep visible
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PREMIUM && resultCode == RESULT_OK) {
            checkPremiumStatus() // This will refresh the premium state
        }
    }
}