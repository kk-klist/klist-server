package com.kk.klist.domain.poi.domain.exception;

import com.kk.klist.global.exception.BusinessException;

public class PoiException extends BusinessException {

    public PoiException(PoiErrorCode errorCode) {
        super(errorCode);
    }
}
