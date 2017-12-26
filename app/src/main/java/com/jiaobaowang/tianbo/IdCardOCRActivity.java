package com.jiaobaowang.tianbo;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.telpo.tps550.api.idcard.IdentityInfo;

import java.util.List;

/**
 * 摄像头识别身份证正面和反面的信息
 */
public class IdCardOCRActivity extends AppCompatActivity {
    private static final String TAG = "IdCardOCRActivity";
    private final int ID_REQ1 = 2;//正面
    private final int ID_REQ2 = 3;//背面
    private Context mContent;
    private Button idCardOCRFrontBtn, idCardOCRBackBtn;
    private TextView idCardOCRFrontTv, idCardOCRBackTv;
    private ImageView idCardImage;//二代身份证图片

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_card_ocr);
        //页面初始化
        mContent = this;
        idCardOCRFrontBtn = findViewById(R.id.id_card_ocr_front_btn);
        idCardOCRBackBtn = findViewById(R.id.id_card_ocr_back_btn);
        idCardOCRFrontTv = findViewById(R.id.id_card_ocr_front_tv);
        idCardOCRBackTv = findViewById(R.id.id_card_ocr_back_tv);
        idCardImage = findViewById(R.id.id_card_iv);
        idCardOCRFrontBtn.setEnabled(false);
        idCardOCRBackBtn.setEnabled(false);
        //识别身份证正面的信息
        idCardOCRFrontBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idCardOCRFrontBtn.setEnabled(false);
                Intent intent = new Intent();
                intent.setClassName("com.telpo.tps550.api",
                        "com.telpo.tps550.api.ocr.IdCardOcr");
                intent.putExtra("type", true);
                intent.putExtra("show_head_photo", true);

                //intent.putExtra("isKeepPicture", true);// 是否保存图片
                // true是，false:否，不传入时，默认为否
                //intent.putExtra("PictPath", "/sdcard/DCIM/Camera/003.png");// 图片路径，不传入时保存到默认路径/sdcard/OCRPict
                //intent.putExtra("PictFormat", "PNG");// 图片格式：JPEG，PNG，WEBP，不传入时默认为PNG格式
                try {
                    startActivityForResult(intent, ID_REQ1);
                } catch (ActivityNotFoundException exception) {
                    Toast.makeText(mContent,
                            getResources().getString(R.string.identify_fail),
                            Toast.LENGTH_LONG).show();// "未安装API模块，无法进行二维码/身份证识别"
                }
            }
        });
        //识别身份证反面的信息
        idCardOCRBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idCardOCRFrontBtn.setEnabled(false);
                Intent intent = new Intent();
                intent.setClassName("com.telpo.tps550.api",
                        "com.telpo.tps550.api.ocr.IdCardOcr");
                intent.putExtra("type", false);
                try {
                    startActivityForResult(intent, ID_REQ2);
                } catch (ActivityNotFoundException exception) {
                    Toast.makeText(mContent,
                            getResources().getString(R.string.identify_fail),
                            Toast.LENGTH_LONG).show();// "未安装API模块，无法进行二维码/身份证识别"
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPackage("com.telpo.tps550.api")) {
            Toast.makeText(this,
                    getResources().getString(R.string.identify_fail),
                    Toast.LENGTH_LONG).show();// "未安装API模块，无法进行二维码/身份证识别"
            idCardOCRFrontBtn.setEnabled(false);
            idCardOCRBackBtn.setEnabled(false);
        } else {
            idCardOCRFrontBtn.setEnabled(true);
            idCardOCRBackBtn.setEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == 0 && (requestCode == ID_REQ1 || requestCode == ID_REQ2)) {
            IdentityInfo info = null;
            String tocastString;
            try {
                info = (IdentityInfo) data.getSerializableExtra("idInfo");
                if (requestCode == ID_REQ1) {
                    //正面
                    if (info != null && info.getName() != null
                            && info.getNo() != null) {
                        //成功
                        tocastString = getResources().getString(R.string.get_id_front_success);
                        idCardOCRFrontTv.setText("身份证正面信息：" + "\n\n"
                                + getResources().getString(R.string.idcard_xm) + info.getName() + "\n\n"
                                + getResources().getString(R.string.idcard_xb) + info.getSex() + "\n\n"
                                + getResources().getString(R.string.idcard_mz) + info.getNation() + "\n\n"
                                + getResources().getString(R.string.idcard_csrq) + info.getBorn() + "\n\n"
                                + getResources().getString(R.string.idcard_dz) + info.getAddress() + "\n\n"
                                + getResources().getString(R.string.idcard_sfhm) + info.getNo());
                        if (info.getHead_photo() != null) {
                            idCardImage.setImageBitmap(BitmapFactory.decodeByteArray(info.getHead_photo(), 0, info.getHead_photo().length));
                        }
                    } else {
                        //失败
                        idCardOCRFrontTv.setText(getResources().getString(R.string.none));
                        tocastString = getResources().getString(R.string.get_id_front_fail);
                    }
                } else {
                    //反面
                    if (info != null && info.getPeriod() != null
                            && info.getApartment() != null) {
                        //成功
                        tocastString = getResources().getString(R.string.get_id_back_success);
                        idCardOCRBackTv.setText("身份证反面信息：" + "\n\n"
                                + getResources().getString(R.string.idcard_qzjg) + info.getApartment() + "\n\n"
                                + getResources().getString(R.string.idcard_yxqx) + info.getPeriod());
                    } else {
                        //失败
                        idCardOCRBackTv.setText(getResources().getString(R.string.none));
                        tocastString = getResources().getString(R.string.get_id_back_fail);
                    }
                }
                Toast.makeText(this, tocastString, Toast.LENGTH_SHORT).show();
            } catch (NullPointerException e) {
                if (requestCode == ID_REQ1) {
                    idCardOCRFrontTv.setText(getResources().getString(R.string.none));
                    tocastString = getResources().getString(R.string.get_id_front_fail);
                } else {
                    idCardOCRBackTv.setText(getResources().getString(R.string.none));
                    tocastString = getResources().getString(R.string.get_id_back_fail);
                }
                Toast.makeText(this, tocastString, Toast.LENGTH_SHORT).show();
            }
            if (info != null) {
                Log.i(TAG, "---身份证---" + "\n"
                        + "姓名：" + info.getName() + "\n"
                        + "性别：" + info.getSex() + "\n"
                        + "民族：" + info.getNation() + "\n"
                        + "出生日期：" + info.getBorn() + "\n"
                        + "地址：" + info.getAddress() + "\n"
                        + "签发机关：" + info.getApartment() + "\n"
                        + "有效期限：" + info.getPeriod() + "\n"
                        + "身份证号码：" + info.getNo() + "\n"
                        + "国籍或所在地区代码：" + info.getCountry() + "\n"
                        + "中文姓名：" + info.getCn_name() + "\n"
                        + "证件类型：" + info.getCard_type() + "\n"
                        + "保留信息：" + info.getReserve());
            }
        }
    }

    private boolean checkPackage(String packageName) {
        PackageManager manager = this.getPackageManager();
        Intent intent = new Intent().setPackage(packageName);
        @SuppressLint("WrongConstant") List<ResolveInfo> infos = manager.queryIntentActivities(intent,
                PackageManager.GET_INTENT_FILTERS);
        if (infos == null || infos.size() < 1) {
            return false;
        }
        return true;
    }
}
