#ifndef NFCGATE_SYSTEM_H
#define NFCGATE_SYSTEM_H

/* NCI definitions */
using tNFC_STATUS = uint8_t;
using tNFA_STATUS = uint8_t;
using tNFA_TECHNOLOGY_MASK = uint8_t;
using tNCI_DISCOVERY_TYPE = uint8_t;
// Wildcard AID selected
#define CE_T4T_STATUS_WILDCARD_AID_SELECTED 0x40
// NCI OK
#define NCI_STATUS_OK 0x00

// offset to ce_cb->mem.t4t.status field (ce_int.h)
#define CE_CB_STATUS_POST_O 0xd0
#define CE_CB_STATUS_PRE_O 0xd8
// usual offset to nfa_dm_cb->p_conn_cback field (nfa_dm_int.h)
#define NFA_DM_CB_CONN_CBACK (8 * sizeof(void*))

// modified from nfa_api.h

typedef struct {
    uint8_t type;
    uint8_t frequency;
} tNCI_DISCOVER_PARAMS;

/* NFA EE status */
#define NFA_EE_STATUS_INACTIVE 0x01
/* NFA EE modes */
#define NFA_EE_MD_ACTIVATE 0x01
#define NFA_EE_MD_DEACTIVATE 0x00

/* NFA Connection Callback Events */
#define NFA_POLL_ENABLED_EVT 0
#define NFA_POLL_DISABLED_EVT 1
#define NFA_RF_DISCOVERY_STARTED_EVT 30
#define NFA_RF_DISCOVERY_STOPPED_EVT 31
#define NFA_SET_P2P_LISTEN_TECH_EVT 33

/* NFA Tech Mask Values */
#define NFA_TECHNOLOGY_MASK_A 0x01
#define NFA_TECHNOLOGY_MASK_B 0x02
#define NFA_TECHNOLOGY_MASK_F 0x04
#define NFA_TECHNOLOGY_MASK_V 0x08
#define NFA_TECHNOLOGY_MASK_A_ACTIVE 0x40
#define NFA_TECHNOLOGY_MASK_F_ACTIVE 0x80
#define NFA_TECHNOLOGY_MASK_KOVIO 0x20
// modified from DEFAULT_TECH_MASK in NativeNfcManager.cpp
#define SAFE_TECH_MASK \
    (NFA_TECHNOLOGY_MASK_A | NFA_TECHNOLOGY_MASK_B | NFA_TECHNOLOGY_MASK_F | \
     NFA_TECHNOLOGY_MASK_V | NFA_TECHNOLOGY_MASK_A_ACTIVE | NFA_TECHNOLOGY_MASK_F_ACTIVE | \
     NFA_TECHNOLOGY_MASK_KOVIO)

/* NCI Discovery Mask Values */
#define NCI_DISCOVERY_TYPE_LISTEN_A 0x80
#define NCI_DISCOVERY_TYPE_LISTEN_B 0x81
#define NCI_DISCOVERY_TYPE_LISTEN_F 0x82
#define NCI_DISCOVERY_TYPE_LISTEN_A_ACTIVE 0x83
#define NCI_DISCOVERY_TYPE_LISTEN_F_ACTIVE 0x85
#define NCI_DISCOVERY_TYPE_LISTEN_ISO15693 0x86

/* EE interface types */
#define NCI_NFCEE_INTERFACE_APDU 0x00
#define NCI_NFCEE_INTERFACE_HCI_ACCESS 0x01
#define NCI_NFCEE_INTERFACE_T3T 0x02
#define NCI_NFCEE_INTERFACE_TRANSPARENT 0x03
#define NCI_NFCEE_INTERFACE_PROPRIETARY 0x80


class System {
public:
    enum SdkVersion {
        O_1 = 26,       //< Oreo (8.0.0)
        O_2 = 27,       //< Oreo (8.1.0)
        P = 28,         //< Pie (9)
        Q = 29,         //< Android10 (10)
    };

    static int sdkInt();

    static std::string nfaEventName(uint8_t event);

protected:
    static int sSdkInt;
};

#endif //NFCGATE_SYSTEM_H
