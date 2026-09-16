package com.example.womensafety.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class JoinGroupActivity extends AppCompatActivity {

    private EditText etGroupCode;
    private ProgressBar progressBar;
    private DatabaseReference groupsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        etGroupCode = findViewById(R.id.etGroupCode);
        progressBar = findViewById(R.id.progressBar);
        Button btnJoin = findViewById(R.id.btnJoin);

        btnJoin.setOnClickListener(v -> attemptJoin());
    }

    private void attemptJoin() {
        String code = etGroupCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Enter a join code", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        groupsRef.child(code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (!snapshot.exists()) {
                    Toast.makeText(JoinGroupActivity.this,
                            "No group found with that code", Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                groupsRef.child(code).child("members").child(uid).child("email").setValue(email);

                Intent intent = new Intent(JoinGroupActivity.this, GroupActivity.class);
                intent.putExtra("groupCode", code);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(JoinGroupActivity.this,
                        "Failed to join: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
