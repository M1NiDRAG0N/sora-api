/**
 * 공통 API 호출 함수
 * 모든 fetch 요청을 이 함수를 통해 처리
 */
const API = {
    /**
     * GET 요청
     * @param {string} url - 요청 URL
     * @returns {Promise} 응답 데이터
     */
    get: async (url) => {
        Utils.showLoading();
        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            return await API.handleResponse(response);
        } catch (error) {
            console.error(`GET 요청 실패: ${error.message}`);
            throw new Error(`${error.message}`);
        } finally {
            Utils.hideLoading();
        }
    },

    /**
     * POST 요청
     * @param {string} url - 요청 URL
     * @param {Object} data - 요청 데이터
     * @returns {Promise} 응답 데이터
     */
    post: async (url, data = {}) => {
        Utils.showLoading();
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            return await API.handleResponse(response);
        } catch (error) {
            console.error(`POST 요청 실패: ${error.message}`);
            throw new Error(`${error.message}`);
        } finally {
            Utils.hideLoading();
        }
    },

    /**
     * PUT 요청
     * @param {string} url - 요청 URL
     * @param {Object} data - 요청 데이터
     * @returns {Promise} 응답 데이터
     */
    put: async (url, data = {}) => {
        Utils.showLoading();
        try {
            const response = await fetch(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            return await API.handleResponse(response);
        } catch (error) {
            console.error(`PUT 요청 실패: ${error.message}`);
            throw new Error(`${error.message}`);
        } finally {
            Utils.hideLoading();
        }
    },

    /**
     * PATCH 요청
     * @param {string} url - 요청 URL
     * @param {Object} data - 요청 데이터
     * @returns {Promise} 응답 데이터
     */
    patch: async (url, data = {}) => {
        Utils.showLoading();
        try {
            const response = await fetch(url, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            return await API.handleResponse(response);
        } catch (error) {
            console.error(`PATCH 요청 실패: ${error.message}`);
            throw new Error(`${error.message}`);
        } finally {
            Utils.hideLoading();
        }
    },

    /**
     * DELETE 요청
     * @param {string} url - 요청 URL
     * @returns {Promise} 응답 데이터
     */
    delete: async (url) => {
        Utils.showLoading();
        try {
            const response = await fetch(url, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            return await API.handleResponse(response);
        } catch (error) {
            console.error(`DELETE 요청 실패: ${error.message}`);
            throw new Error(`${error.message}`);
        } finally {
            Utils.hideLoading();
        }
    },

    /**
     * FormData를 포함한 POST 요청 (파일 업로드용)
     * @param {string} url - 요청 URL
     * @param {FormData} formData - FormData 객체
     * @returns {Promise} 응답 데이터
     */
    postFormData: async (url, formData) => {
        Utils.showLoading();
        try {
            const response = await fetch(url, {
                method: 'POST',
                body: formData
                // Content-Type을 설정하지 않음 (브라우저가 자동으로 multipart/form-data 설정)
            });
            return await API.handleResponse(response);
        } catch (error) {
            console.error(`FormData POST 요청 실패: ${error.message}`);
            throw new Error(`${error.message}`);
        } finally {
            Utils.hideLoading();
        }
    },

    /**
     * 응답 처리 공통 함수
     * @param {Response} response - fetch 응답
     * @returns {Promise} JSON 데이터
     */
    handleResponse: async (response) => {
        const text = await response.text();
        if (!text) {
            if (response.ok) return null;
            throw new Error(`HTTP ${response.status} 에러`);
        }
        try {
            const data = JSON.parse(text);
            if (response.ok) return data;
            throw new Error(data.message || `HTTP ${response.status} 에러`);
        } catch (error) {
            if (response.ok) return null;
            throw new Error(`HTTP ${response.status} 에러`);
        }
    }
};
