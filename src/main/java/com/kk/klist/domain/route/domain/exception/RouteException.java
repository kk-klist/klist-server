package com.kk.klist.domain.route.domain.exception;

import com.kk.klist.global.exception.BusinessException;

public class RouteException extends BusinessException {

    public RouteException(RouteErrorCode errorCode) {
        super(errorCode);
    }
}
