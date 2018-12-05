package com.konka.ocrtest;

import android.Manifest;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ocr";
    private int mImgIndicator;
    private Button mSendToTencentBtn;
    private EditText mTencentResultEdt;
    private Button mSendToAliBtn;
    private EditText mAliResultEdt;
    private Button mCompareBtn;
    private EditText mCompareResultEdt;

    private MyHandler mHandler;
    private TencentOcr mTencentOcr;
    private File mImgDir;
    private File[] mImageFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);
        init();
    }

    void init() {
        mHandler = new MyHandler(this);
        mTencentOcr = new TencentOcr();

        mSendToTencentBtn = findViewById(R.id.btn_send_tencent);
        mSendToTencentBtn.setOnClickListener(this);
        mTencentResultEdt = findViewById(R.id.edt_result_tencent);
        mSendToAliBtn = findViewById(R.id.btn_send_ali);
        mSendToAliBtn.setOnClickListener(this);
        mAliResultEdt = findViewById(R.id.edt_result_ali);
        mCompareBtn = findViewById(R.id.btn_send_compare);
        mCompareBtn.setOnClickListener(this);
        mCompareResultEdt = findViewById(R.id.edt_result_compare);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String imgPath = getImgPath();
        if (!TextUtils.isEmpty(imgPath)) {
            Log.d(TAG, "image path = " + imgPath);
            mImgDir = new File(imgPath);
            if (mImgDir.exists()) {
                mImageFiles = mImgDir.listFiles();
                if (mImageFiles != null) {
                    Log.d(TAG, "has " + mImageFiles.length + " ocr test files");
                }
            } else {
                Log.e(TAG, "file not exists");
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (mImgDir != null && mImgDir.exists() && mImageFiles != null) {
            final File imgFile = mImageFiles[mImgIndicator % mImageFiles.length];
            if (imgFile != null) {
                switch (v.getId()) {
                    case R.id.btn_send_tencent:
                        mImgIndicator++;
                        mTencentResultEdt.setText("文件名：" + imgFile.getName() + " 大小：" + imgFile.length() / 1024 + "KB\n");
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                            @Override
                            public void run() {
                                mTencentOcr.start(BitmapFactory.decodeFile(imgFile.getAbsolutePath()), mHandler);
                            }
                        });
                        break;
                    case R.id.btn_send_ali:
                        break;
                    case R.id.btn_send_compare:
                        break;
                    default:
                        break;
                }
            }
        } else {
            Toast.makeText(this, "图片文件夹不存在", Toast.LENGTH_LONG).show();
        }
    }

    private static class MyHandler extends Handler {
        private MainActivity mMainActivity;

        MyHandler(MainActivity mainActivity) {
            mMainActivity = mainActivity;
        }

        @Override
        public void handleMessage(Message msg) {
            String result;
            switch (msg.what) {
                case TencentOcr.MSG_OCR_RESULT:
                    result = (String) msg.obj;
                    mMainActivity.mTencentResultEdt.setText(mMainActivity.mTencentResultEdt.getText().toString() + result);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    private String getImgPath() {
        String sdDir = "";
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory().toString() + File.separator + "ocr_img" + File.separator;
        } else {
            Log.e(TAG, "sdCard is not exit");
        }
        return sdDir;
    }
}
