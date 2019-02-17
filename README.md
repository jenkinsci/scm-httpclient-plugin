##SCM HttpClient Plugin

    The simplest way to know what has changed on your Jenkins builds after developers submit code! Also, the scm changePaths and job changes can be send to http request body as parms. 

  * Introduction
  * How it works?
  * Usage 
  * Configuration
 

#Introduction

The plugin shows changes via compare last with previous build SCM revision diffs, the changes will be show on console. example:
![](https://i.imgur.com/c8sUYbO.png)

    Only Git and Svn based projects are supported.


#How it works?
  1. This plugin expects .git or .svn folders present on your build workspace and will use it to retrieve repository information.
  2. While your job runs the plugin reads your build workspace to retrieve the  build message via compare last with previous build SCM revision diffs.

#Usage

After installation, activate after the build steps SCM HttpClient Plugin. 

**Activate after the build steps**
![](https://i.imgur.com/GTICbtL.png)

#Configuration

The setup is done via build configuration:
![](https://i.imgur.com/ya9cwrW.png)

if Print changeLog attribute is true,the build changes will be show on console
![](https://i.imgur.com/eZBXF1P.png)

if Send httpRequest attribute is true,you can send http request.

When you use Http Request function,the affectedPaths and job build message can be send to http request body. use like  $(AFFECTED_PATH)、$(CI).  example:
![](https://i.imgur.com/Oj82ZlH.png)

After building, you can see console message:
![](https://i.imgur.com/sPjADov.png)

Tip: debug jenkins plugin
mvn hpi:run -Dmaven.javadoc.skip=true -Djetty.port=8090 




