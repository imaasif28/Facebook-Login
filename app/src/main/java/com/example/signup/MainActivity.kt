package com.example.signup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.fbsignup.R
import com.example.signup.callbacks.GetUserCallback
import com.example.signup.callbacks.PermissionCallback
import com.example.signup.requests.UserRequest
import com.facebook.*
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.fbloginsample.entities.User
import com.facebook.fbloginsample.requests.PermissionRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.material.snackbar.Snackbar
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity(), GetUserCallback.IGetUserResponse,
    PermissionCallback.IPermissionResponse, AccessToken.AccessTokenRefreshCallback {

    private val cbManager = CallbackManager.Factory.create()
    private var token = ""

    companion object {
        const val TAG = "FacebookLogin"
        private const val PUBLIC_PROFILE = "public_profile"
        private const val EMAIL = "email"
        private const val AUTH_TYPE = "rerequest"
        private const val APP = "app"

        /*   private const val USER_POSTS = "user_posts"
           private const val RESULT_PROFILE_ACTIVITY = 1
           private const val RESULT_POSTS_ACTIVITY = 2
           private const val RESULT_PERMISSIONS_ACTIVITY = 3
           private const val DEFAULT_FB_APP_ID = "ENTER_YOUR_FB_APP_ID_HERE"
           private const val DEFAULT_FB_CLIENT_COKEN = "ENTER_YOUR_FB_CLIENT_TOKEN_HERE"*/
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialise()
        FacebookSdk.sdkInitialize(applicationContext)
//        AppEventsLogger.activateApp(Application())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        cbManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun initialise() {
        val view = findViewById<TextView>(R.id.textView)
        val data = findViewById<TextView>(R.id.changeView)
        val button = findViewById<AppCompatButton>(R.id.button)
        val loginButton = findViewById<LoginButton>(R.id.login_button)
        printHashKey(this)

        // Make a logout button
        button.setOnClickListener {
            if (button.text == "Logout") {
                LoginManager.getInstance().logOut()
                snackBar("Logout Successfully")
                button.text = "Token"
                view.text = "Status"
                data.text = "Data"
            } else {
                toast("last token: $token")
            }
        }

//        loginButton.permissions = listOf(EMAIL)
        loginButton.authType = AUTH_TYPE


        // Register a callback to respond to the user
        loginButton.registerCallback(cbManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    setResult(RESULT_OK)
                    val loginStatus = getCurrentAccessToken() != null
                            && getCurrentAccessToken()?.isExpired?.not() == true
                    token = result.accessToken.token
                    permissionCheck(result)
                    //request
                    UserRequest.makeUserRequest(GetUserCallback(this@MainActivity).callback)
                    view.text =
                        "Token expired ${result.accessToken.isDataAccessExpired}, is User Logged In : $loginStatus "
                    button.text = "Logout"
//                    finish()
                }

                override fun onCancel() {
                    setResult(RESULT_CANCELED)
//                    finish()
                }

                override fun onError(error: FacebookException) {
                    // Handle exception
                }
            })
    }

    private fun permissionCheck(result: LoginResult) {
        // Set the initial permissions to request from the user while logging in
        val permissions = result.accessToken.permissions

        //Email permission
        if (!permissions.containsAll(listOf(PUBLIC_PROFILE, EMAIL))) {
            // Make request to user to grant email permission
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf(PUBLIC_PROFILE, EMAIL))
        } /*else if (permissions.contains(EMAIL)) {
            // Make revoke email permission request
            PermissionRequest.makeRevokePermRequest(
                EMAIL,
                PermissionCallback(this).callback
            )
        }*/

        //login permission
        // Make request to user to login
//        if (getCurrentAccessToken() == null) LoginManager.getInstance()
//            .logInWithReadPermissions(
//                this@MainActivity,
//                listOf(PUBLIC_PROFILE)
//            ) else PermissionRequest.makeRevokePermRequest(
//            APP,
//            PermissionCallback(this).callback
//        )
    }

    override fun onCompleted(user: User) {
        val view = findViewById<TextView>(R.id.changeView)
        view.text =
            user.name + "\n" + user.permissions + "\nUser ID: " + user.id + "\nEmail: " + user.email
        snackBar(user.name + " details found")
    }

    override fun OnTokenRefreshFailed(exception: FacebookException?) {
        // Handle exception ...
    }

    override fun OnTokenRefreshed(accessToken: AccessToken?) {
        val view = findViewById<TextView>(R.id.textView)
        if (accessToken != null) {
            view.text = accessToken.token
        }

    }

    override fun onCompleted(response: GraphResponse?) {
        response?.error?.let {
            snackBar("Error with permissions request: " + it.errorMessage)
        }
        AccessToken.refreshCurrentAccessTokenAsync(this)
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun printHashKey(pContext: Context) {
        try {
            val info: PackageInfo = pContext.packageManager
                .getPackageInfo(pContext.packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i(TAG, "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "printHashKey()", e)
        } catch (e: Exception) {
            Log.e(TAG, "printHashKey()", e)
        }
    }

    private fun snackBar(msg: String) {
        Snackbar.make(this.findViewById(R.id.textView), msg, Snackbar.LENGTH_INDEFINITE)
            .let { snack ->
                snack.setAction("ok") { snack.dismiss() }
            }.show()
    }

}