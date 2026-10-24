import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";

// Seamless token handoff when switching from HTTPS to HTTP on *.sslip.io
if (typeof window !== "undefined") {
  try {
    const params = new URLSearchParams(window.location.search);
    const transferredToken = params.get("__auth_token");
    const transferredUserInfo = params.get("__user_info");
    let hasTransferred = false;

    if (transferredToken) {
      localStorage.setItem("auth_token", transferredToken);
      params.delete("__auth_token");
      hasTransferred = true;
    }
    if (transferredUserInfo) {
      localStorage.setItem("user_info", transferredUserInfo);
      params.delete("__user_info");
      hasTransferred = true;
    }
    if (hasTransferred) {
      const newSearch = params.toString();
      const cleanUrl = window.location.pathname + (newSearch ? `?${newSearch}` : "") + window.location.hash;
      window.history.replaceState({}, "", cleanUrl);
    }
  } catch (e) {
    console.warn("Handoff notice:", e);
  }
}

createRoot(document.getElementById("root")!).render(<App />);
