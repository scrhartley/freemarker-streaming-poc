# FreeMarker Streaming Proof-of-Concept

Proof-of-concept for HTML streaming in [Spring](https://spring.io/projects/spring-boot) implemented with the
[FreeMarker](https://freemarker.apache.org/) template language.  

The problem with normal MVC is that when a user visits a web page,
nothing happens until all the data has been collected and the server responds with the HTML page. 
In order to shorten that wait, this repo takes the strategy of starting to send HTML to the user before all the data is ready.  
This provides the following benefits: 
- The browser can start downloading assets sooner (JS/CSS/images) 
  - Assets can download simultaneously with the HTML, improving total page load time and indirectly [TTI](https://web.dev/tti/)
- The user can see parts of the page being rendered, rather than staring at a blank page 
([TTFB](https://web.dev/ttfb/), [FCP](https://web.dev/fcp/) and potentially [LCP](https://web.dev/lcp/))

***Traditional page load***  
![Traditional page network requests](/markdown_assets/load_traditional.png)  
***Streaming page load***  
![Streaming page network requests](/markdown_assets/load_streaming.png)  


## Endpoints


### Page loading alternatives
- `/load/traditional` ***Traditional page load***  
Loads page in the traditional MVC manner, assembling the data before running the template.  
The user has to wait for everything to be finished before anything can be shown.  
The browser can not start loading any CSS and JS in the HTML's head until the template has finished running.  


- `/load/streaming` ***HTML Streaming (with auto-flushing)***  
Load page incrementally using streaming, sending pending HTML while waiting for the next piece of data.  
The user will see each piece of the page as soon as it's ready.  
(See `/atoms` example for refining this.)


- `/load/head-first` ***Manual flushing example (still using streaming)***  
Use manual flushing in the template to send the HTML head before the rest of the page.  
In this example, although the browser can download JS and CSS assets earlier, 
the user still does not see anything until the entire page is ready. This could be improved by additional manual flushes, 
but as the complexity of a template grows, this approach seems less convenient and more error-prone.


### Interleaved vs. Concurrent Execution

These types of execution contrast with the standard MVC pattern of assembling all the data before running the template.

- `/callables/basic`,  `/callables/dependencies` ***Interleaved execution***  
These are example implementations using Callables.  
When a value is accessed in the template, its Callable is invoked.  
This approach interleaves running the template and fetching data.  
As well as the basic endpoint, an additional endpoint shows how data dependencies can be managed in the Controller.


- `/futures/basic`, `/futures/dependencies` ***Concurrent execution***  
These are example implementations using Futures.  
When a value is accessed in the template, a blocking wait is done for its Future's result.  
When blocking on the Future being accessed, the execution of other Futures continues concurrently.  
As well as the basic endpoint, an additional endpoint shows how data dependencies can be managed in the Controller.


### Additional directives

- `/atoms` ***Atom directive***  
If you're unhappy about where auto-flushing occurs, then this can be refined using the atom directive.  
This example is a refinement of the behaviour in the `/load/streaming` endpoint (and it's template).  
Atom is implemented as a custom Java directive.  


- `/error-boundaries` ***Error boundary directive***  
An alternative to FreeMarker's attempt/recover directives, providing a more declarative and flexible way
to show fallback content for a part of the page when an error occurs.  
Error boundary is implemented as a macro in the template.  
Inspiration: https://react.dev/reference/react/Component#catching-rendering-errors-with-an-error-boundary


- `/suspend` ***Suspend directive***  
Builds upon atom to show a loading indicator until its content is complete.
If JavaScript is not available, then the loading indicator/fallback will not be shown. 
Perhaps this directive is less useful when using concurrency, since it can only show one
loading indicator at a time and other pending data may complete at roughly the same time.
For this reason it is not called suspense and a proper out-of-order version
seems like it would require changes to FreeMarker itself.  
If you wish to use suspend and error boundary together then, unlike React,
the error boundary should be inside the suspend and not the other way round.  
Suspend is implemented as a macro in the template and uses the atom directive.  
Inspiration: https://react.dev/reference/react/Suspense


## Notes

### Error handling

Since potential exceptions are deferred, error handling is different from traditional MVC pages.  
This provides an opportunity to more easily have parts of the page fail independently (e.g. using error boundaries).  
See `EnhancedTemplateExceptionHandlers` for ways unhandled errors are dealt with, closer to traditional MVC. 

### Java Version

The code in this project related to FreeMarker and its Spring integration is written to compile under JDK 8. 
However, the project was switched to Java 9 in order to be able to use `CompletableFuture#failedFuture` 
in the example service using Spring async. If JDK 8 and failedFuture is required, you could use:
```
public static <T> CompletableFuture<T> failedFuture(Throwable ex) {
    CompletableFuture<T> future = new CompletableFuture<>();
    future.obtrudeException(ex);
    return future;
}
```

### Easter eggs

1. Look at `FakeFuturesController` for how to get interleaved execution while avoiding both async services and
using Callables directly. This implementation uses a special non-concurrent ExecutorService to avoid having to
manage a normal ExecutorService.  
2. Plumb in `ExtraConcurrentFreeMarkerConfig` which automatically converts Callables to Futures in order to get
concurrency in templates without using async services (alternatively, use an ExecutorService in your Controllers).  

### Warnings and Troubleshooting

- HTTP streaming may not work if your servers are configured incorrectly. For notes on buffering and Nagle's algorithm see:
  https://medium.com/airbnb-engineering/improving-performance-with-http-streaming-ba9e72c66408
- Any increase of concurrency in your application may lead to increased demand for database connections.
  Make sure that your connection pooling and database are tuned accordingly.
