package com.sahil.airfiber

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.content.*
import android.graphics.*
import android.net.Uri
import android.os.*
import android.text.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
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
    private var selectedAdvance: Double = 2500.0
    private var selectedDate: LocalDate = LocalDate.now()
    private enum class CycleDetectionState {
        AUTO_VERIFIED,
        MANUAL_SELECTED,
        UNVERIFIED
    }

    private var selectedCycleDay: Int? = null
    private var cycleState: CycleDetectionState = CycleDetectionState.UNVERIFIED
    private var lastWhatsAppReport: String = ""

    private lateinit var etGstBase: EditText
    private lateinit var tvGstResult: TextView
    private lateinit var etPlanBase: EditText
    private lateinit var btnAdv1500: TextView
    private lateinit var btnAdv2500: TextView
    private lateinit var planChips: MutableMap<Int, TextView>
    private lateinit var tvPlanMeta: TextView
    private lateinit var tvDateBtn: TextView
    private lateinit var tvCycleBtn: TextView
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
            addView(TextView(this@MainActivity).apply { text = "Plan Validity & Billing Cycle Calculator"; textSize = 12f; setTextColor(Color.parseColor("#94A3B8")); setPadding(0, dp(2), 0, 0) })
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
                    setOnClickListener { selectedPlan = p; etPlanBase.setText(p.code.toString()); etGstBase.setText(p.code.toString()); selectedAdvance = if (p.code == 3999) 14000.0 else if (selectedAdvance == 14000.0) 2500.0 else selectedAdvance; updateAdvanceToggleUI(); updatePlanChipsUI(); syncCycleWithInputs(); recalculateLedger() }
                }
                planChips[p.code] = chip; planRow.addView(chip)
            }
            planScroll.addView(planRow); addView(planScroll)
            val advRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(2), 0, dp(8))
            }
            etPlanBase = createGlassInput("Plan (499)").apply {
                setText("499")
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
            }
            btnAdv1500 = createPill("₹1500 Adv", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(8) }
                setOnClickListener { selectedAdvance = 1500.0; updateAdvanceToggleUI(); syncCycleWithInputs(); recalculateLedger() }
            }
            btnAdv2500 = createPill("₹2500 Adv", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(8) }
                setOnClickListener { selectedAdvance = 2500.0; updateAdvanceToggleUI(); syncCycleWithInputs(); recalculateLedger() }
            }
            advRow.addView(etPlanBase); advRow.addView(btnAdv1500); advRow.addView(btnAdv2500)
            addView(advRow)

            tvPlanMeta = TextView(this@MainActivity).apply {
                setBackgroundResource(R.drawable.glass_pill); setPadding(dp(12), dp(9), dp(12), dp(9)); textSize = 12f; setTextColor(Color.parseColor("#CBD5E1"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
            }
            addView(tvPlanMeta)
            val dateRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(2), 0, dp(12)) }
            tvDateBtn = createPill("Install: ${selectedDate.format(fullDateFmt)}", false).apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 1.35f); setOnClickListener { openDatePicker() } }
            tvCycleBtn = createPill("Cycle not verified", false).apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 1.05f).apply { marginStart = dp(8) }; setOnClickListener { openCycleSelector() } }
            dateRow.addView(tvDateBtn); dateRow.addView(tvCycleBtn); addView(dateRow)
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
            addView(createPill("Check Updates & Releases (v1.9-beta.1)", true).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(10) }
                setOnClickListener { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/flashholicguy/AirFiberCalc/releases"))) } }
            })
        }
        mainColumn.addView(footerCard)
        rootScroll.addView(mainColumn)
        setContentView(rootScroll)

        etGstBase.addTextChangedListener(simpleWatcher { updateGst() })
        etPlanBase.addTextChangedListener(simpleWatcher { syncCycleWithInputs(); recalculateLedger() })
        updateGst()
        syncCycleWithInputs()
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
        showGlassDatePicker(selectedDate) { picked ->
            selectedDate = picked
            tvDateBtn.text = "Install: ${selectedDate.format(fullDateFmt)}"
            syncCycleWithInputs()
            recalculateLedger()
        }
    }


    private fun showGlassDatePicker(initialDate: LocalDate, onDateConfirmed: (LocalDate) -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        var currentMonth = YearMonth.from(initialDate)
        var tempSelected = initialDate
        var yearPickerMode = false
        val today = LocalDate.now()
        val locale = Locale.getDefault()
        val firstDow = WeekFields.of(locale).firstDayOfWeek
        val orderedDaysOfWeek = (0 until 7).map { firstDow.plus(it.toLong()) }

        val cardBg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#F20B1324"), Color.parseColor("#F2111D35"), Color.parseColor("#F2080E1C"))
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.parseColor("#557DD3FC"))
        }

        val rootCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val topTagRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }
        topTagRow.addView(sectionLabel("SELECT INSTALL DATE").apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val tvSelectedPreview = TextView(this).apply {
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
        }
        topTagRow.addView(tvSelectedPreview)
        rootCard.addView(topTagRow)

        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(10))
        }
        val btnPrev = createPill("‹", false).apply {
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(40))
        }
        val tvMonthYear = createPill("", true).apply {
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
            }
        }
        val btnNext = createPill("›", false).apply {
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(40))
        }
        navRow.addView(btnPrev)
        navRow.addView(tvMonthYear)
        navRow.addView(btnNext)
        rootCard.addView(navRow)

        val weekHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(6))
        }
        orderedDaysOfWeek.forEach { dow ->
            weekHeaderRow.addView(TextView(this).apply {
                text = dow.getDisplayName(TextStyle.SHORT_STANDALONE, locale).take(3).uppercase(locale)
                gravity = Gravity.CENTER
                textSize = 10.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (dow == DayOfWeek.SUNDAY) Color.parseColor("#7DD3FC") else Color.parseColor("#94A3B8"))
                layoutParams = LinearLayout.LayoutParams(0, dp(24), 1f)
            })
        }
        rootCard.addView(weekHeaderRow)

        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootCard.addView(gridContainer)

        lateinit var renderCalendar: () -> Unit
        renderCalendar = {
            tvSelectedPreview.text = tempSelected.format(fullDateFmt)
            val monthName = currentMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            tvMonthYear.text = if (yearPickerMode) "${currentMonth.year}  ▴" else "$monthName ${currentMonth.year}  ▾"
            weekHeaderRow.visibility = if (yearPickerMode) View.GONE else View.VISIBLE
            gridContainer.removeAllViews()

            if (yearPickerMode) {
                val startYear = currentMonth.year - 5
                for (r in 0 until 4) {
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(3), 0, dp(3))
                    }
                    for (c in 0 until 3) {
                        val yr = startYear + (r * 3) + c
                        val isCurYr = yr == currentMonth.year
                        row.addView(createPill(yr.toString(), isCurYr).apply {
                            textSize = 13.5f
                            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                                if (c > 0) marginStart = dp(6)
                            }
                            setOnClickListener {
                                currentMonth = YearMonth.of(yr, currentMonth.monthValue)
                                val clampedDay = tempSelected.dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())
                                tempSelected = LocalDate.of(yr, currentMonth.monthValue, clampedDay)
                                yearPickerMode = false
                                renderCalendar()
                            }
                        })
                    }
                    gridContainer.addView(row)
                }
            } else {
                val firstOfMonth = currentMonth.atDay(1)
                val leadingEmpty = (firstOfMonth.dayOfWeek.value - firstDow.value + 7) % 7
                val daysInMonth = currentMonth.lengthOfMonth()
                var dayCounter = 1

                for (week in 0 until 6) {
                    if (dayCounter > daysInMonth) break
                    val weekRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(2), 0, dp(2))
                    }
                    for (col in 0 until 7) {
                        val cellIndex = week * 7 + col
                        if (cellIndex < leadingEmpty || dayCounter > daysInMonth) {
                            weekRow.addView(View(this).apply {
                                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
                            })
                        } else {
                            val d = dayCounter++
                            val cellDate = currentMonth.atDay(d)
                            val isSelected = cellDate == tempSelected
                            val isToday = cellDate == today

                            val cellView = TextView(this).apply {
                                text = d.toString()
                                gravity = Gravity.CENTER
                                textSize = 13.5f
                                setTypeface(null, if (isSelected || isToday) Typeface.BOLD else Typeface.NORMAL)
                                setTextColor(
                                    when {
                                        isSelected -> Color.parseColor("#F8FAFC")
                                        isToday -> Color.parseColor("#38BDF8")
                                        else -> Color.parseColor("#E2E8F0")
                                    }
                                )
                                when {
                                    isSelected -> setBackgroundResource(R.drawable.glass_pill_active)
                                    isToday -> background = GradientDrawable().apply {
                                        shape = GradientDrawable.RECTANGLE
                                        cornerRadius = dp(12).toFloat()
                                        setColor(Color.parseColor("#1438BDF8"))
                                        setStroke(dp(1), Color.parseColor("#6638BDF8"))
                                    }
                                    else -> setBackgroundColor(Color.TRANSPARENT)
                                }
                                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                                    marginStart = dp(2)
                                    marginEnd = dp(2)
                                }
                                setOnClickListener {
                                    tempSelected = cellDate
                                    renderCalendar()
                                }
                            }
                            weekRow.addView(cellView)
                        }
                    }
                    gridContainer.addView(weekRow)
                }
            }
        }

        btnPrev.setOnClickListener {
            currentMonth = if (yearPickerMode) currentMonth.minusYears(12) else currentMonth.minusMonths(1)
            renderCalendar()
        }
        btnNext.setOnClickListener {
            currentMonth = if (yearPickerMode) currentMonth.plusYears(12) else currentMonth.plusMonths(1)
            renderCalendar()
        }
        tvMonthYear.setOnClickListener {
            yearPickerMode = !yearPickerMode
            renderCalendar()
        }

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        val btnToday = createPill("Today", false).apply {
            textSize = 12.5f
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 0.9f)
            setOnClickListener {
                tempSelected = LocalDate.now()
                currentMonth = YearMonth.from(tempSelected)
                yearPickerMode = false
                renderCalendar()
            }
        }
        val btnCancel = createPill("Cancel", false).apply {
            textSize = 12.5f
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(8) }
            setOnClickListener { dialog.dismiss() }
        }
        val btnConfirm = createPill("Confirm", true).apply {
            textSize = 12.5f
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1.1f).apply { marginStart = dp(8) }
            setOnClickListener {
                onDateConfirmed(tempSelected)
                dialog.dismiss()
            }
        }
        actionRow.addView(btnToday)
        actionRow.addView(btnCancel)
        actionRow.addView(btnConfirm)
        rootCard.addView(actionRow)

        renderCalendar()
        dialog.setContentView(rootCard)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.68f)
            val widthPx = (resources.displayMetrics.widthPixels * 0.92f).toInt()
            setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun openCycleSelector() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val cardBg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#F20B1324"), Color.parseColor("#F2111D35"), Color.parseColor("#F2080E1C"))
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.parseColor("#557DD3FC"))
        }

        val rootCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        headerRow.addView(sectionLabel("SELECT BILLING CYCLE").apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(TextView(this).apply {
            text = selectedCycleDay?.let { "${it}${daySuffix(it)} monthly" } ?: "Not verified"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
        })
        rootCard.addView(headerRow)

        rootCard.addView(TextView(this).apply {
            text = "Choose billing cycle day (1st–28th for all-month consistency):"
            textSize = 11.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, dp(10))
        })

        for (rowIdx in 0 until 7) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(3), 0, dp(3))
            }
            for (colIdx in 0 until 4) {
                val dayNum = (rowIdx * 4) + colIdx + 1
                val isSelected = (selectedCycleDay != null && dayNum == selectedCycleDay)
                val cell = createPill("${dayNum}${daySuffix(dayNum)}", isSelected).apply {
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                        if (colIdx > 0) marginStart = dp(6)
                    }
                    setOnClickListener {
                        selectedCycleDay = dayNum
                        cycleState = CycleDetectionState.MANUAL_SELECTED
                        updateCycleButtonLabel()
                        recalculateLedger()
                        dialog.dismiss()
                    }
                }
                row.addView(cell)
            }
            rootCard.addView(row)
        }

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        val btnAutoReset = createPill("Auto-Detect", cycleState != CycleDetectionState.MANUAL_SELECTED).apply {
            textSize = 12.5f
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
            setOnClickListener {
                cycleState = CycleDetectionState.UNVERIFIED
                selectedCycleDay = null
                syncCycleWithInputs()
                recalculateLedger()
                dialog.dismiss()
            }
        }
        val btnCancel = createPill("Cancel", false).apply {
            textSize = 12.5f
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(8) }
            setOnClickListener { dialog.dismiss() }
        }
        bottomRow.addView(btnAutoReset)
        bottomRow.addView(btnCancel)
        rootCard.addView(bottomRow)

        dialog.setContentView(rootCard)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.68f)
            val widthPx = (resources.displayMetrics.widthPixels * 0.90f).toInt()
            setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun updateAdvanceToggleUI() {
        btnAdv1500.setBackgroundResource(if (selectedAdvance == 1500.0) R.drawable.glass_pill_active else R.drawable.glass_pill)
        btnAdv2500.setBackgroundResource(if (selectedAdvance == 2500.0) R.drawable.glass_pill_active else R.drawable.glass_pill)
    }

    private enum class AirFiberProductCategory {
        STANDALONE_AIRFIBER,
        AIRFIBER_IPTV_OTT
    }

    private fun resolveProductCategory(planCode: Int): AirFiberProductCategory? {
        return when (planCode) {
            499, 799, 999 -> AirFiberProductCategory.STANDALONE_AIRFIBER
            599, 699, 899, 1199, 1599 -> AirFiberProductCategory.AIRFIBER_IPTV_OTT
            else -> null // 3999 and unknown/custom plans intentionally unverified
        }
    }

    private fun resolveVerifiedCycle(planCode: Int, advanceAmount: Int, installDate: LocalDate): Int? {
        val category = resolveProductCategory(planCode) ?: return null
        val daysInMonth = installDate.lengthOfMonth()
        if (daysInMonth != 28 && daysInMonth != 30) return null

        val d = installDate.dayOfMonth
        if (d !in 1..daysInMonth) return null

        return when (category) {
            AirFiberProductCategory.STANDALONE_AIRFIBER -> when (d) {
                in 1..2 -> 6
                in 3..5 -> 9
                in 6..8 -> 12
                in 9..14 -> 18
                in 15..17 -> 24
                in 18..23 -> 27
                in 24..30 -> 3
                else -> null
            }
            AirFiberProductCategory.AIRFIBER_IPTV_OTT -> when (d) {
                in 1..4 -> 9
                in 5..8 -> 12
                in 9..11 -> 18
                in 12..17 -> 24
                in 18..23 -> 27
                in 24..27 -> 3
                in 28..30 -> 6
                else -> null
            }
        }
    }

    private fun updateCycleButtonLabel() {
        val day = selectedCycleDay
        tvCycleBtn.text = when (cycleState) {
            CycleDetectionState.AUTO_VERIFIED ->
                if (day != null) "Cycle: ${day}${daySuffix(day)} • Auto" else "Cycle not verified"
            CycleDetectionState.MANUAL_SELECTED ->
                if (day != null) "Cycle: ${day}${daySuffix(day)} • Manual" else "Cycle not verified"
            CycleDetectionState.UNVERIFIED ->
                "Cycle not verified"
        }
    }

    private fun syncCycleWithInputs() {
        // Preserve explicit manual override across plan, advance, and date changes
        if (cycleState == CycleDetectionState.MANUAL_SELECTED && selectedCycleDay != null) {
            updateCycleButtonLabel()
            return
        }

        val planCode = etPlanBase.text.toString().toDoubleOrNull()?.toInt() ?: selectedPlan.code
        val advanceAmount = selectedAdvance.toInt()
        val detected = resolveVerifiedCycle(planCode, advanceAmount, selectedDate)

        if (detected != null) {
            selectedCycleDay = detected
            cycleState = CycleDetectionState.AUTO_VERIFIED
        } else {
            selectedCycleDay = null
            cycleState = CycleDetectionState.UNVERIFIED
        }
        updateCycleButtonLabel()
    }

    private fun recalculateLedger() {
        val cycleDay = selectedCycleDay
        if (cycleDay == null || cycleState == CycleDetectionState.UNVERIFIED) {
            ledgerContainer.removeAllViews()
            tvSummaryOutput.text = "⚠️ Cycle not verified for this Plan, Advance & Date.\nTap the Cycle button above to select a billing cycle manually."
            lastWhatsAppReport = ""
            return
        }
        val basePlan = etPlanBase.text.toString().toDoubleOrNull() ?: 499.0
        val matchedPlan = plans.find { it.code == basePlan.toInt() }
        val monthlyWithGst = round2(basePlan * 1.18)
        tvPlanMeta.text = if (matchedPlan != null) {
            "${matchedPlan.offering}\nMRP w/ 18% GST: ₹${formatMoney(monthlyWithGst)}  •  Advance Paid: ₹${formatMoney(selectedAdvance)}"
        } else {
            "Custom Plan ₹${basePlan.toInt()}  •  MRP w/ 18% GST: ₹${formatMoney(monthlyWithGst)}  •  Advance Paid: ₹${formatMoney(selectedAdvance)}"
        }

        var remainingAdvance = selectedAdvance
        var currentBillDate = if (selectedDate.dayOfMonth <= cycleDay) {
            selectedDate.withDayOfMonth(cycleDay)
        } else {
            selectedDate.plusMonths(1).withDayOfMonth(cycleDay)
        }

        val firstCycleDays = ChronoUnit.DAYS.between(selectedDate, currentBillDate).toInt().coerceAtLeast(1)

        data class BillRow(
            val monthTag: String,
            val billDate: LocalDate,
            val dueDate: LocalDate,
            val billAmount: Double,
            val advanceAfter: Double,
            val payable: Double
        )

        val rows = mutableListOf<BillRow>()
        var isFirst = true

        while (true) {
            val rawBill = if (isFirst) round2((monthlyWithGst / 30.0) * firstCycleDays) else monthlyWithGst
            isFirst = false
            val dueDate = currentBillDate.plusDays(10)
            val payable: Double
            if (remainingAdvance >= rawBill) {
                remainingAdvance = round2(remainingAdvance - rawBill)
                payable = 0.0
            } else {
                payable = round2(rawBill - remainingAdvance)
                remainingAdvance = 0.0
            }

            rows.add(BillRow(currentBillDate.format(shortMonthFmt), currentBillDate, dueDate, rawBill, remainingAdvance, payable))
            if (payable > 0.0 || rows.size > 18) break
            currentBillDate = currentBillDate.plusMonths(1)
        }

        val finalRow = rows.last()
        val afterDiscount = round2((finalRow.payable - 100.0).coerceAtLeast(0.0))

        val monthWiseBlock = rows.joinToString("\n") { r ->
            if (r.payable == 0.0) "${r.monthTag} Bill - Nil"
            else "${r.monthTag} Payable Amount - ₹${formatMoney(r.payable)} (With GST)"
        }

        val exactOutput = "✅ Bill Details\n\n" +
            "📦 Plan : ${basePlan.toInt()} (₹${formatMoney(selectedAdvance)} Advance)\n" +
            "📅 Install Date : ${selectedDate.format(fullDateFmt)}\n" +
            "🔄 Billing Cycle : ${cycleDay}${daySuffix(cycleDay)} of every month\n\n" +
            "📊 Month Wise Bill - Payable Amount\n\n" +
            "$monthWiseBlock\n\n" +
            "🗓️ Payable Bill Date : ${finalRow.billDate.format(fullDateFmt)}\n" +
            "⏰ Due Date : ${finalRow.dueDate.format(fullDateFmt)}\n\n" +
            "🎁 Get ₹100 Discount for the next 10 month\n" +
            "Pay your bill through Airtel Thanks App\n\n" +
            "💵 Payable Amount After Discount : ₹${formatMoney(afterDiscount)} (With GST)\n\n" +
            "🔗 https://www.airtel.in/thanks"

        lastWhatsAppReport = exactOutput
        tvSummaryOutput.text = exactOutput

        ledgerContainer.removeAllViews()
        rows.forEachIndexed { idx, r ->
            val advLine = if (r.advanceAfter > 0.0) "Advance Remaining : ₹-${formatMoney(r.advanceAfter)}" else "Advance Remaining : ₹0.00"
            val itemCard = TextView(this).apply {
                setBackgroundResource(if (r.payable > 0) R.drawable.glass_pill_active else R.drawable.glass_pill)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                textSize = 13f
                setTextColor(Color.parseColor("#E2E8F0"))
                text = "📅 ${r.monthTag}   |   Bill: ₹${formatMoney(r.billAmount)}   |   Payable: ₹${formatMoney(r.payable)}\n" +
                       "Bill Date: ${r.billDate.format(fullDateFmt)}   •   Due: ${r.dueDate.format(fullDateFmt)}\n" +
                       (if (idx == 0) "Advance Paid: ₹${formatMoney(selectedAdvance)}   •   $advLine" else advLine)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    if (idx > 0) topMargin = dp(8)
                }
            }
            ledgerContainer.addView(itemCard)
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
    private fun daySuffix(d: Int): String = when { d in 11..13 -> "th"; d % 10 == 1 -> "st"; d % 10 == 2 -> "nd"; d % 10 == 3 -> "rd"; else -> "th" }
}
