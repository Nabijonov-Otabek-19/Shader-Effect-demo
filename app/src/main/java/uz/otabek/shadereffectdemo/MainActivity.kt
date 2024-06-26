package uz.otabek.shadereffectdemo

import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.otabek.shadereffectdemo.ui.theme.ShaderEffectDemoTheme

private const val IMG_SHADER_SRC = """
    uniform float2 size;
    uniform float time;
    uniform shader composable;
    
    half4 main(float2 fragCoord) {
        float scale = 1 / size.x;
        float2 scaledCoord = fragCoord * scale;
        float2 center = size * 0.5 * scale;
        float dist = distance(scaledCoord, center);
        float2 dir = scaledCoord - center;
        float sin = sin(dist * 70 - time * 6.28);
        float2 offset = dir * sin;
        float2 textCoord = scaledCoord + offset / 30;
        return composable.eval(textCoord / scale);
    }
"""

private const val FRACTAL_SHADER_SRC = """
    uniform float2 size;
    uniform float time;
    uniform shader composable;
    
    float f(float3 p) {
        p.z -= time * 5.;
        float a = p.z * .1;
        p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
        return .1 - length(cos(p.xy) + sin(p.yz));
    }
    
    half4 main(float2 fragcoord) { 
        float3 d = .5 - fragcoord.xy1 / size.y;
        float3 p=float3(0);
        for (int i = 0; i < 32; i++) {
          p += f(p) * d;
        }
        return ((sin(p) + float3(2, 5, 12)) / length(p)).xyz1;
    }
"""

private const val ROUND_SHADER_SRC = """
    uniform float2 size;
    uniform float time;
    uniform shader composable;
    
    float4 main(float2 FC) {
      float4 o = float4(0);
      float2 p = float2(0), c = p, u = FC.xy * 2. - size.xy;
      float a;
      for (float i = 0; i < 4e2; i++) {
        a = i / 2e2 - 1.;
        p = cos(i * 2.4 + time + float2(0, 11)) * sqrt(1. - a * a);
        c = u / size.y + float2(p.x, a) / (p.y + 2.);
        o += (cos(i + float4(0, 2, 4, 0)) + 1.) / dot(c, c) * (1. - p.y) / 3e4;
      }
      return o;
    }
"""

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val shader = RuntimeShader(IMG_SHADER_SRC)
        //val shader = RuntimeShader(FRACTAL_SHADER_SRC)
        //val shader = RuntimeShader(ROUND_SHADER_SRC)
        val photo = BitmapFactory.decodeResource(resources, R.drawable.car)

        setContent {
            val scope = rememberCoroutineScope()
            val timeMs = remember { mutableFloatStateOf(0f) }
            LaunchedEffect(Unit) {
                scope.launch {
                    while (true) {
                        timeMs.floatValue = (System.currentTimeMillis() % 100_000L) / 1_000f
                        delay(10)
                    }
                }
            }

            ShaderEffectDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = Color.Black
                ) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        modifier = Modifier
                            .onSizeChanged { size ->
                                shader.setFloatUniform(
                                    "size",
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                            }
                            .graphicsLayer {
                                clip = true
                                shader.setFloatUniform("time", timeMs.floatValue)
                                renderEffect =
                                    RenderEffect
                                        .createRuntimeShaderEffect(shader, "composable")
                                        .asComposeRenderEffect()
                            },
                        contentScale = ContentScale.FillHeight,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
