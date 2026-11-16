/* Shared Cloud Auth for Sastra Nusantara Portal.
 * Supabase Auth session is shared by all modules hosted on the SAME HTTPS origin.
 */
(function () {
  "use strict";
  const cfg = window.SN_CLOUD_CONFIG || {};
  const invalid = !cfg.supabaseUrl || cfg.supabaseUrl.includes("PROJECT-REF") ||
    !cfg.supabaseAnonKey || cfg.supabaseAnonKey.includes("ISI_PUBLISHABLE");
  let client = null;

  function loadSdk() {
    if (window.supabase && window.supabase.createClient) return Promise.resolve();
    return new Promise((resolve, reject) => {
      const existing = document.querySelector('script[data-sn-supabase-sdk]');
      if (existing) {
        existing.addEventListener("load", resolve, { once: true });
        existing.addEventListener("error", reject, { once: true });
        return;
      }
      const s = document.createElement("script");
      s.src = "https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2";
      s.async = true;
      s.dataset.snSupabaseSdk = "1";
      s.onload = resolve;
      s.onerror = () => reject(new Error("SDK Supabase gagal dimuat. Periksa koneksi internet."));
      document.head.appendChild(s);
    });
  }

  async function ready() {
    if (invalid) throw new Error("Konfigurasi Cloud belum diisi. Buka web/shared/cloud-config.js.");
    await loadSdk();
    if (!client) {
      client = window.supabase.createClient(cfg.supabaseUrl, cfg.supabaseAnonKey, {
        auth: {
          persistSession: true,
          autoRefreshToken: true,
          detectSessionInUrl: true,
          storageKey: "sn-sastra-cloud-session-v1"
        }
      });
    }
    return client;
  }

  async function profileFor(userId) {
    const sb = await ready();
    const { data, error } = await sb.from("sn_profiles")
      .select("user_id,display_name,role,billing_access,mikrotik_access,is_active")
      .eq("user_id", userId).maybeSingle();
    if (error) throw error;
    if (!data || !data.is_active) throw new Error("Akun belum diaktifkan atau profil akses belum dibuat oleh administrator.");
    return data;
  }

  async function current() {
    const sb = await ready();
    const { data, error } = await sb.auth.getSession();
    if (error) throw error;
    if (!data.session) return null;
    const profile = await profileFor(data.session.user.id);
    return { session: data.session, user: data.session.user, profile };
  }

  async function signIn(email, password) {
    const sb = await ready();
    const { data, error } = await sb.auth.signInWithPassword({ email, password });
    if (error) throw error;
    try {
      const profile = await profileFor(data.user.id);
      return { session: data.session, user: data.user, profile };
    } catch (e) {
      await sb.auth.signOut();
      throw e;
    }
  }

  async function signUp(email, password, displayName) {
    const sb = await ready();
    const { data, error } = await sb.auth.signUp({
      email, password,
      options: { data: { display_name: displayName } }
    });
    if (error) throw error;
    return data;
  }

  async function resetPassword(email) {
    const sb = await ready();
    const base = new URL(cfg.portalBaseUrl || "/", location.origin).toString();
    const { error } = await sb.auth.resetPasswordForEmail(email, { redirectTo: base });
    if (error) throw error;
  }

  async function adminListProfiles() {
    const sb = await ready();
    const { data, error } = await sb.rpc("sn_admin_list_profiles");
    if (error) throw error;
    return data || [];
  }

  async function adminUpdateProfile(profile) {
    const sb = await ready();
    const { data, error } = await sb.rpc("sn_admin_update_profile", {
      p_user_id: profile.user_id,
      p_display_name: profile.display_name,
      p_role: profile.role,
      p_billing_access: !!profile.billing_access,
      p_mikrotik_access: !!profile.mikrotik_access,
      p_is_active: !!profile.is_active
    });
    if (error) throw error;
    return data;
  }

  async function signOut() {
    const sb = await ready();
    const { error } = await sb.auth.signOut();
    if (error) throw error;
  }

  async function requireModule(moduleName) {
    try {
      const state = await current();
      if (!state) {
        location.replace((cfg.portalBaseUrl || "/") + "index.html");
        return null;
      }
      const key = moduleName + "_access";
      if (!state.profile[key]) {
        document.body.innerHTML = '<main style="font:16px system-ui;max-width:560px;margin:12vh auto;padding:24px"><h2>Akses tidak diizinkan</h2><p>Akun ini belum diberi hak akses untuk modul ' +
          moduleName + '.</p><a href="' + (cfg.portalBaseUrl || "/") + 'index.html">Kembali ke Portal</a></main>';
        return null;
      }
      return state;
    } catch (e) {
      console.error(e);
      document.body.innerHTML = '<main style="font:16px system-ui;max-width:560px;margin:12vh auto;padding:24px"><h2>Cloud Login belum siap</h2><p>' +
        String(e.message || e) + '</p><a href="' + (cfg.portalBaseUrl || "/") + 'index.html">Kembali ke Portal</a></main>';
      return null;
    }
  }

  window.SNCloudAuth = { ready, current, signIn, signUp, resetPassword, adminListProfiles, adminUpdateProfile, signOut, requireModule, profileFor };
})();
