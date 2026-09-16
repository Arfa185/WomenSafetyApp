package com.example.womensafety.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.R;
import com.example.womensafety.model.SosContact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SosContactsActivity extends AppCompatActivity {

    private DatabaseReference contactsRef;
    private ListView lvContacts;
    private ArrayAdapter<String> adapter;
    private final List<String> contactLines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_contacts);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        contactsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("contacts");

        lvContacts = findViewById(R.id.lvContacts);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactLines);
        lvContacts.setAdapter(adapter);

        EditText etName = findViewById(R.id.etContactName);
        EditText etPhone = findViewById(R.id.etContactPhone);
        Button btnAdd = findViewById(R.id.btnAddContact);

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Enter a name and phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            contactsRef.push().setValue(new SosContact(name, phone));
            etName.setText("");
            etPhone.setText("");
        });

        listenForContacts();
    }

    private void listenForContacts() {
        contactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactLines.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SosContact contact = child.getValue(SosContact.class);
                    if (contact != null) {
                        contactLines.add(contact.toString());
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SosContactsActivity.this,
                        "Failed to load contacts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
