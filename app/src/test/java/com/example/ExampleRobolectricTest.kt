package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string and version from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Glassly", appName)
    assertEquals("1.0.7", BuildConfig.VERSION_NAME)
    assertEquals(111, BuildConfig.VERSION_CODE)
    assertEquals("com.glassly.app", BuildConfig.APPLICATION_ID)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF), com.example.ui.theme.LightGlassColors.background)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), com.example.ui.theme.DarkGlassColors.background)
  }
}
