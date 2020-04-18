package com.eso.groceryapp.ui.user;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.eso.groceryapp.databinding.ActivityMainUserBinding;
import com.eso.groceryapp.ui.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

@SuppressLint("SetTextI18n")
public class MainUserActivity extends AppCompatActivity {

    ActivityMainUserBinding binding;
    FirebaseAuth firebaseAuth;
    ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUserBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        firebaseAuth = FirebaseAuth.getInstance();
        loading = new ProgressDialog(this);
        loading.setTitle("Please with");
        loading.setCanceledOnTouchOutside(false);
        binding.logoutBtnMainUser.setOnClickListener(v -> makeMeOffline());
        binding.edtProfileBtnMainUser.setOnClickListener(v ->
                startActivity(new Intent(MainUserActivity.this,ProfileEditUserActivity.class)));
        checkUser();
    }

    private void makeMeOffline() {
        loading.setMessage("Logging Out..");
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("online","false");
        DatabaseReference ref  = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).updateChildren(hashMap).addOnSuccessListener(aVoid -> {
            firebaseAuth.signOut();
            checkUser();
        }).addOnFailureListener(e -> {
            loading.dismiss();
            Toast.makeText(MainUserActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }else {
            loadMyInfo();
        }
    }

    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds :dataSnapshot.getChildren()){
                    String name = ds.child("name").getValue(String.class);
                    String accountType = ds.child("accountType").getValue(String.class);

                    binding.nameTvMainUser.setText(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
