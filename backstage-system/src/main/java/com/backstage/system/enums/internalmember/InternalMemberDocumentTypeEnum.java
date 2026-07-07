package com.backstage.system.enums.internalmember;

import com.backstage.common.exception.ServiceException;

public enum InternalMemberDocumentTypeEnum {

    RESUME("resume", "resume"),
    OFFER("offer", "offer");

    private final String code;
    private final String pathSegment;

    InternalMemberDocumentTypeEnum(String code, String pathSegment) {
        this.code = code;
        this.pathSegment = pathSegment;
    }

    public String getCode() {
        return code;
    }

    public String getPathSegment() {
        return pathSegment;
    }

    public static InternalMemberDocumentTypeEnum fromCode(String code) {
        if (code == null) {
            throw new ServiceException("Document type cannot be empty");
        }
        for (InternalMemberDocumentTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("Unsupported document type");
    }
}
