package com.kinetiq.account.domain.output;

import com.kinetiq.account.domain.model.FailedSignIns;
import com.kinetiq.common.domain.model.Email;
import java.util.Optional;

public interface FailedSignInsRepository {

    Optional<FailedSignIns> find(Email email);

    void save(FailedSignIns failedSignIns);

    void delete(Email email);
}
