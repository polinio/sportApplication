package com.example.sportapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.databinding.ActivityLoginBinding

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val reqCode:Int=123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //
        firebaseAuth = FirebaseAuth.getInstance()

        //
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient= GoogleSignIn.getClient(this,gso)

        binding.signIn.setOnClickListener{
            Toast.makeText(this,"Выполняется вход...", Toast.LENGTH_SHORT).show()
            signInWithGoogle()
        }

        binding.signInEmail.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInWithEmail(email, password)
            } else {
                Toast.makeText(this, "Введите email и пароль!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.register.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password)
            } else {
                Toast.makeText(this, "Введите email и пароль!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val user = firebaseAuth.currentUser
        if(GoogleSignIn.getLastSignedInAccount(this)!=null || user != null){
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        }
    }

    // вход по гугл
    private  fun signInWithGoogle(){
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent,reqCode)
    }

    // регистрация по email и passwd
    private fun registerUser(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // вход по email и passwd
    private fun signInWithEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==reqCode){
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>){
        try {
            val account: GoogleSignInAccount? =completedTask.getResult(ApiException::class.java)
            if (account != null) {
                updateUI(account)
            }
        } catch (e: ApiException){
            Toast.makeText(this,e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount){
        val credential= GoogleAuthProvider.getCredential(account.idToken,null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {task->
            if(task.isSuccessful) {
                //account.email.toString() <--- the user's email
                //account.displayName.toString()) <--- the user's name
                val intent = Intent(this, SplashActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}