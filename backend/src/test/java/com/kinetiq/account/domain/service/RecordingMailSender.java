package com.kinetiq.account.domain.service;

import com.kinetiq.common.domain.output.Mail;
import com.kinetiq.common.domain.output.MailSender;
import java.util.ArrayList;
import java.util.List;

class RecordingMailSender implements MailSender {

    final List<Mail> sent = new ArrayList<>();

    @Override
    public void send(Mail mail) {
        sent.add(mail);
    }
}
