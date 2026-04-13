package org.smm.archetype.client.dto;

import java.util.List;
import java.util.Map;

public record EmailRequest(
    List<String> to,
    String templateId,
    Map<String, String> templateParams,
    String subject
) {}
