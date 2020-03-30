# Case study: Java functional programming using Vavr

[Vavr](https://vavr.io) is a functional library for Java 8+ that provides persistent data types and functional control structures. 

Most Vavr tutorials show how to use Vavr primitives but they do not provide intuition on how to write whole applications.
This is something to be addressed here, by means of examining some of the typical aspects of writing microservices.
The provided analysis is based on an example that has its roots in professional programming reality.

Please note that this post is neither introduction to functional programming (FP) nor Vavr tutorial.
Ideally, the reader should have been already exposed to Vavr in one way or another (at least read the documentation).
A very basic understanding of FP concepts like immutability or pure functions is sufficient.

# Visualising programs as pipelines
Before jumping into the promised case study, let's try to plant the seed of the intuition to be developed.
In FP errors are not thrown as they would be in pure Java. Instead, they are returned from methods similar to non-error values.
This allows us to visualize programs as pipelines, through which errors flow alongside non-error results.  

Just one remark here. One might argue whether the word 'pipeline' is appropriate or not.
Why not simply 'stream'? I find 'pipeline' accurate as streams need to flow through something.
From all such 'somethings' first thing which comes to my mind is 'pipeline'.
And it stays in there after giving it a reasonable amount of thoughts.  

Have a look at the pictures below:

![example_flows](images/example_flows.png)

They both present programs as pipelines. A single arrow should be interpreted as a single computational stage (for example a single method),
whereas the colors mean:  
- green - purely functional (no side effects),  
- orange - with side effects,  
- red - the flow of errors

The left example shows a purely functional program, meaning none of the pipeline stages performs any side effects.
It is hard to imagine that such a program could be of any use - no side effects means no printing result on the screen/
writing to a file, no possibility to interactively input parameters during execution, etc.  

The example on the right is more interesting - apart from doing purely FP-style calculations,
it reads user input, calls database service and can display some results on the screen.
Should any error occur, it will be wrapped (with the help of Vavr) into a proper type and returned (not thrown) from a method.
The pipeline, assembled from Vavr primitives, takes care of proper error propagation to the place where the program begins.

Writing programs in FP fashion is nothing more complicated than that.
It is about assembling such pipelines and paying attention to side effects.
Now we are ready to analyze a slightly more complicated program.

Some of its parts will serve as a decent example of the most typical aspects to be dealt with.

# Case study - an example FP style microservice

Firstly, a picture worth more than 1000 words.
It depicts our microservice as a pipeline.
Numbers marking aspects to be examined are added as well:

![service_flow](images/service_flow.png)

To describe it in one sentence - this is a service triggered once in a while which polls external database service for some data and publishes them to 
another endpoint.
Additionally, it leverages S3 to store some basic information about where it previously finished -
it is useful for both the next service run as well to know from where to continue after crashes.

The exact algorithm describing the service functionality is as follows:
- get latest 'polling status' from S3
- use it to compose query to the database
- knowing that database response might consist of multiple pages, for each page (large loop on the picture):
- request access token and (small loop):
- publish to external endpoint (using access token) all entries from a single page, one at a time;
after each such successful operation, update 'polling status' on S3.

This example is based on a real microservice.
It is built without any frameworks apart from Guice to provide dependency injection.
To avoid overcomplication, everything happens in one thread.
Additionally, error while contacting any of the external services means the pipeline breaks.
(OK, some failed HTTP calls might be retried two or three times, but if this fails the pipeline breaks.
This is acceptable - thanks to storing 'polling status' on S3, the next run will continue from the place where previously broke).

All external services are exposed through REST, but what truly matters is that contacting them is a side effect.

Having understood the flow of the service, we are ready to analyze some of its crucial parts to learn how Vavr helps in achieving that.

## Initial considerations
Vavr equips us with two ways of building pipelines: classes Either and Try.
Since Try might be treated as a specific case of Either, for the needs of this case study it does not really matter which one we choose.
Let's choose Either and base all examples on it.

## Aspect 1 - external services' calls
External services' calls produce side effects. In particular, they might throw exceptions.
To 'plug' them into our pipelines, such calls need to be appropriately wrapped.

Let's take a look at the class responsible for publishing events to the external endpoint through REST.
It shows how to wrap an HTTP call which might throw an exception.
Should an exception be thrown, it will be wrapped into Try, then translated to Either.Left and returned from the method.
(note: of course, when you base your pipeline on Try, conversion to Either is not necessary).

Let's assume external HTTP calls are done by client implementing the following interface:
```
interface InventoryPublisherClient {
    Boolean publishItem(ItemData item);
}
```
Then our wrapper class may simply take the form of:
```
@Value
class InventoryPublisher {
    private final InventoryPublisherClient inventoryPublisherClient;

    public Either<Throwable, Boolean> publishItem(ItemData item) {
        return Try.of(() -> inventoryPublisherClient.publishItem(item))
                .toEither();
    }
}
```

This schema is universal to all throwing methods.
Once again - just wrap them before plugging into the pipeline.  
Code with unit tests is available
[here](./code-examples/src/test/java/com/pbroda/codesnippets/vavr/ExternalServiceCall.java).

## Aspect 2 - handling loops
From the picture presenting our microservice, we may tell that it has at least two loops.
Neither Vavr documentation nor most of the tutorials on the Internet do not teach us how to deal with them properly using Vavr.
They explain only trivial scenarios, not diving into even slightly more complicated ones.
We are going to fill this gap and analyze here one such case in detail.
We show how to write a loop where the number of iterations is determined by the result returned from a call to an external service.

Let's talk about some basics first.  
Vavr equips us with tools one may find valuable writing such loops.
These are primitives like ```Stream.continually()```, ```takeWhile()```, ```dropWhile()``` and ```find()```.
And their 'brothers and sister' (```takeUntil()``` etc.).  

**Now comes a very important thing, crucial to understanding this paragraph.
The io.vavr.collection.Stream implementation is a lazy linked list.**
This has very serious consequences which one might observe with following snippet:

```
testStream = Stream.of(pr1, pr2, pr3, pr4, pr5, pr6);
testList = List.of(pr1, pr2, pr3, pr4, pr5, pr6);

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
```

One might observe that lazy implementation of ```Stream``` class causes that ```anyServiceStream::serviceCall```
will be called as many times as needed to fulfill ```find()``` condition. 
This is not the case for the ```List``` - ```anyServiceList::serviceCall``` will be executed as many times as there are elements in ```testList```.
Unit tests showing not only ```find()``` behavior but also other mentioned methods can be found
[here](./code-examples/src/test/java/com/pbroda/codesnippets/vavr/LoopBasics.java).

Now it is time for our example. 
Assume, we want to call two services in a loop (this correspond to the bigger rectangle on the picture)
We call them consecutively in such a way that the result of the first call is fed to the second call.
This proceeds in a loop until there is a certain condition met, based on the results of the first call.
Let's repeat as it is important - first call, not the second one.

If it is not obvious how to write such a loop, we might first try to write it as a kind of pseudo-code presented below.
Unit tests not only assure us the functionality is correct, but also allow us with experimenting on the way to get a nice result.
So our first version might look like: 
```
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
```   

In order to refactor ProcessingPipeline class above we need to deal with two problems, both related to the condition
which terminates the loop:  
- it is based on the result of a service call - we have already covered it at the beginning of this paragraph,  
- it is based on the result of the first service call, as already stressed.

How to solve the latter? It would be no problem if the condition ending the loop was based on the result of the second
call. That gives us a hint that we should make it this way - i.e. we should keep aside the result of the first call,
perform the second call, and return both together. We use ```Tuple``` to achieve that:

```
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
                .flatMap(itemsResponse -> publishAvailableItems(itemsResponse));
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

```

The whole code, with definitions of both service calls, can be found
[here](./code-examples/src/test/java/com/pbroda/codesnippets/vavr/LoopCaseStudy.java).

## Aspect 3 - logging
When logging methods we write are meant to just log messages passed to them, i.e. their signatures are of form:
```
void logInfo(String message) {...}
void logDebug(String message) {...}
void logError(String message) {...}
```
then they might be easily plugged into the pipeline:  
```.peek(v -> logInfo(createLogMessage(v))``` or  
```.peek(v -> logDebug(createLogMessage(v))``` or  
```.peekLeft(err -> logError(err, createLogMessage(v))```  

However, one might be tempted to write logging methods such that (internally) they call other methods with side effects.
Example signature of such method would be:
```
public static <T> Either<Throwable, T> logInfo(T rightValue, String message) {...}
```
Because of the returned value, it needs to be plugged into the pipeline using ```map()``` instead of ```peek()```.
```
.map(value -> logInfo(value, "log message"))
```

I would rather advise splitting such method into two parts: just logging and the rest with side effects.
Apart from sticking to single responsibility rule, it would be great to have methods to log all levels (info, debug, error, etc.)
in a consistent way.
And that becomes hard for writing ```logError()``` method.
```LogError()``` method is supposed to log exceptions, but it would be plausible that ```logError()``` itself would result in an exception.
It would be still feasible to code it, with for example help of 'suppressed' exceptions, but one might unnecessarily
end up here with an overcomplicated code.

## Aspect 4 - exceptions
Exceptions simply travel through the pipeline as Either.Left.
As they make their way towards the end of the pipeline, there might be a need for translating them.
In our example, we are in a very comfortable situation in which we allow them to propagate to the very end and log them there - just in one place.
Should any translation of an error be needed at any stage of the pipeline, then simply:
```
.mapLeft(err -> translateError(err))
```

What is more, I also consider a good practice wrapping the starting point of an application with ```Try```:
```
public static void main(String[] args) {
    Try.of(() -> startApplication())
        .onFailure(t -> logError(t, "Service failure"))
}
```
so that we have at least a chance to log all exceptions not caught by our more granular instances of ```Try.of()```.

## Other aspects - with or without ifs?
Both Java Streams and Vavr allow us to remove ifs from the code completely.
For example, instead of:
```
public boolean filter(String label) {
    if (label == null || label.isBlank()) {
        return false;
    }
    if (!allowedLabels.contains(label.toUpperCase())) {
        return false;
    }
    return true;
}
```

one might refactor it to:
```
public boolean filter2(String label) {
    return Option.of(label)
            .map(String::toUpperCase)
            .filter(allowedLabels::contains)
            .isDefined();
}
```

Which is better? IMHO it depends:
- if there are no side effects involved, just pure logic like in the example above, then it is a matter of personal taste,  
- if there are side effects involved, I would compose code into a pipeline as this allow to propagate exceptions nicely:  

```
public Either<Throwable, Boolean> filter3(String label) {
    return Option.of(label)
            .map(allowedLabelsService::isAllowed)
            .getOrElse(() -> Either.right(false));
}
```

All three methods (filter1, filter2 and filter3) with unit tests can be found
[here](./code-examples/src/test/java/com/pbroda/codesnippets/vavr/WithOrWithoutIfs.java).

# Summary
The case study presented above should enable the reader to write Java microservices in a functional style using Vavr.
It may also serve as proof that it is not necessary to bring any of the advanced FP-related terminologies to talk about FP usage in practice!
