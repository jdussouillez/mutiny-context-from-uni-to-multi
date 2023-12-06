package com.github.jdussouillez.mutinycontext;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MutinyContextFromUniToMultiTest {

    private AtomicBoolean contextAvailableInMulti;

    @BeforeEach
    void beforeEach() {
        contextAvailableInMulti = new AtomicBoolean(false);
    }

    @Test
    void testUniMultiContextAwait() {
        var values = fetch()
            .collect()
            .asList()
            .await()
            .indefinitely();
        assertEquals(List.of(10, 20, 30), values);
        assertTrue(contextAvailableInMulti.get()); // Failure!
    }

    @Test
    void testUniMultiContextSubscribe() {
        var values = fetch()
            .subscribe()
            .asStream()
            .toList();
        assertEquals(List.of(10, 20, 30), values);
        assertTrue(contextAvailableInMulti.get()); // OK
    }

    private Multi<Integer> fetch() {
        return Uni.createFrom().item(1)
            .call(value1 -> Uni.createFrom().item(2)
                // Store "value2 = 2" in context
                .withContext((uniValue2, ctx) -> uniValue2.invoke(value2 -> ctx.put("value2", value2)))
            )
            .chain(value1 -> Uni.createFrom().item(3))
            .onItem()
            .transformToMulti(value3 -> Multi.createFrom().items(10, 20, 30))
            .withContext((multi, ctx) -> multi
                .onCompletion()
                .call(() -> {
                    // If need "value2" here to do some stuff
                    contextAvailableInMulti.set(ctx.contains("value2"));
                    // TODO: use "value2"
                    return Uni.createFrom().voidItem();
                })
            );
    }
}
