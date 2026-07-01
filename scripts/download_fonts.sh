#!/usr/bin/env bash
# Downloads Outfit + Inter font weights into app/src/main/res/font/
# Run from repo root: bash scripts/download_fonts.sh
# After this: uncomment the font families in app/src/main/kotlin/com/genesyx/app/ui/theme/Type.kt

set -e
FONT_DIR="app/src/main/res/font"
mkdir -p "$FONT_DIR"

BASE="https://github.com/google/fonts/raw/main"

echo "Downloading Outfit..."
curl -fsSL "$BASE/ofl/outfit/Outfit%5Bwght%5D.ttf" -o /tmp/Outfit.ttf
# Split variable font into static weights using fonttools (pip install fonttools)
# OR just download static builds:
curl -fsSL "https://github.com/Outfitio/Outfit-Fonts/raw/main/fonts/static/TTF/Outfit-Regular.ttf"  -o "$FONT_DIR/outfit_regular.ttf"
curl -fsSL "https://github.com/Outfitio/Outfit-Fonts/raw/main/fonts/static/TTF/Outfit-Medium.ttf"   -o "$FONT_DIR/outfit_medium.ttf"
curl -fsSL "https://github.com/Outfitio/Outfit-Fonts/raw/main/fonts/static/TTF/Outfit-SemiBold.ttf" -o "$FONT_DIR/outfit_semibold.ttf"
curl -fsSL "https://github.com/Outfitio/Outfit-Fonts/raw/main/fonts/static/TTF/Outfit-Bold.ttf"     -o "$FONT_DIR/outfit_bold.ttf"

echo "Downloading Inter..."
curl -fsSL "https://github.com/rsms/inter/raw/master/docs/font-files/Inter-Regular.ttf"  -o "$FONT_DIR/inter_regular.ttf"
curl -fsSL "https://github.com/rsms/inter/raw/master/docs/font-files/Inter-Medium.ttf"   -o "$FONT_DIR/inter_medium.ttf"
curl -fsSL "https://github.com/rsms/inter/raw/master/docs/font-files/Inter-SemiBold.ttf" -o "$FONT_DIR/inter_semibold.ttf"

echo ""
echo "Done. Files in $FONT_DIR:"
ls -lh "$FONT_DIR"
echo ""
echo "Next: in Type.kt, swap the two lines:"
echo "  DisplayFamily = FontFamily.SansSerif  →  DisplayFamily = outfitFamily"
echo "  BodyFamily    = FontFamily.SansSerif  →  BodyFamily    = interFamily"
echo "and uncomment the font family declarations above them."
