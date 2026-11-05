package com.example.toolsappkotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.toolsappkotlin.ui.theme.ToolsAppKotlinTheme
import android.content.Intent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class ActivitySelector : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activityselector)

        val AccelerometerButton = findViewById<Button>(R.id.btnOpenAccelerometer)
        val AccelerometerWithoutGravityButton =  findViewById<Button>(R.id.btnOpenAccelerometerWithoutGravity)
        val MagnetometerButton =  findViewById<Button>(R.id.btnOpenMagnetometer)
        val PressureSensorButton =  findViewById<Button>(R.id.btnOpenPressureSensor)


        // 2. Set the click listener
        AccelerometerButton.setOnClickListener {
            val intent = Intent(this, Accelerometer::class.java)
            startActivity(intent)
        }

        AccelerometerWithoutGravityButton.setOnClickListener {
            val intent = Intent(this, AccelerometerWithoutGravity::class.java)
            startActivity(intent)
        }

        MagnetometerButton.setOnClickListener {
            val intent = Intent(this, Magnetometer::class.java)
            startActivity(intent)
        }

        PressureSensorButton.setOnClickListener {
            val intent = Intent(this, PressureSensor::class.java)
            startActivity(intent)
        }

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToolsAppKotlinTheme {
        Greeting("Android")
    }
}
