package example.streaming;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.lang.Nullable;
import org.springframework.ui.Model;

public interface AsyncModel extends Model {

    <T> Future<T> addAttribute(String attributeName, Callable<T> attributeValue);

    @Override
    AsyncModel addAttribute(String attributeName, @Nullable Object attributeValue);
    @Override
    AsyncModel addAttribute(Object attributeValue);
    @Override
    AsyncModel addAllAttributes(@Nullable Collection<?> attributeValues);
    @Override
    AsyncModel addAllAttributes(@Nullable Map<String, ?> attributes);
    @Override
    AsyncModel mergeAttributes(@Nullable Map<String, ?> attributes);

}
