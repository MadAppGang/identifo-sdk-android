package com.prytula.identifolibui

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import java.lang.StringBuilder


/*
 * Created by Eugene Prytula on 2/26/21.
 * Copyright (c) 2021 MadAppGang. All rights reserved.
 */

interface OnTextChangeListener {
    fun textEntered(code: String)
}

private const val MIX_SYMBOL_SIZE = 4

class OtpCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val typedArray: TypedArray by lazy { context.obtainStyledAttributes(attrs, R.styleable.OtpCodeView) }

    private var otpLineColor = 0
    private var otpLineWidth = 0F
    private var otpTextColor = 0
    private var otpTextSize = 0
        set(value) {
            field = value.coerceAtLeast(MIX_SYMBOL_SIZE)
        }

    private val systemMetrics by lazy { Resources.getSystem().displayMetrics }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.OtpCodeView, 0, 0).apply {
            try {
                otpLineColor = typedArray.getColor(R.styleable.OtpCodeView_otpLineColor, 0)
                otpLineWidth = typedArray.getDimension(R.styleable.OtpCodeView_otpLineWidth, 0F)
                otpTextSize = typedArray.getInt(R.styleable.OtpCodeView_otpTextNumber, MIX_SYMBOL_SIZE)
                otpTextColor = typedArray.getColor(R.styleable.OtpCodeView_otpTextColor, 0)
            } finally {
                recycle()
            }
        }
    }

    private var textChangeListener : OnTextChangeListener? = null
    private var viewHeight: Int = 0
    private var viewWidth: Int = 0

    private val codeBuilder: StringBuilder by lazy { StringBuilder() }

    private var blankLine = 0F
    private var solidLine = 0F

    private var solidPoints : Array<PointF?>? = null

    private val linePaint = Paint().apply {
        isAntiAlias = true
        color = otpLineColor
        strokeWidth = otpLineWidth
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
        textAlign = Paint.Align.CENTER
        isFocusableInTouchMode = true
        color = otpTextColor
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        val baseInputConnection: BaseInputConnection = object : BaseInputConnection(this, false) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                val downKeyAction = sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                val upKeyAction = sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                return downKeyAction && upKeyAction
            }
        }

        outAttrs?.run {
            actionLabel = null
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }

        return baseInputConnection
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyDown(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_DEL && codeBuilder.isNotEmpty()) {
            codeBuilder.deleteCharAt(codeBuilder.length - 1)
            invalidate()
        } else if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 && codeBuilder.length < otpTextSize) {
            codeBuilder.append(event?.displayLabel)
            invalidate()
        }

        if (codeBuilder.length >= otpTextSize || keyCode == KeyEvent.KEYCODE_ENTER) {
            textChangeListener?.textEntered(codeBuilder.toString())
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMode)
        viewHeight = MeasureSpec.getSize(heightMode)

        if (widthMode == MeasureSpec.AT_MOST) {
            viewWidth = systemMetrics.widthPixels * 2 / 3
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            viewHeight = systemMetrics.heightPixels / 4
        }

        blankLine = viewWidth / (4F * otpTextSize - 1)
        solidLine = viewWidth / (4F * otpTextSize - 1) * 3

        textPaint.textSize = solidLine

        calculateStartAndEndPoint(otpTextSize)
        setMeasuredDimension(viewWidth, viewHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val inputLength = codeBuilder.length
        val fontMetricsInt = textPaint.fontMetricsInt
        val linePosY = viewHeight / 2F
        val baseLine : Float = (viewHeight / 2 + (fontMetricsInt.bottom - fontMetricsInt.top) / 2 - fontMetricsInt.bottom).toFloat()

        for (i in 0 until otpTextSize) {
            if (inputLength > i) {
                canvas?.drawText(codeBuilder.toString(), i, i + 1, solidPoints!![i]!!.y - solidLine / 2, baseLine, textPaint)
            } else {
                canvas?.drawLine(solidPoints!![i]!!.x + paddingLeft, linePosY, solidPoints!![i]!!.y - paddingRight, linePosY, linePaint)
            }
        }
    }

    private fun calculateStartAndEndPoint(textSize : Int) {
        solidPoints = arrayOfNulls(textSize)
        for (i in 1..otpTextSize) {
            val point = PointF((i - 1) * blankLine + (i - 1) * solidLine, (i - 1) * blankLine + i * solidLine)
            solidPoints!![i - 1] = point
        }
    }

    fun setText(code : String) {
        if (code.length > otpTextSize) return
        if (code.length == otpTextSize) textChangeListener?.textEntered(code)
        if (codeBuilder.isNotBlank()) codeBuilder.clear()
        codeBuilder.append(code)
        invalidate()
    }

    fun setTextChangeListener(changeListener: OnTextChangeListener) {
        this.textChangeListener = changeListener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        textChangeListener = null
    }
}