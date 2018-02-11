# AWS Projects

These are some projects I did while messing around with AWS.

##Cloud Tree
Creates trees using DyanmoDB as a store. There is only a Binary Search Tree implementation; however, there is abstraction setup to create more tree structures with ease.

**To Compile**
`javac -cp "lib/*;CloudTree/src" CloudTree/src/Tester.java`

**To Run**
`java -cp "lib/*;CloudTree/src" Tester`

##Drag And Drop
A basic drop box using S3 as a store. Allows you to create buckets, delete buckets, upload folders/files to buckets, download files from buckets and delete folders/files from buckets.

**To Compile**
`javac -cp "lib/*;DragAndDrop/src" CloudTree/src/DragDropFiles.java`

**To Run**
`java -cp "lib/*;DragAndDrop/src" DragDropFiles`

###Note on Running These Programs
You need to have your AWS credentials in a file called `credentials` located at `~/.aws` folder. For more information visit the [AWS docs](https://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html).