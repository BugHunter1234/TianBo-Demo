package com.jiaobaowang.tianbo;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;

import java.util.List;

/**
 * 主页
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Context mContent;
    TextView qrcodeTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //页面初始化
        mContent = this;
        TextView deviceTypeTv = findViewById(R.id.device_type_tv);
        qrcodeTv = findViewById(R.id.qrcode_tv);
        Button idCardReadBtn = findViewById(R.id.id_card_read_btn);
        Button idCardOCRdBtn = findViewById(R.id.id_card_ocr_btn);
        Button usbPrinterBtn = findViewById(R.id.usb_printer_btn);
        Button qrcodeBtn = findViewById(R.id.qrcode_btn);
        //读取二代身份证
        idCardReadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContent, IdCardReadActivity.class));
            }
        });
        //识别二代身份证
        idCardOCRdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContent, IdCardOCRActivity.class));
            }
        });
        //热敏打印机
        usbPrinterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContent, UsbPrinterActivity.class));
            }
        });
        //热敏打印机
        usbPrinterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContent, UsbPrinterActivity.class));
            }
        });
        //识别二维码
        qrcodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPackage("com.telpo.tps550.api")) {
                    Intent intent = new Intent();
                    intent.setClassName("com.telpo.tps550.api", "com.telpo.tps550.api.barcode.Capture");
                    try {
                        startActivityForResult(intent, 0x124);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.identify_fail), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.identify_fail), Toast.LENGTH_LONG).show();
                }
            }
        });
        //获取设备型号
        int deviceType = SystemUtil.getDeviceType();
        StringUtil.DeviceModelEnum[] values = StringUtil.DeviceModelEnum.values();
        String deviceName = "设备型号:" + values[deviceType];
        Log.i(TAG, deviceName);
        deviceTypeTv.setText(deviceName);
    }

    private boolean checkPackage(String packageName) {
        PackageManager manager = this.getPackageManager();
        Intent intent = new Intent().setPackage(packageName);
        @SuppressLint("WrongConstant") List<ResolveInfo> infos = manager.queryIntentActivities(intent, PackageManager.GET_INTENT_FILTERS);
        if (infos == null || infos.size() < 1) {
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0x124) {
            if (resultCode == 0) {
                if (data != null) {
                    String qrcode = data.getStringExtra("qrCode");
                    String qrcodeText = "条码/二维码：" + qrcode;
                    Log.i(TAG, qrcodeText);
                    qrcodeTv.setText(qrcodeText);
                }
            } else {
                qrcodeTv.setText("条码/二维码：扫描失败");
                Toast.makeText(MainActivity.this, "扫描失败", Toast.LENGTH_LONG).show();
            }
        }

    }
}
