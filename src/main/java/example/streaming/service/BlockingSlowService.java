package example.streaming.service;

import org.springframework.stereotype.Service;

@Service
public class BlockingSlowService {

    public String getData1() throws Exception {
        Thread.sleep(3_500);
        return "Work done";
    }

    public String getIntermediateData() {
        return "Intermediate work done";
    }

    public String getData2(String param) throws Exception {
        Thread.sleep(3_000);
        return param;
    }

}
