import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class DragDropFiles extends JFrame {

    private AmazonS3 s3;
    private JTree tree;
    private JLabel statusLabel;
    private DefaultTreeModel treeModel;
    private JPanel wrap;

    public DragDropFiles() {
        super("S3 Drop Box");

        this.s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
        this.treeModel = new DefaultTreeModel(null);
        this.tree = new JTree(treeModel);
        this.wrap = new JPanel();
        this.statusLabel = new JLabel("Loading Buckets...");

        //Tree Setup
        tree.setDropMode(DropMode.ON);
        tree.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRowHeight(0);
        tree.setTransferHandler(new UploadHandler(s3, treeModel, statusLabel));

        //GUI Setup
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        wrap.add(this.statusLabel);
        p.add(Box.createHorizontalStrut(4));
        p.add(Box.createGlue());
        p.add(wrap);
        p.add(Box.createGlue());
        p.add(Box.createHorizontalStrut(4));
        getContentPane().add(p, BorderLayout.NORTH);

        getContentPane().add(new JScrollPane(tree), BorderLayout.CENTER);

        JButton createBucket = new JButton("Create Bucket");
        createBucket.addActionListener(new CreateBucketBtnListener(s3, treeModel, statusLabel));

        JButton downloadSelected = new JButton("Download Selected");
        downloadSelected.addActionListener(new DownloadBtnListener(s3, tree, statusLabel));

        JButton deleteSelected = new JButton("Delete Selected");
        deleteSelected.addActionListener(new DeleteSelectedBtnListener(s3, treeModel, tree, statusLabel));

        p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        wrap = new JPanel();
        wrap.add(createBucket);
        wrap.add(downloadSelected);
        wrap.add(deleteSelected);
        p.add(Box.createHorizontalStrut(4));
        p.add(Box.createGlue());
        p.add(wrap);
        p.add(Box.createGlue());
        p.add(Box.createHorizontalStrut(4));
        getContentPane().add(p, BorderLayout.SOUTH);

        getContentPane().setPreferredSize(new Dimension(500, 450));
        System.out.println("Loading Buckets...");
        loadBuckets();
    }

    private static void increaseFont(String type) {
        Font font = UIManager.getFont(type);
        font = font.deriveFont(font.getSize() + 4f);
        UIManager.put(type, font);
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        DragDropFiles test = new DragDropFiles();
        test.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //Display the window.
        test.pack();
        test.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                increaseFont("Tree.font");
                increaseFont("Label.font");
                increaseFont("ComboBox.font");
                increaseFont("List.font");
            } catch (Exception ignored) {
            }

            //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            createAndShowGUI();
        });
    }

    private void loadBuckets() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Buckets");

        List<Bucket> bucketList = s3.listBuckets();

        for (Bucket bucket : bucketList) {
            if (bucket.getName().startsWith("aws"))
                continue;
            DefaultMutableTreeNode bucketNode = new DefaultMutableTreeNode(new NodeBucket(bucket.getName()));
            loadFiles(bucket.getName(), "", bucketNode);
            root.add(bucketNode);
        }

        treeModel.setRoot(root);
        statusLabel.setText("Ready!");
    }

    private void loadFiles(String bucketName, String prefix, DefaultMutableTreeNode parent) {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withDelimiter("/");

        ObjectListing listing = s3.listObjects(request);

        List<S3ObjectSummary> files = listing.getObjectSummaries();
        for (S3ObjectSummary file : files) {
            String name = file.getKey().substring(prefix.length());
            MutableTreeNode node = new DefaultMutableTreeNode(new NodeFile(name, file.getKey(), bucketName));
            parent.add(node);
        }

        List<String> folders = listing.getCommonPrefixes();
        for (String folder : folders) {
            String name = folder.substring(prefix.length(), folder.length() - 1);
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeFolder(name, folder, bucketName));
            loadFiles(bucketName, folder, node);
            parent.add(node);
        }
    }

    private void copyFile(File source, File dest)
            throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            input.close();
            output.close();
        }
    }
}

class NodeFile {
    private String name;
    private String key;
    private String bucketName;

    public NodeFile(String name, String key, String bucketName) {
        this.name = name;
        this.key = key;
        this.bucketName = bucketName;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getBucketName() {
        return bucketName;
    }
}

class NodeFolder {
    private String name;
    private String key;
    private String bucketName;

    public NodeFolder(String name, String key, String bucketName) {
        this.name = name;
        this.key = key;
        this.bucketName = bucketName;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String toString() {
        return name;
    }
}

class NodeBucket {
    private String name;

    public NodeBucket(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

class UploadHandler extends TransferHandler {

    private AmazonS3 s3;
    private DefaultTreeModel treeModel;
    private JLabel statusLabel;

    public UploadHandler(AmazonS3 s3, DefaultTreeModel treeModel, JLabel statusLabel) {
        this.s3 = s3;
        this.treeModel = treeModel;
        this.statusLabel = statusLabel;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }

        info.setDropAction(COPY);
        info.setShowDropLocation(true);

        if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }

        JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
        TreePath path = dl.getPath();

        if (path == null) {
            return false;
        }

        //Only the "All Buckets" node should be a string
        if (((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject() instanceof String) {
            return false;
        }

        return true;
    }


    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!canImport(info)) {
            statusLabel.setText("Can't Upload That!");
            return false;
        }

        statusLabel.setText("Uploading...");
        JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();

        TreePath path = dl.getPath();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        while (!(parentNode.getUserObject() instanceof NodeFolder) && !(parentNode.getUserObject() instanceof NodeBucket)) {
            parentNode = (DefaultMutableTreeNode) parentNode.getParent();
        }

        String bucketName;
        String keyPrefix = "";

        if (parentNode.getUserObject() instanceof NodeFolder) {
            bucketName = ((NodeFolder) parentNode.getUserObject()).getBucketName();
            keyPrefix = ((NodeFolder) parentNode.getUserObject()).getKey();
        } else {
            bucketName = ((NodeBucket) parentNode.getUserObject()).getName();
        }

        Transferable t = info.getTransferable();

        try {
            List<File> list = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            uploadFiles(list, path, parentNode, bucketName, keyPrefix);
        } catch (Exception e) {
            statusLabel.setText("Error When Uploading!");
            e.printStackTrace();
            return false;
        }

        statusLabel.setText("Upload Finished!");
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    private void uploadFiles(List<File> files, TreePath path, DefaultMutableTreeNode parent, String bucketName, String keyPrefix) {
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                String key = keyPrefix + name + "/";
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeFolder(name, key, bucketName));
                uploadFiles(Arrays.asList(file.listFiles()), path.pathByAddingChild(node), node, bucketName, key);
                treeModel.insertNodeInto(node, parent, parent.getChildCount());
            } else {
                String key = keyPrefix + name;
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeFile(name, key, bucketName));
                s3.putObject(bucketName, key, file);
                treeModel.insertNodeInto(node, parent, parent.getChildCount());
            }
        }
    }
}

class DownloadBtnListener implements ActionListener {

    private AmazonS3 s3;
    private JTree tree;
    private JLabel statusLabel;

    public DownloadBtnListener(AmazonS3 s3, JTree tree, JLabel statusLabel) {
        this.s3 = s3;
        this.tree = tree;
        this.statusLabel = statusLabel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        TreePath path = tree.getSelectionModel().getSelectionPath();
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object object = treeNode.getUserObject();

        if (!(object instanceof NodeFile)) {
            JOptionPane.showMessageDialog(null,
                    "Error: File not selected!\nPlease select a file!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        NodeFile node = (NodeFile) object;

        int choice = JOptionPane.showConfirmDialog(null,
                "Would you like to download: " + node.getName() + "?",
                "Download File",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            statusLabel.setText("Downloading...");
            try {
                S3Object s3Object = s3.getObject(node.getBucketName(), node.getKey());
                S3ObjectInputStream in = s3Object.getObjectContent();
                FileOutputStream out = new FileOutputStream(new File(node.getName()));

                byte[] read_buf = new byte[1024];
                int read_len;
                while ((read_len = in.read(read_buf)) > 0) {
                    out.write(read_buf, 0, read_len);
                }

                in.close();
                out.close();
                statusLabel.setText("Download Complete!");
            } catch (Exception e) {
                statusLabel.setText("Error When Downloading!");
                e.printStackTrace();
            }
        }
    }
}

class CreateBucketBtnListener implements ActionListener {

    private AmazonS3 s3;
    private DefaultTreeModel treeModel;
    private JLabel statusLabel;

    public CreateBucketBtnListener(AmazonS3 s3, DefaultTreeModel treeModel, JLabel statusLabel) {
        this.s3 = s3;
        this.treeModel = treeModel;
        this.statusLabel = statusLabel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String bucketName = "";
        boolean bucketExists = true;

        while (bucketExists) {
            bucketName = JOptionPane.showInputDialog(null, "Please input a name for your bucket:", "Create Bucket", JOptionPane.PLAIN_MESSAGE);
            if(bucketName.isEmpty())
                return;
            bucketExists = s3.doesBucketExistV2(bucketName);
            if (bucketExists)
                JOptionPane.showMessageDialog(null, "Bucket name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        try {
            s3.createBucket(bucketName);
        } catch (Exception e) {
            statusLabel.setText("Error Creating Bucket!");
            e.printStackTrace();
            return;
        }

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeBucket(bucketName));
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        treeModel.insertNodeInto(node, root, root.getChildCount());
        statusLabel.setText("Created Bucket \"" + bucketName + "\"!");
    }
}

class DeleteSelectedBtnListener implements ActionListener {

    private AmazonS3 s3;
    private DefaultTreeModel treeModel;
    private JTree tree;
    private JLabel statusLabel;

    public DeleteSelectedBtnListener(AmazonS3 s3, DefaultTreeModel treeModel, JTree tree, JLabel statusLabel) {
        this.s3 = s3;
        this.treeModel = treeModel;
        this.tree = tree;
        this.statusLabel = statusLabel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TreePath path = tree.getSelectionModel().getSelectionPath();
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object object = treeNode.getUserObject();

        if (object instanceof NodeFile) {
            NodeFile node = (NodeFile) object;

            if (JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to delete the file: \"" + node.getName() + "\"?",
                    "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            s3.deleteObject(node.getBucketName(), node.getKey());
            treeModel.removeNodeFromParent(treeNode);
            statusLabel.setText("Deleted \"" + node.getName() + "\"!");
        } else if (object instanceof NodeFolder) {
            NodeFolder node = (NodeFolder) object;

            if (JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to delete the folder: \"" + node.getName() + "\"? " +
                            "Deleting this folder will delete the folders and files that are in it as well!",
                    "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            ListObjectsRequest request = new ListObjectsRequest()
                    .withBucketName(node.getBucketName())
                    .withPrefix(node.getKey());
            ObjectListing listing = s3.listObjects(request);

            List<S3ObjectSummary> objects = listing.getObjectSummaries();
            for (S3ObjectSummary os : objects) {
                s3.deleteObject(node.getBucketName(), os.getKey());
            }

            treeModel.removeNodeFromParent(treeNode);
            statusLabel.setText("Deleted Folder \"" + node.getName() + "\" and all of its contents!");
        } else if (object instanceof NodeBucket) {
            NodeBucket node = (NodeBucket) object;

            if (JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to delete the bucket: \"" + node.getName() + "\"? " +
                            "Deleting this bucket will delete the folders and files that are in it and" +
                            "you might not be able to get the bucket name back!",
                    "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            ObjectListing listing = s3.listObjects(node.getName());
            List<S3ObjectSummary> objects = listing.getObjectSummaries();
            for (S3ObjectSummary os : objects) {
                s3.deleteObject(node.getName(), os.getKey());
            }

            s3.deleteBucket(node.getName());
            treeModel.removeNodeFromParent(treeNode);
            statusLabel.setText("Deleted Bucket \"" + node.getName() + "\"!");
        }
    }
}
