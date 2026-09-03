package com.ether4o4.pixavatar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class AvatarView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var start = System.currentTimeMillis()
    private var nextBlink = 2500L
    private var blinkUntil = 0L
    private var speakingUntil = 0L
    private var expression = Expression.IDLE
    var onSpeak: ((String) -> Unit)? = null

    private enum class Expression { IDLE, HAPPY, THINKING, SURPRISED, ANNOYED }

    init {
        isFocusable = true
        paint.strokeCap = Paint.Cap.ROUND
        setBackgroundColor(0xFF090A08.toInt())
    }

    fun setExpression(name: String) {
        expression = when (name.lowercase()) {
            "happy", "smile" -> Expression.HAPPY
            "thinking", "curious" -> Expression.THINKING
            "surprised" -> Expression.SURPRISED
            "annoyed", "angry" -> Expression.ANNOYED
            else -> Expression.IDLE
        }
        invalidate()
    }

    fun startSpeaking(durationMs: Long) {
        speakingUntil = System.currentTimeMillis() + durationMs.coerceIn(500L, 120_000L)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        val t = (now - start) / 1000f
        if (now >= nextBlink && now > blinkUntil) {
            blinkUntil = now + 130L
            nextBlink = now + 2200L + Random.nextLong(1800L)
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.44f
        val scale = minOf(w, h) / 520f
        val talking = now < speakingUntil
        val blink = now < blinkUntil
        val bob = sin(t * 1.6f) * 3f * scale

        paint.style = Paint.Style.FILL
        paint.color = 0xFF536737.toInt()
        canvas.drawRoundRect(RectF(cx - 175 * scale, cy - 210 * scale + bob, cx + 175 * scale, cy + 60 * scale + bob), 75 * scale, 75 * scale, paint)
        paint.color = 0xFFB68A35.toInt()
        canvas.drawRoundRect(RectF(cx - 175 * scale, cy - 28 * scale + bob, cx + 175 * scale, cy + 5 * scale + bob), 16 * scale, 16 * scale, paint)

        paint.color = 0xFF111311.toInt()
        canvas.drawOval(RectF(cx - 145 * scale, cy - 145 * scale + bob, cx + 145 * scale, cy + 135 * scale + bob), paint)

        val eyeY = cy - 45 * scale + bob
        drawEye(canvas, cx - 62 * scale, eyeY, 48 * scale, blink, expression, t)
        drawEye(canvas, cx + 62 * scale, eyeY, 48 * scale, blink, expression, t + .2f)

        paint.color = 0xFFE7E0CF.toInt()
        if (talking) {
            val open = (0.55f + 0.45f * sin(t * 18f)) * scale
            canvas.drawOval(RectF(cx - 32 * scale, cy + 52 * scale + bob, cx + 32 * scale, cy + (52 + 22 * open) * scale + bob), paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 7f * scale
            val smile = if (expression == Expression.HAPPY) 18f else 4f
            canvas.drawArc(RectF(cx - 38 * scale, cy + (35 - smile) * scale + bob, cx + 38 * scale, cy + (78 + smile) * scale + bob), 20f, 140f, false, paint)
            paint.style = Paint.Style.FILL
        }

        paint.color = 0xFF252B24.toInt()
        canvas.drawRoundRect(RectF(cx - 150 * scale, cy + 120 * scale, cx + 150 * scale, cy + 390 * scale), 55 * scale, 55 * scale, paint)
        paint.color = 0xFF8C9E4E.toInt()
        canvas.drawCircle(cx, cy + 245 * scale, 34 * scale, paint)
        paint.color = 0xFFD0A13E.toInt()
        paint.textSize = 32 * scale
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("M", cx, cy + 256 * scale, paint)

        paint.color = 0xFFB7C29A.toInt()
        paint.textSize = 17 * scale
        canvas.drawText("MARTIAN", cx, h - 48 * scale, paint)
        paint.color = 0xFF697C42.toInt()
        paint.textSize = 12 * scale
        canvas.drawText(stateLabel(talking), cx, h - 25 * scale, paint)

        postInvalidateOnAnimation()
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, r: Float, blink: Boolean, expression: Expression, t: Float) {
        paint.color = 0xFFF1EEE3.toInt()
        if (blink) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 7f
            canvas.drawLine(x - r * .55f, y, x + r * .55f, y, paint)
            paint.style = Paint.Style.FILL
            return
        }
        val vertical = when (expression) {
            Expression.SURPRISED -> 1.15f
            Expression.THINKING -> .72f
            Expression.ANNOYED -> .62f
            else -> .9f
        }
        canvas.drawOval(RectF(x - r, y - r * vertical, x + r, y + r * vertical), paint)
        paint.color = 0xFF161914.toInt()
        val look = sin(t * .7f) * r * .25f
        canvas.drawCircle(x + look, y, r * .48f, paint)
    }

    private fun stateLabel(talking: Boolean): String = when {
        talking -> "SPEAKING"
        expression == Expression.THINKING -> "THINKING"
        expression == Expression.HAPPY -> "HAPPY"
        expression == Expression.SURPRISED -> "SURPRISED"
        expression == Expression.ANNOYED -> "ANNOYED"
        else -> "IDLE"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            expression = when (expression) {
                Expression.IDLE -> Expression.HAPPY
                Expression.HAPPY -> Expression.THINKING
                Expression.THINKING -> Expression.SURPRISED
                Expression.SURPRISED -> Expression.ANNOYED
                Expression.ANNOYED -> Expression.IDLE
            }
            startSpeaking(2200L)
            onSpeak?.invoke("What's up? I'm PixAvatar.")
            performClick()
            return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
