# NutCracker

## Create Executable
1. mvn package
2. Fat JAR: target/nutcracker-jar-with-dependencies.jar

## Launch
1. Compare two strings:
   * java-jar nutcracker-jar-with-dependencies.jar -t string1 string2
   * Example:
      * java-jar nutcracker-jar-with-dependencies.jar -t "The dog sat on the mat" "The mutt sat on the rug"
2. Compare contents of two plain-text documents:
   * java-jar nutcracker-jar-with-dependencies.jar -f path1 path2
   * Example (files are included):
      * java-jar nutcracker-jar-with-dependencies.jar -t ../src/main/resources/benchmark/f1_1.txt ../src/main/resources/benchmark/f1_2.txt
