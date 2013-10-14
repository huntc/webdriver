webdriver
=========

An alternate webdriver implementation based on Scala, Akka and Spray.

Same usage can be obtained by inspecting the webdriver-tester sub-project. There's a main class there that
illustrates essential interactions.

An additional webdriver-sbt sub-project is provided that declares useful stuff in order to use webdriver from
within an sbt plugin. For example settings are declared that locate js main and test source files. This
sub-project has a separate release cycle to webdriver itself and could be spun off into its own repo at a later
point in time e.g. if/when Maven/Gradle support is required. The main point here is that the core webdriver
library is not related to sbt at all and should be usable from other build tools.
