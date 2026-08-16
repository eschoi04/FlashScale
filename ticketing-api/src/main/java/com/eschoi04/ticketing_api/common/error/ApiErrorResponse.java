package com.eschoi04.ticketing_api.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    Map<String, String> fieldErrors) {}
