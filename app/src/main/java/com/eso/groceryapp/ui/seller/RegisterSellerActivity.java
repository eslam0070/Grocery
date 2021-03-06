package com.eso.groceryapp.ui.seller;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eso.groceryapp.databinding.ActivityRegisterSellerBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RegisterSellerActivity extends AppCompatActivity implements LocationListener {

    ActivityRegisterSellerBinding binding;
    static final int LOCATION_REQUEST_CODE = 100;
    static final int CAMERA_REQUEST_CODE = 200;
    static final int STORAGE_REQUEST_CODE = 300;
    static final int IMAGE_PICK_GALLERY_CODE = 400;
    static final int IMAGE_PICK_CAMERA_CODE = 500;
    String[] locationPermissions, cameraPermissions, storagePermissions;
    Uri image_uri = null;
    LocationManager locationManager;
    double latitude, longitude;
    FirebaseAuth firebaseAuth;
    ProgressDialog loading;
    String fullName,shopName,phoneNumber,deliveryFee,country,state,city,address,email,password,confirmPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterSellerBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        binding.backBtnSeller.setOnClickListener(v -> onBackPressed());
        binding.gpsBtnSeller.setOnClickListener(v -> {
            //detect current location
            if (checkLocationPermission()){
                //already allowed
                detectLocation();
            }else {
                //not allowed,  request
                requestLocationPermission();
            }
        });
        binding.profileIvSeller.setOnClickListener(v -> showImagePickDialog());
        binding.registerBtnSeller.setOnClickListener(v -> inputData());
        firebaseAuth = FirebaseAuth.getInstance();
        loading = new ProgressDialog(this);
        loading.setTitle("Please wait");
        loading.setCanceledOnTouchOutside(false);
        //init permission array
        locationPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }



    private void inputData() {
        fullName = binding.nameEtSeller.getText().toString().trim();
        shopName = binding.shopNameEtSeller.getText().toString().trim();
        phoneNumber = binding.phoneEtSeller.getText().toString().trim();
        deliveryFee = binding.deliveryPhoneEtSeller.getText().toString().trim();
        email = binding.emailEtSeller.getText().toString().trim();
        password = binding.passwordEtSeller.getText().toString().trim();
        confirmPassword = binding.cPasswordEtSeller.getText().toString().trim();
        if (TextUtils.isEmpty(fullName)){
            binding.nameEtSeller.setError("Enter Full Name");
            binding.nameEtSeller.setFocusable(true);
        }else if (TextUtils.isEmpty(shopName)){
            binding.shopNameEtSeller.setError("Enter Shop Name");
            binding.shopNameEtSeller.setFocusable(true);
        }else if (TextUtils.isEmpty(phoneNumber)){
            binding.phoneEtSeller.setError("Enter Phone");
            binding.phoneEtSeller.setFocusable(true);
        }else if (TextUtils.isEmpty(deliveryFee)){
            binding.deliveryPhoneEtSeller.setError("Enter Delivery Fee");
            binding.deliveryPhoneEtSeller.setFocusable(true);
        }else if (latitude == 0.0 || longitude == 0.0){
            Toast.makeText(this, "Please Click GPS button to detect location..", Toast.LENGTH_SHORT).show();
        }else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEtSeller.setError("Invalid Email");
            binding.emailEtSeller.setFocusable(true);
        }else if (password.length() < 6) {
            binding.passwordEtSeller.setError("Password length at least 6 characters");
            binding.passwordEtSeller.setFocusable(true);
        }else if (!password.equals(confirmPassword)){
            binding.cPasswordEtSeller.setError("Password doesn't match");
            binding.cPasswordEtSeller.setFocusable(true);
        }else
            createAccount();
    }

    private void createAccount() {
        loading.setMessage("Creating Account..");
        loading.show();
        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener(authResult -> saveToFirebase()).addOnFailureListener(e -> {
            loading.dismiss();
            Toast.makeText(RegisterSellerActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveToFirebase() {
        loading.setMessage("Saving Account Info..");
        loading.show();
        final String timestamp = "" + System.currentTimeMillis();
        if(image_uri == null){
            // save info without image

            //setup data to save
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid","" + firebaseAuth.getUid());
            hashMap.put("email","" + email);
            hashMap.put("name","" + fullName);
            hashMap.put("shopName","" + shopName);
            hashMap.put("phone","" + phoneNumber);
            hashMap.put("deliveryFee","" + deliveryFee);
            hashMap.put("country","" + country);
            hashMap.put("state","" + state);
            hashMap.put("city","" + city);
            hashMap.put("address","" + address);
            hashMap.put("latitude","" + latitude);
            hashMap.put("longitude","" + longitude);
            hashMap.put("timestamp","" + timestamp);
            hashMap.put("accountType","Seller");
            hashMap.put("online","true");
            hashMap.put("shopOpen","true");
            hashMap.put("profileImage","");
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).setValue(hashMap).addOnSuccessListener(aVoid -> {
                loading.dismiss();
                startActivity(new Intent(RegisterSellerActivity.this,MainSellerActivity.class));
                finish();
            }).addOnFailureListener(e -> {
                loading.dismiss();
                Toast.makeText(RegisterSellerActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
        }else {
            // save info with image

            // name and path of image
            String filePathAndName = "profile_images/" + " " + firebaseAuth.getUid();
            //upload Image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(image_uri).addOnSuccessListener(taskSnapshot -> {
                // get url of uploaded image
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                Uri downloadUrl = uriTask.getResult();
                if(uriTask.isSuccessful()){
                    //setup data to save
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("uid","" + firebaseAuth.getUid());
                    hashMap.put("email","" + email);
                    hashMap.put("name","" + fullName);
                    hashMap.put("shopName","" + shopName);
                    hashMap.put("phone","" + phoneNumber);
                    hashMap.put("deliveryFee","" + deliveryFee);
                    hashMap.put("country","" + country);
                    hashMap.put("state","" + state);
                    hashMap.put("city","" + city);
                    hashMap.put("address","" + address);
                    hashMap.put("latitude","" + latitude);
                    hashMap.put("longitude","" + longitude);
                    hashMap.put("timestamp","" + timestamp);
                    hashMap.put("accountType","Seller");
                    hashMap.put("online","true");
                    hashMap.put("shopOpen","true");
                    hashMap.put("profileImage",downloadUrl);
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                    ref.child(firebaseAuth.getUid()).setValue(hashMap).addOnSuccessListener(aVoid -> {
                        loading.dismiss();
                        startActivity(new Intent(RegisterSellerActivity.this,MainSellerActivity.class));
                        finish();
                    }).addOnFailureListener(e -> {
                        loading.dismiss();
                        Toast.makeText(RegisterSellerActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

            }).addOnFailureListener(e -> {
                loading.dismiss();
                Toast.makeText(RegisterSellerActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });

        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE);
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

    private void detectLocation() {
        Toast.makeText(this, "Please wait...", Toast.LENGTH_LONG).show();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        //location detected
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        findAddress();
    }

    private void findAddress() {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latitude,longitude,1);
            String address = addresses.get(0).getAddressLine(0); //complete address
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();

            binding.countryEtSeller.setText(country);
            binding.stateEtSeller.setText(state);
            binding.cityEtSeller.setText(city);
            binding.addressEtSeller.setText(address);

        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        //gps/location disabled
        Toast.makeText(this, "Please turn on location...", Toast.LENGTH_SHORT).show();
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

    private void pickFromGalley(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"Temp_Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Temp_Image Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(intent,IMAGE_PICK_CAMERA_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted)
                        detectLocation();
                    else
                        Toast.makeText(this, "Location permission are necessary...", Toast.LENGTH_SHORT).show();
                }
            }
            break;
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
                binding.profileIvSeller.setImageURI(image_uri);
            }else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                binding.profileIvSeller.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
