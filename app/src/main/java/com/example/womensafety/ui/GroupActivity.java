package com.example.womensafety.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.womensafety.R;
import com.example.womensafety.model.MemberLocation;
import com.example.womensafety.model.SosContact;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the live locations of everyone in the group and lets the user
 * fire an SOS: sends the current location by SMS to every saved contact
 * and places a call to the first one.
 *
 * Realtime DB layout:
 *   /groups/{code}/members/{uid}/{email, latitude, longitude, updatedAt}
 *   /users/{uid}/contacts/{pushId}/{name, phone}
 */
public class GroupActivity extends AppCompatActivity {

    private static final long LOCATION_INTERVAL_MS = 10_000;

    private String groupCode;
    private DatabaseReference membersRef;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private ListView lvMembers;
    private ArrayAdapter<String> membersAdapter;
    private final List<String> memberLines = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
                boolean fineLocation = Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (fineLocation) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(this, "Location permission is required to share your position",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        groupCode = getIntent().getStringExtra("groupCode");
        membersRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupCode).child("members");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        TextView tvGroupCode = findViewById(R.id.tvGroupCode);
        tvGroupCode.setText("Group code: " + groupCode);

        lvMembers = findViewById(R.id.lvMembers);
        membersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, memberLines);
        lvMembers.setAdapter(membersAdapter);

        Button btnSos = findViewById(R.id.btnSos);
        btnSos.setOnClickListener(v -> triggerSos());

        listenForMemberUpdates();
        ensureLocationPermissionAndStart();
    }

    // ---------- Real-time location tracking ----------

    private void ensureLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    pushLocationToFirebase(location);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
        }
    }

    private void pushLocationToFirebase(Location location) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        MemberLocation memberLocation = new MemberLocation(
                email, location.getLatitude(), location.getLongitude(), System.currentTimeMillis());
        membersRef.child(uid).setValue(memberLocation);
    }

    private void listenForMemberUpdates() {
        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                memberLines.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    MemberLocation member = child.getValue(MemberLocation.class);
                    if (member != null) {
                        memberLines.add(member.toString());
                    }
                }
                membersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GroupActivity.this,
                        "Failed to load members: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // ---------- SOS ----------

    private void triggerSos() {
        boolean hasSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCall = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasSms || !hasCall) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE}, 101);
            Toast.makeText(this, "Grant SMS/call permissions, then tap SOS again", Toast.LENGTH_LONG).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String message = "SOS! I need help. My location: "
                    + (location != null
                        ? "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude()
                        : "unavailable");
            sendSosToContacts(message);
        });
    }

    private void sendSosToContacts(String message) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference contactsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("contacts");

        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(GroupActivity.this,
                            "No SOS contacts saved yet. Add some first.", Toast.LENGTH_LONG).show();
                    return;
                }

                SmsManager smsManager = SmsManager.getDefault();
                boolean calledOne = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    SosContact contact = child.getValue(SosContact.class);
                    if (contact == null || contact.getPhone() == null) continue;

                    smsManager.sendTextMessage(contact.getPhone(), null, message, null, null);

                    if (!calledOne) {
                        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact.getPhone()));
                        startActivity(callIntent);
                        calledOne = true;
                    }
                }
                Toast.makeText(GroupActivity.this, "SOS sent to your contacts", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GroupActivity.this,
                        "Could not load contacts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
