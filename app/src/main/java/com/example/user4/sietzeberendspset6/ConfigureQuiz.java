package com.example.user4.sietzeberendspset6;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ConfigureQuiz extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;

    RequestQueue queue;
    String categoriesUrl;
    ArrayList<String> categories;
    ArrayList<String> difficulties;
    ArrayList<String> numbers;
    Button confirm;
    Button choiceUp;
    Button choiceRight;
    TextView choice;
    TextView todo;
    Integer currentChoice;
    Integer state;
    JSONArray categoriesJson;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure_quiz_layout);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        confirm = (Button) findViewById(R.id.confirm);
        choice = (TextView) findViewById(R.id.choice);
        todo = (TextView) findViewById(R.id.todo);
        queue = Volley.newRequestQueue(this);
        categoriesUrl = "https://opentdb.com/api_category.php";
        choiceUp = (Button) findViewById(R.id.choice_up);
        choiceRight = (Button) findViewById(R.id.choice_down);
        categoriesJson = new JSONArray();

        setListener();

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
     * Loads the current state and highscore if possible, also loads the current question
     */
    private void setListener() {

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                }

                else {
                }
            }
        };
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                state = dataSnapshot.child(user.getUid()).child("state").getValue(Integer.class);
                if(dataSnapshot.child(user.getUid()).child("highscoreUser").getValue(Integer.class) == null) {
                    mDatabase.child(user.getUid()).child("highscoreUser").setValue(0);
                }
                currentChoice = dataSnapshot.child(user.getUid()).child("currentChoice").getValue(Integer.class);
                if (currentChoice == null) {
                    currentChoice = 0;
                }
                updateUI(user);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
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
     * Disable back navigation. Navigation is done with the Action Bar instead
     */
    @Override
    public void onBackPressed() {
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("currentChoice", currentChoice);
    }

    /**
     * Remembers the current choice (category, difficulty or amount of questions)
     * @param savedInstanceState
     */
    @Override
    public void onRestoreInstanceState (Bundle savedInstanceState) {
        currentChoice = savedInstanceState.getInt("currentChoice");
        getFromApi(queue);
    }

    public void logout() {
        mAuth.signOut();
        updateUI(null);
    }

    /**
     * Updates the UI based on the state. If there's no user, redirect to login page
     *
     * @param user
     */
    public void updateUI(FirebaseUser user) {
        if (currentChoice == null) {
            currentChoice = 0;
            mDatabase.child(user.getUid()).child("currentChoice").setValue(currentChoice);
        }

        if(user == null) {
            String message = "Log the user out";
            Intent intent = new Intent(this, LoginRegister.class);
            startActivity(intent);
        }
        else{
            if (state == null) {
                state = 0;
            }
            mDatabase.child(user.getUid()).child("email").setValue(user.getEmail());

            switch(state) {
                // choose category
                case 0:
                    todo.setText("What category?");
                    if(categories == null) {
                        getFromApi(queue);
                    }
                    else{
                        if(categories.size() == 0) {
                            getFromApi(queue);
                        }
                        else {
                            showData(categories);
                        }
                    }
                    break;
                // choose difficulty
                case 1:
                    todo.setText("What difficulty?");
                    difficulties = new ArrayList<String>();
                    difficulties.add("Easy");
                    difficulties.add("Medium");
                    difficulties.add("Hard");
                    showData(difficulties);
                    break;
                // choose amount of questions
                case 2:
                    todo.setText("How many questions?");
                    numbers = new ArrayList<String>();
                    for(int i = 1; i <= 50; i++) {
                        numbers.add(String.valueOf(i));
                    }
                    showData(numbers);
                    break;

                case 3:
                    Intent intent = new Intent(this, Quiz.class);
                    startActivity(intent);
                    break;
            }
        }
    }

    /**
     * Choose a different category, difficulty or amount of questions.
     * @param view
     */
    public void changeChoice(View view) {
        ArrayList<String> input = new ArrayList<String>();
        switch (state) {
            case 0:
                input = categories;
                break;
            case 1:
                input = difficulties;
                break;
            case 2:
                input = numbers;
                break;
        }

        switch (view.getId()) {

            // go down
            case R.id.choice_down:
                if (currentChoice == 0) {
                    currentChoice = input.size() - 1;
                    mDatabase.child(user.getUid()).child("currentChoice").setValue(currentChoice);
                }
                else {
                    currentChoice -= 1;
                    mDatabase.child(user.getUid()).child("currentChoice").setValue(currentChoice);
                }
                showData(input);
                break;

            // go up
            case R.id.choice_up:
                currentChoice += 1;
                mDatabase.child(user.getUid()).child("currentChoice").setValue(currentChoice);
                showData(input);
                break;
        }
    }

    /**
     * Confirm category, difficulty or amount of questions
     * @param view
     */
    public void confirmChoice(View view) {
        switch(state) {

            // save category, go to difficulty
            case 0:
                state += 1;
                mDatabase.child(user.getUid()).child("state").setValue(state);

                Integer categoryId = 0;
                String category = choice.getText().toString();
                for (int i = 0; i < categoriesJson.length(); i++) {
                    try {
                        JSONObject categoryJson = categoriesJson.getJSONObject(i);
                        if(categoryJson.getString("name").equals(category)) {
                            categoryId = categoryJson.getInt("id");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                mDatabase.child(user.getUid()).child("categoryId").setValue(categoryId);
                updateUI(user);
                break;

            // save difficulty, go to amount of questions
            case 1:
                state += 1;
                mDatabase.child(user.getUid()).child("state").setValue(state);
                mDatabase.child(user.getUid()).child("difficulty").setValue(choice.getText().toString().toLowerCase());
                updateUI(user);
                break;

            // save amount of questions, go to the quiz
            case 2:
                state += 1;
                mDatabase.child(user.getUid()).child("state").setValue(state);

                mDatabase.child(user.getUid()).child("amount").setValue(String.valueOf(choice.getText()));

                mDatabase.child(user.getUid()).child("score").setValue(0);

                updateUI(user);
                break;
        }
    }

    /**
     * Gets all possible categories from the OpenTDB API
     * @param queue
     */
    public void getFromApi(RequestQueue queue){
        categories = new ArrayList<String>();
        categoriesJson = new JSONArray();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, categoriesUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray jsonArray = jsonObject.getJSONArray("trivia_categories");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject category = jsonArray.getJSONObject(i);
                        String categoryString = category.getString("name");
                        categoriesJson.put(category);
                        categories.add(categoryString);
                    }
                    showData(categories);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        queue.add(stringRequest);
    }

    /**
     * Shows the data with which the user can configure a quiz in the UI. Can be a category,
     * difficulty or amount
     * @param input
     */
    public void showData(ArrayList<String> input) {
        if (currentChoice != 0){
            currentChoice %= input.size();
            mDatabase.child(user.getUid()).child("currentChoice").setValue(currentChoice);
        }
        choice.setText(Html.fromHtml(input.get(currentChoice),Html.FROM_HTML_MODE_LEGACY));
        }

    /**
     * Action bar option, resets parameters and let's the user configure a new game
     */
    public void newGame() {
        mDatabase.child(user.getUid()).child("state").setValue(0);
        mDatabase.child(user.getUid()).child("score").setValue(0);
        mDatabase.child(user.getUid()).child("currentQuestionNo").setValue(0);
        Intent intent = new Intent(this, ConfigureQuiz.class);
        startActivity(intent);
    }

    /**
     * Action bar option, resets parameters and show's the highscore
     */
    public void highscores() {
        mDatabase.child(user.getUid()).child("state").setValue(0);
        mDatabase.child(user.getUid()).child("score").setValue(0);
        mDatabase.child(user.getUid()).child("currentQuestionNo").setValue(0);
        Intent intent = new Intent(this, Highscores.class);
        startActivity(intent);
    }
}