package com.example.user4.sietzeberendspset6;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Highscores extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;

    ArrayList<String> users;
    HashMap <String, Integer> highscoresDb;
    ArrayList <Map.Entry<String, Integer>> listscores;
    ArrayList <String> listScoresSorted;
    ListView highscores;
    ArrayAdapter<String> hsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.highscores_layout);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        users = new ArrayList<String>();
        highscores = (ListView) findViewById(R.id.highscores);
        highscoresDb = new HashMap<String, Integer>();
        listScoresSorted = new ArrayList<String>();

        setListener();
    }

    /**
     * sets the AuthStateListener and calls loadHighscores
     */
    private void setListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    loadHighscores();
                }

                else {
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
                break;
            case R.id.highscores:
                highscores();
                break;
            case R.id.newGame:
                newGame();
                break;
        }
        return super.onOptionsItemSelected(item);
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


    @Override
    public void onBackPressed() {
    }

    public void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginRegister.class);
        startActivity(intent);
    }

    /**
     * We're already in the highscores activity
     */
    public void highscores() {
        return;
    }

    /**
     * Resets some parameters and let's the user configure a new game
     */
    public void newGame() {
        mDatabase.child(user.getUid()).child("state").setValue(0);
        mDatabase.child(user.getUid()).child("score").setValue(0);
        mDatabase.child(user.getUid()).child("currentQuestionNo").setValue(0);
        Intent intent = new Intent(this, ConfigureQuiz.class);
        startActivity(intent);
    }

    /**
     * Loads the highscores from firebase, sorts them descending and calls adaptScore
     */
    public void loadHighscores() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mDatabase.child("highscores").orderByValue();
                highscoresDb = (HashMap<String, Integer>) dataSnapshot.child("highscores").getValue();
                listscores = new ArrayList<Map.Entry<String, Integer>>(highscoresDb.entrySet());
                Collections.sort(listscores, new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                        int oo1 = Integer.parseInt(String.valueOf(o1.getValue()));
                        int oo2 = Integer.parseInt(String.valueOf(o2.getValue()));
                        return ((Integer) oo2).compareTo((Integer) oo1);
                    }
                });

                for(int i = 0; i < listscores.size(); i++) {
                    String email = dataSnapshot.child(listscores.get(i).getKey()).child("email").getValue(String.class);
                    listScoresSorted.add((i+1) + ". " + email + ": " + listscores.get(i).getValue() + " points");
                }
                adaptScore();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Adapts the score in a listview
     */
    public void adaptScore() {
        hsAdapter = new ArrayAdapter<String>(this, R.layout.highscore_row, listScoresSorted);
        highscores.setAdapter(hsAdapter);
    }
}
