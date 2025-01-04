#ifndef NFCD_MAPINFO_H
#define NFCD_MAPINFO_H

#include <set>
#include <string>
#include <unordered_map>
#include <vector>

class MapInfo {
public:
    struct RangeData {
        RangeData(uintptr_t s, uintptr_t e, uint8_t p) : start(s), end(e), perms(p) { }

        uintptr_t start, end;
        uint8_t perms;
    };
    struct LibraryData {
        explicit LibraryData(const std::string &l) : label(l) { }

        uintptr_t base = 0;
        std::string label;
        std::vector<RangeData> ranges;
    };
    struct LookupResult {
        operator bool() const { // NOLINT(*-explicit-constructor)
            return library && range;
        }

        const LibraryData *library;
        const RangeData *range;
    };

    bool create();

    std::set<std::string> loadedLibraries() const;
    void *getBaseAddress(const std::string &library) const;
    LookupResult lookupRange(uintptr_t address, uint64_t size = 0) const;

protected:
    std::unordered_map<std::string, LibraryData> mLibraryData;
};

#endif //NFCD_MAPINFO_H
