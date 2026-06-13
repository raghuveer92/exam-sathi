package com.examsaathi.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    private static final String PRIMARY_COLOR = "#2563eb";

    public String buildVerificationOtpEmail(String fullName, String otp) {
        String greeting = fullName != null && !fullName.isBlank() ? fullName : "there";
        return wrapTemplate(
            "Verify Your ExamSaathi Account",
            """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#374151;">
                  Hi %s,
                </p>
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#374151;">
                  Thank you for registering with ExamSaathi. Your verification code is:
                </p>
                %s
                <p style="margin:24px 0 0;font-size:14px;line-height:1.6;color:#6b7280;">
                  This OTP expires in <strong>10 minutes</strong>. If you did not request this, please ignore this email.
                </p>
                """.formatted(greeting, otpBlock(otp))
        );
    }

    public String buildPasswordResetOtpEmail(String fullName, String otp) {
        String greeting = fullName != null && !fullName.isBlank() ? fullName : "there";
        return wrapTemplate(
            "Reset Your ExamSaathi Password",
            """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#374151;">
                  Hi %s,
                </p>
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#374151;">
                  Your password reset code is:
                </p>
                %s
                <p style="margin:24px 0 0;font-size:14px;line-height:1.6;color:#6b7280;">
                  This OTP expires in <strong>10 minutes</strong>. If you did not request this, please ignore this email.
                </p>
                """.formatted(greeting, otpBlock(otp))
        );
    }

    private String otpBlock(String otp) {
        return """
            <div style="margin:24px 0;text-align:center;">
              <div style="display:inline-block;padding:18px 32px;border-radius:12px;background:#f3f4f6;border:2px dashed %s;">
                <span style="font-size:32px;font-weight:700;letter-spacing:8px;color:#111827;">%s</span>
              </div>
            </div>
            """.formatted(PRIMARY_COLOR, otp);
    }

    private String wrapTemplate(String title, String bodyContent) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f3f4f6;padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                           style="max-width:560px;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.05);">
                      <tr>
                        <td style="background:%s;padding:28px 32px;text-align:center;">
                          <div style="width:56px;height:56px;margin:0 auto 12px;border-radius:12px;background:#ffffff;display:inline-block;line-height:56px;font-size:24px;font-weight:700;color:%s;">
                            ES
                          </div>
                          <h1 style="margin:0;font-size:22px;font-weight:700;color:#ffffff;">ExamSaathi</h1>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h2 style="margin:0 0 20px;font-size:20px;font-weight:600;color:#111827;">%s</h2>
                          %s
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:20px 32px 28px;border-top:1px solid #e5e7eb;text-align:center;">
                          <p style="margin:0;font-size:12px;line-height:1.5;color:#9ca3af;">
                            &copy; ExamSaathi. All rights reserved.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(title, PRIMARY_COLOR, PRIMARY_COLOR, title, bodyContent);
    }
}
