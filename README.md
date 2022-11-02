# Cryptobox4J

Wire Cryptobox for Java intended for use on servers.

The library is available on the Maven Central. However, to use it, one needs to have native Cryptobox installed.
See makefiles in [mk](mk) directory.

```xml

<dependency>
    <groupId>com.wire</groupId>
    <artifactId>cryptobox4j</artifactId>
    <version>1.2.2</version>
</dependency>
```

## Local installation

In order to install the cryptobox binary to your local system one needs to following:

1. build all necessary libraries `make dist`
2. find out where to copy the binaries by running `make list-java-library`, this lists all folders that are in `java.library.path` (for
   example `/Library/Java/Extensions` on MacOS or `/usr/java/packages/lib`, `/usr/lib` on Linux) - another way is to set env
   variable `LD_LIBRARY_PATH` to point to the folder with the cryptobox binaries
3. copy content of the folder `dist/lib` to one of the folders from step 2, you might need to use sudo or if none of them exists create
   the folder
