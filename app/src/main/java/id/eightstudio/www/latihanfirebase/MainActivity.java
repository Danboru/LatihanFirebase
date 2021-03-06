package id.eightstudio.www.latihanfirebase;

import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import id.eightstudio.www.latihanfirebase.Adapter.MainAdapter;
import id.eightstudio.www.latihanfirebase.Models.Message;
import id.eightstudio.www.latihanfirebase.Utils.MyUtils;

public class MainActivity extends AppCompatActivity {

    private Button button_send;
    private EditText editText_message;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private ArrayList<Message> messagesList = new ArrayList<>();
    private ListView main_listview;
    private MainAdapter mainAdapter;
    private FirebaseAnalytics mFirebaseAnalytics;
    private String test_string;
    private String username;
    private MainActivity mContext;
    private TextView textView_is_typing;
    private MyCountDownTimmer isTypingTimmer = new MyCountDownTimmer(1000, 1000);
    private boolean isTyping = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = MainActivity.this;

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mFirebaseAnalytics.setUserProperty("user_type", "author");

        button_send = (Button) findViewById(R.id.button_send);
        editText_message = (EditText) findViewById(R.id.editText_message);
        textView_is_typing = (TextView) findViewById(R.id.textView_is_typing);
        main_listview = (ListView) findViewById(R.id.main_listview);
        username = getSharedPreferences("PREFS", 0).getString("username", "Anonymous");
        textView_is_typing.setVisibility(View.INVISIBLE);

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();

        mainAdapter = new MainAdapter(mContext, messagesList);
        main_listview.setAdapter(mainAdapter);



        test_string = null;

        //Kirim pesan
        button_send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {

                //removes the "is typing" from the other person's screen
                isTypingTimmer.cancel();
                databaseReference.child("room-typing").child("irc").child(username).setValue(false);


                process_message(editText_message.getText().toString().trim());
                editText_message.setText("");
            }
        });

        //Set user defaulth
        databaseReference.child("users").child(MyUtils.generateUniqueUserId(mContext)).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                username = dataSnapshot.getValue(String.class);
                if (username == null) {
                    username = "Anonymous";
                }
            }

            @Override public void onCancelled(DatabaseError databaseError) {
            }
        });

        //Fungsi insert update delete bisa di lihat di = https://firebase.google.com/docs/database/admin/retrieve-data
        //Belajar insert update delete = https://stackoverflow.com/questions/41869606/update-and-delete-data-in-firebase

        //Child Added
        databaseReference.child("db_messages").limitToLast(20).addChildEventListener(new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                messagesList.add(message);
                mainAdapter.notifyDataSetChanged();
                Log.d("message", message.toString());
            }

            @Override public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d("onChildChanged", dataSnapshot.toString());
            }

            @Override public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d("onChildRemoved", dataSnapshot.toString());
            }

            @Override public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                Log.d("onChildMoved", dataSnapshot.toString());
            }

            @Override public void onCancelled(DatabaseError databaseError) {
                Log.d("onCancelled", databaseError.toString());
            }
        });

        //Child Changed
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                Message message = dataSnapshot.getValue(Message.class);
                System.out.println("The updated post title is: " + message.message);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });


        //Child Remove
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Message message = dataSnapshot.getValue(Message.class);
                System.out.println("The blog post titled " + message.message + " has been deleted");
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        //Method untuk merekam input data
        editText_message.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isTypingTimmer.cancel();
                isTypingTimmer.start();
                if (!isTyping) {
                    databaseReference.child("room-typing").child("irc").child(username).setValue(true);
                    isTyping = true;
                }
            }
        });

        databaseReference.child("room-typing").child("irc").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {

                HashMap<String, Boolean> hashMap = (HashMap<String, Boolean>) dataSnapshot.getValue();
                if (hashMap == null) {
                    return;
                }
                if (hashMap.containsKey(username)) {
                    hashMap.remove(username);
                }
                String output = "";
                for (String key : hashMap.keySet()) {
                    if (hashMap.get(key).equals(true)) {
                        output = output + key + " ";
                    }
                }
                if (!output.isEmpty()) {
                    textView_is_typing.setText(output + " is typing");
                    textView_is_typing.setVisibility(View.VISIBLE);
                } else {
                    textView_is_typing.setVisibility(View.INVISIBLE);
                }
            }

            @Override public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    //Fungsi mengirim pesan
    private void process_message(String message) {

        if (message.length() < 1) {
            return;
        }

        //Mengirim database ke server
        String key = databaseReference.child("db_messages").push().getKey();//Menggenerate auto key
        Message post = new Message(MyUtils.generateUniqueUserId(mContext), username, message, System.currentTimeMillis() / 1000L);
        Map<String, Object> postValues = post.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/db_messages/" + key, postValues);
        databaseReference.updateChildren(childUpdates);
    }

    //Create instance Option Menu
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.main_activity, menu);
        return (super.onCreateOptionsMenu(menu));
    }

    //Option menu untuk ubah user
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.username:

                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                final EditText edittext = new EditText(this);
                alert.setMessage("Ingin mengganti username ?");
                alert.setTitle(null);

                username = getSharedPreferences("PREFS", 0).getString("username", "Anonymous");
                edittext.setText(username);
                alert.setView(edittext);
                alert.setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String new_username = edittext.getText().toString();
                        mContext.getSharedPreferences("PREFS", 0).edit().putString("username", new_username).commit();
                        username = new_username;
                        databaseReference.child("users").child(MyUtils.generateUniqueUserId(mContext)).setValue(username);
                    }
                });

                alert.setNegativeButton("Batal", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                alert.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Fungsi untuk menampilkan siapa yang sedang mengetik
    public class MyCountDownTimmer extends CountDownTimer {

        public MyCountDownTimmer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override public void onTick(long l) {

        }

        @Override public void onFinish() {
            databaseReference.child("room-typing").child("irc").child(username).setValue(false);
            isTyping = false;
        }
    }


}
