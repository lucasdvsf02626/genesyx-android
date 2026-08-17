# iOS → Android asset transfer

Copied from `genesxy_apple.V1.02` `Assets.xcassets` on 17 Aug 2026 so the Android app can take the same photographs.

## What was actually missing

Android already had the Learn heroes, brand lockup, eggs, and home/page backgrounds. The only new photographs are the **eight recipe plates**. Those are now in:

- `app/src/main/res/drawable-nodpi/recipe_*.jpg` (wired into the app)
- `missing/recipes/` (the same eight files, for a manual hand-off)

| Android drawable | Recipe |
|---|---|
| `recipe_lentil_spinach_lemon_dal` | Lentil, spinach and lemon dal |
| `recipe_ginger_sweet_potato_soup` | Ginger and sweet potato soup |
| `recipe_kefir_berry_breakfast_bowl` | Kefir and berry breakfast bowl |
| `recipe_sprouted_seed_tofu_traybake` | Sprouted seed and tofu traybake |
| `recipe_big_green_quinoa_salad` | Big green salad with quinoa |
| `recipe_rainbow_pepper_bean_bowl` | Rainbow pepper and bean bowl |
| `recipe_dark_chocolate_almond_oat_bars` | Dark chocolate and almond oat bars |
| `recipe_salmon_oats_greens_traybake` | Salmon, oats and greens traybake |

## Same picture, different name (do not copy again)

These iOS stems are already on Android under another filename:

| iOS imageset | Android drawable already present |
|---|---|
| `learn_hero_mucus` | `learn_hero_cervical_mucus` |
| `learn_hero_preconception` | `learn_hero_before_conception` |
| `learn_hero_support` | `learn_hero_ask_for_support` |
| `learn_hero_timing` | `learn_hero_timing_sex` |

The iOS-named copies sit in `missing/learn-heroes-ios-names/` if you want them as aliases. They are **not** in `drawable-nodpi`, so Learn keeps using the existing Android names.

## Full replica

`full-ios-copy/` is every iOS raster (48 files). It is gitignored. Recreate it any time with:

```bash
python3 - <<'PY'
from pathlib import Path
import shutil
src = Path("/Users/lucasvalenca_sf/genesxy_apple.V1.02/App/Genesyx/Resources/Assets.xcassets")
dst = Path("ios-asset-transfer/full-ios-copy")
dst.mkdir(parents=True, exist_ok=True)
for d in src.glob("*.imageset"):
    files = [p for p in d.iterdir() if p.suffix.lower() in {".png",".jpg",".jpeg",".webp"}]
    if files:
        shutil.copy2(files[0], dst / files[0].name)
print("copied", len(list(dst.iterdir())))
PY
```

Drop any extra photos you get later into `missing/` first, then into `app/src/main/res/drawable-nodpi/` using a lowercase underscore name.
