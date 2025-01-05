#include <nfcd/helper/EEManager.h>
#include <nfcd/nfcd.h>

#include <cinttypes>
#include <vector>

std::set<uint16_t> EEManager::findActiveEEs() const {
    std::set<uint16_t> result;

    // exact number of EEs in the list
    size_t eeCount = countEEs();
    // approximate or exact struct size as needed
    size_t structSize = findStructSize(eeCount);

    if (eeCount > 0) {
        std::vector<uint8_t> buffer(structSize * eeCount);

        uint8_t num_ee = eeCount;
        auto rv = globals.hNFA_EeGetInfo->call<def_NFA_EeGetInfo>(
                &num_ee, reinterpret_cast<void*>(buffer.data()));
        LOG_ASSERT_S(rv == NCI_STATUS_OK, return result, "[EEManager] NFA_EeGetInfo failed for get data");

        for (size_t i = 0; i < num_ee; i++) {
            uint8_t *eeInfo = buffer.data() + structSize * i;

            // uint16_t ee_handle is always at the start of each struct
            uint16_t ee_handle = *reinterpret_cast<uint16_t *>(eeInfo);
            // uint8_t ee_status is always at offset 2 in each struct
            uint8_t ee_status = *reinterpret_cast<uint16_t *>(eeInfo + sizeof(uint8_t) * 2);
            // uint8_t num_interface + uint8_t[] ee_interface is always at offset 3
            uint8_t num_interface = *reinterpret_cast<uint8_t *>(eeInfo + sizeof(uint8_t) * 3);
            std::vector<uint8_t> ee_interface;
            for (size_t j = 0; j < num_interface; j++)
                ee_interface.push_back(*reinterpret_cast<uint8_t *>(eeInfo + sizeof(uint8_t) * (4 + j)));
            // uint8_t la_protocol is always at offset -3 from the end of each struct
            uint8_t la_protocol = *reinterpret_cast<uint8_t *>(eeInfo + structSize - sizeof(uint8_t) * 3);
            // uint8_t lb_protocol is always at offset -2 from the end of each struct
            uint8_t lb_protocol = *reinterpret_cast<uint8_t *>(eeInfo + structSize - sizeof(uint8_t) * 2);
            // uint8_t lf_protocol is always at offset -1 from the end of each struct
            uint8_t lf_protocol = *reinterpret_cast<uint8_t *>(eeInfo + structSize - sizeof(uint8_t) * 1);

            // debug info
            std::string str_ee_interfaces;
            for (auto ef : ee_interface)
                str_ee_interfaces += (str_ee_interfaces.empty() ? "" : ",") + std::to_string(ef);
            LOGD("EE found: %x with {status=%d, ee_interface=[%s], la_protocol=%d, lb_protocol=%d, lf_protocol=%d}",
                 ee_handle, ee_status, str_ee_interfaces.c_str(), la_protocol, lb_protocol, lf_protocol);

            auto itProp = std::find(ee_interface.begin(), ee_interface.end(), NCI_NFCEE_INTERFACE_PROPRIETARY);
            bool proprietaryOrNoInterface = itProp != ee_interface.end() || ee_interface.empty();

            // only non-deactivated proprietary EEs or EEs without an interface
            if (ee_status != NFA_EE_STATUS_INACTIVE && proprietaryOrNoInterface)
                result.insert(ee_handle);
        }
    }

    return result;
}

void EEManager::markDeactivated(uint16_t handle) {
    mDeactivated.insert(handle);
}

void EEManager::markActivated(uint16_t handle) {
    mDeactivated.erase(handle);
}

size_t EEManager::countEEs() const {
    std::vector<uint8_t> temp(255 * getApproxStructSize());

    uint8_t num_ee = 255;
    auto rv = globals.hNFA_EeGetInfo->call<def_NFA_EeGetInfo>(
            &num_ee, reinterpret_cast<void*>(temp.data()));
    LOG_ASSERT_S(rv == NCI_STATUS_OK, return 0, "[EEManager] NFA_EeGetInfo failed for count");

    LOGD("[EEManager] Got count: %d", (int)num_ee);
    return num_ee;
}

size_t EEManager::findStructSize(size_t eeCount) const {
    StructSizeProber prober([] (int numEE, uint8_t *dest, size_t size) {
        uint8_t num_ee = numEE;

        auto rv = globals.hNFA_EeGetInfo->call<def_NFA_EeGetInfo>(&num_ee, dest);
        LOG_ASSERT_S(rv == NCI_STATUS_OK, return false, "[EEManager] NFA_EeGetInfo failed for prober");

        return num_ee == numEE;
    });

    // probe and cache struct size if needed
    if (!mStructSize)
        mStructSize = prober.detectStructSize(eeCount, getApproxStructSize());

    // return exact struct size for eeCount > 0
    return mStructSize;
}
