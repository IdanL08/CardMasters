package com.example.cardmasters.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    // מתודה סטטית להצגת הדיאלוג
    public static void showLeaderboardDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_leaderboard);

        // הפיכת רקע הדיאלוג לשקוף כדי שיראו את העיצוב שלנו מה-XML
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        RecyclerView recyclerView = dialog.findViewById(R.id.leaderboard_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        List<User> leaderboardUsers = new ArrayList<>();
        LeaderboardAdapter adapter = new LeaderboardAdapter(leaderboardUsers);
        recyclerView.setAdapter(adapter);

        // משיכת נתונים מ-Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    leaderboardUsers.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String username = doc.getString("username");
                        Long rating = doc.getLong("rating");
                        if (username != null && rating != null) {
                            leaderboardUsers.add(new User(username, rating.intValue()));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });

        Button btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // הגדרת גודל הדיאלוג (90% מרוחב המסך)
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
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
        int rank = position + 1;

        holder.txtRank.setText(String.valueOf(rank));
        holder.txtUsername.setText(user.getUsername());
        holder.txtRating.setText(user.getRating() + " PTS");

        // לוגיקת עיצוב ניאון לפי מקום (1, 2, 3)
        switch (rank) {
            case 1: // זהב ניאון (The King)
                holder.txtRank.setTextColor(android.graphics.Color.parseColor("#FFD700"));
                holder.txtUsername.setTextColor(android.graphics.Color.parseColor("#FFD700"));
                holder.itemContainer.setBackgroundColor(android.graphics.Color.parseColor("#33FFD700"));
                // אם תרצה להוסיף כתר מצד שמאל:
                // holder.txtUsername.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_crown, 0, 0, 0);
                break;
            case 2: // כסף ניאון
                holder.txtRank.setTextColor(android.graphics.Color.parseColor("#C0C0C0"));
                holder.txtUsername.setTextColor(android.graphics.Color.parseColor("#C0C0C0"));
                holder.itemContainer.setBackgroundColor(android.graphics.Color.parseColor("#11FFFFFF"));
                break;
            case 3: // ברונזה ניאון
                holder.txtRank.setTextColor(android.graphics.Color.parseColor("#CD7F32"));
                holder.txtUsername.setTextColor(android.graphics.Color.parseColor("#CD7F32"));
                holder.itemContainer.setBackgroundColor(android.graphics.Color.parseColor("#11CD7F32"));
                break;
            default: // שאר השחקנים - צהוב ניאון "פיפ-בוי"
                holder.txtRank.setTextColor(android.graphics.Color.parseColor("#CCFF00"));
                holder.txtUsername.setTextColor(android.graphics.Color.WHITE);
                holder.itemContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    // ViewHolder שמחזיק את האלמנטים של השורה
    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView txtUsername, txtRating, txtRank;
        View itemContainer;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtRating = itemView.findViewById(R.id.txtRating);
            txtRank = itemView.findViewById(R.id.txtRank);
            itemContainer = itemView.findViewById(R.id.item_container);
        }
    }

    // מחלקת המודל - חייבת להיות static אם היא בתוך האדאפטר
    public static class User {
        public String username;
        public int rating;

        public User() {} // נחוץ עבור Firebase

        public User(String username, int rating) {
            this.username = username;
            this.rating = rating;
        }

        public String getUsername() { return username; }
        public int getRating() { return rating; }
    }
}