package com.trodix.signature.exceptions;

import java.util.List;
import org.springframework.dao.DataAccessException;

public class DataResultException extends DataAccessException {

    public <T> DataResultException(String msg, List<T> result) {
        super(new StringBuilder().append(msg).append(Character.SPACE_SEPARATOR).append(result == null ? null : result.size()).toString());
    }
    
}
