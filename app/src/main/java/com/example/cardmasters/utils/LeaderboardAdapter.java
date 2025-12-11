package com.example.cardmasters.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.cardmasters.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;


import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends ArrayAdapter<User> {

    private Context context;
    private List<User> users;

    public LeaderboardAdapter(Context context, List<User> users) {
        super(context, 0, users);
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_leaderboard, parent, false);
        }

        TextView txtUsername = convertView.findViewById(R.id.txtUsername);
        TextView txtRating = convertView.findViewById(R.id.txtRating);

        User user = users.get(position);

        txtUsername.setText(user.getUsername());
        txtRating.setText(String.valueOf(user.getRating()));

        return convertView;
    }

    public static void showLeaderboardDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_leaderboard);
        dialog.setCancelable(true);

        TextView title = dialog.findViewById(R.id.dialog_title);
        title.setText("Leaderboard");

        ListView listView = dialog.findViewById(R.id.leaderboard_list);
        Button btnClose = dialog.findViewById(R.id.btn_close);

        List<User> leaderboardUsers = new ArrayList<>();
        LeaderboardAdapter adapter = new LeaderboardAdapter(context, leaderboardUsers);
        listView.setAdapter(adapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .orderBy("rating", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String username = doc.getString("username");
                        Long rating = doc.getLong("rating");

                        leaderboardUsers.add(new User(username, rating.intValue()));

                    }
                    adapter.notifyDataSetChanged();
                });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }
    }

}



 class User {//להצגה בטבלה
    public String username;

    public int rating;

    public User() {} // Firestore needs empty constructor

    public User(String username, int rating) {
        this.username = username;

        this.rating = rating;
    }
    public  String getUsername() {
        return username;
    }


    public  int getRating() {
        return rating;
    }

}