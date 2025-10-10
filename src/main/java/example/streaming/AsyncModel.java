package example.streaming;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.ui.Model;

public interface AsyncModel extends Model {

    <T> Future<T> addAttribute(String attributeName, Callable<T> attributeValue);

}
