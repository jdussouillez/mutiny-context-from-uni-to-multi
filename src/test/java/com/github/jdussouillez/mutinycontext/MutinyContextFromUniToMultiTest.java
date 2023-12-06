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

    private static final List<String> VALUES = List.of("step 4.1", "step 4.2", "step 4.3");

    private AtomicBoolean contextAvailableInMulti;

    @BeforeEach
    void beforeEach() {
        contextAvailableInMulti = new AtomicBoolean(false);
    }

    @Test
    void testUniMultiContextAwait() {
        var results = fetch()
            .collect()
            .asList()
            .await()
            .indefinitely();
        assertEquals(VALUES, results);
        assertTrue(contextAvailableInMulti.get()); // Failure!
    }

    @Test
    void testUniMultiContextSubscribe() {
        var results = fetch()
            .subscribe()
            .asStream()
            .toList();
        assertEquals(VALUES, results);
        assertTrue(contextAvailableInMulti.get()); // OK
    }

    @Test
    void testUniMultiContextSubscribeCollect() {
        var results = fetch()
            .collect()
            .asList()
            .subscribe()
            .asCompletionStage()
            .join();
        assertEquals(VALUES, results);
        assertTrue(contextAvailableInMulti.get()); // OK
    }

    private Multi<String> fetch() {
        return step1()
            .call(step1Result -> step2()
                // Store "step2" result in the context
                .withContext((s2, ctx) -> s2.invoke(step2Result -> ctx.put("s2", step2Result)))
            )
            .chain(step1Result -> step3())
            .onItem()
            .transformToMulti(step3Result -> step4(step3Result))
            .withContext((multi, ctx) -> multi
                .onCompletion()
                .call(() -> {
                    // I need "step2" result here to do some stuff
                    contextAvailableInMulti.set(ctx.contains("s2")); // Check if the context has "step2" result
                    return Uni.createFrom().voidItem();
                })
            );
    }

    private Uni<String> step1() {
        return Uni.createFrom().item("step 1");
    }

    private Uni<String> step2() {
        return Uni.createFrom().item("step 2");
    }

    private Uni<String> step3() {
        return Uni.createFrom().item("step 3");
    }

    private Multi<String> step4(final String input) {
        return Multi.createFrom().iterable(VALUES);
    }
}
