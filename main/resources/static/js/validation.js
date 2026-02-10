/**
 * 입력값 검증 공통 함수
 */
const Validation = {
    /**
     * 이메일 검증
     * @param {string} email - 이메일 주소
     * @returns {boolean} 유효 여부
     */
    email: (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },

    /**
     * 전화번호 검증 (01X-XXXX-XXXX)
     * @param {string} phone - 전화번호
     * @returns {boolean} 유효 여부
     */
    phone: (phone) => {
        const phoneRegex = /^01[0-9]-\d{3,4}-\d{4}$/;
        return phoneRegex.test(phone);
    },

    /**
     * 비밀번호 검증 (8자리 이상 16자리 이하, 대문자/소문자/숫자/특수문자 포함)
     * @param {string} password - 비밀번호
     * @returns {boolean} 유효 여부
     */
    password: (password) => {
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$!%*?&])[A-Za-z\d@#$!%*?&]{8,16}$/;
        return passwordRegex.test(password);
    },

    /**
     * 필수 입력값 검증 (공백 체크)
     * @param {string} value - 입력값
     * @returns {boolean} 유효 여부
     */
    required: (value) => {
        return value && value.trim().length > 0;
    },

    /**
     * 최소 길이 검증
     * @param {string} value - 입력값
     * @param {number} minLength - 최소 길이
     * @returns {boolean} 유효 여부
     */
    minLength: (value, minLength) => {
        return value && value.length >= minLength;
    },

    /**
     * URL 검증
     * @param {string} url - URL 주소
     * @returns {boolean} 유효 여부
     */
    url: (url) => {
        try {
            new URL(url);
            return true;
        } catch {
            return false;
        }
    }
};
