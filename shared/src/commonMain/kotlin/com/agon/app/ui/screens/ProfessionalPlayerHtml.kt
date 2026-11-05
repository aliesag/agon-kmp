package com.agon.app.ui.screens

import com.agon.app.data.VideoServer

fun buildProfessionalPlayerHtml(
    streamUrl: String,
    title: String = "",
    servers: List<VideoServer> = emptyList(),
    currentServerIndex: Int = 0
): String {
    val serversJson = servers.mapIndexed { idx, s ->
        val safeName = s.name.replace("\"", "\\\"")
        val safeUrl = s.url.replace("\"", "\\\"")
        """{"index":$idx,"name":"$safeName","url":"$safeUrl"}"""
    }.joinToString(prefix = "[", postfix = "]", separator = ",")

    val safeTitle = title.replace("\"", "\\\"").ifEmpty { "مشاهدة الحلقة" }

    return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="unsafe-url">
            <meta name="referrer" content="always">
            <title>مشغل البث الاحترافي</title>
            <script>window.isInternalAgonPlayer = true;</script>
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    user-select: none;
                    -webkit-user-select: none;
                    -webkit-touch-callout: none;
                }
                body, html {
                    width: 100%;
                    height: 100%;
                    min-height: 100%;
                    margin: 0;
                    padding: 0;
                    background: #000;
                    overflow: hidden;
                    position: relative;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                }
                #playerContainer {
                    position: relative;
                    width: 100%;
                    height: 100%;
                    background: #000;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    overflow: hidden;
                    margin: auto;
                }
                video {
                    width: 100%;
                    height: 100%;
                    max-width: 100%;
                    max-height: 100%;
                    object-fit: contain;
                    object-position: center center;
                    background: #000;
                    margin: auto;
                    display: block;
                }
                .overlay {
                    position: absolute;
                    top: 0; left: 0; right: 0; bottom: 0;
                    width: 100%;
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    background: rgba(0, 0, 0, 0.4);
                    transition: opacity 0.3s ease, visibility 0.3s ease;
                    z-index: 10;
                    pointer-events: auto;
                }
                .overlay.hidden {
                    opacity: 0;
                    visibility: hidden;
                    pointer-events: none;
                }
                .top-bar {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    padding: 10px 16px;
                    background: linear-gradient(180deg, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0) 100%);
                    z-index: 15;
                }
                .top-left, .top-right {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                }
                .title-badge-group {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                }
                .video-title {
                    color: #fff;
                    font-size: 15px;
                    font-weight: 700;
                    max-width: 280px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
                .badge-fhd {
                    background: #E50914;
                    color: #fff;
                    font-size: 10px;
                    font-weight: 800;
                    padding: 2px 7px;
                    border-radius: 4px;
                    letter-spacing: 0.5px;
                }
                .center-controls {
                    position: absolute !important;
                    top: 50% !important;
                    left: 50% !important;
                    transform: translate(-50%, -50%) !important;
                    -webkit-transform: translate(-50%, -50%) !important;
                    display: flex !important;
                    align-items: center !important;
                    justify-content: center !important;
                    gap: 32px !important;
                    pointer-events: none !important;
                    z-index: 25 !important;
                    margin: 0 !important;
                }
                .center-controls button {
                    pointer-events: auto;
                }
                .btn-icon {
                    background: rgba(20, 20, 24, 0.7);
                    border: 1px solid rgba(255, 255, 255, 0.15);
                    color: #fff;
                    border-radius: 50%;
                    width: 44px;
                    height: 44px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;
                    outline: none;
                    transition: transform 0.15s ease, background 0.2s ease, border-color 0.2s ease;
                    backdrop-filter: blur(8px);
                }
                .btn-icon:active {
                    transform: scale(0.92);
                    background: rgba(229, 9, 20, 0.8);
                }
                .btn-pill {
                    background: rgba(20, 20, 24, 0.75);
                    border: 1px solid rgba(255, 255, 255, 0.18);
                    color: #fff;
                    border-radius: 20px;
                    padding: 6px 14px;
                    font-size: 12px;
                    font-weight: 600;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    cursor: pointer;
                    outline: none;
                    backdrop-filter: blur(8px);
                    transition: background 0.2s, transform 0.15s;
                }
                .btn-pill:active {
                    transform: scale(0.95);
                    background: #E50914;
                }
                .btn-play-pause {
                    width: 68px;
                    height: 68px;
                    background: rgba(229, 9, 20, 0.9);
                    border: 2px solid rgba(255, 255, 255, 0.4);
                    box-shadow: 0 0 24px rgba(229, 9, 20, 0.5);
                }
                .btn-play-pause:active {
                    transform: scale(0.9);
                    background: #ff1f2d;
                }
                .btn-seek {
                    width: 52px;
                    height: 52px;
                    position: relative;
                }
                .seek-label {
                    position: absolute;
                    font-size: 10px;
                    font-weight: 800;
                    top: 19px;
                    color: #fff;
                }
                .bottom-bar {
                    display: flex;
                    flex-direction: column;
                    padding: 12px 20px 16px;
                    background: linear-gradient(0deg, rgba(0,0,0,0.92) 0%, rgba(0,0,0,0) 100%);
                    gap: 8px;
                }
                .progress-container {
                    position: relative;
                    width: 100%;
                    height: 20px;
                    display: flex;
                    align-items: center;
                    cursor: pointer;
                    touch-action: none;
                }
                .progress-track {
                    position: relative;
                    width: 100%;
                    height: 5px;
                    background: rgba(255, 255, 255, 0.25);
                    border-radius: 3px;
                    overflow: visible;
                }
                .buffer-bar {
                    position: absolute;
                    top: 0; left: 0;
                    height: 100%;
                    background: rgba(255, 255, 255, 0.45);
                    border-radius: 3px;
                    width: 0%;
                }
                .played-bar {
                    position: absolute;
                    top: 0; left: 0;
                    height: 100%;
                    background: #E50914;
                    border-radius: 3px;
                    width: 0%;
                }
                .scrubber-thumb {
                    position: absolute;
                    top: 50%;
                    left: 0%;
                    transform: translate(-50%, -50%);
                    width: 15px;
                    height: 15px;
                    background: #fff;
                    border: 2px solid #E50914;
                    border-radius: 50%;
                    box-shadow: 0 0 8px rgba(229, 9, 20, 0.8);
                    pointer-events: none;
                }
                .bottom-row {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }
                .time-box {
                    color: #fff;
                    font-size: 13px;
                    font-weight: 600;
                    letter-spacing: 0.5px;
                    direction: ltr;
                }
                .bottom-actions {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                }
                .ripple-badge {
                    position: absolute;
                    top: 50%;
                    transform: translateY(-50%);
                    background: rgba(0, 0, 0, 0.8);
                    border: 1px solid rgba(255, 255, 255, 0.3);
                    color: #fff;
                    padding: 10px 18px;
                    border-radius: 24px;
                    font-size: 14px;
                    font-weight: 700;
                    pointer-events: none;
                    opacity: 0;
                    transition: opacity 0.25s ease, transform 0.25s ease;
                    z-index: 25;
                }
                .ripple-badge.left {
                    left: 40px;
                }
                .ripple-badge.right {
                    right: 40px;
                }
                .ripple-badge.active {
                    opacity: 1;
                    transform: translateY(-50%) scale(1.08);
                }
                /* next-ep-card CSS removed */
                .servers-modal {
                    position: absolute;
                    bottom: 0; left: 0; right: 0;
                    background: rgba(18, 18, 22, 0.96);
                    border-top: 1px solid rgba(255, 255, 255, 0.15);
                    border-radius: 18px 18px 0 0;
                    padding: 18px 20px;
                    display: flex;
                    flex-direction: column;
                    gap: 14px;
                    z-index: 30;
                    backdrop-filter: blur(20px);
                    transform: translateY(105%);
                    transition: transform 0.3s ease;
                }
                .servers-modal.open {
                    transform: translateY(0);
                }
                .modal-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }
                .modal-title {
                    color: #fff;
                    font-size: 15px;
                    font-weight: 700;
                }
                .modal-close {
                    background: transparent;
                    border: none;
                    color: #fff;
                    font-size: 18px;
                    cursor: pointer;
                }
                .servers-grid {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    max-height: 200px;
                    overflow-y: auto;
                }
                .server-chip {
                    background: rgba(36, 36, 42, 0.85);
                    border: 1px solid rgba(255, 255, 255, 0.12);
                    color: #fff;
                    border-radius: 8px;
                    padding: 9px 14px;
                    font-size: 12px;
                    font-weight: 600;
                    cursor: pointer;
                    transition: background 0.15s, border-color 0.15s;
                }
                .server-chip.selected {
                    background: #E50914;
                    border-color: #fff;
                    font-weight: 800;
                }
                .unlock-overlay {
                    position: absolute;
                    top: 16px;
                    right: 16px;
                    z-index: 40;
                    display: none;
                }
                .unlock-overlay.visible {
                    display: block;
                }
                .player-toast {
                    position: absolute;
                    top: 70px;
                    left: 50%;
                    transform: translateX(-50%);
                    background: rgba(20, 20, 24, 0.9);
                    border: 1px solid rgba(255, 255, 255, 0.2);
                    color: #fff;
                    padding: 8px 16px;
                    border-radius: 20px;
                    font-size: 12px;
                    font-weight: 600;
                    pointer-events: none;
                    opacity: 0;
                    transition: opacity 0.25s ease;
                    z-index: 35;
                    backdrop-filter: blur(8px);
                }
                .player-toast.show {
                    opacity: 1;
                }
                svg {
                    width: 22px;
                    height: 22px;
                    fill: currentColor;
                }
            </style>
        </head>
        <body>
            <div id="playerContainer">
                <video id="v" playsinline webkit-playsinline autoplay preload="auto" controlslist="nodownload" src="$streamUrl"></video>

                <!-- Double Tap Visual Ripple Badges -->
                <div id="rippleLeft" class="ripple-badge left">« 10 ثوانٍ</div>
                <div id="rippleRight" class="ripple-badge right">10 ثوانٍ »</div>

                <!-- Toast -->
                <div id="toast" class="player-toast"></div>

                <!-- Unlock button (shown only when locked) -->
                <div id="unlockOverlay" class="unlock-overlay">
                    <button id="unlockBtn" class="btn-pill" style="background:#E50914;">
                        <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h2c0-1.66 1.34-3 3-3s3 1.34 3 3v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10zm-6-3c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z"/></svg>
                        <span>إلغاء القفل</span>
                    </button>
                </div>

                <!-- Next Episode Floating Card removed -->

                <!-- Controls Overlay -->
                <div id="overlay" class="overlay">
                    <!-- Top Bar -->
                    <div class="top-bar">
                        <div class="top-left">
                            <button id="btnBack" class="btn-icon">
                                <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                            </button>
                            <div class="title-badge-group">
                                <span class="video-title">$safeTitle</span>
                                <span class="badge-fhd">FHD 1080p</span>
                            </div>
                        </div>
                        <div class="top-right">
                            <button id="btnOpenServers" class="btn-pill">
                                <svg viewBox="0 0 24 24"><path d="M19 15v4H5v-4h14m1-2H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zm-1-9v4H5V4h14m1-2H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1V3c0-.55-.45-1-1-1z"/></svg>
                                <span>السيرفرات</span>
                            </button>
                            <button id="btnAspect" class="btn-pill">
                                <svg viewBox="0 0 24 24"><path d="M19 12h-2v3h-3v2h5v-5zM7 9h3V7H5v5h2V9zm14-6H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16.01H3V4.99h18v14.02z"/></svg>
                                <span id="aspectLabel">تناسب</span>
                            </button>
                            <button id="btnSpeed" class="btn-pill">
                                <span id="speedLabel">1.0x</span>
                            </button>
                            <button id="btnLock" class="btn-icon">
                                <svg viewBox="0 0 24 24"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
                            </button>
                        </div>
                    </div>

                    <!-- Center Controls -->
                    <div class="center-controls">
                        <button id="btnRewind" class="btn-icon btn-seek">
                            <svg viewBox="0 0 24 24"><path d="M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z"/></svg>
                            <span class="seek-label">10-</span>
                        </button>
                        <button id="btnPlayPause" class="btn-icon btn-play-pause">
                            <svg id="playIcon" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                            <svg id="pauseIcon" viewBox="0 0 24 24" style="display:none;"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                        </button>
                        <button id="btnForward" class="btn-icon btn-seek">
                            <svg viewBox="0 0 24 24"><path d="M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z"/></svg>
                            <span class="seek-label">10+</span>
                        </button>
                    </div>

                    <!-- Bottom Bar -->
                    <div class="bottom-bar">
                        <!-- Progress Bar -->
                        <div id="progressContainer" class="progress-container">
                            <div class="progress-track">
                                <div id="bufferBar" class="buffer-bar"></div>
                                <div id="playedBar" class="played-bar"></div>
                                <div id="scrubberThumb" class="scrubber-thumb"></div>
                            </div>
                        </div>

                        <!-- Bottom Row -->
                        <div class="bottom-row">
                            <div class="time-box">
                                <span id="currentTime">00:00</span> / <span id="durationTime">00:00</span>
                            </div>
                            <div class="bottom-actions">
                                <button id="btnNextEp" class="btn-pill">
                                    <svg viewBox="0 0 24 24"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
                                    <span>الحلقة التالية</span>
                                </button>
                                <button id="btnEpisodes" class="btn-pill">
                                    <svg viewBox="0 0 24 24"><path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/></svg>
                                    <span>الحلقات</span>
                                </button>
                                <button id="btnFullscreen" class="btn-icon">
                                    <svg viewBox="0 0 24 24"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/></svg>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Servers Selection Sheet Modal -->
                <div id="serversModal" class="servers-modal">
                    <div class="modal-header">
                        <span class="modal-title">⚡ اختر سيرفر البث</span>
                        <button id="btnCloseServers" class="modal-close">✕</button>
                    </div>
                    <div id="serversGrid" class="servers-grid"></div>
                </div>
            </div>

            <script>
                const v = document.getElementById('v');
                const overlay = document.getElementById('overlay');
                const btnPlayPause = document.getElementById('btnPlayPause');
                const playIcon = document.getElementById('playIcon');
                const pauseIcon = document.getElementById('pauseIcon');
                const btnRewind = document.getElementById('btnRewind');
                const btnForward = document.getElementById('btnForward');
                const btnBack = document.getElementById('btnBack');
                const btnAspect = document.getElementById('btnAspect');
                const aspectLabel = document.getElementById('aspectLabel');
                const btnSpeed = document.getElementById('btnSpeed');
                const speedLabel = document.getElementById('speedLabel');
                const btnLock = document.getElementById('btnLock');
                const unlockOverlay = document.getElementById('unlockOverlay');
                const unlockBtn = document.getElementById('unlockBtn');
                const btnOpenServers = document.getElementById('btnOpenServers');
                const btnCloseServers = document.getElementById('btnCloseServers');
                const serversModal = document.getElementById('serversModal');
                const serversGrid = document.getElementById('serversGrid');
                const btnFullscreen = document.getElementById('btnFullscreen');
                const btnNextEp = document.getElementById('btnNextEp');
                const btnEpisodes = document.getElementById('btnEpisodes');
                const progressContainer = document.getElementById('progressContainer');
                const bufferBar = document.getElementById('bufferBar');
                const scrubberThumb = document.getElementById('scrubberThumb');
                const currentTimeEl = document.getElementById('currentTime');
                const durationTimeEl = document.getElementById('durationTime');
                const toast = document.getElementById('toast');
                const rippleLeft = document.getElementById('rippleLeft');
                const rippleRight = document.getElementById('rippleRight');

                const servers = $serversJson;
                let currentServerIndex = $currentServerIndex;

                let isLocked = false;
                let hideTimeout = null;
                let lastTapTime = 0;
                let tapCount = 0;

                function showToast(msg) {
                    toast.textContent = msg;
                    toast.classList.add('show');
                    setTimeout(() => toast.classList.remove('show'), 2200);
                }

                function formatTime(sec) {
                    if (isNaN(sec) || sec < 0) return "00:00";
                    sec = Math.floor(sec);
                    const h = Math.floor(sec / 3600);
                    const m = Math.floor((sec % 3600) / 60);
                    const s = sec % 60;
                    if (h > 0) {
                        return (h < 10 ? "0" + h : h) + ":" + (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
                    }
                    return (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
                }

                function resetHideTimer() {
                    clearTimeout(hideTimeout);
                    overlay.classList.remove('hidden');
                    if (!v.paused && !isLocked) {
                        hideTimeout = setTimeout(() => {
                            if (!v.paused && !isLocked && !serversModal.classList.contains('open')) {
                                overlay.classList.add('hidden');
                            }
                        }, 3500);
                    }
                }

                function togglePlay() {
                    if (v.paused) {
                        v.play().catch(() => {});
                    } else {
                        v.pause();
                    }
                    resetHideTimer();
                }

                btnPlayPause.addEventListener('click', (e) => { e.stopPropagation(); togglePlay(); });
                v.addEventListener('play', () => {
                    playIcon.style.display = 'none';
                    pauseIcon.style.display = 'block';
                    resetHideTimer();
                    if (window.AndroidNativePlayer && window.AndroidNativePlayer.onDirectPlayerReady) {
                        window.AndroidNativePlayer.onDirectPlayerReady();
                    }
                });
                v.addEventListener('pause', () => {
                    playIcon.style.display = 'block';
                    pauseIcon.style.display = 'none';
                    resetHideTimer();
                });

                btnRewind.addEventListener('click', (e) => {
                    e.stopPropagation();
                    v.currentTime = Math.max(0, v.currentTime - 10);
                    resetHideTimer();
                });
                btnForward.addEventListener('click', (e) => {
                    e.stopPropagation();
                    v.currentTime = Math.min(v.duration || 0, v.currentTime + 10);
                    resetHideTimer();
                });

                document.getElementById('playerContainer').addEventListener('click', (e) => {
                    if (isLocked) return;
                    if (serversModal.classList.contains('open')) {
                        serversModal.classList.remove('open');
                        resetHideTimer();
                        return;
                    }
                    const now = Date.now();
                    const diff = now - lastTapTime;
                    const rect = e.currentTarget.getBoundingClientRect();
                    const clickX = e.clientX - rect.left;

                    if (diff < 300) {
                        tapCount = 0;
                        if (clickX < rect.width / 2) {
                            v.currentTime = Math.max(0, v.currentTime - 10);
                            rippleLeft.classList.add('active');
                            setTimeout(() => rippleLeft.classList.remove('active'), 600);
                        } else {
                            v.currentTime = Math.min(v.duration || 0, v.currentTime + 10);
                            rippleRight.classList.add('active');
                            setTimeout(() => rippleRight.classList.remove('active'), 600);
                        }
                    } else {
                        if (overlay.classList.contains('hidden')) {
                            resetHideTimer();
                        } else {
                            overlay.classList.add('hidden');
                        }
                    }
                    lastTapTime = now;
                });

                overlay.addEventListener('click', (e) => {
                    if (e.target === overlay) {
                        overlay.classList.add('hidden');
                    } else {
                        resetHideTimer();
                    }
                });

                v.addEventListener('timeupdate', () => {
                    if (v.duration) {
                        const pct = (v.currentTime / v.duration) * 100;
                        playedBar.style.width = pct + '%';
                        scrubberThumb.style.left = pct + '%';
                        currentTimeEl.textContent = formatTime(v.currentTime);
                        durationTimeEl.textContent = formatTime(v.duration);

                        if (v.buffered.length > 0) {
                            const bufPct = (v.buffered.end(v.buffered.length - 1) / v.duration) * 100;
                            bufferBar.style.width = bufPct + '%';
                        }

                        // Removed nextEpCard logic
                    }
                });

                let isDragging = false;
                function seekToPosition(e) {
                    const rect = progressContainer.getBoundingClientRect();
                    const clientX = e.touches ? e.touches[0].clientX : e.clientX;
                    const pos = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
                    if (v.duration) {
                        v.currentTime = pos * v.duration;
                    }
                }
                progressContainer.addEventListener('pointerdown', (e) => {
                    e.stopPropagation();
                    isDragging = true;
                    seekToPosition(e);
                });
                window.addEventListener('pointermove', (e) => {
                    if (isDragging) {
                        e.preventDefault();
                        seekToPosition(e);
                    }
                });
                window.addEventListener('pointerup', () => { isDragging = false; });

                const aspectModes = ['contain', 'cover', 'fill'];
                const aspectLabels = ['تناسب', 'ملء الشاشة', '16:9'];
                let aspectIdx = 0;
                btnAspect.addEventListener('click', (e) => {
                    e.stopPropagation();
                    aspectIdx = (aspectIdx + 1) % aspectModes.length;
                    v.style.objectFit = aspectModes[aspectIdx];
                    aspectLabel.textContent = aspectLabels[aspectIdx];
                    showToast('أبعاد العرض: ' + aspectLabels[aspectIdx]);
                    resetHideTimer();
                });

                const speeds = [1.0, 1.25, 1.5, 2.0, 0.75];
                let speedIdx = 0;
                btnSpeed.addEventListener('click', (e) => {
                    e.stopPropagation();
                    speedIdx = (speedIdx + 1) % speeds.length;
                    v.playbackRate = speeds[speedIdx];
                    speedLabel.textContent = speeds[speedIdx] + 'x';
                    showToast('سرعة التشغيل: ' + speeds[speedIdx] + 'x');
                    resetHideTimer();
                });

                btnLock.addEventListener('click', (e) => {
                    e.stopPropagation();
                    isLocked = true;
                    overlay.classList.add('hidden');
                    unlockOverlay.classList.add('visible');
                    showToast('تم قفل الشاشة 🔒');
                });
                unlockBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    isLocked = false;
                    unlockOverlay.classList.remove('visible');
                    resetHideTimer();
                    showToast('تم إلغاء القفل 🔓');
                });

                function renderServers() {
                    serversGrid.innerHTML = '';
                    if (!servers || servers.length === 0) {
                        serversGrid.innerHTML = '<span style="color:#aaa;font-size:12px;">سيرفر البث الحالي يعمل بشكل ممتاز</span>';
                        return;
                    }
                    servers.forEach((s) => {
                        const chip = document.createElement('button');
                        chip.className = 'server-chip' + (s.index === currentServerIndex ? ' selected' : '');
                        chip.textContent = s.name;
                        chip.addEventListener('click', () => {
                            currentServerIndex = s.index;
                            serversModal.classList.remove('open');
                            showToast('جاري التبديل إلى: ' + s.name + ' ⚡');
                            if (window.AndroidNativePlayer && window.AndroidNativePlayer.selectServerIndex) {
                                window.AndroidNativePlayer.selectServerIndex(s.index);
                            }
                        });
                        serversGrid.appendChild(chip);
                    });
                }
                renderServers();

                btnOpenServers.addEventListener('click', (e) => {
                    e.stopPropagation();
                    serversModal.classList.toggle('open');
                    resetHideTimer();
                });
                btnCloseServers.addEventListener('click', (e) => {
                    e.stopPropagation();
                    serversModal.classList.remove('open');
                    resetHideTimer();
                });

                btnBack.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (window.AndroidNativePlayer && window.AndroidNativePlayer.goBack) {
                        window.AndroidNativePlayer.goBack();
                    }
                });
                btnFullscreen.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (window.AndroidNativePlayer && window.AndroidNativePlayer.toggleFullScreen) {
                        window.AndroidNativePlayer.toggleFullScreen();
                    }
                });
                btnEpisodes.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (window.AndroidNativePlayer && window.AndroidNativePlayer.openEpisodesList) {
                        window.AndroidNativePlayer.openEpisodesList();
                    }
                });
                btnNextEp.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (window.AndroidNativePlayer && window.AndroidNativePlayer.playNextEpisode) {
                        window.AndroidNativePlayer.playNextEpisode();
                    }
                });
                // Removed btnPlayNextNow logic

                v.addEventListener('ended', () => {
                    showToast('جاري تشغيل الحلقة التالية...');
                    setTimeout(() => {
                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.playNextEpisode) {
                            window.AndroidNativePlayer.playNextEpisode();
                        }
                    }, 1000);
                });

                var hasHandledError = false;
                function handleSourceError() {
                    if (hasHandledError) return;
                    hasHandledError = true;
                    showToast('⚠️ تعذر تشغيل السيرفر، جاري المحاولة على سيرفر بديل...');
                    setTimeout(() => {
                        if (window.AndroidNativePlayer && window.AndroidNativePlayer.onServerError) {
                            window.AndroidNativePlayer.onServerError();
                        }
                    }, 1200);
                }

                v.addEventListener('error', function() {
                    if (v.error && v.error.code !== 0) {
                        handleSourceError();
                    }
                });

                document.addEventListener('keydown', (e) => {
                    resetHideTimer();
                    switch (e.keyCode) {
                        case 23:
                        case 66:
                        case 32:
                            togglePlay();
                            break;
                        case 21:
                        case 37:
                            v.currentTime = Math.max(0, v.currentTime - 10);
                            break;
                        case 22:
                        case 39:
                            v.currentTime = Math.min(v.duration || 0, v.currentTime + 10);
                            break;
                        case 19:
                            v.volume = Math.min(1, v.volume + 0.1);
                            showToast('مستوى الصوت: ' + Math.round(v.volume * 100) + '%');
                            break;
                        case 20:
                            v.volume = Math.max(0, v.volume - 0.1);
                            showToast('مستوى الصوت: ' + Math.round(v.volume * 100) + '%');
                            break;
                    }
                });

                if (!v.currentSrc && !v.src) {
                    v.src = '$streamUrl';
                }
                v.play().catch(() => {});
                resetHideTimer();
            </script>
        </body>
        </html>
    """.trimIndent()
}
