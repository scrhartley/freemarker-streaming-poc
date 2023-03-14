package simon.example.streaming;

/**
 * This class exists to allow FreeMarker to automatically convert Callables in the Model to Futures.
 * This allows concurrency in the templates without having to write async services.
 * Note: if you're using caching with your Callables, make sure those cache implementation are thread-safe.
 *
 * (Without any use of Futures, you still get the benefits of interleaved execution
 * and hence the ability to start sending HTML to the client before all data has been retrieved.)
 */
//@Configuration
public class ExtraConcurrentFreeMarkerConfig {

//    @Bean
//    FreeMarkerViewResolver freeMarkerViewResolver(FreeMarkerProperties properties, ExecutorService executorService) {
//        class CustomFreeMarkerView extends FreeMarkerView {
//            @Override @SuppressWarnings("unchecked")
//            public void render(@Nullable Map<String,?> model, HttpServletRequest request,
//                               HttpServletResponse response) throws Exception {
//                if (model != null) {
//                    callablesToFutures((Map<String,Object>) model);
//                }
//                super.render(model, request, response);
//            }
//            private void callablesToFutures(Map<String,Object> model) {
//                for (Map.Entry<String,Object> entry : model.entrySet()) {
//                    Object value = entry.getValue();
//                    if (value instanceof Callable<?>) {
//                        Future<?> future = executorService.submit((Callable<?>) value);
//                        entry.setValue(future);
//                    }
//                }
//            }
//        }
//        class CustomFreeMarkerViewResolver extends FreeMarkerViewResolver {
//            @Override
//            protected Class<?> requiredViewClass() {
//                return CustomFreeMarkerView.class;
//            }
//            @Override
//            protected AbstractUrlBasedView instantiateView() {
//                return getViewClass() == CustomFreeMarkerView.class
//                        ? new CustomFreeMarkerView() : super.instantiateView();
//            }
//        }
//
//        FreeMarkerViewResolver resolver = new CustomFreeMarkerViewResolver();
//        properties.applyToMvcViewResolver(resolver);
//        return resolver;
//    }

}
