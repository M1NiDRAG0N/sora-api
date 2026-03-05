/**
 * 일반 유틸리티 함수들
 */
const Utils = {
    /**
     * 문자열 트림 (공백 제거)
     * @param {string} str - 문자열
     * @returns {string} 트림된 문자열
     */
    trim: (str) => {
        return str ? str.trim() : '';
    },

    /**
     * 널 또는 undefined 체크
     * @param {*} value - 값
     * @returns {boolean}
     */
    isNull: (value) => {
        return value === null || value === undefined;
    },

    /**
     * 빈 객체 체크
     * @param {Object} obj - 객체
     * @returns {boolean}
     */
    isEmpty: (obj) => {
        return obj && Object.keys(obj).length === 0;
    },

    /**
     * 배열 필터링 (null/undefined 제거)
     * @param {Array} arr - 배열
     * @returns {Array} 필터링된 배열
     */
    filterEmpty: (arr) => {
        return arr ? arr.filter(item => item !== null && item !== undefined) : [];
    },

    /**
     * 딜레이 (밀리초)
     * @param {number} ms - 밀리초
     * @returns {Promise}
     */
    delay: (ms) => {
        return new Promise(resolve => setTimeout(resolve, ms));
    },

    /**
     * 숫자 포맷팅 (천 단위 쉼표)
     * @param {number} num - 숫자
     * @returns {string} 포맷된 문자열
     */
    formatNumber: (num) => {
        return num ? num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',') : '0';
    },

    /**
     * 날짜 포맷팅 (YYYY-MM-DD)
     * @param {Date} date - 날짜 객체
     * @returns {string} 포맷된 날짜
     */
    formatDate: (date) => {
        if (!date) return '';
        const d = new Date(date);
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${d.getFullYear()}-${month}-${day}`;
    },

    /**
     * 시간 포맷팅 (HH:MM:SS)
     * @param {Date} date - 날짜 객체
     * @returns {string} 포맷된 시간
     */
    formatTime: (date) => {
        if (!date) return '';
        const d = new Date(date);
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        const seconds = String(d.getSeconds()).padStart(2, '0');
        return `${hours}:${minutes}:${seconds}`;
    },

    /**
     * 객체를 쿼리 스트링으로 변환
     * @param {Object} obj - 객체
     * @returns {string} 쿼리 스트링
     */
    toQueryString: (obj) => {
        const params = new URLSearchParams(obj);
        return params.toString();
    },

    /**
     * 로컬스토리지 저장
     * @param {string} key - 키
     * @param {*} value - 값
     */
    setStorage: (key, value) => {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            console.error('로컬스토리지 저장 실패:', error);
        }
    },

    /**
     * 로컬스토리지 조회
     * @param {string} key - 키
     * @returns {*} 저장된 값
     */
    getStorage: (key) => {
        try {
            const value = localStorage.getItem(key);
            return value ? JSON.parse(value) : null;
        } catch (error) {
            console.error('로컬스토리지 조회 실패:', error);
            return null;
        }
    },

    /**
     * 로컬스토리지 삭제
     * @param {string} key - 키
     */
    removeStorage: (key) => {
        try {
            localStorage.removeItem(key);
        } catch (error) {
            console.error('로컬스토리지 삭제 실패:', error);
        }
    },

    /**
     * 커스텀 Toast 알림 표시
     * @param {string} message - 메시지
     * @param {string} type - 타입 (success, error, info, warning)
     * @param {number} duration - 표시 시간 (ms, 기본값 3000)
     */
    notify: (message, type = 'info', duration = 3000) => {
        // 애니메이션 정의 (최초 1회)
        if (!document.getElementById('toast-animation')) {
            const style = document.createElement('style');
            style.id = 'toast-animation';
            style.textContent = `
                @keyframes toastIn {
                    from { opacity: 0; transform: scale(0.85) translateX(20px); }
                    to   { opacity: 1; transform: scale(1) translateX(0); }
                }
                @keyframes toastOut {
                    from { opacity: 1; transform: scale(1) translateX(0); }
                    to   { opacity: 0; transform: scale(0.85) translateX(20px); }
                }
            `;
            document.head.appendChild(style);
        }

        // app-container 우측 경계 기준으로 위치 계산
        const appEl = document.querySelector('.app-container');
        const appRect = appEl ? appEl.getBoundingClientRect() : null;
        const rightOffset = appRect ? Math.max(20, window.innerWidth - appRect.right + 20) : 20;

        // Toast 컨테이너 찾기 또는 생성
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }
        container.style.cssText = `
            position: fixed;
            top: 20px;
            right: ${rightOffset}px;
            z-index: 10000;
            display: flex;
            flex-direction: column;
            align-items: flex-end;
            gap: 10px;
            pointer-events: none;
        `;

        // 타입별 색상 지정
        const colors = {
            success: { bg: '#10b981', icon: '✓' },
            error:   { bg: '#ef4444', icon: '✕' },
            like:    { bg: '#ff72e5', icon: '✕' },
            info:    { bg: '#3b82f6', icon: 'ℹ' },
            warning: { bg: '#f59e0b', icon: '⚠' }
        };
        const color = colors[type] || colors.info;

        // Toast 요소 생성
        const toast = document.createElement('div');
        toast.style.cssText = `
            background: ${color.bg};
            color: white;
            padding: 14px 20px;
            border-radius: 12px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.25);
            font-size: 15px;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 10px;
            white-space: nowrap;
            animation: toastIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
            pointer-events: auto;
        `;
        toast.innerHTML = `<span style="font-weight:bold;font-size:18px;">${color.icon}</span><span>${message}</span>`;
        container.appendChild(toast);

        // 자동 제거
        setTimeout(() => {
            toast.style.animation = 'toastOut 0.2s ease-in forwards';
            setTimeout(() => container.remove(), 200);
        }, duration);
    },

    _loadingCount: 0,

    showLoading: () => {
        Utils._loadingCount++;
        let overlay = document.getElementById('loading-overlay');
        if (!overlay) {
            if (!document.getElementById('loading-style')) {
                const style = document.createElement('style');
                style.id = 'loading-style';
                style.textContent = `
                    @keyframes spin {
                        to { transform: rotate(360deg); }
                    }
                    #loading-overlay .spinner {
                        width: 48px;
                        height: 48px;
                        border: 4px solid rgba(255,255,255,0.35);
                        border-top-color: #fff;
                        border-radius: 50%;
                        animation: spin 0.75s linear infinite;
                    }
                `;
                document.head.appendChild(style);
            }
            overlay = document.createElement('div');
            overlay.id = 'loading-overlay';
            overlay.style.cssText = `
                position: fixed;
                inset: 0;
                background: rgba(0,0,0,0.35);
                z-index: 9998;
                display: flex;
                align-items: center;
                justify-content: center;
            `;
            overlay.innerHTML = '<div class="spinner"></div>';
            document.body.appendChild(overlay);
        }
        overlay.style.display = 'flex';
    },

    hideLoading: () => {
        Utils._loadingCount = Math.max(0, Utils._loadingCount - 1);
        if (Utils._loadingCount === 0) {
            const overlay = document.getElementById('loading-overlay');
            if (overlay) overlay.style.display = 'none';
        }
    },

    /**
     * 카카오톡 스타일 채팅 목록 시간 표시
     * - 오늘: 오전/오후 H:MM
     * - 어제: 어제
     * - 이번 주: 요일 (월요일)
     * - 올해: M월 D일
     * - 이전 연도: YYYY년 M월 D일
     * @param {string|Array} dateString - 날짜
     * @returns {string}
     */
    chatTime: (dateString) => {
        if (!dateString) return '';

        let date;
        if (Array.isArray(dateString)) {
            const [y, m, d, h=0, min=0, s=0] = dateString;
            date = new Date(y, m - 1, d, h, min, s);
        } else {
            let safeStr = String(dateString).replace('T', ' ').split('.')[0].replace(/-/g, '/');
            date = new Date(safeStr);
            if (isNaN(date.getTime())) date = new Date(dateString);
        }
        if (isNaN(date.getTime())) return '';

        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const target = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        const diffDays = Math.floor((today - target) / 86400000);

        if (diffDays === 0) {
            const h = date.getHours();
            const m = String(date.getMinutes()).padStart(2, '0');
            return `${h < 12 ? '오전' : '오후'} ${h === 0 ? 12 : h > 12 ? h - 12 : h}:${m}`;
        }
        if (diffDays === 1) return '어제';
        if (diffDays < 7) {
            const days = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
            return days[date.getDay()];
        }
        if (date.getFullYear() === now.getFullYear()) {
            return `${date.getMonth() + 1}월 ${date.getDate()}일`;
        }
        return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
    },

    /**
     * N시간 전, N일 전 등 반환 (Java 로직 JS로 포팅)
     * @param {string|Date|Array} dateString - 날짜 문자열 (ex: '2026-02-11T14:30:00')
     * @returns {string} 방금 전, N분 전 등
     */
    timeAgo: (dateString) => {
        if (!dateString) return '';

        let date;
        if (Array.isArray(dateString)) {
            const [y, m, d, h=0, min=0, s=0] = dateString;
            date = new Date(y, m - 1, d, h, min, s);
        } else if (typeof dateString === 'string') {
            // 브라우저 호환성을 위해 'T'를 ' '로 변경하고 소수점 이하 밀리초 제거, '-'를 '/'로 변경
            let safeStr = dateString.replace('T', ' ').split('.')[0].replace(/-/g, '/');
            date = new Date(safeStr);
            if (isNaN(date.getTime())) {
                date = new Date(dateString); // 원본 문자열로 재시도
            }
        } else {
            date = new Date(dateString);
        }

        if (isNaN(date.getTime())) return String(dateString); // 유효하지 않은 날짜면 원본 문자열 반환

        const now = new Date();
        let seconds = Math.floor((now.getTime() - date.getTime()) / 1000);
        if (seconds < 0) seconds = 0; // 서버 시간이 조금 빠를 경우 대비

        if (seconds < 60) return "방금 전";

        const minutes = Math.floor(seconds / 60);
        if (minutes < 60) return `${minutes}분 전`;

        const hours = Math.floor(minutes / 60);
        if (hours < 24) return `${hours}시간 전`;

        const days = Math.floor(hours / 24);
        if (days < 7) return `${days}일 전`;

        // 7일 넘어가면 기존 Utils.formatDate 재사용해서 'YYYY-MM-DD' 반환
        return Utils.formatDate(date);
    }
};
