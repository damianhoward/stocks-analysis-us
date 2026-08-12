package com.damianhoward.stocks.exception;

public class DataRetrievalError extends Exception {
    public DataRetrievalError(Exception e) {
        super(e);
    }

    public DataRetrievalError(String message) {
        super(message);
    }
}
