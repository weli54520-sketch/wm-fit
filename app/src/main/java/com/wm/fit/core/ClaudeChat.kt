package com.wm.fit.core

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.OverloadedException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams

/**
 * المحادثة الذكية مع Claude.
 *
 * الخصوصية: مقفلة افتراضياً. لما المستخدم يفعّلها، يرسل لـ Anthropic **بس** اللي يكتبه
 * في المحادثة، وملخص خطته **لو هو اختار يشاركه** (وتنعرض له قبل الإرسال).
 * المحادثة نفسها ما تنحفظ في الجوال، تنمسح لما تقفل التطبيق.
 */
object ClaudeChat {
    const val MODEL = "claude-opus-5-5"

    data class Msg(val user: Boolean, val text: String)

    sealed class Result {
        data class Ok(val text: String) : Result()
        data class Err(val text: String) : Result()
    }

    private fun system(context: String?): String = buildString {
        append(
            "أنت مدرب شخصي داخل تطبيق WM Fit. المستخدم يتمرن في النادي.\n" +
                "- رد باللهجة السعودية، بجمل قصيرة وواضحة.\n" +
                "- أي مصطلح إنجليزي حطه بين قوسين مع معناه.\n" +
                "- لو فيه خطوات، اكتبها مرقمة.\n" +
                "- اعتمد على أبحاث منشورة (مثل ACSM و ISSN)، ولو مو متأكد قل كذا.\n" +
                "- أنت مو طبيب. لو فيه ألم قوي، أو أعراض قلب، أو مرض، أو أدوية، انصحه يراجع طبيب.\n" +
                "- لا تطلب منه أي معلومات شخصية زيادة.\n",
        )
        if (!context.isNullOrBlank()) {
            append("\nملخص وضع المستخدم (هو اختار يشاركه):\n")
            append(context)
        }
    }

    /** ملخص الخطة اللي ينرسل لو المستخدم فعّل المشاركة. ينعرض له كامل قبل الإرسال. */
    fun contextSummary(): String = buildString {
        val d = Calc.daily()
        append("- الهدف: ${goalAr()}\n")
        append("- العمر ${P.age}، الطول ${P.heightCm} سم، الوزن ${Workout.kg(P.weightKg)} كجم، ")
        append(if (P.sex == "f") "أنثى\n" else "ذكر\n")
        append("- المستوى: ${when (P.level) { "beg" -> "مبتدئ"; "mid" -> "متوسط"; else -> "متقدم" }}، ")
        append("${P.daysPerWeek} أيام تمرين بالأسبوع\n")
        val f = P.focus.mapNotNull { Muscle.byId(it)?.ar }
        append("- عضلات التركيز: ${if (f.isEmpty()) "ولا شي (متوازنة)" else f.joinToString("، ")}\n")
        append("- السعرات ${d.kcal}، البروتين ${d.protein} جم باليوم\n")
        if (Store.injuries.isNotEmpty()) {
            append("- إصابات: " + Store.injuries.values.joinToString("، ") {
                "${it.area.ar} (${if (it.chronic) "مزمنة" else "جديدة"}، ${listOf("", "خفيف", "متوسط", "قوي")[it.level]})"
            } + "\n")
        }
    }

    private fun goalAr() = when (P.goal) {
        Goal.CUT -> "تنشيف"
        Goal.BULK -> "تضخيم"
        Goal.MAINTAIN -> "تثبيت"
        else -> "لياقة"
    }

    /** يرسل المحادثة. يشتغل على خيط خلفي (مو الواجهة). */
    fun send(history: List<Msg>, context: String?): Result {
        val key = Secrets.key() ?: return Result.Err("ما فيه مفتاح محفوظ. حطه من الإعدادات.")
        val client = AnthropicOkHttpClient.builder().apiKey(key).build()
        return try {
            val b = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(16000L)
                .system(system(context))
                .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.MEDIUM).build())
                // لو رفض نموذج السؤال، الخادم يحوّله لنموذج ثاني مناسب لحاله
                .addBeta("server-side-fallback-2026-07-01")
                .fallbacksDefault()
            history.forEach { if (it.user) b.addUserMessage(it.text) else b.addAssistantMessage(it.text) }
            val r = client.beta().messages().create(b.build())
            if (r.stopReason().orElse(null) == BetaStopReason.REFUSAL) {
                return Result.Err("ما قدر يجاوب على هذا السؤال. جرّب تصيغه بطريقة ثانية.")
            }
            val text = r.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("\n").trim()
            if (text.isEmpty()) Result.Err("ما جا رد. جرّب مرة ثانية.") else Result.Ok(text)
        } catch (e: UnauthorizedException) {
            Result.Err("المفتاح غلط أو انلغى. تأكد منه في الإعدادات.")
        } catch (e: PermissionDeniedException) {
            Result.Err("المفتاح ما عنده صلاحية. تأكد من حسابك في Anthropic.")
        } catch (e: RateLimitException) {
            Result.Err("رسائل كثيرة ورا بعض. استنى دقيقة وجرّب.")
        } catch (e: OverloadedException) {
            Result.Err("الخدمة مشغولة الحين. جرّب بعد شوي.")
        } catch (e: AnthropicServiceException) {
            Result.Err("صار خطأ من الخدمة (${e.statusCode()}). جرّب بعد شوي.")
        } catch (e: AnthropicIoException) {
            Result.Err("تأكد من النت وجرّب مرة ثانية.")
        } catch (e: Exception) {
            Result.Err("صار خطأ غير متوقع. جرّب مرة ثانية.")
        } finally {
            client.close()
        }
    }
}
