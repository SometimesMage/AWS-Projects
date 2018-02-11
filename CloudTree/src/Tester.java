import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.Scanner;

public class Tester {

    public static void main(String[] args) throws InterruptedException {
        if(args.length < 1) {
            System.out.println("Invalid Argument! Usage: java Tester treeName [credentialsFile]");
            System.exit(0);
        }

        CloudTree tree;
        Scanner in = new Scanner(System.in);

        System.out.println("Loading Tree...");
        if(args.length == 1) {
            tree = new BinarySearchCloudTree(args[0], null);
        } else {
            tree = new BinarySearchCloudTree(args[0], args[1]);
        }
        System.out.println("Tree Loaded!");

        boolean running = true;

        while(running) {
            int choice = -1;
            while(choice < 1 || choice > 6) {
                menu();
                try {
                    choice = Integer.parseInt(in.nextLine());
                } catch (NumberFormatException e) {
                    choice = -1;
                }
            }

            switch (choice) {
                case 1:
                    tree.print();
                    break;
                case 2:
                    System.out.println("Please input a key:");
                    String key = in.nextLine();
                    System.out.println("Please input a value:");
                    String value = in.nextLine();
                    tree.insert(key, value);
                    break;
                case 3:
                    System.out.println("Please input file location:");
                    String fileLocation = in.nextLine();
                    try {
                        Scanner fin = new Scanner(new File(fileLocation));
                        while(fin.hasNextLine()) {
                            String[] parts = fin.nextLine().split(":");
                            tree.insert(parts[0], parts[1]);
                        }
                        System.out.println("File Scan Complete!");
                    } catch (FileNotFoundException e) {
                        System.out.println("Invalid File Location!");
                    }
                    break;
                case 4:
                    System.out.println("Please input a key:");
                    key = in.nextLine();
                    Optional<String> query = tree.query(key);
                    if(query.isPresent())
                        System.out.println("Value: " + query.get());
                    else
                        System.out.println("Couldn't find key in tree.");
                    break;
                case 5:
                    System.out.println("Please input a key:");
                    key = in.nextLine();
                    query = tree.delete(key);
                    if(query.isPresent())
                        System.out.println("Value: " + query.get());
                    else
                        System.out.println("Couldn't find key in tree.");
                    break;
                case 6:
                    System.out.println("Quitting...");
                    running = false;
            }
        }
    }

    private static void menu() {
        System.out.println("\nMenu Options:\n" +
                "1. Print Tree\n" +
                "2. Insert Key Value Pair\n" +
                "3. Insert Key Value Pairs from a File\n" +
                "4. Get Value of Key\n" +
                "5. Remove Key\n" +
                "6. Quit\n");
    }
}
