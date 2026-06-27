package com.omnixys.storage;

import com.omnixys.commons.error.BaseOmnixysException;
import com.omnixys.commons.error.DefaultExceptionContext;
import com.omnixys.commons.error.ErrorCode;
import com.omnixys.commons.error.ExceptionContext;

import java.util.Map;

public class StorageException extends BaseOmnixysException {

    private static ExceptionContext withMetadata(String operation) {
        return new DefaultExceptionContext(null, null, null, null, null,
                Map.of("operation", operation));
    }

    public StorageException(String operation, String message) {
        super(ErrorCode.STORAGE_ERROR, message, withMetadata(operation));
    }

    public StorageException(String operation, String message, Throwable cause) {
        super(ErrorCode.STORAGE_ERROR, message, cause, withMetadata(operation));
    }

    public String getOperation() {
        Object op = getMetadata().get("operation");
        return op instanceof String s ? s : null;
    }
}
