# Daadi Game - Beautiful Supabase Email Templates

These templates have been carefully designed to match the **Daadi** app's warm, traditional board game aesthetic:
- **Primary Color:** `#C75D27` (Warm Terracotta / Burnt Orange)
- **Secondary Color:** `#5C2D0A` (Rich Dark Chocolate Brown)
- **Gold Accent:** `#E5A93B` (Golden Ochre)
- **Background Vibe:** `#FDF3E3` (Soft Peach/Ivory Linen)

---

## 1. Confirm Sign Up (Confirm Email)

**Location in Supabase Console:** `Authentication` -> `Email Templates` -> `Confirm signup`

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Confirm Your Daadi Account</title>
</head>
<body style="margin: 0; padding: 0; background-color: #FDF3E3; font-family: 'Georgia', 'Times New Roman', Times, serif; -webkit-font-smoothing: antialiased;">
  <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #FDF3E3; padding: 40px 10px;">
    <tr>
      <td align="center">
        <!-- Main Card Container -->
        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #FFFDF8; border: 2px solid #E5A93B; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(92, 45, 10, 0.08);">
          
          <!-- Header Banner -->
          <tr>
            <td align="center" style="background-color: #5C2D0A; padding: 30px 20px; border-bottom: 3px solid #E5A93B;">
              <h1 style="margin: 0; color: #FFFDF8; font-size: 28px; font-weight: bold; letter-spacing: 1px;">DAADI MULTIPLAYER</h1>
              <p style="margin: 5px 0 0 0; color: #E5A93B; font-size: 13px; font-style: italic; font-family: 'Arial', sans-serif; letter-spacing: 2px; text-transform: uppercase;">Arena of Strategy & Mind</p>
            </td>
          </tr>

          <!-- Main Content -->
          <tr>
            <td style="padding: 40px 30px; align-items: center;">
              <!-- Decorative Icon Placeholder -->
              <div style="text-align: center; margin-bottom: 25px;">
                <span style="font-size: 48px; line-height: 1;">⚔️</span>
              </div>
              
              <h2 style="margin: 0 0 15px 0; color: #5C2D0A; font-size: 22px; text-align: center; font-weight: bold;">Verify Your Player Profile</h2>
              
              <p style="margin: 0 0 20px 0; color: #4A4A4A; font-size: 15px; line-height: 1.6; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                Welcome to the Daadi Pro Multiplayer Arena! To complete your registration and begin competing in ranked matches under India's DPDP Act privacy protections, please confirm your email address.
              </p>

              <!-- Call to Action Button -->
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="margin: 30px 0;">
                <tr>
                  <td align="center">
                    <a href="{{ .ConfirmationURL }}" target="_blank" style="display: inline-block; background-color: #C75D27; color: #FFFDF8; font-family: 'Arial', sans-serif; font-size: 16px; font-weight: bold; text-decoration: none; padding: 14px 36px; border-radius: 12px; border-bottom: 3px solid #8F3B12; transition: background 0.2s;">
                      Confirm Email Address
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 0 0 15px 0; color: #7F8C8D; font-size: 13px; line-height: 1.5; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                This link is secure and will redirect you straight into your Daadi mobile application.
              </p>
            </td>
          </tr>

          <!-- Divider -->
          <tr>
            <td style="padding: 0 30px;">
              <hr style="border: 0; border-top: 1px solid #E5A93B; opacity: 0.3; margin: 0;">
            </td>
          </tr>

          <!-- Footer Info -->
          <tr>
            <td style="padding: 30px; background-color: #FFFDF8; text-align: center; font-family: 'Arial', sans-serif;">
              <p style="margin: 0 0 8px 0; color: #8B5E3C; font-size: 12px; font-weight: bold;">
                Daadi Pro Board Games Inc.
              </p>
              <p style="margin: 0; color: #95A5A6; font-size: 11px; line-height: 1.4;">
                If you did not sign up for this account, you can safely ignore this email.<br>
                Protected under Digital Personal Data Protection (DPDP) Act, 2023.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

---

## 2. Invite User

**Location in Supabase Console:** `Authentication` -> `Email Templates` -> `Invite user`

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>You've Been Invited to Daadi</title>
</head>
<body style="margin: 0; padding: 0; background-color: #FDF3E3; font-family: 'Georgia', 'Times New Roman', Times, serif; -webkit-font-smoothing: antialiased;">
  <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #FDF3E3; padding: 40px 10px;">
    <tr>
      <td align="center">
        <!-- Main Card Container -->
        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #FFFDF8; border: 2px solid #E5A93B; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(92, 45, 10, 0.08);">
          
          <!-- Header Banner -->
          <tr>
            <td align="center" style="background-color: #5C2D0A; padding: 30px 20px; border-bottom: 3px solid #E5A93B;">
              <h1 style="margin: 0; color: #FFFDF8; font-size: 28px; font-weight: bold; letter-spacing: 1px;">DAADI MULTIPLAYER</h1>
              <p style="margin: 5px 0 0 0; color: #E5A93B; font-size: 13px; font-style: italic; font-family: 'Arial', sans-serif; letter-spacing: 2px; text-transform: uppercase;">Arena of Strategy & Mind</p>
            </td>
          </tr>

          <!-- Main Content -->
          <tr>
            <td style="padding: 40px 30px; align-items: center;">
              <!-- Decorative Icon Placeholder -->
              <div style="text-align: center; margin-bottom: 25px;">
                <span style="font-size: 48px; line-height: 1;">✉️</span>
              </div>
              
              <h2 style="margin: 0 0 15px 0; color: #5C2D0A; font-size: 22px; text-align: center; font-weight: bold;">You've Been Invited!</h2>
              
              <p style="margin: 0 0 20px 0; color: #4A4A4A; font-size: 15px; line-height: 1.6; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                You have been invited to join the <strong>Daadi Multiplayer Arena</strong>! Accept the invitation below to create your official player profile, customize your screen name, and start tracking your matchmaking performance.
              </p>

              <!-- Call to Action Button -->
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="margin: 30px 0;">
                <tr>
                  <td align="center">
                    <a href="{{ .ConfirmationURL }}" target="_blank" style="display: inline-block; background-color: #C75D27; color: #FFFDF8; font-family: 'Arial', sans-serif; font-size: 16px; font-weight: bold; text-decoration: none; padding: 14px 36px; border-radius: 12px; border-bottom: 3px solid #8F3B12; transition: background 0.2s;">
                      Accept Invitation
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 0 0 15px 0; color: #7F8C8D; font-size: 13px; line-height: 1.5; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                Clicking the button will open the Daadi mobile application and guide you through your profile setup.
              </p>
            </td>
          </tr>

          <!-- Divider -->
          <tr>
            <td style="padding: 0 30px;">
              <hr style="border: 0; border-top: 1px solid #E5A93B; opacity: 0.3; margin: 0;">
            </td>
          </tr>

          <!-- Footer Info -->
          <tr>
            <td style="padding: 30px; background-color: #FFFDF8; text-align: center; font-family: 'Arial', sans-serif;">
              <p style="margin: 0 0 8px 0; color: #8B5E3C; font-size: 12px; font-weight: bold;">
                Daadi Pro Board Games Inc.
              </p>
              <p style="margin: 0; color: #95A5A6; font-size: 11px; line-height: 1.4;">
                If you did not expect this invite, you can safely ignore this email.<br>
                Protected under Digital Personal Data Protection (DPDP) Act, 2023.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

---

## 3. Reset Password (Recovery Link)

**Location in Supabase Console:** `Authentication` -> `Email Templates` -> `Reset password`

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Reset Your Daadi Password</title>
</head>
<body style="margin: 0; padding: 0; background-color: #FDF3E3; font-family: 'Georgia', 'Times New Roman', Times, serif; -webkit-font-smoothing: antialiased;">
  <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #FDF3E3; padding: 40px 10px;">
    <tr>
      <td align="center">
        <!-- Main Card Container -->
        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #FFFDF8; border: 2px solid #E5A93B; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(92, 45, 10, 0.08);">
          
          <!-- Header Banner -->
          <tr>
            <td align="center" style="background-color: #5C2D0A; padding: 30px 20px; border-bottom: 3px solid #E5A93B;">
              <h1 style="margin: 0; color: #FFFDF8; font-size: 28px; font-weight: bold; letter-spacing: 1px;">DAADI MULTIPLAYER</h1>
              <p style="margin: 5px 0 0 0; color: #E5A93B; font-size: 13px; font-style: italic; font-family: 'Arial', sans-serif; letter-spacing: 2px; text-transform: uppercase;">Arena of Strategy & Mind</p>
            </td>
          </tr>

          <!-- Main Content -->
          <tr>
            <td style="padding: 40px 30px; align-items: center;">
              <!-- Decorative Icon Placeholder -->
              <div style="text-align: center; margin-bottom: 25px;">
                <span style="font-size: 48px; line-height: 1;">🔑</span>
              </div>
              
              <h2 style="margin: 0 0 15px 0; color: #5C2D0A; font-size: 22px; text-align: center; font-weight: bold;">Reset Your Password</h2>
              
              <p style="margin: 0 0 20px 0; color: #4A4A4A; font-size: 15px; line-height: 1.6; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                We received a request to reset the password associated with your Daadi multiplayer account. Tap the link below to securely set a new password.
              </p>

              <!-- Call to Action Button -->
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="margin: 30px 0;">
                <tr>
                  <td align="center">
                    <a href="{{ .ConfirmationURL }}" target="_blank" style="display: inline-block; background-color: #C75D27; color: #FFFDF8; font-family: 'Arial', sans-serif; font-size: 16px; font-weight: bold; text-decoration: none; padding: 14px 36px; border-radius: 12px; border-bottom: 3px solid #8F3B12; transition: background 0.2s;">
                      Reset Password
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 0 0 15px 0; color: #7F8C8D; font-size: 13px; line-height: 1.5; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                This secure single-use link will redirect you directly back into the Daadi mobile application where you can instantly update your credentials.
              </p>
            </td>
          </tr>

          <!-- Divider -->
          <tr>
            <td style="padding: 0 30px;">
              <hr style="border: 0; border-top: 1px solid #E5A93B; opacity: 0.3; margin: 0;">
            </td>
          </tr>

          <!-- Footer Info -->
          <tr>
            <td style="padding: 30px; background-color: #FFFDF8; text-align: center; font-family: 'Arial', sans-serif;">
              <p style="margin: 0 0 8px 0; color: #8B5E3C; font-size: 12px; font-weight: bold;">
                Daadi Pro Board Games Inc.
              </p>
              <p style="margin: 0; color: #95A5A6; font-size: 11px; line-height: 1.4;">
                If you did not request a password change, you can safely ignore this email. Your credentials remain completely secure.<br>
                Protected under Digital Personal Data Protection (DPDP) Act, 2023.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

---

## 4. Magic Link or OTP

**Location in Supabase Console:** `Authentication` -> `Email Templates` -> `Magic link`

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Your Daadi Magic Sign-In Link</title>
</head>
<body style="margin: 0; padding: 0; background-color: #FDF3E3; font-family: 'Georgia', 'Times New Roman', Times, serif; -webkit-font-smoothing: antialiased;">
  <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #FDF3E3; padding: 40px 10px;">
    <tr>
      <td align="center">
        <!-- Main Card Container -->
        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #FFFDF8; border: 2px solid #E5A93B; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(92, 45, 10, 0.08);">
          
          <!-- Header Banner -->
          <tr>
            <td align="center" style="background-color: #5C2D0A; padding: 30px 20px; border-bottom: 3px solid #E5A93B;">
              <h1 style="margin: 0; color: #FFFDF8; font-size: 28px; font-weight: bold; letter-spacing: 1px;">DAADI MULTIPLAYER</h1>
              <p style="margin: 5px 0 0 0; color: #E5A93B; font-size: 13px; font-style: italic; font-family: 'Arial', sans-serif; letter-spacing: 2px; text-transform: uppercase;">Arena of Strategy & Mind</p>
            </td>
          </tr>

          <!-- Main Content -->
          <tr>
            <td style="padding: 40px 30px; align-items: center;">
              <!-- Decorative Icon Placeholder -->
              <div style="text-align: center; margin-bottom: 25px;">
                <span style="font-size: 48px; line-height: 1;">✨</span>
              </div>
              
              <h2 style="margin: 0 0 15px 0; color: #5C2D0A; font-size: 22px; text-align: center; font-weight: bold;">Your Magic Sign-In Link</h2>
              
              <p style="margin: 0 0 20px 0; color: #4A4A4A; font-size: 15px; line-height: 1.6; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                Tap the secure magic link below to instantly log in to your Daadi game profile without needing to input a password.
              </p>

              <!-- Call to Action Button -->
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="margin: 30px 0;">
                <tr>
                  <td align="center">
                    <a href="{{ .ConfirmationURL }}" target="_blank" style="display: inline-block; background-color: #C75D27; color: #FFFDF8; font-family: 'Arial', sans-serif; font-size: 16px; font-weight: bold; text-decoration: none; padding: 14px 36px; border-radius: 12px; border-bottom: 3px solid #8F3B12; transition: background 0.2s;">
                      Log In to Daadi
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 0 0 15px 0; color: #7F8C8D; font-size: 13px; line-height: 1.5; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                This single-use magic link is active for 15 minutes and will securely redirect you into your mobile app.
              </p>
            </td>
          </tr>

          <!-- Divider -->
          <tr>
            <td style="padding: 0 30px;">
              <hr style="border: 0; border-top: 1px solid #E5A93B; opacity: 0.3; margin: 0;">
            </td>
          </tr>

          <!-- Footer Info -->
          <tr>
            <td style="padding: 30px; background-color: #FFFDF8; text-align: center; font-family: 'Arial', sans-serif;">
              <p style="margin: 0 0 8px 0; color: #8B5E3C; font-size: 12px; font-weight: bold;">
                Daadi Pro Board Games Inc.
              </p>
              <p style="margin: 0; color: #95A5A6; font-size: 11px; line-height: 1.4;">
                If you didn't request a magic link, you can safely disregard this email.<br>
                Protected under Digital Personal Data Protection (DPDP) Act, 2023.
              </p>
            </td>
          </tr>

         </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

---

## 5. Change Email Address (Verification Link)

**Location in Supabase Console:** `Authentication` -> `Email Templates` -> `Change email address`

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Confirm Your New Email Address</title>
</head>
<body style="margin: 0; padding: 0; background-color: #FDF3E3; font-family: 'Georgia', 'Times New Roman', Times, serif; -webkit-font-smoothing: antialiased;">
  <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #FDF3E3; padding: 40px 10px;">
    <tr>
      <td align="center">
        <!-- Main Card Container -->
        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #FFFDF8; border: 2px solid #E5A93B; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(92, 45, 10, 0.08);">
          
          <!-- Header Banner -->
          <tr>
            <td align="center" style="background-color: #5C2D0A; padding: 30px 20px; border-bottom: 3px solid #E5A93B;">
              <h1 style="margin: 0; color: #FFFDF8; font-size: 28px; font-weight: bold; letter-spacing: 1px;">DAADI MULTIPLAYER</h1>
              <p style="margin: 5px 0 0 0; color: #E5A93B; font-size: 13px; font-style: italic; font-family: 'Arial', sans-serif; letter-spacing: 2px; text-transform: uppercase;">Arena of Strategy & Mind</p>
            </td>
          </tr>

          <!-- Main Content -->
          <tr>
            <td style="padding: 40px 30px; align-items: center;">
              <!-- Decorative Icon Placeholder -->
              <div style="text-align: center; margin-bottom: 25px;">
                <span style="font-size: 48px; line-height: 1;">📧</span>
              </div>
              
              <h2 style="margin: 0 0 15px 0; color: #5C2D0A; font-size: 22px; text-align: center; font-weight: bold;">Confirm New Email Address</h2>
              
              <p style="margin: 0 0 20px 0; color: #4A4A4A; font-size: 15px; line-height: 1.6; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                We received a request to change the email address associated with your Daadi multiplayer account. Please verify this new email address to complete the update.
              </p>

              <!-- Call to Action Button -->
              <table border="0" cellpadding="0" cellspacing="0" width="100%" style="margin: 30px 0;">
                <tr>
                  <td align="center">
                    <a href="{{ .ConfirmationURL }}" target="_blank" style="display: inline-block; background-color: #C75D27; color: #FFFDF8; font-family: 'Arial', sans-serif; font-size: 16px; font-weight: bold; text-decoration: none; padding: 14px 36px; border-radius: 12px; border-bottom: 3px solid #8F3B12; transition: background 0.2s;">
                      Confirm New Email
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin: 0 0 15px 0; color: #7F8C8D; font-size: 13px; line-height: 1.5; text-align: center; font-family: 'Arial', 'Helvetica', sans-serif;">
                Clicking the button will instantly verify your new address and securely return you to the Daadi mobile application.
              </p>
            </td>
          </tr>

          <!-- Divider -->
          <tr>
            <td style="padding: 0 30px;">
              <hr style="border: 0; border-top: 1px solid #E5A93B; opacity: 0.3; margin: 0;">
            </td>
          </tr>

          <!-- Footer Info -->
          <tr>
            <td style="padding: 30px; background-color: #FFFDF8; text-align: center; font-family: 'Arial', sans-serif;">
              <p style="margin: 0 0 8px 0; color: #8B5E3C; font-size: 12px; font-weight: bold;">
                Daadi Pro Board Games Inc.
              </p>
              <p style="margin: 0; color: #95A5A6; font-size: 11px; line-height: 1.4;">
                If you did not request this change, you can safely ignore this email. Your current address will remain unchanged.<br>
                Protected under Digital Personal Data Protection (DPDP) Act, 2023.
              </p>
            </td>
          </tr>

         </table>
      </td>
    </tr>
  </table>
</body>
</html>
```
