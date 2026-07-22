package com.calb.qr2card

import android.content.Context
import com.calb.qr2card.data.EmployeeCardData
import com.calb.qr2card.data.TemplateConfig
import com.calb.qr2card.svg.EditableSvgPackageService
import com.calb.qr2card.svg.escapeSvgText
import com.calb.qr2card.svg.ptToMm
import com.calb.qr2card.svg.qrPathData
import com.calb.qr2card.svg.svgNumber
import com.calb.qr2card.qr.QrCodeService
import com.google.zxing.common.BitMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditableSvgMarkupTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `escapes all XML text delimiters`() {
        assertEquals(
            "CALB &amp; &lt;Design&gt; &quot;A&quot; &apos;B&apos;",
            escapeSvgText("CALB & <Design> \"A\" 'B'"),
        )
    }

    @Test
    fun `creates vector path commands only for dark QR modules`() {
        val matrix = BitMatrix(2, 2).apply {
            set(0, 0)
            set(1, 1)
        }

        assertEquals("M0 0h1v1h-1zM1 1h1v1h-1z", qrPathData(matrix))
    }

    @Test
    fun `generates an unscaled QR module matrix for SVG export`() {
        val matrix = QrCodeService().generateQrMatrix("BEGIN:VCARD\r\nEND:VCARD\r\n", sizePx = 1)

        assertTrue(matrix.width > 20)
        assertEquals(matrix.width, matrix.height)
        assertTrue(qrPathData(matrix).isNotBlank())
    }

    @Test
    fun `converts point sizes to millimetres for physical SVG layout`() {
        assertEquals("12.6", svgNumber(ptToMm(35.716537f)))
        assertTrue(ptToMm(12.6f) > 4f)
    }

    @Test
    fun `writes one XML-valid Illustrator SVG holding both card faces`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val svgFile = EditableSvgPackageService().generateEditableSvg(
            context = context,
            data = EmployeeCardData(),
            config = TemplateConfig(),
            outputDir = temporaryFolder.newFolder("editable-svg"),
        )

        assertEquals("CALB_Business_Card_Alex_Zhao_Editable.svg", svgFile.name)
        val markup = svgFile.readText()
        assertTrue(markup.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        // Nothing may be linked: a relative reference breaks as soon as the file is moved,
        // and Illustrator then draws a missing-link placeholder where the logo should be.
        assertEquals(0, markup.split("href=\"assets/").size - 1)

        val document = svgFile.inputStream().use {
            DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(it)
        }
        val root = document.documentElement
        assertEquals("svg", root.localName)
        // 98 x 62 mm per card, stacked with a 4 mm gap.
        assertEquals("98mm", root.getAttribute("width"))
        assertEquals("128mm", root.getAttribute("height"))
        assertEquals("0 0 98 128", root.getAttribute("viewBox"))

        val groupIds = document.getElementsByTagName("g").let { nodes ->
            (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("id") }
        }
        assertTrue(groupIds.contains("card-front"))
        assertTrue(groupIds.contains("card-back"))

        val images = document.getElementsByTagName("image")
        assertEquals(2, images.length)
        for (index in 0 until images.length) {
            val href = images.item(index)
                .attributes
                .getNamedItemNS("http://www.w3.org/1999/xlink", "href")
                .nodeValue
            assertTrue(href.startsWith("data:image/png;base64,"))
        }
        val logo = images.item(1).attributes
        assertEquals("11.3", logo.getNamedItem("x").nodeValue)
        assertEquals("10.35", logo.getNamedItem("y").nodeValue)
        assertEquals("22.7", logo.getNamedItem("width").nodeValue)
        assertEquals("7.1", logo.getNamedItem("height").nodeValue)

        assertTrue(document.getElementsByTagName("text").length > 0)
        // Vector QR squares live on the back card.
        assertTrue(document.getElementsByTagName("path").length > 2)
    }

    @Test
    fun `packages the svg and fonts into a shareable zip`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val zipFile = EditableSvgPackageService().generateEditablePackage(
            context = context,
            data = EmployeeCardData(),
            config = TemplateConfig(),
            outputDir = temporaryFolder.newFolder("editable-zip"),
        )

        assertEquals("CALB_Business_Card_Alex_Zhao_Editable.zip", zipFile.name)
        ZipFile(zipFile).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            assertTrue(names.contains("Alex_Zhao_Editable.svg"))
            assertTrue(names.contains("fonts/Manrope-Regular.otf"))
            assertTrue(names.contains("fonts/Manrope-Bold.otf"))
            assertTrue(names.contains("fonts/HarmonyOS_Sans_SC_Regular.ttf"))
            assertTrue(names.contains("README.txt"))

            val svg = zip.getInputStream(zip.getEntry("Alex_Zhao_Editable.svg"))
                .use { it.readBytes().decodeToString() }
            assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
            assertEquals(0, svg.split("href=\"assets/").size - 1)
        }
    }
}
