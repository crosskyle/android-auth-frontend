/*
Author: Kyle Cross
Date: 8-14-17
Description: An activity that allows a user to update or delete a car and its properties.
*/

package com.kylecross.finalproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CarItemActivity extends AppCompatActivity {
    private OkHttpClient mOkHttpClient;
    private String carId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_item);

        Bundle bundle = getIntent().getExtras();

        carId = bundle.getString("id");

        EditText makeEditText = (EditText)findViewById(R.id.edit_make_edit_text);
        makeEditText.setText(bundle.getString("make"), TextView.BufferType.EDITABLE);

        EditText modelEditText = (EditText)findViewById(R.id.edit_model_edit_text);
        modelEditText.setText(bundle.getString("model"), TextView.BufferType.EDITABLE);

        EditText yearEditText = (EditText)findViewById(R.id.edit_year_edit_text);
        yearEditText.setText(bundle.getString("year"), TextView.BufferType.EDITABLE);

        EditText colorEditText = (EditText)findViewById(R.id.edit_color_edit_text);
        colorEditText.setText(bundle.getString("color"), TextView.BufferType.EDITABLE);


        ((Button)findViewById(R.id.update_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    updateCar();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        ((Button)findViewById(R.id.delete_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    deleteCar();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }


    void updateCar () {
        MediaType JSON = MediaType.parse("application/json");
        String makeInput = ((EditText)findViewById(R.id.edit_make_edit_text)).getText().toString();
        String modelInput = ((EditText)findViewById(R.id.edit_model_edit_text)).getText().toString();
        String yearInput = ((EditText)findViewById(R.id.edit_year_edit_text)).getText().toString();
        String colorInput = ((EditText)findViewById(R.id.edit_color_edit_text)).getText().toString();
        int year =  Integer.parseInt(yearInput);
        String json = "{\n" +
                "\t\"make\": \"" + makeInput + "\",\n" +
                "\t\"model\": \"" + modelInput + "\",\n" +
                "\t\"color\": \"" + colorInput + "\",\n" +
                "\t\"year\": " + year + "\n" +
                "}";

        mOkHttpClient = new OkHttpClient();
        String url = "https://data-segment-176616.appspot.com/cars/" + carId;
        HttpUrl reqUrl = HttpUrl.parse(url);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(reqUrl)
                .patch(body)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    Intent intent = new Intent(CarItemActivity.this, CarActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    void deleteCar () {
        mOkHttpClient = new OkHttpClient();
        String url = "https://data-segment-176616.appspot.com/cars/" + carId;
        HttpUrl reqUrl = HttpUrl.parse(url);
        Request request = new Request.Builder()
                .url(reqUrl)
                .delete()
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    Intent intent = new Intent(CarItemActivity.this, CarActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
