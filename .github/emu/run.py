#!/usr/bin/env python3
"""
جوال وهمي يجرب WM Onyx ويصوّره.
يشتغل على سيرفر GitHub بعد ما يشتغل الـ Emulator. يسوي سيناريوهات ثابتة
(تقليب صفحات، ضغطة طويلة، سحب، وضع الترتيب، الإشعارات) بمقاس الشاشة الخارجية والداخلية،
ويحفظ صور + فيديو لكل سيناريو بمجلد emu-out، عشان Claude يشوفها قبل ما توصل النسخة لوليد.

ما يوقف لو سيناريو فشل: يكتب الخطأ بالتقرير ويكمل.
"""
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

APK = sys.argv[1]
OUT = sys.argv[2]
PKG = "com.waleed.onyx"
os.makedirs(OUT, exist_ok=True)
report = []

# مقاسات Fold 7 (أقرب فولد معروف). نضبطها على جوال وليد لما يرسل المقاس الحقيقي
SCREENS = {
    "outer": ("1080x2520", 420),
    "inner": ("1968x2184", 420),
}


def sh(cmd, check=False, timeout=120):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError(f"{cmd}\n{r.stdout}\n{r.stderr}")
    return r.stdout.strip()


def adb(cmd, **kw):
    return sh(f"adb shell {cmd}", **kw)


def log(msg):
    print(msg, flush=True)
    report.append(msg)


def shot(name):
    path = f"{OUT}/{name}.png"
    sh(f"adb exec-out screencap -p > {path}")
    # نسخة صغيرة عشان تنقرا بسرعة
    sh(f"ffmpeg -loglevel error -y -i {path} -vf scale=540:-1 {OUT}/{name}_s.png && mv {OUT}/{name}_s.png {path}")
    log(f"  📸 {name}.png")


class Record:
    """فيديو للشاشة وقت السيناريو، ويتحول لورقة لقطات (كل لقطة = ٠.١٦ ثانية)"""

    def __init__(self, name):
        self.name = name

    def __enter__(self):
        adb("rm -f /sdcard/rec.mp4")
        self.p = subprocess.Popen("adb shell screenrecord --bit-rate 6000000 --time-limit 30 /sdcard/rec.mp4", shell=True)
        time.sleep(1.2)
        return self

    def __exit__(self, *a):
        time.sleep(0.8)
        sh("adb shell pkill -INT screenrecord")
        try:
            self.p.wait(timeout=10)
        except Exception:
            self.p.kill()
        time.sleep(1.5)
        mp4 = f"{OUT}/{self.name}.mp4"
        sh(f"adb pull /sdcard/rec.mp4 {mp4}")
        if os.path.exists(mp4) and os.path.getsize(mp4) > 1000:
            sh(f'ffmpeg -loglevel error -y -i {mp4} -vf "fps=6,scale=216:-1,tile=6x4:padding=4:color=gray" -start_number 1 {OUT}/{self.name}_sheet_%02d.png')
            log(f"  🎞 {self.name}.mp4 + ورقة لقطات")
        else:
            log(f"  ⚠️ الفيديو ما انحفظ ({self.name})")


# ───────── اللمس ─────────

def down(x, y):
    adb(f"input motionevent DOWN {int(x)} {int(y)}")


def move(x, y):
    adb(f"input motionevent MOVE {int(x)} {int(y)}")
    report.append(f"    > move {int(x)},{int(y)}")


def up(x, y):
    adb(f"input motionevent UP {int(x)} {int(y)}")


def tap(x, y):
    adb(f"input tap {int(x)} {int(y)}")


def swipe(x1, y1, x2, y2, ms):
    adb(f"input swipe {int(x1)} {int(y1)} {int(x2)} {int(y2)} {ms}")


def long_press_drag(x1, y1, path, hold=0.8, dwell=0.0):
    """ضغطة طويلة، وبعدين يمشي على النقاط [path]، ويوقف [dwell] ثانية عند كل نقطة"""
    down(x1, y1)
    time.sleep(hold)
    lx, ly = x1, y1
    for (x, y) in path:
        steps = 4
        for i in range(1, steps + 1):
            move(lx + (x - lx) * i / steps, ly + (y - ly) * i / steps)
        lx, ly = x, y
        if dwell:
            time.sleep(dwell)
    up(lx, ly)


# ───────── قراءة الشاشة ─────────

def nodes():
    """كل العناصر اللي لها اسم (الأيقونات اسمها اسم التطبيق)"""
    adb("uiautomator dump /sdcard/ui.xml", timeout=60)
    xml = adb("cat /sdcard/ui.xml")
    out = []
    try:
        root = ET.fromstring(xml[xml.find("<"):])
    except Exception:
        return out
    for n in root.iter("node"):
        b = re.findall(r"\d+", n.get("bounds", ""))
        if len(b) != 4:
            continue
        x1, y1, x2, y2 = map(int, b)
        out.append({
            "desc": n.get("content-desc", ""), "text": n.get("text", ""),
            "id": n.get("resource-id", ""), "x": (x1 + x2) / 2, "y": (y1 + y2) / 2,
            "w": x2 - x1, "h": y2 - y1, "box": [x1, y1, x2, y2],
        })
    return out


def icons():
    """الأيقونات اللي على الشاشة (لها اسم ومقاسها مربع تقريباً)"""
    return [n for n in nodes() if n["desc"] and n["w"] > 40 and abs(n["w"] - n["h"]) < 12]


def screen_wh():
    m = re.search(r"(\d+)x(\d+)", adb("wm size").splitlines()[-1])
    return int(m.group(1)), int(m.group(2))


def home():
    adb("input keyevent KEYCODE_HOME")
    time.sleep(1.5)


CURRENT = []


def set_screen(name):
    CURRENT[:] = [name]
    size, dpi = SCREENS[name]
    adb(f"wm size {size}")
    adb(f"wm density {dpi}")
    time.sleep(3)
    home()
    time.sleep(2)


BASE = {}


def grid_left():
    W, H = screen_wh()
    ic = [n for n in icons() if n["x"] < W * 0.8]
    return min((n["box"][0] for n in ic), default=None)


def check_aligned(title):
    """الحارس: بعد أي سيناريو الصفحة لازم تكون واقفة على صفحة كاملة (ما تعلق بالنص)"""
    time.sleep(1.2)
    x = grid_left()
    base = BASE.get("left")
    if x is None or base is None:
        return
    ok = abs(x - base) <= 3
    log(f"  {'✅' if ok else '❌'} الصفحة واقفة صح بعد ({title}): أول أيقونة x={x} (المفروض {base})")


def scenario(title, fn):
    log(f"\n## {title}")
    sh("adb logcat -c")
    try:
        fn()
    except Exception as e:
        log(f"  ❌ فشل: {e}")
        home()
    drag_log = sh("adb logcat -d -s OnyxDrag:D -v brief")
    lines = [l.split(": ", 1)[-1] for l in drag_log.splitlines() if "OnyxDrag" in l]
    for l in lines[-12:]:
        log(f"    · {l}")
    # الحارس: الأيقونة المسحوبة لازم تبقى تحت الإصبع (السيناريوهات تمسك الأيقونة من نصها)
    worst = 0.0
    dens = SCREENS[CURRENT[0]][1] / 160 if CURRENT else 2.625
    for l in lines:
        m = re.search(r"pointer=(\d+),(\d+)px center=(\d+),(\d+)dp", l)
        if m:
            px_, py_, cx_, cy_ = map(int, m.groups())
            worst = max(worst, abs(px_ / dens - cx_), abs(py_ / dens - cy_))
    if any("pointer=" in l and "center=" in l for l in lines):
        log(f"  {'✅' if worst <= 12 else '❌'} الأيقونة تحت الإصبع: أبعد فرق {worst:.0f}dp")
    check_aligned(title)


# ───────── السيناريوهات ─────────

def layout_report(tag):
    """مقاسات الأيقونات والعمود من الشاشة نفسها (عشان نقارنها بالكتاب)"""
    ic = icons()
    W, H = screen_wh()
    data = {"screen": [W, H], "icons": [{"desc": n["desc"], "box": n["box"]} for n in ic]}
    with open(f"{OUT}/{tag}_layout.json", "w") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    if ic:
        sizes = sorted({n["w"] for n in ic})
        log(f"  أيقونات على الشاشة: {len(ic)} · المقاسات (px): {sizes[:6]}")
        right = [n for n in ic if n["x"] > W * 0.8]
        if right:
            log(f"  أيقونات يمين الشاشة (العمود؟): {len(right)} · x من {min(n['box'][0] for n in right)} إلى {max(n['box'][2] for n in right)}")


def run_screen(tag):
    W, H = screen_wh()
    log(f"\n# الشاشة: {tag} ({W}×{H})")

    def s_home():
        BASE["left"] = grid_left()
        shot(f"{tag}_01_home")
        layout_report(tag)
        # فحص على الجوال نفسه: أيقونات الشبكة ما توصل للعمود
        ic = icons()
        side = [n for n in ic if n["x"] > W * 0.82]
        grid = [n for n in ic if n["x"] <= W * 0.82]
        if side and grid:
            gap = min(n["box"][0] for n in side) - max(n["box"][2] for n in grid)
            log(f"  المسافة بين آخر أيقونة بالشبكة وأيقونات العمود: {gap}px " + ("✅" if gap > 0 else "❌ تداخل"))
        sides = sorted(n["box"][1] for n in side)
        if len(sides) > 1:
            log(f"  المسافة بين أيقونات العمود (px): {[sides[k+1]-sides[k] for k in range(len(sides)-1)]}")
    scenario("الشاشة الرئيسية", s_home)

    def s_pages():
        with Record(f"{tag}_02_pages"):
            swipe(W * 0.75, H * 0.45, W * 0.15, H * 0.45, 140)   # سحبة سريعة
            time.sleep(1.5)
            swipe(W * 0.2, H * 0.45, W * 0.55, H * 0.45, 600)    # سحبة بطيئة قصيرة (لازم ترجع)
            time.sleep(1.5)
            swipe(W * 0.15, H * 0.45, W * 0.85, H * 0.45, 140)   # رجوع
            time.sleep(1.5)
        shot(f"{tag}_02_pages_end")
    scenario("تقليب الصفحات (سريع، بطيء، رجوع)", s_pages)

    def s_menu():
        ic = [n for n in icons() if n["x"] < W * 0.75]
        if not ic:
            raise RuntimeError("ما لقيت أيقونات")
        a = ic[0]
        down(a["x"], a["y"]); time.sleep(0.9); up(a["x"], a["y"])
        time.sleep(0.8)
        shot(f"{tag}_03_menu")
        adb("input keyevent KEYCODE_BACK"); time.sleep(0.8)
    scenario("ضغطة طويلة على أيقونة ورفع", s_menu)

    def s_drag():
        ic = [n for n in icons() if n["x"] < W * 0.75]
        a = ic[0]
        cell = ic[1]["x"] - ic[0]["x"] if len(ic) > 1 else 200
        path = [(a["x"] + cell, a["y"]), (a["x"] + cell * 2, a["y"]), (a["x"] + cell * 2, a["y"] + cell), (a["x"] + cell, a["y"] + cell)]
        with Record(f"{tag}_04_drag"):
            long_press_drag(a["x"], a["y"], path, hold=0.8, dwell=0.15)  # يمر بسرعة
            time.sleep(1.2)
            long_press_drag(a["x"] + cell, a["y"] + cell, [(a["x"] + cell * 2, a["y"])], hold=0.8, dwell=1.2)  # يوقف
            time.sleep(1.2)
        shot(f"{tag}_04_drag_end")
        home()
    scenario("سحب أيقونة (يمر بسرعة، وبعدين يوقف)", s_drag)

    def s_edge():
        ic = [n for n in icons() if n["x"] < W * 0.75]
        a = ic[-1]
        with Record(f"{tag}_05_edge"):
            long_press_drag(a["x"], a["y"], [(W * 0.80, a["y"]), (W * 0.97, a["y"])], hold=0.8, dwell=0.2)
            time.sleep(0.2)
        # نسخة ثانية: يوقف عند الحافة ثانيتين
        with Record(f"{tag}_05b_edge_hold"):
            down(a["x"], a["y"]); time.sleep(0.8)
            for f in (0.6, 0.8, 0.9, 0.97):
                move(W * f, a["y"])
            time.sleep(2.5)
            up(W * 0.97, a["y"])
            time.sleep(1.2)
        shot(f"{tag}_05_edge_end")
        home()
    scenario("نقل أيقونة لحافة الشاشة (صفحة ثانية)", s_edge)

    def s_edit():
        # الصفحات مليانة: ندخل وضع الترتيب من قائمة الأيقونة
        ic = [n for n in icons() if n["x"] < W * 0.75]
        a = ic[0]
        down(a["x"], a["y"]); time.sleep(0.9); up(a["x"], a["y"]); time.sleep(0.8)
        row = [n for n in nodes() if "ترتيب الشاشة" in n["text"]]
        if not row:
            raise RuntimeError("ما لقيت زر ترتيب الشاشة")
        tap(row[0]["x"], row[0]["y"])
        time.sleep(1.0)
        shot(f"{tag}_06_edit")
        tap(W * 0.4, H * 0.3)  # ضغطة على فاضي بوضع الترتيب (المفروض ما تطلعه)
        time.sleep(0.8)
        shot(f"{tag}_06b_edit_after_tap")
        home()
    scenario("وضع الترتيب + ضغطة على فاضي", s_edit)

    def s_side_swap():
        # من الشبكة للعمود: يوقف فوق أيقونة بالعمود ويفلت = يتبادلون
        ic = icons()
        side = sorted([n for n in ic if n["x"] > W * 0.82], key=lambda n: n["y"])
        grid = [n for n in ic if n["x"] <= W * 0.82]
        if not side or not grid:
            raise RuntimeError("ما لقيت العمود أو الشبكة")
        a, s0 = grid[0], side[0]
        log(f"  قبل: الشبكة={a['desc']} العمود={s0['desc']}")
        with Record(f"{tag}_08_side_swap"):
            long_press_drag(a["x"], a["y"], [(s0["x"] - 60, s0["y"]), (s0["x"], s0["y"])], hold=0.8, dwell=0.6)
            time.sleep(1.2)
        ic2 = icons()
        side2 = sorted([n for n in ic2 if n["x"] > W * 0.82], key=lambda n: n["y"])
        log(f"  بعد: العمود={side2[0]['desc'] if side2 else '-'} " + ("✅ تبادلوا" if side2 and side2[0]["desc"] == a["desc"] else "❌ ما تبادلوا"))
        shot(f"{tag}_08_side_swap_end")
        home()
    scenario("تبديل أيقونة مع العمود", s_side_swap)

    def s_shade():
        swipe(W * 0.5, H * 0.35, W * 0.5, H * 0.7, 200)
        time.sleep(1.5)
        shot(f"{tag}_07_shade")
        adb("cmd statusbar collapse")
        time.sleep(1)
    scenario("سحب لتحت (الإشعارات)", s_shade)

    def s_flipback():
        # سحبة بطيئة قصيرة لازم ترجع لنفس الصفحة، وسحبة طويلة تقلب صفحة وحدة
        before = grid_left()
        swipe(W * 0.55, H * 0.45, W * 0.40, H * 0.45, 700)
        time.sleep(1.5)
        log(f"  سحبة قصيرة بطيئة: {'✅ رجعت' if abs((grid_left() or 0) - (before or 0)) <= 3 else '❌ ما رجعت'}")
        swipe(W * 0.75, H * 0.45, W * 0.10, H * 0.45, 500)
        time.sleep(1.5)
        shot(f"{tag}_09_page2")
        swipe(W * 0.10, H * 0.45, W * 0.75, H * 0.45, 500)
        time.sleep(1.5)
    scenario("سحبة قصيرة ترجع، وطويلة تقلب", s_flipback)


def main():
    log("# WM Onyx · الجوال الوهمي")
    log(sh("adb shell getprop ro.build.version.release") + " · API " + sh("adb shell getprop ro.build.version.sdk"))
    sh(f"adb install -r -g {APK}", check=True, timeout=240)
    adb(f"cmd package set-home-activity {PKG}/.MainActivity")
    adb("settings put global window_animation_scale 1")
    adb("settings put global transition_animation_scale 1")
    adb("settings put global animator_duration_scale 1")
    home()
    time.sleep(4)  # أول تشغيل: يرتب التطبيقات
    # نزور الشاشتين عشان كل وحدة تحفظ عدد صفوفها، وبعدين نعبي صفحات للاختبار
    set_screen("inner")
    set_screen("outer")
    adb(f"am start -n {PKG}/.MainActivity --es onyx_test_fill wm-emu-2026")
    time.sleep(3)
    home()
    for tag in SCREENS:
        set_screen(tag)
        run_screen(tag)
    adb("wm size reset"); adb("wm density reset")
    with open(f"{OUT}/REPORT.md", "w") as f:
        f.write("\n".join(report) + "\n")


if __name__ == "__main__":
    main()
