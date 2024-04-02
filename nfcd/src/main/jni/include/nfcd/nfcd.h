#include <unistd.h>

#include <nfcd/error.h>
#include <nfcd/helper/Config.h>
#include <nfcd/helper/EventQueue.h>
#include <nfcd/helper/MapInfo.h>
#include <nfcd/helper/StringUtil.h>
#include <nfcd/helper/SymbolTable.h>
#include <nfcd/helper/System.h>
#include <nfcd/hook/IHook.h>

/*
 * enable(1) or disable(0) any permutation of these to test the code
 *
 * remember to always re-build, re-install the app and terminate
 * the com.android.nfc process to reload the hook (e.g. killall com.android.nfc in adb)
 * when testing a new combination
 */
#define DEBUG_1 1
#define DEBUG_2 1
#define DEBUG_3 1
#define DEBUG_4 1
#define DEBUG_5 1

extern tNFC_STATUS hook_NFC_SetConfig(uint8_t tlv_size, uint8_t *p_param_tlvs);
extern tNFA_STATUS hook_NFA_Enable(void *p_dm_cback, void *p_conn_cback);
extern tNFC_STATUS hook_ce_select_t4t (void);

using def_NFC_SetConfig = decltype(hook_NFC_SetConfig);
using def_NFC_SetConfig = decltype(hook_NFC_SetConfig);
using def_NFA_StopRfDiscovery = tNFA_STATUS();
using def_NFA_DisablePolling = tNFA_STATUS();
using def_NFA_StartRfDiscovery = tNFA_STATUS();
using def_NFA_EnablePolling = tNFA_STATUS(tNFA_TECHNOLOGY_MASK poll_mask);
using def_NFA_CONN_CBACK = void(uint8_t event, void *data);
using def_ce_select_t4t = decltype(hook_ce_select_t4t);

using def_NFA_SetP2pListenTech = tNFA_STATUS(tNFA_TECHNOLOGY_MASK tech_mask);

class HookGlobals {
public:
    HookGlobals();

    Config origValues, hookValues;
    EventQueue eventQueue;
    SymbolTable symbolTable;
    MapInfo mapInfo;

    bool hookStaticEnabled = false;
    bool hookDynamicEnabled = false;

    bool patchEnabled = false;
    bool guardEnabled = true;

    IHook_ref hNFC_SetConfig;
    IHook_ref hNFA_Enable;
    IHook_ref hce_select_t4t;
    Symbol_ref nfa_dm_cb;
    Symbol_ref hce_cb;
    Symbol_ref hNFA_StopRfDiscovery;
    Symbol_ref hNFA_DisablePolling;
    Symbol_ref hNFA_StartRfDiscovery;
    Symbol_ref hNFA_EnablePolling;
    Symbol_ref hNFA_SetP2pListenTech;

    def_NFA_CONN_CBACK *origNfaConnCBack = nullptr;
    std::mutex nfaConnCBackMutex;

    bool tryHookNFACB();

protected:
    std::string findLibNFC() const;

    bool checkNFACBOffset(uint32_t offset);
    uint32_t findNFACBOffset();

    Symbol_ref lookupSymbol(const std::string &name) const;
    IHook_ref hookSymbol(const std::string &name, void *hook) const;

    void *mHandle;
    std::string mLibrary, mLibraryRe;
};

extern HookGlobals globals;
