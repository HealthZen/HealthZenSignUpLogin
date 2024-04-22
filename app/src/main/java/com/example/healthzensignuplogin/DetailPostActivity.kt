package com.example.healthzensignuplogin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.healthzensignuplogin.MyPostDataClass
import com.example.healthzensignuplogin.R
import com.google.firebase.auth.FirebaseAuth

class DetailPostActivity : AppCompatActivity() {
    private lateinit var editPostButton:Button
    private lateinit var deletePostButton:Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore:FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_post)
        editPostButton=findViewById(R.id.editPostButton)
        deletePostButton=findViewById(R.id.deletePostButton)
        firebaseAuth=FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.commentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Retrieve postId from intent extra
        val postId = intent.getStringExtra("postId")

        // Retrieve post details based on postId (from database or elsewhere)
        getPostDetails(postId) { posts ->
            // Display post details in TextViews
//            findViewById<TextView>(R.id.textViewPoster).text = post?.poster  // Set poster first
            findViewById<TextView>(R.id.textViewPostTitle).text = posts?.posttitle
            findViewById<TextView>(R.id.textViewPostContent).text = posts?.postcontent
            findViewById<TextView>(R.id.postDate).text = posts?.date
        }


        editPostButton.setOnClickListener {
            val intent = Intent(this, EditMyPostActivity::class.java)
            intent.putExtra("postId", postId)
            startActivity(intent)
        }

        deletePostButton.setOnClickListener {
            val postId = postId ?: return@setOnClickListener
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Delete")
            builder.setMessage("Are you sure you want to delete this post?")
            builder.setPositiveButton("Yes") { dialog, which ->
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("posts").document(postId)
                        .delete()
                        .addOnSuccessListener {

                            val commentRef= db.collection("posts").document(postId)
                            commentRef.collection("comments")
                                .whereEqualTo("postId", postId)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    val batch = db.batch()
                                    for (document in querySnapshot.documents) {
                                        batch.delete(document.reference)
                                    }
                                    batch.commit()
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this@DetailPostActivity,
                                                "Post and comments deleted successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            startActivity(
                                                Intent(
                                                    this@DetailPostActivity,
                                                    MyPostsActivity::class.java
                                                )
                                            )
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                this@DetailPostActivity,
                                                "Failed to delete post: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this@DetailPostActivity,
                                        "Failed to delete post: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        }


                }
                else {
                    Toast.makeText(this@DetailPostActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                }


            }
            builder.setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }



        //retrieve comments

        firestore = FirebaseFirestore.getInstance()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && postId != null) {
            // Retrieve comments
            val postRef = firestore.collection("posts").document(postId)
            postRef.collection("comments")
                .get()
                .addOnSuccessListener { documents ->
                    val comments = mutableListOf<Comment>()
                    val repliesMap = mutableMapOf<String, List<RepliedComment>>()
                    for (document in documents) {
                        val commentId = document.id
                        val commentAuthor = document.getString("commentAuthor") ?: ""
                        val commentContent = document.getString("commentContent") ?: ""
                        val commentAuthorId = document.getString("commentAuthorId") ?: ""
                        val timestamp = document.getTimestamp("timestamp")
                        val timestampString = timestamp?.toDate()?.toString() ?: ""

// Create the comment object
                        val comment = Comment(
                            commentAuthorId,
                            commentContent,
                            timestampString,
                            commentId,
                            postId,
                            commentAuthor
                        )

// Retrieve replies for this comment
                        val repliesRef = postRef.collection("comments").document(commentId)
                            .collection("replies")
                        repliesRef.get()
                            .addOnSuccessListener { repliedDocuments ->
                                val replies = mutableListOf<RepliedComment>()
                                for (repliedDocument in repliedDocuments) {
                                    // Retrieve replied comments data
                                    val repliedAuthor = repliedDocument.getString("repliedAuthor") ?: ""
                                    val repliedContent = repliedDocument.getString("repliedContent") ?: ""
                                    val repliedAuthorId = repliedDocument.getString("repliedAuthorId") ?: ""
                                    val repliedTimestamp = repliedDocument.getTimestamp("timestamp")
                                    val repliedTimestampString = repliedTimestamp?.toDate()?.toString() ?: ""
                                    val parentCommentId = repliedDocument.getString("parentCommentId") ?: ""
                                    val postId = repliedDocument.getString("postId") ?: ""

                                    // Create the replied comment object
                                    val repliedComment = RepliedComment(
                                        repliedAuthor = repliedAuthor,
                                        repliedContent = repliedContent,
                                        repliedAuthorId = repliedAuthorId,
                                        repliedDate = repliedTimestampString,
                                        parentCommentId = parentCommentId,
                                        postId = postId
                                    )
                                    replies.add(repliedComment)
                                }

                                // Add replies to the map
                                repliesMap[commentId] = replies
                                // Add comment to the list
                                comments.add(comment)

                                // Notify adapter if necessary
                                commentAdapter = CommentAdapter(comments, repliesMap)
                                recyclerView.adapter = commentAdapter
                                commentAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { exception ->
                                // Handle errors
                                exception.printStackTrace()
                            }
                    }
                }

        }

    }

    // Function to retrieve post details from database or elsewhere
    private fun getPostDetails(postId: String?, callback: (MyPostDataClass?) -> Unit) {
        if (postId == null) {
            callback(null)
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("posts")
            .document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val postTitle = document.getString("postTitle") ?: ""
                    val postContent = document.getString("postContent") ?: ""
                    val poster = document.getString("poster") ?: ""
                    val timestamp = document.getTimestamp("timestamp")
                    val timestampString = timestamp?.toDate()?.toString() ?: ""

                    val post = MyPostDataClass( postTitle, postContent, poster,postId,timestampString)
                    callback(post)
                } else {
                    // Post not found or document doesn't exist
                    // Handle error or return null/default post
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                // Handle errors
                exception.printStackTrace()
                callback(null)
            }
    }
}
