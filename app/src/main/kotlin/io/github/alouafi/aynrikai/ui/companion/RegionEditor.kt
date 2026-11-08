package io.github.alouafi.aynrikai.ui.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.alouafi.aynrikai.R
import io.github.alouafi.aynrikai.settings.NormalizedRect
import kotlin.math.min

/** Lets the player drag a rectangle over the game's text box, on a frame of the game screen. */
@Composable
fun RegionEditor(
    frame: ImageBitmap,
    initial: NormalizedRect?,
    onSave: (NormalizedRect?) -> Unit,
    onCancel: () -> Unit,
) {
    var selection by remember { mutableStateOf(initial) }
    val stroke = MaterialTheme.colorScheme.primary

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
    ) {
        Text(
            stringResource(R.string.region_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val image = fitInside(
                content = Size(frame.width.toFloat(), frame.height.toFloat()),
                container = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
            )
            Image(
                bitmap = frame,
                contentDescription = stringResource(R.string.region_frame),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(image) {
                        var start = Offset.Zero
                        detectDragGestures(
                            onDragStart = { start = it },
                            onDrag = { change, _ ->
                                change.consume()
                                selection = image.normalize(start, change.position)
                            },
                        )
                    },
            ) {
                val selected = selection ?: return@Canvas
                val box = Rect(
                    left = image.left + selected.left * image.width,
                    top = image.top + selected.top * image.height,
                    right = image.left + selected.right * image.width,
                    bottom = image.top + selected.bottom * image.height,
                )
                clipRect(box.left, box.top, box.right, box.bottom, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = DIM_ALPHA), topLeft = image.topLeft, size = image.size)
                }
                drawRect(stroke, topLeft = box.topLeft, size = box.size, style = Stroke(width = 2.dp.toPx()))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.region_whole_screen)) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = { onSave(selection) }, enabled = selection?.isUsable == true) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

/** Where an image of [content] size lands when fitted and centered in [container]. */
internal fun fitInside(content: Size, container: Size): Rect {
    if (content.width <= 0f || content.height <= 0f) return Rect.Zero
    val scale = min(container.width / content.width, container.height / content.height)
    val width = content.width * scale
    val height = content.height * scale
    val left = (container.width - width) / 2
    val top = (container.height - height) / 2
    return Rect(left, top, left + width, top + height)
}

/** The rectangle between two touch points, in fractions of this on-screen image. */
private fun Rect.normalize(from: Offset, to: Offset): NormalizedRect = NormalizedRect.spanning(
    (from.x - left) / width,
    (from.y - top) / height,
    (to.x - left) / width,
    (to.y - top) / height,
)

private const val DIM_ALPHA = 0.6f
