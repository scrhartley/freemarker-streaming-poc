package simon.example.streaming.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import simon.example.streaming.service.BlockingSlowService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@Controller
@RequestMapping("/callables")
public class CallablesController {
    @Autowired
    private BlockingSlowService service;

    @GetMapping("/basic")
    public String basicPage(Model model) {

        model.addAttribute("myData", (Callable<String>) service::getData1);

        model.addAttribute("myData2", (Callable<String>) () -> {
            String param = service.getIntermediateData();
            return service.getData2(param + " and other work done");
        });

        return "callables";
    }

    @GetMapping("/dependencies")
    public String pageWithDependencies(Model model) {

        Callable<String> data1 = resolvingOnce(service::getData1);

        model.addAttribute("myData", data1);

        model.addAttribute("myData2", resolvingOnce(() -> {
            String data = data1.call();
            return service.getData2(data + " and sub-work is done");
        }));

        return "callables";
    }



    // Helps when both the template and the controller invoke the same callable.
    // This implementation is thread-safe, which might not always be required.
    private static <T> Callable<T> resolvingOnce(Callable<T> callable) {
        return new Callable<T>() {
            private final FutureTask<T> future = new FutureTask<>(callable);
            @Override
            public T call() throws Exception {
                future.run(); // Returns immediately if already running
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof Exception) {
                        throw (Exception) e.getCause();
                    }
                    throw e;
                }
            }
        };
    }


}
