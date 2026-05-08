package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private TextView tvCountrySelector;
    private EditText etPhoneNumber;
    private Button btnCaptchaLogin, btnPasswordLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tvCountrySelector = findViewById(R.id.tv_country_selector);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        btnCaptchaLogin = findViewById(R.id.btn_login_captcha);
        btnPasswordLogin = findViewById(R.id.btn_login_password);

        // 1. 点击选择国家和地区区号 (使用 BottomSheetDialogFragment 底部弹窗)
        tvCountrySelector.setOnClickListener(v -> {
            CountrySelectFragment bottomSheet = new CountrySelectFragment();
            // 接收弹窗传回来的区号
            bottomSheet.setOnCountrySelectListener(countryCode -> {
                tvCountrySelector.setText(countryCode);
                // 切换区号后重新校验一次手机号位数，刷新按钮颜色
                checkInputStatus(etPhoneNumber.getText().toString());
            });
            // 显示 3/4 屏幕的底部弹窗
            bottomSheet.show(getSupportFragmentManager(), "CountrySelect");
        });

        // 2. 监听手机号输入
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInputStatus(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. 按钮点击跳转逻辑
        btnCaptchaLogin.setOnClickListener(v -> {
            // 跳转到验证码验证界面
            Intent intent = new Intent(LoginActivity.this, CaptchaVerifyActivity.class);
            intent.putExtra("phone", etPhoneNumber.getText().toString());
            // 如果需要带上区号，可以加上下面这行：
            // intent.putExtra("countryCode", tvCountrySelector.getText().toString());
            startActivity(intent);
        });

        btnPasswordLogin.setOnClickListener(v -> {
            // 跳转到密码输入界面
            Intent intent = new Intent(LoginActivity.this, PasswordInputActivity.class);
            intent.putExtra("phone", etPhoneNumber.getText().toString());
            // intent.putExtra("countryCode", tvCountrySelector.getText().toString());
            startActivity(intent);
        });
    }

    // 检查输入状态并动态切换按钮颜色
    private void checkInputStatus(String phone) {
        String currentCode = tvCountrySelector.getText().toString();
        boolean isValid = false;

        if (currentCode.equals("+86")) {
            // +86 必须满11位
            isValid = phone.length() == 11;
        } else {
            // 其他地区暂定非空即可，可根据需要增加规则
            isValid = phone.length() > 5;
        }

        if (isValid) {
            // 输入合法：变为红底白字，并启用按钮
            btnCaptchaLogin.setEnabled(true);
            btnCaptchaLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));

            btnPasswordLogin.setEnabled(true);
            btnPasswordLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED));
        } else {
            // 输入不合法：恢复灰底白字，并禁用按钮
            btnCaptchaLogin.setEnabled(false);
            btnCaptchaLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#CCCCCC")));

            btnPasswordLogin.setEnabled(false);
            btnPasswordLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        }
    }
}