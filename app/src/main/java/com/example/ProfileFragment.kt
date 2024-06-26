package com.example

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.healthzensignuplogin.LogInActivity
import com.example.healthzensignuplogin.MyPostsActivity
import com.example.healthzensignuplogin.R
import com.example.healthzensignuplogin.SignUpActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class ProfileFragment : Fragment() {

    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var profileUsername: TextView
    private lateinit var profilePassword: TextView
    private lateinit var titleName: TextView
    private lateinit var titleUsername: TextView
    private lateinit var logoutButton: Button
    private lateinit var deleteButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var editButton: Button
    private lateinit var postsNumber:TextView

    private lateinit var profilePic: ImageView
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var getContent: ActivityResultLauncher<String>
    private lateinit var progressBar: ProgressBar


    // Add variables to store user data
    private var nameUser: String? = null
    private var emailUser: String? = null
    private var usernameUser: String? = null
    private var passwordUser: String? = null

    private var imageUri: Uri = Uri.EMPTY
    private var profilePicUrlUser: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        logoutButton = view.findViewById(R.id.logoutButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        editButton=view.findViewById(R.id.editButton)
        postsNumber=view.findViewById(R.id.postsNumber)

        profilePic =view.findViewById(R.id.profileImg)

        progressBar = view.findViewById(R.id.progressBar)


        storage = FirebaseStorage.getInstance()
        storageReference = storage.getReference()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // Handle the selected image URI here
                imageUri = uri
                profilePic.setImageURI(imageUri)
                uploadPicture()
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileName = view.findViewById(R.id.profileName)
        profileEmail = view.findViewById(R.id.profileEmail)
        profileUsername = view.findViewById(R.id.profileUsername)
        profilePassword = view.findViewById(R.id.profilePassword)
        titleName = view.findViewById(R.id.titleName)
        titleUsername = view.findViewById(R.id.titleUsername)

        val sharedPref = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")
        val username = sharedPref.getString("username", "")
        val password = sharedPref.getString("password", "")
        val profilePicUrl = sharedPref.getString("profilePicUrl", "")


        // Set user data to TextViews
        setUserData(name, email, username, password, profilePicUrl)

        logoutButton.setOnClickListener {
            // Build an AlertDialog to confirm logout
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // If the user confirms logout, sign out
                    auth.signOut()
                    // Show a toast message indicating successful logout
                    Toast.makeText(activity, "You have successfully logged out. See you again soon.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(activity, LogInActivity::class.java))
                    activity?.finish()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    // If the user cancels logout, dismiss the dialog
                    dialog.dismiss()
                }
                .show()
        }

        //navigate to my post
        postsNumber.setOnClickListener {
            startActivity(Intent(activity,MyPostsActivity::class.java))
        }



        val userId = FirebaseAuth.getInstance().currentUser?.uid
       //fetch the number of posts from firestore
        if(userId!=null){
            firestore.collection("posts")
                .whereEqualTo("userId",userId)
                .get()
                .addOnSuccessListener { documents->
                    val postCount=documents.size()

                    postsNumber.text= postCount.toString()
                }
        }




        deleteButton.setOnClickListener {
            // Build an AlertDialog to confirm account deletion
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("All your data will not be recovered after account deletion. Are you sure you want to delete your account? ")
                .setPositiveButton("Yes") { dialog, _ ->
                    // If the user confirms deletion, delete the account
                    deleteUserAccount()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    // If the user cancels deletion, dismiss the dialog
                    dialog.dismiss()
                }
                .show()
        }

        editButton.setOnClickListener {
            val builder= androidx.appcompat.app.AlertDialog.Builder(requireContext())
            val view=layoutInflater.inflate(R.layout.dialog_changeusername,null)
            val newUserName=view.findViewById<EditText>(R.id.editBox)

            builder.setView(view)
            val dialog=builder.create()


            view.findViewById<Button>(R.id.btnDone).setOnClickListener {
                changeUserName(newUserName)

                dialog.dismiss()
            }
            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }
            if (dialog.window!=null){
                dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
            }
            dialog.show()

        }
        profilePic.setOnClickListener {
            choosePicture()
        }
    }

    private fun choosePicture() {
        getContent.launch("image/*")
    }

    private fun uploadPicture() {
        if (imageUri == Uri.EMPTY) { // Check if imageUri is empty
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        val randomKey = UUID.randomUUID().toString()
        val imagesRef = storageReference.child("images/$randomKey")

        imagesRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                progressBar.visibility = View.GONE
                view?.let { rootView ->
                    Snackbar.make(rootView, "Image Uploaded", Snackbar.LENGTH_LONG).show()
                }
                imagesRef.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrlToFirestore(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to Upload: ${exception.message}", Toast.LENGTH_LONG).show()
            }
            .addOnProgressListener { taskSnapshot ->
                val progressPercent = (100.00 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                updateProgress(progressPercent)
            }
    }

    private fun saveImageUrlToSharedPreferences(imageUrl: String) {
        val sharedPref = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("profilePicUrl", imageUrl)
        editor.apply()
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userRef = firestore.collection("users").document(userId)
            userRef.update("profilePicUrl", imageUrl)
                .addOnSuccessListener {
                    // Profile picture URL saved successfully
                }
                .addOnFailureListener { e ->
                    // Failed to save profile picture URL
                }
        }
        saveImageUrlToSharedPreferences(imageUrl)
    }
    private fun updateProgress(progressPercent: Int) {
        progressBar.progress = progressPercent
    }


    //change username
    private fun changeUserName(newName:EditText) {
        if (newName.text.toString().isNotEmpty()) {
            auth= FirebaseAuth.getInstance()

            val currentUser=auth.currentUser
            if (currentUser!=null){
                val userId=auth.currentUser!!.uid


                val newusername=newName.text.toString()
                val updateMap= mapOf(
                    "username" to newusername
                )
                firestore.collection("users").document(userId)
                    .update(updateMap)
                    .addOnSuccessListener {
                        Toast.makeText(
                            activity,
                            "Changed username successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        setUserData(nameUser, emailUser, newusername, passwordUser, profilePicUrlUser)

                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            activity,
                            "Failed to change username:${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }else{
                Toast.makeText(
                    activity,
                    "User not logged in"
                    ,
                    Toast.LENGTH_SHORT
                ).show()
            }}
        else {
            Toast.makeText(
                activity,
                "field cannot be empty",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    private fun deleteUserAccount() {
        val user = auth.currentUser
        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If the account is successfully deleted, delete user data from Firestore
                    val userId = user.uid
                    firestore.collection("users").document(userId)
                        .delete()
                        .addOnSuccessListener {
                            // Account deletion and user data deletion successful
                            Toast.makeText(
                                activity,
                                "Account deleted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Redirect to the login screen
                            startActivity(Intent(activity, SignUpActivity::class.java))
                            activity?.finish()
                        }
                        .addOnFailureListener { e ->
                            // Failed to delete user data from Firestore
                            Toast.makeText(
                                activity,
                                "Failed to delete user data: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // Failed to delete account
                    Toast.makeText(
                        activity,
                        "Failed to delete account: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }



    // Function to set user data
    private fun setUserData(name: String?, email: String?, username: String?, password: String?, profilePicUrl: String?) {
        nameUser = name
        emailUser = email
        usernameUser = username
        passwordUser = password
        profilePicUrlUser = profilePicUrl

        // Update UI with user data
        showUserData()
    }

    // Function to update UI with user data
    private fun showUserData() {
        // Check if TextViews are initialized
        if (::titleName.isInitialized && ::titleUsername.isInitialized &&
            ::profileName.isInitialized && ::profileEmail.isInitialized &&
            ::profileUsername.isInitialized && ::profilePassword.isInitialized&&
            ::profilePic.isInitialized) {

            val sharedPref = requireActivity().getSharedPreferences("UserData", Context.MODE_PRIVATE)
            val profilePicUrl = sharedPref.getString("profilePicUrl", "")

            // Set user data to TextViews
            titleName.text = nameUser
            titleUsername.text = usernameUser
            profileName.text = nameUser
            profileEmail.text = emailUser
            profileUsername.text = usernameUser
            profilePassword.text = "********"
            // Retrieve profile picture URL from SharedPreferences

            // Load profile picture if URL is not empty
            if (!profilePicUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profilePicUrl)
                    .into(profilePic)
            } else {
                profilePic.setImageResource(R.drawable.photologo)
            }
        }
    }
}