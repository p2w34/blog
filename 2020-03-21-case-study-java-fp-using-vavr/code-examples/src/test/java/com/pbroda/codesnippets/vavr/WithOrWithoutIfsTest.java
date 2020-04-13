package com.pbroda.codesnippets.vavr;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

interface AllowedLabelsService {
    Either<Throwable, Boolean> isAllowed(String label);
}

@Value
class LabelFilter {
    private final Set<String> allowedLabels = HashSet.of("AB", "CD", "EF");
    private final AllowedLabelsService allowedLabelsService;

    public boolean filter(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        if (!allowedLabels.contains(label.toUpperCase())) {
            return false;
        }
        return true;
    }

    public boolean filter2(String label) {
        return Option.of(label)
                .map(String::toUpperCase)
                .exists(allowedLabels::contains);
    }

    public Either<Throwable, Boolean> filter3(String label) {
        return Option.of(label)
                .map(allowedLabelsService::isAllowed)
                .getOrElse(() -> Either.right(false));
    }
}

@ExtendWith(MockitoExtension.class)
public class WithOrWithoutIfsTest {

    private AllowedLabelsService allowedLabelsServiceImpl;
    private LabelFilter labelFilter;

    private LabelFilter createLabelFilterTestInstance(Either<Throwable, Boolean> allowedLabelsServiceResult) {
        allowedLabelsServiceImpl = mock(AllowedLabelsService.class);
        lenient().when(allowedLabelsServiceImpl.isAllowed(any())).thenReturn(allowedLabelsServiceResult);
        return new LabelFilter(allowedLabelsServiceImpl);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "GH"})
    void testShouldReturnFalseWhenLabelNotAllowed(String label) {
        labelFilter = createLabelFilterTestInstance(Either.right(false));
        assertFalse(labelFilter.filter(label));
        assertFalse(labelFilter.filter2(label));
        assertFalse(labelFilter.filter3(label).get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "CD", "EF", "ab"})
    void testShouldReturnTrueWhenLabelIsAllowed(String label) {
        labelFilter = createLabelFilterTestInstance(Either.right(true));
        assertTrue(labelFilter.filter(label));
        assertTrue(labelFilter.filter2(label));
        assertTrue(labelFilter.filter3(label).get());
    }

    @Test
    void testShouldReturnLeftWhenLabelInfoServiceReturnsLeft() {
        var expectedException = new Exception();
        labelFilter = createLabelFilterTestInstance(Either.left(expectedException));
        assertEquals(labelFilter.filter3("anyLabel").getLeft(), expectedException);
    }
}
