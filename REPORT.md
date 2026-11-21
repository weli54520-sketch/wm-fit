# WM Onyx · الجوال الوهمي
14 · API 34

# الشاشة: outer (1080×2520)

## الشاشة الرئيسية
  📸 outer_01_home.png
  أيقونات على الشاشة: 26 · المقاسات (px): [159]
  أيقونات يمين الشاشة (العمود؟): 2 · x من 886 إلى 1045
  المسافة بين آخر أيقونة بالشبكة وأيقونات العمود: 49px ✅
  المسافة بين أيقونات العمود (px): [395]
  ✅ الصفحة واقفة صح بعد (الشاشة الرئيسية): أول أيقونة x=65 (المفروض 65)

## تقليب الصفحات (سريع، بطيء، رجوع)
  🎞 outer_02_pages.mp4 + ورقة لقطات
  📸 outer_02_pages_end.png
  الصفحات بعد كل سحبة: ['1', '1', '0']
  ✅ اتجاه التقليب صح (ما يرجع للخلف بالغلط)
  النعومة (جوال وهمي بطيء، للمقارنة بس): إطارات متأخرة 97.22% · 90%=250ms · 99%=600ms
  ✅ الصفحة واقفة صح بعد (تقليب الصفحات (سريع، بطيء، رجوع)): أول أيقونة x=65 (المفروض 65)

## بوضع الترتيب: سحبة سريعة فوق الأيقونات
  ✅ سحبة سريعة بوضع الترتيب قلبت الصفحة
  📸 outer_11_edit_swipe.png
  ✅ الصفحة واقفة صح بعد (بوضع الترتيب: سحبة سريعة فوق الأيقونات): أول أيقونة x=65 (المفروض 65)

## ضغطة طويلة على أيقونة ورفع
  📸 outer_03_menu.png
  ✅ الصفحة واقفة صح بعد (ضغطة طويلة على أيقونة ورفع): أول أيقونة x=65 (المفروض 65)

## سحب أيقونة (يمر بسرعة، وبعدين يوقف)
    > move 195,646
    > move 246,646
    > move 297,646
    > move 348,646
    > move 399,646
    > move 450,646
    > move 501,646
    > move 552,646
    > move 552,697
    > move 552,748
    > move 552,799
    > move 552,850
    > move 501,850
    > move 450,850
    > move 399,850
    > move 348,850
    > move 399,799
    > move 450,748
    > move 501,697
    > move 552,646
  🎞 outer_04_drag.mp4 + ورقة لقطات
  📸 outer_04_drag_end.png
    · pointer=246,646px center=94,246dp size=78x78dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=0, y=0)
    · pointer=297,646px center=113,246dp size=77x77dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=1, y=0)
    · pointer=450,646px center=172,246dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=552,799px center=210,304dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=1)
    · pointer=399,850px center=152,324dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · drop pointer=348,850px target=CellAt(page=0, x=1, y=1)
    · pointer=348,850px center=137,338dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · pointer=450,748px center=175,299dp size=69x69dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · drop pointer=552,646px target=CellAt(page=0, x=2, y=0)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (سحب أيقونة (يمر بسرعة، وبعدين يوقف)): أول أيقونة x=65 (المفروض 65)

## نقل أيقونة لحافة الشاشة (صفحة ثانية)
    > move 784,1668
    > move 810,1668
    > move 837,1668
    > move 864,1668
    > move 909,1668
    > move 955,1668
    > move 1001,1668
    > move 1047,1668
  🎞 outer_05_edge.mp4 + ورقة لقطات
  📸 outer_05_edge_dropped.png
    > move 648,1668
    > move 864,1668
    > move 972,1668
    > move 1047,1668
    > move 486,1668
  🎞 outer_05b_edge_hold.mp4 + ورقة لقطات
  ✅ نقل أيقونة لصفحة ثانية (نزلت بالصفحة 1)
  📸 outer_05_edge_end.png
    · pointer=810,1668px center=309,635dp size=78x78dp kind=0 k=1.00 page=0 target=EdgeAt(dir=1)
    · drop pointer=1047,1668px target=CellAt(page=2, x=3, y=5)
    · pointer=648,1668px center=247,635dp size=78x78dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=2, y=5)
    · pointer=864,1668px center=329,635dp size=74x74dp kind=0 k=0.95 page=0 target=EdgeAt(dir=1)
    · pointer=486,1668px center=185,635dp size=69x69dp kind=0 k=0.88 page=1 target=CellAt(page=1, x=2, y=5)
    · drop pointer=486,1668px target=CellAt(page=1, x=2, y=5)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 0dp
  ✅ الصفحة واقفة صح بعد (نقل أيقونة لحافة الشاشة (صفحة ثانية)): أول أيقونة x=65 (المفروض 65)

## وضع الترتيب + ضغطة على فاضي
  📸 outer_06_edit.png
  📸 outer_06b_edit_after_tap.png
  ✅ الصفحة واقفة صح بعد (وضع الترتيب + ضغطة على فاضي): أول أيقونة x=65 (المفروض 65)

## تبديل أيقونة مع العمود
  قبل: الشبكة=Calendar العمود=Phone
    > move 641,700
    > move 729,753
    > move 817,807
    > move 905,860
    > move 920,860
    > move 935,860
    > move 950,860
    > move 965,860
  🎞 outer_08_side_swap.mp4 + ورقة لقطات
  بعد: العمود=Calendar ✅ تبادلوا
  📸 outer_08_side_swap_end.png
    · pointer=641,700px center=244,267dp size=78x78dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=729,753px center=278,287dp size=72x72dp kind=0 k=0.92 page=0 target=CellAt(page=0, x=3, y=0)
    · pointer=817,807px center=311,307dp size=69x69dp kind=0 k=0.88 page=0 target=EdgeAt(dir=1)
    · pointer=905,860px center=345,328dp size=69x69dp kind=0 k=0.88 page=0 target=SlotAt(container=1, index=0)
    · drop pointer=965,860px target=SlotAt(container=1, index=0)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (تبديل أيقونة مع العمود): أول أيقونة x=65 (المفروض 65)

## ويدجت: إضافة، قائمة، سحب
  📸 outer_10_gallery.png
  📸 outer_10_widget_added.png
  ✅ انضاف ويدجت: 409×613px
  📸 outer_10_widget_menu.png
    > move 450,1400
    > move 450,1338
    > move 450,1275
    > move 450,1213
    > move 450,1175
    > move 450,1138
    > move 450,1100
    > move 450,1063
  🎞 outer_10_widget_drag.mp4 + ورقة لقطات
  ✅ الويدجت وهو يتسحب ما يكبر: 137×206dp (الويدجت 156×234dp)
  📸 outer_10_widget_drag_end.png
    > move 622,1331
    > move 658,1367
    > move 694,1403
    > move 730,1439
    > move 766,1475
    > move 802,1511
    > move 838,1547
    > move 874,1583
  🎞 outer_12_widget_resize.mp4 + ورقة لقطات
  ✅ التكبير يتبع الإصبع: [171, 187, 202, 218, 233, 234, 234, 234]
  ❌ بعد ما فلت: 2×3 ← 2×3
  📸 outer_12_widget_resize_end.png
  ✅ الصفحة واقفة صح بعد (ويدجت: إضافة، قائمة، سحب): أول أيقونة x=65 (المفروض 65)

## سحب لتحت (الإشعارات)
  📸 outer_07_shade.png
  ✅ الصفحة واقفة صح بعد (سحب لتحت (الإشعارات)): أول أيقونة x=65 (المفروض 65)

## سحبة قصيرة ترجع، وطويلة تقلب
  سحبة قصيرة بطيئة: ✅ رجعت
  📸 outer_09_page2.png
  ✅ الصفحة واقفة صح بعد (سحبة قصيرة ترجع، وطويلة تقلب): أول أيقونة x=65 (المفروض 65)

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
  الصفحات بعد كل سحبة: ['1', '1', '0']
  ✅ اتجاه التقليب صح (ما يرجع للخلف بالغلط)
  النعومة (جوال وهمي بطيء، للمقارنة بس): إطارات متأخرة 100.00% · 90%=200ms · 99%=250ms
  ✅ الصفحة واقفة صح بعد (تقليب الصفحات (سريع، بطيء، رجوع)): أول أيقونة x=127 (المفروض 127)

## بوضع الترتيب: سحبة سريعة فوق الأيقونات
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
    · pointer=296,385px center=113,147dp size=101x101dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=0, y=0)
    · pointer=429,385px center=164,147dp size=92x92dp kind=0 k=0.91 page=0 target=CellAt(page=0, x=1, y=0)
    · pointer=694,385px center=265,147dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · pointer=760,584px center=290,223dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=1)
    · pointer=628,650px center=240,248dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · drop pointer=495,650px target=CellAt(page=0, x=1, y=1)
    · pointer=495,650px center=205,260dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=1, y=1)
    · pointer=628,518px center=255,210dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=2, y=0)
    · drop pointer=760,385px target=CellAt(page=0, x=2, y=0)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (سحب أيقونة (يمر بسرعة، وبعدين يوقف)): أول أيقونة x=127 (المفروض 127)

## نقل أيقونة لحافة الشاشة (صفحة ثانية)
    > move 1360,1444
    > move 1431,1444
    > move 1503,1444
    > move 1574,1444
    > move 1658,1444
    > move 1741,1444
    > move 1825,1444
    > move 1908,1444
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
    · pointer=1360,1444px center=519,550dp size=101x101dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=4, y=4)
    · pointer=1431,1444px center=546,550dp size=93x93dp kind=0 k=0.92 page=0 target=CellAt(page=0, x=5, y=4)
    · pointer=1658,1444px center=632,550dp size=89x89dp kind=0 k=0.88 page=0 target=EdgeAt(dir=1)
    · drop pointer=1908,1444px target=CellAt(page=1, x=5, y=4)
    · pointer=1180,1444px center=450,550dp size=101x101dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=4, y=4)
    · pointer=1574,1444px center=600,550dp size=101x101dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=5, y=4)
    · pointer=1771,1444px center=675,550dp size=89x89dp kind=0 k=0.88 page=0 target=EdgeAt(dir=1)
    · pointer=885,1444px center=337,550dp size=89x89dp kind=0 k=0.88 page=1 target=CellAt(page=1, x=2, y=4)
    · drop pointer=885,1444px target=CellAt(page=1, x=2, y=4)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (نقل أيقونة لحافة الشاشة (صفحة ثانية)): أول أيقونة x=127 (المفروض 127)

## وضع الترتيب + ضغطة على فاضي
  📸 inner_06_edit.png
  📸 inner_06b_edit_after_tap.png
  ✅ الصفحة واقفة صح بعد (وضع الترتيب + ضغطة على فاضي): أول أيقونة x=127 (المفروض 127)

## تبديل أيقونة مع العمود
  قبل: الشبكة=Calendar العمود=Phone
    > move 1016,435
    > move 1272,485
    > move 1528,535
    > move 1785,585
    > move 1800,585
    > move 1815,585
    > move 1830,585
    > move 1845,585
  🎞 inner_08_side_swap.mp4 + ورقة لقطات
  بعد: العمود=Calendar ✅ تبادلوا
  📸 inner_08_side_swap_end.png
    · pointer=1016,435px center=387,166dp size=101x101dp kind=0 k=1.00 page=0 target=CellAt(page=0, x=3, y=0)
    · pointer=1272,485px center=485,185dp size=90x90dp kind=0 k=0.89 page=0 target=CellAt(page=0, x=4, y=0)
    · pointer=1528,535px center=582,204dp size=89x89dp kind=0 k=0.88 page=0 target=CellAt(page=0, x=5, y=0)
    · pointer=1785,585px center=680,223dp size=89x89dp kind=0 k=0.88 page=0 target=SlotAt(container=1, index=0)
    · drop pointer=1845,585px target=SlotAt(container=1, index=0)
  ✅ الأيقونة ماسكة نفس النقطة تحت الإصبع: أكبر تغير 1dp
  ✅ الصفحة واقفة صح بعد (تبديل أيقونة مع العمود): أول أيقونة x=127 (المفروض 127)

## ويدجت: إضافة، قائمة، سحب
  📸 inner_10_gallery.png
  📸 inner_10_widget_added.png
  ✅ انضاف ويدجت: 265×530px
  📸 inner_10_widget_menu.png
    > move 760,455
    > move 760,393
    > move 760,330
    > move 760,268
    > move 760,230
    > move 760,193
    > move 760,155
    > move 760,118
  🎞 inner_10_widget_drag.mp4 + ورقة لقطات
  ✅ الويدجت وهو يتسحب ما يكبر: 89×178dp (الويدجت 101×202dp)
  📸 inner_10_widget_drag_end.png
    > move 892,804
    > move 939,851
    > move 985,897
    > move 1032,944
    > move 1079,991
    > move 1125,1037
    > move 1172,1084
    > move 1218,1130
  🎞 inner_12_widget_resize.mp4 + ورقة لقطات
  ✅ التكبير يتبع الإصبع: [121, 141, 161, 181, 202, 222, 242, 262]
  ✅ بعد ما فلت: 1×2 ← 2×3
  📸 inner_12_widget_resize_end.png
  ✅ الصفحة واقفة صح بعد (ويدجت: إضافة، قائمة، سحب): أول أيقونة x=127 (المفروض 127)

## سحب لتحت (الإشعارات)
  📸 inner_07_shade.png
  ✅ الصفحة واقفة صح بعد (سحب لتحت (الإشعارات)): أول أيقونة x=127 (المفروض 127)

## سحبة قصيرة ترجع، وطويلة تقلب
  سحبة قصيرة بطيئة: ✅ رجعت
  📸 inner_09_page2.png
  ✅ الصفحة واقفة صح بعد (سحبة قصيرة ترجع، وطويلة تقلب): أول أيقونة x=127 (المفروض 127)
