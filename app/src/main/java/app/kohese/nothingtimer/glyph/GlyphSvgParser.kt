package app.kohese.nothingtimer.glyph

import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

data class SvgStop(
    val id: String,
    val offset: Float,
    val stopColor: String,
    val stopOpacity: Float
)

data class SvgLinearGradient(
    val id: String,
    val x1Fraction: Float,
    val y1Fraction: Float,
    val x2Fraction: Float,
    val y2Fraction: Float,
    val stops: List<SvgStop>
)

data class SvgPath(
    val id: String,
    val d: String,
    val opacity: Float,
    val style: String?
)

data class SvgRect(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val fill: String,
    val opacity: Float
)

data class SvgGroupTransform(
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f
)

data class ParsedGlyphSvg(
    val id: String,
    val width: Float,
    val height: Float,
    val viewBox: List<Float>,
    val transform: SvgGroupTransform,
    val paths: List<SvgPath>,
    val rects: List<SvgRect>,
    val gradients: Map<String, SvgLinearGradient>
)

object GlyphSvgParser {

    fun parse(inputStream: InputStream): ParsedGlyphSvg {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        val root = doc.documentElement

        val id = root.getAttribute("id") ?: ""
        val width = root.getAttribute("width").toFloatOrNull() ?: 182f
        val height = root.getAttribute("height").toFloatOrNull() ?: 182f
        val viewBoxAttr = root.getAttribute("viewBox") ?: "0 0 $width $height"
        val viewBox = viewBoxAttr.trim().split(Regex("\\s+|,")).mapNotNull { it.toFloatOrNull() }

        val gradients = mutableMapOf<String, SvgLinearGradient>()
        val paths = mutableListOf<SvgPath>()
        val rects = mutableListOf<SvgRect>()
        var groupTransform = SvgGroupTransform()

        parseElement(root, gradients, paths, rects) { transform ->
            groupTransform = transform
        }

        return ParsedGlyphSvg(
            id = id,
            width = width,
            height = height,
            viewBox = if (viewBox.size == 4) viewBox else listOf(0f, 0f, width, height),
            transform = groupTransform,
            paths = paths,
            rects = rects,
            gradients = gradients
        )
    }

    private fun parseElement(
        element: Element,
        gradients: MutableMap<String, SvgLinearGradient>,
        paths: MutableList<SvgPath>,
        rects: MutableList<SvgRect>,
        onTransformFound: (SvgGroupTransform) -> Unit
    ) {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val child = node as Element
            when (child.localName ?: child.tagName) {
                "linearGradient" -> {
                    val gId = child.getAttribute("id") ?: ""
                    val x1 = parseFractionOrPercent(child.getAttribute("x1"))
                    val y1 = parseFractionOrPercent(child.getAttribute("y1"))
                    val x2 = parseFractionOrPercent(child.getAttribute("x2"))
                    val y2 = parseFractionOrPercent(child.getAttribute("y2"))
                    val stops = mutableListOf<SvgStop>()
                    val stopNodes = child.getElementsByTagName("stop")
                    for (j in 0 until stopNodes.length) {
                        val stopEl = stopNodes.item(j) as Element
                        val sId = stopEl.getAttribute("id") ?: ""
                        val offset = parseFractionOrPercent(stopEl.getAttribute("offset"))
                        val stopColor = stopEl.getAttribute("stop-color").ifEmpty { "white" }
                        val stopOpacity = stopEl.getAttribute("stop-opacity").toFloatOrNull() ?: 1.0f
                        stops.add(SvgStop(sId, offset, stopColor, stopOpacity))
                    }
                    gradients[gId] = SvgLinearGradient(gId, x1, y1, x2, y2, stops)
                }
                "g" -> {
                    val transformAttr = child.getAttribute("transform")
                    if (!transformAttr.isNullOrEmpty()) {
                        onTransformFound(parseTransform(transformAttr))
                    }
                    parseElement(child, gradients, paths, rects, onTransformFound)
                }
                "path" -> {
                    val pId = child.getAttribute("id") ?: ""
                    val rawD = child.getAttribute("d") ?: ""
                    // Normalize standard SVG minus zero anomalies (e.g. "l-0,-25" -> "l0,-25")
                    val normalizedD = rawD.replace("-0,", "0,").replace("-0 ", "0 ").replace("-0.", "-0.")
                    val opacity = child.getAttribute("opacity").toFloatOrNull() ?: 1.0f
                    val style = child.getAttribute("style").takeIf { it.isNotEmpty() }
                    paths.add(SvgPath(pId, normalizedD, opacity, style))
                }
                "rect" -> {
                    val rId = child.getAttribute("id") ?: ""
                    val x = child.getAttribute("x").toFloatOrNull() ?: 0f
                    val y = child.getAttribute("y").toFloatOrNull() ?: 0f
                    val w = child.getAttribute("width").toFloatOrNull() ?: 0f
                    val h = child.getAttribute("height").toFloatOrNull() ?: 0f
                    val fill = child.getAttribute("fill").ifEmpty { "white" }
                    val opacity = child.getAttribute("opacity").toFloatOrNull() ?: 1.0f
                    rects.add(SvgRect(rId, x, y, w, h, fill, opacity))
                }
                else -> {
                    parseElement(child, gradients, paths, rects, onTransformFound)
                }
            }
        }
    }

    private fun parseTransform(transformStr: String): SvgGroupTransform {
        var tx = 0f
        var ty = 0f
        var sx = 1f
        var sy = 1f

        // Check for matrix(a, b, c, d, e, f)
        val matrixMatch = Regex("""matrix\(\s*([-\d.]+)\s*,\s*([-\d.]+)\s*,\s*([-\d.]+)\s*,\s*([-\d.]+)\s*,\s*([-\d.]+)\s*,\s*([-\d.]+)\s*\)""")
            .find(transformStr)
        if (matrixMatch != null) {
            val a = matrixMatch.groupValues[1].toFloatOrNull() ?: 1f
            val d = matrixMatch.groupValues[4].toFloatOrNull() ?: 1f
            val e = matrixMatch.groupValues[5].toFloatOrNull() ?: 0f
            val f = matrixMatch.groupValues[6].toFloatOrNull() ?: 0f
            sx = a
            sy = d
            tx = e
            ty = f
            return SvgGroupTransform(tx, ty, sx, sy)
        }

        // Check for translate(x, y)
        val translateMatch = Regex("""translate\(\s*([-\d.]+)\s*,\s*([-\d.]+)\s*\)""").find(transformStr)
        if (translateMatch != null) {
            tx = translateMatch.groupValues[1].toFloatOrNull() ?: 0f
            ty = translateMatch.groupValues[2].toFloatOrNull() ?: 0f
        }

        // Check for scale(sx) or scale(sx, sy)
        val scaleMatch = Regex("""scale\(\s*([-\d.]+)(?:\s*,\s*([-\d.]+))?\s*\)""").find(transformStr)
        if (scaleMatch != null) {
            val s1 = scaleMatch.groupValues[1].toFloatOrNull() ?: 1f
            val s2 = scaleMatch.groupValues[2].toFloatOrNull() ?: s1
            sx = s1
            sy = s2
        }

        return SvgGroupTransform(tx, ty, sx, sy)
    }

    private fun parseFractionOrPercent(value: String): Float {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return 0f
        return if (trimmed.endsWith("%")) {
            val num = trimmed.removeSuffix("%").toFloatOrNull() ?: 0f
            num / 100f
        } else {
            trimmed.toFloatOrNull() ?: 0f
        }
    }
}


