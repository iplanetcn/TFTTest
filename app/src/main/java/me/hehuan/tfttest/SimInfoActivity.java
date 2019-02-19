package me.hehuan.tfttest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Arrays;

public class SimInfoActivity extends AppCompatActivity {

    TextView tvPermissionResult;
    TextView tvSubid;
    TextView tvOperatorname;
    Button btnRequestPermission;
    Button btnGetInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_info);

        tvPermissionResult = findViewById(R.id.tv_has_read_phone_permission);
        tvSubid = findViewById(R.id.tv_get_subscribeId);
        tvOperatorname = findViewById(R.id.tv_operatorname);
        btnRequestPermission = findViewById(R.id.btn_request_permission);
        btnGetInfo = findViewById(R.id.btn_getinfo);

        btnRequestPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPhonePermissions();
            }
        });
        btnGetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] info = getIMSI();
                tvSubid.setText(Arrays.toString(info));
                tvOperatorname.setText("运营商：\n");
                tvOperatorname.append("卡1：");
                tvOperatorname.append(getSimOperatorByMnc(info[0]));
                tvOperatorname.append("\n卡2：");
                tvOperatorname.append(getSimOperatorByMnc(info[1]));
            }
        });

        hasPermission();

    }

    private void requestPhonePermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1000);
    }

    private String getDefaultSubId() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Activity.TELEPHONY_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return "无权限";
        }
        String id = tm.getSubscriberId();
        return id;

    }

    private void hasPermission() {
        int d = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        boolean result = d == PackageManager.PERMISSION_GRANTED;
        tvPermissionResult.setText("" + d + "---" + result);
    }

    public String getSimOperatorByMnc(String id) {
        if (TextUtils.isEmpty(id) ||!id.startsWith("460")) {
            return "传参错误：" + id;
        }

        String operator = id.substring(0, 5);
        switch (operator) {
            case "46000":
            case "46002":
            case "46007":
                return "中国移动";
            case "46001":
            case "46006":
                return "中国联通";
            case "46003":
            case "46005":
            case "46008":
            case "46009":
            case "46010":
            case "46011":
                return "中国电信";
            default:
                return operator;
        }
    }

    /**
     * 获取双卡手机的两个卡的IMSI 需要 READ_PHONE_STATE 权限
     *
     * @return 下标0为一卡的IMSI，下标1为二卡的IMSI
     */
    @SuppressLint("MissingPermission")
    public String[] getIMSI() {
        // 双卡imsi的数组
        String[] imsis = new String[2];
        imsis[0] = getDefaultSubId();

        // 然后进行二卡IMSI的获取,默认先获取展讯的IMSI
        TelephonyManager tm = (TelephonyManager) getSystemService(Activity.TELEPHONY_SERVICE);
        try {
            Method method = tm.getClass().getDeclaredMethod("getSubscriberIdGemini", int.class);
            method.setAccessible(true);
            // 0 表示 一卡，1 表示二卡，下方获取相同
            imsis[1] = (String) method.invoke(tm, 1);
        } catch (Exception e) {
            // 异常清空数据，继续获取下一个
            imsis[1] = null;
        }
        if (imsis[1] == null || "".equals(imsis[1])) { // 如果二卡为空就获取mtk
            try {
                Class<?> c = Class.forName("com.android.internal.telephony.PhoneFactory");
                Method m = c.getMethod("getServiceName", String.class,
                        int.class);
                String spreadTmService = (String) m.invoke(c,
                        Context.TELEPHONY_SERVICE, 1);
                TelephonyManager tm1 = (TelephonyManager)getSystemService(spreadTmService);
                imsis[1] = tm1.getSubscriberId();
            } catch (Exception ex) {
                imsis[1] = null;
            }
        }
        if (imsis[1] == null || "".equals(imsis[1])) { // 如果二卡为空就获取高通 IMSI获取
            try {
                Method addMethod2 = tm.getClass().getDeclaredMethod(
                        "getSimSerialNumber", int.class);
                addMethod2.setAccessible(true);
                imsis[1] = (String) addMethod2.invoke(tm, 1);
            } catch (Exception ex) {
                imsis[1] = null;
            }
        }
        return imsis;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1000) {
            hasPermission();
        }

    }
}
