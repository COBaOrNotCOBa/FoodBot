import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFontFactory
import java.io.File
import java.io.FileOutputStream

class FilePdf() {
//создаем PDF файл с текстом

    fun createPdf(filePath: String, content: String): File {
        val pdf = PdfDocument(PdfWriter(FileOutputStream(filePath)))
        val document = Document(pdf)

        // Указываем поддержку кириллицы
        val font = PdfFontFactory.createFont("path_to_your_cyrillic_font.ttf", PdfEncodings.IDENTITY_H, true)

        document.setFont(font) // устанавливаем шрифт для документа
        document.add(Paragraph(content))
        document.close()

        return File(filePath)
    }
}