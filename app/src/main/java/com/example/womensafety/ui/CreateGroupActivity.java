package com.example.womensafety.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.R;
import com.example.womensafety.util.CodeGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Creates a new private group with a unique, human-shareable join code.
 * Group data lives at /groups/{code} so JoinGroupActivity can look it up directly by code.
 */
public class CreateGroupActivity extends AppCompatActivity {

    private DatabaseReference groupsRef;
    private String generatedCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        groupsRef = FirebaseDatabase.getInstance().getReference("groups");

        TextView tvGeneratedCode = findViewById(R.id.tvGeneratedCode);
        Button btnEnterGroup = findViewById(R.id.btnEnterGroup);
        btnEnterGroup.setEnabled(false);
        tvGeneratedCode.setText("Generating code...");

        createGroupWithUniqueCode(tvGeneratedCode, btnEnterGroup);
    }

    private void createGroupWithUniqueCode(TextView tvGeneratedCode, Button btnEnterGroup) {
        String candidate = CodeGenerator.generate();

        // Check the code isn't already taken; retry on the rare collision.
        groupsRef.child(candidate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    createGroupWithUniqueCode(tvGeneratedCode, btnEnterGroup); // collision, retry
                    return;
                }
                generatedCode = candidate;
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                groupsRef.child(candidate).child("owner").setValue(uid);
                groupsRef.child(candidate).child("createdAt").setValue(System.currentTimeMillis());
                groupsRef.child(candidate).child("members").child(uid).child("email").setValue(email);

                tvGeneratedCode.setText(generatedCode);
                btnEnterGroup.setEnabled(true);
                btnEnterGroup.setOnClickListener(v -> {
                    Intent intent = new Intent(CreateGroupActivity.this, GroupActivity.class);
                    intent.putExtra("groupCode", generatedCode);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CreateGroupActivity.this,
                        "Failed to create group: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
