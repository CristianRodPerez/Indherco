package com.indherco.postes.shared.integration;

public interface EmailSenderPort {

    void send(String to, String subject, String body);
}
