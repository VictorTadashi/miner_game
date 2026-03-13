"""
Generate a pixel-art miner sprite (minerador.png) for the Cave Miner game.
Based on the user's reference image: miner with yellow helmet, red beard,
blue shirt, brown overalls, dark boots, and pickaxe in hand.
Output: 32x40 pixel sprite with transparency.
"""
from PIL import Image

# Canvas: 32 wide x 40 tall
W, H = 32, 40
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
px = img.load()

# Color palette (matching the reference image)
TRANSPARENT = (0, 0, 0, 0)
HELMET_YELLOW = (220, 190, 40, 255)
HELMET_DARK = (180, 155, 30, 255)
LAMP_BODY = (160, 160, 170, 255)
LAMP_GLOW = (255, 255, 200, 255)
SKIN = (235, 190, 145, 255)
SKIN_SHADOW = (210, 165, 120, 255)
BEARD_ORANGE = (200, 100, 40, 255)
BEARD_DARK = (170, 80, 30, 255)
EYE_WHITE = (255, 255, 255, 255)
EYE_PUPIL = (40, 30, 20, 255)
SHIRT_BLUE = (70, 100, 155, 255)
SHIRT_DARK = (55, 80, 130, 255)
OVERALL_BROWN = (120, 85, 50, 255)
OVERALL_DARK = (95, 65, 35, 255)
BELT_BROWN = (100, 70, 40, 255)
BUCKLE_GOLD = (210, 185, 60, 255)
BOOT_DARK = (50, 40, 35, 255)
BOOT_SOLE = (35, 28, 22, 255)
PICK_HANDLE = (140, 100, 50, 255)
PICK_HEAD = (170, 170, 180, 255)
PICK_EDGE = (200, 200, 210, 255)
STRAP_GOLD = (190, 170, 50, 255)

def fill_rect(x0, y0, w, h, color):
    for yy in range(y0, min(y0 + h, H)):
        for xx in range(x0, min(x0 + w, W)):
            if 0 <= xx < W and 0 <= yy < H:
                px[xx, yy] = color

def set_px(x, y, color):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = color

# ============================================================
# HELMET (rows 0-7)
# ============================================================
# Helmet dome (wider brim)
for x in range(8, 24):
    set_px(x, 4, HELMET_YELLOW)
    set_px(x, 5, HELMET_YELLOW)
# Top of helmet dome
for x in range(10, 22):
    set_px(x, 2, HELMET_YELLOW)
    set_px(x, 3, HELMET_YELLOW)
for x in range(12, 20):
    set_px(x, 1, HELMET_YELLOW)
# Brim (wider)
for x in range(7, 25):
    set_px(x, 6, HELMET_DARK)
    set_px(x, 7, HELMET_DARK)

# Lamp on helmet (right side)
for x in range(21, 25):
    set_px(x, 2, LAMP_BODY)
    set_px(x, 3, LAMP_BODY)
set_px(22, 1, LAMP_GLOW)
set_px(23, 1, LAMP_GLOW)
set_px(22, 0, LAMP_GLOW)
set_px(23, 0, LAMP_GLOW)

# ============================================================
# HEAD / FACE (rows 7-15)
# ============================================================
# Face shape
for y in range(8, 15):
    for x in range(9, 23):
        set_px(x, y, SKIN)
# Face shadow
for y in range(8, 15):
    set_px(9, y, SKIN_SHADOW)
    set_px(22, y, SKIN_SHADOW)

# Eyes (row 9-10)
set_px(13, 9, EYE_WHITE)
set_px(14, 9, EYE_WHITE)
set_px(18, 9, EYE_WHITE)
set_px(19, 9, EYE_WHITE)
set_px(13, 10, EYE_WHITE)
set_px(14, 10, EYE_WHITE)
set_px(18, 10, EYE_WHITE)
set_px(19, 10, EYE_WHITE)
# Pupils
set_px(14, 10, EYE_PUPIL)
set_px(19, 10, EYE_PUPIL)

# Nose
set_px(16, 11, SKIN_SHADOW)

# Beard (rows 12-17) — orange/red beard
for y in range(13, 18):
    w_beard = 14 - (y - 13)  # Gets narrower
    start = 16 - w_beard // 2
    for x in range(start, start + w_beard):
        if 8 <= x <= 23:
            color = BEARD_ORANGE if (x + y) % 3 != 0 else BEARD_DARK
            set_px(x, y, color)
# Wider beard at jaw
for x in range(10, 22):
    set_px(x, 13, BEARD_ORANGE)
for x in range(10, 22):
    set_px(x, 14, BEARD_ORANGE if x % 2 == 0 else BEARD_DARK)
for x in range(11, 21):
    set_px(x, 15, BEARD_ORANGE)
for x in range(12, 20):
    set_px(x, 16, BEARD_DARK)
for x in range(13, 19):
    set_px(x, 17, BEARD_DARK)

# ============================================================
# SHIRT / TORSO (rows 16-22)
# ============================================================
for y in range(16, 23):
    for x in range(8, 24):
        set_px(x, y, SHIRT_BLUE)
# Shirt shadow
for y in range(16, 23):
    set_px(8, y, SHIRT_DARK)
    set_px(23, y, SHIRT_DARK)
# Collar area
for x in range(12, 20):
    set_px(x, 16, SHIRT_DARK)

# ============================================================
# OVERALLS (rows 22-33)
# ============================================================
# Straps
for y in range(18, 23):
    set_px(11, y, STRAP_GOLD)
    set_px(12, y, STRAP_GOLD)
    set_px(19, y, STRAP_GOLD)
    set_px(20, y, STRAP_GOLD)

# Overall body
for y in range(23, 33):
    for x in range(8, 24):
        set_px(x, y, OVERALL_BROWN)
# Overall shadow
for y in range(23, 33):
    set_px(8, y, OVERALL_DARK)
    set_px(23, y, OVERALL_DARK)

# Belt (row 23-24)
for x in range(8, 24):
    set_px(x, 23, BELT_BROWN)
    set_px(x, 24, BELT_BROWN)
# Buckle
for x in range(14, 18):
    set_px(x, 23, BUCKLE_GOLD)
    set_px(x, 24, BUCKLE_GOLD)

# Overall legs separation
for y in range(28, 33):
    set_px(15, y, OVERALL_DARK)
    set_px(16, y, OVERALL_DARK)

# ============================================================
# BOOTS (rows 33-39)
# ============================================================
# Left boot
for y in range(33, 38):
    for x in range(8, 15):
        set_px(x, y, BOOT_DARK)
for x in range(7, 15):
    set_px(x, 38, BOOT_SOLE)
    set_px(x, 39, BOOT_SOLE)

# Right boot
for y in range(33, 38):
    for x in range(17, 24):
        set_px(x, y, BOOT_DARK)
for x in range(17, 25):
    set_px(x, 38, BOOT_SOLE)
    set_px(x, 39, BOOT_SOLE)

# ============================================================
# LEFT ARM (shirt color, rows 17-25)
# ============================================================
for y in range(17, 26):
    for x in range(4, 8):
        set_px(x, y, SHIRT_BLUE if y < 22 else SKIN)
# Hand
for y in range(24, 27):
    for x in range(4, 8):
        set_px(x, y, SKIN)

# ============================================================
# RIGHT ARM + PICKAXE (rows 17-39)
# ============================================================
# Right arm
for y in range(17, 26):
    for x in range(24, 28):
        set_px(x, y, SHIRT_BLUE if y < 22 else SKIN)
# Hand gripping pickaxe
for y in range(24, 27):
    for x in range(24, 28):
        set_px(x, y, SKIN)

# Pickaxe handle (vertical, from hand downward)
for y in range(14, 36):
    set_px(26, y, PICK_HANDLE)
    set_px(27, y, PICK_HANDLE)

# Pickaxe head (at top of handle, pointing right)
# Metal head shape
for x in range(28, 32):
    set_px(x, 13, PICK_HEAD)
    set_px(x, 14, PICK_HEAD)
    set_px(x, 15, PICK_HEAD)
# Pointed tip
set_px(31, 12, PICK_EDGE)
set_px(30, 12, PICK_HEAD)
# Back of head
for x in range(28, 31):
    set_px(x, 16, PICK_HEAD)
# Sharp edge highlight
set_px(31, 13, PICK_EDGE)
set_px(31, 14, PICK_EDGE)

# ============================================================
# Save
# ============================================================
import os
out_res = r"C:\Users\victo\miner_game\src\main\resources\tiles\minerador.png"
out_run = r"C:\Users\victo\miner_game\out\tiles\minerador.png"

img.save(out_res, "PNG")
img.save(out_run, "PNG")
print(f"Saved: {out_res}")
print(f"Saved: {out_run}")
print(f"Size: {W}x{H} pixels")
