import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TreeNode {
    public static final String ROOT_ID = "root";

    private String id;
    private String key;
    private String value;
    private String parent;

    protected TreeNode(String id, String key, String value, String parent) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.parent = parent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getParent() {
        return parent;
    }

    public boolean isRoot() {
        return id.equals(ROOT_ID);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Map<String, AttributeValue> asItem() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", new AttributeValue(id));
        item.put("key", new AttributeValue(key));
        item.put("value", new AttributeValue(value));
        if (parent != null)
            item.put("parent", new AttributeValue(parent));
        return item;
    }

    public static class Builder {
        protected String id;
        protected String key;
        protected String value;
        protected String parent;

        protected Builder() {
            this.id = UUID.randomUUID().toString();
        }

        public Builder fromItem(Map<String, AttributeValue> item) {
            this.id = item.get("id").getS();
            this.key = item.get("key").getS();
            this.value = item.get("value").getS();
            this.parent = item.get("parent") == null ? null : item.get("parent").getS();
            return this;
        }

        public Builder isRoot() {
            this.id = ROOT_ID;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder parent(String parent) {
            this.parent = parent;
            return this;
        }

        public TreeNode build() {
            return new TreeNode(id, key, value, parent);
        }
    }
}
