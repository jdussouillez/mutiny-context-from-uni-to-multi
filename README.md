# mutiny-context-from-uni-to-multi

Project to reproduce a weird behavior with context propagation from an Uni to a Multi.

TODO: add link here

## Description

I have a method using an Uni and returning a Multi. But the twist is: I need some data when the Multi is completed, and this data is accessible in the Uni, before I create the Multi.
So I'm using a [context](https://smallrye.io/smallrye-mutiny/2.5.1/guides/context-passing/) to store this data and get it back later.

```java
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
```

## Actual behavior

When I'm using a `subscribe` then everything works fine:

```java
var values = fetch()
    .subscribe()
    .asStream()
    .toList();
// "s2" value was in the context when the Multi completed: OK
```

But when I'm using `await` then my context is empty!

```java
var values = fetch()
    .collect()
    .asList()
    .await()
    .indefinitely();
// "s2" value wasn't in the context when the Multi completed: Failure
```

## Expected behavior

I guess the context propagation should work the same way, whether we use `subscribe` or `await`, right?

## Run the tests

```sh
./mvnw test
```

Output:
```
[ERROR] Failures: 
[ERROR]   MutinyContextFromUniToMultiTest.testUniMultiContextAwait:31 expected: <true> but was: <false>
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
```
