package com.zpi.slowomir.ui.icons


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val speech_to_text: ImageVector
    get() {
        if (_speech_to_text != null) {
            return _speech_to_text!!
        }
        _speech_to_text =
            ImageVector.Builder(
                name = "speech_to_text",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(17f, 10f)
                        quadTo(16.18f, 10f, 15.59f, 9.42f)
                        reflectiveQuadTo(15f, 8f)
                        verticalLineTo(4f)
                        quadTo(15f, 3.15f, 15.59f, 2.57f)
                        reflectiveQuadTo(17f, 2f)
                        quadToRelative(0.85f, 0f, 1.43f, 0.57f)
                        reflectiveQuadTo(19f, 4f)
                        verticalLineTo(8f)
                        quadToRelative(0f, 0.85f, -0.57f, 1.42f)
                        reflectiveQuadTo(17f, 10f)
                        close()
                        moveTo(5f, 22f)
                        quadTo(4.18f, 22f, 3.59f, 21.41f)
                        reflectiveQuadTo(3f, 20f)
                        verticalLineTo(4f)
                        quadTo(3f, 3.17f, 3.59f, 2.59f)
                        reflectiveQuadTo(5f, 2f)
                        horizontalLineToRelative(8f)
                        verticalLineTo(4f)
                        horizontalLineTo(5f)
                        verticalLineTo(20f)
                        horizontalLineTo(16f)
                        verticalLineTo(18f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(2f)
                        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                        reflectiveQuadTo(16f, 22f)
                        horizontalLineTo(5f)
                        close()
                        moveTo(7f, 18f)
                        verticalLineTo(16f)
                        horizontalLineToRelative(7f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(7f)
                        close()
                        moveTo(7f, 15f)
                        verticalLineTo(13f)
                        horizontalLineToRelative(5f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(7f)
                        close()
                        moveToRelative(11f, 1f)
                        horizontalLineTo(16f)
                        verticalLineTo(13.4f)
                        quadTo(14.08f, 13.05f, 12.79f, 11.54f)
                        quadTo(11.5f, 10.02f, 11.5f, 8f)
                        horizontalLineToRelative(2f)
                        quadToRelative(0f, 1.45f, 1.03f, 2.47f)
                        reflectiveQuadTo(17f, 11.5f)
                        quadToRelative(1.48f, 0f, 2.49f, -1.03f)
                        reflectiveQuadTo(20.5f, 8f)
                        horizontalLineToRelative(2f)
                        quadToRelative(0f, 2.02f, -1.27f, 3.54f)
                        reflectiveQuadTo(18f, 13.4f)
                        verticalLineTo(16f)
                        close()
                    }
                }
                .build()
        return _speech_to_text!!
    }

private var _speech_to_text: ImageVector? = null
