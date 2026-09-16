package com.example.womensafety.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.R;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnCreateGroup = findViewById(R.id.btnCreateGroup);
        Button btnJoinGroup = findViewById(R.id.btnJoinGroup);
        Button btnManageContacts = findViewById(R.id.btnManageContacts);

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "";
        tvWelcome.setText("Welcome, " + email);

        btnCreateGroup.setOnClickListener(v ->
                startActivity(new Intent(this, CreateGroupActivity.class)));

        btnJoinGroup.setOnClickListener(v ->
                startActivity(new Intent(this, JoinGroupActivity.class)));

        btnManageContacts.setOnClickListener(v ->
                startActivity(new Intent(this, SosContactsActivity.class)));
    }
}
