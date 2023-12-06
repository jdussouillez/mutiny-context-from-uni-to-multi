# mutiny-context-from-uni-to-multi

Project to reproduce a weird behavior with context propagation from an Uni to a Multi.

TODO: add link here

## Description

I have a method that using an Uni and returning a Multi. But the twist is: I need some data when the multi is completed, and this data is accessible in the Uni, before I create the multi.
So I'm using a [context](https://smallrye.io/smallrye-mutiny/2.5.1/guides/context-passing/) to store this data and get it back later.

```java
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
```

## Problem

When I'm using a `subscribe` then everything works fine:

```java
var values = fetch()
    .subscribe()
    .asStream()
    .toList();
// "value2" was in the context when the Multi completed: OK
```

But when I'm using `await` then my context is empty!

```java
var values = fetch()
    .collect()
    .asList()
    .await()
    .indefinitely();
// "value2" was not in the context the Multi completed: failure
```
