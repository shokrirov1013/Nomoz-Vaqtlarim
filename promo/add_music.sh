#!/usr/bin/env bash
# Videoga o'z musiqangizni qo'yish (masalan, Magnific "Arabic Roots" trekini).
# Ishlatish:  ./add_music.sh <video.mp4> <musiqa.mp3> [boshlanish_soniya=0] [natija.mp4]
#   boshlanish_soniya — treking qaysi joyidan olish (masalan 12). Video 38 soniya; musiqa 1.2 s da kuchayadi,
#   oxirgi 2.8 s da so'nadi.
set -euo pipefail
VIDEO="${1:?video fayli}"; TRACK="${2:?musiqa fayli}"; START="${3:-0}"; OUT="${4:-promo_final.mp4}"
ffmpeg -y -loglevel error -i "$VIDEO" -ss "$START" -t 38 -i "$TRACK" \
  -filter_complex "[1:a]afade=t=in:st=0:d=1.2,afade=t=out:st=35.2:d=2.8,loudnorm=I=-16:TP=-1.5[a]" \
  -map 0:v -map "[a]" -c:v copy -c:a aac -b:a 192k -shortest "$OUT"
echo "tayyor: $OUT"
