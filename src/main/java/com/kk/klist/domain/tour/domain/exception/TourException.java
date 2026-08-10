package com.kk.klist.domain.tour.domain.exception;

import com.kk.klist.global.exception.BusinessException;

public class TourException extends BusinessException {

    public TourException(TourErrorCode errorCode) {
        super(errorCode);
    }
}
