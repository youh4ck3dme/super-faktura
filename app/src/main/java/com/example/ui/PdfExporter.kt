package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun exportInvoicesToPdf(context: Context, invoices: List<Invoice>) {
        if (invoices.isEmpty()) {
            Toast.makeText(context, "Žiadne doklady na exportovanie", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create a temporary file in the cache directory
            val cacheDir = context.cacheDir
            val pdfFile = File(cacheDir, "Prehlad_dokladov_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)

            // Initialize PDF Document
            val pdfDocument = PdfDocument()
            var pageNumber = 1

            // Page dimensions: A4 (595 x 842 points)
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Paint configurations
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(29, 27, 32) // Dark text
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val boldPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(29, 27, 32)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val rightAlignPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(29, 27, 32)
                textSize = 9f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val rightAlignBoldPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(29, 27, 32)
                textSize = 9f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val headerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(103, 80, 164) // Primary M3 Purple
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subHeaderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(73, 69, 79) // Medium grey
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val linePaint = Paint().apply {
                isAntiAlias = true
                strokeWidth = 1f
                color = Color.rgb(202, 196, 208) // Outline gray
            }

            val thinLinePaint = Paint().apply {
                isAntiAlias = true
                strokeWidth = 0.5f
                color = Color.rgb(230, 225, 230) // Faint gray
            }

            val boxPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(243, 237, 255) // Light purple background
                style = Paint.Style.FILL
            }

            // Function to draw header on a page
            fun drawPageHeader(pageNum: Int, canvasToWrite: android.graphics.Canvas) {
                // Main Title
                canvasToWrite.drawText("PREHĽAD VÝDAVKOV A FAKTÚR", 40f, 60f, headerPaint)
                
                // Subtitle / Description
                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                canvasToWrite.drawText("Mesačný prehľad vygenerovaný dňa: $dateStr", 40f, 78f, subHeaderPaint)
                
                // Page Number right-aligned
                canvasToWrite.drawText("Strana $pageNum", 555f, 60f, rightAlignPaint)

                // Colored line under header
                linePaint.color = Color.rgb(103, 80, 164)
                linePaint.strokeWidth = 1.5f
                canvasToWrite.drawLine(40f, 90f, 555f, 90f, linePaint)
            }

            // 1. Draw Page Header on first page
            drawPageHeader(pageNumber, canvas)

            // 2. Draw Overview Summary box (only on first page)
            canvas.drawRoundRect(40f, 110f, 555f, 165f, 12f, 12f, boxPaint)
            linePaint.color = Color.rgb(202, 196, 208)
            linePaint.strokeWidth = 1f
            linePaint.style = Paint.Style.STROKE
            canvas.drawRoundRect(40f, 110f, 555f, 165f, 12f, 12f, linePaint)
            linePaint.style = Paint.Style.FILL

            // Content inside Summary box
            boldPaint.textSize = 9.5f
            canvas.drawText("SÚHRNNÉ INFORMÁCIE PRE ÚČTOVNÍKA", 55f, 128f, boldPaint)
            boldPaint.textSize = 9f

            val totalInvoiced = invoices.sumOf { it.totalAmount }
            val totalVat = invoices.sumOf { it.vatAmount ?: 0.0 }
            val totalDocCount = invoices.size

            canvas.drawText("Počet dokladovcelkovo: $totalDocCount", 55f, 143f, textPaint)
            canvas.drawText("Obdobie: Aktuálny výber", 55f, 155f, textPaint)

            canvas.drawText("Suma celkom (s DPH): ${String.format(Locale.GERMANY, "%.2f €", totalInvoiced)}", 300f, 143f, boldPaint)
            canvas.drawText("DPH celkom: ${String.format(Locale.GERMANY, "%.2f €", totalVat)}", 300f, 155f, textPaint)

            // 3. Draw Table Headers
            var y = 195f
            boldPaint.color = Color.rgb(29, 27, 32)
            canvas.drawText("Dátum", 40f, y, boldPaint)
            canvas.drawText("Dodávateľ", 110f, y, boldPaint)
            canvas.drawText("IČO / DIČ", 240f, y, boldPaint)
            canvas.drawText("Kategória", 345f, y, boldPaint)
            canvas.drawText("DPH", 460f, y, boldPaint)
            canvas.drawText("Suma s DPH", 555f, y, rightAlignBoldPaint)

            // Table Header separator line
            linePaint.color = Color.rgb(103, 80, 164)
            linePaint.strokeWidth = 1.2f
            canvas.drawLine(40f, y + 8f, 555f, y + 8f, linePaint)

            // Start rows at y = 220f
            y = 220f

            // Iterate over invoices and render them
            invoices.forEachIndexed { index, invoice ->
                // Check page break condition
                if (y > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    // Draw Header on new page
                    drawPageHeader(pageNumber, canvas)

                    // Draw Table Headers again on new page
                    y = 115f
                    canvas.drawText("Dátum", 40f, y, boldPaint)
                    canvas.drawText("Dodávateľ", 110f, y, boldPaint)
                    canvas.drawText("IČO / DIČ", 240f, y, boldPaint)
                    canvas.drawText("Kategória", 345f, y, boldPaint)
                    canvas.drawText("DPH", 460f, y, boldPaint)
                    canvas.drawText("Suma s DPH", 555f, y, rightAlignBoldPaint)

                    linePaint.color = Color.rgb(103, 80, 164)
                    linePaint.strokeWidth = 1.2f
                    canvas.drawLine(40f, y + 8f, 555f, y + 8f, linePaint)

                    y = 140f
                }

                // Format row fields
                val dateText = invoice.issueDate
                val supplierText = if (invoice.supplierName.length > 22) {
                    invoice.supplierName.substring(0, 20) + "..."
                } else {
                    invoice.supplierName
                }

                val icoDicText = when {
                    !invoice.ico.isNullOrBlank() && !invoice.dic.isNullOrBlank() -> "${invoice.ico} / ${invoice.dic}"
                    !invoice.ico.isNullOrBlank() -> invoice.ico
                    !invoice.dic.isNullOrBlank() -> invoice.dic
                    else -> "-"
                }
                val truncatedIcoDic = if (icoDicText.length > 18) {
                    icoDicText.substring(0, 16) + "..."
                } else {
                    icoDicText
                }

                val categoryText = invoice.category
                val vatText = if (invoice.vatAmount != null && invoice.vatAmount > 0.0) {
                    String.format(Locale.GERMANY, "%.2f €", invoice.vatAmount)
                } else {
                    "-"
                }
                val amountText = String.format(Locale.GERMANY, "%.2f €", invoice.totalAmount)

                // Alternating light row backgrounds for readability
                if (index % 2 == 1) {
                    val rowBgPaint = Paint().apply {
                        color = Color.rgb(250, 248, 252)
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(40f, y - 11f, 555f, y + 14f, rowBgPaint)
                }

                // Render row texts
                canvas.drawText(dateText, 40f, y, textPaint)
                canvas.drawText(supplierText, 110f, y, textPaint)
                canvas.drawText(truncatedIcoDic, 240f, y, textPaint)
                canvas.drawText(categoryText, 345f, y, textPaint)
                canvas.drawText(vatText, 460f, y, textPaint)
                canvas.drawText(amountText, 555f, y, rightAlignPaint)

                // Divider line
                canvas.drawLine(40f, y + 14f, 555f, y + 14f, thinLinePaint)

                y += 25f
            }

            // Draw Table Footer (Summary total row)
            if (y > 760f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawPageHeader(pageNumber, canvas)
                y = 110f
            }

            // Heavy separator line above totals
            linePaint.color = Color.rgb(29, 27, 32)
            linePaint.strokeWidth = 1.5f
            canvas.drawLine(40f, y, 555f, y, linePaint)

            y += 18f
            boldPaint.textSize = 10f
            rightAlignBoldPaint.textSize = 10f

            canvas.drawText("SPOLU CELKOM (€):", 345f, y, boldPaint)
            canvas.drawText(
                if (totalVat > 0.0) String.format(Locale.GERMANY, "%.2f €", totalVat) else "-",
                460f,
                y,
                boldPaint
            )
            canvas.drawText(
                String.format(Locale.GERMANY, "%.2f €", totalInvoiced),
                555f,
                y,
                rightAlignBoldPaint
            )

            // Double border line under total sum (classic accounting pattern)
            canvas.drawLine(40f, y + 6f, 555f, y + 6f, thinLinePaint)
            canvas.drawLine(40f, y + 8.5f, 555f, y + 8.5f, thinLinePaint)

            pdfDocument.finishPage(page)

            // Write to stream
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            // Open intent to share/view PDF
            sharePdfFile(context, pdfFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Chyba pri generovaní PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdfFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Mesačný prehľad dokladov pre účtovníka")
            putExtra(
                Intent.EXTRA_TEXT,
                "Dobrý deň,\nv prílohe Vám zasielam mesačný prehľad výdavkov a prijatých faktúr vygenerovaný z našej aplikácie."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Zdieľať mesačný prehľad (PDF)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
