package com.arachnisapps.sars;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;


import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private Handler handler;
    SharedPreferences sharedPreferences;
    private String NetId=null,password=null;
    private EditText etNetId,etPass;

    public void onCreate(final Bundle bundle)
    {
        super.onCreate(bundle);
        sharedPreferences= this.getSharedPreferences("MYPREFERENCES", 0);
        this.NetId = sharedPreferences.getString("username", null);
        this.password = sharedPreferences.getString("password", null);
        if (NetId != null && password != null)
        {
            Intent startmain = new Intent(getApplicationContext(), MainActivity.class);
            startmain.putExtra("username", NetId);
            startmain.putExtra("password", password);
            startActivity(startmain);
        }

        setContentView(R.layout.activity_login);

        etNetId = (EditText)findViewById(R.id.email);
        etPass = (EditText)findViewById(R.id.password);
        ((Button)findViewById(R.id.login)).setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(View v)
            {
                NetId = etNetId.getText().toString();
                password = etPass.getText().toString();
                if(isValid()) {
                    confirmDetails();
                }
            }
        });

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 1) {
                    etNetId.setError("Invalid NetId");
                    Toast.makeText(LoginActivity.this, "Please check your NetID", Toast.LENGTH_SHORT).show();
                }
                else if (msg.what == 2) {
                    etPass.setError("Invalid password");
                    Toast.makeText(LoginActivity.this, "Please check your password", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    private boolean isValid()
    {
        if(NetId.length() != 5) {
            etNetId.setError("Invalid NetID");
            return false;
        }
        if(password.length() == 0) {
            etPass.setError("Empty password");
            return false;
        }
        return true;
    }

    Call post(Callback callback) throws IOException {
        OkHttpClient client = getUnsafeOkHttpClient();
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client.setCookieHandler(cookieManager);
        RequestBody requestBody = new FormEncodingBuilder()
                .add("user_id", NetId)
                .add("user_password", password)
                .build();
        Request request = new Request.Builder()
                .url("https://studentmaintenance.webapps.snu.edu.in/students/public/studentslist/studentslist/loginauth")
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private void confirmDetails() {
        final ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Loading...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        try {
            post(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e(TAG, "Details confirmation failed. " + e.toString());
                    dialog.dismiss();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    /**
                     * The student details website uses class "label label-danger"
                     * to show error in authentication. If this class is refereed
                     * then there is an error authenticating the user.
                     */
                    dialog.dismiss();
                    String responseData = response.body().string();
                    if (responseData.contains("label label-danger")) {
                        Log.e(TAG, "Bad login credentials");

                        /**
                         * If true then wrong SNU Net ID.
                         * Else wrong password.
                         */
                        if (responseData.contains("You are not a valid user of the system")) {
                            Message message = handler.obtainMessage(1);
                            message.sendToTarget();
                        } else {
                            Message message = handler.obtainMessage(2);
                            message.sendToTarget();
                        }
                        return;
                    }
                    sharedPreferences= getSharedPreferences("MYPREFERENCES", 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("username", NetId);
                    editor.putString("password", password);
                    editor.apply();
                    Intent startMain = new Intent(getApplicationContext(), MainActivity.class);
                    startMain.putExtra("username", NetId);
                    startMain.putExtra("password", password);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(startMain);
                    finish();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }
}
