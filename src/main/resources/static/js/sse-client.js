/**
 * SSE(Server-Sent Events) 클라이언트
 * 서버의 실시간 알림을 수신하는 기능
 */
const SSEClient = {
    eventSource: null,
    userId: null,
    connectRetries: 0,
    maxRetries: 5,
    retryDelay: 3000, // 3초

    /**
     * SSE 연결 초기화
     * @param {number} userId - 사용자 ID
     */
    init: function(userId) {
        this.userId = userId;
        console.log('[SSE] 초기화 시작 - 사용자:', userId);
        
        // 이미 연결이 있으면 종료
        if (this.eventSource) {
            this.disconnect();
        }
        
        this.connect();
    },

    /**
     * SSE 연결
     */
    connect: function() {
        if (!this.userId) {
            console.error('[SSE] 사용자 ID가 없습니다');
            return;
        }

        console.log('[SSE] 연결 중...');
        
        try {
            // SSE 구독 엔드포인트에 연결
            this.eventSource = new EventSource(`/notifications/subscribe/${this.userId}`);
            
            // 연결 성공 (초기 이벤트 수신)
            this.eventSource.addEventListener('connected', (event) => {
                console.log('[SSE] 연결 성공:', event.data);
                this.connectRetries = 0; // 재시도 횟수 초기화
            });
            
            // 알림 수신
            this.eventSource.addEventListener('notification', (event) => {
                this.handleNotification(event);
            });
            
            // 에러 처리
            this.eventSource.onerror = (event) => {
                console.error('[SSE] 에러 발생:', event);
                this.handleError();
            };
            
        } catch (error) {
            console.error('[SSE] 연결 실패:', error);
            this.handleError();
        }
    },

    /**
     * 알림 수신 처리
     */
    handleNotification: function(event) {
        try {
            const data = JSON.parse(event.data);
            console.log('[SSE] 알림 수신:', data);
            
            // 알림 타입별 처리
            switch(data.type) {
                case 'USED_KEYWORD_MATCH':
                    this.handleUsedKeywordMatch(data);
                    break;
                case 'KEYWORD':
                    this.handleKeywordNotification(data);
                    break;
                case 'LIKE':
                    this.handleLikeNotification(data);
                    break;
                case 'CHAT':
                    this.handleChatNotification(data);
                    break;
                default:
                    console.warn('[SSE] 알 수 없는 알림 타입:', data.type);
            }
            
            // 전역 이벤트 발행 (헤더 배지, 알림 목록 등에서 구독)
            window.dispatchEvent(new CustomEvent('sse-notification', { detail: data }));

            // 사용자에게 UI 피드백 제공
            this.showNotificationUI(data);
            
        } catch (error) {
            console.error('[SSE] 알림 파싱 실패:', error);
        }
    },

    /**
     * 중고거래 키워드 매칭 알림 처리
     */
    handleUsedKeywordMatch: function(data) {
        console.log('[SSE] 중고거래 키워드 매칭 알림:', data);
        
        // Toast 표시
        if (typeof Utils !== 'undefined' && Utils.notify) {
            Utils.notify(data.message, 'success');
        } else {
            alert(data.message);
        }
        
        // 필요시 페이지 새로고침 또는 데이터 갱신
        // 예: location.reload();
    },

    /**
     * 키워드 알림 처리
     */
    handleKeywordNotification: function(data) {
        console.log('[SSE] 키워드 알림:', data);
        if (typeof Utils !== 'undefined' && Utils.notify) {
            Utils.notify(data.message, 'info');
        }
    },

    /**
     * 좋아요 알림 처리
     */
    handleLikeNotification: function(data) {
        console.log('[SSE] 좋아요 알림:', data);
        if (typeof Utils !== 'undefined' && Utils.notify) {
            Utils.notify(data.message, 'info');
        }
    },

    /**
     * 채팅 알림 처리
     */
    handleChatNotification: function(data) {
        console.log('[SSE] 채팅 알림:', data);
        if (typeof Utils !== 'undefined' && Utils.notify) {
            Utils.notify(data.message, 'warning');
        }
    },

    /**
     * 알림 UI 표시
     */
    showNotificationUI: function(data) {
        // 브라우저 알림 권한이 있으면 데스크톱 알림 발송
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification('알림', {
                body: data.message,
                icon: '/images/logo.png'
            });
        }
        
        // 또는 페이지 내 알림 배너 표시
        this.showNotificationBanner(data.message);
    },

    /**
     * 알림 배너 표시
     */
    showNotificationBanner: function(message) {
        // 알림 배너 HTML 생성
        const banner = document.createElement('div');
        banner.className = 'notification-banner notification-banner-success';
        banner.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #4CAF50;
            color: white;
            padding: 16px 24px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
            font-size: 14px;
            z-index: 10000;
            animation: slideIn 0.3s ease-out;
        `;
        banner.textContent = message;
        document.body.appendChild(banner);
        
        // 4초 후 자동 제거
        setTimeout(() => {
            banner.style.animation = 'slideOut 0.3s ease-out';
            setTimeout(() => banner.remove(), 300);
        }, 4000);
    },

    /**
     * 에러 처리 (자동 재연결)
     */
    handleError: function() {
        console.error('[SSE] 연결 끊김 - 재연결 시도');
        this.disconnect();
        
        if (this.connectRetries < this.maxRetries) {
            this.connectRetries++;
            const delay = this.retryDelay * this.connectRetries;
            console.log(`[SSE] ${delay}ms 후 재연결 시도 (${this.connectRetries}/${this.maxRetries})`);
            
            setTimeout(() => {
                this.connect();
            }, delay);
        } else {
            console.error('[SSE] 최대 재연결 횟수 초과');
            if (typeof Utils !== 'undefined' && Utils.notify) {
                Utils.notify('알림 연결이 실패했습니다', 'error');
            }
        }
    },

    /**
     * SSE 연결 해제
     */
    disconnect: function() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
            console.log('[SSE] 연결 해제됨');
        }
    },

    /**
     * 브라우저 알림 권한 요청
     */
    requestNotificationPermission: function() {
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission();
        }
    }
};

// 페이지 로드 시 SSE 초기화 (인증된 사용자인 경우)
document.addEventListener('DOMContentLoaded', () => {
    // 사용자 ID를 어딘가에서 가져와야 함
    // 예: <input type="hidden" id="loginUserIdx" value="1">
    const userIdElement = document.getElementById('loginUserIdx');
    if (userIdElement) {
        const userId = parseInt(userIdElement.value);
        if (userId && !isNaN(userId)) {
            SSEClient.init(userId);
            SSEClient.requestNotificationPermission();
        }
    }
});

// 페이지 언로드 시 연결 해제
window.addEventListener('beforeunload', () => {
    SSEClient.disconnect();
});
