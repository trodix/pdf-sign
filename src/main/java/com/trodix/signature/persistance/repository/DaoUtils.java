package com.trodix.signature.persistance.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.util.CollectionUtils;
import com.trodix.signature.exceptions.DataResultException;

public class DaoUtils {

    public static <T> Optional<T> findOne(final List<T> result) throws DataResultException {

        if (CollectionUtils.isEmpty(result)) {
            return Optional.ofNullable(null);
        } else if (result.size() == 1) {
            return Optional.of(result.get(0));
        } else {
            throw new DataResultException("Query must return 0 or 1 row", result);
        }
    }

}
