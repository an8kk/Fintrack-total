const getApiUrl = () => {
    const envoyUrl = import.meta.env.VITE_API_URL;
    if (!envoyUrl || envoyUrl === "fintrack-backend-b56q") {
        // Fallback to the known live URL if the env var is missing or truncated by Render
        return "https://fintrack-backend-b56q.onrender.com/api";
    }
    if (envoyUrl.startsWith("http")) return envoyUrl;
    return `https://${envoyUrl}`;
};

export const API_URL = getApiUrl();
export const TOKEN_KEY = "fintrack-admin-token";
export const USER_KEY = "fintrack-admin-user";
