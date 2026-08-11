package com.indherco.postes.shared.integration;

public interface ExternalStoragePort {

    String store(String path, byte[] content);
}
