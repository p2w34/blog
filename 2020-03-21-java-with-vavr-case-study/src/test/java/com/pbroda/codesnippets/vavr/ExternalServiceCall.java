package com.pbroda.codesnippets.vavr;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.Value;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Value
class ItemData {
    String itemDescription;
}

interface InventoryPublisherClient {
    Boolean publishItem(ItemData item) throws RuntimeException;
}

@Value
class InventoryPublisher {
    private final InventoryPublisherClient inventoryPublisherClient;

    public Either<Throwable, Boolean> publishItem(ItemData item) {
        return Try.of(() -> inventoryPublisherClient.publishItem(item))
                .toEither();
    }
}

@ExtendWith(MockitoExtension.class)
public class ExternalServiceCall {

    private InventoryPublisherClient inventoryPublisherClient;
    private InventoryPublisher inventoryPublisher;

    private ItemData anyItemData = new ItemData("anyItemDescription");

    @BeforeEach
    void init() {
        inventoryPublisherClient = mock(InventoryPublisherClient.class);
    }

    @Test
    void testPublishSuccessful() {
        // given
        val publishResult = true;
        when(inventoryPublisherClient.publishItem(any())).thenReturn(publishResult);
        inventoryPublisher = new InventoryPublisher(inventoryPublisherClient);

        // when
        val result = inventoryPublisher.publishItem(anyItemData);

        // then
        assertEquals(Either.right(publishResult), result);
        verify(inventoryPublisherClient, times(1)).publishItem(anyItemData);
    }

    @Test
    void testPublishFailure() {
        // given
        val anyException = new RuntimeException();
        when(inventoryPublisherClient.publishItem(any())).thenThrow(anyException);
        inventoryPublisher = new InventoryPublisher(inventoryPublisherClient);

        // when
        val result = inventoryPublisher.publishItem(anyItemData);

        // then
        assertEquals(Either.left(anyException), result);
        verify(inventoryPublisherClient, times(1)).publishItem(anyItemData);
    }
}
