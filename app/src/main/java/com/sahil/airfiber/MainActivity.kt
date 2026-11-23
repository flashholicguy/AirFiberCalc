package com.sahil.airfiber

import android.app.DatePickerDialog
import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.*
import android.text.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {

    data class AirFiberPlan(val code: Int, val offering: String, val mrpWithGst: Double, val putDownPrice: Int, val validMonths: Int, val validDays: Int) {
        fun validityText(): String {
            val m = if (validMonths == 1) "1 Month" else "$validMonths Months"
            return when (validDays) { 0 -> m; 1 -> "$m 1 Day"; else -> "$m $validDays Days" }
        }
    }

    private val plans = listOf(
        AirFiberPlan(499, "40 MBPS WiFi ONLY", 588.82, 2500, 4, 7),
        AirFiberPlan(599, "30 MBPS BLACK IPTV + OTT", 706.82, 2500, 3, 16),
        AirFiberPlan(699, "40 MBPS BLACK IPTV + OTT", 824.82, 2500, 3, 1),
        AirFiberPlan(799, "100 MBPS WiFi ONLY", 942.82, 2500, 2, 20),
        AirFiberPlan(899, "100 MBPS BLACK IPTV + OTT", 1060.82, 2500, 2, 11),
        AirFiberPlan(999, "100 MBPS WiFi ONLY + OTT (Amazon & Netflix)", 1178.82, 2500, 2, 4),
        AirFiberPlan(1199, "100 MBPS BLACK IPTV + OTT (Prime & Netflix)", 1414.82, 2500, 1, 23),
        AirFiberPlan(1599, "300 MBPS BLACK IPTV + OTT (Prime & Netflix)", 1886.82, 2500, 1, 10),
        AirFiberPlan(3999, "1 GBPS BLACK IPTV + OTT (Prime & Netflix)", 4718.82, 14000, 3, 0)
    )

    private var selectedPlan: AirFiberPlan = plans.first()
    private var selectedDate: LocalDate = LocalDate.now()
    private var lastWhatsAppReport: String = ""

    private lateinit var etGstBase: EditText
    private lateinit var tvGstResult: TextView
    private lateinit var planChips: MutableMap<Int, TextView>
    private lateinit var tvPlanMeta: TextView
    private lateinit var tvDateBtn: TextView
    private lateinit var tvSummaryOutput: TextView
    private lateinit var ledgerContainer: LinearLayout

    private val fullDateFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    private val shortMonthFmt = DateTimeFormatter.ofPattern("MMM''yy", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableMaxRefreshRate()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val rootScroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_ALWAYS }
        val mainColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(46), dp(16), dp(36))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(16))
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f)
            addView(TextView(this@MainActivity).apply { text = "AirFiber Billing & GST"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#F8FAFC")) })
            addView(TextView(this@MainActivity).apply { text = "Plan Validity & 27th Cycle Calculator"; textSize = 12f; setTextColor(Color.parseColor("#94A3B8")); setPadding(0, dp(2), 0, 0) })
        }
        val creditCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.glass_pill); setPadding(dp(10), dp(7), dp(10), dp(7))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.05f).apply { marginStart = dp(8) }
            addView(TextView(this@MainActivity).apply { text = "Made with ❤️ by Sahil Ahmed"; textSize = 10.5f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#E0F2FE")) })
            addView(TextView(this@MainActivity).apply { text = "Field Service Engineer at\nHindustan Enterprises"; textSize = 9.5f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#94A3B8")); setPadding(0, dp(2), 0, 0) })
        }
        headerRow.addView(titleCol); headerRow.addView(creditCol); mainColumn.addView(headerRow)

        val gstCard = createGlassCard().apply {
            addView(sectionLabel("1. QUICK 18% GST CALCULATOR"))
            val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
            etGstBase = createGlassInput("Base Plan (e.g. 499)").apply { setText("499"); layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f) }
            tvGstResult = TextView(this@MainActivity).apply { text = "₹588.82"; textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#38BDF8")); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            row.addView(etGstBase); row.addView(tvGstResult); addView(row)
            val chipScroll = HorizontalScrollView(this@MainActivity).apply { isHorizontalScrollBarEnabled = false }
            val chipRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            plans.forEachIndexed { idx, p ->
                chipRow.addView(createPill(p.code.toString(), false).apply {
                    setPadding(dp(11), dp(6), dp(11), dp(6)); textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (idx > 0) marginStart = dp(6) }
                    setOnClickListener { etGstBase.setText(p.code.toString()) }
                })
            }
            chipScroll.addView(chipRow); addView(chipScroll)
        }
        mainColumn.addView(gstCard)

        val advCard = createGlassCard().apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) }
            addView(sectionLabel("2. AIRFIBER PLAN & BILL CALCULATOR"))
            val planScroll = HorizontalScrollView(this@MainActivity).apply { isHorizontalScrollBarEnabled = false; setPadding(0, dp(10), 0, dp(8)) }
            val planRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            planChips = mutableMapOf()
            plans.forEachIndexed { idx, p ->
                val chip = createPill("₹${p.code}", p.code == selectedPlan.code).apply {
                    setPadding(dp(14), dp(9), dp(14), dp(9)); textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (idx > 0) marginStart = dp(8) }
                    setOnClickListener { selectedPlan = p; etGstBase.setText(p.code.toString()); updatePlanChipsUI(); recalculateLedger() }
                }
                planChips[p.code] = chip; planRow.addView(chip)
            }
            planScroll.addView(planRow); addView(planScroll)
            tvPlanMeta = TextView(this@MainActivity).apply {
                setBackgroundResource(R.drawable.glass_pill); setPadding(dp(12), dp(9), dp(12), dp(9)); textSize = 12f; setTextColor(Color.parseColor("#CBD5E1"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
            }
            addView(tvPlanMeta)
            val dateRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(2), 0, dp(12)) }
            tvDateBtn = createPill("Install: ${selectedDate.format(fullDateFmt)}", false).apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 1.35f); setOnClickListener { openDatePicker() } }
            val cycleBadge = createPill("Cycle: 27th", true).apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 0.95f).apply { marginStart = dp(8) } }
            dateRow.addView(tvDateBtn); dateRow.addView(cycleBadge); addView(dateRow)
            tvSummaryOutput = TextView(this@MainActivity).apply { setBackgroundResource(R.drawable.glass_pill_active); setPadding(dp(14), dp(12), dp(14), dp(12)); textSize = 13.5f; setLineSpacing(dp(3).toFloat(), 1f); setTextColor(Color.parseColor("#F8FAFC")) }
            addView(tvSummaryOutput)
            val actionRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, dp(10)) }
            actionRow.addView(createPill("Copy Bill Text", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                setOnClickListener { (getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(ClipData.newPlainText("Bill Details", lastWhatsAppReport)); Toast.makeText(this@MainActivity, "Copied WhatsApp bill text", Toast.LENGTH_SHORT).show() }
            })
            actionRow.addView(createPill("Share WhatsApp", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(8) }
                setOnClickListener { shareText(lastWhatsAppReport) }
            })
            addView(actionRow)
            addView(sectionLabel("PLAN & VALIDITY BREAKDOWN").apply { setPadding(0, dp(6), 0, dp(8)) })
            ledgerContainer = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            addView(ledgerContainer)
        }
        mainColumn.addView(advCard)

        val footerCard = createGlassCard().apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) }
            addView(sectionLabel("DEVELOPER & APP UPDATES"))
            addView(TextView(this@MainActivity).apply { text = "Feedback appreciated — reach out or check source updates:"; textSize = 12f; setTextColor(Color.parseColor("#94A3B8")); setPadding(0, dp(6), 0, dp(10)) })
            addView(createIconLinkRow(R.drawable.ic_email, "sahilahmed698@gmail.com") { runCatching { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:sahilahmed698@gmail.com")).apply { putExtra(Intent.EXTRA_SUBJECT, "AirFiber Calc Feedback") }) } })
            addView(createIconLinkRow(R.drawable.ic_github, "github.com/sahilhv") { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/flashholicguy"))) } }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(8) })
            addView(createPill("Check Updates & Releases (v1.2)", true).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(10) }
                setOnClickListener { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/flashholicguy/AirFiberCalc/releases"))) } }
            })
        }
        mainColumn.addView(footerCard)
        rootScroll.addView(mainColumn)
        setContentView(rootScroll)

        etGstBase.addTextChangedListener(simpleWatcher { updateGst() })
        updateGst()
        recalculateLedger()
    }

    private fun createIconLinkRow(iconRes: Int, label: String, onClick: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundResource(R.drawable.glass_pill); setPadding(dp(14), dp(11), dp(14), dp(11))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        addView(ImageView(this@MainActivity).apply { setImageResource(iconRes); layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)) })
        addView(TextView(this@MainActivity).apply { text = label; textSize = 13.5f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#F8FAFC")); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) } })
        setOnClickListener { onClick() }
    }

    private fun enableMaxRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.supportedModes?.maxByOrNull { it.refreshRate }?.let { window.attributes = window.attributes.apply { preferredDisplayModeId = it.modeId } }
        }
    }

    private fun updateGst() {
        val base = etGstBase.text.toString().toDoubleOrNull() ?: 0.0
        tvGstResult.text = "₹${formatMoney(round2(base * 1.18))}"
    }

    private fun openDatePicker() {
        DatePickerDialog(this, { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d); tvDateBtn.text = "Install: ${selectedDate.format(fullDateFmt)}"; recalculateLedger() }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
    }

    private fun recalculateLedger() {
        val p = selectedPlan
        tvPlanMeta.text = "${p.offering}\nMRP w/ 18% GST: ₹${formatMoney(p.mrpWithGst)}  •  Put Down: ₹${p.putDownPrice}  •  Validity: ${p.validityText()}"
        val validityEndDate = selectedDate.plusMonths(p.validMonths.toLong()).plusDays(p.validDays.toLong())
        var cycleDate = if (selectedDate.dayOfMonth <= 27) selectedDate.withDayOfMonth(27) else selectedDate.plusMonths(1).withDayOfMonth(27)
        val monthLines = mutableListOf<String>()
        while (!cycleDate.isAfter(validityEndDate)) {
            monthLines.add("${cycleDate.format(shortMonthFmt)} Bill - Nil")
            cycleDate = cycleDate.plusMonths(1)
        }
        val payableBillDate = cycleDate
        val dueDate = payableBillDate.plusDays(10)
        val remainingDays = ChronoUnit.DAYS.between(validityEndDate, payableBillDate).toInt().coerceIn(1, 30)
        val remainingAmount = round2((p.mrpWithGst / 30.0) * remainingDays)
        val afterDiscount = round2((remainingAmount - 100.0).coerceAtLeast(0.0))
        monthLines.add("${payableBillDate.format(shortMonthFmt)} Bill - ₹${formatMoney(remainingAmount)}")
        val monthWiseBlock = monthLines.joinToString("\n")

        val exactOutput = "✅ Bill Details\n\n" +
            "📦 Plan : ${p.code}\n" +
            "📅 Install Date : ${selectedDate.format(fullDateFmt)}\n" +
            "🔄 Billing Cycle : 27th of every month\n\n" +
            "📊 Month Wise Bill - Payable Amount\n\n" +
            "$monthWiseBlock\n\n" +
            "🗓️ Payable Bill Date : ${payableBillDate.format(fullDateFmt)}\n" +
            "⏰ Due Date : ${dueDate.format(fullDateFmt)}\n\n" +
            "🎁 Get ₹100 Discount for the next 10 month\n" +
            "Pay your bill through Airtel Thanks App\n\n" +
            "💵 Payable Amount After Discount : ₹${formatMoney(afterDiscount)} (With GST)\n\n" +
            "🔗 https://www.airtel.in/thanks"

        lastWhatsAppReport = exactOutput
        tvSummaryOutput.text = exactOutput
        ledgerContainer.removeAllViews()
        listOf(
            "Plan Offering: ${p.offering}",
            "Put Down Paid at Install: ₹${p.putDownPrice} (Covers ${p.validityText()})",
            "Validity Ends On: ${validityEndDate.format(fullDateFmt)}",
            "Unbilled Days Before ${payableBillDate.format(fullDateFmt)}: $remainingDays days",
            "Prorated Bill: (₹${formatMoney(p.mrpWithGst)} ÷ 30) × $remainingDays = ₹${formatMoney(remainingAmount)}",
            "After ₹100 Airtel Thanks Discount: ₹${formatMoney(afterDiscount)}"
        ).forEachIndexed { idx, line ->
            ledgerContainer.addView(TextView(this).apply {
                setBackgroundResource(if (idx == 5) R.drawable.glass_pill_active else R.drawable.glass_pill)
                setPadding(dp(12), dp(9), dp(12), dp(9)); textSize = 12.5f; setTextColor(Color.parseColor("#E2E8F0")); text = line
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (idx > 0) topMargin = dp(6) }
            })
        }
    }

    private fun shareText(content: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, content) }, "Send Bill Details")) }
    private fun updatePlanChipsUI() { planChips.forEach { (code, v) -> v.setBackgroundResource(if (code == selectedPlan.code) R.drawable.glass_pill_active else R.drawable.glass_pill) } }
    private fun createGlassCard(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundResource(R.drawable.glass_card); setPadding(dp(16), dp(16), dp(16), dp(16)) }
    private fun createGlassInput(h: String): EditText = EditText(this).apply { hint = h; setHintTextColor(Color.parseColor("#64748B")); setTextColor(Color.parseColor("#F8FAFC")); textSize = 16f; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setBackgroundResource(R.drawable.glass_pill); setPadding(dp(12), dp(8), dp(12), dp(8)) }
    private fun createPill(l: String, active: Boolean): TextView = TextView(this).apply { text = l; gravity = Gravity.CENTER; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#F8FAFC")); setBackgroundResource(if (active) R.drawable.glass_pill_active else R.drawable.glass_pill) }
    private fun sectionLabel(t: String): TextView = TextView(this).apply { text = t; textSize = 11f; letterSpacing = 0.08f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#7DD3FC")) }
    private fun simpleWatcher(cb: () -> Unit): TextWatcher = object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}; override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}; override fun afterTextChanged(s: Editable?) = cb() }
    private fun round2(v: Double): Double = (v * 100.0).roundToLong() / 100.0
    private fun formatMoney(v: Double): String = String.format(Locale.ENGLISH, "%.2f", v)
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
