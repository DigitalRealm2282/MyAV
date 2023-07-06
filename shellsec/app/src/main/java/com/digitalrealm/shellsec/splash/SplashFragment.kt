package com.digitalrealm.shellsec.splash

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.digitalrealm.shellsec.MainActivity
import com.digitalrealm.shellsec.R
import com.digitalrealm.shellsec.data.AuthProvider
import com.digitalrealm.shellsec.helper.ThemeToggleHelper.getSharedPreferences
import java.util.concurrent.Executor


class SplashFragment : Fragment() {

    private lateinit var authProvider: AuthProvider
    private lateinit var sharedPreferences: SharedPreferences
    private var biometric = false
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSharedPreferences()

        authProvider = AuthProvider(this,requireContext())
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        biometric = sharedPreferences.getBoolean("pref_biometric_auth", false)

        if (biometric){
            executor = ContextCompat.getMainExecutor(requireContext())
            biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(requireContext(),
                            "Authentication error: $errString", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult,
                    ) {
                        super.onAuthenticationSucceeded(result)
                        Toast.makeText(requireContext(),
                            "Authentication succeeded!", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(requireContext(), "Authentication failed",
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                })

            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build()

            authProvider.confirmBiometricAuth(R.string.verification_required, R.string.verify_to_proceed, onAuthFailed ={
            }, onAuthError = { i, s ->
                //handle auth error message and codes
                authFailed(s,i)
            }) {
                //handle successful authentication
                proceedFurther()
            }

        }else{
            proceedFurther()
        }


    }



    private fun authFailed(stringFail:String,intFail:Int){
        Toast.makeText(requireContext(),stringFail, Toast.LENGTH_SHORT).show()
    }
    private fun setupSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }
    private fun proceedFurther(){
        requireActivity().runOnUiThread {
            Handler().postDelayed({
                val startIntent = Intent(requireContext(), MainActivity::class.java)
                startActivity(startIntent)
                requireActivity().finish()
            }, 2000)
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }


    fun getInstance(username: String?): SplashFragment {
        val bundle = Bundle()
//        bundle.putInt("USERNAME", username)
        val fragment = SplashFragment()
        fragment.arguments = bundle
        return fragment
    }

}