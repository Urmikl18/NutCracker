# NutCracker

## Create Executable
1. Run `mvn package` in `classifier` folder.
2. Fat JAR is to be found in `target/nutcracker-jar-with-dependencies.jar`.

## Prerequisites
Before starting the classifier, do the following steps:
1. Go to `submission/implementation/executables`.
2. Run `java -cp languagetool-server.jar org.languagetool.server.HTTPServer --port 8081`.

## Launch
1. Compare two strings:
   * java -jar nutcracker-jar-with-dependencies.jar -t string1 string2
   * Example:
      * java -jar nutcracker-jar-with-dependencies.jar -t "The dog sat on the mat" "The mutt sat on the rug"
2. Compare contents of two plain-text documents:
   * java -jar nutcracker-jar-with-dependencies.jar -f path1 path2
   * Example (files are included):
      * java -jar nutcracker-jar-with-dependencies.jar -f ../demo/f1_1.txt ../demo/f1_2.txt

## Visualization
At the end the visualization is created and the path to it is printed out.
