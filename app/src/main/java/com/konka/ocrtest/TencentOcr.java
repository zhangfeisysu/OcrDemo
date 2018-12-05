package com.konka.ocrtest;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 腾讯通用OCR方案，技术文档按照<a>https://ai.qq.com/doc/ocrgeneralocr.shtml</a>进行对接
 *
 * @author Affy
 */
public class TencentOcr {
    private long mStartOcrTime = 0;
    static final int MSG_OCR_RESULT = 0;
    private OkHttpClient mOkHttpClient;

    public TencentOcr() {
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(null, null, null);

        //默认支持所有https请求
        mOkHttpClient = new OkHttpClient.Builder().
                connectTimeout(10, TimeUnit.SECONDS)
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .addNetworkInterceptor(new LogInterceptor())
                .build();
    }

    /**
     * 发起识别
     *
     * @param bitmap        要进行识别的图片
     * @param resultHandler 识别结果回传到主线程进行展示。信息包括ocr本地计算时间，发起识别到获取结果时间，云端返回信息
     */
    public void start(final Bitmap bitmap, final Handler resultHandler) {
        try {
            mStartOcrTime = System.currentTimeMillis();
            final StringBuilder signStrBuilder = new StringBuilder();
            signStrBuilder.append("app_id=" + URLEncoder.encode("2110179801", "UTF-8") + "&");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();
            final String base64CodeImg = Base64.encodeToString(bytes, Base64.DEFAULT);
            signStrBuilder.append("image=" + URLEncoder.encode(base64CodeImg, "UTF-8") + "&");

            final String randomStr = UUID.randomUUID().toString().replace("-", "");
            signStrBuilder.append("nonce_str=" + URLEncoder.encode(randomStr, "UTF-8") + "&");
            final String timeStamp = "" + System.currentTimeMillis() / 1000;
            signStrBuilder.append("time_stamp=" + URLEncoder.encode(timeStamp, "UTF-8") + "&");
            signStrBuilder.append("app_key=" + URLEncoder.encode("CPt3DXV6GeRLFvqC", "UTF-8"));


            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        RequestBody formBody = new FormBody.Builder()
                                .add("app_id", "2110179801")
                                .add("time_stamp", timeStamp)
                                .add("nonce_str", randomStr)
                                .add("sign", md5(signStrBuilder.toString()))
                                .add("image", base64CodeImg)
                                .build();

                        Request request = new Request.Builder()
                                .url("https://api.ai.qq.com/fcgi-bin/ocr/ocr_generalocr")
                                .post(formBody)
                                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                                .build();

                        final long computeCostTime = System.currentTimeMillis() - mStartOcrTime;
                        Log.d("ocrTime", "compute ocr cost: " + computeCostTime + " ms");
                        Response response = mOkHttpClient.newCall(request).execute();
                        final String responseResult = response.body().string();
                        final long getResultCostTime = System.currentTimeMillis() - mStartOcrTime;
                        if (response.isSuccessful()) {
                            Log.d("ocrTime", "ocr success cost: " + getResultCostTime + " ms");
                            resultHandler.sendMessage(resultHandler.obtainMessage(MSG_OCR_RESULT, "计算耗时："
                                    + computeCostTime + " 获取结果耗时：" + getResultCostTime + "\n" + responseResult));
                            printResponse(responseResult);
                        } else {
                            resultHandler.sendMessage(resultHandler.obtainMessage(MSG_OCR_RESULT, "计算耗时："
                                    + computeCostTime + " 获取结果耗时：" + getResultCostTime + "\n" + responseResult));
                            Log.d("ocrTime", "ocr fail cost: " + getResultCostTime + " ms");
                        }
                    } catch (IOException e) {
                        resultHandler.sendMessage(resultHandler.obtainMessage(MSG_OCR_RESULT, e.getMessage()));
                        e.printStackTrace();
                        Log.d("ocr", "error:" + e.getMessage());
                    }
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    /**
     * MD5单向加密，32位，用于加密密码，因为明文密码在信道中传输不安全，明文保存在本地也不安全
     *
     * @param str
     * @return
     */
    String md5(String str) {
        Log.d("ocr", "before md5:" + str);
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        char[] charArray = str.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        byte[] md5Bytes = md5.digest(byteArray);

        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString().toUpperCase();
    }

    void printResponse(String response) {
        int maxLength = 4000;
        byte[] bytes = response.getBytes();
        int length = bytes.length;
        if (length <= maxLength) {
            Log.d("ocr", response);
        } else {
            for (int i = 0; i < length; i += maxLength) {
                int count = Math.min(length - i, maxLength);
                //create a new String with system's default charset (which is UTF-8 for Android)
                Log.d("ocr", new String(bytes, i, count));
            }
        }
    }
}
