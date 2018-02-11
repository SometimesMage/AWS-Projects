import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractCloudTree<T> implements CloudTree {

    private AmazonDynamoDB db;
    private String treeName;

    public AbstractCloudTree(String treeName, String credentialsFile) throws InterruptedException {
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_WEST_2);

        if (credentialsFile != null && !credentialsFile.isEmpty()) {
            builder.withCredentials(new PropertiesFileCredentialsProvider(credentialsFile));
        }

        this.db = builder.build();

        this.treeName = treeName;

        CreateTableRequest request = new CreateTableRequest()
                .withAttributeDefinitions(new AttributeDefinition("id", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("id", KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
                .withTableName(treeName);

        TableUtils.createTableIfNotExists(db, request);
        TableUtils.waitUntilActive(db, treeName);
    }

    abstract T createNode(Map<String, AttributeValue> item);

    public void putNode(TreeNode node) {
        PutItemRequest request = new PutItemRequest()
                .withTableName(treeName)
                .withItem(node.asItem());

        db.putItem(request);
    }

    public Optional<T> getRootNode() {
        return getNode(TreeNode.ROOT_ID);
    }

    public Optional<T> getNode(String id) {
        Map<String, AttributeValue> key = new HashMap<>();

        key.put("id", new AttributeValue(id));

        GetItemRequest request = new GetItemRequest()
                .withTableName(treeName)
                .withKey(key);

        GetItemResult result = db.getItem(request);

        Map<String, AttributeValue> item = result.getItem();

        if (item == null || item.isEmpty())
            return Optional.empty();

        return Optional.of(createNode(item));
    }

    public void deleteNode(String id) {
        Map<String, AttributeValue> key = new HashMap<>();

        key.put("id", new AttributeValue(id));

        DeleteItemRequest request = new DeleteItemRequest()
                .withTableName(treeName)
                .withKey(key);

        db.deleteItem(request);
    }

    public String getTreeName() {
        return treeName;
    }
}
