package com.browseros.android;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.browseros.android.ai.AIService;
import com.browseros.android.ai.AgentExecutor;
import com.browseros.android.ai.AgentToolManager;
import com.browseros.android.ai.AnthropicProvider;
import com.browseros.android.ai.OpenAIProvider;
import com.browseros.android.ai.PageContentExtractor;
import com.browseros.android.browser.BookmarkManager;
import com.browseros.android.browser.BrowserEngine;
import com.browseros.android.browser.HistoryManager;
import com.browseros.android.browser.TabManager;
import com.browseros.android.privacy.DataManager;
import com.browseros.android.privacy.SecureStorage;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_HOME = "https://www.google.com";
    private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private EditText addressInput;
    private MaterialButton aiButton;
    private ImageButton menuButton;
    private ImageButton aiCloseButton;
    private View overlay;
    private View menuPanel;
    private View aiPanel;
    private GridLayout menuGrid;
    private LinearLayout aiMessageContainer;
    private NestedScrollView aiScroll;
    private EditText aiInputField;
    private ImageButton aiSendButton;
    private LinearProgressIndicator pageProgress;
    private Chip desktopToggleChip;

    private TabManager tabManager;
    private BrowserEngine browserEngine;
    private DataManager dataManager;
    private BookmarkManager bookmarkManager;
    private SecureStorage secureStorage;
    private AIService aiService;
    private AgentToolManager agentToolManager;
    private AgentExecutor agentExecutor;
    private PageContentExtractor contentExtractor;

    private WebView webView;
    private boolean isMenuOpen = false;
    private boolean isAiOpen = false;
    private boolean isDesktopMode = false;
    private boolean isIncognitoMode = false;
    private String mobileUserAgent;
    private final Pattern urlPattern = Patterns.WEB_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataManager = new DataManager(this);
        bookmarkManager = new BookmarkManager(this);
        secureStorage = dataManager.getSecureStorage();

        initializeViews();
        setupMenuGrid();
        setupListeners();
        setupQuickActions();
        initializeBrowser();

        tabManager.createTab(DEFAULT_HOME);
        updateWebView();
        ensureAiWelcomeMessage();
    }

    private void initializeViews() {
        addressInput = findViewById(R.id.address_input);
        aiButton = findViewById(R.id.btn_ai);
        menuButton = findViewById(R.id.btn_menu);
        aiCloseButton = findViewById(R.id.btn_close_ai);
        overlay = findViewById(R.id.panel_overlay);
        menuPanel = findViewById(R.id.menu_panel);
        aiPanel = findViewById(R.id.ai_panel);
        menuGrid = findViewById(R.id.menu_grid);
        aiMessageContainer = findViewById(R.id.ai_message_container);
        aiScroll = findViewById(R.id.ai_scroll);
        aiInputField = findViewById(R.id.ai_input_field);
        aiSendButton = findViewById(R.id.ai_send_button);
        pageProgress = findViewById(R.id.page_progress);
    }

    private void setupListeners() {
        addressInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                loadUrl(addressInput.getText().toString());
                return true;
            }
            return false;
        });

        aiButton.setOnClickListener(v -> {
            if (isMenuOpen) {
                closeMenuPanel();
            }
            toggleAiPanel();
        });

        menuButton.setOnClickListener(v -> {
            if (isAiOpen) {
                closeAiPanel();
            }
            toggleMenuPanel();
        });

        overlay.setOnClickListener(v -> {
            closeMenuPanel();
            closeAiPanel();
        });

        aiCloseButton.setOnClickListener(v -> closeAiPanel());
        aiSendButton.setOnClickListener(v -> sendAiMessage());
        aiInputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAiMessage();
                return true;
            }
            return false;
        });
    }

    private void setupQuickActions() {
        Chip refreshChip = findViewById(R.id.chip_refresh);
        if (refreshChip != null) {
            refreshChip.setOnClickListener(v -> {
                addChatMessage(ChatSender.USER, getString(R.string.chip_refresh));
                performRefreshAction();
                addChatMessage(ChatSender.BOT, getString(R.string.ai_refreshed));
            });
        }

        Chip backChip = findViewById(R.id.chip_back);
        if (backChip != null) {
            backChip.setOnClickListener(v -> {
                addChatMessage(ChatSender.USER, getString(R.string.chip_back));
                boolean moved = performBackAction();
                addChatMessage(ChatSender.BOT,
                        getString(moved ? R.string.ai_back_success : R.string.ai_back_fail));
            });
        }

        Chip forwardChip = findViewById(R.id.chip_forward);
        if (forwardChip != null) {
            forwardChip.setOnClickListener(v -> {
                addChatMessage(ChatSender.USER, getString(R.string.chip_forward));
                boolean moved = performForwardAction();
                addChatMessage(ChatSender.BOT,
                        getString(moved ? R.string.ai_forward_success : R.string.ai_forward_fail));
            });
        }

        Chip newTabChip = findViewById(R.id.chip_new_tab);
        if (newTabChip != null) {
            newTabChip.setOnClickListener(v -> {
                addChatMessage(ChatSender.USER, getString(R.string.chip_new_tab));
                createNewForegroundTab(DEFAULT_HOME);
                addChatMessage(ChatSender.BOT, getString(R.string.ai_new_tab_blank));
            });
        }

        desktopToggleChip = findViewById(R.id.chip_desktop);
        if (desktopToggleChip != null) {
            desktopToggleChip.setOnClickListener(v -> {
                addChatMessage(ChatSender.USER, desktopToggleChip.getText());
                toggleDesktopMode();
                addChatMessage(ChatSender.BOT,
                        getString(isDesktopMode ? R.string.toast_desktop_on : R.string.toast_desktop_off));
            });
            updateDesktopChipLabel();
        }

        Chip scrollDownChip = findViewById(R.id.chip_scroll_down);
        if (scrollDownChip != null) {
            scrollDownChip.setOnClickListener(v -> {
                addChatMessage(ChatSender.USER, getString(R.string.chip_scroll_down));
                scrollWebView(true);
                addChatMessage(ChatSender.BOT, getString(R.string.ai_scroll_down));
            });
        }

        Chip scrollUpChip = findViewById(R.id.chip_scroll_up);
        if (scrollUpChip != null) {
            scrollUpChip.setOnClickListener(v -> {
                addChatMessage(ChatSender.USER, getString(R.string.chip_scroll_up));
                scrollWebView(false);
                addChatMessage(ChatSender.BOT, getString(R.string.ai_scroll_up));
            });
        }
    }

    private void setupMenuGrid() {
        menuGrid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        MenuAction[] actions = new MenuAction[]{
                new MenuAction(MenuActionType.HISTORY, R.drawable.ic_history, R.string.menu_history, R.string.menu_history_desc),
                new MenuAction(MenuActionType.REFRESH, R.drawable.ic_refresh, R.string.menu_refresh, R.string.menu_refresh_desc),
                new MenuAction(MenuActionType.DOWNLOADS, R.drawable.ic_download, R.string.menu_downloads, R.string.menu_downloads_desc),
                new MenuAction(MenuActionType.DESKTOP, R.drawable.ic_desktop, R.string.menu_desktop, R.string.menu_desktop_desc),
                new MenuAction(MenuActionType.ADD_BOOKMARK, R.drawable.ic_bookmark_add, R.string.menu_add_bookmark, R.string.menu_add_bookmark_desc),
                new MenuAction(MenuActionType.BOOKMARKS, R.drawable.ic_bookmarks, R.string.menu_bookmarks, R.string.menu_bookmarks_desc),
                new MenuAction(MenuActionType.HOME, R.drawable.ic_home, R.string.menu_home, R.string.menu_home_desc),
                new MenuAction(MenuActionType.BACK, R.drawable.ic_back, R.string.menu_back, R.string.menu_back_desc),
                new MenuAction(MenuActionType.SETTINGS, R.drawable.ic_settings, R.string.menu_settings, R.string.menu_settings_desc),
                new MenuAction(MenuActionType.INCOGNITO, R.drawable.ic_incognito, R.string.menu_incognito, R.string.menu_incognito_desc),
                new MenuAction(MenuActionType.SHARE, R.drawable.ic_share, R.string.menu_share, R.string.menu_share_desc),
                new MenuAction(MenuActionType.FIND, R.drawable.ic_find, R.string.menu_find, R.string.menu_find_desc),
                new MenuAction(MenuActionType.INFO, R.drawable.ic_info, R.string.menu_info, R.string.menu_info_desc)
        };

        for (MenuAction action : actions) {
            View item = inflater.inflate(R.layout.menu_action_item, menuGrid, false);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(12, 12, 12, 12);
            item.setLayoutParams(params);
            ImageView icon = item.findViewById(R.id.action_icon);
            TextView title = item.findViewById(R.id.action_title);
            TextView subtitle = item.findViewById(R.id.action_subtitle);
            icon.setImageResource(action.iconRes);
            title.setText(action.titleRes);
            subtitle.setText(action.subtitleRes);
            item.setOnClickListener(v -> handleMenuAction(action.type));
            menuGrid.addView(item);
        }
    }

    private void toggleMenuPanel() {
        if (isMenuOpen) {
            closeMenuPanel();
        } else {
            openPanel(menuPanel);
            isMenuOpen = true;
        }
        syncOverlay();
    }

    private void closeMenuPanel() {
        if (!isMenuOpen) return;
        closePanel(menuPanel);
        isMenuOpen = false;
        syncOverlay();
    }

    private void toggleAiPanel() {
        if (isAiOpen) {
            closeAiPanel();
        } else {
            initializeAiServiceIfNeeded();
            ensureAiWelcomeMessage();
            openPanel(aiPanel);
            isAiOpen = true;
        }
        syncOverlay();
    }

    private void closeAiPanel() {
        if (!isAiOpen) return;
        closePanel(aiPanel);
        isAiOpen = false;
        syncOverlay();
    }

    private void syncOverlay() {
        overlay.setVisibility(isMenuOpen || isAiOpen ? View.VISIBLE : View.GONE);
    }

    private void openPanel(View panel) {
        panel.setVisibility(View.VISIBLE);
        panel.post(() -> {
            panel.setTranslationY(panel.getHeight());
            panel.animate()
                    .translationY(0f)
                    .setDuration(280)
                    .setInterpolator(AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in))
                    .start();
        });
    }

    private void closePanel(View panel) {
        panel.animate()
                .translationY(panel.getHeight())
                .setDuration(220)
                .withEndAction(() -> panel.setVisibility(View.GONE))
                .start();
    }

    private void handleMenuAction(MenuActionType type) {
        closeMenuPanel();
        switch (type) {
            case HISTORY:
                showHistoryDialog();
                break;
            case REFRESH:
                performRefreshAction();
                break;
            case DOWNLOADS:
                openDownloads();
                break;
            case DESKTOP:
                toggleDesktopMode();
                break;
            case ADD_BOOKMARK:
                addBookmark();
                break;
            case BOOKMARKS:
                showBookmarkDialog();
                break;
            case HOME:
                loadUrl(DEFAULT_HOME);
                break;
            case BACK:
                if (!performBackAction()) {
                    showToast(R.string.toast_no_previous_page);
                }
                break;
            case SETTINGS:
                startActivity(new Intent(this, com.browseros.android.ui.SettingsActivity.class));
                break;
            case INCOGNITO:
                toggleIncognitoMode();
                break;
            case SHARE:
                sharePage();
                break;
            case FIND:
                findInPage();
                break;
            case INFO:
                showPageInfo();
                break;
        }
    }

    private void showHistoryDialog() {
        List<HistoryManager.HistoryItem> history = dataManager.getHistoryManager().getHistory(15);
        if (history.isEmpty()) {
            showToast(R.string.toast_history_empty);
            return;
        }
        CharSequence[] entries = new CharSequence[history.size()];
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (int i = 0; i < history.size(); i++) {
            HistoryManager.HistoryItem item = history.get(i);
            String title = TextUtils.isEmpty(item.getTitle()) ? item.getUrl() : item.getTitle();
            entries[i] = title + "\n" + df.format(item.getTimestamp());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_history_title)
                .setItems(entries, (dialog, which) -> loadUrl(history.get(which).getUrl()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showBookmarkDialog() {
        List<BookmarkManager.BookmarkItem> bookmarks = bookmarkManager.getBookmarks();
        if (bookmarks.isEmpty()) {
            showToast(R.string.toast_no_bookmarks);
            return;
        }
        CharSequence[] entries = new CharSequence[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) {
            BookmarkManager.BookmarkItem item = bookmarks.get(i);
            entries[i] = item.title + "\n" + item.url;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_bookmarks_title)
                .setItems(entries, (dialog, which) -> loadUrl(bookmarks.get(which).url))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openDownloads() {
        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            showToast(R.string.toast_downloads_unavailable);
        }
    }

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        applyUserAgentToCurrentTab();
        if (browserEngine != null) {
            browserEngine.reload();
        }
        updateDesktopChipLabel();
        showToast(isDesktopMode ? R.string.toast_desktop_on : R.string.toast_desktop_off);
    }

    private void updateDesktopChipLabel() {
        if (desktopToggleChip != null) {
            desktopToggleChip.setText(isDesktopMode ? R.string.chip_mobile_mode : R.string.chip_desktop);
        }
    }

    private void addBookmark() {
        if (browserEngine == null || TextUtils.isEmpty(browserEngine.getUrl())) {
            showToast(R.string.toast_no_bookmarks);
            return;
        }
        bookmarkManager.addBookmark(browserEngine.getUrl(), browserEngine.getTitle());
        showToast(R.string.toast_bookmark_added);
    }

    private void loadUrl(String input) {
        if (browserEngine == null || TextUtils.isEmpty(input)) return;
        String url = input.trim();
        if (url.isEmpty()) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(" ") || !url.contains(".")) {
                url = "https://www.google.com/search?q=" + Uri.encode(url);
            } else {
                url = "https://" + url;
            }
        }
        addressInput.setText(url);
        browserEngine.loadUrl(url);
    }

    private void updateWebView() {
        TabManager.BrowserTab currentTab = tabManager.getCurrentTab();
        if (currentTab == null) {
            return;
        }
        ViewGroup container = findViewById(R.id.web_view_container);
        WebView tabWebView = currentTab.getWebView();
        if (webView != null && webView != tabWebView && webView.getParent() == container) {
            container.removeView(webView);
        }
        if (tabWebView.getParent() == null) {
            container.addView(tabWebView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        webView = tabWebView;
        mobileUserAgent = webView.getSettings().getUserAgentString();
        applyUserAgentToCurrentTab();
        updateBrowserEngine();
    }

    private void applyUserAgentToCurrentTab() {
        if (webView == null) return;
        WebSettings settings = webView.getSettings();
        if (mobileUserAgent == null) {
            mobileUserAgent = settings.getUserAgentString();
        }
        settings.setUserAgentString(isDesktopMode ? DESKTOP_UA : mobileUserAgent);
    }

    private void initializeBrowser() {
        tabManager = new TabManager(this);
    }

    private void updateBrowserEngine() {
        TabManager.BrowserTab currentTab = tabManager.getCurrentTab();
        if (currentTab == null) return;
        browserEngine = currentTab.getBrowserEngine();
        browserEngine.setBrowserListener(new BrowserEngine.BrowserListener() {
            @Override
            public void onPageStarted(String url) {
                runOnUiThread(() -> {
                    addressInput.setText(url);
                    pageProgress.setVisibility(View.VISIBLE);
                    pageProgress.setProgress(0);
                });
            }

            @Override
            public void onPageFinished(String url) {
                runOnUiThread(() -> {
                    addressInput.setText(url);
                    pageProgress.setVisibility(View.GONE);
                    dataManager.getHistoryManager().addHistory(url, browserEngine.getTitle());
                });
            }

            @Override
            public void onTitleReceived(String title) {
                runOnUiThread(() -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(title);
                    }
                });
            }

            @Override
            public void onProgressChanged(int progress) {
                runOnUiThread(() -> {
                    pageProgress.setVisibility(View.VISIBLE);
                    pageProgress.setProgress(progress);
                });
            }

            @Override
            public void onReceivedError(int errorCode, String description, String failingUrl) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        getString(R.string.error_loading), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void performRefreshAction() {
        if (browserEngine != null) {
            browserEngine.reload();
        }
    }

    private boolean performBackAction() {
        return browserEngine != null && browserEngine.goBack();
    }

    private boolean performForwardAction() {
        return browserEngine != null && browserEngine.goForward();
    }

    private void createNewForegroundTab(String url) {
        tabManager.createTab(url);
        updateWebView();
    }

    private boolean switchToTabByIndex(int index) {
        if (index <= 0) {
            return false;
        }
        List<TabManager.BrowserTab> tabs = tabManager.getAllTabs();
        if (index > tabs.size()) {
            return false;
        }
        TabManager.BrowserTab target = tabs.get(index - 1);
        tabManager.switchTab(target.getId());
        updateWebView();
        return true;
    }

    private boolean closeCurrentTab() {
        TabManager.BrowserTab current = tabManager.getCurrentTab();
        if (current == null) {
            return false;
        }
        boolean closed = tabManager.closeTab(current.getId());
        if (tabManager.getCurrentTab() == null) {
            tabManager.createTab(DEFAULT_HOME);
        }
        updateWebView();
        return closed;
    }

    private void scrollWebView(boolean down) {
        if (webView == null) {
            return;
        }
        int distance = (int) (webView.getHeight() * 0.8f);
        if (distance == 0) {
            distance = 400;
        }
        int finalDistance = down ? distance : -distance;
        webView.post(() -> webView.scrollBy(0, finalDistance));
    }

    private void initializeAiServiceIfNeeded() {
        if (aiService != null) return;
        String openaiKey = secureStorage.getApiKey("openai_api_key");
        if (!TextUtils.isEmpty(openaiKey)) {
            // 获取自定义URL和模型
            String openaiUrl = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("openai_url", "https://api.openai.com/v1/chat/completions");
            String openaiModel = getSharedPreferences("settings", MODE_PRIVATE)
                    .getString("openai_model", "gpt-3.5-turbo");
            aiService = new OpenAIProvider(openaiKey, openaiUrl, openaiModel);
            
            // 初始化 Agent 系统
            if (browserEngine != null && webView != null) {
                agentToolManager = new AgentToolManager(this, browserEngine);
                agentExecutor = new AgentExecutor(aiService, agentToolManager);
                contentExtractor = new PageContentExtractor(webView);
            }
            
            addChatMessage(ChatSender.BOT, getString(R.string.ai_connected_openai));
            return;
        }
        String anthropicKey = secureStorage.getApiKey("anthropic_api_key");
        if (!TextUtils.isEmpty(anthropicKey)) {
            aiService = new AnthropicProvider(anthropicKey);
            addChatMessage(ChatSender.BOT, getString(R.string.ai_connected_anthropic));
            return;
        }
        // 如果没有配置任何API密钥，提示用户
        addChatMessage(ChatSender.BOT, getString(R.string.ai_no_service_configured));
    }

    private void ensureAiWelcomeMessage() {
        if (aiMessageContainer.getChildCount() == 0) {
            addChatMessage(ChatSender.BOT, getString(R.string.ai_welcome));
        }
    }

    private void sendAiMessage() {
        String message = aiInputField.getText().toString().trim();
        if (message.isEmpty()) return;

        aiInputField.setText("");
        addChatMessage(ChatSender.USER, message);

        if (handleLocalInstruction(message)) {
            return;
        }
        if (aiService == null) {
            showToast(R.string.toast_ai_unavailable);
            return;
        }
        TextView thinkingView = addChatMessage(ChatSender.BOT, getString(R.string.ai_thinking));
        
        // 如果 Agent 系统已初始化，使用 Agent 执行器
        if (agentExecutor != null && agentToolManager != null) {
            agentExecutor.execute(message, new AgentExecutor.AgentCallback() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> thinkingView.setText(result));
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> thinkingView.setText(getString(R.string.ai_error_message, error)));
                }
            });
        } else {
            // 回退到普通聊天模式
            aiService.chat(message, new AIService.AICallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> thinkingView.setText(response));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> thinkingView.setText(getString(R.string.ai_error_message, error)));
                }
            });
        }
    }

    private boolean handleLocalInstruction(String message) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        String normalized = message.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        String targetUrl = findUrlInText(normalized);

        if ((lower.contains("打开") || lower.contains("visit") || lower.contains("navigate")) && targetUrl != null) {
            loadUrl(targetUrl);
            addChatMessage(ChatSender.BOT, getString(R.string.ai_opening_url, targetUrl));
            return true;
        }

        if (lower.contains("搜索") || lower.contains("search")) {
            String query = normalized.replaceAll("搜索|search", "").trim();
            if (query.isEmpty()) query = normalized;
            loadUrl("https://www.google.com/search?q=" + Uri.encode(query));
            addChatMessage(ChatSender.BOT, getString(R.string.ai_searching_query, query));
            return true;
        }

        if (lower.contains("刷新") || lower.contains("reload")) {
            performRefreshAction();
            addChatMessage(ChatSender.BOT, getString(R.string.ai_refreshed));
            return true;
        }

        if (lower.contains("主页") || lower.contains("home")) {
            loadUrl(DEFAULT_HOME);
            addChatMessage(ChatSender.BOT, getString(R.string.ai_home));
            return true;
        }

        if (lower.contains("后退") || lower.contains("back")) {
            boolean moved = performBackAction();
            addChatMessage(ChatSender.BOT,
                    getString(moved ? R.string.ai_back_success : R.string.ai_back_fail));
            return true;
        }

        if (lower.contains("前进") || lower.contains("forward")) {
            boolean moved = performForwardAction();
            addChatMessage(ChatSender.BOT,
                    getString(moved ? R.string.ai_forward_success : R.string.ai_forward_fail));
            return true;
        }

        if (lower.contains("新标签") || lower.contains("new tab")) {
            if (targetUrl != null) {
                createNewForegroundTab(targetUrl);
                addChatMessage(ChatSender.BOT, getString(R.string.ai_new_tab_with_url, targetUrl));
            } else {
                createNewForegroundTab(DEFAULT_HOME);
                addChatMessage(ChatSender.BOT, getString(R.string.ai_new_tab_blank));
            }
            return true;
        }

        if ((lower.contains("切换") && lower.contains("标签")) || lower.contains("switch tab")) {
            Matcher matcher = NUMBER_PATTERN.matcher(normalized);
            if (matcher.find()) {
                int index = Integer.parseInt(matcher.group(1));
                boolean switched = switchToTabByIndex(index);
                addChatMessage(ChatSender.BOT,
                        getString(switched ? R.string.ai_switch_tab_success : R.string.ai_switch_tab_fail, index));
            } else {
                addChatMessage(ChatSender.BOT, getString(R.string.ai_switch_tab_fail_generic));
            }
            return true;
        }

        if (((lower.contains("关闭") || lower.contains("关掉") || lower.contains("close")) && lower.contains("标签"))) {
            boolean closed = closeCurrentTab();
            addChatMessage(ChatSender.BOT,
                    getString(closed ? R.string.ai_close_tab_success : R.string.ai_close_tab_fail));
            return true;
        }

        if (lower.contains("滚动") || lower.contains("scroll")) {
            boolean down = lower.contains("下") || lower.contains("底") || lower.contains("down");
            scrollWebView(down);
            addChatMessage(ChatSender.BOT, getString(down ? R.string.ai_scroll_down : R.string.ai_scroll_up));
            return true;
        }

        if (lower.contains("停止") || lower.contains("stop")) {
            if (browserEngine != null) {
                browserEngine.stopLoading();
            }
            addChatMessage(ChatSender.BOT, getString(R.string.ai_stop_loading));
            return true;
        }

        if (lower.contains("桌面") || lower.contains("desktop")) {
            toggleDesktopMode();
            addChatMessage(ChatSender.BOT,
                    getString(isDesktopMode ? R.string.toast_desktop_on : R.string.toast_desktop_off));
            return true;
        }

        if (lower.contains("移动模式") || lower.contains("mobile mode")) {
            if (isDesktopMode) {
                toggleDesktopMode();
            }
            addChatMessage(ChatSender.BOT, getString(R.string.toast_desktop_off));
            return true;
        }

        if (lower.contains("菜单") || lower.contains("menu")) {
            openPanel(menuPanel);
            isMenuOpen = true;
            syncOverlay();
            addChatMessage(ChatSender.BOT, getString(R.string.menu_title));
            return true;
        }

        return false;
    }

    private String findUrlInText(String source) {
        Matcher matcher = urlPattern.matcher(source);
        return matcher.find() ? matcher.group() : null;
    }

    private TextView addChatMessage(ChatSender sender, CharSequence text) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(0, 12, 0, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = sender == ChatSender.USER ? Gravity.END : Gravity.START;
        params.setMargins(0, 6, 0, 6);
        bubble.setLayoutParams(params);

        TextView content = new TextView(this);
        content.setText(text);
        content.setTextSize(15);
        content.setTextColor(sender == ChatSender.USER ? 0xFFFFFFFF : getColor(R.color.textPrimary));
        content.setBackgroundResource(sender == ChatSender.USER ? R.drawable.bg_ai_msg_user : R.drawable.bg_ai_msg_bot);
        content.setPadding(32, 20, 32, 20);
        bubble.addView(content);
        aiMessageContainer.addView(bubble);
        aiScroll.post(() -> aiScroll.fullScroll(View.FOCUS_DOWN));
        return content;
    }

    private void showToast(@StringRes int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 切换无痕浏览模式
     */
    private void toggleIncognitoMode() {
        isIncognitoMode = !isIncognitoMode;
        if (isIncognitoMode) {
            // 清除当前会话数据
            if (webView != null) {
                webView.clearCache(true);
                webView.clearHistory();
            }
            showToast(R.string.toast_incognito_on);
        } else {
            showToast(R.string.toast_incognito_off);
        }
    }
    
    /**
     * 分享当前页面
     */
    private void sharePage() {
        if (browserEngine == null || TextUtils.isEmpty(browserEngine.getUrl())) {
            showToast(R.string.toast_no_url_to_share);
            return;
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, browserEngine.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, browserEngine.getUrl());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share)));
    }
    
    /**
     * 在页面中查找文本
     */
    private void findInPage() {
        if (webView == null) {
            showToast(R.string.toast_webview_not_ready);
            return;
        }
        
        // 显示查找对话框
        EditText input = new EditText(this);
        input.setHint(getString(R.string.find_hint));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_find)
                .setView(input)
                .setPositiveButton(R.string.find, (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) {
                        webView.findAllAsync(query);
                        webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
                            if (isDoneCounting && numberOfMatches > 0) {
                                webView.findNext(true);
                                showToast(getString(R.string.find_results, activeMatchOrdinal + 1, numberOfMatches));
                            } else if (isDoneCounting && numberOfMatches == 0) {
                                showToast(R.string.find_no_results);
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * 显示页面信息
     */
    private void showPageInfo() {
        if (browserEngine == null) {
            showToast(R.string.toast_webview_not_ready);
            return;
        }
        
        String url = browserEngine.getUrl();
        String title = browserEngine.getTitle();
        
        if (TextUtils.isEmpty(url)) {
            showToast(R.string.toast_no_page_info);
            return;
        }
        
        StringBuilder info = new StringBuilder();
        info.append(getString(R.string.info_title)).append(": ").append(
                TextUtils.isEmpty(title) ? getString(R.string.info_no_title) : title).append("\n\n");
        info.append(getString(R.string.info_url)).append(": ").append(url).append("\n\n");
        
        if (webView != null) {
            android.webkit.WebSettings settings = webView.getSettings();
            info.append(getString(R.string.info_user_agent)).append(": ").append(settings.getUserAgentString()).append("\n\n");
            info.append(getString(R.string.info_js_enabled)).append(": ").append(settings.getJavaScriptEnabled() ? getString(R.string.yes) : getString(R.string.no)).append("\n");
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_info)
                .setMessage(info.toString())
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (isAiOpen) {
            closeAiPanel();
            return;
        }
        if (isMenuOpen) {
            closeMenuPanel();
            return;
        }
        if (browserEngine != null && browserEngine.canGoBack()) {
            browserEngine.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理 WebView 资源，防止内存泄漏
        if (webView != null) {
            webView.onPause();
            webView.removeAllViews();
            webView.destroyDrawingCache();
            webView.destroy();
            webView = null;
        }
        
        // 关闭所有标签页
        if (tabManager != null) {
            tabManager.closeAllTabs();
            tabManager = null;
        }
        
        // 清理其他引用
        browserEngine = null;
        aiService = null;
        agentExecutor = null;
        agentToolManager = null;
        contentExtractor = null;
        dataManager = null;
        bookmarkManager = null;
        secureStorage = null;
    }

    private enum MenuActionType {
        HISTORY, REFRESH, DOWNLOADS, DESKTOP, ADD_BOOKMARK, BOOKMARKS, HOME, BACK, SETTINGS,
        INCOGNITO, SHARE, FIND, INFO
    }

    private static class MenuAction {
        final MenuActionType type;
        final @DrawableRes int iconRes;
        final @StringRes int titleRes;
        final @StringRes int subtitleRes;

        MenuAction(MenuActionType type, @DrawableRes int iconRes,
                   @StringRes int titleRes, @StringRes int subtitleRes) {
            this.type = type;
            this.iconRes = iconRes;
            this.titleRes = titleRes;
            this.subtitleRes = subtitleRes;
        }
    }

    private enum ChatSender {USER, BOT}
}
