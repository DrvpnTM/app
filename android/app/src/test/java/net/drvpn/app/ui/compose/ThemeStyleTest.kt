package net.drvpn.app.ui.compose

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeStyleTest {
    private val styles = listOf("default", "neomorph", "glass", "ocean", "forest", "sunset", "midnight")

    @Test
    fun everyStyleHasDistinctLightAndDarkAccents() {
        assertEqualsUnique(styles.map { themeColorScheme(it, dark = false).primary.value.toString() })
        assertEqualsUnique(styles.map { themeColorScheme(it, dark = true).primary.value.toString() })
    }

    @Test
    fun everyStyleKeepsReadableSurfaceTextInLightAndDarkModes() {
        styles.forEach { style ->
            listOf(false, true).forEach { dark ->
                val scheme = themeColorScheme(style, dark)
                assertTrue("$style (dark=$dark)", contrast(scheme.onSurface, scheme.surface) >= 4.5)
            }
        }
    }

    @Test
    fun stylesChangeMaterialComponentShapes() {
        assertNotEquals(themeShapes("default").medium, themeShapes("midnight").medium)
        assertNotEquals(themeShapes("default").medium, themeShapes("ocean").medium)
    }

    private fun assertEqualsUnique(values: List<String>) {
        assertTrue("Expected a distinct accent per theme", values.toSet().size == values.size)
    }

    private fun contrast(foreground: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color): Double {
        val first = luminance(foreground)
        val second = luminance(background)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    private fun luminance(color: androidx.compose.ui.graphics.Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
