package com.example.user4.sietzeberendspset6;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.HashMap;

public class Quiz extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;

    ArrayList<JSONObject> questionObjects;
    ArrayList<String> answers;
    ArrayAdapter<String> gvAdapter;
    GridView gv;
    JSONArray incorrectAnswers;
    TextView question;
    String questionsUrl;
    Integer currentQuestionNo;
    JSONObject currentQuestion;
    Integer categoryId;
    String difficulty;
    String answer;
    Button answerButton;
    Integer amount;
    RequestQueue queue;
    Integer score;
    Integer highscoreUser;
    HashMap<String, Integer> userScoreObject;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_layout);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        queue = Volley.newRequestQueue(this);

        question = (TextView) findViewById(R.id.question);
        questionObjects = new ArrayList<JSONObject>();
        answers = new ArrayList<String>();
        incorrectAnswers = new JSONArray();
        gv = (GridView) findViewById(R.id.answers);

        setListener();
        loadData();

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

    private void setListener() {
        System.out.println("setlistener");
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    System.out.println("userfound, loaddata");
                    Log.w("signed in", "onAuthStateChanged: signed_in:" + user.getUid());
                }

                else {
                    Log.w("signed out", "onAuthStateChanged: signed_out");
                    logout();
                }
            }
        };
    }
    public void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginRegister.class);
        startActivity(intent);
    }

    private void loadData() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                currentQuestionNo = dataSnapshot.child(user.getUid()).child("currentQuestionNo").getValue(Integer.class);
                if (currentQuestionNo == null) {
                    currentQuestionNo = 0;
                }
                categoryId = dataSnapshot.child(user.getUid()).child("categoryId").getValue(Integer.class);
                difficulty = dataSnapshot.child(user.getUid()).child("difficulty").getValue(String.class);
                amount = Integer.parseInt(dataSnapshot.child(user.getUid()).child("amount").getValue(String.class));
                questionsUrl = "https://opentdb.com/api.php?amount=" + amount + "&category=" + categoryId +
                        "&difficulty=" + difficulty + "&type=multiple";
                System.out.println(questionsUrl);
                questionObjects = (ArrayList<JSONObject>) dataSnapshot.child(user.getUid()).child("questionObjects").getValue();
                if (questionObjects == null) {
                    questionObjects = new ArrayList<JSONObject>();
                    getFromApi(queue);
                }
                score = dataSnapshot.child(user.getUid()).child("score").getValue(Integer.class);
                highscoreUser = dataSnapshot.child(user.getUid()).child("highscoreUser").getValue(Integer.class);
                userScoreObject = (HashMap<String, Integer>) dataSnapshot.child("highscores").getValue();
                if(userScoreObject == null) {
                    userScoreObject = new HashMap<String, Integer>();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getFromApi(RequestQueue queue){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, questionsUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray jsonArray = jsonObject.getJSONArray("results");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject questionObject = jsonArray.getJSONObject(i);
                        questionObjects.add(questionObject);
                    }
                    System.out.println("output size: " + questionObjects.size());

                    if(questionObjects.size() != 0) {
                        showData(questionObjects);
                    }
                    else {
                        String message = "Category/difficulty combination seems to have no questions;";
                        message += ", please choose a different combination";
                        Toast.makeText(Quiz.this, message, Toast.LENGTH_LONG).show();
                        newGame();
                    }



                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("errorlistener");
            }
        });
        queue.add(stringRequest);
    }

    public void showData(ArrayList<JSONObject> input) {
        if (currentQuestionNo == input.size()) {
            double percentage = ((double) score / (double)questionObjects.size() * 100);
            percentage = Math.round(percentage * 100.0) / 100.0;
            question.setText("Good job! In total, you answered " + score + " questions correct out of "
            + questionObjects.size() + " questions, which is " + percentage + "%. Use the menu to start " +
                            "a new game or view the highscores.");
            gvAdapter.clear();

            if (score > highscoreUser) {
                highscoreUser = score;
                userScoreObject.put(user.getUid(), highscoreUser);
                mDatabase.child("highscores").setValue(userScoreObject);
            }

            mDatabase.child(user.getUid()).child("state").setValue(0);
            mDatabase.child(user.getUid()).child("score").setValue(0);
            mDatabase.child(user.getUid()).child("currentQuestionNo").setValue(0);

            return;

        }
        else if (currentQuestionNo != 0){
            currentQuestionNo %= input.size();
            Log.d("size", String.valueOf(input.size()));
            Log.d("afterModulo", currentQuestionNo.toString());
        }
        try {
            System.out.println("CQN: " + currentQuestionNo);
            currentQuestion = input.get(currentQuestionNo);
            question.setText(Html.fromHtml(currentQuestion.getString("question"),Html.FROM_HTML_MODE_LEGACY));
            answers.add(Html.fromHtml(input.get(currentQuestionNo).getString("correct_answer"),Html.FROM_HTML_MODE_LEGACY).toString());
            incorrectAnswers = input.get(currentQuestionNo).getJSONArray("incorrect_answers");
            for (int i = 0; i < incorrectAnswers.length(); i++) {
                answers.add(Html.fromHtml(incorrectAnswers.get(i).toString(),Html.FROM_HTML_MODE_LEGACY).toString());
            }
            gvAdapter = new ArrayAdapter<String>(this, R.layout.answer_row, answers);
            gv.setAdapter(gvAdapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void answer(View view) {
        answerButton = view.findViewById(R.id.chosenAnswer);
        answer = answerButton.getText().toString();
        try {
            String message;
            String correct = Html.fromHtml(currentQuestion.getString("correct_answer"),Html.FROM_HTML_MODE_LEGACY).toString();
            if (answer.equals(correct)) {
                score += 1;
                message = "Answer correct! Score is now: " + score;
                Toast.makeText(Quiz.this, message, Toast.LENGTH_SHORT).show();
                mDatabase.child(user.getUid()).child("score").setValue(score);
            }

            else {
                message = "Answer incorrect! Correct answer was: " + correct;
                Toast.makeText(Quiz.this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        currentQuestionNo += 1;
        mDatabase.child(user.getUid()).child("currentQuestionNo").setValue(currentQuestionNo);
        answers.clear();
        showData(questionObjects);
    }

    public void newGame() {
        mDatabase.child(user.getUid()).child("state").setValue(0);
        mDatabase.child(user.getUid()).child("score").setValue(0);
        mDatabase.child(user.getUid()).child("currentQuestionNo").setValue(0);
        Intent intent = new Intent(this, ConfigureQuiz.class);
        startActivity(intent);
    }

    public void highscores() {
        mDatabase.child(user.getUid()).child("state").setValue(0);
        mDatabase.child(user.getUid()).child("score").setValue(0);
        mDatabase.child(user.getUid()).child("currentQuestionNo").setValue(0);
        Intent intent = new Intent(this, Highscores.class);
        startActivity(intent);
    }
}
