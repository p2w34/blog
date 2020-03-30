package com.pbroda.codesnippets.vavr;

import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vavr.collection.Stream;

import lombok.Value;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.vavr.API.Some;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Value
class ProcessingResult {
    private final int id;
    private final boolean successful;
}

class AnyService {
    public ProcessingResult serviceCall(ProcessingResult pr) {
        return pr;
    }
}

interface AnyInterface {
    Either<Throwable, ProcessingResult> serviceCall();
}

@ExtendWith(MockitoExtension.class)
public class LoopBasics {

    Stream<ProcessingResult> testStream;
    List<ProcessingResult> testList;

    ProcessingResult pr1 = new ProcessingResult(1, true);
    ProcessingResult pr2 = new ProcessingResult(2, true);
    ProcessingResult pr3 = new ProcessingResult(3, false);
    ProcessingResult pr4 = new ProcessingResult(4, false);
    ProcessingResult pr5 = new ProcessingResult(5, true);
    ProcessingResult pr6 = new ProcessingResult(6, true);

    AnyService anyServiceStream;
    AnyService anyServiceList;

    @BeforeEach
    void init() {
        testStream = Stream.of(pr1, pr2, pr3, pr4, pr5, pr6);
        testList = List.of(pr1, pr2, pr3, pr4, pr5, pr6);

        anyServiceStream = spy(AnyService.class);
        anyServiceList = spy(AnyService.class);
    }

    @Test
    void testTakeWhile() {
        var resultStream = testStream
                .map(anyServiceStream::serviceCall)
                .takeWhile(processingResult -> processingResult.isSuccessful());

        var resultList = testList
                .map(anyServiceList::serviceCall)
                .takeWhile(processingResult -> processingResult.isSuccessful());

        verify(anyServiceStream, times(1)).serviceCall(any()); // result of implementation Stream as lazy linked list
        verify(anyServiceList, times(6)).serviceCall(any());

        assertEquals(List.of(pr1, pr2), resultStream);
        assertEquals(List.of(pr1, pr2), resultList);
    }

    @Test
    void testDropWhile() {
        var resultStream = testStream
                .map(anyServiceStream::serviceCall)
                .dropWhile(processingResult -> processingResult.isSuccessful());

        var resultList = testList
                .map(anyServiceList::serviceCall)
                .dropWhile(processingResult -> processingResult.isSuccessful());

        verify(anyServiceStream, times(3)).serviceCall(any()); // result of implementation Stream as lazy linked list
        verify(anyServiceList, times(6)).serviceCall(any());

        assertEquals(List.of(pr3, pr4, pr5, pr6), resultStream);
        assertEquals(List.of(pr3, pr4, pr5, pr6), resultList);
    }

    @Test
    void testFind() {
        var resultStream = testStream
                .map(anyServiceStream::serviceCall) // it returns false for the first three calls, true for the rest
                .find(processingResult -> !processingResult.isSuccessful());

        var resultList = testList
                .map(anyServiceList::serviceCall) // it returns false for the first three calls, true for the rest
                .find(processingResult -> !processingResult.isSuccessful());

        verify(anyServiceStream, times(3)).serviceCall(any()); // result of implementation Stream as lazy linked list
        verify(anyServiceList, times(6)).serviceCall(any());

        assertEquals(Some(pr3), resultStream);
        assertEquals(Some(pr3), resultList);
    }

    @Test
    void testContinually() {
        AnyInterface anyService = mock(AnyInterface.class);
        when(anyService.serviceCall())
                .thenReturn(Either.right(pr1))
                .thenReturn(Either.right(pr2))
                .thenReturn(Either.right(pr3));

        Stream.continually(() -> anyService.serviceCall())
                .find(result -> result.isRight() && !result.get().isSuccessful());

        verify(anyService, times(3)).serviceCall();
    }
}
