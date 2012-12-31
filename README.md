# cloudflow

**Overview**:  Cloudflow is a simple libary that provides a framework for creating and executing workflows in a Java process without the 
step definitions of those workflows being explicitly dependent on one another.  This loose coupling ties only the steps to the compiled product 
and allows the workflow definition to be injected at runtime.


**Simple Contrived Example**:

*TBD*


**FAQ**:

*Why did you develop Cloudflow in Python/Ruby/Scala/Go/Erlang/C/C++ or my other favorite language?*

I'm more of a Java guy than any other language and had a need for a dynamic workflow provisioner for some other projects written in 
Java.  It's not unlikely that I may port Cloudflow to python at some point in the near future, since the codebase is lightweight and small.


*Can I use Cloudflow in Glassfish/JBoss/Tomcat/Websphere?*

Absolutely.  Cloudflow is agnostic to the container in which it runs.  In fact, one of the potential use cases for Cloudflow would be to 
receive the workflow definition via a Servlet and either execute it inline or asynchronously with the HTTP request.


*Can I use Cloudflow in [Spring](http://www.springsource.com)?*

Also, yes.  In fact, Cloudflow was designed with an extension into Spring in mind: if you need to do Spring bean injection on steps at their creation
time, you can do so with very little code.  By extending `JsonParser`, making it `ApplicationContextAware`, and overriding the `pre(Step)` or `post(Step)`
to inject the required beans when the Step is created during parsing.

If this use case becomes popular, it's not impossible that Cloudflow may provide this functionality in the future. 


*Under what Java versions does Cloudflow work?*

Cloudflow runs on Java 5 or higher.


*How many developer hours went into Cloudflow?*

About 16 total hours: Cloudflow was developed over two six-hour plane flights in December 2012 with a couple of hours on the side. 


*Can I submit a patch?*

Absolutely!  Feel free to pull, branch, and I'll work with you to get your changes integrated into `master`.


*I found a bug!  I need a feature!*
  
Please file an issue request [here](https://github.com/bdimmick/cloudflow/issues).