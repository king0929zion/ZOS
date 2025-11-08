package com.browseros.android.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.browseros.android.R;
import com.browseros.android.ai.AIService;
import com.browseros.android.ai.AnthropicProvider;
import com.browseros.android.ai.OpenAIProvider;
import com.browseros.android.privacy.DataManager;
import com.browseros.android.privacy.SecureStorage;

/**
 * AI 对话活动
 * 提供与 AI 模型的对话界面
 * 
 * @author BrowserOS Team
 */
public class AIChatActivity extends AppCompatActivity {
    private AIService aiService;
    private SecureStorage secureStorage;
    
    private TextView chatDisplay;
    private EditText messageInput;
    private Button sendButton;
    private ScrollView scrollView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        
        // 显示返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // 初始化组件
        DataManager dataManager = new DataManager(this);
        secureStorage = dataManager.getSecureStorage();
        
        initializeViews();
        initializeAIService();
        setupListeners();
    }
    
    /**
     * 初始化视图组件
     */
    private void initializeViews() {
        chatDisplay = findViewById(R.id.chat_display);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        scrollView = findViewById(R.id.chat_scroll);
        
        // 显示欢迎消息
        appendMessage("系统", "欢迎使用 BrowserOS AI 助手！\n请选择 AI 服务提供商并配置 API 密钥。");
    }
    
    /**
     * 初始化 AI 服务
     * 按优先级尝试：OpenAI -> Anthropic
     */
    private void initializeAIService() {
        // 尝试 OpenAI
        String openaiKey = secureStorage.getApiKey("openai_api_key");
        if (openaiKey != null && !openaiKey.isEmpty()) {
            // 获取自定义URL和模型
            String openaiUrl = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("openai_url", "https://api.openai.com/v1/chat/completions");
            String openaiModel = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("openai_model", "gpt-3.5-turbo");
            aiService = new OpenAIProvider(openaiKey, openaiUrl, openaiModel);
            appendMessage("系统", "已连接到 OpenAI（" + openaiModel + "）");
            return;
        }
        
        // 尝试 Anthropic
        String anthropicKey = secureStorage.getApiKey("anthropic_api_key");
        if (anthropicKey != null && !anthropicKey.isEmpty()) {
            aiService = new AnthropicProvider(anthropicKey);
            appendMessage("系统", "已连接到 Anthropic Claude");
            return;
        }
        
        // 没有配置任何服务
        appendMessage("系统", "未检测到已配置的 AI 服务");
        appendMessage("系统", "请在设置中配置 OpenAI 或 Anthropic API 密钥");
    }
    
    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        
        // 回车发送
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }
    
    /**
     * 发送消息
     */
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }
        
        if (aiService == null) {
            Toast.makeText(this, "AI 服务未配置，请在设置中配置 API 密钥", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 显示用户消息
        appendMessage("你", message);
        messageInput.setText("");
        
        // 显示"正在思考..."
        appendMessage("AI", "正在思考...");
        
        // 发送到 AI
        aiService.chat(message, new AIService.AICallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    // 移除"正在思考..."消息
                    String currentText = chatDisplay.getText().toString();
                    if (currentText.endsWith("正在思考...\n\n")) {
                        currentText = currentText.substring(0, 
                                currentText.length() - "正在思考...\n\n".length());
                        chatDisplay.setText(currentText);
                    }
                    
                    // 显示 AI 回复
                    appendMessage("AI", response);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // 移除"正在思考..."消息
                    String currentText = chatDisplay.getText().toString();
                    if (currentText.endsWith("正在思考...\n\n")) {
                        currentText = currentText.substring(0, 
                                currentText.length() - "正在思考...\n\n".length());
                        chatDisplay.setText(currentText);
                    }
                    
                    appendMessage("系统", "错误: " + error);
                });
            }
        });
    }
    
    /**
     * 添加消息到聊天显示
     * @param sender 发送者
     * @param message 消息内容
     */
    private void appendMessage(String sender, String message) {
        String currentText = chatDisplay.getText().toString();
        String newText = currentText + sender + ": " + message + "\n\n";
        chatDisplay.setText(newText);
        
        // 滚动到底部
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
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

