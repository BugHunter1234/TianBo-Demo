package com.jiaobaowang.tianbo;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.other.BeepManager;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.CountryMap;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.idcard.IdentityInfo;

/**
 * 读取二代身份证
 */
public class IdCardReadActivity extends AppCompatActivity {
    private static final String TAG = "IdCardReadActivity";
    private IdentityInfo info;//二代身份证信息
    private Bitmap bitmap;
    private String fingerPrintData;
    private BeepManager beepManager;//bee声音
    private CountryMap countryMap = CountryMap.getInstance();//国家代码
    private Context mContent;
    private TextView idCardInfoTv;//二代身份证信息
    private Button idCardReadBtn;//读取二代身份证
    private ImageView idCardImage;//二代身份证图片

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_card_read);
        //页面初始化
        mContent = this;
        idCardReadBtn = findViewById(R.id.id_card_read_btn);
        idCardInfoTv = findViewById(R.id.id_card_info_tv);
        idCardImage = findViewById(R.id.id_card_iv);
        idCardReadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GetIDInfoTask().execute();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        beepManager = new BeepManager(this, R.raw.beep);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    IdCard.open(IdCardReadActivity.this);
                } catch (TelpoException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(IdCardReadActivity.this, R.string.idcard_wfljdkq, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        beepManager.close();
        beepManager = null;
        IdCard.close();
    }

    private class GetIDInfoTask extends AsyncTask<Void, Integer, TelpoException> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            //在execute被调用后立即执行
            super.onPreExecute();
            idCardReadBtn.setEnabled(false);
            dialog = new ProgressDialog(mContent);
            dialog.setTitle(getString(R.string.idcard_czz));
            dialog.setMessage(getString(R.string.idcard_ljdkq));
            dialog.setCancelable(false);
            dialog.show();
            info = null;
            bitmap = null;
        }

        @Override
        protected TelpoException doInBackground(Void... voids) {
            //在onPreExecute()完成后立即执行
            TelpoException result = null;
            try {
                publishProgress(1);
                // info = IdCard.checkIdCard(4000);
                info = IdCard.checkIdCard(1600);// luyq modify
                if (info != null) {
                    byte[] image = IdCard.getIdCardImage();
                    bitmap = IdCard.decodeIdCardImage(image);
                    // luyq add 增加指纹信息
                    if (!"I".equals(info.getCard_type())) {
                        // luyq add 增加指纹信息 中国籍才有指纹
                        byte[] fingerPrint = IdCard.getFringerPrint();
                        fingerPrintData = getFingerInfo(fingerPrint);
                    }
                }
            } catch (TelpoException e) {
                e.printStackTrace();
                result = e;
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //在调用publishProgress时此方法被执行
            super.onProgressUpdate(values);
            if (values[0] == 1) {
                dialog.setMessage(getString(R.string.idcard_hqsfzxx));
            }
        }

        @Override
        protected void onPostExecute(TelpoException result) {
            //当后台操作结束时，此方法将会被调用
            super.onPostExecute(result);
            dialog.dismiss();
            idCardReadBtn.setEnabled(true);
            if (result == null) {
                beepManager.playBeepSoundAndVibrate();
                if ("I".equals(info.getCard_type())) {
                    // luyq add 20170823 增加外籍身份证信息显示
                    idCardInfoTv.setText(getString(R.string.idcard_xm) + info.getName() + "\n\n"
                            + getString(R.string.idcard_cn_name) + info.getCn_name() + "\n\n" // Chinese name
                            + getString(R.string.idcard_xb) + info.getSex() + "\n\n"
                            + getString(R.string.idcard_csrq) + info.getBorn() + "\n\n"
                            + getString(R.string.idcard_country) + countryMap.getCountry(info.getCountry()) + " / " + info.getCountry() + "\n\n"
                            + getString(R.string.idcard_yxqx) + info.getPeriod() + "\n\n"
                            + getString(R.string.idcard_qzjg) + info.getApartment() + "\n\n"
                            + getString(R.string.idcard_sfhm) + info.getNo() + "\n\n"
                            + getString(R.string.idcard_version) + info.getIdcard_version() + "\n\n");
                } else {
                    idCardInfoTv.setText(getString(R.string.idcard_xm) + info.getName() + "\n\n"
                            + getString(R.string.idcard_xb) + info.getSex() + "\n\n"
                            + getString(R.string.idcard_mz) + info.getNation() + "\n\n"
                            + getString(R.string.idcard_csrq) + info.getBorn() + "\n\n"
                            + getString(R.string.idcard_dz) + info.getAddress() + "\n\n"
                            + getString(R.string.idcard_sfhm) + info.getNo() + "\n\n"
                            + getString(R.string.idcard_qzjg) + info.getApartment() + "\n\n"
                            + getString(R.string.idcard_yxqx) + info.getPeriod() + "\n\n"
                            + getString(R.string.idcard_zwxx) + fingerPrintData);
                }
                idCardImage.setImageBitmap(bitmap);
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
            } else {
                idCardInfoTv.setText(getString(R.string.idcard_dqsbhcs));
                idCardImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            }
        }
    }

    private String GetFingerName(int fingerPos) {
        String fingerName = "";
        switch (fingerPos) {
            case 11:
                // 右手拇指
                fingerName = getString(R.string.idcard_ysmz);
                break;
            case 12:
                // 右手食指
                fingerName = getString(R.string.idcard_yssz);
                break;
            case 13:
                // 右手中指
                fingerName = getString(R.string.idcard_yszz);
                break;
            case 14:
                // 右手环指
                fingerName = getString(R.string.idcard_yshz);
                break;
            case 15:
                // 右手小指
                fingerName = getString(R.string.idcard_ysxz);
                break;
            case 16:
                // 左手拇指
                fingerName = getString(R.string.idcard_zsmz);
                break;
            case 17:
                // 左手食指
                fingerName = getString(R.string.idcard_zssz);
                break;
            case 18:
                // 左手中指
                fingerName = getString(R.string.idcard_zszz);
                break;
            case 19:
                // 左手环指
                fingerName = getString(R.string.idcard_zshz);
                break;
            case 20:
                // 左手小指
                fingerName = getString(R.string.idcard_zsxz);
                break;
            case 97:
                // 右手不确定指位
                fingerName = getString(R.string.idcard_ysbqdzw);
                break;
            case 98:
                // 左手不确定指位
                fingerName = getString(R.string.idcard_zsbqdzw);
                break;
            case 99:
                // 其他不确定指位
                fingerName = getString(R.string.idcard_qtbqdzw);
                break;
            default:
                // 指位未知
                fingerName = getString(R.string.idcard_zwwz);
                break;
        }
        return fingerName;
    }

    // 第5字节为注册结果代码，0x01-注册成功，0x02--注册失败, 0x03--未注册, 0x09--未知
    private String GetFingerStatus(int fingerStatus) {
        String fingerStatusName = "";
        switch (fingerStatus) {
            case 0x01:
                // 注册成功
                fingerStatusName = getString(R.string.idcard_zccg);
                break;
            case 0x02:
                // 注册失败
                fingerStatusName = getString(R.string.idcard_zcsb);
                break;
            case 0x03:
                // 未注册
                fingerStatusName = getString(R.string.idcard_wzc);
                break;
            case 0x09:
                // 注册状态未知
                fingerStatusName = getString(R.string.idcard_zcztwz);
                break;
            default:
                // 注册状态未知
                fingerStatusName = getString(R.string.idcard_zcztwz);
                break;
        }
        return fingerStatusName;
    }

    private String getFingerInfo(byte[] fpData) {
        // 解释第1枚指纹，总长度512字节，部分数据格式：
        // 第1字节为特征标识'C'
        // 第5字节为注册结果代码，0x01-注册成功，0x02--注册失败, 0x03--未注册, 0x09--未知
        // 第6字节为指位代码
        // 第7字节为指纹质量值，0x00表示未知，1~100表示质量值
        // 第512字节 crc8值
        String fingerInfo = "";
        if (fpData != null && fpData.length == 1024 && fpData[0] == 'C') {
            fingerInfo = fingerInfo + GetFingerName(fpData[5]);

            if (fpData[4] == 0x01) {
                fingerInfo = fingerInfo + " " + getString(R.string.idcard_zwzl) + String.valueOf(fpData[6]);
            } else {
                fingerInfo = fingerInfo + GetFingerStatus(fpData[4]);
            }

            fingerInfo = fingerInfo + "  ";
            if (fpData[512] == 'C') {
                fingerInfo = fingerInfo + GetFingerName(fpData[512 + 5]);

                if (fpData[512 + 4] == 0x01) {
                    fingerInfo = fingerInfo + " " + getString(R.string.idcard_zwzl) + String.valueOf(fpData[512 + 6]);
                } else {
                    fingerInfo = fingerInfo + GetFingerStatus(fpData[512 + 4]);
                }
            }
        } else {
            fingerInfo = getString(R.string.idcard_wdqhbhzw);
        }

        return fingerInfo;
    }
}
