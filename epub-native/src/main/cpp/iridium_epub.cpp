// Iridium EPUB native core.
//
// A focused, bounded EPUB reader: ZIP central directory, raw DEFLATE via zlib,
// and a targeted XML scanner for the OPF / EPUB3 nav / EPUB2 NCX structures the
// library needs. It deliberately does not implement a general XML parser.
//
// Stability contract: every function is bounds-checked, every allocation is
// capped, and no exception crosses the JNI boundary. Failures return null and
// the Kotlin layer falls back to the JVM engine, so native code can never take
// the app down.

#include <jni.h>
#include <zlib.h>

#include <algorithm>
#include <cctype>
#include <cstdint>
#include <cstring>
#include <map>
#include <string>
#include <utility>
#include <vector>

#include <sys/stat.h>
#include <unistd.h>

namespace {

constexpr uint32_t kEocdSig = 0x06054b50u;
constexpr uint32_t kCdSig = 0x02014b50u;
constexpr uint32_t kLocalSig = 0x04034b50u;

constexpr uint64_t kMaxCdBytes = 32ull * 1024 * 1024;
constexpr uint64_t kMaxEntryBytes = 64ull * 1024 * 1024;
constexpr uint64_t kMaxCoverBytes = 16ull * 1024 * 1024;
constexpr uint64_t kMaxNavBytes = 8ull * 1024 * 1024;
constexpr size_t kMaxTocEntries = 2000;
constexpr uint32_t kMaxEntries = 65535;

constexpr uint16_t kMethodStored = 0;
constexpr uint16_t kMethodDeflated = 8;

uint16_t rd16(const uint8_t* p) {
    return static_cast<uint16_t>(p[0] | (p[1] << 8));
}

uint32_t rd32(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) | (static_cast<uint32_t>(p[3]) << 24);
}

bool readAt(int fd, uint64_t offset, size_t length, std::vector<uint8_t>& out) {
    out.assign(length, 0);
    size_t done = 0;
    while (done < length) {
        const ssize_t n = pread(fd, out.data() + done, length - done,
                                static_cast<off_t>(offset + done));
        if (n <= 0) {
            out.resize(done);
            return done == length;
        }
        done += static_cast<size_t>(n);
    }
    return true;
}

struct Entry {
    std::string name;
    uint16_t method = 0;
    uint64_t compressedSize = 0;
    uint64_t uncompressedSize = 0;
    uint64_t localHeaderOffset = 0;
    bool encrypted = false;
};

bool fileSize(int fd, uint64_t& size) {
    struct stat st {};
    if (fstat(fd, &st) != 0) return false;
    if (st.st_size <= 0) return false;
    size = static_cast<uint64_t>(st.st_size);
    return true;
}

bool findEocd(int fd, uint64_t size, uint64_t& eocdOffset) {
    const uint64_t tail = size < (65535ull + 22ull) ? size : (65535ull + 22ull);
    if (tail < 22) return false;
    std::vector<uint8_t> buffer;
    if (!readAt(fd, size - tail, static_cast<size_t>(tail), buffer)) return false;
    if (buffer.size() < 22) return false;
    for (int i = static_cast<int>(buffer.size()) - 22; i >= 0; --i) {
        if (rd32(buffer.data() + i) != kEocdSig) continue;
        const uint16_t commentLength = rd16(buffer.data() + i + 20);
        if (static_cast<uint64_t>(i) + 22 + commentLength == buffer.size()) {
            eocdOffset = size - tail + static_cast<uint64_t>(i);
            return true;
        }
    }
    return false;
}

bool parseCentralDirectory(int fd, uint64_t size, std::vector<Entry>& entries) {
    uint64_t eocdOffset = 0;
    if (!findEocd(fd, size, eocdOffset)) return false;

    std::vector<uint8_t> eocd;
    if (!readAt(fd, eocdOffset, 22, eocd) || eocd.size() < 22) return false;

    const uint64_t cdOffset = rd32(eocd.data() + 16);
    const uint32_t count = rd16(eocd.data() + 10);

    // ZIP64 sentinels: refuse rather than guess. The JVM engine handles these.
    if (cdOffset == 0xFFFFFFFFull || count == 0xFFFF) return false;
    if (cdOffset == 0 || cdOffset >= size) return false;

    uint64_t cdLength = size - cdOffset;
    if (cdLength > kMaxCdBytes) cdLength = kMaxCdBytes;

    std::vector<uint8_t> cd;
    if (!readAt(fd, cdOffset, static_cast<size_t>(cdLength), cd)) return false;
    if (cd.size() < 46) return false;

    size_t p = 0;
    uint32_t parsed = 0;
    while (p + 46 <= cd.size() && parsed < count && parsed < kMaxEntries) {
        if (rd32(cd.data() + p) != kCdSig) break;

        const uint16_t flags = rd16(cd.data() + p + 8);
        const uint16_t method = rd16(cd.data() + p + 10);
        const uint64_t compressedSize = rd32(cd.data() + p + 20);
        const uint64_t uncompressedSize = rd32(cd.data() + p + 24);
        const uint16_t nameLength = rd16(cd.data() + p + 28);
        const uint16_t extraLength = rd16(cd.data() + p + 30);
        const uint16_t commentLength = rd16(cd.data() + p + 32);
        const uint64_t localOffset = rd32(cd.data() + p + 42);

        // ZIP64 sizes/offsets inside the entry: hand back to the JVM engine.
        if (compressedSize == 0xFFFFFFFFull || uncompressedSize == 0xFFFFFFFFull ||
            localOffset == 0xFFFFFFFFull) {
            return false;
        }

        const size_t nameStart = p + 46;
        if (nameStart + nameLength > cd.size()) break;
        std::string name(reinterpret_cast<const char*>(cd.data() + nameStart), nameLength);

        if (!name.empty() && name.back() != '/') {
            Entry entry;
            entry.name = std::move(name);
            entry.method = method;
            entry.compressedSize = compressedSize;
            entry.uncompressedSize = uncompressedSize;
            entry.localHeaderOffset = localOffset;
            entry.encrypted = (flags & 0x0001) != 0;
            entries.push_back(std::move(entry));
        }

        p = nameStart + nameLength + extraLength + commentLength;
        ++parsed;
    }
    return !entries.empty();
}

bool extractEntry(int fd, const Entry& entry, uint64_t maxBytes, std::vector<uint8_t>& out) {
    if (entry.encrypted) return false;
    if (entry.uncompressedSize > maxBytes || entry.compressedSize > maxBytes) return false;

    std::vector<uint8_t> header;
    if (!readAt(fd, entry.localHeaderOffset, 30, header) || header.size() < 30) return false;
    if (rd32(header.data()) != kLocalSig) return false;

    const uint16_t nameLength = rd16(header.data() + 26);
    const uint16_t extraLength = rd16(header.data() + 28);
    const uint64_t dataOffset =
        entry.localHeaderOffset + 30 + nameLength + extraLength;

    std::vector<uint8_t> compressed;
    if (!readAt(fd, dataOffset, static_cast<size_t>(entry.compressedSize), compressed)) {
        return false;
    }
    if (compressed.size() < entry.compressedSize) return false;

    if (entry.method == kMethodStored) {
        if (compressed.size() > maxBytes) return false;
        out = std::move(compressed);
        return true;
    }
    if (entry.method != kMethodDeflated) return false;

    out.clear();
    out.reserve(static_cast<size_t>(
        std::min<uint64_t>(entry.uncompressedSize, maxBytes)));

    z_stream stream{};
    // Raw DEFLATE: no zlib wrapper, so negative window bits.
    if (inflateInit2(&stream, -MAX_WBITS) != Z_OK) return false;
    stream.next_in = compressed.data();
    stream.avail_in = static_cast<uInt>(compressed.size());

    uint8_t buffer[16384];
    int status = Z_OK;
    bool ok = false;
    do {
        stream.next_out = buffer;
        stream.avail_out = sizeof(buffer);
        status = inflate(&stream, Z_NO_FLUSH);
        if (status != Z_OK && status != Z_STREAM_END && status != Z_BUF_ERROR) break;

        const size_t produced = sizeof(buffer) - stream.avail_out;
        if (out.size() + produced > maxBytes) break;
        out.insert(out.end(), buffer, buffer + produced);

        if (status == Z_STREAM_END) {
            ok = true;
            break;
        }
        if (status == Z_BUF_ERROR && produced == 0) break;
    } while (true);

    inflateEnd(&stream);
    return ok;
}

// MARK: XML scanning

std::string trim(const std::string& value) {
    size_t start = 0;
    size_t end = value.size();
    while (start < end && std::isspace(static_cast<unsigned char>(value[start]))) ++start;
    while (end > start && std::isspace(static_cast<unsigned char>(value[end - 1]))) --end;
    return value.substr(start, end - start);
}

std::string collapseWhitespace(const std::string& value) {
    std::string out;
    out.reserve(value.size());
    bool inSpace = false;
    for (char c : value) {
        if (std::isspace(static_cast<unsigned char>(c))) {
            inSpace = true;
        } else {
            if (inSpace && !out.empty()) out.push_back(' ');
            inSpace = false;
            out.push_back(c);
        }
    }
    return out;
}

std::string localName(const std::string& tag) {
    const size_t colon = tag.find(':');
    return colon == std::string::npos ? tag : tag.substr(colon + 1);
}

/** Finds `name="value"` (or single quotes) with an attribute boundary before it. */
std::string attr(const std::string& tag, const std::string& name) {
    size_t pos = 0;
    while ((pos = tag.find(name, pos)) != std::string::npos) {
        // A leading ':' is allowed so namespaced attributes such as
        // `epub:type` and `xlink:href` resolve by their local name.
        const char previous = pos == 0 ? ' ' : tag[pos - 1];
        const bool boundary = std::isspace(static_cast<unsigned char>(previous)) || previous == ':';
        const size_t after = pos + name.size();
        if (!boundary || after >= tag.size() || tag[after] != '=') {
            pos = after;
            continue;
        }
        size_t quoteIdx = after + 1;
        if (quoteIdx >= tag.size()) return {};
        const char quote = tag[quoteIdx];
        if (quote != '"' && quote != '\'') return {};
        const size_t end = tag.find(quote, quoteIdx + 1);
        if (end == std::string::npos) return {};
        return tag.substr(quoteIdx + 1, end - quoteIdx - 1);
    }
    return {};
}

struct Tag {
    std::string name;
    std::string full;
    bool closing = false;
    bool selfClosing = false;
    size_t start = 0;  // offset of the '<'
};

bool nextTag(const std::string& xml, size_t& pos, Tag& tag) {
    while (true) {
        const size_t lt = xml.find('<', pos);
        if (lt == std::string::npos) return false;
        if (lt + 1 >= xml.size()) return false;

        const char next = xml[lt + 1];
        if (next == '!' || next == '?') {
            const size_t gt = xml.find('>', lt);
            if (gt == std::string::npos) return false;
            pos = gt + 1;
            continue;
        }

        const size_t gt = xml.find('>', lt);
        if (gt == std::string::npos) return false;
        tag.start = lt;
        tag.full = xml.substr(lt + 1, gt - lt - 1);
        pos = gt + 1;
        tag.closing = !tag.full.empty() && tag.full[0] == '/';
        tag.selfClosing = !tag.full.empty() && tag.full.back() == '/';
        const size_t start = tag.closing ? 1 : 0;
        const size_t end = tag.full.find_first_of(" \t\r\n/", start);
        tag.name = tag.full.substr(
            start, end == std::string::npos ? std::string::npos : end - start);
        return !tag.name.empty();
    }
}

std::string resolveHref(const std::string& base, const std::string& href) {
    std::string clean = href.substr(0, href.find('#'));
    clean = trim(clean);
    if (clean.empty()) return {};
    if (!clean.empty() && clean[0] == '/') clean.erase(0, 1);
    if (base.empty()) return clean;

    std::vector<std::string> parts;
    auto push = [&parts](const std::string& path) {
        size_t start = 0;
        while (start <= path.size()) {
            const size_t slash = path.find('/', start);
            const size_t end = slash == std::string::npos ? path.size() : slash;
            const std::string segment = path.substr(start, end - start);
            if (segment.empty() || segment == ".") {
                // skip
            } else if (segment == "..") {
                if (!parts.empty()) parts.pop_back();
            } else {
                parts.push_back(segment);
            }
            if (slash == std::string::npos) break;
            start = slash + 1;
        }
    };
    push(base);
    push(clean);

    std::string out;
    for (size_t i = 0; i < parts.size(); ++i) {
        if (i > 0) out.push_back('/');
        out += parts[i];
    }
    return out;
}

std::string baseDir(const std::string& path) {
    const size_t slash = path.find_last_of('/');
    return slash == std::string::npos ? std::string() : path.substr(0, slash);
}

// MARK: OPF

struct ManifestItem {
    std::string id;
    std::string href;
    std::string mediaType;
    bool nav = false;
    bool coverImage = false;
};

struct OpfData {
    std::string title;
    std::string author;
    std::string coverId;
    std::string guideCover;
    std::vector<ManifestItem> items;
    std::vector<std::string> spine;
};

bool parseOpf(const std::string& xml, OpfData& out) {
    size_t pos = 0;
    Tag tag;
    while (nextTag(xml, pos, tag)) {
        const std::string local = localName(tag.name);
        if (tag.closing) continue;

        if (local == "item") {
            ManifestItem item;
            item.id = attr(tag.full, "id");
            item.href = attr(tag.full, "href");
            item.mediaType = attr(tag.full, "media-type");
            const std::string properties = attr(tag.full, "properties");
            item.nav = properties.find("nav") != std::string::npos;
            item.coverImage = properties.find("cover-image") != std::string::npos;
            if (!item.id.empty() && !item.href.empty()) {
                out.items.push_back(std::move(item));
            }
        } else if (local == "itemref") {
            const std::string idref = attr(tag.full, "idref");
            if (!idref.empty()) out.spine.push_back(idref);
        } else if (local == "meta") {
            if (attr(tag.full, "name") == "cover") {
                out.coverId = attr(tag.full, "content");
            }
        } else if (local == "reference") {
            if (attr(tag.full, "type") == "cover") {
                out.guideCover = attr(tag.full, "href");
            }
        } else if (local == "title" || local == "creator") {
            const std::string closingTag = "</" + tag.name;
            const size_t close = xml.find(closingTag, pos);
            const std::string text = collapseWhitespace(trim(
                close == std::string::npos ? std::string() : xml.substr(pos, close - pos)));
            if (local == "title") {
                if (out.title.empty()) out.title = text;
            } else {
                if (out.author.empty()) out.author = text;
            }
            if (close != std::string::npos) {
                const size_t gt = xml.find('>', close);
                pos = gt == std::string::npos ? pos : gt + 1;
            }
        }

        if (out.items.size() > kMaxEntries || out.spine.size() > kMaxEntries) break;
    }
    return !out.items.empty();
}

// MARK: TOC

using Chapter = std::pair<std::string, std::string>;  // href, title

void parseNav(const std::string& xml, std::vector<Chapter>& out) {
    size_t pos = 0;
    Tag tag;
    bool inToc = false;
    bool inAnchor = false;
    std::string anchorHref;
    size_t anchorTextStart = 0;

    while (nextTag(xml, pos, tag)) {
        const std::string local = localName(tag.name);
        if (!inToc) {
            if (!tag.closing && local == "nav") {
                const std::string type = attr(tag.full, "type");
                if (type.find("toc") != std::string::npos) inToc = true;
            }
            continue;
        }
        if (tag.closing && local == "nav") break;

        if (!tag.closing && local == "a") {
            inAnchor = true;
            anchorHref = attr(tag.full, "href");
            anchorTextStart = pos;
        } else if (tag.closing && local == "a" && inAnchor) {
            // The link label is the text between the opening and closing tags.
            const std::string text = collapseWhitespace(trim(
                xml.substr(anchorTextStart, tag.start - anchorTextStart)));
            if (!anchorHref.empty() && !text.empty() && out.size() < kMaxTocEntries) {
                out.emplace_back(anchorHref, text);
            }
            inAnchor = false;
            anchorHref.clear();
        }
    }
}

void parseNcx(const std::string& xml, std::vector<Chapter>& out) {
    size_t pos = 0;
    Tag tag;
    std::string label;
    bool capturing = false;
    size_t captureStart = 0;

    while (nextTag(xml, pos, tag)) {
        const std::string local = localName(tag.name);
        if (!tag.closing && local == "text" && !capturing) {
            capturing = true;
            captureStart = pos;
        } else if (tag.closing && local == "text" && capturing) {
            label = collapseWhitespace(trim(xml.substr(captureStart, tag.full.size())));
            // Recompute from the opening tag boundary: take text before "</text".
            const size_t close = xml.find("</", captureStart);
            if (close != std::string::npos) {
                label = collapseWhitespace(trim(xml.substr(captureStart, close - captureStart)));
            }
            capturing = false;
        } else if (!tag.closing && local == "content") {
            const std::string src = attr(tag.full, "src");
            if (!src.empty() && out.size() < kMaxTocEntries) {
                out.emplace_back(src, label);
            }
            label.clear();
        }
    }
}

// MARK: Result assembly

struct Result {
    std::string title;
    std::string author;
    std::string coverMime;
    std::string coverHref;
    std::vector<Chapter> chapters;
    int spineCount = 0;
};

std::string jsonEscape(const std::string& value) {
    std::string out;
    out.reserve(value.size() + 8);
    for (unsigned char c : value) {
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (c < 0x20) {
                    char buffer[8];
                    std::snprintf(buffer, sizeof(buffer), "\\u%04x", c);
                    out += buffer;
                } else {
                    out.push_back(static_cast<char>(c));
                }
        }
    }
    return out;
}

bool findEntry(const std::vector<Entry>& entries, const std::string& name, Entry& out) {
    for (const Entry& entry : entries) {
        if (entry.name == name) {
            out = entry;
            return true;
        }
    }
    return false;
}

bool buildResult(int fd, Result& result) {
    uint64_t size = 0;
    if (!fileSize(fd, size)) return false;

    std::vector<Entry> entries;
    if (!parseCentralDirectory(fd, size, entries)) return false;

    // container.xml -> OPF path
    std::string opfPath;
    Entry containerEntry;
    if (findEntry(entries, "META-INF/container.xml", containerEntry)) {
        std::vector<uint8_t> bytes;
        if (extractEntry(fd, containerEntry, 1024 * 1024, bytes)) {
            const std::string xml(reinterpret_cast<const char*>(bytes.data()), bytes.size());
            size_t pos = 0;
            Tag tag;
            while (nextTag(xml, pos, tag)) {
                if (localName(tag.name) == "rootfile") {
                    opfPath = attr(tag.full, "full-path");
                    if (!opfPath.empty()) break;
                }
            }
        }
    }
    if (opfPath.empty()) {
        for (const Entry& entry : entries) {
            if (entry.name.size() > 4 &&
                entry.name.compare(entry.name.size() - 4, 4, ".opf") == 0) {
                opfPath = entry.name;
                break;
            }
        }
    }
    if (opfPath.empty()) return false;

    Entry opfEntry;
    if (!findEntry(entries, opfPath, opfEntry)) return false;
    std::vector<uint8_t> opfBytes;
    if (!extractEntry(fd, opfEntry, 8 * 1024 * 1024, opfBytes)) return false;

    const std::string opfXml(reinterpret_cast<const char*>(opfBytes.data()), opfBytes.size());
    OpfData opf;
    if (!parseOpf(opfXml, opf)) return false;

    const std::string base = baseDir(opfPath);
    result.title = opf.title;
    result.author = opf.author;
    result.spineCount = static_cast<int>(opf.spine.size());

    // Chapters: EPUB3 nav, else EPUB2 NCX, else spine fallback.
    std::vector<Chapter> chapters;
    const ManifestItem* navItem = nullptr;
    const ManifestItem* ncxItem = nullptr;
    for (const ManifestItem& item : opf.items) {
        if (!navItem && item.nav) navItem = &item;
        if (!ncxItem && item.mediaType == "application/x-dtbncx+xml") ncxItem = &item;
    }
    if (navItem != nullptr) {
        const std::string navPath = resolveHref(base, navItem->href);
        Entry navEntry;
        std::vector<uint8_t> bytes;
        if (findEntry(entries, navPath, navEntry) &&
            extractEntry(fd, navEntry, kMaxNavBytes, bytes)) {
            const std::string navXml(reinterpret_cast<const char*>(bytes.data()), bytes.size());
            std::vector<Chapter> raw;
            parseNav(navXml, raw);
            const std::string navBase = baseDir(navPath);
            for (const auto& chapter : raw) {
                chapters.emplace_back(resolveHref(navBase, chapter.first), chapter.second);
            }
        }
    }
    if (chapters.empty() && ncxItem != nullptr) {
        const std::string ncxPath = resolveHref(base, ncxItem->href);
        Entry ncxEntry;
        std::vector<uint8_t> bytes;
        if (findEntry(entries, ncxPath, ncxEntry) &&
            extractEntry(fd, ncxEntry, kMaxNavBytes, bytes)) {
            const std::string ncxXml(reinterpret_cast<const char*>(bytes.data()), bytes.size());
            std::vector<Chapter> raw;
            parseNcx(ncxXml, raw);
            const std::string ncxBase = baseDir(ncxPath);
            for (const auto& chapter : raw) {
                chapters.emplace_back(resolveHref(ncxBase, chapter.first), chapter.second);
            }
        }
    }
    if (chapters.empty()) {
        std::map<std::string, const ManifestItem*> byId;
        for (const ManifestItem& item : opf.items) byId[item.id] = &item;
        for (const std::string& id : opf.spine) {
            auto it = byId.find(id);
            if (it == byId.end()) continue;
            const std::string href = resolveHref(base, it->second->href);
            std::string label = href;
            const size_t slash = label.find_last_of('/');
            if (slash != std::string::npos) label = label.substr(slash + 1);
            const size_t hash = label.find('#');
            if (hash != std::string::npos) label = label.substr(0, hash);
            if (label.empty()) label = "Chapter";
            chapters.emplace_back(href, label);
        }
    }
    result.chapters = std::move(chapters);

    // Cover: properties, meta id, guide, else first image.
    const ManifestItem* coverItem = nullptr;
    for (const ManifestItem& item : opf.items) {
        if (item.coverImage) { coverItem = &item; break; }
    }
    if (coverItem == nullptr && !opf.coverId.empty()) {
        for (const ManifestItem& item : opf.items) {
            if (item.id == opf.coverId) { coverItem = &item; break; }
        }
    }
    if (coverItem == nullptr && !opf.guideCover.empty()) {
        result.coverHref = resolveHref(base, opf.guideCover);
    } else if (coverItem != nullptr) {
        result.coverHref = resolveHref(base, coverItem->href);
        result.coverMime = coverItem->mediaType;
    }
    if (result.coverHref.empty()) {
        for (const ManifestItem& item : opf.items) {
            if (item.mediaType.rfind("image/", 0) == 0) {
                result.coverHref = resolveHref(base, item.href);
                result.coverMime = item.mediaType;
                break;
            }
        }
    }
    return true;
}

std::string resultToJson(const Result& result) {
    std::string json = "{";
    json += "\"title\":\"" + jsonEscape(result.title) + "\",";
    json += "\"author\":\"" + jsonEscape(result.author) + "\",";
    json += "\"coverMime\":\"" + jsonEscape(result.coverMime) + "\",";
    json += "\"spineCount\":" + std::to_string(result.spineCount) + ",";
    json += "\"chapters\":[";
    for (size_t i = 0; i < result.chapters.size(); ++i) {
        if (i > 0) json += ",";
        json += "{\"href\":\"" + jsonEscape(result.chapters[i].first) + "\",";
        json += "\"title\":\"" + jsonEscape(result.chapters[i].second) + "\"}";
    }
    json += "]}";
    return json;
}

jbyteArray toByteArray(JNIEnv* env, const std::vector<uint8_t>& bytes) {
    if (bytes.empty()) return nullptr;
    jbyteArray array = env->NewByteArray(static_cast<jsize>(bytes.size()));
    if (array == nullptr) return nullptr;
    env->SetByteArrayRegion(array, 0, static_cast<jsize>(bytes.size()),
                            reinterpret_cast<const jbyte*>(bytes.data()));
    return array;
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_iridium_epub_nativecore_NativeEpub_nativeInspect(JNIEnv* env, jobject /*thiz*/, jint fd) {
    Result result;
    if (!buildResult(fd, result)) return nullptr;
    const std::string json = resultToJson(result);
    return env->NewStringUTF(json.c_str());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_iridium_epub_nativecore_NativeEpub_nativeCover(JNIEnv* env, jobject /*thiz*/, jint fd) {
    Result result;
    if (!buildResult(fd, result) || result.coverHref.empty()) return nullptr;

    uint64_t size = 0;
    if (!fileSize(fd, size)) return nullptr;
    std::vector<Entry> entries;
    if (!parseCentralDirectory(fd, size, entries)) return nullptr;

    Entry coverEntry;
    if (!findEntry(entries, result.coverHref, coverEntry)) return nullptr;

    std::vector<uint8_t> bytes;
    if (!extractEntry(fd, coverEntry, kMaxCoverBytes, bytes)) return nullptr;
    return toByteArray(env, bytes);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_iridium_epub_nativecore_NativeEpub_nativeChapter(JNIEnv* env, jobject /*thiz*/, jint fd,
                                                      jstring name) {
    if (name == nullptr) return nullptr;
    const char* chars = env->GetStringUTFChars(name, nullptr);
    if (chars == nullptr) return nullptr;
    const std::string href(chars);
    env->ReleaseStringUTFChars(name, chars);

    uint64_t size = 0;
    if (!fileSize(fd, size)) return nullptr;
    std::vector<Entry> entries;
    if (!parseCentralDirectory(fd, size, entries)) return nullptr;

    Entry entry;
    if (!findEntry(entries, href, entry)) return nullptr;

    std::vector<uint8_t> bytes;
    if (!extractEntry(fd, entry, kMaxEntryBytes, bytes)) return nullptr;
    return toByteArray(env, bytes);
}
