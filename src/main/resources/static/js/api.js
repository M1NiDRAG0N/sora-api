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
        }
    },

    /**
     * POST 요청
     * @param {string} url - 요청 URL
     * @param {Object} data - 요청 데이터
     * @returns {Promise} 응답 데이터
     */
    post: async (url, data = {}) => {
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
        }
    },

    /**
     * PUT 요청
     * @param {string} url - 요청 URL
     * @param {Object} data - 요청 데이터
     * @returns {Promise} 응답 데이터
     */
    put: async (url, data = {}) => {
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
        }
    },

    /**
     * DELETE 요청
     * @param {string} url - 요청 URL
     * @returns {Promise} 응답 데이터
     */
    delete: async (url) => {
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
        }
    },

    /**
     * 응답 처리 공통 함수
     * @param {Response} response - fetch 응답
     * @returns {Promise} JSON 데이터
     */
    handleResponse: async (response) => {
        try {
            const data = await response.json();

            // 서버가 성공 응답을 보낸 경우
            if (response.ok) {
                return data;
            }

            // 서버가 에러 응답을 보낸 경우 (ApiResponse 형식)
            throw new Error(data.message || `HTTP ${response.status} 에러`);
        } catch (error) {
            throw error;
        }
    }
};
