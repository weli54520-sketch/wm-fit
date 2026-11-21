# WM Onyx · الجوال الوهمي
14 · API 34
**الخلاصة: 62 ✅ · 0 ❌**

# الشاشة: outer (1080×2520)

## الشاشة الرئيسية
  📸 outer_01_home.png
  أيقونات على الشاشة: 26 · المقاسات (px): [137, 159]
  أيقونات يمين الشاشة (العمود؟): 2 · x من 886 إلى 1045
  المسافة بين آخر أيقونة بالشبكة وأيقونات العمود: 39px ✅
  المسافة بين أيقونات العمود (px): [395]
  ✅ الصفحة واقفة صح بعد (الشاشة الرئيسية): أول أيقونة x=65 (المفروض 65)

## تقليب الصفحات (سريع، بطيء، رجوع)
  🎞 outer_02_pages.mp4 + ورقة لقطات
  📸 outer_02_pages_end.png
    · release offset=0.69 v=0px/s target=1
    · release offset=0.75 v=0px/s target=1
    · release offset=0.19 v=0px/s target=0
  الصفحات بعد كل سحبة: ['1', '1', '0']
  ✅ اتجاه التقليب صح (ما يرجع للخلف بالغلط)
  النعومة (جوال وهمي بطيء، للمقارنة بس): إطارات متأخرة 86.05% · 90%=150ms · 99%=250ms
  ✅ الصفحة واقفة صح بعد (تقليب الصفحات (سريع، بطيء، رجوع)): أول أيقونة x=65 (المفروض 65)

## بوضع الترتيب: سحبة سريعة فوق الأيقونات
  السحبة من فوق Photos (539,1020)
  ✅ سحبة سريعة بوضع الترتيب قلبت الصفحة
  📸 outer_11_edit_swipe.png
  ✅ الصفحة واقفة صح بعد (بوضع الترتيب: سحبة سريعة فوق الأيقونات): أول أيقونة x=65 (المفروض 65)

## ضغطة طويلة على أيقونة ورفع
  📸 outer_03_menu.png
  ✅ الصفحة واقفة صح بعد (ضغطة طويلة على أيقونة ورفع): أول أيقونة x=65 (المفروض 65)

## سحب أيقونة (يمر بسرعة، وبعدين يوقف)
    > move 195,598
    > move 246,598
    > move 297,598
    > move 348,598
    > move 399,598
    > move 450,598
    > move 501,598
    > move 552,598
    > move 552,649
    > move 552,700
    > move 552,751
    > move 552,802
    > move 501,802
    > move 450,802
    > move 399,802
    > move 348,802
    > move 399,751
    > move 450,700
    > move 501,649
    > move 552,598
  🎞 outer_04_drag.mp4 + ورقة لقطات
  📸 outer_04_drag_end.png
    · pointer=552,649px center=210,247dp size=69x69dp kind=0 k=0.88 page=0 target=MergeAt(id=6)
    · pointer=552,751px center=210,286dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=1)
    · pointer=552,802px center=210,306dp size=69x69dp kind=0 k=0.88 page=0 target=MergeAt(id=10)
    · pointer=450,802px center=172,306dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=1)
    · pointer=399,802px center=152,306dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · pointer=348,802px center=133,306dp size=69x69dp kind=0 k=0.88 page=0 target=MergeAt(id=9)
    · drop pointer=348,802px target=MergeAt(id=9)
    · start fromSlot=null menuCut=true
    · pointer=348,802px center=137,320dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · pointer=450,700px center=175,281dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=501,649px center=195,262dp size=69x69dp kind=0 k=0.88 page=0 target=MergeAt(id=6)
    · drop pointer=552,598px target=MergeAt(id=6)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (سحب أيقونة (يمر بسرعة، وبعدين يوقف)): أول أيقونة x=65 (المفروض 65)

## نقل أيقونة لحافة الشاشة (صفحة ثانية)
    > move 631,598
    > move 708,598
    > move 786,598
    > move 864,598
    > move 909,598
    > move 955,598
    > move 1001,598
    > move 1047,598
  🎞 outer_05_edge.mp4 + ورقة لقطات
  📸 outer_05_edge_dropped.png
    > move 648,1620
    > move 864,1620
    > move 972,1620
    > move 1047,1620
    > move 486,1620
  🎞 outer_05b_edge_hold.mp4 + ورقة لقطات
  ✅ نقل أيقونة لصفحة ثانية (نزلت بالصفحة 1)
  📸 outer_05_edge_end.png
    · start fromSlot=null menuCut=true
    · pointer=631,598px center=240,228dp size=78x78dp kind=2 k=1.00 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=708,598px center=270,228dp size=72x72dp kind=2 k=0.93 page=0 target=CellAt(page=0, x=3, y=0)
    · pointer=864,598px center=329,228dp size=69x69dp kind=2 k=0.88 page=0 target=EdgeAt(dir=1)
    · drop pointer=1047,598px target=CellAt(page=1, x=3, y=0)
    · start fromSlot=null menuCut=true
    · pointer=648,1620px center=247,617dp size=78x78dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=2, y=5)
    · pointer=648,1620px center=247,617dp size=75x75dp kind=0 k=0.97 page=0 target=CellAt(page=0, x=3, y=5)
    · pointer=864,1620px center=329,617dp size=75x75dp kind=0 k=0.97 page=0 target=EdgeAt(dir=1)
    · pointer=486,1620px center=185,617dp size=69x69dp kind=0 k=0.88 page=1 target=CellAt(page=1, x=2, y=5)
    · drop pointer=486,1620px target=CellAt(page=1, x=2, y=5)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (نقل أيقونة لحافة الشاشة (صفحة ثانية)): أول أيقونة x=65 (المفروض 65)

## وضع الترتيب + ضغطة على فاضي
  📸 outer_06_edit.png
  📸 outer_06b_edit_after_tap.png
  ✅ الصفحة واقفة صح بعد (وضع الترتيب + ضغطة على فاضي): أول أيقونة x=65 (المفروض 65)

## تبديل أيقونة مع العمود
  قبل: الشبكة=Calendar العمود=Phone
    > move 334,652
    > move 525,706
    > move 715,759
    > move 905,813
    > move 920,813
    > move 935,813
    > move 950,813
    > move 965,813
  🎞 outer_08_side_swap.mp4 + ورقة لقطات
  بعد: العمود=Calendar ✅ تبادلوا
  📸 outer_08_side_swap_end.png
    · start fromSlot=null menuCut=true
    · pointer=334,652px center=127,249dp size=78x78dp kind=0 k=1.00 page=0 target=MergeAt(id=5)
    · pointer=525,706px center=200,269dp size=69x69dp kind=0 k=0.89 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=715,759px center=272,289dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=3, y=1)
    · pointer=905,813px center=345,310dp size=69x69dp kind=0 k=0.88 page=0 target=SlotAt(container=1, index=0)
    · drop pointer=965,813px target=SlotAt(container=1, index=0)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (تبديل أيقونة مع العمود): أول أيقونة x=65 (المفروض 65)

## ويدجت: إضافة، قائمة، سحب
  📸 outer_10_gallery.png
  📸 outer_10_widget_added.png
  ✅ انضاف ويدجت: 409×613px
  📸 outer_10_widget_menu.png
    > move 655,1365
    > move 655,1315
    > move 655,1265
    > move 655,1215
    > move 655,1185
    > move 655,1155
    > move 655,1125
    > move 655,1095
  🎞 outer_10_widget_drag.mp4 + ورقة لقطات
  ✅ الويدجت وهو يتسحب ما يكبر: 137×206dp (الويدجت 156×234dp)
  📸 outer_10_widget_drag_end.png
  ✅ الويدجت نزل بالمكان اللي اختاره (الإفلات: CellAt)
    > move 767,1283
    > move 767,1319
    > move 767,1355
    > move 767,1391
    > move 767,1427
    > move 767,1463
    > move 767,1499
    > move 767,1535
  🎞 outer_12_widget_resize.mp4 + ورقة لقطات
    · cells=2x4
    · end cells=2x4 from=2x3
  ✅ التكبير يتبع الإصبع: [249, 264, 280, 296, 311, 327, 342, 358]
  ✅ بعد ما فلت: 2×3 ← 2×4
  📸 outer_12_widget_resize_end.png
  ✅ الصفحة واقفة صح بعد (ويدجت: إضافة، قائمة، سحب): أول أيقونة x=65 (المفروض 65)

## الشريط: تغيير الترتيب داخله
  الشريط قبل: ['Settings', 'TMoble', 'Voice Search', 'YT Music']
    > move 341,2228
    > move 381,2225
    > move 420,2223
    > move 460,2220
    > move 500,2223
    > move 540,2225
    > move 579,2228
    > move 619,2230
  🎞 outer_13_dock_reorder.mp4 + ورقة لقطات
  الشريط بعد: ['TMoble', 'Voice Search', 'Settings', 'YT Music']
    · drop at=2 order=16,17,15,18
    · dock pages=0,1,2,3 apps=com.android.stk,com.google.android.googlequicksearchbox,com.android.settings,com.google.android.apps.youtube.music
  ✅ بداية السحب من القائمة: الأيقونة المرفوعة تختفي فوراً (ما فيه نسخة باهتة)
  ✅ ترتيب داخل الشريط: Settings صار ثالث
  📸 outer_13_dock_reorder_end.png
    · start fromSlot=2 menuCut=true
    · pointer=381,2225px center=145,848dp size=78x78dp kind=0 k=1.00 page=0 target=DockAt(index=1)
    · pointer=540,2225px center=206,848dp size=69x69dp kind=0 k=0.88 page=0 target=DockAt(index=2)
    · drop pointer=619,2230px target=DockAt(index=2)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 0dp
  ✅ الصفحة واقفة صح بعد (الشريط: تغيير الترتيب داخله): أول أيقونة x=65 (المفروض 65)

## الشريط: طلوع أيقونة ورجوعها
    > move 778,2136
    > move 778,2041
    > move 778,1947
    > move 778,1852
    > move 691,1653
    > move 605,1455
    > move 518,1256
    > move 432,1058
  🎞 outer_14_dock_out.mp4 + ورقة لقطات
  طلعت YT Music: الشريط 4 ← 3 ['TMoble', 'Voice Search', 'Settings']
  ✅ الأيقونة طلعت من الشريط
  ✅ ما فيه فراغ بالشريط (المحفوظ 0،1،2…)
  ✅ الباقي متجمع بالنص (النص 540، الشاشة 540)
  ✅ المسافات بينهم متساوية [159.0, 159.0]
  📸 outer_14_dock_out_end.png
    > move 348,1275
    > move 348,1543
    > move 348,1811
    > move 348,2079
    > move 376,2117
    > move 404,2154
    > move 432,2192
    > move 460,2230
  🎞 outer_15_dock_in.mp4 + ورقة لقطات
  رجعت: ['TMoble', 'YT Music', 'Voice Search', 'Settings']
    · drop at=1 order=16,18,17,15
    · dock pages=0,1,2,3 apps=com.android.stk,com.google.android.apps.youtube.music,com.google.android.googlequicksearchbox,com.android.settings
  ✅ رجعت للشريط بالمكان الثاني
  ✅ الشريط متجمع بعد الرجوع
  📸 outer_15_dock_in_end.png
    · start fromSlot=null menuCut=true
    · pointer=348,1275px center=133,486dp size=78x78dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=1, y=3)
    · pointer=348,1543px center=133,588dp size=75x75dp kind=0 k=0.97 page=0 target=CellAt(page=0, x=1, y=5)
    · pointer=348,1543px center=133,588dp size=69x69dp kind=0 k=0.88 page=0 target=MergeAt(id=43)
    · pointer=348,1811px center=133,690dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=5)
    · pointer=348,2079px center=133,792dp size=69x69dp kind=0 k=0.88 page=0 target=com.waleed.onyx.grid.DragTarget$Nowhere@9ef650e
    · pointer=376,2117px center=143,807dp size=69x69dp kind=0 k=0.88 page=0 target=DockAt(index=0)
    · pointer=404,2154px center=154,821dp size=69x69dp kind=0 k=0.88 page=0 target=DockAt(index=1)
    · drop pointer=460,2230px target=DockAt(index=1)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (الشريط: طلوع أيقونة ورجوعها): أول أيقونة x=65 (المفروض 65)

## النقاط: البيت، الضغط، السحب
  النقاط: 3 صفحات، الحالية 0
  ✅ ضغطة على آخر نقطة ← الصفحة 2
  📸 outer_16_dots_last.png
  ✅ ضغطة على البيت ← الرئيسية
  ✅ الصفحة واقفة صح بعد (ضغطة البيت): أول أيقونة x=65 (المفروض 65)
    > move 508,2091
    > move 524,2091
    > move 540,2091
    > move 555,2091
    > move 571,2091
    > move 587,2091
  🎞 outer_16_dots_scrub.mp4 + ورقة لقطات
  ✅ سحب على النقاط ← الصفحة 2
  ✅ الصفحة واقفة صح بعد (سحب النقاط): أول أيقونة x=65 (المفروض 65)
  ✅ الصفحة واقفة صح بعد (النقاط: البيت، الضغط، السحب): أول أيقونة x=65 (المفروض 65)

## القائمة: الإعدادات من أي أيقونة
  ✅ زر الإعدادات موجود بقائمة الأيقونة
  ✅ انفتحت الإعدادات بدون مكان فاضي
  📸 outer_17_menu_settings.png
  ✅ الصفحة واقفة صح بعد (القائمة: الإعدادات من أي أيقونة): أول أيقونة x=65 (المفروض 65)

## الصفحة الفاضية بآخر وضع الترتيب
  📸 outer_18_empty_page.png
  🎞 outer_18_last_page.mp4 + ورقة لقطات
  ✅ طلعت من الصفحة الفاضية بحركة (مو قفزة)
  ✅ وقفت على آخر صفحة فيها شي (2 من 3)
  ✅ الصفحة واقفة صح بعد (الصفحة الأخيرة): أول أيقونة x=65 (المفروض 65)
  ✅ الصفحة واقفة صح بعد (الصفحة الفاضية بآخر وضع الترتيب): أول أيقونة x=65 (المفروض 65)

## المجلد: إنشاء وفتح
    > move 213,660
    > move 248,660
    > move 283,660
    > move 318,660
    > move 328,660
    > move 338,660
    > move 348,660
    > move 358,660
  🎞 outer_19_folder.mp4 + ورقة لقطات
  ✅ سحب أيقونة فوق أيقونة = مجلد
  ✅ أيقونة المجلد على الشاشة
  ✅ المجلد ينفتح بضغطة (زجاج سائل واسمه فوق)
  📸 outer_19_folder_open.png
  📸 outer_19_folder_closed.png
    · start fromSlot=null menuCut=false
    · pointer=179,660px center=68,251dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=0, y=0)
    · pointer=283,660px center=108,251dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=0)
    · pointer=318,660px center=121,251dp size=69x69dp kind=0 k=0.88 page=0 target=MergeAt(id=5)
    · drop pointer=358,660px target=MergeAt(id=5)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 0dp
  ✅ الصفحة واقفة صح بعد (المجلد: إنشاء وفتح): أول أيقونة x=65 (المفروض 65)

## الدرج: الفئات والبحث والحاسبة
  ✅ السحب لفوق يفتح الدرج وفيه شريط البحث تحت
  ✅ التطبيقات مقسمة فئات: ['التواصل', 'الإنتاجية', 'الصور', 'الفيديو']
  📸 outer_20_drawer.png
  ✅ الحاسبة بالبحث: 25*4 = 100
  📸 outer_20_drawer_calc.png
  ✅ البحث يلقى التطبيق (Sett ← Settings)
  📸 outer_20_drawer_search.png
  ✅ الصفحة واقفة صح بعد (الدرج: الفئات والبحث والحاسبة): أول أيقونة x=65 (المفروض 65)

## قفل الترتيب ما يحبسك
  ✅ الترتيب مقفل والضغطة الطويلة تفتح الإعدادات (ما تنحبس)
  📸 outer_21_lock_settings.png
  ✅ الصفحة واقفة صح بعد (قفل الترتيب ما يحبسك): أول أيقونة x=65 (المفروض 65)

## سحب لتحت (الإشعارات)
  📸 outer_07_shade.png
  ✅ الصفحة واقفة صح بعد (سحب لتحت (الإشعارات)): أول أيقونة x=65 (المفروض 65)

## سحبة قصيرة ترجع، وطويلة تقلب
  سحبة قصيرة بطيئة: ✅ رجعت
  📸 outer_09_page2.png
  ✅ الصفحة واقفة صح بعد (سحبة قصيرة ترجع، وطويلة تقلب): أول أيقونة x=65 (المفروض 65)

# WM Onyx · الجوال الوهمي
14 · API 34
**الخلاصة: 62 ✅ · 0 ❌**

# الشاشة: inner (1968×2184)

## الشاشة الرئيسية
  📸 inner_01_home.png
  أيقونات على الشاشة: 32 · المقاسات (px): [178, 207]
  أيقونات يمين الشاشة (العمود؟): 2 · x من 1756 إلى 1934
  المسافة بين آخر أيقونة بالشبكة وأيقونات العمود: 98px ✅
  المسافة بين أيقونات العمود (px): [440]
  ✅ الصفحة واقفة صح بعد (الشاشة الرئيسية): أول أيقونة x=127 (المفروض 127)

## تقليب الصفحات (سريع، بطيء، رجوع)
  🎞 inner_02_pages.mp4 + ورقة لقطات
  📸 inner_02_pages_end.png
    · release offset=0.64 v=0px/s target=1
    · release offset=0.77 v=0px/s target=1
    · release offset=0.24 v=0px/s target=0
  الصفحات بعد كل سحبة: ['1', '1', '0']
  ✅ اتجاه التقليب صح (ما يرجع للخلف بالغلط)
  النعومة (جوال وهمي بطيء، للمقارنة بس): إطارات متأخرة 100.00% · 90%=150ms · 99%=300ms
  ✅ الصفحة واقفة صح بعد (تقليب الصفحات (سريع، بطيء، رجوع)): أول أيقونة x=127 (المفروض 127)

## بوضع الترتيب: سحبة سريعة فوق الأيقونات
  السحبة من فوق Calendar (1004,916)
  ✅ سحبة سريعة بوضع الترتيب قلبت الصفحة
  📸 inner_11_edit_swipe.png
  ✅ الصفحة واقفة صح بعد (بوضع الترتيب: سحبة سريعة فوق الأيقونات): أول أيقونة x=127 (المفروض 127)

## ضغطة طويلة على أيقونة ورفع
  📸 inner_03_menu.png
  ✅ الصفحة واقفة صح بعد (ضغطة طويلة على أيقونة ورفع): أول أيقونة x=127 (المفروض 127)

## سحب أيقونة (يمر بسرعة، وبعدين يوقف)
    > move 296,385
    > move 363,385
    > move 429,385
    > move 495,385
    > move 561,385
    > move 628,385
    > move 694,385
    > move 760,385
    > move 760,451
    > move 760,518
    > move 760,584
    > move 760,650
    > move 694,650
    > move 628,650
    > move 561,650
    > move 495,650
    > move 561,584
    > move 628,518
    > move 694,451
    > move 760,385
  🎞 inner_04_drag.mp4 + ورقة لقطات
  📸 inner_04_drag_end.png
    · pointer=760,385px center=290,147dp size=89x89dp kind=0 k=0.88 page=0 target=MergeAt(id=24)
    · pointer=760,518px center=290,198dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=760,584px center=290,223dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=1)
    · pointer=760,650px center=290,248dp size=89x89dp kind=0 k=0.88 page=0 target=MergeAt(id=30)
    · pointer=694,650px center=265,248dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=1)
    · pointer=628,650px center=240,248dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · pointer=561,650px center=214,248dp size=89x89dp kind=0 k=0.88 page=0 target=MergeAt(id=29)
    · drop pointer=495,650px target=MergeAt(id=29)
    · start fromSlot=null menuCut=true
    · pointer=495,650px center=205,260dp size=89x89dp kind=2 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · pointer=628,518px center=255,210dp size=89x89dp kind=2 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · drop pointer=760,385px target=CellAt(page=0, x=2, y=0)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (سحب أيقونة (يمر بسرعة، وبعدين يوقف)): أول أيقونة x=127 (المفروض 127)

## نقل أيقونة لحافة الشاشة (صفحة ثانية)
    > move 964,332
    > move 1167,332
    > move 1371,332
    > move 1574,332
    > move 1658,332
    > move 1741,332
    > move 1825,332
    > move 1908,332
  🎞 inner_05_edge.mp4 + ورقة لقطات
  📸 inner_05_edge_dropped.png
    > move 1180,1444
    > move 1574,1444
    > move 1771,1444
    > move 1908,1444
    > move 885,1444
  🎞 inner_05b_edge_hold.mp4 + ورقة لقطات
  ✅ نقل أيقونة لصفحة ثانية (نزلت بالصفحة 1)
  📸 inner_05_edge_end.png
    · start fromSlot=null menuCut=true
    · pointer=964,332px center=367,147dp size=101x101dp kind=2 k=1.00 page=0 target=CellAt(page=0, x=3, y=0)
    · pointer=1167,332px center=444,146dp size=96x96dp kind=2 k=0.95 page=0 target=CellAt(page=0, x=4, y=0)
    · pointer=1371,332px center=522,144dp size=89x89dp kind=2 k=0.88 page=0 target=CellAt(page=0, x=5, y=0)
    · pointer=1658,332px center=631,144dp size=89x89dp kind=2 k=0.88 page=0 target=EdgeAt(dir=1)
    · drop pointer=1908,332px target=CellAt(page=1, x=5, y=0)
    · start fromSlot=null menuCut=true
    · pointer=1180,1444px center=450,550dp size=101x101dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=4, y=4)
    · pointer=1574,1444px center=600,550dp size=93x93dp kind=0 k=0.92 page=0 target=CellAt(page=0, x=5, y=4)
    · pointer=1771,1444px center=675,550dp size=89x89dp kind=0 k=0.88 page=0 target=EdgeAt(dir=1)
    · pointer=885,1444px center=338,550dp size=89x89dp kind=0 k=0.88 page=1 target=CellAt(page=1, x=2, y=4)
    · drop pointer=885,1444px target=CellAt(page=1, x=2, y=4)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 3dp
  ✅ الصفحة واقفة صح بعد (نقل أيقونة لحافة الشاشة (صفحة ثانية)): أول أيقونة x=127 (المفروض 127)

## وضع الترتيب + ضغطة على فاضي
  📸 inner_06_edit.png
  📸 inner_06b_edit_after_tap.png
  ✅ الصفحة واقفة صح بعد (وضع الترتيب + ضغطة على فاضي): أول أيقونة x=127 (المفروض 127)

## تبديل أيقونة مع العمود
  قبل: الشبكة=Camera العمود=Phone
    > move 817,435
    > move 1140,485
    > move 1462,535
    > move 1785,585
    > move 1800,585
    > move 1815,585
    > move 1830,585
    > move 1845,585
  🎞 inner_08_side_swap.mp4 + ورقة لقطات
  بعد: العمود=Camera ✅ تبادلوا
  📸 inner_08_side_swap_end.png
    · start fromSlot=null menuCut=true
    · pointer=817,435px center=311,166dp size=101x101dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=1140,485px center=435,185dp size=93x93dp kind=0 k=0.92 page=0 target=CellAt(page=0, x=4, y=0)
    · pointer=1462,535px center=557,204dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=5, y=0)
    · pointer=1785,585px center=680,223dp size=89x89dp kind=0 k=0.88 page=0 target=SlotAt(container=1, index=0)
    · drop pointer=1845,585px target=SlotAt(container=1, index=0)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (تبديل أيقونة مع العمود): أول أيقونة x=127 (المفروض 127)

## ويدجت: إضافة، قائمة، سحب
  📸 inner_10_gallery.png
  📸 inner_10_widget_added.png
  ✅ انضاف ويدجت: 265×530px
  📸 inner_10_widget_menu.png
    > move 760,568
    > move 760,618
    > move 760,668
    > move 760,718
    > move 760,748
    > move 760,778
    > move 760,808
    > move 760,838
  🎞 inner_10_widget_drag.mp4 + ورقة لقطات
  ✅ الويدجت وهو يتسحب ما يكبر: 89×178dp (الويدجت 101×202dp)
  📸 inner_10_widget_drag_end.png
  ✅ الويدجت نزل بالمكان اللي اختاره (الإفلات: CellAt)
    > move 846,1037
    > move 846,1084
    > move 846,1130
    > move 846,1177
    > move 846,1224
    > move 846,1270
    > move 846,1317
    > move 846,1363
  🎞 inner_12_widget_resize.mp4 + ورقة لقطات
    · cells=1x3
    · end cells=1x3 from=1x2
  ✅ التكبير يتبع الإصبع: [222, 242, 262, 282, 303, 323, 343, 363]
  ✅ بعد ما فلت: 1×2 ← 1×3
  📸 inner_12_widget_resize_end.png
  ✅ الصفحة واقفة صح بعد (ويدجت: إضافة، قائمة، سحب): أول أيقونة x=127 (المفروض 127)

## الشريط: تغيير الترتيب داخله
  الشريط قبل: ['Settings', 'TMoble', 'Voice Search', 'YT Music']
    > move 722,1871
    > move 774,1869
    > move 826,1866
    > move 879,1864
    > move 931,1866
    > move 984,1869
    > move 1036,1871
    > move 1089,1874
  🎞 inner_13_dock_reorder.mp4 + ورقة لقطات
  الشريط بعد: ['TMoble', 'Voice Search', 'Settings', 'YT Music']
    · drop at=2 order=34,35,33,36
    · dock pages=0,1,2,3 apps=com.android.stk,com.google.android.googlequicksearchbox,com.android.settings,com.google.android.apps.youtube.music
  ✅ بداية السحب من القائمة: الأيقونة المرفوعة تختفي فوراً (ما فيه نسخة باهتة)
  ✅ ترتيب داخل الشريط: Settings صار ثالث
  📸 inner_13_dock_reorder_end.png
    · start fromSlot=2 menuCut=true
    · pointer=774,1869px center=295,712dp size=101x101dp kind=0 k=1.00 page=0 target=DockAt(index=0)
    · pointer=826,1866px center=315,711dp size=98x98dp kind=0 k=0.97 page=0 target=DockAt(index=1)
    · pointer=1036,1871px center=395,713dp size=89x89dp kind=0 k=0.88 page=0 target=DockAt(index=2)
    · drop pointer=1089,1874px target=DockAt(index=2)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 0dp
  ✅ الصفحة واقفة صح بعد (الشريط: تغيير الترتيب داخله): أول أيقونة x=127 (المفروض 127)

## الشريط: طلوع أيقونة ورجوعها
    > move 1298,1792
    > move 1298,1710
    > move 1298,1628
    > move 1298,1546
    > move 1170,1389
    > move 1042,1231
    > move 914,1074
    > move 787,917
  🎞 inner_14_dock_out.mp4 + ورقة لقطات
  طلعت YT Music: الشريط 4 ← 3 ['TMoble', 'Voice Search', 'Settings']
  ✅ الأيقونة طلعت من الشريط
  ✅ ما فيه فراغ بالشريط (المحفوظ 0،1،2…)
  ✅ الباقي متجمع بالنص (النص 984، الشاشة 984)
  ✅ المسافات بينهم متساوية [209.0, 210.0]
  📸 inner_14_dock_out_end.png
    > move 760,1122
    > move 760,1329
    > move 760,1536
    > move 760,1742
    > move 790,1775
    > move 820,1808
    > move 849,1841
    > move 879,1874
  🎞 inner_15_dock_in.mp4 + ورقة لقطات
  رجعت: ['TMoble', 'YT Music', 'Voice Search', 'Settings']
    · drop at=1 order=34,36,35,33
    · dock pages=0,1,2,3 apps=com.android.stk,com.google.android.apps.youtube.music,com.google.android.googlequicksearchbox,com.android.settings
  ✅ رجعت للشريط بالمكان الثاني
  ✅ الشريط متجمع بعد الرجوع
  📸 inner_15_dock_in_end.png
    · start fromSlot=null menuCut=true
    · pointer=760,1122px center=290,428dp size=101x101dp kind=0 k=1.00 page=0 target=MergeAt(id=87)
    · pointer=760,1329px center=290,506dp size=99x99dp kind=0 k=0.98 page=0 target=CellAt(page=0, x=2, y=4)
    · pointer=760,1329px center=290,506dp size=91x91dp kind=0 k=0.90 page=0 target=MergeAt(id=93)
    · pointer=760,1536px center=290,585dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=4)
    · pointer=760,1742px center=290,664dp size=89x89dp kind=0 k=0.88 page=0 target=DockAt(index=0)
    · pointer=790,1775px center=301,676dp size=89x89dp kind=0 k=0.88 page=0 target=DockAt(index=1)
    · drop pointer=879,1874px target=DockAt(index=1)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (الشريط: طلوع أيقونة ورجوعها): أول أيقونة x=127 (المفروض 127)

## النقاط: البيت، الضغط، السحب
  النقاط: 3 صفحات، الحالية 0
  ✅ ضغطة على آخر نقطة ← الصفحة 2
  📸 inner_16_dots_last.png
  ✅ ضغطة على البيت ← الرئيسية
  ✅ الصفحة واقفة صح بعد (ضغطة البيت): أول أيقونة x=127 (المفروض 127)
    > move 952,1715
    > move 968,1715
    > move 984,1715
    > move 999,1715
    > move 1015,1715
    > move 1031,1715
  🎞 inner_16_dots_scrub.mp4 + ورقة لقطات
  ✅ سحب على النقاط ← الصفحة 2
  ✅ الصفحة واقفة صح بعد (سحب النقاط): أول أيقونة x=127 (المفروض 127)
  ✅ الصفحة واقفة صح بعد (النقاط: البيت، الضغط، السحب): أول أيقونة x=127 (المفروض 127)

## القائمة: الإعدادات من أي أيقونة
  ✅ زر الإعدادات موجود بقائمة الأيقونة
  ✅ انفتحت الإعدادات بدون مكان فاضي
  📸 inner_17_menu_settings.png
  ✅ الصفحة واقفة صح بعد (القائمة: الإعدادات من أي أيقونة): أول أيقونة x=127 (المفروض 127)

## الصفحة الفاضية بآخر وضع الترتيب
  📸 inner_18_empty_page.png
  🎞 inner_18_last_page.mp4 + ورقة لقطات
  ✅ طلعت من الصفحة الفاضية بحركة (مو قفزة)
  ✅ وقفت على آخر صفحة فيها شي (2 من 3)
  ✅ الصفحة واقفة صح بعد (الصفحة الأخيرة): أول أيقونة x=127 (المفروض 127)
  ✅ الصفحة واقفة صح بعد (الصفحة الفاضية بآخر وضع الترتيب): أول أيقونة x=127 (المفروض 127)

## المجلد: إنشاء وفتح
    > move 643,449
    > move 750,449
    > move 857,449
    > move 964,449
    > move 974,449
    > move 984,449
    > move 994,449
    > move 1004,449
  🎞 inner_19_folder.mp4 + ورقة لقطات
  ✅ سحب أيقونة فوق أيقونة = مجلد
  ✅ أيقونة المجلد على الشاشة
  ✅ المجلد ينفتح بضغطة (زجاج سائل واسمه فوق)
  📸 inner_19_folder_open.png
  📸 inner_19_folder_closed.png
    · start fromSlot=null menuCut=false
    · pointer=537,449px center=205,171dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=0)
    · pointer=750,449px center=286,171dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=964,449px center=367,171dp size=89x89dp kind=0 k=0.88 page=0 target=MergeAt(id=24)
    · drop pointer=1004,449px target=MergeAt(id=24)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (المجلد: إنشاء وفتح): أول أيقونة x=127 (المفروض 127)

## الدرج: الفئات والبحث والحاسبة
  ✅ السحب لفوق يفتح الدرج وفيه شريط البحث تحت
  ✅ التطبيقات مقسمة فئات: ['التواصل', 'الإنتاجية', 'الصور', 'الفيديو']
  📸 inner_20_drawer.png
  ✅ الحاسبة بالبحث: 25*4 = 100
  📸 inner_20_drawer_calc.png
  ✅ البحث يلقى التطبيق (Sett ← Settings)
  📸 inner_20_drawer_search.png
  ✅ الصفحة واقفة صح بعد (الدرج: الفئات والبحث والحاسبة): أول أيقونة x=127 (المفروض 127)

## قفل الترتيب ما يحبسك
  ✅ الترتيب مقفل والضغطة الطويلة تفتح الإعدادات (ما تنحبس)
  📸 inner_21_lock_settings.png
  ✅ الصفحة واقفة صح بعد (قفل الترتيب ما يحبسك): أول أيقونة x=127 (المفروض 127)

## سحب لتحت (الإشعارات)
  📸 inner_07_shade.png
  ✅ الصفحة واقفة صح بعد (سحب لتحت (الإشعارات)): أول أيقونة x=127 (المفروض 127)

## سحبة قصيرة ترجع، وطويلة تقلب
  سحبة قصيرة بطيئة: ✅ رجعت
  📸 inner_09_page2.png
  ✅ الصفحة واقفة صح بعد (سحبة قصيرة ترجع، وطويلة تقلب): أول أيقونة x=127 (المفروض 127)

