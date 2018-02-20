h1. NutCracker

h2. Start
1. mvn package
2. cd target
3. java -jar nutracker-jar-with-dependencies.jar

h2. Modes
1. Compare two strings:
* java-jar nutracker-jar-with-dependencies.jar -t "The dog sat on the mat" "The mutt sat on the rug"
2. Compare contents of two plain-text documents:
* java-jar nutracker-jar-with-dependencies.jar -f path1 path2
* Example:
** java-jar nutracker-jar-with-dependencies.jar -t ../src/main/resources/benchmark/f1_1.txt ../src/main/resources/benchmark/f1_2.txt