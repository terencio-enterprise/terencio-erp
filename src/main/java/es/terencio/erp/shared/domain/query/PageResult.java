package es.terencio.erp.shared.domain.query;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A shared domain abstraction for paginated results.
 * This prevents Spring Data's Page<T> from leaking into the Domain and
 * Application layers.
 */
public record PageResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int pageNumber,
        int pageSize) {
    /**
     * Maps the content of the PageResult to a different type.
     * Extremely useful for mapping Domain objects to DTOs/Responses in the
     * controllers.
     */
    public <U> PageResult<U> map(Function<? super T, ? extends U> converter) {
        return new PageResult<>(
                content.stream().map(converter).collect(Collectors.toList()),
                totalElements,
                totalPages,
                pageNumber,
                pageSize);
    }
}