package com.browseros.android.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.browseros.android.R;
import com.browseros.android.privacy.DataManager;
import com.browseros.android.privacy.SecureStorage;

/**
 * 设置活动
 * 管理应用设置，包括 API 密钥配置和隐私设置
 * 
 * @author BrowserOS Team
 */
public class SettingsActivity extends AppCompatActivity {
    private DataManager dataManager;
    private SecureStorage secureStorage;
    
    private EditText openaiKeyInput;
    private EditText anthropicKeyInput;
    private EditText ollamaUrlInput;
    private TextView dataSizeText;
    private Button clearHistoryButton;
    private Button clearAllDataButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 显示返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // 初始化组件
        dataManager = new DataManager(this);
        secureStorage = dataManager.getSecureStorage();
        
        initializeViews();
        loadSettings();
        setupListeners();
    }
    
    /**
     * 初始化视图组件
     */
    private void initializeViews() {
        openaiKeyInput = findViewById(R.id.openai_key_input);
        anthropicKeyInput = findViewById(R.id.anthropic_key_input);
        ollamaUrlInput = findViewById(R.id.ollama_url_input);
        dataSizeText = findViewById(R.id.data_size_text);
        clearHistoryButton = findViewById(R.id.clear_history_button);
        clearAllDataButton = findViewById(R.id.clear_all_data_button);
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        // 加载 API 密钥（显示为已配置，但不显示实际值）
        if (secureStorage.hasApiKey("openai_api_key")) {
            openaiKeyInput.setHint("已配置（点击修改）");
        }
        if (secureStorage.hasApiKey("anthropic_api_key")) {
            anthropicKeyInput.setHint("已配置（点击修改）");
        }
        
        // 加载 Ollama URL（从 SharedPreferences）
        String ollamaUrl = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("ollama_url", "http://localhost:11434");
        ollamaUrlInput.setText(ollamaUrl);
        
        // 更新数据大小
        updateDataSize();
    }
    
    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 保存按钮
        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveSettings());
        
        // 清除历史记录按钮
        clearHistoryButton.setOnClickListener(v -> {
            dataManager.clearHistory();
            Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show();
            updateDataSize();
        });
        
        // 清除所有数据按钮
        clearAllDataButton.setOnClickListener(v -> {
            dataManager.clearAllBrowsingData();
            Toast.makeText(this, "所有数据已清除", Toast.LENGTH_SHORT).show();
            updateDataSize();
        });
    }
    
    /**
     * 保存设置
     */
    private void saveSettings() {
        // 保存 OpenAI API 密钥
        String openaiKey = openaiKeyInput.getText().toString().trim();
        if (!openaiKey.isEmpty()) {
            secureStorage.saveApiKey("openai_api_key", openaiKey);
        }
        
        // 保存 Anthropic API 密钥
        String anthropicKey = anthropicKeyInput.getText().toString().trim();
        if (!anthropicKey.isEmpty()) {
            secureStorage.saveApiKey("anthropic_api_key", anthropicKey);
        }
        
        // 保存 Ollama URL
        String ollamaUrl = ollamaUrlInput.getText().toString().trim();
        if (!ollamaUrl.isEmpty()) {
            getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putString("ollama_url", ollamaUrl)
                    .apply();
        }
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    /**
     * 更新数据大小显示
     */
    private void updateDataSize() {
        long size = dataManager.getDataSize();
        dataSizeText.setText("数据大小: " + dataManager.formatDataSize(size));
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

