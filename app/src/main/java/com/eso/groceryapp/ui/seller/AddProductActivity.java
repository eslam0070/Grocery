package com.eso.groceryapp.ui.seller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.eso.groceryapp.R;
import com.eso.groceryapp.databinding.ActivityAddProductBinding;
import com.eso.groceryapp.model.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddProductActivity extends AppCompatActivity {

    ActivityAddProductBinding binding;
    static final int CAMERA_REQUEST_CODE = 200;
    static final int STORAGE_REQUEST_CODE = 300;
    static final int IMAGE_PICK_GALLERY_CODE = 400;
    static final int IMAGE_PICK_CAMERA_CODE = 500;
    String[] cameraPermissions, storagePermissions;
    Uri image_uri = null;
    FirebaseAuth firebaseAuth;
    ProgressDialog loading;
    String productTitle,productDescription,productCategory,productQuantity,originalPrice,discountPrice,discountNote;
    boolean discountAvailable = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();
        loading = new ProgressDialog(this);
        loading.setTitle("Please wait");
        loading.setCanceledOnTouchOutside(false);
        binding.discountPriceEtAddProduct.setVisibility(View.GONE);
        binding.discountNoteEtAddProduct.setVisibility(View.GONE);
        binding.backBtnAddProduct.setOnClickListener(v -> onBackPressed());
        binding.profileIvAddProduct.setOnClickListener(v -> showImagePickDialog());
        binding.categoryTvAddProduct.setOnClickListener(v -> categoryDialog());
        binding.addProductBtn.setOnClickListener(v -> inputData());
        binding.discountSwAddProduct.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    binding.discountPriceEtAddProduct.setVisibility(View.VISIBLE);
                    binding.discountNoteEtAddProduct.setVisibility(View.VISIBLE);
                }else {
                    binding.discountPriceEtAddProduct.setVisibility(View.GONE);
                    binding.discountNoteEtAddProduct.setVisibility(View.GONE);
                }
            }
        });
    }

    private void inputData() {
        productTitle = binding.titleEtAddProduct.getText().toString().trim();
        productDescription = binding.descriptionEtAddProduct.getText().toString().trim();
        productCategory = binding.categoryTvAddProduct.getText().toString().trim();
        productQuantity = binding.quantityEtAddProduct.getText().toString().trim();
        originalPrice = binding.priceEtAddProduct.getText().toString().trim();
        discountAvailable = binding.discountSwAddProduct.isChecked();
        if (TextUtils.isEmpty(productTitle)){
            binding.titleEtAddProduct.setError("Title is required..");
            binding.titleEtAddProduct.setFocusable(true);
        }else if (TextUtils.isEmpty(productCategory)){
            binding.categoryTvAddProduct.setError("Category is required..");
            binding.categoryTvAddProduct.setFocusable(true);
        }else if (TextUtils.isEmpty(originalPrice)){
            binding.priceEtAddProduct.setError("Price is required..");
            binding.priceEtAddProduct.setFocusable(true);
        } else if (discountAvailable) {
            discountPrice = binding.discountPriceEtAddProduct.getText().toString().trim();
            discountNote = binding.discountNoteEtAddProduct.getText().toString().trim();
            Toast.makeText(this, "Discount Price is required..", Toast.LENGTH_SHORT).show();
        }else {
            discountPrice = "0";
            discountNote = "";
        }
        addProduct();

    }

    private void addProduct() {
        loading.setMessage("Adding Product");
        loading.show();

        String timestamp = "" + System.currentTimeMillis();
        if (image_uri == null){
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productId",""+timestamp);
            hashMap.put("productTitle",""+productTitle);
            hashMap.put("productDescription",""+productDescription);
            hashMap.put("productCategory",""+productCategory);
            hashMap.put("productQuantity",""+productQuantity);
            hashMap.put("productIcon",""+"");
            hashMap.put("originalPrice",""+originalPrice);
            hashMap.put("discountPrice",""+discountPrice);
            hashMap.put("discountNote",""+discountNote);
            hashMap.put("discountAvailable",""+discountAvailable);
            hashMap.put("timestamp",""+timestamp);
            hashMap.put("uid",""+firebaseAuth.getUid());

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(firebaseAuth.getUid()).child("Products").child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(aVoid -> {
                        loading.dismiss();
                        Toast.makeText(AddProductActivity.this, "Product Added...", Toast.LENGTH_SHORT).show();
                        clearData();
                    }).addOnFailureListener(e -> {
                        loading.dismiss();
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    });
        }else {
            String filePathAndName = "product_images/" + "" + timestamp;
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());
                    Uri downloadImageUri = uriTask.getResult();
                    if(uriTask.isSuccessful()){
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("productId",""+timestamp);
                        hashMap.put("productTitle",""+productTitle);
                        hashMap.put("productDescription",""+productDescription);
                        hashMap.put("productCategory",""+productCategory);
                        hashMap.put("productQuantity",""+productQuantity);
                        hashMap.put("productIcon",""+downloadImageUri);
                        hashMap.put("originalPrice",""+originalPrice);
                        hashMap.put("discountPrice",""+discountPrice);
                        hashMap.put("discountNote",""+discountNote);
                        hashMap.put("discountAvailable",""+discountAvailable);
                        hashMap.put("timestamp",""+timestamp);
                        hashMap.put("uid",""+firebaseAuth.getUid());

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                        reference.child(firebaseAuth.getUid()).child("Products").child(timestamp).setValue(hashMap)
                                .addOnSuccessListener(aVoid -> {
                                    loading.dismiss();
                                    Toast.makeText(AddProductActivity.this, "Product Added...", Toast.LENGTH_SHORT).show();
                                    clearData();
                                }).addOnFailureListener(e -> {
                            loading.dismiss();
                            Toast.makeText(AddProductActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).addOnFailureListener(e -> {
                loading.dismiss();
                Toast.makeText(AddProductActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void clearData() {
        binding.titleEtAddProduct.setText("");
        binding.descriptionEtAddProduct.setText("");
        binding.categoryTvAddProduct.setText("");
        binding.quantityEtAddProduct.setText("");
        binding.priceEtAddProduct.setText("");
        binding.discountPriceEtAddProduct.setText("");
        binding.discountNoteEtAddProduct.setText("");
        binding.profileIvAddProduct.setImageResource(R.drawable.ic_add_shopping_primary);
        image_uri = null;
    }

    private void categoryDialog() {
        new AlertDialog.Builder(this).setTitle("Product Category")
                .setItems(Constants.options, (dialog, which) -> {
                    String category = Constants.options[which];
                    binding.categoryTvAddProduct.setText(category);
                }).show();
    }

    private void showImagePickDialog() {
        String[] option = {"Camera","Gallery"};
        new AlertDialog.Builder(this).setTitle("Pick Image")
                .setItems(option, (dialog, which) -> {
                    if (which == 0){
                        if (checkCameraPermission()){
                            pickFromCamera();
                        }else {
                            requestCameraPermission();
                        }
                    } else if (which == 1) {
                        if (checkStoragePermission()){
                            pickFromGalley();
                        }else {
                            requestStoragePermission();
                        }
                    }
                }).show();
    }

    private void pickFromGalley() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Temp_Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent,IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED;
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED;
        return result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted)
                        pickFromCamera();
                    else
                        Toast.makeText(this, "Camera permission are necessary...", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted)
                        pickFromGalley();
                    else
                        Toast.makeText(this, "Storage permission are necessary...", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                image_uri = data.getData();
                binding.profileIvAddProduct.setImageURI(image_uri);
            }else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                binding.profileIvAddProduct.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
