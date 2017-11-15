/*
Author: Kyle Cross
Date: 8-14-17
Description: An activity that allows a user to log in with a Google account and manage a list of cars
that are stored in a Google Cloud datastore. On starting the activity, authorization is requested through
Google Plus. The response from the Google server is handled in AuthCompleteActivity where a the AuthState
is updated. The user's email is then obtained through use of the authorization token by making a call to
the Google Plus API.
The user is identified by their email. Users can add cars to their list which is stored in the
Google Cloud datastore. The list of cars added by the user appear as they are added. Clicking on the
items in the list allows the user to update or delete the car and its properties.
 */

package com.kylecross.finalproject;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CarActivity extends AppCompatActivity {

    private AuthorizationService mAuthorizationService;
    private AuthState mAuthState;
    private OkHttpClient mOkHttpClient;
    private String emailAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);
        mAuthorizationService = new AuthorizationService(this);


        ((Button)findViewById(R.id.add_car_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    createCar();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart(){
        mAuthState = getOrCreateAuthState();
        super.onStart();

        try {
            mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException e) {
                    if(e == null){
                        getEmail(accessToken, idToken, e);
                    }
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    AuthState getOrCreateAuthState(){
        AuthState auth = null;
        SharedPreferences authPreference = getSharedPreferences("auth", MODE_PRIVATE);
        String stateJson = authPreference.getString("stateJson", null);
        if(stateJson != null){
            try {
                auth = AuthState.jsonDeserialize(stateJson);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        if( auth != null && auth.getAccessToken() != null){
            return auth;
        } else {
            updateAuthState();
            return null;
        }
    }

    void updateAuthState(){

        Uri authEndpoint = new Uri.Builder().scheme("https").authority("accounts.google.com").path("/o/oauth2/v2/auth").build();
        Uri tokenEndpoint = new Uri.Builder().scheme("https").authority("www.googleapis.com").path("/oauth2/v4/token").build();
        Uri redirect = new Uri.Builder().scheme("com.kylecross.finalproject").path("foo").build();

        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint, null);
        AuthorizationRequest req = new AuthorizationRequest.Builder(config, "868941441887-jckp50a1q2am0a8smkv0hvv486m7bcgf.apps.googleusercontent.com", ResponseTypeValues.CODE, redirect)
                .setScopes("https://www.googleapis.com/auth/plus.me", "https://www.googleapis.com/auth/userinfo.email", "https://www.googleapis.com/auth/userinfo.profile")
                .build();

        Intent authComplete = new Intent(this, AuthCompleteActivity.class);
        mAuthorizationService.performAuthorizationRequest(req, PendingIntent.getActivity(this, req.hashCode(), authComplete, 0));
    }

    void getEmail(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException e) {
        mOkHttpClient = new OkHttpClient();
        HttpUrl reqUrl = HttpUrl.parse("https://www.googleapis.com/plus/v1/people/me");
        Request request = new Request.Builder()
                .url(reqUrl)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String r = response.body().string();

                try {
                    JSONObject j = new JSONObject(r);
                    JSONArray emails = j.getJSONArray("emails");
                    emailAddress = emails.getJSONObject(0).getString("value");

                    if (emailAddress != null)
                        getCarList();

                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    void getCarList() {
        mOkHttpClient = new OkHttpClient();
        String url = "https://data-segment-176616.appspot.com/users/" + emailAddress + "/cars";
        HttpUrl reqUrl = HttpUrl.parse(url);
        Request request = new Request.Builder()
                .url(reqUrl)
                .get()
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String r = response.body().string();

                try {
                    JSONObject j = new JSONObject(r);
                    JSONArray items = j.getJSONArray("cars");
                    List<Map<String,String>> cars = new ArrayList<>();
                    for(int i = 0; i < items.length(); i++){
                        HashMap<String, String> m = new HashMap<>();
                        m.put("make", items.getJSONObject(i).getString("make"));
                        m.put("model",items.getJSONObject(i).getString("model"));
                        m.put("year",items.getJSONObject(i).getString("year"));
                        m.put("color",items.getJSONObject(i).getString("color"));
                        m.put("id",items.getJSONObject(i).getString("id"));
                        m.put("email",items.getJSONObject(i).getString("email"));
                        cars.add(m);
                    }
                    final SimpleAdapter carAdapter = new SimpleAdapter(
                            CarActivity.this,
                            cars,
                            R.layout.car_item,
                            new String[]{"make", "model", "year", "color"},
                            new int[]{R.id.car_item_make_text, R.id.car_item_model_text, R.id.car_item_year_text, R.id.car_item_color_text});


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final ListView listView = ((ListView)findViewById(R.id.car_list));

                            listView.setAdapter(carAdapter);

                            AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View container, int position, long id) {
                                    Map<String, String> itemData = (Map<String, String>) listView.getItemAtPosition(position);

                                    Intent intent = new Intent(CarActivity.this, CarItemActivity.class);

                                    Bundle bundle = new Bundle();

                                    bundle.putString("make", itemData.get("make"));
                                    bundle.putString("model", itemData.get("model"));
                                    bundle.putString("year", itemData.get("year"));
                                    bundle.putString("color", itemData.get("color"));
                                    bundle.putString("id", itemData.get("id"));
                                    bundle.putString("email", itemData.get("email"));

                                    intent.putExtras(bundle);

                                    startActivity(intent);
                                }
                            };

                            listView.setOnItemClickListener(itemClickListener);
                        }
                    });
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    void createCar() {
        MediaType JSON = MediaType.parse("application/json");
        String makeInput = ((EditText)findViewById(R.id.add_make_edit_text)).getText().toString();
        String modelInput = ((EditText)findViewById(R.id.add_model_edit_text)).getText().toString();
        String yearInput = ((EditText)findViewById(R.id.add_year_edit_text)).getText().toString();
        String colorInput = ((EditText)findViewById(R.id.add_color_edit_text)).getText().toString();
        int year =  Integer.parseInt(yearInput);
        String json = "{\n" +
                "\t\"make\": \"" + makeInput + "\",\n" +
                "\t\"model\": \"" + modelInput + "\",\n" +
                "\t\"color\": \"" + colorInput + "\",\n" +
                "\t\"year\": " + year + "\n" +
                "}";

        mOkHttpClient = new OkHttpClient();
        String url = "https://data-segment-176616.appspot.com/users/" + emailAddress + "/cars";
        HttpUrl reqUrl = HttpUrl.parse(url);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(reqUrl)
                .post(body)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    getCarList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
