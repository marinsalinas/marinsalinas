package mx.axtel.connectedcar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.gc.materialdesign.views.ButtonFlat;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.gson.Gson;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.axtel.connectedcar.helpers.NetworkHelper;
import mx.axtel.connectedcar.helpers.Session;
import mx.axtel.connectedcar.models.User;


public class LoginActivity extends Activity{

    // UI references.
    private MaterialEditText mAccountView;
    private MaterialEditText mUsernameView;
    private MaterialEditText mPasswordView;
    private View mProgressView;
    private View mUserLoginFormView;
    private SignInButton mPlusSignInButton;
    private View mSignOutButtons;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        // Set up the login form.
        mAccountView = (MaterialEditText) findViewById(R.id.account);

        mUsernameView = (MaterialEditText) findViewById(R.id.username);

        mPasswordView = (MaterialEditText) findViewById(R.id.password);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        ButtonFlat mUserSignInButton = (ButtonFlat) findViewById(R.id.email_sign_in_button);
        mUserSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(NetworkHelper.isOnline(getApplicationContext())){
                    attemptLogin();
                    return;
                }

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.check_internet), Toast.LENGTH_LONG).show();

            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.layout_progress);
        mUserLoginFormView = findViewById(R.id.user_login_form);
    }

    public void attemptLogin() {

        // Reset errors.
        mAccountView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);


        // Store values at the time of the login attempt.
        final String account = mAccountView.getText().toString();
        final String username = mUsernameView.getText().toString();
        final String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        // Check for a valid account address.
        if (TextUtils.isEmpty(account)) {
            mAccountView.setError(getString(R.string.error_field_required));
            focusView = mAccountView;
            cancel = true;
        }

        if (cancel) {
            //Ocurrio algun error.
            focusView.requestFocus();
        } else {
            //Paso la validación de Strings
            showProgress(true);

                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                //Post Params.
                Map<String, String> params = new HashMap<String, String>();
                params.put("account", account);
                params.put("user", username);
                params.put("password", password);

                String URL =  getResources().getString(R.string.auth_login);

                JsonObjectRequest req = new JsonObjectRequest(
                        Request.Method.POST,
                        URL,
                        new JSONObject(params),
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                showProgress(false);
                                //Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                                Session session = new Session(getApplicationContext());
                                Gson gson  = new Gson();
                                session.createLoginSession(gson.fromJson(response.toString(), User.class));
                                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                if(error instanceof TimeoutError){
                                    Toast.makeText(getApplicationContext(), R.string.check_internet, Toast.LENGTH_LONG).show();
                                    showProgress(false);
                                    return;
                                }



                                if(error.networkResponse.statusCode == 400){
                                    mAccountView.setError("BAD REQUEST");
                                }else if(error.networkResponse.statusCode == 401){
                                    mAccountView.setError(getResources().getString(R.string.error_unable_login));
                                }else if(error.networkResponse.statusCode == 403){
                                    mAccountView.setError(getResources().getString(R.string.error_forbidden));
                                }else{
                                    Log.e("ERROR", error.networkResponse.data.toString());
                                    mAccountView.setError(getResources().getString(R.string.error_unable_login));
                                }
                                showProgress(false);
                            }
                        }) {
                    //Configurando Headers para que tome JSON
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("Content-Type", "application/json; charset=utf-8");
                        return headers;
                    }
                };
                queue.add(req);
        }
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);


        }
    }



}



