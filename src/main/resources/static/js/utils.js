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
        // Toast 컨테이너 찾기 또는 생성
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 10000;
                display: flex;
                flex-direction: column;
                gap: 10px;
                pointer-events: none;
            `;
            document.body.appendChild(container);
        }

        // Toast 요소 생성
        const toast = document.createElement('div');

        // 타입별 색상 지정
        const colors = {
            success: { bg: '#10b981', icon: '✓' },
            error: { bg: '#ef4444', icon: '✕' },
            info: { bg: '#3b82f6', icon: 'ℹ' },
            warning: { bg: '#f59e0b', icon: '⚠' }
        };

        const color = colors[type] || colors.info;

        toast.style.cssText = `
            background: ${color.bg};
            color: white;
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            font-size: 14px;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 10px;
            animation: slideIn 0.3s ease-out;
            pointer-events: auto;
            max-width: 350px;
            word-break: break-word;
        `;

        toast.innerHTML = `<span style="font-weight: bold; font-size: 18px;">${color.icon}</span><span>${message}</span>`;

        container.appendChild(toast);

        // 애니메이션 정의
        if (!document.getElementById('toast-animation')) {
            const style = document.createElement('style');
            style.id = 'toast-animation';
            style.textContent = `
                @keyframes slideIn {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
                @keyframes slideOut {
                    from {
                        transform: translateX(0);
                        opacity: 1;
                    }
                    to {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                }
            `;
            document.head.appendChild(style);
        }

        // 자동 제거
        setTimeout(() => {
            toast.style.animation = 'slideOut 0.3s ease-out';
            setTimeout(() => {
                toast.remove();
            }, 300);
        }, duration);
    },

    /**
     * N시간 전, N일 전 등 반환 (Java 로직 JS로 포팅)
     * @param {string|Date} dateString - 날짜 문자열 (ex: '2026-02-11T14:30:00')
     * @returns {string} 방금 전, N분 전 등
     */
    timeAgo: (dateString) => {
        if (!dateString) return '';

        const date = new Date(dateString);
        const now = new Date();
        const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

        if (seconds < 60) return "방금 전";

        const minutes = Math.floor(seconds / 60);
        if (minutes < 60) return `${minutes}분 전`;

        const hours = Math.floor(minutes / 60);
        if (hours < 24) return `${hours}시간 전`;

        const days = Math.floor(hours / 24);
        if (days < 30) return `${days}일 전`;

        // 30일 넘어가면 기존 Utils.formatDate 재사용해서 'YYYY-MM-DD' 반환
        return Utils.formatDate(date);
    }
};
