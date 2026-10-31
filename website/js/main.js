// CodX Testing & Download Portal Engine
document.addEventListener("DOMContentLoaded", () => {
  initHealthCheck();
  initModals();
  initApiTester();
  initDynamicUrls();
});

// Auto-detect current host to generate dynamic APK download URLs
function initDynamicUrls() {
  const currentHost = window.location.origin;
  const serverInput = document.getElementById("server-url-input");
  if (serverInput) {
    serverInput.value = currentHost.includes("http") ? currentHost : "http://localhost:8000";
  }

  // Update download links to point to actual download path
  const recorderLink = document.getElementById("dl-recorder-btn");
  const controllerLink = document.getElementById("dl-controller-btn");
  const sourceLink = document.getElementById("dl-source-btn");

  if (recorderLink) recorderLink.href = `${currentHost}/downloads/CodX_Recorder_v1.0_Test.apk`;
  if (controllerLink) controllerLink.href = `${currentHost}/downloads/CodX_Controller_v1.0_Test.apk`;
  if (sourceLink) sourceLink.href = `${currentHost}/downloads/CodX_Android_Project.zip`;
}

// Check live FastAPI cloud health
async function initHealthCheck() {
  const statusEl = document.getElementById("system-status-indicator");
  const latencyEl = document.getElementById("system-latency");

  try {
    const startTime = performance.now();
    const res = await fetch("/health");
    const endTime = performance.now();
    const latency = Math.round(endTime - startTime);

    if (res.ok) {
      if (statusEl) {
        statusEl.innerHTML = `<span class="status-dot"></span> Cloud API Online (${latency}ms)`;
      }
      if (latencyEl) {
        latencyEl.textContent = `${latency}ms latency`;
      }
    } else {
      if (statusEl) {
        statusEl.innerHTML = `<span class="status-dot" style="background:#EF4444;box-shadow:0 0 10px #EF4444;"></span> Server Response: ${res.status}`;
      }
    }
  } catch (err) {
    if (statusEl) {
      statusEl.innerHTML = `<span class="status-dot" style="background:#F59E0B;box-shadow:0 0 10px #F59E0B;"></span> Local Mode (Server Offline or Sandboxed)`;
    }
  }
}

// Live API Tester
function initApiTester() {
  const pingBtn = document.getElementById("test-ping-btn");
  const registerBtn = document.getElementById("test-register-btn");
  const consoleEl = document.getElementById("tester-console-output");
  const serverInput = document.getElementById("server-url-input");

  function log(msg) {
    if (!consoleEl) return;
    const time = new Date().toLocaleTimeString();
    consoleEl.textContent += `[${time}] ${msg}\n`;
    consoleEl.scrollTop = consoleEl.scrollHeight;
  }

  if (pingBtn) {
    pingBtn.addEventListener("click", async () => {
      const base = serverInput ? serverInput.value.replace(/\/$/, "") : "";
      log(`Pinging GET ${base}/health ...`);
      try {
        const t0 = performance.now();
        const res = await fetch(`${base}/health`);
        const data = await res.json();
        const ms = Math.round(performance.now() - t0);
        log(`✓ Status ${res.status} OK (${ms}ms): ${JSON.stringify(data)}`);
      } catch (e) {
        log(`✗ Ping failed: ${e.message}`);
      }
    });
  }

  if (registerBtn) {
    registerBtn.addEventListener("click", async () => {
      const base = serverInput ? serverInput.value.replace(/\/$/, "") : "";
      log(`Registering test device POST ${base}/api/auth/register-recorder ...`);
      try {
        const res = await fetch(`${base}/api/auth/register-recorder`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name: "Web Tester Phone A" })
        });
        const data = await res.json();
        log(`✓ Success: Device registered!`);
        log(`  Device ID: ${data.device_id}`);
        log(`  PAIRING CODE: ${data.pairing_code}`);
      } catch (e) {
        log(`✗ Registration failed: ${e.message}`);
      }
    });
  }
}

// Modal handling for Terms & Privacy
function initModals() {
  const termsModal = document.getElementById("modal-terms");
  const privacyModal = document.getElementById("modal-privacy");

  window.openModal = function(type) {
    if (type === "terms" && termsModal) termsModal.classList.add("active");
    if (type === "privacy" && privacyModal) privacyModal.classList.add("active");
  };

  window.closeModal = function(type) {
    if (type === "terms" && termsModal) termsModal.classList.remove("active");
    if (type === "privacy" && privacyModal) privacyModal.classList.remove("active");
  };

  // Close when clicking overlay
  document.querySelectorAll(".modal-overlay").forEach(overlay => {
    overlay.addEventListener("click", (e) => {
      if (e.target === overlay) {
        overlay.classList.remove("active");
      }
    });
  });
}
