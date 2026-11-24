/**
 * LPL VAULT — Nordic Minimalist Shared Utilities & Components
 */
(function (window) {
    const STORAGE_USER_KEY = 'lpl_vault_user';
    const STORAGE_FAVS_KEY = 'lpl_vault_favorites';
    const STORAGE_ORDERS_KEY = 'lpl_vault_orders';

    const TEAM_NAMES = {
        BLG: 'Bilibili Gaming',
        TES: 'Top Esports',
        WBG: 'Weibo Gaming',
        EDG: 'EDward Gaming',
        JDG: 'JD Gaming',
        LNG: 'LNG Esports',
        IG:  'Invictus Gaming',
        AL:  "Anyone's Legend"
    };

    function getTeamFullName(team) {
        return TEAM_NAMES[(team || '').toUpperCase()] || (team || 'LPL');
    }

    function getUser() {
        try {
            const raw = localStorage.getItem(STORAGE_USER_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function setUser(user) {
        if (user) {
            localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(user));
        } else {
            localStorage.removeItem(STORAGE_USER_KEY);
        }
    }

    function logout() {
        localStorage.removeItem(STORAGE_USER_KEY);
        showToast('已退出登录', 'info');
        setTimeout(() => {
            window.location.href = 'home.html';
        }, 650);
    }

    function getFavorites() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_FAVS_KEY) || '[]');
        } catch (e) {
            return [];
        }
    }

    function isFavorite(cardId) {
        return getFavorites().includes(Number(cardId));
    }

    function toggleFavorite(cardId) {
        const id = Number(cardId);
        let favs = getFavorites();
        let added = false;
        if (favs.includes(id)) {
            favs = favs.filter(x => x !== id);
        } else {
            favs.push(id);
            added = true;
        }
        localStorage.setItem(STORAGE_FAVS_KEY, JSON.stringify(favs));
        return added;
    }

    function getOrders() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_ORDERS_KEY) || '[]');
        } catch (e) {
            return [];
        }
    }

    function addOrder(card, buyer) {
        const orders = getOrders();
        orders.unshift({
            orderNo: 'LPL' + Date.now().toString().slice(-8),
            cardId: card.id,
            playerName: card.playerName,
            team: card.team,
            img_url: card.img_url,
            grade: card.grade,
            price: card.price || 0,
            buyerName: buyer ? (buyer.username || buyer.name || '藏家') : '藏家',
            status: '待发货',
            createdAt: new Date().toLocaleString('zh-CN')
        });
        localStorage.setItem(STORAGE_ORDERS_KEY, JSON.stringify(orders));
    }

    function formatPrice(val) {
        const num = Number(val);
        if (Number.isNaN(num) || num <= 0) return '—';
        return num.toFixed(2);
    }

    function getCardImageSrc(imgUrl) {
        if (!imgUrl) return 'image/board2.jpg';
        return `lpl卡片/${imgUrl}.png`;
    }

    function gradeLabel(grade) {
        const g = (grade || 'A').toString().toUpperCase();
        if (g === 'S') return 'S · 典藏';
        if (g === 'A') return 'A · 闪卡';
        return 'B · 基础';
    }

    function formatSeriesText(card) {
        const year = card.releaseYear ? String(card.releaseYear) : '';
        const series = card.series || 'LPL Official';
        if (year && !series.includes(year)) {
            return `${year} ${series}`;
        }
        return series;
    }

    function escapeHtml(str) {
        return String(str ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    /* Quiet Toast */
    function ensureToastStack() {
        let stack = document.getElementById('toast-stack');
        if (!stack) {
            stack = document.createElement('div');
            stack.id = 'toast-stack';
            stack.className = 'toast-stack';
            stack.setAttribute('aria-live', 'polite');
            document.body.appendChild(stack);
        }
        return stack;
    }

    function showToast(message, type = 'info') {
        const stack = ensureToastStack();
        const el = document.createElement('div');
        el.className = `toast toast-${type}`;
        el.textContent = message;
        stack.appendChild(el);
        setTimeout(() => {
            el.style.opacity = '0';
            setTimeout(() => el.remove(), 700);
        }, 2800);
    }

    /* Shared Quiet Header */
    function renderNavbar(activePage) {
        const host = document.getElementById('site-header');
        if (!host) return;
        const user = getUser();
        const displayName = user ? (user.username || user.name || '藏家') : '';
        const initial = displayName ? displayName.charAt(0).toUpperCase() : 'U';

        host.className = 'site-header';
        host.innerHTML = `
            <div class="container header-inner">
                <a href="home.html" class="brand-link" aria-label="LPL VAULT 首页">
                    <span class="brand-title">LPL Vault</span>
                    <span class="brand-sub">Archive</span>
                </a>

                <nav class="header-nav" aria-label="主导航">
                    <a href="home.html" class="nav-link ${activePage === 'home' ? 'active' : ''}">交易大厅</a>
                    <a href="search.html" class="nav-link ${activePage === 'search' ? 'active' : ''}">全库检索</a>
                    <a href="search.html?grade=S" class="nav-link ${activePage === 'limited' ? 'active' : ''}">S 级典藏</a>
                    <a href="userInfo.html" class="nav-link ${activePage === 'user' ? 'active' : ''}">我的卡库</a>
                </nav>

                <div class="header-right">
                    <form class="header-search" id="globalSearchForm" role="search">
                        <input type="text" id="globalSearchInput" placeholder="搜索战队或选手" aria-label="搜索战队或选手" autocomplete="off">
                        <button type="submit" aria-label="执行搜索">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                <circle cx="11" cy="11" r="7.5"/><path d="M20 20l-4-4"/>
                            </svg>
                        </button>
                    </form>

                    <button type="button" class="recog-trigger-btn" id="navRecognizeBtn" title="上传照片识别 LPL 选手" aria-label="以图识人">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <path d="M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2"/>
                            <circle cx="12" cy="11" r="3"/><path d="M8.5 17a3.5 3.5 0 0 1 7 0"/>
                        </svg>
                        <span>识人</span>
                    </button>

                    <div class="header-actions">
                        ${user ? `
                            <a href="userInfo.html" class="user-pill" title="前往个人中心">
                                <span class="user-monogram">${escapeHtml(initial)}</span>
                                <span>${escapeHtml(displayName)}</span>
                            </a>
                            <button type="button" class="text-link" id="navLogoutBtn">退出</button>
                        ` : `
                            <a href="login.html" class="text-link">登录</a>
                            <a href="register.html" class="text-link" style="color:var(--color-charcoal);">注册</a>
                        `}
                    </div>
                </div>
            </div>
        `;

        const form = document.getElementById('globalSearchForm');
        const input = document.getElementById('globalSearchInput');
        const params = new URLSearchParams(window.location.search);
        if (params.get('q') && input) {
            input.value = params.get('q');
        }
        if (form) {
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                const q = (input.value || '').trim();
                window.location.href = q ? `search.html?q=${encodeURIComponent(q)}` : 'search.html';
            });
        }
        const logoutBtn = document.getElementById('navLogoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', logout);
        }
        const recogBtn = document.getElementById('navRecognizeBtn');
        if (recogBtn) {
            recogBtn.addEventListener('click', openRecognizeModal);
        }
    }

    function renderFooter() {
        const host = document.getElementById('site-footer');
        if (!host) return;
        host.className = 'site-footer';
        host.innerHTML = `
            <div class="container">
                <div class="footer-grid">
                    <div>
                        <div class="brand-title" style="margin-bottom: 16px;">LPL Vault</div>
                        <p style="font-size: 14px; color: var(--color-wood); max-width: 30em;">
                            面向英雄联盟职业联赛选手卡藏家的安静交易空间。呈现清晰卡面信息、规范的检索与简约流转体验。
                        </p>
                    </div>
                    <div class="footer-links-cols">
                        <div class="footer-col">
                            <h4>馆藏导航</h4>
                            <ul>
                                <li><a href="home.html">交易大厅</a></li>
                                <li><a href="search.html">全库检索</a></li>
                                <li><a href="search.html?grade=S">S 级典藏</a></li>
                                <li><a href="search.html?team=BLG">BLG 专区</a></li>
                            </ul>
                        </div>
                        <div class="footer-col">
                            <h4>个人空间</h4>
                            <ul>
                                <li><a href="userInfo.html">我的卡库</a></li>
                                <li><a href="login.html">账号登录</a></li>
                                <li><a href="register.html">新用户注册</a></li>
                            </ul>
                        </div>
                        <div class="footer-col">
                            <h4>说明条款</h4>
                            <ul>
                                <li><a href="asserts/privacy_policy.txt" target="_blank">隐私政策</a></li>
                                <li style="color: var(--color-wood);">本地演示环境</li>
                            </ul>
                        </div>
                    </div>
                </div>
                <div class="footer-bottom">
                    <span>&copy; 2024–2026 LPL Vault</span>
                    <span>Quiet Collectibles Exchange</span>
                </div>
            </div>
        `;
    }

    /* Hygge Card Builder (no transform, no shadow, 700ms ease-in-out hover) */
    function createTradingCardElement(card) {
        const el = document.createElement('article');
        const grade = (card.grade || 'A').toString().toUpperCase();
        const faved = isFavorite(card.id);

        el.className = 'hygge-card';
        el.tabIndex = 0;
        el.dataset.id = card.id;

        el.innerHTML = `
            <div class="card-top-meta">
                <span class="card-team-label">${escapeHtml(card.team || 'LPL')}</span>
                <span class="card-grade-tag grade-${escapeHtml(grade)}">${escapeHtml(gradeLabel(grade))}</span>
            </div>
            <div class="card-art-frame">
                <img src="${escapeHtml(getCardImageSrc(card.img_url))}" alt="${escapeHtml(card.team)} ${escapeHtml(card.playerName)}" loading="lazy">
            </div>
            <div class="card-body">
                <div class="card-title-row">
                    <h3 class="card-player-name">${escapeHtml(card.playerName || 'Player')}</h3>
                </div>
                <p class="card-series">${escapeHtml(formatSeriesText(card))} · ${escapeHtml(card.parallel || 'Holo')}</p>
                <span class="card-number">${escapeHtml(card.cardNumber || '')}</span>
                <div class="card-footer-row">
                    <span class="card-price"><span class="currency">¥</span>${formatPrice(card.price)}</span>
                    <button type="button" class="fav-quiet-btn ${faved ? 'active' : ''}" aria-label="${faved ? '取消收藏' : '收藏卡片'}">
                        ${faved ? '已藏' : '收藏'}
                    </button>
                </div>
            </div>
        `;

        const favBtn = el.querySelector('.fav-quiet-btn');
        favBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            const added = toggleFavorite(card.id);
            favBtn.classList.toggle('active', added);
            favBtn.textContent = added ? '已藏' : '收藏';
            favBtn.setAttribute('aria-label', added ? '取消收藏' : '收藏卡片');
            showToast(added ? `已收藏 ${card.team} · ${card.playerName}` : `已移出收藏夹`, added ? 'success' : 'info');
        });

        function openDetail() {
            sessionStorage.setItem('card', JSON.stringify(card));
            window.location.href = `detail.html?id=${encodeURIComponent(card.id)}`;
        }

        el.addEventListener('click', openDetail);
        el.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                openDetail();
            }
        });

        return el;
    }

    /* API Wrappers */
    async function fetchCardList(params = {}) {
        const qs = new URLSearchParams();
        Object.entries(params).forEach(([k, v]) => {
            if (v !== undefined && v !== null && String(v).trim() !== '') {
                qs.set(k, v);
            }
        });
        const res = await fetch(`/card/list${qs.toString() ? '?' + qs.toString() : ''}`);
        if (!res.ok) throw new Error('加载卡片列表失败');
        return res.json();
    }

    async function fetchCardById(id) {
        const res = await fetch(`/card/search_by_id?id=${encodeURIComponent(id)}`);
        if (!res.ok) throw new Error('加载卡片详情失败');
        const text = await res.text();
        return text ? JSON.parse(text) : null;
    }

    async function fetchRecommendations() {
        const res = await fetch('/card/recomendtions');
        if (!res.ok) throw new Error('加载推荐失败');
        return res.json();
    }

    /* ========================
       Nordic Image Recognition Modal
       ======================== */
    let recogModalInitialized = false;
    let recogPreviewObjectUrl = null;

    function ensureRecognizeModal() {
        if (recogModalInitialized) return;
        recogModalInitialized = true;

        const backdrop = document.createElement('div');
        backdrop.id = 'recognizeModal';
        backdrop.className = 'modal-backdrop';
        backdrop.setAttribute('role', 'dialog');
        backdrop.setAttribute('aria-modal', 'true');
        backdrop.setAttribute('aria-labelledby', 'recogModalHeading');

        backdrop.innerHTML = `
            <div class="recog-modal-card">
                <div class="recog-modal-head">
                    <div>
                        <span class="label-text">Player Recognition · YuNet + SFace + OCR</span>
                        <h2 class="recog-modal-title" id="recogModalHeading">以图识别 LPL 选手</h2>
                    </div>
                    <button type="button" class="text-link" id="closeRecogModalBtn">关闭</button>
                </div>

                <div class="recog-layout">
                    <div>
                        <div class="recog-dropzone" id="recogDropzone" tabindex="0" role="button" aria-label="选择或拖入选手图片">
                            <div id="recogUploadPrompt">
                                <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#a89279" stroke-width="1.4" style="margin-bottom:8px;">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                                    <polyline points="17 8 12 3 7 8"/>
                                    <line x1="12" y1="3" x2="12" y2="15"/>
                                </svg>
                                <div style="font-size:14px;color:var(--color-charcoal);">点击选择、拖入或粘贴图片</div>
                                <div style="font-size:12px;color:var(--color-wood);margin-top:6px;">支持 JPG / PNG / WebP（10MB 以内）</div>
                            </div>
                            <div class="recog-preview-stage" id="recogPreviewStage" style="display:none;">
                                <div style="position:relative;display:inline-block;">
                                    <img id="recogPreviewImg" class="recog-preview-img" src="" alt="待识别图片预览">
                                    <div id="recogFaceBox" class="recog-face-box"></div>
                                </div>
                            </div>
                            <input type="file" id="recogFileInput" accept="image/*" style="display:none;">
                        </div>

                        <div class="recog-samples">
                            <span>样图试一试：</span>
                            <button type="button" class="text-link" data-sample="lpl卡片/BLG-BIN.png" data-name="BLG-BIN.png" style="font-size:12px;">BLG·BIN</button>
                            <button type="button" class="text-link" data-sample="lpl卡片/TES-JACKEYLOVE.png" data-name="TES-JACKEYLOVE.png" style="font-size:12px;">TES·JACKEYLOVE</button>
                            <button type="button" class="text-link" data-sample="lpl卡片/JDG-RULER.png" data-name="JDG-RULER.png" style="font-size:12px;">JDG·RULER</button>
                        </div>
                    </div>

                    <div class="recog-result-col" id="recogResultCol">
                        <div class="recog-status-line" id="recogServiceStatus">正在检查识别服务状态…</div>
                        <p style="color:var(--color-wood);font-size:14px;">
                            上传选手人像照、赛场截图或实体卡照片，系统将自动提取人脸 128 维特征向量并结合卡面文字检索当前 16 位 LPL 选手底库。
                        </p>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(backdrop);

        const dropzone = document.getElementById('recogDropzone');
        const fileInput = document.getElementById('recogFileInput');
        const closeBtn = document.getElementById('closeRecogModalBtn');

        function closeModal() {
            backdrop.classList.remove('open');
        }

        closeBtn.addEventListener('click', closeModal);
        backdrop.addEventListener('click', (e) => {
            if (e.target === backdrop) closeModal();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && backdrop.classList.contains('open')) closeModal();
        });

        dropzone.addEventListener('click', () => fileInput.click());
        dropzone.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                fileInput.click();
            }
        });

        fileInput.addEventListener('change', () => {
            if (fileInput.files && fileInput.files[0]) {
                runImageRecognition(fileInput.files[0]);
                fileInput.value = '';
            }
        });

        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });
        dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            const f = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
            if (f) runImageRecognition(f);
        });

        document.addEventListener('paste', (e) => {
            if (!backdrop.classList.contains('open')) return;
            const items = e.clipboardData && e.clipboardData.items;
            if (!items) return;
            for (const item of items) {
                if (item.type && item.type.startsWith('image/')) {
                    const blob = item.getAsFile();
                    if (blob) {
                        runImageRecognition(blob);
                        break;
                    }
                }
            }
        });

        backdrop.querySelectorAll('[data-sample]').forEach((btn) => {
            btn.addEventListener('click', async () => {
                const sampleUrl = btn.getAttribute('data-sample');
                const sampleName = btn.getAttribute('data-name') || 'sample.png';
                try {
                    const resp = await fetch(sampleUrl);
                    if (!resp.ok) throw new Error('无法读取样图');
                    const blob = await resp.blob();
                    const file = new File([blob], sampleName, { type: blob.type || 'image/png' });
                    runImageRecognition(file);
                } catch (err) {
                    showToast('加载样图失败', 'error');
                }
            });
        });
    }

    async function refreshRecognizeHealth() {
        const el = document.getElementById('recogServiceStatus');
        if (!el) return;
        try {
            const resp = await fetch('/api/recognize/health');
            const payload = await resp.json();
            const info = payload && payload.data ? payload.data : {};
            if (info.online) {
                el.textContent = `引擎就绪 · 已索引 ${info.playerCount || 16} 位 LPL 选手${info.ocrEnabled ? ' · 人脸 + OCR 双路' : ' · 人脸特征通路'}`;
                el.style.color = 'var(--color-moss)';
            } else {
                el.textContent = info.message || '识别服务未连接（8001 端口）';
                el.style.color = 'var(--color-wood)';
            }
        } catch (e) {
            el.textContent = '暂时无法检查识别服务状态';
        }
    }

    function openRecognizeModal() {
        ensureRecognizeModal();
        const backdrop = document.getElementById('recognizeModal');
        backdrop.classList.add('open');
        refreshRecognizeHealth();
    }

    async function runImageRecognition(file) {
        const promptEl = document.getElementById('recogUploadPrompt');
        const previewStage = document.getElementById('recogPreviewStage');
        const previewImg = document.getElementById('recogPreviewImg');
        const faceBoxEl = document.getElementById('recogFaceBox');
        const resultCol = document.getElementById('recogResultCol');

        if (recogPreviewObjectUrl) {
            URL.revokeObjectURL(recogPreviewObjectUrl);
        }
        recogPreviewObjectUrl = URL.createObjectURL(file);
        previewImg.src = recogPreviewObjectUrl;
        promptEl.style.display = 'none';
        previewStage.style.display = 'flex';
        faceBoxEl.style.display = 'none';

        resultCol.innerHTML = `
            <span class="label-text">Analyzing</span>
            <p style="font-size:16px;color:var(--color-charcoal);">正在提取人脸特征并比对底库…</p>
            <p style="font-size:13px;color:var(--color-wood);">依次执行 YuNet 人脸对齐、SFace 向量相似度计算与卡面文字核验。</p>
        `;

        try {
            const fd = new FormData();
            fd.append('file', file);
            const resp = await fetch('/api/recognize?topK=3', {
                method: 'POST',
                body: fd
            });
            const payload = await resp.json();

            if (!payload || payload.code !== 200 || !payload.data) {
                const errMsg = (payload && payload.message) || '识别请求失败';
                resultCol.innerHTML = `
                    <span class="label-text">Service Notice</span>
                    <p style="font-size:16px;color:var(--color-charcoal);">${escapeHtml(errMsg)}</p>
                    <p style="font-size:13px;color:var(--color-wood);">可检查图片格式或确认 Python 识别服务（8001 端口）已启动。</p>
                `;
                return;
            }

            renderRecognitionResult(payload.data);
        } catch (err) {
            console.error(err);
            resultCol.innerHTML = `
                <span class="label-text">Error</span>
                <p style="font-size:16px;color:var(--color-charcoal);">识别请求出错，请稍后重试。</p>
            `;
        }
    }

    function renderRecognitionResult(data) {
        const faceBoxEl = document.getElementById('recogFaceBox');
        const resultCol = document.getElementById('recogResultCol');

        if (data.faceDetected && Array.isArray(data.faceBox) && data.faceBox.length === 4) {
            const [x, y, w, h] = data.faceBox;
            faceBoxEl.style.left = `${(x * 100).toFixed(2)}%`;
            faceBoxEl.style.top = `${(y * 100).toFixed(2)}%`;
            faceBoxEl.style.width = `${(w * 100).toFixed(2)}%`;
            faceBoxEl.style.height = `${(h * 100).toFixed(2)}%`;
            faceBoxEl.style.display = 'block';
        } else {
            faceBoxEl.style.display = 'none';
        }

        const matches = Array.isArray(data.matches) ? data.matches : [];
        if (!data.recognized || !matches.length) {
            resultCol.innerHTML = `
                <span class="label-text">No Match · ${escapeHtml(data.elapsedMs || 0)} ms</span>
                <h3 style="font-size:20px;font-weight:300;color:var(--color-charcoal);">未能确认具体 LPL 选手</h3>
                <p style="font-size:14px;color:var(--color-wood);">${escapeHtml(data.message || '请尝试上传更清晰的选手正面照或实体卡照片。')}</p>
            `;
            return;
        }

        const top = matches[0];
        const card = top.card;
        const pct = Math.max(0, Math.min(99, Number(top.confidencePercent || 0)));
        const reasonsHtml = (top.matchReasons || [])
            .map(r => `<span class="recog-reason-tag">${escapeHtml(r)}</span>`)
            .join('');

        const cardBoxHtml = card ? `
            <div class="recog-card-match">
                <div class="card-art-frame recog-card-thumb">
                    <img src="${escapeHtml(getCardImageSrc(card.img_url))}" alt="${escapeHtml(card.playerName)}">
                </div>
                <div style="flex:1;min-width:0;">
                    <div class="label-text">${escapeHtml(card.team)} · ${escapeHtml(gradeLabel(card.grade))}</div>
                    <div style="font-size:16px;color:var(--color-charcoal);margin-top:4px;">${escapeHtml(card.playerName)} · ${escapeHtml(card.cardNumber || '')}</div>
                    <div class="tabular" style="font-size:18px;color:var(--color-charcoal);margin-top:6px;">¥ ${formatPrice(card.price)}</div>
                </div>
                <a href="detail.html?id=${encodeURIComponent(card.id)}" class="quiet-btn quiet-btn-sm">查看详情</a>
            </div>
        ` : '';

        const alts = matches.slice(1).filter(m => m.card);
        const altsHtml = alts.length ? `
            <div class="recog-alt-list">
                <span class="label-text">其他相近候选</span>
                ${alts.map(m => `
                    <a class="recog-alt-row" href="detail.html?id=${encodeURIComponent(m.card.id)}">
                        <span>${escapeHtml(m.team)} · ${escapeHtml(m.playerName)}</span>
                        <span style="color:var(--color-wood);font-size:12px;">匹配度 ${escapeHtml(m.confidencePercent)}% · ¥ ${formatPrice(m.card.price)}</span>
                    </a>
                `).join('')}
            </div>
        ` : '';

        resultCol.innerHTML = `
            <div>
                <span class="label-text">Matched · 耗时 ${escapeHtml(data.elapsedMs || 0)} ms</span>
                <h3 style="font-size:26px;font-weight:300;line-height:1.3;margin-top:6px;color:var(--color-charcoal);">
                    ${escapeHtml(top.team)} · ${escapeHtml(top.playerName)}
                </h3>
                <p style="font-size:13px;color:var(--color-wood);margin-top:4px;">
                    ${escapeHtml(getTeamFullName(top.team))} · 综合置信度 ${pct}%（人脸相似度 ${(Number(top.faceScore || 0) * 100).toFixed(1)}%）
                </p>
                <div class="recog-confidence-track">
                    <div class="recog-confidence-fill" id="recogConfBar"></div>
                </div>
                <div class="recog-reason-row">${reasonsHtml}</div>
            </div>
            ${cardBoxHtml}
            ${altsHtml}
        `;

        requestAnimationFrame(() => {
            const bar = document.getElementById('recogConfBar');
            if (bar) bar.style.width = `${pct}%`;
        });
    }

    window.LPLVault = {
        TEAM_NAMES,
        getTeamFullName,
        getUser,
        setUser,
        logout,
        getFavorites,
        isFavorite,
        toggleFavorite,
        getOrders,
        addOrder,
        formatPrice,
        formatSeriesText,
        getCardImageSrc,
        gradeLabel,
        showToast,
        escapeHtml,
        renderNavbar,
        renderFooter,
        createTradingCardElement,
        fetchCardList,
        fetchCardById,
        fetchRecommendations,
        openRecognizeModal
    };
})(window);
