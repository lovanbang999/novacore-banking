package com.novacore.banking.shared.i18n;

import org.springframework.stereotype.Component;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@Component
public class MessageHelper {
    private static MessageSource messageSource;

    // Spring will be injected at runtime
    public MessageHelper(MessageSource messageSource) {
        MessageHelper.messageSource = messageSource;
    }

    public static String get(String code, Object... args) {
        if (messageSource == null) {
            return code;
        }

        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return code; // fallback to code
        }
    }
}
