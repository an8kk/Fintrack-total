const getApiUrl = () => {
    const envoyUrl = import.meta.env.VITE_API_URL;
    if (!envoyUrl) return "http://localhost:8080/api";
    if (envoyUrl.startsWith("http")) return envoyUrl;
    return `https://${envoyUrl}`;
};

export const API_URL = getApiUrl();
export const TOKEN_KEY = "fintrack-admin-token";
export const USER_KEY = "fintrack-admin-user";
