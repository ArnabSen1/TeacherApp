package com.rcciit.teacherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    private static final int RC_SIGN_IN = 9001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private StringBuilder allLocationDataStringBuilder = new StringBuilder();
    private TextView tvLocationData;
    private Button btnCreateAttendanceExcel; // Added button for creating attendance excel
    private Button btnTakenAttendance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestStoragePermission();
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("411672249625-eg552pfl6s8vljn2vnh8e8dd8eiibgeb.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        tvLocationData = findViewById(R.id.tvLocationData);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            signIn();
        } else {
            fetchLocationData();
        }
        btnCreateAttendanceExcel = findViewById(R.id.btnCreateExcel);
        btnTakenAttendance = findViewById(R.id.btnTakenAttendance);

        btnTakenAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnTakenAttendance.setEnabled(false); // Disable the button
                // Enable the button after a 10-second delay
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        btnCreateAttendanceExcel.setEnabled(true); // Enable the button
                    }
                }, 10000); // 10 seconds delay
            }
        });
    }
    private void requestStoragePermission() {
        // Check if the permission has been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission hasn't been granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            // Permission has been granted
            // Do any other initialization here
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // Do any other initialization here
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission required for this app", Toast.LENGTH_SHORT).show();
                // Optionally, you can finish the activity or take other action here
            }
        }
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            Log.d("Firebase", "User signed in: " + user.getDisplayName());
                            fetchLocationData();
                        } else {
                            Log.w("Firebase", "signInWithCredential:failure", task.getException());
                        }
                    }
                });
    }

    private void fetchLocationData() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allLocationDataStringBuilder.setLength(0); // Clear StringBuilder

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    LocationData locationData = snapshot.getValue(LocationData.class);
                    if (locationData != null) {
                        calculateDistance(locationData);
                    }
                }
                // After processing all data, create the attendance Excel

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error fetching location data", databaseError.toException());
            }
        });
    }

    private void calculateDistance(LocationData locationData) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location deviceLocation) {
                    if (deviceLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                deviceLocation.getLatitude(), deviceLocation.getLongitude(),
                                locationData.getLatitude(), locationData.getLongitude(),
                                results);
                        if (results[0] <= 20) {
                            allLocationDataStringBuilder.append(locationData.getName())
                                    .append(", ")
                                    .append(locationData.getEmail())
                                    .append(", Present\n");
                        } else {
                            allLocationDataStringBuilder.append(locationData.getName())
                                    .append(", ")
                                    .append(locationData.getEmail())
                                    .append(", Absent\n");
                        }
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public void createAttendanceExcel(View view) {

        EditText editText = findViewById(R.id.etFileName);
        String value = editText.getText().toString();

        String fileName = value +"__"+ new SimpleDateFormat("yyyy_MM_dd_HH:mm", Locale.getDefault()).format(new Date()) + ".csv";
        String currentDate = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date());
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/attendance/" + currentDate;
        File directory = new File(filePath);
        if (!directory.exists()) {
            directory.mkdirs(); // Create directory if it doesn't exist
        }


        filePath += "/" + fileName;

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath));
            fileOutputStream.write(allLocationDataStringBuilder.toString().getBytes());
            fileOutputStream.close();
            Toast.makeText(this, "Attendance file created at: " + filePath, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating attendance file", Toast.LENGTH_SHORT).show();
        }
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Firebase Realtime Database cleared", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to clear Firebase Realtime Database", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
