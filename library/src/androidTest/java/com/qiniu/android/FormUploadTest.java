package com.qiniu.android;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import junit.framework.Assert;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FormUploadTest extends InstrumentationTestCase {
    private UploadManager uploadManager;
    private Map<String, String> bucketTokenMap;
    private Map<String, Zone> bucketZoneMap;
    private Map<String, Zone> mockBucketZoneMap;
    private volatile JSONObject responseBody;

    public void setUp() throws Exception {
        this.uploadManager = new UploadManager();
        this.bucketTokenMap = new HashMap<>();
//        this.bucketTokenMap.put(TestConfig.bucket_z0, TestConfig.token_z0);
//        this.bucketTokenMap.put(TestConfig.bucket_z1, TestConfig.token_z1);
//        this.bucketTokenMap.put(TestConfig.bucket_z2, TestConfig.token_z2);
        this.bucketTokenMap.put(TestConfig.bucket_na0, TestConfig.token_na0);

        this.bucketZoneMap = new HashMap<>();
//        this.bucketZoneMap.put(TestConfig.bucket_z0, FixedZone.zone0);
//        this.bucketZoneMap.put(TestConfig.bucket_z1, FixedZone.zone1);
//        this.bucketZoneMap.put(TestConfig.bucket_z2, FixedZone.zone2);
        this.bucketZoneMap.put(TestConfig.bucket_na0, FixedZone.zoneNa0);


        //mock
        this.mockBucketZoneMap = new HashMap<>();
//        this.mockBucketZoneMap.put(TestConfig.bucket_z0, TestConfig.mock_bucket_zone0);
//        this.mockBucketZoneMap.put(TestConfig.bucket_z1, TestConfig.mock_bucket_zone1);
//        this.mockBucketZoneMap.put(TestConfig.bucket_z2, TestConfig.mock_bucket_zone2);
        this.mockBucketZoneMap.put(TestConfig.bucket_na0, TestConfig.mock_bucket_zoneNa0);

    }

    /**
     * 1. 测试可选参数设置
     * 2. 测试mimeType设置
     * 3. 测试自动根据空间获取上传入口
     * 4. 测试不同机房的上传
     */
    @SmallTest
    public void testPutBytesWithAutoZone() throws Throwable {
        //mime type
        final String mimeType = "text/plain";
        final UploadOptions options = new UploadOptions(null, mimeType, true, null, null);
        byte[] putData = "hello qiniu cloud storage".getBytes();

        for (Map.Entry<String, String> bucketToken : this.bucketTokenMap.entrySet()) {
            final CountDownLatch signal = new CountDownLatch(1);
            final String bucket = bucketToken.getKey();
            final String upToken = bucketToken.getValue();
            final String expectKey = String.format("androidsdk/%s/qiniu_put_bytes_测试.txt", bucket);

            uploadManager.put(putData, expectKey, upToken, new UpCompletionHandler() {
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    Log.i("Qiniu.TestPutBytes", "upload result of bucket " + bucket);
                    Log.d("Qiniu.TestPutBytes", info.toString());

                    signal.countDown();
                }
            }, options);

            signal.await(120, TimeUnit.SECONDS);
        }
    }


    ///////////////固定Zone测试/////////////////
    @SmallTest
    public void testPutBytesWithFixedZone() {
        //mime type
        final String mimeType = "text/plain";
        final UploadOptions options = new UploadOptions(null, mimeType, true, null, null);
        byte[] putData = "hello qiniu cloud storage".getBytes();

        for (Map.Entry<String, Zone> bucketZone : this.bucketZoneMap.entrySet()) {
            final CountDownLatch signal = new CountDownLatch(1);
            final String bucket = bucketZone.getKey();
            final Zone zone = bucketZone.getValue();
            final String upToken = this.bucketTokenMap.get(bucket);

            final String expectKey = String.format("androidsdk/%s/qiniu_put_bytes_test.txt", bucket);

            Configuration cfg = new Configuration.Builder()
                    .zone(zone)
                    .useHttps(false)
                    .build();
            UploadManager uploadManagerWithCfg = new UploadManager(cfg);
            uploadManagerWithCfg.put(putData, expectKey, upToken, new UpCompletionHandler() {
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    Log.i("Qiniu.TestPutBytes", "upload result of bucket " + bucket);
                    Log.d("Qiniu.TestPutBytes", info.toString());

                    responseBody = response;
                    signal.countDown();

                }
            }, options);

            try {
                signal.await(120, TimeUnit.SECONDS);
            } catch (Exception ex) {
                Assert.fail("Qiniu.TestPutBytes timeout");
            }

            try {
                Assert.assertEquals("Qiniu.TestPutBytes upload failed", expectKey,
                        responseBody.getString("key"));
            } catch (Exception ex) {

            }
        }
    }


    /////模拟域名失败后，再重试的场景，需要手动修改zone域名无效来模拟/////
    @SmallTest
    public void testPutBytesWithFixedZoneUseBackupDomains() {
        //have changed old code , because token policy been changed
        //mime type
        final String mimeType = "text/plain";
        final UploadOptions options = new UploadOptions(null, mimeType, true, null, null);
        byte[] putData = "hello qiniu cloud storage".getBytes();

        for (Map.Entry<String, Zone> bucketZone : this.mockBucketZoneMap.entrySet()) {
            final CountDownLatch signal = new CountDownLatch(1);
            final String bucket = bucketZone.getKey();
            final Zone zone = bucketZone.getValue();
            final String upToken = this.bucketTokenMap.get(bucket);
            Log.e("qiniutest",upToken);

            final String expectKey = String.format("androidsdk/%s/qiniu_put_bytes_test.txt", bucket);

            Configuration cfg = new Configuration.Builder()
                    .zone(zone)
                    .useHttps(false)
                    .build();
            UploadManager uploadManagerWithCfg = new UploadManager(cfg);
            uploadManagerWithCfg.put(putData, expectKey, upToken, new UpCompletionHandler() {
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    Log.i("Qiniu.TestPutBytes", "upload result of bucket " + bucket);
                    Log.d("Qiniu.TestPutBytes", info.toString());

                    responseBody = response;
                    signal.countDown();

                }
            }, options);

            try {
                signal.await(120, TimeUnit.SECONDS);
            } catch (Exception ex) {
                Assert.fail("Qiniu.TestPutBytes timeout");
            }
        }

        //retry will success
        for (Map.Entry<String, Zone> bucketZone : this.mockBucketZoneMap.entrySet()) {
            final CountDownLatch signal = new CountDownLatch(1);
            final String bucket = bucketZone.getKey();
            final String upToken = this.bucketTokenMap.get(bucket);
            Log.e("qiniutest","retry:"+upToken);
            final String expectKey = String.format("androidsdk/%s/qiniu_put_bytes_test.txt", bucket);

            Configuration cfg = new Configuration.Builder()
                    .useHttps(false)
                    .build();
            UploadManager uploadManagerWithCfg = new UploadManager(cfg);
            uploadManagerWithCfg.put(putData, expectKey, upToken, new UpCompletionHandler() {
                public void complete(String key, ResponseInfo info, JSONObject response) {
                    Log.i("Qiniu.TestPutBytes", "upload result of bucket " + bucket);
                    Log.d("Qiniu.TestPutBytes", info.toString());

                    responseBody = response;
                    Log.e("qiniutest","responseBody:"+responseBody.toString());
                    signal.countDown();

                }
            }, options);

            try {
                signal.await(120, TimeUnit.SECONDS);
            } catch (Exception ex) {
                Assert.fail("Qiniu.TestPutBytes timeout");
            }

            try {
                Log.e("qiniutest",responseBody.toString());
                Assert.assertEquals("Qiniu.TestPutBytes upload failed", expectKey,
                        responseBody.getString("key"));
            } catch (Exception ex) {
                Assert.fail("Qiniu.TestPutBytes " + ex.getMessage());
            }
        }


    }

}
