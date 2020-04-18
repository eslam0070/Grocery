package com.eso.groceryapp.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.eso.groceryapp.databinding.ActivityLoginBinding;
import com.eso.groceryapp.ui.seller.MainSellerActivity;
import com.eso.groceryapp.ui.user.MainUserActivity;
import com.eso.groceryapp.ui.user.RegisterUserActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    String email,password;
    ProgressDialog loading;
    FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        firebaseAuth = FirebaseAuth.getInstance();
        loading = new ProgressDialog(this);
        binding.forgetTvLogin.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgetPasswordActivity.class)));
        binding.loginBtnLogin.setOnClickListener(v ->loginUser() );
        binding.notHaveTvLogin.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterUserActivity.class)));
    }

    private void loginUser() {
        email = binding.emailEtLogin.getText().toString().trim();
        password = binding.passwordEtLogin.getText().toString().trim();
     if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
         binding.emailEtLogin.setError("Invalid Email");
         binding.emailEtLogin.setFocusable(true);
        }else if (password.length() < 6) {
         binding.passwordEtLogin.setError("Password length at least 6 characters");
         binding.passwordEtLogin.setFocusable(true);
        }

     loading.setMessage("Logging In..");
     loading.show();

     firebaseAuth.signInWithEmailAndPassword(email,password).addOnSuccessListener
             (authResult -> makeMeOnline()).addOnFailureListener(e -> {
         loading.dismiss();
         Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
     });
    }

    private void makeMeOnline() {
        loading.setMessage("Checking User..");
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("online","true");
        DatabaseReference ref  = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).updateChildren(hashMap).addOnSuccessListener(
                aVoid -> checkUserType()).addOnFailureListener(e -> {
            loading.dismiss();
            Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void checkUserType() {
         DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
         ref.orderByChild("uid").equalTo(firebaseAuth.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                 for (DataSnapshot ds : dataSnapshot.getChildren()){
                     String accountType = ds.child("accountType").getValue(String.class);
                     if(accountType.equals("Seller")){
                         loading.dismiss();
                         startActivity(new Intent(LoginActivity.this, MainSellerActivity.class));
                         finish();
                     }else {
                         loading.dismiss();
                         startActivity(new Intent(LoginActivity.this, MainUserActivity.class));
                         finish();
                     }
                 }
             }

             @Override
             public void onCancelled(@NonNull DatabaseError databaseError) {

             }
         });
    }
}
