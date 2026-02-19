package com.example.uleammed.scoring

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Genera un reporte PDF profesional de salud laboral (3 páginas).
 *
 * Página 1 — Portada + Score global + Resumen de riesgo por áreas
 * Página 2 — Análisis detallado (pesos, umbrales, estado de cada área)
 * Página 3 — Áreas prioritarias + Recomendaciones + Pie de informe
 *
 * Uso: PdfExportUtils.exportAndOpen(context, healthScore)
 */
object PdfExportUtils {

    // ── Dimensiones A4 a 72 dpi ───────────────────────────────────────────────
    private const val PW   = 595f
    private const val PH   = 842f
    private const val M    = 44f          // margen lateral
    private const val CW   = PW - M * 2  // ancho de contenido

    // ── Paleta de colores ─────────────────────────────────────────────────────
    private val C_NAVY     = Color.parseColor("#0D1B4B")
    private val C_BLUE     = Color.parseColor("#1565C0")
    private val C_BLUE_LT  = Color.parseColor("#E3F2FD")
    private val C_BODY     = Color.parseColor("#1A1A2E")
    private val C_MUTED    = Color.parseColor("#6B7280")
    private val C_DIVIDER  = Color.parseColor("#E5E7EB")
    private val C_CARD     = Color.parseColor("#F8FAFC")
    private val C_WHITE    = Color.WHITE

    // Colores de riesgo (coinciden con RiskLevel.color)
    private val C_BAJO     = Color.parseColor("#4CAF50")
    private val C_MODERADO = Color.parseColor("#FFC107")
    private val C_ALTO     = Color.parseColor("#FF9800")
    private val C_MUY_ALTO = Color.parseColor("#F44336")

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private val TF_BOLD    = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val TF_NORMAL  = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val TF_ITALIC  = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

    // ── Pesos de áreas (del ScoreCalculator) ─────────────────────────────────
    private val AREA_WEIGHTS = mapOf(
        "estres"              to 0.18,
        "sintomas_musculares" to 0.16,
        "carga_trabajo"       to 0.14,
        "sueno"               to 0.11,
        "balance"             to 0.11,
        "salud_general"       to 0.10,
        "ergonomia"           to 0.09,
        "sintomas_visuales"   to 0.07,
        "actividad_fisica"    to 0.04
    )

    // ── Datos de cada área ────────────────────────────────────────────────────
    private data class AreaInfo(
        val key: String,
        val label: String,
        val score: Int,
        val risk: RiskLevel,
        val higherIsBetter: Boolean,
        val weight: Double
    ) {
        /** Score normalizado a escala "mayor = mayor riesgo" */
        val riskScore: Int get() =
            if (higherIsBetter) (100 - score).coerceIn(0, 100)
            else score.coerceIn(0, 100)
    }

    // ── Punto de entrada ─────────────────────────────────────────────────────
    fun exportAndOpen(context: Context, healthScore: HealthScore) {
        val file = generatePdf(context, healthScore)
        openPdf(context, file)
    }

    // ── Generación del documento ─────────────────────────────────────────────
    private fun generatePdf(context: Context, hs: HealthScore): File {
        val doc       = PdfDocument()
        val timestamp = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale("es")).format(Date())
        val areas     = buildAreas(hs)

        // ── Página 1: Portada + Score global + Resumen visual ────────────────
        var info = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), 1).create()
        var page = doc.startPage(info)
        drawPage1(page.canvas, hs, areas, timestamp)
        doc.finishPage(page)

        // ── Página 2: Análisis detallado por área ────────────────────────────
        info = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), 2).create()
        page = doc.startPage(info)
        drawPage2(page.canvas, hs, areas)
        doc.finishPage(page)

        // ── Página 3: Áreas críticas + Recomendaciones ───────────────────────
        if (hs.topConcerns.isNotEmpty() || hs.recommendations.isNotEmpty()) {
            info = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), 3).create()
            page = doc.startPage(info)
            drawPage3(page.canvas, hs)
            doc.finishPage(page)
        }

        val outDir = File(context.cacheDir, "pdf_reports").apply { mkdirs() }
        val file   = File(outDir, "reporte_salud_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PÁGINA 1
    // ─────────────────────────────────────────────────────────────────────────
    private fun drawPage1(canvas: Canvas, hs: HealthScore, areas: List<AreaInfo>, timestamp: String) {

        // ── Franja de cabecera degradada (simulada con dos rect) ──────────────
        canvas.drawRect(0f, 0f, PW, 110f, fill(C_NAVY))
        canvas.drawRect(0f, 90f, PW, 115f, fill(Color.parseColor("#112266")))

        // Logo textual
        canvas.drawText("UleaMmed", M, 42f, paint(C_WHITE, 22f, TF_BOLD))
        canvas.drawText("Sistema de Evaluación de Salud Laboral", M, 60f,
            paint(Color.parseColor("#B0C4DE"), 10f))

        // Título del reporte
        canvas.drawText("REPORTE DE SALUD LABORAL", M, 88f, paint(C_WHITE, 13f, TF_BOLD))

        // Fecha (alineada a la derecha)
        val datePaint = paint(Color.parseColor("#B0C4DE"), 9f).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(timestamp, PW - M, 88f, datePaint)

        var y = 128f

        // ── Tarjeta de Score Global ───────────────────────────────────────────
        val riskColor = hs.overallRisk.color.toInt()
        val riskScore = hs.overallScore.coerceIn(0, 100)

        // Fondo de la tarjeta
        roundRect(canvas, M, y, M + CW, y + 118f, 10f, Color.parseColor("#F0F4FF"))
        // Borde izquierdo de color del riesgo
        canvas.drawRect(M, y, M + 5f, y + 118f, fill(riskColor))

        // Score grande
        canvas.drawText("$riskScore", M + 22f, y + 68f, paint(riskColor, 56f, TF_BOLD))
        canvas.drawText("/ 100", M + 22f, y + 85f, paint(C_MUTED, 11f))

        // Título y descripción a la derecha del número
        canvas.drawText("Índice Global de Riesgo Laboral", M + 100f, y + 26f,
            paint(C_NAVY, 14f, TF_BOLD))
        canvas.drawText(hs.overallRisk.displayName.uppercase(), M + 100f, y + 45f,
            paint(riskColor, 11f, TF_BOLD))

        val descText = when (hs.overallRisk) {
            RiskLevel.BAJO     -> "Tu salud laboral se encuentra en buen estado. Mantén tus hábitos actuales."
            RiskLevel.MODERADO -> "Existen áreas que necesitan atención. Revisa las recomendaciones del informe."
            RiskLevel.ALTO     -> "Se detectaron múltiples factores de riesgo. Toma acción prioritaria."
            RiskLevel.MUY_ALTO -> "Situación crítica detectada. Se recomienda apoyo profesional inmediato."
        }
        drawWrappedText(canvas, descText, M + 100f, y + 60f, CW - 110f,
            paint(C_BODY, 9f, TF_ITALIC), 13f)

        // Barra de progreso del score
        val barY = y + 98f
        roundRect(canvas, M + 100f, barY, M + CW - 8f, barY + 10f, 5f, C_DIVIDER)
        roundRect(canvas, M + 100f, barY,
            M + 100f + (CW - 108f) * (riskScore / 100f), barY + 10f, 5f, riskColor)
        canvas.drawText("0", M + 100f, barY + 20f, paint(C_MUTED, 7f))
        val maxPaint = paint(C_MUTED, 7f).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("100", M + CW - 8f, barY + 20f, maxPaint)

        y += 132f

        // ── Leyenda de colores ─────────────────────────────────────────────────
        drawSectionTitle(canvas, "NIVELES DE RIESGO", y)
        y += 22f

        val levels = listOf(
            Triple(RiskLevel.BAJO,     "Bajo (< 25)",     "Sin factores significativos"),
            Triple(RiskLevel.MODERADO, "Moderado (25-44)", "Requiere seguimiento"),
            Triple(RiskLevel.ALTO,     "Alto (45-64)",     "Intervención recomendada"),
            Triple(RiskLevel.MUY_ALTO, "Muy Alto (≥ 65)", "Atención urgente necesaria")
        )
        val colW = CW / 4f
        levels.forEachIndexed { i, (lvl, name, desc) ->
            val x = M + i * colW
            val rc = lvl.color.toInt()
            roundRect(canvas, x + 4f, y, x + colW - 4f, y + 48f, 6f, C_CARD)
            canvas.drawRect(x + 4f, y, x + 4f + 4f, y + 48f, fill(rc))
            canvas.drawText(name, x + 14f, y + 16f, paint(rc, 8f, TF_BOLD))
            drawWrappedText(canvas, desc, x + 14f, y + 28f, colW - 20f, paint(C_MUTED, 7f), 11f)
        }
        y += 60f

        // ── Mapa de calor de áreas (grid 3 x 3) ───────────────────────────────
        drawSectionTitle(canvas, "RESUMEN POR ÁREAS", y)
        y += 22f

        val completedAreas = areas.filter { it.score != -1 }
        if (completedAreas.isEmpty()) {
            canvas.drawText("No hay encuestas completadas aún.", M, y + 14f, paint(C_MUTED, 10f, TF_ITALIC))
            y += 30f
        } else {
            val cols   = 3
            val tileW  = CW / cols
            val tileH  = 66f
            completedAreas.forEachIndexed { idx, area ->
                val col = idx % cols
                val row = idx / cols
                val tx  = M + col * tileW
                val ty  = y + row * (tileH + 6f)
                val rc  = area.risk.color.toInt()

                roundRect(canvas, tx + 3f, ty, tx + tileW - 3f, ty + tileH, 8f, C_CARD)
                // Borde superior del color del riesgo
                canvas.drawRect(tx + 3f, ty, tx + tileW - 3f, ty + 4f, fill(rc))

                canvas.drawText(area.label, tx + 10f, ty + 18f, paint(C_NAVY, 8f, TF_BOLD))

                // Score + nivel
                canvas.drawText("${area.riskScore}", tx + 10f, ty + 38f, paint(rc, 18f, TF_BOLD))
                canvas.drawText("/ 100  •  ${area.risk.displayName}", tx + 10f, ty + 50f,
                    paint(C_MUTED, 7f))

                // Mini barra
                val bx = tx + 10f
                val bw = tileW - 22f
                roundRect(canvas, bx, ty + 55f, bx + bw, ty + 61f, 3f, C_DIVIDER)
                roundRect(canvas, bx, ty + 55f, bx + bw * (area.riskScore / 100f), ty + 61f,
                    3f, rc)

                // Peso del área (badge pequeño)
                val weightPct = "${(area.weight * 100).toInt()}%"
                val wp = paint(Color.parseColor("#64748B"), 7f, TF_BOLD).apply {
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(weightPct, tx + tileW - 10f, ty + 18f, wp)
            }
            val rowCount = Math.ceil(completedAreas.size / cols.toDouble()).toInt()
            y += rowCount * (tileH + 6f) + 8f
        }

        drawFooter(canvas, 1, 3)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PÁGINA 2 — Análisis detallado
    // ─────────────────────────────────────────────────────────────────────────
    private fun drawPage2(canvas: Canvas, hs: HealthScore, areas: List<AreaInfo>) {
        drawPageHeader(canvas, "ANÁLISIS DETALLADO POR ÁREA", "Interpretación clínica y umbrales de riesgo")

        var y = 80f

        // ── Tabla de pesos ─────────────────────────────────────────────────
        drawSectionTitle(canvas, "IMPORTANCIA RELATIVA DE CADA ÁREA", y)
        y += 22f

        // Cabecera de tabla
        roundRect(canvas, M, y, M + CW, y + 22f, 4f, C_NAVY)
        canvas.drawText("ÁREA DE EVALUACIÓN", M + 10f, y + 15f, paint(C_WHITE, 8f, TF_BOLD))
        canvas.drawText("PESO", M + 220f, y + 15f, paint(C_WHITE, 8f, TF_BOLD))
        canvas.drawText("PUNTUACIÓN RIESGO", M + 270f, y + 15f, paint(C_WHITE, 8f, TF_BOLD))
        canvas.drawText("NIVEL", M + 395f, y + 15f, paint(C_WHITE, 8f, TF_BOLD))
        canvas.drawText("ESTADO", M + 460f, y + 15f, paint(C_WHITE, 8f, TF_BOLD))
        y += 22f

        val tableAreas = areas.sortedByDescending { it.weight }
        tableAreas.forEachIndexed { idx, area ->
            val rowBg = if (idx % 2 == 0) C_CARD else C_WHITE
            canvas.drawRect(M, y, M + CW, y + 26f, fill(rowBg))

            val rc = if (area.score == -1) C_MUTED else area.risk.color.toInt()

            canvas.drawText(area.label, M + 10f, y + 17f, paint(C_BODY, 9f))

            // Barra de peso (visual)
            val weightBarW = 38f * area.weight / 0.18
            roundRect(canvas, M + 218f, y + 8f, M + 218f + weightBarW.toFloat(), y + 18f, 3f,
                Color.parseColor("#3B82F6"))
            canvas.drawText("${(area.weight * 100).toInt()}%", M + 262f, y + 17f,
                paint(C_BLUE, 8f, TF_BOLD))

            // Score
            if (area.score == -1) {
                canvas.drawText("Pendiente", M + 278f, y + 17f, paint(C_MUTED, 8f, TF_ITALIC))
            } else {
                // Mini barra de progreso
                roundRect(canvas, M + 278f, y + 10f, M + 388f, y + 20f, 3f, C_DIVIDER)
                roundRect(canvas, M + 278f, y + 10f,
                    M + 278f + 110f * (area.riskScore / 100f), y + 20f, 3f, rc)
                canvas.drawText("${area.riskScore}", M + 392f, y + 17f, paint(rc, 8f, TF_BOLD))
            }

            // Nivel de riesgo
            if (area.score != -1) {
                roundRect(canvas, M + 403f, y + 5f, M + 450f, y + 21f, 3f,
                    Color(rc).copy(alpha = 0.15f).toArgbInt())
                val lvlP = paint(rc, 7f, TF_BOLD).apply { textAlign = Paint.Align.CENTER }
                canvas.drawText(area.risk.displayName, M + 426f, y + 16f, lvlP)
            }

            // Estado (ícono textual)
            val statusText = when {
                area.score == -1       -> "--"
                area.riskScore < 25    -> "OK"
                area.riskScore < 45    -> "Atender"
                area.riskScore < 65    -> "Urgente"
                else                   -> "Critico"
            }
            val statusColor = when {
                area.score == -1    -> C_MUTED
                area.riskScore < 25 -> C_BAJO
                area.riskScore < 45 -> C_MODERADO
                area.riskScore < 65 -> C_ALTO
                else                -> C_MUY_ALTO
            }
            canvas.drawText(statusText, M + 468f, y + 17f, paint(statusColor, 8f, TF_BOLD))

            // Línea divisora
            canvas.drawLine(M, y + 26f, M + CW, y + 26f, paint(C_DIVIDER, 0.5f))
            y += 26f
        }
        y += 14f

        // ── Fichas detalladas de áreas completadas ──────────────────────────
        drawSectionTitle(canvas, "DETALLE POR ÁREA", y)
        y += 22f

        val completedAreas = areas.filter { it.score != -1 }
        completedAreas.forEach { area ->
            if (y > PH - 90f) return@forEach  // evitar salir de página

            val rc    = area.risk.color.toInt()
            val cardH = 68f

            roundRect(canvas, M, y, M + CW, y + cardH, 8f, C_CARD)
            canvas.drawRect(M, y, M + 5f, y + cardH, fill(rc))

            // Nombre + peso
            canvas.drawText(area.label, M + 14f, y + 16f, paint(C_NAVY, 10f, TF_BOLD))
            canvas.drawText("Peso: ${(area.weight * 100).toInt()}%  •  Escala: ${
                if (area.higherIsBetter) "Mayor = Mejor" else "Mayor = Mayor riesgo"
            }", M + 14f, y + 28f, paint(C_MUTED, 8f))

            // Score destacado
            canvas.drawText("${area.riskScore}", M + CW - 64f, y + 34f, paint(rc, 26f, TF_BOLD).apply {
                textAlign = Paint.Align.RIGHT
            })
            canvas.drawText("Índice riesgo", M + CW - 64f, y + 46f, paint(C_MUTED, 7f).apply {
                textAlign = Paint.Align.RIGHT
            })

            // Barra ancha
            val bx = M + 14f
            val bw = CW - 80f
            roundRect(canvas, bx, y + 38f, bx + bw, y + 48f, 4f, C_DIVIDER)
            roundRect(canvas, bx, y + 38f, bx + bw * (area.riskScore / 100f), y + 48f, 4f, rc)

            // Umbral del área
            val threshold = getAreaThreshold(area.key)
            canvas.drawText(threshold, M + 14f, y + 60f, paint(C_MUTED, 7f, TF_ITALIC))

            y += cardH + 8f
        }

        drawFooter(canvas, 2, 3)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PÁGINA 3 — Áreas críticas + Recomendaciones
    // ─────────────────────────────────────────────────────────────────────────
    private fun drawPage3(canvas: Canvas, hs: HealthScore) {
        drawPageHeader(canvas, "ÁREAS PRIORITARIAS Y RECOMENDACIONES", "Plan de acción personalizado")

        var y = 80f

        // ── Áreas prioritarias ─────────────────────────────────────────────
        if (hs.topConcerns.isNotEmpty()) {
            drawSectionTitle(canvas, "ÁREAS QUE REQUIEREN ATENCIÓN INMEDIATA", y)
            y += 22f

            hs.topConcerns.forEachIndexed { i, concern ->
                if (y > PH - 120f) return@forEachIndexed
                val rc = concern.risk.color.toInt()

                roundRect(canvas, M, y, M + CW, y + 44f, 8f, C_CARD)
                canvas.drawRect(M, y, M + 5f, y + 44f, fill(rc))

                // Número de orden
                canvas.drawCircle(M + 22f, y + 22f, 12f, fill(rc))
                canvas.drawText("${i + 1}", M + 22f, y + 26f,
                    paint(C_WHITE, 10f, TF_BOLD).apply { textAlign = Paint.Align.CENTER })

                // Nombre del área
                canvas.drawText(concern.name, M + 42f, y + 18f, paint(C_NAVY, 11f, TF_BOLD))

                // Descripción de urgencia
                val urgDesc = when (concern.risk) {
                    RiskLevel.MUY_ALTO -> "Requiere atención profesional urgente"
                    RiskLevel.ALTO     -> "Se recomienda intervención en los próximos 7 días"
                    RiskLevel.MODERADO -> "Monitorear y aplicar cambios de hábito"
                    RiskLevel.BAJO     -> "Continuar con las prácticas actuales"
                }
                canvas.drawText(urgDesc, M + 42f, y + 32f, paint(C_MUTED, 8f))

                // Badge nivel
                val bx = M + CW - 84f
                roundRect(canvas, bx, y + 10f, bx + 76f, y + 34f, 4f,
                    Color(rc).copy(alpha = 0.15f).toArgbInt())
                canvas.drawText(concern.risk.displayName.uppercase(), bx + 38f, y + 26f,
                    paint(rc, 8f, TF_BOLD).apply { textAlign = Paint.Align.CENTER })

                y += 50f
            }
            y += 8f
        }

        // ── Recomendaciones ────────────────────────────────────────────────
        if (hs.recommendations.isNotEmpty()) {
            drawSectionTitle(canvas, "RECOMENDACIONES PERSONALIZADAS", y)
            y += 22f

            hs.recommendations.forEach { rec ->
                if (y > PH - 80f) return@forEach

                val rc = rec.urgency.color.toInt()
                val urgLabel = when (rec.urgency) {
                    RiskLevel.MUY_ALTO -> "URGENTE"
                    RiskLevel.ALTO     -> "PRIORITARIO"
                    RiskLevel.MODERADO -> "RECOMENDADO"
                    RiskLevel.BAJO     -> "SUGERIDO"
                }

                // Calcular altura necesaria
                val lines = wrapText(rec.text, paint(C_BODY, 9f), CW - 100f)
                val cardH = 20f + lines.size * 13f + 10f

                roundRect(canvas, M, y, M + CW, y + cardH, 6f, C_CARD)
                canvas.drawRect(M, y, M + 4f, y + cardH, fill(rc))

                // Badge de urgencia
                roundRect(canvas, M + 12f, y + 8f, M + 80f, y + 22f, 3f,
                    Color(rc).copy(alpha = 0.15f).toArgbInt())
                canvas.drawText(urgLabel, M + 46f, y + 18f,
                    paint(rc, 7f, TF_BOLD).apply { textAlign = Paint.Align.CENTER })

                // Texto de la recomendación
                var ty = y + 10f
                lines.forEach { line ->
                    canvas.drawText(line, M + 88f, ty + 12f, paint(C_BODY, 9f))
                    ty += 13f
                }

                y += cardH + 6f
            }
            y += 8f
        }

        // ── Bloque de cierre / disclaimer ─────────────────────────────────
        if (y < PH - 120f) {
            val disclaimerY = maxOf(y + 16f, PH - 160f)
            canvas.drawLine(M, disclaimerY, M + CW, disclaimerY, paint(C_DIVIDER, 1f))
            canvas.drawText("AVISO IMPORTANTE", M, disclaimerY + 16f,
                paint(C_NAVY, 9f, TF_BOLD))
            val disclaimer = "Este reporte ha sido generado automáticamente por el sistema UleaMmed a partir de " +
                    "las respuestas proporcionadas en las encuestas de salud laboral. Los resultados son " +
                    "orientativos y no sustituyen el diagnóstico de un profesional de la salud. Si experimenta " +
                    "síntomas severos o persistentes, consulte con su médico o especialista."
            drawWrappedText(canvas, disclaimer, M, disclaimerY + 30f, CW,
                paint(C_MUTED, 8f, TF_ITALIC), 13f)
        }

        drawFooter(canvas, 3, 3)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPONENTES DE DIBUJO REUTILIZABLES
    // ─────────────────────────────────────────────────────────────────────────

    /** Cabecera interna de página (páginas 2 y 3) */
    private fun drawPageHeader(canvas: Canvas, title: String, subtitle: String) {
        canvas.drawRect(0f, 0f, PW, 68f, fill(C_NAVY))
        canvas.drawText("UleaMmed", M, 28f, paint(C_WHITE, 10f, TF_BOLD))
        canvas.drawText(title, M, 46f, paint(C_WHITE, 13f, TF_BOLD))
        canvas.drawText(subtitle, M, 60f, paint(Color.parseColor("#B0C4DE"), 8f))
    }

    /** Título de sección con línea decorativa */
    private fun drawSectionTitle(canvas: Canvas, title: String, y: Float) {
        canvas.drawRect(M, y + 2f, M + 4f, y + 16f, fill(C_BLUE))
        canvas.drawText(title, M + 10f, y + 14f, paint(C_NAVY, 10f, TF_BOLD))
        canvas.drawLine(M + 10f, y + 18f, M + CW, y + 18f, paint(C_DIVIDER, 1f))
    }

    /** Pie de página */
    private fun drawFooter(canvas: Canvas, current: Int, total: Int) {
        canvas.drawRect(0f, PH - 28f, PW, PH, fill(C_NAVY))
        canvas.drawText("UleaMmed • Reporte de Salud Laboral", M, PH - 10f,
            paint(Color.parseColor("#8899BB"), 8f))
        val pagePaint = paint(Color.parseColor("#8899BB"), 8f).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Página $current de $total", PW - M, PH - 10f, pagePaint)
    }

    /** Rect con esquinas redondeadas */
    private fun roundRect(canvas: Canvas, l: Float, t: Float, r: Float, b: Float,
                          radius: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(l, t, r, b), radius, radius, p)
    }

    /** Paint de relleno sólido */
    private fun fill(color: Int) = Paint().apply { this.color = color; style = Paint.Style.FILL }

    /** Paint de texto configurable */
    private fun paint(color: Int, size: Float, typeface: Typeface = TF_NORMAL) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color    = color
            this.textSize = size
            this.typeface = typeface
        }

    /** Texto multilínea */
    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float,
                                maxW: Float, paint: Paint, lineH: Float = 14f) {
        wrapText(text, paint, maxW).forEachIndexed { i, line ->
            canvas.drawText(line, x, y + i * lineH, paint)
        }
    }

    /** Divide texto en líneas que caben en maxWidth */
    private fun wrapText(text: String, paint: Paint, maxW: Float): List<String> {
        val words   = text.split(" ")
        val lines   = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxW) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATOS
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildAreas(hs: HealthScore): List<AreaInfo> = listOf(
        AreaInfo("estres",              "Estrés y Salud Mental",  hs.estresSaludMentalScore,   hs.estresSaludMentalRisk,   false, AREA_WEIGHTS["estres"]!!),
        AreaInfo("sintomas_musculares", "Síntomas Musculares",    hs.sintomasMuscularesScore,  hs.sintomasMuscularesRisk,  false, AREA_WEIGHTS["sintomas_musculares"]!!),
        AreaInfo("carga_trabajo",       "Carga de Trabajo",       hs.cargaTrabajoScore,        hs.cargaTrabajoRisk,        false, AREA_WEIGHTS["carga_trabajo"]!!),
        AreaInfo("sueno",               "Calidad del Sueño",      hs.habitosSuenoScore,        hs.habitosSuenoRisk,        false, AREA_WEIGHTS["sueno"]!!),
        AreaInfo("balance",             "Balance Vida-Trabajo",   hs.balanceVidaTrabajoScore,  hs.balanceVidaTrabajoRisk,  false, AREA_WEIGHTS["balance"]!!),
        AreaInfo("salud_general",       "Salud General",          hs.saludGeneralScore,        hs.saludGeneralRisk,        false, AREA_WEIGHTS["salud_general"]!!),
        AreaInfo("ergonomia",           "Ergonomía",              hs.ergonomiaScore,           hs.ergonomiaRisk,           true,  AREA_WEIGHTS["ergonomia"]!!),
        AreaInfo("sintomas_visuales",   "Síntomas Visuales",      hs.sintomasVisualesScore,    hs.sintomasVisualesRisk,    false, AREA_WEIGHTS["sintomas_visuales"]!!),
        AreaInfo("actividad_fisica",    "Actividad Física",       hs.actividadFisicaScore,     hs.actividadFisicaRisk,     false, AREA_WEIGHTS["actividad_fisica"]!!),
    )

    /** Texto de umbral / interpretación clínica por área */
    private fun getAreaThreshold(key: String): String = when (key) {
        "ergonomia"            -> "Bajo (≥80) • Moderado (60-79) • Alto (40-59) • Muy Alto (<40)  |  Escala invertida: mayor puntaje = mejor ergonomía"
        "sintomas_musculares"  -> "Bajo (<20) • Moderado (20-39) • Alto (40-59) • Muy Alto (≥60)"
        "sintomas_visuales"    -> "Bajo (<20) • Moderado (20-39) • Alto (40-59) • Muy Alto (≥60)"
        "carga_trabajo"        -> "Bajo (<25) • Moderado (25-44) • Alto (45-64) • Muy Alto (≥65)"
        "estres"               -> "Bajo (<25) • Moderado (25-44) • Alto (45-64) • Muy Alto (≥65)"
        "sueno"                -> "Bajo (<20) • Moderado (20-39) • Alto (40-59) • Muy Alto (≥60)"
        "actividad_fisica"     -> "Bajo (<25) • Moderado (25-44) • Alto (45-64) • Muy Alto (≥65)"
        "balance"              -> "Bajo (<25) • Moderado (25-44) • Alto (45-64) • Muy Alto (≥65)"
        "salud_general"        -> "Bajo (<20) • Moderado (20-39) • Alto (40-59) • Muy Alto (≥60)"
        else                   -> ""
    }

    // ── Extensión: Long → Int para android.graphics.Color ─────────────────
    private fun Long.toInt(): Int = this.and(0xFFFFFFFFL).toInt()

    /** Convierte androidx.compose.ui.graphics.Color → ARGB Int para android.graphics */
    private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int =
        android.graphics.Color.argb(
            (alpha * 255).toInt(), (red * 255).toInt(),
            (green * 255).toInt(), (blue * 255).toInt()
        )

    /** Versión lightweight sin Compose: mezcla color con alpha blanco */
    private fun Color(argb: Int): AndroidColor = AndroidColor(argb)
    private data class AndroidColor(val argb: Int) {
        fun copy(alpha: Float): AndroidColor {
            val a = (alpha * 255).toInt()
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            return AndroidColor(android.graphics.Color.argb(a, r, g, b))
        }
        fun toArgbInt() = argb
    }

    // ── Abrir PDF con FileProvider ────────────────────────────────────────────
    private fun openPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir reporte PDF"))
    }
}