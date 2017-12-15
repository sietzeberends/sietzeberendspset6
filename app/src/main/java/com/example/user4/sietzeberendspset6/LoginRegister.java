package com.example.user4.sietzeberendspset6;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * LoginRegister.java - a class used to login an existing user or register a new one.
 */
public class LoginRegister extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;
    private FirebaseUser user;

    String email;
    String password;
    Button login;
    Button register;
    EditText getEmail;
    EditText getPassword;
    Integer state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_register_layout);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        getEmail = (EditText) findViewById(R.id.getEmail);
        getPassword = (EditText) findViewById(R.id.getPassword);
        login = (Button) findViewById(R.id.login);
        register = (Button) findViewById(R.id.register);

        setListener();

    }

    /**
     * Disable back navigation. Navigation is done with the Action Bar instead
     */
    @Override
    public void onBackPressed() {
    }

    /**
     * sets the AuthStateListener to check if a user is logged in already. If so, it will call
     * setStateListener()
     */
    private void setListener() {

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    Log.w("signed in", "onAuthStateChanged: signed_in:" + user.getUid());
                    setStateListener();
                }

                else {
                    Log.w("signed out", "onAuthStateChanged: signed_out");
                }
            }
        };
    }

    /**
     * Gets the state the user was in the last time he or she used the application.
     * Calls updateUI() when it has got the state
     */
    private void setStateListener() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                state = dataSnapshot.child(user.getUid()).child("state").getValue(Integer.class);
                if (state == null) {
                    state = 0;
                }
                updateUI(user);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /**
     * sets the AuthStateListener when OnCreate is finished
     */
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    /**
     * removes the AuthStateListener when the activity is stopped
     */
    @Override
    public void onStop() {
        super.onStop();

        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    /**
     * Registers a user when the register button is clicked, also does some error checking for
     * password and email
     * @param view
     */
    public void register(View view) {
        email = getEmail.getText().toString();
        password = getEmail.getText().toString();
        if((email.isEmpty() || email == null) || (password.isEmpty() || password == null)) {
            String message = "Email or password missing, please enter both.";
            Toast.makeText(LoginRegister.this, message, Toast.LENGTH_LONG).show();
            return;
        }

        else{
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.w("create user", "createUserWithEmail:success");

                            // Register account failure, give error message to user
                            if(!task.isSuccessful()) {
                                String message = "Email or password already exists or are invalid. ";
                                message += " Please enter a valid email-address and make sure that ";
                                message += "your password is at least 6 characters";
                                Toast.makeText(LoginRegister.this, message, Toast.LENGTH_LONG).show();
                                updateUI(null);
                            }

                            // Register account succes, give message to user
                            else {
                                String message = "Account created succesfully, please login.";
                                updateUI(null);
                                Toast.makeText(LoginRegister.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * Logs a user in.
     * Calls UpdateUI or setStateListener depending on whether the login was succesful
     * @param view
     */
    public void login(View view) {
        email = getEmail.getText().toString();
        password = getEmail.getText().toString();

        if((email.isEmpty() || email == null) || (password.isEmpty() || password == null)) {
            String message = "Email or password missing, please enter both.";
            Toast.makeText(LoginRegister.this, message, Toast.LENGTH_LONG).show();
            return;
        }

        else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                setStateListener();
                            } else {
                                String message = "Login failed, username or password incorrect";
                                Toast.makeText(LoginRegister.this, message, Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        }
                    });
        }
    }

    /**
     * Updates the user interface to show the correct screen, based on the state the user is in.
     * Clears the email and password box if the login wasn't succesful in order to let the user
     * try again or register a new account.
     * @param user: a FirebaseUser that is currently logged in
     */
    public void updateUI(FirebaseUser user) {
        if (user != null) {
            System.out.println("State is: " + state);
            Intent intent;
            if (state == 3) {
                intent = new Intent(this, Quiz.class);
                startActivity(intent);
            }
            else {
                intent = new Intent(this, ConfigureQuiz.class);
                startActivity(intent);
            }
        }

        else {
            getEmail.setText("");
            getPassword.setText("");
        }
    }

}
