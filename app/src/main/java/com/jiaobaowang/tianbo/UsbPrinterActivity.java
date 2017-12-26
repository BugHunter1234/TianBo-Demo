package com.jiaobaowang.tianbo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.printer.UsbThermalPrinter;
import com.telpo.tps550.api.util.StringUtil;
import com.telpo.tps550.api.util.SystemUtil;

import java.util.Hashtable;

public class UsbPrinterActivity extends AppCompatActivity {
    private static final String TAG = "UsbPrinterActivity";
    private final int NOPAPER = 3;//打印机缺纸
    private final int LOWBATTERY = 4;
    private final int PRINTVERSION = 5;
    private final int PRINTBARCODE = 6;
    private final int PRINTQRCODE = 7;
    private final int PRINTPAPERWALK = 8;
    private final int PRINTCONTENT = 9;
    private final int CANCELPROMPT = 10;
    private final int PRINTERR = 11;
    private final int OVERHEAT = 12;//打印机过热
    private final int MAKER = 13;
    private final int PRINTPICTURE = 14;
    private final int NOBLACKBLOCK = 15;
    private final int PRINTSHORTCONTENT = 16;
    private final int PRINTLONGPICTURE = 17;
    private final int PRINTLONGTEXT = 18;
    private final int PRINTBLACK = 19;

    private String Result;
    private String printVersion;//打印机版本
    private ProgressDialog dialog;
    private ProgressDialog progressDialog;
    private MyHandler handler;

    //打印机
    private Boolean LowBattery = false;//低电量
    private Boolean nopaper = false;//缺纸
    private UsbThermalPrinter mUsbThermalPrinter;
    private TextView printerVersionTv;//打印机版本
    //走纸测试
    private int paperWalk;
    private EditText paperWalkEt;
    //打印
    private int leftDistance, lineDistance, wordFont, printGray;
    private EditText leftDistanceEt;//左边距
    private EditText lineDistanceEt;//行距
    private EditText wordFontEt;//文字大小
    private EditText printGrayEt;//灰度
    // 文字打印
    private String printText;//打印的内容
    private EditText printTextEt;//打印的内容
    //二维码打印
    private String printQRCode;//二维码的内容
    private EditText printQRCodeEt;
    //条码打印
    private String printBarCode;//条码的内容
    private EditText printBarCodeEt;

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOPAPER://缺纸
                    noPaperDlg();
                    break;
                case LOWBATTERY://低电量
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(UsbPrinterActivity.this);
                    alertDialog.setTitle(R.string.operation_result);
                    alertDialog.setMessage(getString(R.string.LowBattery));
                    alertDialog.setPositiveButton(getString(R.string.dialog_comfirm),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                    alertDialog.show();
                    break;
//                case NOBLACKBLOCK:
//                    Toast.makeText(UsbPrinterActivity.this, R.string.maker_not_find, Toast.LENGTH_SHORT).show();
//                    break;
                case PRINTVERSION://打印机版本
                    dialog.dismiss();
                    if (msg.obj.equals("1")) {
                        String text = "打印机版本：" + printVersion;
                        printerVersionTv.setText(text);
                    } else {
                        Toast.makeText(UsbPrinterActivity.this, R.string.operation_fail, Toast.LENGTH_LONG).show();
                    }
                    break;
                case PRINTBARCODE:
                    new barcodePrintThread().start();
                    break;
                case PRINTQRCODE:
                    new qrcodePrintThread().start();
                    break;
                case PRINTPAPERWALK://走纸测试
                    new paperWalkPrintThread().start();
                    break;
                case PRINTCONTENT:
                    new contentPrintThread().start();
                    break;
//                case MAKER:
//                    new MakerThread().start();
//                    break;
//                case PRINTPICTURE:
//                    new printPicture().start();
//                    break;
                case CANCELPROMPT://取消提示
                    if (progressDialog != null && !UsbPrinterActivity.this.isFinishing()) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    break;
                case OVERHEAT://过热
                    AlertDialog.Builder overHeatDialog = new AlertDialog.Builder(UsbPrinterActivity.this);
                    overHeatDialog.setTitle(R.string.operation_result);
                    overHeatDialog.setMessage(getString(R.string.overTemp));
                    overHeatDialog.setPositiveButton(getString(R.string.dialog_comfirm),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                    overHeatDialog.show();
                    break;
//                case PRINTSHORTCONTENT:
//                    new ShortTextPrintThread().start();
//                    break;
//                case PRINTLONGPICTURE:
//                    new printLongPicture().start();
//                    break;
//                case PRINTLONGTEXT:
//                    new printLongText().start();
//                    break;
//                case PRINTBLACK:
//                    new printBlackPicture().start();
//                    break;
                default:
                    Toast.makeText(UsbPrinterActivity.this, "Print Error!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_printer);
        initPrinter();
        initPrinterVersion();
        initBattery();
        initPaperWalk();
        initPrintText();
        initPrintQRCode();
        initPrintBarCode();
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && !UsbPrinterActivity.this.isFinishing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        unregisterReceiver(printReceive);
        mUsbThermalPrinter.stop();
        super.onDestroy();
    }

    /**
     * 初始化打印机
     */
    private void initPrinter() {
        handler = new MyHandler();
        mUsbThermalPrinter = new UsbThermalPrinter(UsbPrinterActivity.this);
    }

    /**
     * 初始化电量广播
     */
    private void initBattery() {
        IntentFilter pIntentFilter = new IntentFilter();
        pIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        pIntentFilter.addAction("android.intent.action.BATTERY_CAPACITY_EVENT");
        registerReceiver(printReceive, pIntentFilter);
    }

    /**
     * 获取打印机版本
     */
    private void initPrinterVersion() {
        printerVersionTv = findViewById(R.id.printer_version_tv);
        dialog = new ProgressDialog(UsbPrinterActivity.this);
        dialog.setTitle(R.string.idcard_czz);
        dialog.setMessage(getText(R.string.watting));
        dialog.setCancelable(false);
        dialog.show();
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    mUsbThermalPrinter.start(0);
                    mUsbThermalPrinter.reset();
                    printVersion = mUsbThermalPrinter.getVersion();
                } catch (TelpoException e) {
                    e.printStackTrace();
                } finally {
                    Message message = new Message();
                    message.what = PRINTVERSION;
                    if (printVersion != null) {
                        message.obj = "1";
                    } else {
                        message.obj = "0";
                    }
                    handler.sendMessage(message);
                }
            }
        }).start();
    }

    /**
     * 电量的广播
     */
    private final BroadcastReceiver printReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                // TPS390 can not print,while in low battery,whether is charging or not charging
                if (SystemUtil.getDeviceType() == StringUtil.DeviceModelEnum.TPS390.ordinal()) {
                    if (level * 5 <= scale) {
                        LowBattery = true;
                    } else {
                        LowBattery = false;
                    }
                } else {
                    if (status != BatteryManager.BATTERY_STATUS_CHARGING) {
                        if (level * 5 <= scale) {
                            LowBattery = true;
                        } else {
                            LowBattery = false;
                        }
                    } else {
                        LowBattery = false;
                    }
                }
            }
            // Only use for TPS550MTK devices
            else if (action.equals("android.intent.action.BATTERY_CAPACITY_EVENT")) {
                int status = intent.getIntExtra("action", 0);
                int level = intent.getIntExtra("level", 0);
                if (status == 0) {
                    if (level < 1) {
                        LowBattery = true;
                    } else {
                        LowBattery = false;
                    }
                } else {
                    LowBattery = false;
                }
            }
        }
    };

    /**
     * 初始化走纸测试
     */
    private void initPaperWalk() {
        paperWalkEt = findViewById(R.id.paper_walk_et);
        Button paperWalkBtn = findViewById(R.id.paper_walk_btn);
        paperWalkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String walkLine;
                walkLine = paperWalkEt.getText().toString();
                if (walkLine.length() == 0) {
                    Toast.makeText(UsbPrinterActivity.this, getString(R.string.empty), Toast.LENGTH_LONG).show();
                    return;
                }
                if (Integer.parseInt(walkLine) < 1 || Integer.parseInt(walkLine) > 255) {
                    Toast.makeText(UsbPrinterActivity.this, getString(R.string.walk_paper_intput_value),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                paperWalk = Integer.parseInt(walkLine);
                initStartPrinting(PRINTPAPERWALK);
            }
        });
    }

    /**
     * 初始化打印文字
     */
    private void initPrintText() {
        leftDistanceEt = findViewById(R.id.left_distance_et);
        lineDistanceEt = findViewById(R.id.line_distance_et);
        wordFontEt = findViewById(R.id.word_font_et);
        printGrayEt = findViewById(R.id.print_gray_et);
        printTextEt = findViewById(R.id.print_text_et);
        Button printTextBtn = findViewById(R.id.print_text_btn);
        Button printTextClearBtn = findViewById(R.id.print_text_clear_btn);
        Button printTextSampleBtn = findViewById(R.id.print_text_sample_btn);
        printTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打印内容
                printText = printTextEt.getText().toString();
                if (!printCheck(printText)) {
                    return;
                }
                initStartPrinting(PRINTCONTENT);
            }
        });
        //清空
        printTextClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printTextEt.setText("");
            }
        });
        //样例
        printTextSampleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = "烧烤"
                        + "\n---------------------------"
                        + "\n日期：2015-01-01 16:18:20"
                        + "\n卡号：12378945664"
                        + "\n单号：1001000000000529142"
                        + "\n---------------------------"
                        + "\n    项目     数量   单价  小计"
                        + "\n秘制烤羊腿    1     56    56"
                        + "\n黯然牛排        2     24    48"
                        + "\n烤火鸡           2     50    100"
                        + "\n炭烧鳗鱼        1     40    40"
                        + "\n 合计：1000：00元"
                        + "\n---------------------------"
                        + "\n本卡金额：10000.00"
                        + "\n累计消费：1000.00"
                        + "\n本卡结余：9000.00"
                        + "\n---------------------------"
                        + "\n 地址：广东省佛山市南海区桂城街道桂澜南路45号鹏瑞利广场A317.B-18号铺"
                        + "\n欢迎您的再次光临";
                printTextEt.setText(text);
            }
        });
    }

    /**
     * 初始化打印二维码
     */
    private void initPrintQRCode() {
        printQRCodeEt = findViewById(R.id.print_qrcode_et);
        Button printQRCodeBtn = findViewById(R.id.print_qrcode_btn);
        Button printQRCodeClearBtn = findViewById(R.id.print_qrcode_clear_btn);
        Button printQRCodeSampleBtn = findViewById(R.id.print_qrcode_sample_btn);
        printQRCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printQRCode = printQRCodeEt.getText().toString();
                if (!printCheck(printQRCode)) {
                    return;
                }
                initStartPrinting(PRINTQRCODE);
            }
        });
        printQRCodeClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printQRCodeEt.setText("");
            }
        });
        printQRCodeSampleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = "http://www.jiaobaowang.com/SSO2/Account/login?returl=OTQ2Rjg2Qjk4NkIwNzc3QzE3REI0MjczNzVFNzk5MUVCN0MzNkI0QUJCMTY2RTY3Q0JGNTRCOEM3NTdDNTVDMkIzQTk5NUMzMDQ3OUFDQUNGQUM0NjMyMDI1RkQxOTQzMTIyM0I3RjVCMDgyOTQ1MQ";
                printQRCodeEt.setText(text);
            }
        });
    }

    /**
     * 初始化打印条码
     */
    private void initPrintBarCode(){
        printBarCodeEt = findViewById(R.id.print_barcode_et);
        Button printBarCodeBtn = findViewById(R.id.print_barcode_btn);
        Button printBarCodeClearBtn = findViewById(R.id.print_barcode_clear_btn);
        Button printBarCodeSampleBtn = findViewById(R.id.print_barcode_sample_btn);
        printBarCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printBarCode = printBarCodeEt.getText().toString();
                if (!printCheck(printBarCode)) {
                    return;
                }
                initStartPrinting(PRINTBARCODE);
            }
        });
        printBarCodeClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printBarCodeEt.setText("");
            }
        });
        printBarCodeSampleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = "6936983800013";
                printBarCodeEt.setText(text);
            }
        });

    }

    /**
     * 打印设置检查
     */
    private boolean printCheck(String text) {
        //打印的内容
        if (text == null || text.length() == 0) {
            Toast.makeText(UsbPrinterActivity.this, getString(R.string.empty), Toast.LENGTH_LONG).show();
            return false;
        }
        String et;
        //左边距
        et = leftDistanceEt.getText().toString();
        if (et.equals("")) {
            Toast.makeText(UsbPrinterActivity.this,
                    getString(R.string.left_margin) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        leftDistance = Integer.parseInt(et);
        //行距
        et = lineDistanceEt.getText().toString();
        if (et.length() < 1) {
            Toast.makeText(UsbPrinterActivity.this,
                    getString(R.string.row_space) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        lineDistance = Integer.parseInt(et);
        //字体大小
        et = wordFontEt.getText().toString();
        if (et.length() < 1) {
            Toast.makeText(UsbPrinterActivity.this,
                    getString(R.string.font_size) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        wordFont = Integer.parseInt(et);
        //灰度
        et = printGrayEt.getText().toString();
        if (et.length() < 1) {
            Toast.makeText(UsbPrinterActivity.this,
                    getString(R.string.gray_level) + getString(R.string.lengthNotEnougth), Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        printGray = Integer.parseInt(et);
        if (leftDistance > 255) {
            Toast.makeText(UsbPrinterActivity.this, getString(R.string.outOfLeft), Toast.LENGTH_LONG).show();
            return false;
        }
        if (lineDistance > 255) {
            Toast.makeText(UsbPrinterActivity.this, getString(R.string.outOfLine), Toast.LENGTH_LONG).show();
            return false;
        }
        if (wordFont > 4 || wordFont < 1) {
            Toast.makeText(UsbPrinterActivity.this, getString(R.string.outOfFont), Toast.LENGTH_LONG).show();
            return false;
        }
        if (printGray < 0 || printGray > 7) {
            Toast.makeText(UsbPrinterActivity.this, getString(R.string.outOfGray), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * 开始打印
     */
    private void initStartPrinting(int type) {
        if (LowBattery) {
            handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
        } else {
            if (!nopaper) {
                progressDialog = ProgressDialog.show(UsbPrinterActivity.this, getString(R.string.bl_dy),
                        getString(R.string.printing_wait));
                handler.sendMessage(handler.obtainMessage(type, 1, 0, null));
            } else {
                Toast.makeText(UsbPrinterActivity.this, getString(R.string.ptintInit), Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    /**
     * 打印机缺纸，的弹出框提示
     */
    private void noPaperDlg() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(UsbPrinterActivity.this);
        dlg.setTitle(getString(R.string.noPaper));
        dlg.setMessage(getString(R.string.noPaperNotice));
        dlg.setCancelable(false);
        dlg.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        dlg.show();
    }

    /**
     * 走纸测试
     */
    private class paperWalkPrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.walkPaper(paperWalk);
            } catch (Exception e) {
                e.printStackTrace();
                printCatch(e.toString());
            } finally {
                printFinally();
            }
        }
    }

    /**
     * 文字打印
     */
    private class contentPrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
                mUsbThermalPrinter.setLeftIndent(leftDistance);
                mUsbThermalPrinter.setLineSpace(lineDistance);
                if (wordFont == 4) {
                    mUsbThermalPrinter.setFontSize(2);
                    mUsbThermalPrinter.enlargeFontSize(2, 2);
                } else if (wordFont == 3) {
                    mUsbThermalPrinter.setFontSize(1);
                    mUsbThermalPrinter.enlargeFontSize(2, 2);
                } else if (wordFont == 2) {
                    mUsbThermalPrinter.setFontSize(2);
                } else if (wordFont == 1) {
                    mUsbThermalPrinter.setFontSize(1);
                }
                mUsbThermalPrinter.setGray(printGray);
                mUsbThermalPrinter.addString(printText);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(20);
            } catch (Exception e) {
                e.printStackTrace();
                printCatch(e.toString());
            } finally {
                printFinally();
            }
        }
    }

    /**
     * 二维码打印
     */
    private class qrcodePrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setGray(printGray);
                Bitmap bitmap = CreateCode(printQRCode, BarcodeFormat.QR_CODE, 256, 256);
                if (bitmap != null) {
                    //打印二维码
                    mUsbThermalPrinter.printLogo(bitmap, true);
                }
                //打印二维码的内容
                //mUsbThermalPrinter.addString(printQRCode);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(20);
            } catch (Exception e) {
                e.printStackTrace();
                printCatch(e.toString());
            } finally {
                printFinally();
            }
        }
    }

    /**
     * 条码打印
     */
    private class barcodePrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setGray(printGray);
                Bitmap bitmap = CreateCode(printBarCode, BarcodeFormat.CODE_128, 320, 176);
                if (bitmap != null) {
                    mUsbThermalPrinter.printLogo(bitmap, true);
                }
                mUsbThermalPrinter.addString(printBarCode);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(20);
            } catch (Exception e) {
                e.printStackTrace();
                printCatch(e.toString());
            } finally {
                printFinally();
            }
        }
    }

    /**
     * 打印异常
     */
    private void printCatch(String error) {
        switch (error) {
            case "com.telpo.tps550.api.printer.NoPaperException":
                //打印机缺纸
                nopaper = true;
                break;
            case "com.telpo.tps550.api.printer.OverHeatException":
                //打印机过热
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                break;
            default:
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                break;
        }

    }

    /**
     * 打印异常
     */
    private boolean printFinally() {
        handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
        if (nopaper) {
            handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
            nopaper = false;
            return false;
        }
        return true;
    }

    /**
     * 生成条码
     *
     * @param str       条码内容
     * @param type      条码类型： AZTEC, CODABAR, CODE_39, CODE_93, CODE_128, DATA_MATRIX,
     *                  EAN_8, EAN_13, ITF, MAXICODE, PDF_417, QR_CODE, RSS_14,
     *                  RSS_EXPANDED, UPC_A, UPC_E, UPC_EAN_EXTENSION;
     * @param bmpWidth  生成位图宽,宽不能大于384，不然大于打印纸宽度
     * @param bmpHeight 生成位图高，8的倍数
     */

    public Bitmap CreateCode(String str, com.google.zxing.BarcodeFormat type, int bmpWidth, int bmpHeight)
            throws WriterException {
        Hashtable<EncodeHintType, String> mHashtable = new Hashtable<EncodeHintType, String>();
        mHashtable.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        // 生成二维矩阵,编码时要指定大小,不要生成了图片以后再进行缩放,以防模糊导致识别失败
        BitMatrix matrix = new MultiFormatWriter().encode(str, type, bmpWidth, bmpHeight, mHashtable);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        // 二维矩阵转为一维像素数组（一直横着排）
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                } else {
                    pixels[y * width + x] = 0xffffffff;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 通过像素数组生成bitmap,具体参考api
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
