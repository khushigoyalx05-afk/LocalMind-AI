
const API_BASE = (
  import.meta.env.VITE_API_BASE_URL ||
  "http://localhost:8080"
).replace(/\/+$/, "");

/*
 * LocalMind AI API Client
 *
 * These endpoint paths should match your Spring Boot
 * controller mappings.
 */
const ENDPOINTS = {
  conversations: "/api/conversations",
  documents: "/api/documents",
  documentUpload: "/api/documents/upload",
  agentStream: "/api/agent/stream",
  analysisUpload: "/api/analysis/upload",
  analysisChart: "/api/analysis/chart",
};

function url(path) {
  return `${API_BASE}${path}`;
}

function conversationPath(id) {
  return `${ENDPOINTS.conversations}/${encodeURIComponent(id)}`;
}

function jsonOptions(method, data, signal) {
  return {
    method,
    headers: {
      "Content-Type": "application/json",
    },
    body:
      data === undefined
        ? undefined
        : JSON.stringify(data),
    signal,
  };
}

async function request(path, options = {}) {
  const response = await fetch(url(path), options);

  if (!response.ok) {
    const message = await response.text().catch(() => "");

    throw new Error(
      message || `HTTP ${response.status}: ${response.statusText}`
    );
  }

  if (response.status === 204) {
    return null;
  }

  const contentType =
    response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

/*
 * Streaming handler
 * Supports:
 * - text/plain
 * - text/event-stream (SSE)
 */
async function readStream(response, signal, onChunk) {
  if (!response.ok) {
    const message = await response.text().catch(() => "");

    throw new Error(
      message || `Streaming failed: HTTP ${response.status}`
    );
  }

  if (!response.body) {
    throw new Error("Streaming response body unavailable");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  const contentType =
    response.headers.get("content-type") || "";

  const isSSE = contentType.includes("text/event-stream");

  let buffer = "";

  function emitSSE(block) {
    const lines = block.split(/\r?\n/);

    const data = lines
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.slice(5).trimStart())
      .join("\n");

    if (!data || data === "[DONE]") {
      return;
    }

    let parsed;

    try {
      parsed = JSON.parse(data);
    } catch {
      onChunk?.(data);
      return;
    }

    if (typeof parsed === "string") {
      onChunk?.(parsed);
      return;
    }

    if (parsed && typeof parsed === "object") {
      if (parsed.error) {
        throw new Error(String(parsed.error));
      }

      const chunk =
        parsed.content ??
        parsed.delta ??
        parsed.text ??
        parsed.token;

      if (typeof chunk === "string") {
        onChunk?.(chunk);
      }
    }
  }

  try {
    while (true) {
      if (signal?.aborted) {
        throw new DOMException(
          "Generation stopped",
          "AbortError"
        );
      }

      const { value, done } = await reader.read();

      if (done) break;

      const text = decoder.decode(value, {
        stream: true,
      });

      if (!isSSE) {
        onChunk?.(text);
        continue;
      }

      buffer += text;

      const blocks = buffer.split(/\r?\n\r?\n/);
      buffer = blocks.pop() || "";

      for (const block of blocks) {
        emitSSE(block);
      }
    }

    const remaining = decoder.decode();

    if (!isSSE) {
      if (remaining) onChunk?.(remaining);
    } else {
      buffer += remaining;

      if (buffer.trim()) {
        emitSSE(buffer);
      }
    }
  } finally {
    reader.releaseLock();
  }
}

export const api = {
  baseUrl: API_BASE,

  // ==========================================
  // CONVERSATIONS
  // ==========================================

  conversations(signal) {
    return request(ENDPOINTS.conversations, {
      signal,
    });
  },

  create(mode = "GENERAL", title = "New Chat") {
    return request(
      ENDPOINTS.conversations,
      jsonOptions("POST", {
        chatMode: mode,
        title,
      })
    );
  },

  messages(conversationId, signal) {
    return request(
      `${conversationPath(conversationId)}/messages`,
      { signal }
    );
  },

  rename(conversationId, title) {
    return request(
      conversationPath(conversationId),
      jsonOptions("PATCH", { title })
    );
  },

  delete(conversationId) {
    return request(conversationPath(conversationId), {
      method: "DELETE",
    });
  },

  // ==========================================
  // DOCUMENTS / PDF RAG
  // ==========================================

  documents(conversationId, signal) {
    return request(
      `${conversationPath(conversationId)}/documents`,
      { signal }
    );
  },

  upload(file, signal) {
    const formData = new FormData();
    formData.append("file", file);

    return request(ENDPOINTS.documentUpload, {
      method: "POST",
      body: formData,
      signal,
    });
  },

  attach(conversationId, documentId) {
    return request(
      `${conversationPath(conversationId)}/documents/${encodeURIComponent(documentId)}`,
      {
        method: "POST",
      }
    );
  },

  detach(conversationId, documentId) {
    return request(
      `${conversationPath(conversationId)}/documents/${encodeURIComponent(documentId)}`,
      {
        method: "DELETE",
      }
    );
  },

  // ==========================================
  // STREAMING CHAT
  // Signature matches App.jsx:
  // stream(id, message, signal, onChunk)
  // ==========================================

  async stream(
    conversationId,
    message,
    signal,
    onChunk
  ) {
    const response = await fetch(
      url(`${conversationPath(conversationId)}/chat`),
      {
        method: "POST",
        headers: {
          "Content-Type": "text/plain",
          Accept: "text/plain, text/event-stream",
        },
        body: message,
        signal,
      }
    );

    return readStream(response, signal, onChunk);
  },

  // ==========================================
  // AI AGENT STREAMING
  // Signature matches App.jsx:
  // agentStream(id, payload, signal, onChunk)
  // ==========================================

  async agentStream(
    conversationId,
    payload,
    signal,
    onChunk
  ) {
    const formData = new FormData();

    formData.append(
      "conversationId",
      String(conversationId)
    );

    Object.entries(payload || {}).forEach(
      ([key, value]) => {
        if (value == null) return;

        if (value instanceof File) {
          formData.append(key, value);
        } else if (Array.isArray(value)) {
          value.forEach((item) => {
            if (item instanceof File) {
              formData.append(key, item);
            } else if (item != null) {
              formData.append(key, String(item));
            }
          });
        } else if (typeof value === "object") {
          formData.append(
            key,
            JSON.stringify(value)
          );
        } else {
          formData.append(key, String(value));
        }
      }
    );

    const response = await fetch(
      url(ENDPOINTS.agentStream),
      {
        method: "POST",
        body: formData,
        signal,
        headers: {
          Accept: "text/plain, text/event-stream",
        },
      }
    );

    return readStream(response, signal, onChunk);
  },

  // ==========================================
  // DATA ANALYSIS
  // ==========================================

  uploadAnalysisFile(file, signal) {
    const formData = new FormData();
    formData.append("file", file);

    return request(ENDPOINTS.analysisUpload, {
      method: "POST",
      body: formData,
      signal,
    });
  },

  generateChart(payload, signal) {
    return request(
      ENDPOINTS.analysisChart,
      jsonOptions("POST", payload, signal)
    );
  },

  // ==========================================
  // COMPATIBILITY ALIASES
  // ==========================================

  getConversations(signal) {
    return this.conversations(signal);
  },

  createConversation(data = {}) {
    return this.create(
      data.chatMode || "GENERAL",
      data.title || "New Chat"
    );
  },

  getMessages(id, signal) {
    return this.messages(id, signal);
  },

  deleteConversation(id) {
    return this.delete(id);
  },

  uploadDocument(file, conversationId, signal) {
    return this.upload(file, signal);
  },

  get(path, signal) {
    return request(path, { signal });
  },

  post(path, payload, signal) {
    return request(
      path,
      jsonOptions("POST", payload, signal)
    );
  },
};

export default api;
