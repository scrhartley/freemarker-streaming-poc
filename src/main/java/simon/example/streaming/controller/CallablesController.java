package simon.example.streaming.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import simon.example.streaming.service.BlockingSlowService;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

        Callable<String> data1 = caching(service::getData1);

        model.addAttribute("myData", data1);

        model.addAttribute("myData2", caching(() -> {
            String data = data1.call();
            return service.getData2(data + " and sub-work is done");
        }));

        return "callables";
    }



    // Caching is a solution to help when both the template and the controller invoke the same callable.
    // This implementation is thread-safe, which might not always be required.
    private static <T> Callable<T> caching(Callable<T> callable) {
        return new Callable<>() {
            // At time of writing, preview Virtual Threads work best when avoiding synchronized keyword.
            private final Lock lock = new ReentrantLock();
            private Callable<T> resultCallable = callable;
            @Override
            public T call() throws Exception {
                lock.lock();
                try {
                    try {
                        T result = resultCallable.call();
                        if (resultCallable == callable) {
                            resultCallable = () -> result;
                        }
                        return result;
                    }
                    catch (Exception e) {
                        if (resultCallable == callable) {
                            resultCallable = () -> { throw e; };
                        }
                        throw e;
                    }
                } finally {
                    lock.unlock();
                }
            }
        };
    }

}
