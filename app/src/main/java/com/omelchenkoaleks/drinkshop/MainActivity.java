package com.omelchenkoaleks.drinkshop;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.omelchenkoaleks.drinkshop.model.CheckUserResponse;
import com.omelchenkoaleks.drinkshop.model.User;
import com.omelchenkoaleks.drinkshop.retrofit.IDrinkShopAPI;
import com.omelchenkoaleks.drinkshop.utils.Common;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.szagurskii.patternedtextwatcher.PatternedTextWatcher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    IDrinkShopAPI mService;
    Button continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mService = Common.getAPI();

        continueBtn = findViewById(R.id.continue_btn);
        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLoginPage(LoginType.PHONE);
            }
        });
    }

    private void startLoginPage(LoginType loginType) {
        Intent intent = new Intent(this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder builder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(loginType,
                        AccountKitActivity.ResponseType.TOKEN);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, builder.build());
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);

            if (result.getError() != null) {
                Toast.makeText(this,
                        "" + result.getError().getErrorType().getMessage(), Toast.LENGTH_LONG)
                        .show();
            } else if (result.wasCancelled()) {
                Toast.makeText(this, "Cancel", Toast.LENGTH_LONG).show();
            } else {
                if (result.getAccessToken() != null) {
                    final AlertDialog alertDialog= new SpotsDialog.Builder()
                            .setContext(MainActivity.this).build();
                    alertDialog.show();
                    alertDialog.setMessage("Please waiting ...");

                    // Get User phone and Check exists on server
                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(final Account account) {
                            mService.checkUserExists(account.getPhoneNumber().toString())
                                    .enqueue(new Callback<CheckUserResponse>() {
                                        @Override
                                        public void onResponse(Call<CheckUserResponse> call,
                                                               Response<CheckUserResponse> response) {
                                            CheckUserResponse userResponse = response.body();
                                            if (userResponse.isExists()) {
                                                // If User already exists, just start new Activity
                                                alertDialog.dismiss();
                                            } else {
                                                // Else, need register
                                                alertDialog.dismiss();
                                                showRegisterDialog(account.getPhoneNumber().toString());
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<CheckUserResponse> call,
                                                              Throwable t) {

                                        }
                                    });
                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {
                            Log.d("ERROR", accountKitError.getErrorType().getMessage());
                        }
                    });
                }
            }
        }
    }

    private void showRegisterDialog(final String phone) {
        final AlertDialog alertDialog= new SpotsDialog.Builder()
                .setContext(MainActivity.this).build();
        alertDialog.setTitle("REGISTER");

        LayoutInflater inflater = this.getLayoutInflater();
        View register_layout = inflater.inflate(R.layout.register_layout, null);

        final MaterialEditText name_et = (MaterialEditText) register_layout.findViewById(R.id.name_et);
        final MaterialEditText address_et = (MaterialEditText) register_layout.findViewById(R.id.address_et);
        final MaterialEditText birthdate_et = (MaterialEditText) register_layout.findViewById(R.id.birthdate_et);

        Button register_btn = register_layout.findViewById(R.id.register_btn);

        birthdate_et.addTextChangedListener(new PatternedTextWatcher("####-##-##"));

        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(address_et.getText().toString())) {
                    Toast.makeText(MainActivity.this,
                            "Please enter your address ...",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(birthdate_et.getText().toString())) {
                    Toast.makeText(MainActivity.this,
                            "Please enter your birthdate ...",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (TextUtils.isEmpty(name_et.getText().toString())) {
                    Toast.makeText(MainActivity.this,
                            "Please enter your name",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                final AlertDialog watingDialog = new SpotsDialog.Builder()
                        .setContext(MainActivity.this).build();
                watingDialog.show();
                watingDialog.setMessage("Please waiting ...");

                mService.registerNewUser(phone,
                        name_et.getText().toString(),
                        address_et.getText().toString(),
                        birthdate_et.getText().toString())
                        .enqueue(new Callback<User>() {
                            @Override
                            public void onResponse(Call<User> call, Response<User> response) {
                                watingDialog.dismiss();
                                User user = response.body();
                                if (TextUtils.isEmpty(user.getError_msg())) {
                                    Toast.makeText(MainActivity.this,
                                            "User register successfully!",
                                            Toast.LENGTH_LONG).show();
                                    // Start new Activity
                                }
                            }

                            @Override
                            public void onFailure(Call<User> call, Throwable t) {
                                watingDialog.dismiss();
                            }
                        });
            }
        });

        alertDialog.setView(register_layout);
        alertDialog.show();
    }

    // Generate Key for Facebook.
    private void printKeyHadh() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.omelchenkoaleks.drinkshop",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KEYHASH", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
