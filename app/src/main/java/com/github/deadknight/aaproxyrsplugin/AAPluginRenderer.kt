package com.github.deadknight.aaproxyrsplugin

import SerializerMoshi
import android.content.Context
import androidx.compose.remote.creation.compose.action.HostAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.github.deadknight.aaproxyrsplugin.plugin.PluginAction
import com.github.deadknight.aaproxyrsplugin.plugin.PluginActions
import com.github.deadknight.aaproxyrsplugin.plugin.PluginContract
import com.github.deadknight.aaproxyrsplugin.plugin.PluginDataKeys
import com.github.deadknight.aaproxyrsplugin.plugin.PluginJson
import com.github.deadknight.aaproxyrsplugin.plugin.PluginRenderRequest
import com.github.deadknight.aaproxyrsplugin.plugin.SystemThemeData
import com.github.deadknight.aaproxyrsplugin.plugin.VehicleOdometerData
import com.github.deadknight.aaproxyrsplugin.plugin.VehicleSpeedData
import com.github.deadknight.aaproxyrsplugin.plugin.VehicleTpmsData
import kotlin.math.roundToInt
import toJson

class AAPluginRenderer(private val context: Context) {
  suspend fun render(request: PluginRenderRequest, action: PluginAction? = null): ByteArray {

    val speed =
        PluginJson.decode<VehicleSpeedData>(request.data[PluginDataKeys.VEHICLE_SPEED.value])
    val odometer =
        PluginJson.decode<VehicleOdometerData>(request.data[PluginDataKeys.VEHICLE_ODOMETER.value])
    val tpms = PluginJson.decode<VehicleTpmsData>(request.data[PluginDataKeys.VEHICLE_TPMS.value])
    val theme = PluginJson.decode<SystemThemeData>(request.data[PluginDataKeys.SYSTEM_THEME.value])

    val currentScreen = request.navigationState.stack.lastOrNull()?.screen ?: "home"
    val background = if (theme?.theme == "light") Color(0xFFF5F7FA) else Color(0xFF1E293B)

    val captured =
        captureSingleRemoteDocument(
            creationDisplayInfo =
                RemoteCreationDisplayInfo(request.widthPx, request.heightPx, request.densityDpi),
            context = context,
        ) {
          RemoteBox(modifier = RemoteModifier.fillMaxSize().background(background)) {
            RemoteColumn(
                modifier =
                    RemoteModifier.fillMaxWidth().padding(horizontal = 24.rdp, vertical = 24.rdp)
            ) {
              when (currentScreen) {
                "home" -> {
                  RemoteText("Speed: ${speed?.speedKph?.roundToInt() ?: "--"}")
                  RemoteText("Odometer: ${odometer?.totalKm ?: "--"} km")
                  RemoteText(
                      "OPEN TPMS",
                      modifier =
                          RemoteModifier.padding(top = 24.rdp, bottom = 24.rdp)
                              .clickable(
                                  HostAction(
                                      PluginContract.HostActionIds.PLUGIN_ACTION,
                                      RemoteString("plugin_action"),
                                      RemoteString(
                                          SerializerMoshi.moshiKotlin.toJson(
                                              PluginAction::class.java,
                                              PluginAction(
                                                  action = PluginActions.PUSH,
                                                  screen = "tpms_detail",
                                                  paramsJson = "{}",
                                              ),
                                          )
                                      ),
                                  )
                              ),
                  )
                }

                "tpms_detail" -> {

                  val carBmp = remember {
                    context.getDrawable(R.drawable.ic_car)!!.toBitmap().asImageBitmap().rb
                  }

                  RemoteText("TPMS")
                  RemoteText(
                      "BACK",
                      modifier =
                          RemoteModifier.padding(top = 16.rdp, bottom = 24.rdp)
                              .clickable(
                                  HostAction(
                                      PluginContract.HostActionIds.PLUGIN_ACTION,
                                      RemoteString("plugin_action"),
                                      RemoteString(
                                          SerializerMoshi.moshiKotlin.toJson(
                                              PluginAction::class.java,
                                              PluginAction(action = PluginActions.POP),
                                          )
                                      ),
                                  )
                              ),
                  )

                  RemoteRow(modifier = RemoteModifier.fillMaxHeight()) {
                    RemoteColumn(modifier = RemoteModifier.fillMaxHeight()) {
                      RemoteText(
                          modifier = RemoteModifier.padding(0.rdp, 40.rdp, 0.rdp, 0.rdp),
                          text = tpms?.fl?.pressureKpa?.toString()?.rs ?: "-".rs,
                      )
                      RemoteBox(modifier = RemoteModifier.weight(1.0f.rf))
                      RemoteText(
                          modifier = RemoteModifier.padding(0.rdp, 0.rdp, 0.rdp, 40.rdp),
                          text = tpms?.rl?.pressureKpa?.toString()?.rs ?: "-".rs,
                      )
                    }

                    RemoteImage(remoteBitmap = carBmp, contentDescription = "".rs)

                    RemoteColumn(modifier = RemoteModifier.wrapContentSize()) {
                      RemoteText(
                          modifier = RemoteModifier.padding(0.rdp, 40.rdp, 0.rdp, 0.rdp),
                          text = tpms?.fr?.pressureKpa?.toString()?.rs ?: "-".rs,
                      )
                      RemoteBox(modifier = RemoteModifier.weight(1.0f.rf))
                      RemoteText(
                          modifier = RemoteModifier.padding(0.rdp, 0.rdp, 0.rdp, 40.rdp),
                          text = tpms?.rr?.pressureKpa?.toString()?.rs ?: "-".rs,
                      )
                    }
                  }

                  /*TpmsOverviewResponsive(
                      tpms = tpms,
                      surfaceWidthPx = request.widthPx,
                      surfaceHeightPx = request.heightPx,
                      densityDpi = request.densityDpi
                  )*/
                }
              }
            }
          }
        }

    return captured.bytes
  }
}

private fun formatPressureKpa(value: Float?): String {
  return value?.roundToInt()?.let { "$it kPa" } ?: "-"
}

private fun paint(color: Color): RemotePaint {
  return RemotePaint().apply { this.color = color.rc }
}

@Composable
private fun TpmsOverviewResponsive(
    tpms: VehicleTpmsData?,
    surfaceWidthPx: Int,
    surfaceHeightPx: Int,
    densityDpi: Int,
) {
  val density = densityDpi.coerceAtLeast(1) / 160f
  val surfaceWidthDp = surfaceWidthPx / density
  val surfaceHeightDp = surfaceHeightPx / density

  val overviewHeight = clampFloat(surfaceHeightDp * 0.48f, 300f, 430f)

  val margin = 24f
  val cardWidth = clampFloat(surfaceWidthDp * 0.24f, 150f, 230f)
  val cardHeight = 118f

  val leftX = margin
  val rightX = surfaceWidthDp - cardWidth - margin

  val topY = 20f
  val bottomY = overviewHeight - cardHeight - 20f

  RemoteBox(
      modifier =
          RemoteModifier.fillMaxWidth()
              .height(overviewHeight.rdp)
              .padding(top = 8.rdp, bottom = 8.rdp)
  ) {
    TpmsCarCanvasResponsive(surfaceWidthDp = surfaceWidthDp, overviewHeightDp = overviewHeight)

    TpmsInfoBlock(
        title = "FL",
        pressure = formatPressureKpa(tpms?.fl?.pressureKpa),
        widthDp = cardWidth,
        heightDp = cardHeight,
        modifier = RemoteModifier.padding(start = leftX.rdp, top = topY.rdp),
    )

    TpmsInfoBlock(
        title = "RL",
        pressure = formatPressureKpa(tpms?.rl?.pressureKpa),
        widthDp = cardWidth,
        heightDp = cardHeight,
        modifier = RemoteModifier.padding(start = leftX.rdp, top = bottomY.rdp),
    )

    TpmsInfoBlock(
        title = "FR",
        pressure = formatPressureKpa(tpms?.fr?.pressureKpa),
        widthDp = cardWidth,
        heightDp = cardHeight,
        modifier = RemoteModifier.padding(start = rightX.rdp, top = topY.rdp),
    )

    TpmsInfoBlock(
        title = "RR",
        pressure = formatPressureKpa(tpms?.rr?.pressureKpa),
        widthDp = cardWidth,
        heightDp = cardHeight,
        modifier = RemoteModifier.padding(start = rightX.rdp, top = bottomY.rdp),
    )
  }
}

@Composable
private fun TpmsInfoBlock(
    title: String,
    pressure: String,
    widthDp: Float,
    heightDp: Float,
    modifier: RemoteModifier,
) {
  RemoteBox(
      modifier =
          modifier
              .width(widthDp.rdp)
              .height(heightDp.rdp)
              .background(Color(0xFFD7E3F4))
              .padding(horizontal = 14.rdp, vertical = 10.rdp)
  ) {
    RemoteColumn {
      RemoteText(title)
      RemoteText("Pressure: $pressure")
      RemoteText("Temp: -")
      RemoteText("Status: -")
    }
  }
}

private fun clamp(value: Float, min: Float, max: Float): Float {
  return kotlin.math.max(min, kotlin.math.min(max, value))
}

private fun clampFloat(value: Float, lower: Float, upper: Float): Float {
  return kotlin.math.max(lower, kotlin.math.min(upper, value))
}

@Composable
private fun TpmsCarCanvasResponsive(surfaceWidthDp: Float, overviewHeightDp: Float) {
  val canvasWidth = surfaceWidthDp
  val canvasHeight = overviewHeightDp

  val cx = canvasWidth / 2f

  val carHeight = clampFloat(canvasHeight * 0.72f, 210f, 330f)
  val carWidth = clampFloat(carHeight * 0.56f, 120f, 190f)

  val carLeft = cx - carWidth / 2f
  val carTop = (canvasHeight - carHeight) / 2f

  val tireWidth = carWidth * 0.16f
  val tireHeight = carHeight * 0.28f
  val tireRadius = tireWidth * 0.45f
  val tireGap = carWidth * 0.10f

  val frontTireY = carTop + carHeight * 0.18f
  val rearTireY = carTop + carHeight * 0.58f

  RemoteCanvas(modifier = RemoteModifier.fillMaxWidth().height(canvasHeight.rdp)) {
    val tirePaint = paint(Color(0xFF020617))
    val bodyPaint = paint(Color(0xFF64748B))
    val hoodPaint = paint(Color(0xFF94A3B8))
    val glassPaint = paint(Color(0xFF334155))
    val cabinPaint = paint(Color(0xFF475569))

    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft - tireWidth - tireGap, frontTireY),
        size = RemoteSize(tireWidth.rf, tireHeight.rf),
        cornerRadius = RemoteOffset(tireRadius, tireRadius),
    )

    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft - tireWidth - tireGap, rearTireY),
        size = RemoteSize(tireWidth.rf, tireHeight.rf),
        cornerRadius = RemoteOffset(tireRadius, tireRadius),
    )

    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft + carWidth + tireGap, frontTireY),
        size = RemoteSize(tireWidth.rf, tireHeight.rf),
        cornerRadius = RemoteOffset(tireRadius, tireRadius),
    )

    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft + carWidth + tireGap, rearTireY),
        size = RemoteSize(tireWidth.rf, tireHeight.rf),
        cornerRadius = RemoteOffset(tireRadius, tireRadius),
    )

    drawRoundRect(
        paint = bodyPaint,
        topLeft = RemoteOffset(carLeft, carTop),
        size = RemoteSize(carWidth.rf, carHeight.rf),
        cornerRadius = RemoteOffset(carWidth * 0.34f, carWidth * 0.34f),
    )

    drawRoundRect(
        paint = hoodPaint,
        topLeft = RemoteOffset(cx - carWidth * 0.25f, carTop + carHeight * 0.09f),
        size = RemoteSize((carWidth * 0.50f).rf, (carHeight * 0.18f).rf),
        cornerRadius = RemoteOffset(18f, 18f),
    )

    drawRoundRect(
        paint = glassPaint,
        topLeft = RemoteOffset(cx - carWidth * 0.32f, carTop + carHeight * 0.34f),
        size = RemoteSize((carWidth * 0.64f).rf, (carHeight * 0.17f).rf),
        cornerRadius = RemoteOffset(18f, 18f),
    )

    drawRoundRect(
        paint = cabinPaint,
        topLeft = RemoteOffset(cx - carWidth * 0.34f, carTop + carHeight * 0.60f),
        size = RemoteSize((carWidth * 0.68f).rf, (carHeight * 0.20f).rf),
        cornerRadius = RemoteOffset(18f, 18f),
    )
  }
}

@Composable
private fun TpmsCarCanvas() {
  RemoteCanvas(
      modifier =
          RemoteModifier.fillMaxWidth().height(260.rdp).padding(top = 12.rdp, bottom = 12.rdp)
  ) {
    val w = size.width
    val cx = w / 2f

    val carWidth = 150f
    val carHeight = 220f
    val carLeft = cx - carWidth / 2f
    val carTop = 20f

    val tirePaint = paint(Color(0xFF020617))
    val bodyPaint = paint(Color(0xFF334155))
    val hoodPaint = paint(Color(0xFF475569))
    val glassPaint = paint(Color(0xFF111827))
    val cabinPaint = paint(Color(0xFF1E293B))

    // Left tires
    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft - 34f, carTop + 34f),
        size = RemoteSize(30f.rf, 64f.rf),
        cornerRadius = RemoteOffset(10f, 10f),
    )

    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft - 34f, carTop + 138f),
        size = RemoteSize(30f.rf, 64f.rf),
        cornerRadius = RemoteOffset(10f, 10f),
    )

    // Right tires
    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft + carWidth + 4f, carTop + 34f),
        size = RemoteSize(30f.rf, 64f.rf),
        cornerRadius = RemoteOffset(10f, 10f),
    )

    drawRoundRect(
        paint = tirePaint,
        topLeft = RemoteOffset(carLeft + carWidth + 4f, carTop + 138f),
        size = RemoteSize(30f.rf, 64f.rf),
        cornerRadius = RemoteOffset(10f, 10f),
    )

    // Body
    drawRoundRect(
        paint = bodyPaint,
        topLeft = RemoteOffset(carLeft, carTop),
        size = RemoteSize(carWidth.rf, carHeight.rf),
        cornerRadius = RemoteOffset(48f, 48f),
    )

    // Hood
    drawRoundRect(
        paint = hoodPaint,
        topLeft = RemoteOffset(cx - 40f, carTop + 20f),
        size = RemoteSize(80f.rf, 44f.rf),
        cornerRadius = RemoteOffset(20f, 20f),
    )

    // Front glass
    drawRoundRect(
        paint = glassPaint,
        topLeft = RemoteOffset(cx - 48f, carTop + 78f),
        size = RemoteSize(96f.rf, 42f.rf),
        cornerRadius = RemoteOffset(18f, 18f),
    )

    // Cabin
    drawRoundRect(
        paint = cabinPaint,
        topLeft = RemoteOffset(cx - 52f, carTop + 132f),
        size = RemoteSize(104f.rf, 52f.rf),
        cornerRadius = RemoteOffset(22f, 22f),
    )
  }
}
