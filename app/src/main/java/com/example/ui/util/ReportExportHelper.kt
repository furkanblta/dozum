package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.MedicationEntity
import com.example.data.model.MedicationWithDose
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportHelper {

    fun generatePdfReport(
        context: Context,
        userName: String,
        period: String,
        medications: List<MedicationEntity>,
        doses: List<MedicationWithDose>
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 (595 x 842)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val nowStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr")).format(Date())

            val totalDoses = doses.size
            val takenDoses = doses.count { it.dose.status == "TAKEN" }
            val adherence = if (totalDoses > 0) ((takenDoses.toFloat() / totalDoses) * 100).toInt() else 100

            // Header Background Accent
            paint.color = Color.rgb(0, 107, 98) // Dozum Primary
            canvas.drawRect(0f, 0f, 595f, 75f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DOZUM - KİŞİSEL İLAÇ VE DOZ TAKİP RAPORU", 30f, 38f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Düzenli ve Güvenli İlaç Takip Asistanı", 30f, 56f, paint)

            // Patient & Period Info Box
            var y = 105f
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val displayName = if (userName.isNotBlank()) userName else "Kayıtlı Kullanıcı"
            canvas.drawText("Kullanıcı: $displayName", 30f, y, paint)
            canvas.drawText("Rapor Dönemi: $period", 340f, y, paint)

            y += 18f
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Rapor Tarihi: $nowStr", 30f, y, paint)
            canvas.drawText("Aktif Takip Edilen İlaç: ${medications.size} Adet", 340f, y, paint)

            // Statistics Summary Box
            y += 28f
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(30f, y, 565f, y + 60f, 8f, 8f, paint)

            val boxY = y + 25f
            paint.color = Color.rgb(0, 107, 98)
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("%$adherence", 60f, boxY, paint)
            canvas.drawText("$takenDoses / $totalDoses", 220f, boxY, paint)
            canvas.drawText("${medications.size}", 430f, boxY, paint)

            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Dönem Uyum Oranı", 60f, boxY + 16f, paint)
            canvas.drawText("Alınan Doz / Toplam", 220f, boxY + 16f, paint)
            canvas.drawText("Kayıtlı İlaç Dolabı", 430f, boxY + 16f, paint)

            // Section 1: Active Medications
            y += 85f
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("1. Kayıtlı İlaçlar ve Kullanım Bilgileri", 30f, y, paint)

            // Table Header
            y += 16f
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(30f, y, 565f, y + 20f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("İlaç Adı", 35f, y + 14f, paint)
            canvas.drawText("Dozaj / Form", 180f, y + 14f, paint)
            canvas.drawText("Öğün Zamanı", 290f, y + 14f, paint)
            canvas.drawText("Hatırlatma Saatleri", 380f, y + 14f, paint)
            canvas.drawText("Kalan Stok", 495f, y + 14f, paint)

            y += 20f
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f

            medications.take(6).forEach { med ->
                y += 18f
                paint.color = Color.rgb(30, 41, 59)
                val medName = if (med.name.length > 22) med.name.take(20) + ".." else med.name
                canvas.drawText(medName, 35f, y, paint)
                canvas.drawText("${med.dosage} (${med.form})", 180f, y, paint)
                canvas.drawText(med.mealTiming, 290f, y, paint)
                val times = med.reminderTimes.removePrefix("OFF:").ifBlank { "Serbest" }
                canvas.drawText(times, 380f, y, paint)
                canvas.drawText("${med.currentStock} ${med.form.lowercase()}", 495f, y, paint)

                paint.color = Color.rgb(241, 245, 249)
                canvas.drawLine(30f, y + 4f, 565f, y + 4f, paint)
            }

            // Section 2: Recent Dose History Log
            y += 30f
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("2. Son Doz Kullanım ve İşaretleme Geçmişi", 30f, y, paint)

            y += 16f
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(30f, y, 565f, y + 20f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Tarih", 35f, y + 14f, paint)
            canvas.drawText("Planlı Saat", 120f, y + 14f, paint)
            canvas.drawText("İlaç", 200f, y + 14f, paint)
            canvas.drawText("Durum", 370f, y + 14f, paint)
            canvas.drawText("Alınma Zamanı", 470f, y + 14f, paint)

            y += 20f
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f

            // Sort doses descending by date and time
            val recentDoses = doses.sortedWith(
                compareByDescending<MedicationWithDose> { it.dose.dateKey }
                    .thenByDescending { it.dose.scheduledTime }
            ).take(15)

            recentDoses.forEach { item ->
                y += 18f
                paint.color = Color.rgb(30, 41, 59)
                canvas.drawText(item.dose.dateKey, 35f, y, paint)
                canvas.drawText(item.dose.scheduledTime, 120f, y, paint)
                val mName = if (item.medication.name.length > 24) item.medication.name.take(22) + ".." else item.medication.name
                canvas.drawText(mName, 200f, y, paint)

                if (item.dose.status == "TAKEN") {
                    paint.color = Color.rgb(16, 149, 107) // Green
                    canvas.drawText("✓ Alındı", 370f, y, paint)
                    paint.color = Color.rgb(71, 85, 105)
                    canvas.drawText(item.dose.takenTime ?: "Alındı", 470f, y, paint)
                } else if (item.dose.status == "SNOOZED") {
                    paint.color = Color.rgb(217, 119, 6) // Orange
                    canvas.drawText("⏰ Ertelendi", 370f, y, paint)
                    canvas.drawText("-", 470f, y, paint)
                } else {
                    paint.color = Color.rgb(220, 38, 38) // Red
                    canvas.drawText("⏳ Bekliyor / Atlandı", 370f, y, paint)
                    canvas.drawText("-", 470f, y, paint)
                }

                paint.color = Color.rgb(241, 245, 249)
                canvas.drawLine(30f, y + 4f, 565f, y + 4f, paint)
            }

            // Footer / Disclaimer
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(
                "Bu rapor Dozum uygulaması kişisel takip kayıtlarından derlenmiştir. Tıbbi reçete yerine geçmez.",
                30f,
                820f,
                paint
            )

            pdfDocument.finishPage(page)

            val outputFile = File(context.cacheDir, "Dozum_Ilac_Takip_Raporu.pdf")
            val outputStream = FileOutputStream(outputFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openOrSharePdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "PDF Raporunu Aç").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // If ACTION_VIEW fails (e.g. no direct viewer), fallback to ACTION_SEND
            sharePdfFile(context, pdfFile)
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Dozum İlaç Takip Raporu")
                putExtra(Intent.EXTRA_TEXT, "Dozum uygulaması üzerinden oluşturulmuş kişisel ilaç takip ve uyum raporum ekte yer almaktadır.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Raporu Paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareReportAsText(
        context: Context,
        userName: String,
        period: String,
        medications: List<MedicationEntity>,
        doses: List<MedicationWithDose>
    ) {
        val totalDoses = doses.size
        val takenDoses = doses.count { it.dose.status == "TAKEN" }
        val adherence = if (totalDoses > 0) ((takenDoses.toFloat() / totalDoses) * 100).toInt() else 100
        val nowStr = SimpleDateFormat("dd MMMM yyyy", Locale("tr")).format(Date())

        val sb = StringBuilder()
        sb.append("📋 DOZUM İLAÇ KULLANIM VE TAKİP RAPORU\n")
        if (userName.isNotBlank()) sb.append("Kullanıcı: $userName\n")
        sb.append("Rapor Dönemi: $period ($nowStr)\n")
        sb.append("------------------------------------\n")
        sb.append("📊 Genel Uyum Oranı: %$adherence ($takenDoses/$totalDoses Doz Alındı)\n")
        sb.append("💊 Kayıtlı İlaç Sayısı: ${medications.size} Adet\n\n")

        sb.append("📦 AKTİF İLAÇLAR:\n")
        medications.forEach { med ->
            sb.append("• ${med.name} (${med.dosage}, ${med.frequency}) - Kalan: ${med.currentStock} ${med.form.lowercase()}\n")
        }

        sb.append("\n📅 SON DOZ HAREKETLERİ:\n")
        val recent = doses.sortedWith(
            compareByDescending<MedicationWithDose> { it.dose.dateKey }
                .thenByDescending { it.dose.scheduledTime }
        ).take(10)

        recent.forEach { item ->
            val statusEmoji = if (item.dose.status == "TAKEN") "✓ [Alındı ${item.dose.takenTime ?: ""}]" else "⏳ [Bekliyor]"
            sb.append("${item.dose.dateKey} ${item.dose.scheduledTime} - ${item.medication.name}: $statusEmoji\n")
        }

        sb.append("\n(Dozum İlaç Takip Asistanı ile hazırlanmıştır)")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Dozum İlaç Takip Raporu")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Raporu Paylaş").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
