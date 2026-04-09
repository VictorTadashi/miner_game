"""
Process all miner sprite images for the Cave Miner game.

Key technique: All sprites are aligned by their CENTER OF MASS so the
character's visual center stays in the exact same position across frames.
This prevents the "trembling" effect during walk animation.

Pipeline per sprite:
  1. Remove gray checkerboard background (BFS flood fill)
  2. Crop to content bounding box
  3. Scale based on HEIGHT only (consistent character height)
  4. Place on 32x40 canvas aligned by center of mass
"""
from PIL import Image
import numpy as np
from collections import deque
import os

RES_DIR = r"C:\Users\victo\miner_game\src\main\resources\tiles"
OUT_DIR = r"C:\Users\victo\miner_game\out\tiles"

TARGET_W, TARGET_H = 48, 60


def remove_background(img):
    """Remove gray checkerboard background via flood fill from edges."""
    data = np.array(img)
    h, w = data.shape[:2]

    def is_background(r, g, b):
        if abs(int(r) - int(g)) > 30 or abs(int(g) - int(b)) > 30:
            return False
        avg = (int(r) + int(g) + int(b)) / 3
        return 100 < avg < 250

    alpha_mask = np.ones((h, w), dtype=bool)
    visited = np.zeros((h, w), dtype=bool)
    queue = deque()

    for sy, sx in [(0, 0), (0, w-1), (h-1, 0), (h-1, w-1)]:
        if is_background(data[sy, sx, 0], data[sy, sx, 1], data[sy, sx, 2]):
            queue.append((sy, sx))
            visited[sy, sx] = True
            alpha_mask[sy, sx] = False

    for x in range(0, w, 4):
        for y in [0, 1, h-2, h-1]:
            if not visited[y, x] and is_background(data[y, x, 0], data[y, x, 1], data[y, x, 2]):
                queue.append((y, x))
                visited[y, x] = True
                alpha_mask[y, x] = False
    for y in range(0, h, 4):
        for x in [0, 1, w-2, w-1]:
            if not visited[y, x] and is_background(data[y, x, 0], data[y, x, 1], data[y, x, 2]):
                queue.append((y, x))
                visited[y, x] = True
                alpha_mask[y, x] = False

    directions = [(-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (-1, 1), (1, -1), (1, 1)]
    while queue:
        cy, cx = queue.popleft()
        for dy, dx in directions:
            ny, nx = cy + dy, cx + dx
            if 0 <= ny < h and 0 <= nx < w and not visited[ny, nx]:
                visited[ny, nx] = True
                r, g, b = data[ny, nx, 0], data[ny, nx, 1], data[ny, nx, 2]
                if is_background(r, g, b):
                    alpha_mask[ny, nx] = False
                    queue.append((ny, nx))

    data[~alpha_mask, 3] = 0

    for y in range(1, h - 1):
        for x in range(1, w - 1):
            if not alpha_mask[y, x]:
                continue
            has_transparent_neighbor = False
            for dy, dx in [(-1,0),(1,0),(0,-1),(0,1)]:
                ny, nx = y + dy, x + dx
                if 0 <= ny < h and 0 <= nx < w and not alpha_mask[ny, nx]:
                    has_transparent_neighbor = True
                    break
            if has_transparent_neighbor:
                r, g, b = int(data[y, x, 0]), int(data[y, x, 1]), int(data[y, x, 2])
                gray_diff = abs(r - g) + abs(g - b)
                avg = (r + g + b) / 3
                if gray_diff < 30 and avg > 140:
                    data[y, x, 3] = max(0, min(255, int(255 * (1 - (avg - 150) / 100))))

    removed = np.sum(~alpha_mask)
    print(f"  Background removed: {removed} px ({removed*100//(h*w)}%)")
    return Image.fromarray(data)


def remove_bg_and_crop(src_path):
    """Load image, remove background, crop to content."""
    print(f"  Loading: {os.path.basename(src_path)}")
    img = Image.open(src_path).convert("RGBA")
    print(f"  Original: {img.size}")
    result = remove_background(img)
    bbox = result.getbbox()
    if bbox:
        result = result.crop(bbox)
        print(f"  Cropped: {result.size}")
    return result


def get_center_of_mass_x(img):
    """Get the horizontal center of mass of non-transparent pixels."""
    data = np.array(img)
    alpha = data[:, :, 3]
    yy, xx = np.where(alpha > 0)
    if len(xx) == 0:
        return img.size[0] / 2
    return float(np.mean(xx))


def process_sprite_com(cropped, target_w, target_h, com_target_x):
    """
    Process a single sprite: scale by height, then place on canvas
    so that the center of mass lands at com_target_x.

    This ensures all sprites have:
    - Same height (character doesn't shrink/grow)
    - Same horizontal center of gravity (character doesn't jump left/right)
    """
    cw, ch = cropped.size

    # Scale based on HEIGHT to ensure consistent character size
    scale = target_h / ch
    new_w = max(1, round(cw * scale))
    new_h = target_h

    resized = cropped.resize((new_w, new_h), Image.LANCZOS)

    # Find center of mass of the resized sprite
    com_x = get_center_of_mass_x(resized)

    # Place on canvas so that center of mass aligns with com_target_x
    final = Image.new("RGBA", (target_w, target_h), (0, 0, 0, 0))

    # offset_x positions the resized image so com_x maps to com_target_x
    offset_x = round(com_target_x - com_x)

    # Paste (handling sprites that extend beyond canvas edges)
    # We need to calculate the overlap region
    src_left = max(0, -offset_x)
    src_right = min(new_w, target_w - offset_x)
    dst_left = max(0, offset_x)
    dst_right = min(target_w, offset_x + new_w)

    if src_right > src_left and dst_right > dst_left:
        crop_region = resized.crop((src_left, 0, src_right, new_h))
        final.paste(crop_region, (dst_left, 0), crop_region)

    return final


def save_sprite(img, output_name):
    """Save sprite to both resource and output directories."""
    for d in [RES_DIR, OUT_DIR]:
        path = os.path.join(d, output_name)
        img.save(path, "PNG")
    print(f"  Saved: {output_name} ({img.size})")


# ============================================================
# Source paths
# ============================================================
IDLE_SRC = r"C:\Users\victo\Downloads\ImgMineiroParado_extracted\ImgMineiroParado.png"
WALK1_SRC = r"C:\Users\victo\Downloads\MineradorAndando_extracted\MineradorAndando.png"
WALK2_SRC = r"C:\Users\victo\Downloads\MineradorAndando02_extracted\MineradorAndando02.png"
ATTACK1_SRC = r"C:\Users\victo\Downloads\MineradorAnimacao_extracted\MineradorAnimacao.png"
ATTACK2_SRC = r"C:\Users\victo\Downloads\MineradorAnimacao02_extracted\MineradorAnimacao02.png"

# ============================================================
# Step 1: Remove backgrounds and crop
# ============================================================
print("=" * 50)
print("Step 1: Remove backgrounds and crop")
print("=" * 50)

print("\n[Idle]")
idle_cropped = remove_bg_and_crop(IDLE_SRC)

print("\n[Walk Frame 1]")
walk1_cropped = remove_bg_and_crop(WALK1_SRC)

print("\n[Walk Frame 2]")
walk2_cropped = remove_bg_and_crop(WALK2_SRC)

print("\n[Attack Frame 1]")
attack1_cropped = remove_bg_and_crop(ATTACK1_SRC)

print("\n[Attack Frame 2]")
attack2_cropped = remove_bg_and_crop(ATTACK2_SRC)

# ============================================================
# Step 2: Compute center-of-mass for each at target scale
# ============================================================
print("\n" + "=" * 50)
print("Step 2: Analyze center of mass at target scale")
print("=" * 50)

def preview_com(cropped, label):
    """Calculate where the center of mass would be after height-based scaling."""
    cw, ch = cropped.size
    scale = TARGET_H / ch
    new_w = max(1, round(cw * scale))
    resized = cropped.resize((new_w, TARGET_H), Image.LANCZOS)
    com_x = get_center_of_mass_x(resized)
    print(f"  {label}: size={new_w}x{TARGET_H}, CoM_x={com_x:.1f}")
    return com_x

idle_com = preview_com(idle_cropped, "idle")
walk1_com = preview_com(walk1_cropped, "walk1")
walk2_com = preview_com(walk2_cropped, "walk2")

# Use the AVERAGE center of mass as the target alignment point.
# Map this to the center of the target canvas (TARGET_W / 2).
avg_com = (idle_com + walk1_com + walk2_com) / 3
print(f"\n  Average CoM: {avg_com:.1f}")
print(f"  Target canvas center: {TARGET_W / 2}")

# The target x on the final canvas where all CoM should land
com_target_x = TARGET_W / 2
print(f"  All sprites will be aligned with CoM at x={com_target_x}")

# ============================================================
# Step 3: Process and save all sprites with CoM alignment
# ============================================================
print("\n" + "=" * 50)
print("Step 3: Process sprites with CoM alignment")
print("=" * 50)

print("\n[Idle -> minerador.png]")
idle_final = process_sprite_com(idle_cropped, TARGET_W, TARGET_H, com_target_x)
save_sprite(idle_final, "minerador.png")

print("\n[Walk1 -> minerador_walk1.png]")
walk1_final = process_sprite_com(walk1_cropped, TARGET_W, TARGET_H, com_target_x)
save_sprite(walk1_final, "minerador_walk1.png")

print("\n[Walk2 -> minerador_walk2.png]")
walk2_final = process_sprite_com(walk2_cropped, TARGET_W, TARGET_H, com_target_x)
save_sprite(walk2_final, "minerador_walk2.png")

print("\n[Attack1 -> minerador_attack1.png]")
attack1_final = process_sprite_com(attack1_cropped, TARGET_W, TARGET_H, com_target_x)
save_sprite(attack1_final, "minerador_attack1.png")

print("\n[Attack2 -> minerador_attack2.png]")
attack2_final = process_sprite_com(attack2_cropped, TARGET_W, TARGET_H, com_target_x)
save_sprite(attack2_final, "minerador_attack2.png")

# ============================================================
# Step 4: Title sprite (96x120)
# ============================================================
print("\n" + "=" * 50)
print("Step 4: Title sprite (96x120)")
print("=" * 50)

title_final = process_sprite_com(idle_cropped, 96, 120, 48)
save_sprite(title_final, "minerador_title.png")

# ============================================================
# Step 5: Verify alignment
# ============================================================
print("\n" + "=" * 50)
print("Step 5: Verify alignment of final sprites")
print("=" * 50)

for name in ["minerador.png", "minerador_walk1.png", "minerador_walk2.png",
             "minerador_attack1.png", "minerador_attack2.png"]:
    path = os.path.join(OUT_DIR, name)
    img = Image.open(path)
    com_x = get_center_of_mass_x(img)
    data = np.array(img)
    alpha = data[:, :, 3]
    opaque = np.sum(alpha > 0)
    cols = np.any(alpha > 0, axis=1)
    rows = np.any(alpha > 0, axis=0)
    rmin, rmax = np.where(rows)[0][[0, -1]]
    print(f"  {name}: CoM_x={com_x:.1f}, opaque_px={opaque}, content_cols=[{rmin},{rmax}]")

print("\nAll miner sprites processed with center-of-mass alignment!")

# ============================================================
# Step 6: Monster sprite (48x48) — larger than the player
# ============================================================
print("\n" + "=" * 50)
print("Step 6: Monster sprite (72x72)")
print("=" * 50)

MONSTER_SRC = r"C:\Users\victo\Downloads\GameMonster01_extracted\GameMonster01.png"

print("\n[Monster]")
monster_cropped = remove_bg_and_crop(MONSTER_SRC)

MONSTER_W, MONSTER_H = 72, 72
monster_final = process_sprite_com(monster_cropped, MONSTER_W, MONSTER_H, MONSTER_W / 2)
save_sprite(monster_final, "monster_01.png")

# Verify monster
path = os.path.join(OUT_DIR, "monster_01.png")
img = Image.open(path)
com_x = get_center_of_mass_x(img)
data = np.array(img)
alpha = data[:, :, 3]
opaque = np.sum(alpha > 0)
print(f"  monster_01.png: CoM_x={com_x:.1f}, opaque_px={opaque}, size={img.size}")

print("\nAll sprites processed!")
