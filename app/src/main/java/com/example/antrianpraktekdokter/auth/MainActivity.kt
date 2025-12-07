package com.example.antrianpraktekdokter.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.DoctorPage.DokterActivity
import com.example.antrianpraktekdokter.R
//import com.facebook.CallbackManager
//import com.facebook.FacebookCallback
//import com.facebook.FacebookException
//import com.facebook.login.LoginManager
//import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Google & Facebook Vars
    private lateinit var googleSignInClient: GoogleSignInClient
    //private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 2. Binding Tombol (Pastikan ID di XML sesuai)
        val btnGoLogin: Button = findViewById(R.id.btnGoLogin)
        val btnGoRegister: Button = findViewById(R.id.btnGoRegister)

        // Tombol Ikon Sosial (Sesuai ID layout Anda sebelumnya)
        val btnGoogle: MaterialButton = findViewById(R.id.btn_google_login)
        val btnFacebook: MaterialButton = findViewById(R.id.btn_facebook_login)
        val btnEmail: MaterialButton = findViewById(R.id.btn_email_login)

        // 3. Navigasi Tombol Standar
        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 4. Logika Tombol Ikon EMAIL
        // Karena login email butuh input manual, kita arahkan ke LoginActivity
        btnEmail.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // =================================================================
        // 5. SETUP GOOGLE LOGIN
        // =================================================================
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // =================================================================
        //  SETUP FACEBOOK LOGIN masih di usahakan
        // =================================================================
//        callbackManager = CallbackManager.Factory.create()
//
//        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
//            override fun onSuccess(result: LoginResult) {
//                handleFacebookAccessToken(result.accessToken)
//            }
//            override fun onCancel() {
//                Toast.makeText(applicationContext, "Login Facebook Dibatalkan", Toast.LENGTH_SHORT).show()
//            }
//            override fun onError(error: FacebookException) {
//                Toast.makeText(applicationContext, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//
//        btnFacebook.setOnClickListener {
//            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
//        }

        // Cek apakah user sudah login sebelumnya? (Auto Login)
        // Jika Anda ingin user langsung masuk tanpa menekan tombol jika sudah login:
        /*
        if (auth.currentUser != null) {
             checkUserRole()
        }
        */
    }

    // --- Helper Functions (Sama seperti logika LoginActivity sebelumnya) ---

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkAndSaveUserToFirestore(auth.currentUser)
                } else {
                    Toast.makeText(this, "Auth Gagal.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleFacebookAccessToken(token: com.facebook.AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkAndSaveUserToFirestore(auth.currentUser)
                } else {
                    Toast.makeText(this, "Auth Gagal.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Penting untuk Facebook Login
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        callbackManager.onActivityResult(requestCode, resultCode, data)
//    }

    // Fungsi: Simpan User Baru ke Firestore (Role Default: Patient)
    private fun checkAndSaveUserToFirestore(user: FirebaseUser?) {
        if (user == null) return
        val docRef = db.collection("users").document(user.uid)

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // User lama -> Cek Role -> Masuk
                checkUserRole()
            } else {
                // User baru -> Simpan data -> Masuk
                val userMap = hashMapOf(
                    "nama" to (user.displayName ?: "User"),
                    "email" to (user.email ?: ""),
                    "role" to "patient"
                )
                docRef.set(userMap).addOnSuccessListener {
                    checkUserRole()
                }
            }
        }
    }

    // Fungsi: Arahkan User Sesuai Role
    private fun checkUserRole() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nama = document.getString("nama") ?: "User"
                    val role = document.getString("role") ?: "patient"

                    val intent = when (role) {
                        "doctor" -> Intent(this, com.example.antrianpraktekdokter.doctor.DoctorHomeActivity::class.java)
                        "admin" -> Intent(this, com.example.antrianpraktekdokter.admin.AdminHomeActivity::class.java)
                        else -> Intent(this, com.example.antrianpraktekdokter.patient.DashboardActivity::class.java)
                    }
                    intent.putExtra("nama", nama)
                    intent.putExtra("role", role)
                    startActivity(intent)
                    finish() // Tutup MainActivity agar tidak bisa kembali
                }
            }
    }
}