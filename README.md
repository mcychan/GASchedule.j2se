# GASchedule.j2se
Making a Class Schedule Using a Genetic Algorithm with java

Port from C# .NET core
https://github.com/mcychan/GASchedule.cs

<img src="https://i.stack.imgur.com/QDPIS.png" /></p>
# How to call this api
If you are using Java, you would call GASchedule as follows:

```java
    final String FILE_NAME = "/GaSchedule.json";

    Configuration configuration = new Configuration();
    File targetFile = new File(System.getProperty("user.dir") + FILE_NAME);
    if(!targetFile.exists())
      targetFile = new File(new File(ConsoleApp.class.getResource("/").toURI()).getParentFile() + FILE_NAME);
    configuration.parseFile(targetFile.getAbsolutePath());

    GeneticAlgorithm<Schedule> ga = new GeneticAlgorithm<>(new Schedule(configuration), 2, 2, 80, 3);
    ga.run(9999, 0.999);
    String htmlResult = HtmlOutput.getResult(ga.getResult());
```
