import type { AuthProvider } from "@refinedev/core";
import { API_URL, TOKEN_KEY, USER_KEY } from "./constants";

interface LoginResponse {
  token: string;
  userId: number;
  username: string;
  email: string;
  role: string;
  blocked: boolean;
}

export const authProvider: AuthProvider = {
  login: async ({ email, password }) => {
    try {
      const response = await fetch(`${API_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        return {
          success: false,
          error: { name: "LoginError", message: "Invalid email or password" },
        };
      }

      const data: LoginResponse = await response.json();

      if (data.role !== "ADMIN") {
        return {
          success: false,
          error: {
            name: "AccessDenied",
            message: "Admin access required",
          },
        };
      }

      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(
        USER_KEY,
        JSON.stringify({
          id: data.userId,
          name: data.username,
          email: data.email,
          role: data.role,
        })
      );

      return { success: true, redirectTo: "/" };
    } catch {
      return {
        success: false,
        error: { name: "LoginError", message: "Network error" },
      };
    }
  },

  logout: async () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    return { success: true, redirectTo: "/login" };
  },

  check: async () => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      return { authenticated: true };
    }
    return { authenticated: false, redirectTo: "/login" };
  },

  getPermissions: async () => {
    const user = localStorage.getItem(USER_KEY);
    if (user) {
      const parsed = JSON.parse(user);
      return parsed.role;
    }
    return null;
  },

  getIdentity: async () => {
    const user = localStorage.getItem(USER_KEY);
    if (user) {
      const parsed = JSON.parse(user);
      return {
        id: parsed.id,
        name: parsed.name,
        email: parsed.email,
        avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(parsed.name)}&background=2E7D32&color=fff`,
      };
    }
    return null;
  },

  onError: async (error) => {
    if (error?.statusCode === 401 || error?.statusCode === 403) {
      return { logout: true, redirectTo: "/login" };
    }
    return { error };
  },
};
