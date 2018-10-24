package com.regiumconsulting.donationssumup;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Process;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sumup.merchant.Models.TransactionInfo;
import com.sumup.merchant.api.SumUpAPI;
import com.sumup.merchant.api.SumUpLogin;
import com.sumup.merchant.api.SumUpPayment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_PAYMENT = 2;

    private static final String LOG_TAG = "DevLog";

//   Only needed for Transparent Authentication
    private static final String CLIENT_ID = "jwFgmtzHXvyQEPH-m5Z0nb8cpFhB";
    private static final String SECRET = "8718e1644ecab66df485c5a9b33c181aa548d86109393fe3bc8a4ee35a3a7390";
    private static final String RED_URI = "myapp://com.regiumconsulting.donationssumup";


    private Button sterling5;
    private Button sterling10;
    private Button sterling15;
    private Button sterling20;
    private TextView maintext;
    private ConstraintLayout mainview;
    private Button[] chosenone = new Button[1];
    private Map<Button, BigDecimal> buttons = new HashMap<>();
    private BigDecimal amount;
    private Thread loginthread;
    private Thread paythread;
    private AlertDialog notloggedin;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        setBGThreads();
        loginthread.start();


        // All Button Clicks
        // 5 pound button clicked
        sterling5.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                btnClick(sterling5);
            }
        });

        // 10 pound button clicked
        sterling10.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                btnClick(sterling10);
            }
        });

        // 15 pound button clicked
        sterling15.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                btnClick(sterling15);
            }
        });

        // 20 pound button clicked
        sterling20.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                btnClick(sterling20);
            }
        });

        mainview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                screenClick();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CODE_LOGIN:
                if (data != null) {
                    Bundle extra = data.getExtras();
                    if (extra.getInt(SumUpAPI.Response.RESULT_CODE) != 1){
                        Log.d(LOG_TAG, "Could Not Sign In to Sum Up");
                        break;
                    }
                    Log.d(LOG_TAG, String.valueOf(extra.getInt(SumUpAPI.Response.RESULT_CODE)));
                    Log.d(LOG_TAG, extra.getString(SumUpAPI.Response.MESSAGE));
                    prepCheckout();
                }
                break;

            case REQUEST_CODE_PAYMENT:
                if (data != null) {
                    Bundle extra = data.getExtras();
                    if (extra.getInt(SumUpAPI.Response.RESULT_CODE) != 1){
                        Log.d(LOG_TAG, "FAILED TRANSACTION!!!");
                        paymentMessage(false);
                        break;
                    }

                    Log.i(LOG_TAG, "Payment Succeeded!");
                    paymentMessage(true);

                    Log.d(LOG_TAG, String.valueOf(extra.getInt(SumUpAPI.Response.RESULT_CODE)));
                    Log.d(LOG_TAG, extra.getString(SumUpAPI.Response.MESSAGE));


                    String txCode = extra.getString(SumUpAPI.Response.TX_CODE);
                    Log.d(LOG_TAG, txCode == null ? "" : "Transaction Code: " + txCode);

                    TransactionInfo transactionInfo = extra.getParcelable(SumUpAPI.Response.TX_INFO);
                    Log.d(LOG_TAG, transactionInfo == null ? "" : "Transaction Info: " + transactionInfo);

                }
                break;

            default:
                break;
        }
    }

            // All helper functions

    // Interface Helpers
    @SuppressLint("ResourceAsColor")
    private void btnClick(Button btn){
        if (chosenone[0] != null){
            chosenone[0].setBackgroundResource(android.R.drawable.btn_default_small);
        }
        chosenone[0] = btn;
        amount = buttons.get(btn);

        btn.setBackgroundColor(Color.rgb(37, 219, 131));
        maintext.setText(R.string.selected);
        maintext.append("  " + amount.toString());

        paythread.start();

    }

    private void screenClick(){
        maintext.setText(R.string.select_msg);
        if (chosenone[0] != null){
            chosenone[0].setBackgroundResource(android.R.drawable.btn_default);
        }

    }



// SumUp Helpers

//    If transparent authentication required, please refer to:
//      http://docs.sumup.com/oauth/#header-authorization-code-grant
    private void loginSumUp(){
        try {
            SumUpLogin sumupLogin = SumUpLogin.builder("e6a8a33a-3982-47d9-8aeb-afb0a63c45b3").build();
//            *** Transparent Auth - token required ***
//            SumUpLogin sumupLogin = SumUpLogin.builder("e6a8a33a-3982-47d9-8aeb-afb0a63c45b3").accessToken(token).build();
//            ***  ---  --- --- --- --- --- --- --- ***
            SumUpAPI.openLoginActivity(MainActivity.this, sumupLogin, REQUEST_CODE_LOGIN);
        } catch (Exception e){
            Log.d(LOG_TAG, "Unable to Log in to SumUp");
            notloggedin.show();
        }
    }

    private void prepCheckout(){
        if (SumUpAPI.isLoggedIn()) {
            SumUpAPI.prepareForCheckout();
            Log.i(LOG_TAG, "Prepared for Checkout beforehand");
        }
        else{
            Log.d(LOG_TAG, "NOT LOGGED IN. Cannot prepare checkout");
        }
    }

    private void makePayment(BigDecimal amnt){
        Log.i(LOG_TAG, "Amount to be paid = " + amnt);
        if (SumUpAPI.isLoggedIn()) {
            SumUpPayment payment = SumUpPayment.builder()
                    // mandatory parameters
                    .total(amnt) // minimum 1.00
                    .currency(SumUpPayment.Currency.GBP)
                    .skipSuccessScreen()
                    .build();

            Log.i(LOG_TAG, "Payment Setup");

            SumUpAPI.checkout(MainActivity.this, payment, REQUEST_CODE_PAYMENT);
            Log.i(LOG_TAG, "Ready to Process Payment");
        }
        else{
            Log.d(LOG_TAG, "NOT LOGGED IN. Cannot pay!");
            notloggedin.show();
        }
    }

    private void paymentMessage(boolean success){
        String snackmsg = "";
        String toastmsg = "";
        int backcolor;

        if (!success){
            snackmsg += "Failed Payment: £" + amount.toString();
            toastmsg += "Sorry!";
            backcolor = Color.rgb(255, 77, 77);
        } else{
            snackmsg += "Payment Confirmed: £" + amount.toString();
            toastmsg += "Thank you!";
            backcolor = Color.rgb(72, 185, 131);
        }

        Snackbar mysnack = Snackbar.make(MainActivity.this.mainview, snackmsg, Snackbar.LENGTH_SHORT);
        View snackview = mysnack.getView();
        try {
            snackview.setBackgroundColor(backcolor);
        }catch (Exception e){ e.printStackTrace();}

        mysnack.show();

        Toast.makeText(MainActivity.this, toastmsg, Toast.LENGTH_LONG).show();

    }



    // Helpers to set up app

    // register all views and alerts
    private void findViews(){
        sterling5 = findViewById(R.id.fivebtn);
        sterling10 = findViewById(R.id.tenbtn);
        sterling15 = findViewById(R.id.fifteenbtn);
        sterling20 = findViewById(R.id.twentybtn);
        maintext = findViewById(R.id.maintxt);
        mainview = findViewById(R.id.fullscreen);

        buttons.put(sterling5, new BigDecimal(5));
        buttons.put(sterling10, new BigDecimal(10));
        buttons.put(sterling15, new BigDecimal(15));
        buttons.put(sterling20, new BigDecimal(20));

        notloggedin = new AlertDialog.Builder(MainActivity.this).create();
        notloggedin.setTitle("ERROR");
        notloggedin.setMessage("Not Logged In to SumUp");
        notloggedin.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        loginthread.start();
                        dialog.dismiss();
                    }
                });

        Log.i(LOG_TAG, "All Views Found");
    }

    // Background Threads to handle login and payment
    private void setBGThreads(){

        loginthread = new Thread( new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                if(!SumUpAPI.isLoggedIn()){
                    loginSumUp();
                }
            }
        });

        paythread = new Thread( new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                makePayment(amount);
            }
        });

    }

}
