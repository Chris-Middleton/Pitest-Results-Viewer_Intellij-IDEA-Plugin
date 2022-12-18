This project was built by me, Chris Middleton, over a 10 week period as part of my Senior Project at Cal Poly.

The goal of this Intellij IDEA plugin is to display the results of pitest mutation tests in-editor, rather than requiring programmers to view html content to see the results of their mutation tests.

For the plugin to work properly, you must enable outputting bytecode and XML in your pitest run configuration, by adding --outputFormats XML --features=+EXPORT as arguments to the run configuration. This plugin just looks at the output of pitest, so it is agnostic of how you run it, though the Intellij IDEA pitest plugin is recommended.

This plugin is not completed and is currently just a minimum viable product. It is currently able to display surviving mutants using hints displaying the mutant operater which percede the mutated operator in the source code.

I hope to continue work on this project (albeit slower than the pace that was during the quarter) and eventually turn it into a genuinely useful plugin.
