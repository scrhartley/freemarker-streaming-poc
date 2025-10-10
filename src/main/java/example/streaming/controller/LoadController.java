package example.streaming.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import example.streaming.AsyncModel;

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
    public String streamingPageLoad(AsyncModel model) {
        model.addAttribute("myData", () -> {
            Thread.sleep(3_500); // Fetching data over network
            return "My data";
        });
        return "load/loading";
    }

    @GetMapping("/head-first")
    public String headBeforeRestOfPage(AsyncModel model) {
        streamingPageLoad(model);
        return "load/head_first";
    }

}
