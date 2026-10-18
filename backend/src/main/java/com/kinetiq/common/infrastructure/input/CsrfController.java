package com.kinetiq.common.infrastructure.input;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sets the {@code XSRF-TOKEN} cookie for a fresh page that has made no backend call yet. */
@RestController
class CsrfController {

    @GetMapping("/api/v1/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf() {}
}
