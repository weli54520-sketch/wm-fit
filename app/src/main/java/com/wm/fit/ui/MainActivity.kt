package com.wm.fit.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wm.fit.core.P
import com.wm.fit.core.RestTimer
import com.wm.fit.core.Store
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WMFitTheme { Root() } }
    }

    override fun onResume() {
        super.onResume()
        // لو تغيّر اليوم والتطبيق مفتوح، نحدّث "اليوم" (جواب جسمك، بدائل اليوم)
        Store.load()
    }
}

private enum class Tab(val label: String, val icon: String) {
    HOME("الرئيسية", "🏠"),
    WORKOUT("التمرين", "🏋️"),
    FOOD("الأكل", "🍽️"),
    PROGRESS("التقدم", "📈"),
    SETTINGS("الإعدادات", "⚙️"),
}

/** الشاشات اللي تفتح فوق التبويبات. */
sealed interface Over {
    data object Sources : Over
    data object Injuries : Over
    data object Library : Over
    data object Plan : Over
    data object AddFood : Over
    data object Supps : Over
    data class Detail(val id: String) : Over
}

/** التنقل بين الشاشات. */
class Nav(
    val push: (Over) -> Unit,
    val workout: () -> Unit,
) {
    fun detail(id: String) = push(Over.Detail(id))
    fun injuries() = push(Over.Injuries)
    fun library() = push(Over.Library)
    fun sources() = push(Over.Sources)
    fun plan() = push(Over.Plan)
}

@Composable
private fun Root() {
    // nonce يخلي الواجهة تعيد البناء بعد المسح أو إعادة الأسئلة
    var nonce by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var stack by remember { mutableStateOf(listOf<Over>()) }
    val nav = remember { Nav(push = { stack = stack + it }, workout = { stack = emptyList(); tab = Tab.WORKOUT }) }
    val pop = { stack = stack.dropLast(1) }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }
    BackHandler(enabled = stack.isEmpty() && tab != Tab.HOME && P.onboarded) { tab = Tab.HOME }

    key(nonce) {
        if (!P.onboarded) {
            Onboarding { nonce++ }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(C.bg)
                    .systemBarsPadding(),
            ) {
                when (tab) {
                    Tab.HOME -> Home(nav)
                    Tab.WORKOUT -> WorkoutScreen(nav)
                    Tab.FOOD -> FoodScreen(nav)
                    Tab.PROGRESS -> ProgressScreen(nav)
                    Tab.SETTINGS -> Settings(
                        nav = nav,
                        onRestart = { nonce++; tab = Tab.HOME; stack = emptyList() },
                    )
                }

                // الشاشة اللي فوق تغطي التبويب، والتبويب يبقى بحالته تحتها
                stack.lastOrNull()?.let { over ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(C.bg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                    ) {
                        when (over) {
                            Over.Sources -> Sources(onBack = pop)
                            Over.Injuries -> InjuryScreen(onBack = pop)
                            Over.Library -> Library(nav, onBack = pop)
                            Over.Plan -> PlanScreen(onBack = pop)
                            Over.AddFood -> AddFoodScreen(onBack = pop)
                            Over.Supps -> SuppsScreen(onBack = pop)
                            is Over.Detail -> key(over.id) { ExerciseDetail(over.id, nav, onBack = pop) }
                        }
                    }
                }

                RestBar(Modifier.align(Alignment.BottomCenter).padding(bottom = if (stack.isEmpty()) 92.dp else 16.dp))

                if (stack.isEmpty()) {
                    BottomBar(tab) { tab = it }
                }
            }
        }
    }
}

/** شريط مؤقت الراحة: يطلع فوق كل الشاشات وقت الراحة. */
@Composable
private fun RestBar(modifier: Modifier) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val end = RestTimer.endAt
    LaunchedEffect(end) {
        while (end > System.currentTimeMillis()) {
            now = System.currentTimeMillis()
            delay(250)
        }
        now = System.currentTimeMillis()
    }
    if (end <= now) return
    val left = RestTimer.remainingSec(now)

    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(C.card2)
            .border(1.dp, C.accent.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("راحة", style = MaterialTheme.typography.labelSmall, color = C.muted)
                Text(
                    String.format(Locale.US, "%d:%02d", left / 60, left % 60),
                    style = MaterialTheme.typography.titleLarge,
                    color = C.accent,
                )
            }
            OutlineButton("−15") { RestTimer.add(ctx, -15) }
            OutlineButton("+15") { RestTimer.add(ctx, 15) }
            OutlineButton("تخطّي", color = C.accent) { RestTimer.stop(ctx) }
        }
        Bar(left / RestTimer.total.coerceAtLeast(1).toFloat(), height = 5)
    }
}

@Composable
private fun BoxScope.BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xE0222523))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(34.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t ->
            val on = t == current
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(t.icon, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(3.dp))
                Text(
                    t.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (on) C.accent else C.muted,
                )
            }
        }
    }
}
