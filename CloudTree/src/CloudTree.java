import java.util.Optional;

public interface CloudTree {

    void insert(String key, String value);

    Optional<String> query(String key);

    Optional<String> delete(String key);

    void print();
}
