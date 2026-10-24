package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.model.FailedSignIns;
import com.kinetiq.account.domain.output.FailedSignInsRepository;
import com.kinetiq.common.domain.model.Email;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeFailedSignInsRepository implements FailedSignInsRepository {

    private final Map<Email, FailedSignIns> byEmail = new HashMap<>();

    @Override
    public Optional<FailedSignIns> find(Email email) {
        return Optional.ofNullable(byEmail.get(email));
    }

    @Override
    public void save(FailedSignIns failedSignIns) {
        byEmail.put(failedSignIns.getEmail(), failedSignIns);
    }

    @Override
    public void delete(Email email) {
        byEmail.remove(email);
    }
}
