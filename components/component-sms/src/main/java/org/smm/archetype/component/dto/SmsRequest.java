package org.smm.archetype.component.dto;

import java.util.Map;

public record SmsRequest(
    String phoneNumber,
    String templateId,
    Map<String, String> templateParams
) {}
