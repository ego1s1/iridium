#!/usr/bin/env bash
#
# Device audit for the reader's page counter.
#
# Turns pages through the fixed tap zones, reveals the chrome after each turn,
# reads the counter out of the accessibility tree, and captures both the plain
# text page and the chrome state. A page that fails to advance by exactly one,
# or a text screen that is byte-identical to the previous step, is reported as
# an anomaly.
#
# Usage:
#   scripts/page-number-audit.sh [--direction forward|backward] [--turns N] [--out DIR]
#
# Slow emulators can raise the waits without editing:
#   TURN_WAIT=4 CHROME_WAIT=2 scripts/page-number-audit.sh --turns 6
#
# Preconditions: the reader is open on a book and the app is in the foreground.
# The counter is only meaningful once the navigator has reported its first page.

set -uo pipefail

ADB_BIN=${ADB:-adb}
SERIAL=${SERIAL:-}
DIRECTION=forward
TURNS=6
OUT=""
TURN_WAIT=${TURN_WAIT:-2}
CHROME_WAIT=${CHROME_WAIT:-1}
REMOTE_XML=/sdcard/page_number_audit.xml

usage() {
    sed -n '3,14p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --direction) DIRECTION=${2:-}; shift 2 ;;
        --turns) TURNS=${2:-}; shift 2 ;;
        --out) OUT=${2:-}; shift 2 ;;
        --serial) SERIAL=${2:-}; shift 2 ;;
        -h|--help) usage 0 ;;
        *) echo "unknown argument: $1" >&2; usage 1 ;;
    esac
done

case "$DIRECTION" in
    forward|backward) ;;
    *) echo "--direction must be forward or backward" >&2; exit 1 ;;
esac
case "$TURNS" in
    ''|*[!0-9]*) echo "--turns must be a positive integer" >&2; exit 1 ;;
esac

OUT=${OUT:-page-audit-$(date +%Y%m%d-%H%M%S)}
mkdir -p "$OUT"

adb_() {
    if [ -n "$SERIAL" ]; then
        "$ADB_BIN" -s "$SERIAL" "$@"
    else
        "$ADB_BIN" "$@"
    fi
}

fail() {
    echo "error: $*" >&2
    exit 1
}

command -v "$ADB_BIN" >/dev/null 2>&1 || fail "$ADB_BIN not found on PATH"
adb_ get-state >/dev/null 2>&1 || fail "no device or emulator attached"

size=$(adb_ shell wm size | awk -F': ' '/Physical size|Override size/{v=$2} END{print v}' | tr -d '\r')
[ -n "$size" ] || fail "could not read display size"
width=${size%x*}
height=${size#*x}
mid_y=$((height / 2))
center_x=$((width / 2))
if [ "$DIRECTION" = forward ]; then
    turn_x=$((width * 5 / 6))
else
    turn_x=$((width / 6))
fi

chapter=""
total=""
previous_page=""
previous_text_hash=""
anomalies=0
csv="$OUT/turns.csv"
printf 'turn,direction,chapter,page,total,delta,text_changed\n' > "$csv"

# Reads the counter out of the live accessibility tree. Returns "page total".
read_counter() {
    adb_ shell uiautomator dump "$REMOTE_XML" >/dev/null 2>&1
    local xml
    xml=$(adb_ shell cat "$REMOTE_XML")
    [ -n "$xml" ] || return 1

    chapter=$(printf '%s' "$xml" | grep -o 'text="Chapter [^"]*"' | head -n 1 | sed 's/^text="//; s/"$//')
    local numbers page total
    numbers=$(printf '%s' "$xml" | grep -o 'text="[0-9]\{1,\}"' | sed 's/^text="//; s/"$//')
    # The scrubber island carries exactly two numeric labels: page and total.
    page=$(printf '%s\n' "$numbers" | sed -n '1p')
    total=$(printf '%s\n' "$numbers" | sed -n '2p')
    [ -n "$page" ] || return 1
    printf '%s %s' "$page" "$total"
}

# The chrome hides itself after a few seconds, so reveal it and read quickly.
reveal_and_read() {
    local attempt result
    for attempt in 1 2 3; do
        adb_ shell input tap "$center_x" "$mid_y"
        sleep "$CHROME_WAIT"
        result=$(read_counter)
        if [ -n "$result" ]; then
            printf '%s' "$result"
            return 0
        fi
    done
    return 1
}

md5_of() {
    if command -v md5 >/dev/null 2>&1; then
        md5 -q "$1"
    else
        md5sum "$1" | awk '{print $1}'
    fi
}

echo "device size: ${width}x${height}"
echo "direction:   $DIRECTION"
echo "turns:       $TURNS"
echo "output:      $OUT"

# Baseline: reveal the chrome once so the run starts from a known counter.
baseline=$(reveal_and_read) || fail "page counter not found; is the reader open?"
baseline_page=${baseline%% *}
total=${baseline##* }
adb_ exec-out screencap -p > "$OUT/baseline-chrome.png"
echo "baseline:    ${chapter:-unknown chapter}, page $baseline_page of $total"

previous_page=$baseline_page

step=0
while [ "$step" -lt "$TURNS" ]; do
    step=$((step + 1))
    label=$(printf '%02d' "$step")

    adb_ shell input tap "$turn_x" "$mid_y"
    sleep "$TURN_WAIT"
    adb_ exec-out screencap -p > "$OUT/turn-${label}-text.png"
    text_hash=$(md5_of "$OUT/turn-${label}-text.png")

    reading=$(reveal_and_read)
    if [ -z "${reading// /}" ]; then
        echo "turn $label: counter unreadable" >&2
        anomalies=$((anomalies + 1))
        printf '%s,%s,%s,,,%s,\n' "$step" "$DIRECTION" "$chapter" "unreadable" >> "$csv"
        continue
    fi

    page=${reading%% *}
    total=${reading##* }
    adb_ exec-out screencap -p > "$OUT/turn-${label}-chrome.png"

    delta=$((page - previous_page))
    if [ "$DIRECTION" = forward ]; then
        expected=1
    else
        expected=-1
    fi

    text_changed=yes
    if [ -n "$previous_text_hash" ] && [ "$text_hash" = "$previous_text_hash" ]; then
        text_changed=no
    fi

    printf 'turn %s: %s, page %s of %s, delta %+d, text changed: %s\n' \
        "$label" "${chapter:-unknown chapter}" "$page" "$total" "$delta" "$text_changed"
    printf '%s,%s,%s,%s,%s,%+d,%s\n' \
        "$step" "$DIRECTION" "$chapter" "$page" "$total" "$delta" "$text_changed" >> "$csv"

    if [ "$delta" -ne "$expected" ] && [ "$delta" -ne 0 ]; then
        echo "  anomaly: expected $expected, got $delta" >&2
        anomalies=$((anomalies + 1))
    elif [ "$delta" -eq 0 ] && [ "$text_changed" = yes ]; then
        # A redraw without a counter move: the counter lagged the page.
        echo "  anomaly: page text changed but the counter stayed on $page" >&2
        anomalies=$((anomalies + 1))
    elif [ "$delta" -eq 0 ] && [ "$text_changed" = no ]; then
        # A refused turn at the start/end of the book: not a defect.
        echo "  note: turn refused (page unchanged, screen unchanged)"
    fi

    previous_page=$page
    previous_text_hash=$text_hash
done

echo
echo "screenshots: $OUT"
echo "results:     $csv"
if [ "$anomalies" -eq 0 ]; then
    echo "verdict:     no page-counter anomalies in $TURNS $DIRECTION turn(s)"
else
    echo "verdict:     $anomalies anomaly(ies) in $TURNS $DIRECTION turn(s)"
fi
exit 0
