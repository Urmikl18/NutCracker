# NutCracker

## Create Executable
1. mvn package
2. Fat JAR: target/nutcracker-jar-with-dependencies.jar

## Launch
1. Compare two strings:
   * java -jar nutcracker-jar-with-dependencies.jar -t string1 string2
   * Example:
      * java -jar nutcracker-jar-with-dependencies.jar -t "The dog sat on the mat" "The mutt sat on the rug"
2. Compare contents of two plain-text documents:
   * java -jar nutcracker-jar-with-dependencies.jar -f path1 path2
   * Example (files are included):
      * java -jar nutcracker-jar-with-dependencies.jar -f ../src/main/resources/demo/f1_1.txt ../src/main/resources/demo/f1_2.txt

## Visualization
At the end the visualization is created and the path to it is printed out.
