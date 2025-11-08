const btnAI = document.getElementById('btnAI');
const btnMenu = document.getElementById('btnMenu');
const btnCloseMenu = document.getElementById('btnCloseMenu');
const btnCloseAI = document.getElementById('btnCloseAI');
const menuPanel = document.getElementById('menuPanel');
const aiPanel = document.getElementById('aiPanel');
const overlay = document.getElementById('overlay');
const addressBar = document.getElementById('addressBar');
const webview = document.getElementById('webview');
const loadingIndicator = document.getElementById('loadingIndicator');
const aiChatContainer = document.getElementById('aiChatContainer');
const aiInput = document.getElementById('aiInput');
const btnSend = document.getElementById('btnSend');
const menuItems = document.querySelectorAll('.menu-item');

const state = {
    isMenuOpen: false,
    isAIOpen: false,
    isDesktopMode: false,
    chatHistory: []
};

const boot = () => {
    registerEventHandlers();
    initializeWebView();
    syncOverlay();
    menuPanel.setAttribute('aria-hidden', 'true');
    aiPanel.setAttribute('aria-hidden', 'true');
    btnAI.setAttribute('aria-expanded', 'false');
    btnMenu.setAttribute('aria-expanded', 'false');
};

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
} else {
    boot();
}

function registerEventHandlers() {
    btnAI.addEventListener('click', toggleAIPanel);
    btnCloseAI.addEventListener('click', closeAIPanel);

    btnMenu.addEventListener('click', toggleMenuPanel);
    btnCloseMenu.addEventListener('click', closeMenuPanel);

    overlay.addEventListener('click', () => {
        if (state.isMenuOpen) closeMenuPanel();
        if (state.isAIOpen) closeAIPanel();
    });

    addressBar.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            loadURL(addressBar.value);
        }
    });

    aiInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            sendAIMessage();
        }
    });

    btnSend.addEventListener('click', sendAIMessage);

    menuItems.forEach((item) => {
        item.addEventListener('click', () => {
            const action = item.getAttribute('data-action');
            handleMenuAction(action);
        });
    });
}

function initializeWebView() {
    loadingIndicator.classList.add('active');

    webview.addEventListener('load', () => {
        loadingIndicator.classList.remove('active');
        updateAddressBar();
    });

    webview.addEventListener('error', () => {
        loadingIndicator.classList.remove('active');
        showMessage('加载失败，请检查网址是否有效');
    });
}

function toggleMenuPanel() {
    if (state.isAIOpen) {
        closeAIPanel();
    }

    state.isMenuOpen = !state.isMenuOpen;
    menuPanel.classList.toggle('active', state.isMenuOpen);
    menuPanel.setAttribute('aria-hidden', String(!state.isMenuOpen));
    btnMenu.classList.toggle('active', state.isMenuOpen);
    btnMenu.setAttribute('aria-expanded', String(state.isMenuOpen));
    syncOverlay();
}

function closeMenuPanel() {
    state.isMenuOpen = false;
    menuPanel.classList.remove('active');
    menuPanel.setAttribute('aria-hidden', 'true');
    btnMenu.classList.remove('active');
    btnMenu.setAttribute('aria-expanded', 'false');
    syncOverlay();
}

function toggleAIPanel() {
    if (state.isMenuOpen) {
        closeMenuPanel();
    }

    state.isAIOpen = !state.isAIOpen;
    aiPanel.classList.toggle('active', state.isAIOpen);
    aiPanel.setAttribute('aria-hidden', String(!state.isAIOpen));
    btnAI.classList.toggle('active', state.isAIOpen);
    btnAI.setAttribute('aria-expanded', String(state.isAIOpen));
    syncOverlay();
}

function closeAIPanel() {
    state.isAIOpen = false;
    aiPanel.classList.remove('active');
    aiPanel.setAttribute('aria-hidden', 'true');
    btnAI.classList.remove('active');
    btnAI.setAttribute('aria-expanded', 'false');
    syncOverlay();
}

function syncOverlay() {
    const shouldShow = state.isMenuOpen || state.isAIOpen;
    overlay.classList.toggle('active', shouldShow);
    overlay.setAttribute('aria-hidden', String(!shouldShow));
}

function loadURL(input) {
    if (!input) return;

    let url = input.trim();
    if (!url) return;

    if (!/^https?:\/\//i.test(url)) {
        if (url.includes(' ') || !url.includes('.')) {
            url = `https://www.google.com/search?q=${encodeURIComponent(url)}`;
        } else {
            url = `https://${url}`;
        }
    }

    loadingIndicator.classList.add('active');
    webview.src = url;
    addressBar.value = url;
}

function updateAddressBar() {
    try {
        const currentURL = webview.contentWindow?.location.href;
        if (currentURL) {
            addressBar.value = currentURL;
        }
    } catch (error) {
        // ignore cross-origin errors
    }
}

function handleMenuAction(action) {
    closeMenuPanel();

    switch (action) {
        case 'history':
            showMessage('历史记录即将推出');
            break;
        case 'refresh':
            webview.src = webview.src;
            showMessage('页面刷新中');
            break;
        case 'downloads':
            showMessage('下载记录即将开放');
            break;
        case 'desktop':
            toggleDesktopMode();
            break;
        case 'add-bookmark':
            addBookmark();
            break;
        case 'bookmarks':
            showBookmarkPreview();
            break;
        case 'home':
            loadURL('https://www.google.com');
            break;
        case 'back':
            if (webview.contentWindow) {
                webview.contentWindow.history.back();
            }
            break;
        case 'settings':
            showMessage('设置页仅用于展示，暂未开放');
            break;
        default:
            break;
    }
}

function toggleDesktopMode() {
    state.isDesktopMode = !state.isDesktopMode;
    webview.classList.toggle('desktop-mode', state.isDesktopMode);
    const message = state.isDesktopMode ? '已切换到桌面模式（演示效果）' : '已切换回移动模式';
    showMessage(message);
}

function addBookmark() {
    const url = (addressBar.value || webview.src || '').trim();
    if (!url) {
        showMessage('当前页面无法收藏');
        return;
    }

    const title = webview.contentDocument?.title || '未命名页面';
    const bookmarks = JSON.parse(localStorage.getItem('browseros-bookmarks') || '[]');
    bookmarks.push({ url, title, createdAt: Date.now() });
    localStorage.setItem('browseros-bookmarks', JSON.stringify(bookmarks));
    showMessage('已添加到书签');
}

function showBookmarkPreview() {
    const bookmarks = JSON.parse(localStorage.getItem('browseros-bookmarks') || '[]');
    if (!bookmarks.length) {
        showMessage('暂无书签，先收藏一个页面吧');
        return;
    }

    const latest = bookmarks
        .slice(-2)
        .map((item) => item.title || item.url)
        .join(' · ');
    showMessage(`最近收藏：${latest}`);
}

function sendAIMessage() {
    const message = aiInput.value.trim();
    if (!message) return;

    addChatMessage('user', message);
    aiInput.value = '';

    setTimeout(() => {
        const aiReply = generateAIResponse(message);
        addChatMessage('ai', aiReply);
    }, 600);
}

function addChatMessage(type, content) {
    const wrapper = document.createElement('div');
    wrapper.className = `ai-message ${type}`;

    const bubble = document.createElement('div');
    bubble.className = 'message-content';
    bubble.textContent = content;

    wrapper.appendChild(bubble);
    aiChatContainer.appendChild(wrapper);
    aiChatContainer.scrollTop = aiChatContainer.scrollHeight;

    state.chatHistory.push({ type, content, timestamp: Date.now() });
}

function generateAIResponse(message) {
    const normalized = message.trim();
    const lower = normalized.toLowerCase();

    const urlMatch = normalized.match(/(https?:\/\/[\w.-]+[^\s]*|www\.[^\s]+|[\w-]+\.[a-z]{2,})/i);

    if (/(打开|访问|go to|navigate)/i.test(normalized) && urlMatch) {
        const target = urlMatch[0];
        setTimeout(() => {
            loadURL(target);
            closeAIPanel();
        }, 200);
        return `好的，正在为你打开 ${target}`;
    }

    if (/(搜索|search)/i.test(normalized)) {
        const query = normalized.replace(/搜索|search/gi, '').trim() || normalized;
        setTimeout(() => {
            loadURL(`https://www.google.com/search?q=${encodeURIComponent(query)}`);
            closeAIPanel();
        }, 200);
        return `已经为你搜索 “${query}”`;
    }

    if (/(刷新|重载|reload)/i.test(lower)) {
        setTimeout(() => {
            webview.src = webview.src;
        }, 200);
        return '已触发刷新';
    }

    if (/(主页|home)/i.test(lower)) {
        setTimeout(() => {
            loadURL('https://www.google.com');
            closeAIPanel();
        }, 200);
        return '正在返回主页';
    }

    if (/(后退|上一页|back)/i.test(lower)) {
        setTimeout(() => {
            if (webview.contentWindow) {
                webview.contentWindow.history.back();
            }
        }, 200);
        return '已执行后退指令';
    }

    if (/(前进|next|forward)/i.test(lower)) {
        setTimeout(() => {
            if (webview.contentWindow) {
                webview.contentWindow.history.forward();
            }
        }, 200);
        return '正在尝试前进一页';
    }

    if (/书签/.test(lower) && /(添加|收藏)/.test(lower)) {
        setTimeout(() => addBookmark(), 200);
        return '已尝试将当前页面加入书签';
    }

    if (/桌面/.test(lower)) {
        setTimeout(() => toggleDesktopMode(), 200);
        return state.isDesktopMode ? '已切回移动模式' : '已切换到桌面模式';
    }

    return `我理解你说的 “${normalized}”。在真实应用中我可以打开网站、搜索内容、刷新页面、返回主页或管理书签，你可以继续告诉我需要执行的操作。`;
}

function showMessage(message) {
    const toast = document.createElement('div');
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 24px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(16, 24, 40, 0.9);
        color: #fff;
        padding: 12px 24px;
        border-radius: 999px;
        font-size: 14px;
        z-index: 999;
        box-shadow: 0 12px 30px rgba(15, 23, 42, 0.35);
        opacity: 0;
        transition: opacity 0.3s ease;
    `;

    document.body.appendChild(toast);
    requestAnimationFrame(() => {
        toast.style.opacity = '1';
    });

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

window.BrowserOS = {
    loadURL,
    toggleAIPanel,
    toggleMenuPanel,
    addChatMessage
};
