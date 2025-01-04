#include <nfcd/helper/MapInfo.h>
#include <nfcd/nfcd.h>

#include <fstream>
#include <regex>

#include <link.h>

bool MapInfo::create() {
    int rv = dl_iterate_phdr([] (struct dl_phdr_info *info, size_t, void *user_data) {
        auto *instance = (MapInfo *)user_data;

        // map library name to new library entry with base address
        auto &entry = *instance->mLibraryData.try_emplace(info->dlpi_name, info->dlpi_name).first;
        for (size_t i = 0; i < info->dlpi_phnum; i++) {
            // set base address to relocation + PT_LOAD vaddr if not already set
            // PT_LOAD headers are sorted in ascending vaddr order so using the first is correct
            if (info->dlpi_phdr[i].p_type == PT_LOAD && entry.second.base == 0)
                entry.second.base = info->dlpi_addr + info->dlpi_phdr[i].p_vaddr;

            entry.second.ranges.emplace_back(
                    // range start address
                    info->dlpi_addr + info->dlpi_phdr[i].p_vaddr,
                    // range end address
                    info->dlpi_addr + info->dlpi_phdr[i].p_vaddr + info->dlpi_phdr[i].p_memsz,
                    // range permissions
                    static_cast<uint8_t>(info->dlpi_phdr[i].p_flags)
            );
        }

        return 0;
    }, this);
    LOG_ASSERT_S(rv == 0, return false, "Error iterating dl program header");

    return true;
}

std::set<std::string> MapInfo::loadedLibraries() const {
    std::set<std::string> result;

    for (auto &it : mLibraryData)
        result.emplace(it.first);

    return result;
}

void *MapInfo::getBaseAddress(const std::string &library) const {
    auto it = mLibraryData.find(library);
    if (it != mLibraryData.end())
        return reinterpret_cast<void*>(it->second.base);

    return nullptr;
}

MapInfo::LookupResult MapInfo::lookupRange(uintptr_t address, uint64_t size) const {
    for (auto &it : mLibraryData)
        for (auto &range : it.second.ranges)
            if (address >= range.start && (address + size) <= range.end)
                return {&it.second, &range};

    return {};
}
