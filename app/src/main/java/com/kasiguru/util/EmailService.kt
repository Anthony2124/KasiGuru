package com.kasiguru.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {

    // TODO: USER, YOU MUST REPLACE THESE TWO VARIABLES WITH YOUR ACTUAL GMAIL AND APP PASSWORD!
    private const val SENDER_EMAIL = "anthonycordial2124@gmail.com"
    private const val SENDER_APP_PASSWORD = "zxyj xpmj iwek felw"

    suspend fun sendOtpEmail(recipientEmail: String, otp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "KasiGuru App"))
                    addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                    subject = "Your KasiGuru Verification Code"
                    
                    val htmlContent = """
                        <div style="font-family: Arial, sans-serif; text-align: center; padding: 20px;">
                            <h2 style="color: #6C63FF;">Welcome to KasiGuru!</h2>
                            <p>Here is your 6-digit verification code to complete your registration:</p>
                            <h1 style="letter-spacing: 5px; color: #333333; background: #F3F4F6; padding: 10px; display: inline-block; border-radius: 8px;">$otp</h1>
                            <p>If you didn't request this, please ignore this email.</p>
                        </div>
                    """.trimIndent()

                    setContent(htmlContent, "text/html; charset=utf-8")
                }

                Transport.send(message)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
