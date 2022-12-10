package simon.example.streaming.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.Callable;

@Controller
@RequestMapping("/load")
public class LoadController {

    @GetMapping("/traditional")
    public String traditionalPageLoad(Model model) throws InterruptedException {
        Thread.sleep(3_500); // Fetching data over network
        model.addAttribute("myData", "My data");
        return "load/loading";
    }

    @GetMapping("/streaming")
    public String streamingPageLoad(Model model) {
        model.addAttribute("myData", (Callable<String>) () -> {
            Thread.sleep(3_500); // Fetching data over network
            return "My data";
        });
        return "load/loading";
    }

    @GetMapping("/head-first")
    public String headBeforeRestOfPage(Model model) {
        streamingPageLoad(model);
        return "load/head_first";
    }

}
