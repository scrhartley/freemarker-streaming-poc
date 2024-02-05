package example.streaming.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.Callable;

@Controller
public class ExtrasController {

    @Autowired CallablesController callablesController;

    @GetMapping("/atoms")
    public String atoms(Model model) {
        callablesController.basicPage(model);
        return "extras/atoms";
    }

    @GetMapping("/error-boundaries")
    public String errorBoundaries(Model model) {
        model.addAttribute("throwsException", (Callable<String>) () -> {
            Thread.sleep(500);
            throw new Exception("Catch me!!!");
        });
        return "extras/error_boundaries";
    }

    @GetMapping("/suspend")
    public String suspend(Model model) {
        for (int i = 1; i <= 3; i++) {
            model.addAttribute("myData" + i, (Callable<String>) () -> {
                Thread.sleep(2_500);
                return "Work done";
            });
        }
        return "extras/suspend";
    }

}
