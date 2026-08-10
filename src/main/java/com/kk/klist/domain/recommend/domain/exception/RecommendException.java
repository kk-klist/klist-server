package com.kk.klist.domain.recommend.domain.exception;

import com.kk.klist.global.exception.BusinessException;

public class RecommendException extends BusinessException {

    public RecommendException(RecommendErrorCode errorCode) {
        super(errorCode);
    }
}
