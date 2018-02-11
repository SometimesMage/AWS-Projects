import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

public class BinarySearchCloudTree extends AbstractCloudTree<BinaryTreeNode> {

    public BinarySearchCloudTree(String treeName, String credentialsFile) throws InterruptedException {
        super(treeName, credentialsFile);
    }

    @Override
    public BinaryTreeNode createNode(Map<String, AttributeValue> item) {
        return BinaryTreeNode.builder().fromItem(item).build();
    }

    @Override
    public void insert(String key, String value) {
        Optional<BinaryTreeNode> root = getRootNode();
        if (!root.isPresent()) {
            putNode(BinaryTreeNode.builder()
                    .isRoot()
                    .key(key)
                    .value(value)
                    .build());
            return;
        }

        BinaryTreeNode focusNode = root.get();
        BinaryTreeNode.Builder newNodeBuilder = (BinaryTreeNode.Builder) BinaryTreeNode.builder().key(key).value(value);
        while (true) {
            int compareValue = key.compareTo(focusNode.getKey());
            newNodeBuilder.parent(focusNode.getId());
            if (compareValue < 0) {
                if (focusNode.getLeftChild() == null) {
                    BinaryTreeNode newNode = newNodeBuilder.build();
                    focusNode.setLeftChild(newNode.getId());
                    putNode(newNode);
                    putNode(focusNode);
                    return;
                }

                focusNode = getNode(focusNode.getLeftChild())
                        .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
            } else if (compareValue > 0) {
                if (focusNode.getRightChild() == null) {
                    BinaryTreeNode newNode = newNodeBuilder.build();
                    focusNode.setRightChild(newNode.getId());
                    putNode(newNode);
                    putNode(focusNode);
                    return;
                }

                focusNode = getNode(focusNode.getRightChild())
                        .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
            } else {
                if (focusNode.getValue().equals(value))
                    return;
                focusNode.setValue(value);
                putNode(focusNode);
            }
        }
    }

    @Override
    public Optional<String> query(String key) {
        Optional<BinaryTreeNode> node = queryNode(key);
        return node.map(TreeNode::getValue);
    }

    private Optional<BinaryTreeNode> queryNode(String key) {
        Optional<BinaryTreeNode> root = getRootNode();

        if (!root.isPresent()) {
            return Optional.empty();
        }

        BinaryTreeNode focusNode = root.get();

        while (true) {
            int compareValue = key.compareTo(focusNode.getKey());
            if (compareValue < 0) {
                if (focusNode.getLeftChild() == null)
                    return Optional.empty();
                focusNode = getNode(focusNode.getLeftChild())
                        .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
            } else if (compareValue > 0) {
                if (focusNode.getRightChild() == null)
                    return Optional.empty();
                focusNode = getNode(focusNode.getRightChild())
                        .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
            } else {
                return Optional.of(focusNode);
            }
        }
    }

    @Override
    public Optional<String> delete(String key) {
        Optional<BinaryTreeNode> query = queryNode(key);

        if(!query.isPresent())
            return Optional.empty();

        BinaryTreeNode focusNode = query.get();
        String oldValue = focusNode.getValue();

        if(focusNode.getLeftChild() == null && focusNode.getRightChild() == null) {
            if(!focusNode.isRoot()) {
                BinaryTreeNode parentNode = getNode(focusNode.getParent())
                        .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
                if(parentNode.getLeftChild() != null && parentNode.getLeftChild().equals(focusNode.getId()))
                    parentNode.setLeftChild(null);
                if(parentNode.getRightChild() != null && parentNode.getRightChild().equals(focusNode.getId()))
                    parentNode.setRightChild(null);
                putNode(parentNode);
            }

            deleteNode(focusNode.getId());
            return Optional.of(oldValue);
        }

        if(focusNode.getLeftChild() == null || focusNode.getRightChild() == null) {
            BinaryTreeNode childNode;

            if(focusNode.getLeftChild() != null) {
                childNode = getNode(focusNode.getLeftChild())
                        .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
            } else {
                childNode = getNode(focusNode.getRightChild())
                        .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
            }

            if(focusNode.isRoot()) {
                focusNode.setKey(childNode.getKey());
                focusNode.setValue(childNode.getValue());
                focusNode.setLeftChild(childNode.getLeftChild());
                focusNode.setRightChild(childNode.getRightChild());

                if(childNode.getLeftChild() != null) {
                    BinaryTreeNode leftChild = getNode(childNode.getLeftChild())
                            .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
                    leftChild.setParent(TreeNode.ROOT_ID);
                    putNode(leftChild);
                }

                if(childNode.getRightChild() != null) {
                    BinaryTreeNode rightChild = getNode(childNode.getRightChild())
                            .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
                    rightChild.setParent(TreeNode.ROOT_ID);
                    putNode(rightChild);
                }

                putNode(focusNode);
                deleteNode(childNode.getId());
                return Optional.of(oldValue);
            }

            BinaryTreeNode parentNode = getNode(focusNode.getParent())
                    .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));

            if(parentNode.getLeftChild().equals(focusNode.getId()))
                parentNode.setLeftChild(childNode.getId());
            if(parentNode.getRightChild().equals(focusNode.getId()))
                parentNode.setRightChild(childNode.getId());

            childNode.setParent(parentNode.getId());
            putNode(childNode);
            putNode(parentNode);
            deleteNode(focusNode.getId());
            return Optional.of(oldValue);
        }

        BinaryTreeNode smallRightNode = getNode(focusNode.getRightChild())
                .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
        BinaryTreeNode parentSmallRightNode = focusNode;

        while(smallRightNode.getLeftChild() != null) {
            parentSmallRightNode = smallRightNode;
            smallRightNode = getNode(smallRightNode.getLeftChild())
                    .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
        }

        if(parentSmallRightNode.getId().equals(focusNode.getId())) {
            parentSmallRightNode.setRightChild(smallRightNode.getRightChild());
        } else {
            parentSmallRightNode.setLeftChild(smallRightNode.getRightChild());
        }

        putNode(parentSmallRightNode);

        if(smallRightNode.getRightChild() != null) {
            BinaryTreeNode rightNode = getNode(smallRightNode.getRightChild())
                    .orElseThrow(() -> new RuntimeException("Invalid Tree! Node should exist but doesn't."));
            rightNode.setParent(parentSmallRightNode.getId());
            putNode(rightNode);
        }

        focusNode.setKey(smallRightNode.getKey());
        focusNode.setValue(smallRightNode.getValue());
        putNode(focusNode);
        deleteNode(smallRightNode.getId());
        return Optional.of(oldValue);
    }

    @Override
    public void print() {
        Optional<BinaryTreeNode> root = getRootNode();

        if(!root.isPresent())
            System.out.println("Empty Tree");
        else
            print(root.get(), "", true);
    }

    private void print(BinaryTreeNode node, String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "└── " : "├── ") + node.getKey() + " : " + node.getValue());
        if(node.getLeftChild() != null & node.getRightChild() != null) {
            print(getNode(node.getLeftChild()).get(), prefix + (isTail ? "    " : "│   "), false);
            print(getNode(node.getRightChild()).get(), prefix + (isTail ? "    " : "│   "), true);
        } else if(node.getLeftChild() != null) {
            print(getNode(node.getLeftChild()).get(), prefix + (isTail ? "    " : "│   "), true);
        } else if(node.getRightChild() != null) {
            print(getNode(node.getRightChild()).get(), prefix + (isTail ? "    " : "│   "), true);
        }
    }
}
