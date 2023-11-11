package example.streaming.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import example.streaming.service.AsyncSlowService;

import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("/futures")
public class FuturesController {
    @Autowired
    private AsyncSlowService service;

    @GetMapping("/basic")
    public String basicPage(Model model) {

        model.addAttribute("myData", service.getData1());

        model.addAttribute("myData2", service.getIntermediateData().thenCompose(
                intermediateData -> service.getData2(intermediateData + " and other work done") ));

        return "futures";
    }

    @GetMapping("/dependencies")
    public String pageWithDependencies(Model model) {

        CompletableFuture<String> data1 = service.getData1();
        model.addAttribute("myData", data1);

        CompletableFuture<String> intermediateData = service.getIntermediateData();

        model.addAttribute("myData2", data1.thenCompose(data -> {
            String result = data + " " + intermediateData.join();
            return service.getData2(result + " and sub-work is done");
        }) );

        return "futures";
    }

}


// Alternatives for dependencies:
//        model.addAttribute("myData2",
//                data1.thenCompose(data -> intermediateData.thenCompose(intermediate -> {
//            String result = data + " " + intermediate;
//            return service.getData2(result + " and sub-work is done");
//        })) );
//        model.addAttribute("myData2",
//                data1.thenCombine(intermediateData, (String data1Result, String intermediateResult) -> {
//            String result = data1Result + " " + intermediateResult;
//            return service.getData2(result + " and sub-work is done").join();
//        }) );
