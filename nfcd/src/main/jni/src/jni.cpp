#include <nfcd/nfcd.h>
#include <jni.h>

static void beginCollectingEvents() {
    globals.eventQueue.beginCollecting();
}

static void waitForEvent(uint8_t event, bool checkStatus = true) {
    uint8_t status;
    if (globals.eventQueue.waitFor(event, status, 500)) {
        if (checkStatus && status != 0)
            LOGW("[event] Unexpected status for %s: expected 0, got %d",
                 System::nfaEventName(event).c_str(), status);
    }
    else
        LOGW("[event] Waiting for %s failed: timeout reached", System::nfaEventName(event).c_str());
}

tNFA_TECHNOLOGY_MASK maskFromConfig(const Config &config) {
    tNFA_TECHNOLOGY_MASK result = 0;

    for (const auto &option : config.options()) {
        if (option.name().find("LA") == 0)
            result |= NFA_TECHNOLOGY_MASK_A | NFA_TECHNOLOGY_MASK_A_ACTIVE;
        else if (option.name().find("LB") == 0)
            result |= NFA_TECHNOLOGY_MASK_B;
        else if (option.name().find("LF") == 0)
            result |= NFA_TECHNOLOGY_MASK_F | NFA_TECHNOLOGY_MASK_F_ACTIVE;
    }

    return result;
}

void nfaEnableDiscovery() {
    beginCollectingEvents();
    globals.hNFA_StartRfDiscovery->call<def_NFA_StartRfDiscovery>();
    LOGD("[nfcd] Starting RF discovery");
    waitForEvent(NFA_RF_DISCOVERY_STARTED_EVT);
}

void nfaDisableDiscovery() {
    beginCollectingEvents();
    globals.hNFA_StopRfDiscovery->call<def_NFA_StopRfDiscovery>();
    LOGD("[nfcd] Stopping RF discovery");
    waitForEvent(NFA_RF_DISCOVERY_STOPPED_EVT, false);
}

void nfaEnablePolling() {
    /*
     * Note: only enable known technologies, since enabling all (0xFF) also enabled exotic
     * proprietary ones, which may fail to start without special configuration
     */
    beginCollectingEvents();
    globals.hNFA_EnablePolling->call<def_NFA_EnablePolling>(SAFE_TECH_MASK);
    LOGD("[nfcd] Enabling polling");
    waitForEvent(NFA_POLL_ENABLED_EVT);
}

void nfaDisablePolling() {
    beginCollectingEvents();
    globals.hNFA_DisablePolling->call<def_NFA_DisablePolling>();
    LOGD("[nfcd] Disabling polling");
    waitForEvent(NFA_POLL_DISABLED_EVT);
}

void nfaSetListenTech(tNFA_TECHNOLOGY_MASK tech) {
    beginCollectingEvents();
    globals.hNFA_SetP2pListenTech->call<def_NFA_SetP2pListenTech>(tech);
    LOGD("[nfcd] Setting listen tech to %d", tech);
    waitForEvent(NFA_SET_P2P_LISTEN_TECH_EVT);
}

void applyConfig(Config &config) {
    config_ref bin_stream;
    config.build(bin_stream);

    globals.guardEnabled = false;
    globals.hNFC_SetConfig->callHook<def_NFC_SetConfig>(config.total(), bin_stream.get());
    globals.guardEnabled = true;

    // wait for config to set before returning
    usleep(35000);
}

extern "C" {
    JNIEXPORT jboolean JNICALL Java_de_tu_1darmstadt_seemoo_nfcgate_xposed_Native_isHookEnabled(JNIEnv *, jobject) {
        return globals.hookStaticEnabled && globals.hookDynamicEnabled;
    }

    JNIEXPORT jboolean JNICALL Java_de_tu_1darmstadt_seemoo_nfcgate_xposed_Native_isPatchEnabled(JNIEnv *, jobject) {
        return globals.patchEnabled;
    }

    JNIEXPORT void JNICALL Java_de_tu_1darmstadt_seemoo_nfcgate_xposed_Native_setConfiguration(JNIEnv *env, jobject, jbyteArray config) {
        if (!env->IsSameObject(config, nullptr)) {
            // parse config value stream
            jsize config_len = env->GetArrayLength(config);
            jbyte *config_data = env->GetByteArrayElements(config, nullptr);
            globals.hookValues.parse(config_len, (uint8_t *) config_data);
            env->ReleaseByteArrayElements(config, config_data, 0);

            // begin re-routing AIDs
            globals.patchEnabled = true;

#if DEBUG_4
            // disable CON_DISCOVERY_PARAM right before setting the hook values
            Config config4;
            config4.add(0x02, (uint8_t *)"\x00", 1);
            applyConfig(config4);
#endif
#if DEBUG_5
            // disable NFCC_CONFIG_CONTROL right before setting the hook values
            Config config5;
            config5.add(0x85, (uint8_t *)"\x00", 1);
            applyConfig(config5);
#endif

            // disable discovery before changing anything
            nfaDisableDiscovery();
            {
                // apply the config stream
                applyConfig(globals.hookValues);
                // disable polling
                nfaDisablePolling();
                // enable listening only for the selected technologies
                auto mask = maskFromConfig(globals.hookValues);
                if (mask != 0)
                    nfaSetListenTech(mask);
            }
            // re-enable discovery after changes were made
            nfaEnableDiscovery();
        }
        else {
            globals.patchEnabled = false;
        }
    }

    JNIEXPORT void JNICALL Java_de_tu_1darmstadt_seemoo_nfcgate_xposed_Native_setPolling(JNIEnv *, jobject, jboolean enabled) {
        // disable discovery before changing anything
        nfaDisableDiscovery();
        {
            // enable / disable polling
            enabled ? nfaEnablePolling() : nfaDisablePolling();
        }
        // re-enable discovery after changes were made
        nfaEnableDiscovery();
    }
}
