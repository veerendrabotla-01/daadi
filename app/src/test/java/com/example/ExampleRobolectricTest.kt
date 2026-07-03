package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.daadi.data.supabase.SupabaseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Daadi", appName)
  }

  @Test
  fun `test password reset deep link handling`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val supabaseManager = SupabaseManager(context)
    
    // Verify initial state is false
    assertFalse(supabaseManager.passwordResetRequired.value)

    // Simulate recovery deep link uri
    val recoveryUri = Uri.parse("daadi://auth-callback#access_token=test_token&type=recovery")
    
    supabaseManager.handleAuthDeepLink(recoveryUri) { _, _ -> }

    // Verify state changed to true
    assertTrue(supabaseManager.passwordResetRequired.value)

    // Clear and verify back to false
    supabaseManager.clearPasswordResetRequired()
    assertFalse(supabaseManager.passwordResetRequired.value)
  }
}
