# NutCracker

## Create Executable
1. Run `mvn package` in `classifier` folder.
2. Fat JAR is to be found in `target/nutcracker-jar-with-dependencies.jar`.

## Prerequisites
Before starting the classifier, do the following steps:
1. Go to `implementation/LanguageTool-4.0-stable`.
2. Run `java -cp languagetool-server.jar org.languagetool.server.HTTPServer --port 8081`.

## Launch
1. Go to `target/`.
2. Compare two strings:
   * java -jar nutcracker-jar-with-dependencies.jar -t string1 string2
   * Example:
      * java -jar nutcracker-jar-with-dependencies.jar -t "The dog sat on the mat" "The mutt sat on the rug"
3. Compare contents of two plain-text documents:
   * java -jar nutcracker-jar-with-dependencies.jar -f path1 path2
   * Example (files are included):
      * java -jar nutcracker-jar-with-dependencies.jar -f ../src/main/resources/demo/f1_1.txt ../src/main/resources/demo/f1_2.txt

## Visualization
At the end the visualization is created and the path to it is printed out (`classifier/visualization`).
