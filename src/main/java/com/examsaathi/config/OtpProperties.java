package com.examsaathi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dev/test OTP settings. When {@code use-fixed} is true, registration and
 * password-reset flows always use {@code fixed-value} (default 999999).
 */
@Component
@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {

    /** When true, {@link com.examsaathi.service.OtpService} uses a fixed OTP. */
    private boolean useFixed = false;

    /** OTP returned by generateOtp and accepted by validateOtp in fixed mode. */
    private String fixedValue = "999999";

    public boolean isUseFixed() {
        return useFixed;
    }

    public void setUseFixed(boolean useFixed) {
        this.useFixed = useFixed;
    }

    public String getFixedValue() {
        return fixedValue;
    }

    public void setFixedValue(String fixedValue) {
        this.fixedValue = fixedValue;
    }
}
