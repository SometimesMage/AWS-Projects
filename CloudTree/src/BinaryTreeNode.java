import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

public class BinaryTreeNode extends TreeNode {

    private String leftChild;
    private String rightChild;

    private BinaryTreeNode(String id, String key, String value, String parent, String leftChild, String rightChild) {
        super(id, key, value, parent);
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getLeftChild() {
        return leftChild;
    }

    public String getRightChild() {
        return rightChild;
    }

    public void setLeftChild(String leftChild) {
        this.leftChild = leftChild;
    }

    public void setRightChild(String rightChild) {
        this.rightChild = rightChild;
    }

    @Override
    public Map<String, AttributeValue> asItem() {
        Map<String, AttributeValue> item = super.asItem();
        if (leftChild != null)
            item.put("leftChild", new AttributeValue(leftChild));
        if (rightChild != null)
            item.put("rightChild", new AttributeValue(rightChild));
        return item;
    }

    public static class Builder extends TreeNode.Builder {

        private String leftChild;
        private String rightChild;

        @Override
        public Builder fromItem(Map<String, AttributeValue> item) {
            super.fromItem(item);
            this.leftChild = item.get("leftChild") == null ? null : item.get("leftChild").getS();
            this.rightChild = item.get("rightChild") == null ? null : item.get("rightChild").getS();
            return this;
        }

        public Builder leftChild(String leftChild) {
            this.leftChild = leftChild;
            return this;
        }

        public Builder rightChild(String rightChild) {
            this.rightChild = rightChild;
            return this;
        }

        @Override
        public BinaryTreeNode build() {
            return new BinaryTreeNode(id, key, value, parent, leftChild, rightChild);
        }
    }
}
