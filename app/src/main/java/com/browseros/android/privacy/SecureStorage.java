package com.browseros.android.privacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * 安全存储类
 * 使用 Android Keystore 加密存储敏感信息（如 API 密钥）
 * 
 * @author BrowserOS Team
 */
public class SecureStorage {
    private static final String TAG = "SecureStorage";
    private static final String KEYSTORE_ALIAS = "BrowserOS_Key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private Context context;
    private KeyStore keyStore;
    
    /**
     * 构造函数
     * @param context Android 上下文
     */
    public SecureStorage(Context context) {
        this.context = context;
        initializeKeyStore();
    }
    
    /**
     * 初始化 KeyStore
     */
    private void initializeKeyStore() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            // 如果密钥不存在，创建新密钥
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                createKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化 KeyStore 失败", e);
        }
    }
    
    /**
     * 创建加密密钥
     */
    private void createKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build();
            
            keyGenerator.init(spec);
            keyGenerator.generateKey();
            Log.d(TAG, "创建加密密钥成功");
        } catch (NoSuchAlgorithmException | NoSuchProviderException | 
                 InvalidAlgorithmParameterException e) {
            Log.e(TAG, "创建加密密钥失败", e);
        }
    }
    
    /**
     * 获取密钥
     * @return SecretKey 实例
     */
    private SecretKey getSecretKey() {
        try {
            return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            Log.e(TAG, "获取密钥失败", e);
            return null;
        }
    }
    
    /**
     * 加密数据
     * @param plaintext 明文数据
     * @return Base64 编码的加密数据（包含 IV）
     */
    private String encrypt(String plaintext) {
        try {
            SecretKey secretKey = getSecretKey();
            if (secretKey == null) {
                return null;
            }
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            // 将 IV 和加密数据组合
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "加密失败", e);
            return null;
        }
    }
    
    /**
     * 解密数据
     * @param encryptedData Base64 编码的加密数据（包含 IV）
     * @return 解密后的明文数据
     */
    private String decrypt(String encryptedData) {
        try {
            SecretKey secretKey = getSecretKey();
            if (secretKey == null) {
                return null;
            }
            
            byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);
            
            // 分离 IV 和加密数据
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "解密失败", e);
            return null;
        }
    }
    
    /**
     * 保存 API 密钥（加密存储）
     * @param key 密钥名称（如 "openai_api_key"）
     * @param value 密钥值
     */
    public void saveApiKey(String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        String encrypted = encrypt(value);
        if (encrypted != null) {
            SharedPreferences prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE);
            prefs.edit().putString(key, encrypted).apply();
            Log.d(TAG, "保存 API 密钥: " + key);
        }
    }
    
    /**
     * 获取 API 密钥（解密）
     * @param key 密钥名称
     * @return 密钥值，如果不存在或解密失败返回 null
     */
    public String getApiKey(String key) {
        SharedPreferences prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE);
        String encrypted = prefs.getString(key, null);
        
        if (encrypted == null) {
            return null;
        }
        
        return decrypt(encrypted);
    }
    
    /**
     * 删除 API 密钥
     * @param key 密钥名称
     */
    public void deleteApiKey(String key) {
        SharedPreferences prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
        Log.d(TAG, "删除 API 密钥: " + key);
    }
    
    /**
     * 检查密钥是否存在
     * @param key 密钥名称
     * @return 是否存在
     */
    public boolean hasApiKey(String key) {
        SharedPreferences prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE);
        return prefs.contains(key);
    }
}

