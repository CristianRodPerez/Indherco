package com.indherco.postes.shared.integration;

public interface ReportGeneratorPort {

    byte[] generate(String reportName, Object data);
}
