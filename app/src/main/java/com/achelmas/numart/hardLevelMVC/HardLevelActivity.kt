package com.achelmas.numart.hardLevelMVC

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.achelmas.numart.R
import com.achelmas.numart.easyLevelMVC.AdapterOfEasyLvl
import com.achelmas.numart.easyLevelMVC.ModelOfEasyLvl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class HardLevelActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterOfHardLvl
    private lateinit var hardLvlList: ArrayList<ModelOfHardLvl>
    private lateinit var myReference: DatabaseReference
    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hard_level)

        mAuth = FirebaseAuth.getInstance()

        // Initialize Views
        toolbar = findViewById(R.id.hardLvlActivity_toolBarId)
        recyclerView = findViewById(R.id.hardLvlActivity_recyclerViewId)

        // Set arrow back button to Toolbar
        toolbar.setNavigationIcon(R.drawable.arrow_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        getDataFromFirebase()
    }

    private fun getDataFromFirebase() {
        hardLvlList = ArrayList()
        myReference = FirebaseDatabase.getInstance().reference

        val userId = mAuth!!.currentUser!!.uid
        val userProgressRef = myReference.child("Users").child(userId).child("UserProgress")
        val targetsRef = myReference.child("Hard Level")

        // Kullanıcı ilerlemesini ve hedefleri paralel olarak çek
        userProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userProgressSnapshot: DataSnapshot) {
                targetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(targetsSnapshot: DataSnapshot) {
                        for (snapshot: DataSnapshot in targetsSnapshot.children) {
                            val model = ModelOfHardLvl()
                            model.target = snapshot.child("Target").value.toString()
                            model.targetNumber = snapshot.child("Target Number").value.toString()
                            model.number1 = snapshot.child("Number1").value.toString()
                            model.number2 = snapshot.child("Number2").value.toString()
                            model.number3 = snapshot.child("Number3").value.toString()
                            model.number4 = snapshot.child("Number4").value.toString()

                            model.isUnlocked = when {
                                model.targetNumber.toInt() in 21..30 ->
                                    userProgressSnapshot.child((model.targetNumber.toInt()).toString()).value == true
                                // Other cases
                                else -> false
                            }

                            hardLvlList.add(model)
                        }

                        adapter = AdapterOfHardLvl(this@HardLevelActivity, hardLvlList)
                        recyclerView.adapter = adapter
                        adapter.notifyDataSetChanged()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}