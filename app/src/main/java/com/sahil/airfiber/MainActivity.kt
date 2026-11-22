package com.sahil.airfiber

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedAdvance: Double = 1500.0
    private var selectedCycleDay: Int = 6
    private var lastFormattedReport: String = ""

    private lateinit var etGstBase: EditText
    private lateinit var tvGstResult: TextView
    private lateinit var etPlanBase: EditText
    private lateinit var tvDateBtn: TextView
    private lateinit var tvCycleBtn: TextView
    private lateinit var btnAdv1500: TextView
    private lateinit var btnAdv2500: TextView
    private lateinit var tvSummaryOutput: TextView
    private lateinit var ledgerContainer: LinearLayout

    private val fullDateFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    private val shortMonthFmt = DateTimeFormatter.ofPattern("MMM''yy", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableMaxRefreshRate()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val rootScroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_ALWAYS
        }

        val mainColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = dp(16)
            setPadding(padH, dp(46), padH, dp(36))
        }

        // HEADER ROW (App Title on Left + Personal Credit on Right)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
        }

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f)
        }
        titleCol.addView(TextView(this).apply {
            text = "AirFiber Billing & GST"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F8FAFC"))
        })
        titleCol.addView(TextView(this).apply {
            text = "Instant GST & Advance Recharge Ledger"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(2), 0, 0)
        })

        val creditCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.glass_pill)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.05f).apply {
                marginStart = dp(8)
            }
        }
        creditCol.addView(TextView(this).apply {
            text = "Made with ❤️ by Sahil Ahmed"
            textSize = 10.5f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E0F2FE"))
        })
        creditCol.addView(TextView(this).apply {
            text = "Field Service Engineer at\nHindustan Enterprises"
            textSize = 9.5f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(2), 0, 0)
        })

        headerRow.addView(titleCol)
        headerRow.addView(creditCol)
        mainColumn.addView(headerRow)

        // CARD 1: Simple 18% GST Calculator
        val gstCard = createGlassCard().apply {
            addView(sectionLabel("1. QUICK 18% GST CALCULATOR"))

            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }

            etGstBase = createGlassInput("Base Plan (e.g. 499)").apply {
                setText("499")
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
            }

            tvGstResult = TextView(this@MainActivity).apply {
                text = "₹588.82"
                textSize = 22f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#38BDF8"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(etGstBase)
            row.addView(tvGstResult)
            addView(row)

            val chipRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            listOf("499", "599", "699", "799", "899", "999").forEachIndexed { idx, amt ->
                val chip = createPill(amt, false).apply {
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    textSize = 12f
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (idx > 0) lp.marginStart = dp(6)
                    layoutParams = lp
                    setOnClickListener { etGstBase.setText(amt) }
                }
                chipRow.addView(chip)
            }
            addView(chipRow)
        }
        mainColumn.addView(gstCard)

        // CARD 2: Advance Recharge Structure Calculator
        val advCard = createGlassCard().apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
            layoutParams = lp

            addView(sectionLabel("2. UPCOMING RECHARGE STRUCTURE"))

            val topRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(8))
            }

            etPlanBase = createGlassInput("Plan (499)").apply {
                setText("499")
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
            }

            btnAdv1500 = createPill("₹1500 Adv", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                    marginStart = dp(8)
                }
                setOnClickListener {
                    selectedAdvance = 1500.0
                    updateAdvanceToggleUI()
                    recalculateLedger()
                }
            }

            btnAdv2500 = createPill("₹2500 Adv", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                    marginStart = dp(8)
                }
                setOnClickListener {
                    selectedAdvance = 2500.0
                    updateAdvanceToggleUI()
                    recalculateLedger()
                }
            }

            topRow.addView(etPlanBase)
            topRow.addView(btnAdv1500)
            topRow.addView(btnAdv2500)
            addView(topRow)

            val dateRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(12))
            }

            tvDateBtn = createPill("Active: ${selectedDate.format(fullDateFmt)}", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1.3f)
                setOnClickListener { openDatePicker() }
            }

            tvCycleBtn = createPill("Cycle: ${selectedCycleDay}th", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                    marginStart = dp(8)
                }
                setOnClickListener { openCycleSelector() }
            }

            dateRow.addView(tvDateBtn)
            dateRow.addView(tvCycleBtn)
            addView(dateRow)

            tvSummaryOutput = TextView(this@MainActivity).apply {
                setBackgroundResource(R.drawable.glass_pill_active)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                textSize = 13.5f
                setLineSpacing(dp(3).toFloat(), 1f)
                setTextColor(Color.parseColor("#F8FAFC"))
            }
            addView(tvSummaryOutput)

            val actionRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, dp(10))
            }

            val btnCopy = createPill("Copy Bill Text", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                setOnClickListener {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Bill Details", lastFormattedReport))
                    Toast.makeText(this@MainActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }

            val btnShare = createPill("Share WhatsApp", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    marginStart = dp(8)
                }
                setOnClickListener { shareText(lastFormattedReport) }
            }

            actionRow.addView(btnCopy)
            actionRow.addView(btnShare)
            addView(actionRow)

            addView(sectionLabel("FULL BILL BREAKDOWN").apply {
                setPadding(0, dp(6), 0, dp(8))
            })

            ledgerContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(ledgerContainer)
        }
        mainColumn.addView(advCard)

        // CARD 3: Developer Contact, GitHub & App Update Key
        val footerCard = createGlassCard().apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
            layoutParams = lp

            addView(sectionLabel("DEVELOPER & APP UPDATES"))

            addView(TextView(this@MainActivity).apply {
                text = "Feedback appreciated — reach out or check source updates:"
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, dp(6), 0, dp(10))
            })

            // Email Pill with Icon
            val emailPill = createIconLinkRow(
                iconRes = R.drawable.ic_email,
                label = "sahilahmed698@gmail.com"
            ) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:sahilahmed698@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "AirFiber Calc Feedback")
                }
                runCatching { startActivity(intent) }
            }
            addView(emailPill)

            // GitHub Pill with Icon
            val githubPill = createIconLinkRow(
                iconRes = R.drawable.ic_github,
                label = "github.com/sahilhv"
            ) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/flashholicguy"))
                runCatching { startActivity(intent) }
            }.apply {
                val p = layoutParams as LinearLayout.LayoutParams
                p.topMargin = dp(8)
                layoutParams = p
            }
            addView(githubPill)

            // App Version & OTA Release Check Button
            val updateBtn = createPill("Check Updates & Releases (v1.1)", true).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(44)
                ).apply { topMargin = dp(10) }
                setOnClickListener {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/flashholicguy/AirFiberCalc/releases")
                    )
                    runCatching { startActivity(intent) }
                }
            }
            addView(updateBtn)
        }
        mainColumn.addView(footerCard)

        rootScroll.addView(mainColumn)
        setContentView(rootScroll)

        etGstBase.addTextChangedListener(simpleWatcher { updateGst() })
        etPlanBase.addTextChangedListener(simpleWatcher { recalculateLedger() })

        updateGst()
        recalculateLedger()
    }

    private fun createIconLinkRow(iconRes: Int, label: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.glass_pill)
            setPadding(dp(14), dp(11), dp(14), dp(11))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val iv = ImageView(this@MainActivity).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            }

            val tv = TextView(this@MainActivity).apply {
                text = label
                textSize = 13.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#F8FAFC"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(10) }
            }

            addView(iv)
            addView(tv)
            setOnClickListener { onClick() }
        }
    }

    private fun enableMaxRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val modes = display?.supportedModes ?: return
            val maxMode = modes.maxByOrNull { it.refreshRate } ?: return
            val params: WindowManager.LayoutParams = window.attributes
            params.preferredDisplayModeId = maxMode.modeId
            window.attributes = params
        }
    }

    private fun updateGst() {
        val base = etGstBase.text.toString().toDoubleOrNull() ?: 0.0
        val total = round2(base * 1.18)
        tvGstResult.text = "₹${formatMoney(total)}"
    }

    private fun openDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                tvDateBtn.text = "Active: ${selectedDate.format(fullDateFmt)}"
                recalculateLedger()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    private fun openCycleSelector() {
        val days = (1..28).map { "${it}${daySuffix(it)} of every month" }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Select Billing Cycle Date")
            .setItems(days) { _, which ->
                selectedCycleDay = which + 1
                tvCycleBtn.text = "Cycle: ${selectedCycleDay}${daySuffix(selectedCycleDay)}"
                recalculateLedger()
            }
            .show()
    }

    private fun recalculateLedger() {
        val basePlan = etPlanBase.text.toString().toDoubleOrNull() ?: 499.0
        val monthlyWithGst = round2(basePlan * 1.18)
        var remainingAdvance = selectedAdvance

        var currentBillDate = if (selectedDate.dayOfMonth <= selectedCycleDay) {
            selectedDate.withDayOfMonth(selectedCycleDay)
        } else {
            selectedDate.plusMonths(1).withDayOfMonth(selectedCycleDay)
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
            val rawBill = if (isFirst) {
                round2((monthlyWithGst / 30.0) * firstCycleDays)
            } else {
                monthlyWithGst
            }
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

            rows.add(
                BillRow(
                    monthTag = currentBillDate.format(shortMonthFmt),
                    billDate = currentBillDate,
                    dueDate = dueDate,
                    billAmount = rawBill,
                    advanceAfter = remainingAdvance,
                    payable = payable
                )
            )

            if (payable > 0.0 || rows.size > 18) break
            currentBillDate = currentBillDate.plusMonths(1)
        }

        val finalRow = rows.last()
        val afterDiscount = round2((finalRow.payable - 100.0).coerceAtLeast(0.0))

        val monthWiseLines = rows.joinToString("\n") { r ->
            if (r.payable == 0.0) {
                "${r.monthTag} Bill - Nil"
            } else {
                "${r.monthTag} Payable Amount - ₹${formatMoney(r.payable)} (With GST)"
            }
        }

        val summaryText = """
✅ Bill Details

📦 Plan : ${basePlan.toInt()} (₹${formatMoney(selectedAdvance)} Advance)
📅 Install Date : ${selectedDate.format(fullDateFmt)}
🔄 Billing Cycle : ${selectedCycleDay}${daySuffix(selectedCycleDay)} of every month

📊 Month Wise Bill - Payable Amount
$monthWiseLines

🗓 Payable Bill Date : ${finalRow.billDate.format(fullDateFmt)}
⏰ Due Date : ${finalRow.dueDate.format(fullDateFmt)}

🎁 Get ₹100 Discount for the next 10 months
Pay your bill through Airtel Thanks App

💵 Payable After Discount : ₹${formatMoney(afterDiscount)} (With GST)
        """.trimIndent()

        tvSummaryOutput.text = summaryText

        val breakdownText = StringBuilder("\n\n📊 FULL BILL BREAKDOWN\n\n")
        ledgerContainer.removeAllViews()

        rows.forEachIndexed { idx, r ->
            val advLine = if (r.advanceAfter > 0.0) {
                "Advance Remaining : ₹-${formatMoney(r.advanceAfter)}"
            } else {
                "Advance Remaining : ₹0.00"
            }

            breakdownText.append(
                "📅 ${r.monthTag}\n" +
                "Bill Date : ${r.billDate.format(fullDateFmt)}\n" +
                "Due Date : ${r.dueDate.format(fullDateFmt)}\n" +
                "Bill Amount : ₹${formatMoney(r.billAmount)}\n" +
                (if (idx == 0) "Advance Paid : ₹${formatMoney(selectedAdvance)}\n" else "") +
                "$advLine\n" +
                "Payable Bill Amount : ₹${formatMoney(r.payable)}\n\n"
            )

            val itemCard = TextView(this).apply {
                setBackgroundResource(if (r.payable > 0) R.drawable.glass_pill_active else R.drawable.glass_pill)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                textSize = 13f
                setTextColor(Color.parseColor("#E2E8F0"))
                text = "📅 ${r.monthTag}   |   Bill: ₹${formatMoney(r.billAmount)}   |   Payable: ₹${formatMoney(r.payable)}\n" +
                       "Bill Date: ${r.billDate.format(fullDateFmt)}   •   Due: ${r.dueDate.format(fullDateFmt)}\n" +
                       advLine
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                if (idx > 0) lp.topMargin = dp(8)
                layoutParams = lp
            }
            ledgerContainer.addView(itemCard)
        }

        lastFormattedReport = summaryText + breakdownText.toString().trimEnd()
    }

    private fun shareText(content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, "Send Bill Breakdown"))
    }

    private fun updateAdvanceToggleUI() {
        btnAdv1500.setBackgroundResource(if (selectedAdvance == 1500.0) R.drawable.glass_pill_active else R.drawable.glass_pill)
        btnAdv2500.setBackgroundResource(if (selectedAdvance == 2500.0) R.drawable.glass_pill_active else R.drawable.glass_pill)
    }

    private fun createGlassCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.glass_card)
        setPadding(dp(16), dp(16), dp(16), dp(16))
    }

    private fun createGlassInput(hintText: String): EditText = EditText(this).apply {
        hint = hintText
        setHintTextColor(Color.parseColor("#64748B"))
        setTextColor(Color.parseColor("#F8FAFC"))
        textSize = 16f
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        setBackgroundResource(R.drawable.glass_pill)
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    private fun createPill(label: String, active: Boolean): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Color.parseColor("#F8FAFC"))
        setBackgroundResource(if (active) R.drawable.glass_pill_active else R.drawable.glass_pill)
    }

    private fun sectionLabel(title: String): TextView = TextView(this).apply {
        text = title
        textSize = 11f
        letterSpacing = 0.08f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Color.parseColor("#7DD3FC"))
    }

    private fun simpleWatcher(onChanged: () -> Unit): TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = onChanged()
    }

    private fun round2(v: Double): Double = (v * 100.0).roundToLong() / 100.0
    private fun formatMoney(v: Double): String = String.format(Locale.ENGLISH, "%.2f", v)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun daySuffix(d: Int): String = when {
        d in 11..13 -> "th"
        d % 10 == 1 -> "st"
        d % 10 == 2 -> "nd"
        d % 10 == 3 -> "rd"
        else -> "th"
    }
}
