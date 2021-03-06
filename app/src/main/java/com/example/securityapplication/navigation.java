package com.example.securityapplication;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.securityapplication.Helper.FirebaseHelper;
import com.example.securityapplication.model.Device;
import com.example.securityapplication.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class navigation extends AppCompatActivity {

    int count=0,aa;
    static User newUser=new User();
    Boolean is_home=true;

    SQLiteDBHelper db=new SQLiteDBHelper(navigation.this);
    public static Boolean test=false;
    public static TextView tmode;
    public static TextView tmode1;

    private int flag=0;
    Menu optionsMenu;

    private FirebaseUser firebaseUser;
    private String TAG = "NavigatonFragment";
    private String mImeiNumber;
    private TelephonyManager telephonyManager;

    public static ValueEventListener mUsersDatabaseReferenceListener;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_navigation);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListner);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_continer,new home_fragment()).commit();

        getImei();

        firebaseHelper = FirebaseHelper.getInstance();
        firebaseHelper.initFirebase();
        firebaseHelper.initContext(navigation.this);
        firebaseHelper.initGoogleSignInClient(getString(R.string.server_client_id));

        firebaseUser = firebaseHelper.getFirebaseAuth().getCurrentUser();

        //sqlite db code here
        Log.d("checking","oncreate option menu 3 is running");
        getData();
        if(db.getTestmode())
        {
            Log.d("checking","oncreate option menu 2 is running");
            test=true;
        }

        tmode1=(TextView)findViewById(R.id.testmode);
        //initialise testmode textview from db
        test=db.getTestmode();
        Log.d("checking","oncreate option menu is running"+db.getTestmode());
        if(test)
        {
            tmode1.setText("TEST MODE : ON");
            tmode1.setTextColor(Color.GREEN);
        }
        else
        {
            tmode1.setTextColor(Color.WHITE);
            tmode1.setText("TEST MODE : OFF");
        }

        tmode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag==0){
                    flag=1;
                    tmode1.setText("TEST MODE : ON");

                  //  tmode1.setPadding(0,0,10,0);
                    tmode1.setTextColor(Color.GREEN);
                }
                else {
                    flag=0;
                    tmode1.setTextColor(Color.WHITE);
                    tmode1.setText("TEST MODE : OFF");
                   // tmode.setTextColor(Color.WHITE);

                }
                //update testmode value in db
                test= (flag==1); //using the global static variable instead of the local variable
                db.updatetestmode(test);
            }
        });
    }

    private void checkFirstSosContact(){
        // check if first sos contact is added
        Cursor res=db.getSosContacts();
        if (res.getCount() == 0){
            //Toast.makeText(getApplicationContext(), "No SOS Contact records Found", Toast.LENGTH_LONG).show();
            Log.d("SOS Activity","No Contact Data found ");
            Intent sosPage = new Intent(navigation.this, sos_page.class);
            startActivityForResult(sosPage,1);
        }
    }

    public void getData(){

        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            mUsersDatabaseReferenceListener = firebaseHelper.getUsersDatabaseReference().child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    newUser = dataSnapshot.getValue(User.class);
                    Log.d("Paid12345","schin1"+newUser.getName()+newUser
                            .isPaid());
                    Log.d("FirebaseUsername", newUser.getName() + " 2 " + newUser.getEmail());
                    db.updateUser(newUser);
                    db.setUser(newUser);
                    if (newUser.getSosContacts() != null)
                        db.addsosContacts(newUser.getSosContacts()); //to fetch SOSContacts from Firebase even if tablepresent

                    // check if user signed in from two devices
                    recheckUserAuthentication();

                    SendSMSService.initContacts(); //to initialise SOS Contacts as soon as the database is ready
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            checkFirstSosContact();
        }
    }


    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu,menu);
        optionsMenu=menu;
        MenuItem titem=optionsMenu.findItem(R.id.testmode);
        test=db.getTestmode();
        Log.d("checking","oncreate option menu is running"+db.getTestmode());
        if(test)
            titem.setChecked(true);
        else
            titem.setChecked(false);
        return true;
    }*/

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.testmode:
                    if (item.isChecked()) {
                        item.setChecked(false);
                        test = false;
                        db.updatetestmode(test);
                        //Log.d("checking1", String.valueOf(db.getTestmode()) + "home" + is_home);
                        Toast.makeText(this, "Test mode Off", Toast.LENGTH_SHORT).show();
                        if (is_home) {
                            TextView tv = (TextView) findViewById(R.id.textView3);
                            tv.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        item.setChecked(true);
                        test = true;
                        db.updatetestmode(test);
                        //Log.d("checking2", String.valueOf(db.getTestmode()));
                        Toast.makeText(this, "Test mode On", Toast.LENGTH_SHORT).show();
                        if (is_home) {
                            TextView tv = (TextView) findViewById(R.id.textView3);
                            tv.setVisibility(View.VISIBLE);
                        }
                    }


                default:
                    return super.onOptionsItemSelected(item);
            }
        }
        catch (Exception e)
        {
            item.setChecked(db.getTestmode());
            Toast.makeText(this, "Loading.....please wait for a second", Toast.LENGTH_LONG).show();
        }
        finally {
            return true;
        }

    }*/
    Fragment selectedFragment = null;
    private BottomNavigationView.OnNavigationItemSelectedListener navListner =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                    switch(menuItem.getItemId()){
                        case R.id.home:
                            is_home=true;
                            selectedFragment = new home_fragment();
                            break;
                        case R.id.setting:
                            is_home=false;
                            selectedFragment = new setting_fragment();
                            break;
                        case R.id.save:
                            is_home=false;
                            selectedFragment = new saviour_fragment();
                            break;
                        case R.id.profile:
                            is_home=false;
                            selectedFragment = new profile_fragment();
                            break;
                    }

                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_continer,
                            selectedFragment).commit();

                    return true;
                }
    };

    @Override
    public void onBackPressed(){
        AlertDialog.Builder a_builder = new AlertDialog.Builder(navigation.this);
        a_builder.setMessage("Do you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        AlertDialog alert = a_builder.create();
        alert.setTitle("Message");
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==10 && requestCode==1)
            try {
                closeNow();
            }catch (Exception e){
                Log.d(TAG,"Exception on closing activity:"+e.getMessage());
                finish();
            }
    }

    public void sos(View view) {
        startActivity(new Intent(this,sos_page.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImei();
                } else {
                    closeNow();
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void closeNow(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            finishAffinity();
        }
        else{
            finish();
        }
    }

    private void getImei(){
        User user=db.getUser();
        Log.d(TAG,"User:"+user);
        if (user != null)
            if (user.getImei() != null) {
                Log.d(TAG,"Imei in db:"+user.getImei());
                mImeiNumber = user.getImei();
                return;
            }

        telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 101);
            return;
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mImeiNumber = telephonyManager.getImei(0);
                Log.d("IMEI", "IMEI Number of slot 1 is:" + mImeiNumber);
            } else {
                mImeiNumber = telephonyManager.getDeviceId();
            }
            try{
                user.setImei(mImeiNumber);
                db.setUser(user);
                db.updateUser(user);
            }catch (Exception e){
                Log.d(TAG,e.getMessage());
            }
        }
    }

    private void recheckUserAuthentication(){
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return;
        Log.d(TAG,FirebaseAuth.getInstance().getCurrentUser().getEmail());
        Log.d(TAG,firebaseUser.getEmail());
        Log.d(TAG,"Inside recheckUserAuthentication");
        if (mImeiNumber==null) {
            getImei();
            return;
        }
        Log.d(TAG,"Imei of device:"+mImeiNumber);
        Log.d(TAG,"Imei from firebase:"+newUser.getImei());
        if (!newUser.getImei().equals(mImeiNumber)){
            // same user trying to login from multiple devices -> logout the user
            Log.d(TAG, "User is LoggedIn in other device");
            Toast.makeText(navigation.this,"You are logged in another device .Please logout from old device to continue", Toast.LENGTH_LONG).show();
            LogOutAndStartMainActivity();
        }
    }

    public void LogOutAndStartMainActivity(){
        firebaseHelper.getUsersDatabaseReference().child(firebaseUser.getUid()).removeEventListener(mUsersDatabaseReferenceListener);
        firebaseHelper.makeDeviceImeiNull(mImeiNumber);
        firebaseHelper.firebaseSignOut();
        firebaseHelper.googleSignOut(navigation.this);
        //delete user records from SQLite
        db.deleteDatabase(navigation.this);

        Intent mLogOutAndRedirect= new Intent(navigation.this, MainActivity.class);
        mLogOutAndRedirect.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mLogOutAndRedirect);
        //finishing the navigation activity
        try {
            closeNow();
            Log.d(TAG,"closed activity successfully");
        }catch (Exception e){
            Log.d(TAG,"Closing app exception:"+e.getMessage());
            finish();
        }
    }
}
