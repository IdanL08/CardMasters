package com.example.cardmasters.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardmasters.MainActivity;
import com.example.cardmasters.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;


import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private List<User> users;

    public LeaderboardAdapter(List<User> users) {
        this.users = users;
    }

    public static void showLeaderboardDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_leaderboard);

        // 1. Setup RecyclerView
        RecyclerView recyclerView = dialog.findViewById(R.id.leaderboard_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        List<User> leaderboardUsers = new ArrayList<>();
        LeaderboardAdapter adapter = new LeaderboardAdapter(leaderboardUsers);
        recyclerView.setAdapter(adapter);

        // 2. Fetch Data
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .orderBy("rating", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    leaderboardUsers.clear(); // Good practice to clear before adding
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String username = doc.getString("username");
                        Long rating = doc.getLong("rating");
                        if (username != null && rating != null) {
                            leaderboardUsers.add(new User(username, rating.intValue()));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });

        // ... (rest of your dialog sizing and button code)
        Button btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {


            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            // This forces the dialog to match the parent width
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            window.setAttributes(layoutParams);
        }
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        User user = users.get(position);
        holder.txtUsername.setText(user.getUsername());
        holder.txtRating.setText(String.valueOf(user.getRating()));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView txtUsername, txtRating;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtRating = itemView.findViewById(R.id.txtRating);
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