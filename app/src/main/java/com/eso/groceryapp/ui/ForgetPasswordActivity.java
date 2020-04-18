package com.eso.groceryapp.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eso.groceryapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgetPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    ImageButton mBackBtnLogin;
    EditText mEmailEtForget;
    Button mRecoverBtnForget;

    FirebaseAuth firebaseAuth;
    ProgressDialog loading;
    String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);
        firebaseAuth = FirebaseAuth.getInstance();
        loading = new ProgressDialog(this);
        loading.setTitle("Please wait");
        loading.setCanceledOnTouchOutside(false);

        mBackBtnLogin = findViewById(R.id.backBtn_Forget);
        mBackBtnLogin.setOnClickListener(this);
        mEmailEtForget = findViewById(R.id.emailEt_Forget);
        mRecoverBtnForget = findViewById(R.id.recoverBtn_Forget);
        mRecoverBtnForget.setOnClickListener(this);
    }

    private void recoverPassword() {
        email = mEmailEtForget.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmailEtForget.setError("Invalid Email");
            mEmailEtForget.setFocusable(true);
        }

        loading.setMessage("Sending instructions to reset password..");
        loading.show();

        firebaseAuth.sendPasswordResetEmail(email).addOnSuccessListener(aVoid -> {
            loading.dismiss();
            Toast.makeText(ForgetPasswordActivity.this, "Password reset instructions sent to your email..", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            loading.dismiss();
            Toast.makeText(ForgetPasswordActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.backBtn_Forget:
                onBackPressed();
                break;
            case R.id.recoverBtn_Forget:
                recoverPassword();
        }
    }

}
