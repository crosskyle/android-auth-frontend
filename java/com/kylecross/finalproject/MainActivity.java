/*
Author: Kyle Cross
Date: 8-14-17
Description: The main activity in a CRUD app that uses OAuth 2.0 through Google Plus and makes HTTP calls
to a REST backend stored in a Google Cloud datastore.
 */

package com.kylecross.finalproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView logInItem = (TextView) findViewById(R.id.Layout1);
        logInItem.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CarActivity.class);
                startActivity(intent);
            }
        });
    }
}