import type { DataProvider } from "@refinedev/core";
import { API_URL, TOKEN_KEY } from "./constants";

const getHeaders = (): HeadersInit => {
  const token = localStorage.getItem(TOKEN_KEY);
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

const handleResponse = async (response: Response) => {
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `HTTP ${response.status}`);
  }
  return response.json();
};

export const dataProvider: DataProvider = {
  getList: async ({ resource, pagination, filters }) => {
    const current = (pagination as any)?.current ?? 1;
    const pageSize = (pagination as any)?.pageSize ?? 10;

    let url = `${API_URL}/admin/${resource}`;
    const params = new URLSearchParams();
    params.set("page", String(current - 1));
    params.set("size", String(pageSize));

    const searchFilter = filters?.find(
      (f) => "field" in f && f.field === "search"
    );
    if (searchFilter && "value" in searchFilter && searchFilter.value) {
      params.set("search", String(searchFilter.value));
    }

    url += `?${params.toString()}`;

    const response = await fetch(url, { headers: getHeaders() });
    const data = await handleResponse(response);

    // The backend returns Spring Page object for /admin/users
    if (data.content) {
      return {
        data: data.content,
        total: data.totalElements ?? data.content.length,
      };
    }

    // Fallback for non-paginated endpoints
    return {
      data: Array.isArray(data) ? data : [data],
      total: Array.isArray(data) ? data.length : 1,
    };
  },

  getOne: async ({ resource, id }) => {
    const response = await fetch(`${API_URL}/admin/${resource}/${id}`, {
      headers: getHeaders(),
    });
    const data = await handleResponse(response);
    return { data };
  },

  create: async ({ resource, variables }) => {
    const response = await fetch(`${API_URL}/admin/${resource}`, {
      method: "POST",
      headers: getHeaders(),
      body: JSON.stringify(variables),
    });
    const data = await handleResponse(response);
    return { data };
  },

  update: async ({ resource, id, variables }) => {
    const url = `${API_URL}/admin/${resource}/${id}`;

    const response = await fetch(url, {
      method: "PUT",
      headers: getHeaders(),
      body: JSON.stringify(variables),
    });
    const data = await handleResponse(response);
    return { data };
  },

  deleteOne: async ({ resource, id }) => {
    const response = await fetch(`${API_URL}/admin/${resource}/${id}`, {
      method: "DELETE",
      headers: getHeaders(),
    });

    if (!response.ok) {
      const message = await response.text();
      throw new Error(message || `HTTP ${response.status}`);
    }

    return { data: { id } as any };
  },

  getApiUrl: () => API_URL,

  custom: async ({ url, method, payload }) => {
    const options: RequestInit = {
      method: method.toUpperCase(),
      headers: getHeaders(),
    };

    if (payload && method !== "get") {
      options.body = JSON.stringify(payload);
    }

    const response = await fetch(url, options);
    const data = await handleResponse(response);
    return { data };
  },
};
