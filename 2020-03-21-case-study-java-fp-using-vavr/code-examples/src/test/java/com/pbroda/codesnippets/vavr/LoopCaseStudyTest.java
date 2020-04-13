package com.pbroda.codesnippets.vavr;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Either;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Value
class Item {
    UUID id;
}

@Accessors(fluent = true)
@Value
@Builder
class AvailableItemsResponse {

    private final List<Item> items;
    private final Integer totalResults;
    private final Integer count;
    private final Boolean hasMore;
    private final Integer limit;
    private final Integer offset;
}

interface WarehouseService {
    Either<Throwable, AvailableItemsResponse> fetchAvailableItems();
}

interface InventoryService {
    Either<Throwable, Boolean> publishAvailableItems(List<Item> items);
}

@Value
class ProcessingPipelineBeforeRefactoring {

    private final WarehouseService warehouseService;
    private final InventoryService inventoryService;

    public Either<Throwable, Boolean> processItems() {
        AvailableItemsResponse availableItemsResponse;
        do {

            var fetchResult = warehouseService.fetchAvailableItems();

            if (fetchResult.isLeft()) {
                return Either.left(fetchResult.getLeft());
            }

            availableItemsResponse = fetchResult.get();

            var publishResult = inventoryService.publishAvailableItems(availableItemsResponse.items());
            if (publishResult.isLeft()) {
                return Either.left(publishResult.getLeft());
            }

        } while (availableItemsResponse.hasMore());
        return Either.right(true);
    }
}

@Value
class ProcessingPipeline {

    private final WarehouseService warehouseService;
    private final InventoryService inventoryService;

    public Either<Throwable, Boolean> processItems() {
        return Stream.continually(() -> fetchAndPublishAvailableItems())
                .find(this::isLastPage)
                .get()
                .map(ignore -> true);
    }

    private Either<Throwable, Tuple2<Boolean, Boolean>> fetchAndPublishAvailableItems() {
        return warehouseService.fetchAvailableItems()
                .flatMap(this::publishAvailableItems);
    }

    private Either<Throwable, Tuple2<Boolean, Boolean>> publishAvailableItems(AvailableItemsResponse itemsResponse) {
        return inventoryService.publishAvailableItems(itemsResponse.items())
                .map(published -> Tuple.of(published, itemsResponse.hasMore()));
    }

    private Boolean isLastPage(Either<Throwable, Tuple2<Boolean, Boolean>> fetchAndPublishResult) {
        return fetchAndPublishResult.isLeft() ||
                (fetchAndPublishResult.isRight() && !fetchAndPublishResult.get()._2.booleanValue());
    }
}

@ExtendWith(MockitoExtension.class)
public class LoopCaseStudyTest {

    private Either<Throwable, AvailableItemsResponse> air1 = Either.right(AvailableItemsResponse.builder().hasMore(true).build());
    private Either<Throwable, AvailableItemsResponse> air2 = Either.right(AvailableItemsResponse.builder().hasMore(false).build());

    private WarehouseService warehouseService;
    private InventoryService inventoryService;
    private ProcessingPipeline processingPipeline;

    @BeforeEach
    void init() {
        warehouseService = mock(WarehouseService.class);
        inventoryService = mock(InventoryService.class);
    }

    @Test
    void testFetchAndPublishSinglePage() {
        // given
        Either<Throwable, Boolean> publishResult = Either.right(true);
        when(warehouseService.fetchAvailableItems())
                .thenReturn(air2);
        when(inventoryService.publishAvailableItems(any()))
                .thenReturn(publishResult);
        processingPipeline = new ProcessingPipeline(warehouseService, inventoryService);

        // when
        var result = processingPipeline.processItems();

        // then
        verify(warehouseService, times(1)).fetchAvailableItems();
        verify(inventoryService, times(1)).publishAvailableItems(any());
        assertEquals(publishResult, result);
    }

    @Test
    void testFetchAndPublishTwoPages() {
        // given
        Either<Throwable, Boolean> publishResult = Either.right(true);
        when(warehouseService.fetchAvailableItems())
                .thenReturn(air1)
                .thenReturn(air2);
        when(inventoryService.publishAvailableItems(any()))
                .thenReturn(publishResult);
        processingPipeline = new ProcessingPipeline(warehouseService, inventoryService);

        // when
        var result = processingPipeline.processItems();

        // then
        verify(warehouseService, times(2)).fetchAvailableItems();
        verify(inventoryService, times(2)).publishAvailableItems(any());
        assertEquals(publishResult, result);
    }

    @Test
    void testFetchAndPublishThreePages() {
        // given
        Either<Throwable, Boolean> publishResult = Either.right(true);
        when(warehouseService.fetchAvailableItems())
                .thenReturn(air1)
                .thenReturn(air1)
                .thenReturn(air2);
        when(inventoryService.publishAvailableItems(any()))
                .thenReturn(publishResult);
        processingPipeline = new ProcessingPipeline(warehouseService, inventoryService);

        // when
        var result = processingPipeline.processItems();

        // then
        verify(warehouseService, times(3)).fetchAvailableItems();
        verify(inventoryService, times(3)).publishAvailableItems(any());
        assertEquals(publishResult, result);
    }

    @Test
    void testFetchFailed() {
        // given
        Either<Throwable, AvailableItemsResponse> fetchResult = Either.left(new Exception());
        when(warehouseService.fetchAvailableItems())
                .thenReturn(fetchResult);
        processingPipeline = new ProcessingPipeline(warehouseService, inventoryService);

        // when
        var result = processingPipeline.processItems();

        // then
        verify(warehouseService, times(1)).fetchAvailableItems();
        verify(inventoryService, times(0)).publishAvailableItems(any());
        assertEquals(fetchResult, result);
    }

    @Test
    void testPublishFailed() {
        // given
        when(warehouseService.fetchAvailableItems())
                .thenReturn(air1);
        Either<Throwable, Boolean> publishResult = Either.left(new Exception());
        when(inventoryService.publishAvailableItems(any()))
                .thenReturn(publishResult);
        processingPipeline = new ProcessingPipeline(warehouseService, inventoryService);

        // when
        var result = processingPipeline.processItems();

        // then
        verify(warehouseService, times(1)).fetchAvailableItems();
        verify(inventoryService, times(1)).publishAvailableItems(any());
        assertEquals(publishResult, result);
    }
}
