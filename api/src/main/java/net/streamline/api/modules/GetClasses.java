package net.streamline.api.modules;

import net.streamline.api.base.module.BaseModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class GetClasses
{
    private static final boolean debug = false;

//    /**
//     * test function with assumed package esc.util
//     */
//    public static void main(String... args)
//    {
//        try
//        {
//            final Class<?>[] list = getClasses("esc.util");
//            for (final Class<?> c : list)
//            {
//                BaseModule.getInstance().logWarning(c.getName());
//            }
//        }
//        catch (final IOException e)
//        {
//            e.printStackTrace();
//        }
//    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @precondition Thread Class loader attracts class and jar files, exclusively
     * @precondition Classes with static code sections are executed, when loaded and thus must not throw exceptions
     *
     * @param packageName
     *            [in] The base package path, dot-separated
     *
     * @return The classes of package /packageName/ and nested packages
     *
     * @throws IOException,
     *             ClassNotFoundException not applicable
     *
     * @author Sam Ginrich, <a href="http://www.java2s.com/example/java/reflection/recursive-method-used-to-find-all-classes-in-a-given-directory-and-sub.html">...</a>
     *
     */
    public static Class<?>[] getClasses(String packageName) throws IOException
    {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        if (debug)
        {
            BaseModule.getInstance().logWarning("Class Loader class is " + classLoader.getClass().getName());
        }
        final String packagePath = packageName.replace('.', '/');
        final Enumeration<URL> resources = classLoader.getResources(packagePath);
        final List<Class<?>> classes = new ArrayList<Class<?>>();
        while (resources.hasMoreElements())
        {
            final URL resource = resources.nextElement();
            final String proto = resource.getProtocol();
            if ("file".equals(proto))
            {
                classes.addAll(findFileClasses(new File(resource.getFile()), packageName));
            }
            else if ("jar".equals(proto))
            {
                classes.addAll(findJarClasses(resource));
            }
            else
            {
                BaseModule.getInstance().logWarning("Protocol " + proto + " not supported");
                continue;
            }
        }
        return classes.toArray(new Class[0]);
    }


    /**
     * Linear search for classes of a package from a jar file
     *
     * @param packageResource
     *            [in] Jar URL of the base package, i.e. file URL bested in jar URL
     *
     * @return The classes of package /packageResource/ and nested packages
     *
     * @author amicngh, Sam Ginrich@stackoverflow.com
     */
    private static List<Class<?>> findJarClasses(URL packageResource)
    {
        final List<Class<?>> classes = new ArrayList<Class<?>>();
        try
        {
            BaseModule.getInstance().logWarning("Jar URL Path is " + packageResource.getPath());
            final URL fileUrl = new URL(packageResource.getPath());
            final String proto = fileUrl.getProtocol();
            if ("file".equals(proto))
            {
                final String filePath = fileUrl.getPath().substring(1); // skip leading /
                final int jarTagPos = filePath.indexOf(".jar!/");
                if (jarTagPos < 0)
                {
                    BaseModule.getInstance().logWarning("Non-conformant jar file reference " + filePath + " !");
                }
                else
                {
                    final String packagePath = filePath.substring(jarTagPos + 6);
                    final String jarFilename = filePath.substring(0, jarTagPos + 4);
                    if (debug)
                    {
                        BaseModule.getInstance().logWarning("Package " + packagePath);
                        BaseModule.getInstance().logWarning("Jar file " + jarFilename);
                    }
                    final String packagePrefix = packagePath + '/';
                    try
                    {
                        final JarInputStream jarFile = new JarInputStream(
                                new FileInputStream(jarFilename));
                        JarEntry jarEntry;

                        while (true)
                        {
                            jarEntry = jarFile.getNextJarEntry();
                            if (jarEntry == null)
                            {
                                break;
                            }
                            final String classPath = jarEntry.getName();
                            if (classPath.startsWith(packagePrefix) && classPath.endsWith(".class"))
                            {
                                final String className = classPath
                                        .substring(0, classPath.length() - 6).replace('/', '.');

                                if (debug)
                                {
                                    BaseModule.getInstance().logWarning("Found entry " + jarEntry.getName());
                                }
                                try
                                {
                                    classes.add(Class.forName(className));
                                }
                                catch (final ClassNotFoundException x)
                                {
                                    BaseModule.getInstance().logWarning("Cannot load class " + className);
                                }
                            }
                        }
                        jarFile.close();
                    }
                    catch (final Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            else
            {
                BaseModule.getInstance().logWarning("Nested protocol " + proto + " not supprted!");
            }
        }
        catch (final MalformedURLException e)
        {
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and sub-dirs.
     *
     * @param directory
     *            The base directory
     * @param packageName
     *            The package name for classes found inside the base directory
     * @return The classes
     * @author <a href="http://www.java2s.com/example/java/reflection/recursive-method-used-to-find-all-classes-in-a-given-directory-and-sub.html">...</a>
     *
     */
    private static List<Class<?>> findFileClasses(File directory, String packageName)
    {
        final List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists())
        {
            BaseModule.getInstance().logWarning("Directory " + directory.getAbsolutePath() + " does not exist.");
            return classes;
        }
        final File[] files = directory.listFiles();
        if (files == null) return new ArrayList<>();
        if (debug)
        {
            BaseModule.getInstance().logWarning("Directory "
                    + directory.getAbsolutePath()
                    + " has "
                    + files.length
                    + " elements.");
        }
        for (final File file : files)
        {
            if (file.isDirectory())
            {
                assert !file.getName().contains(".");
                classes.addAll(findFileClasses(file, packageName + "." + file.getName()));
            }
            else if (file.getName().endsWith(".class"))
            {
                final String className = packageName
                        + '.'
                        + file.getName().substring(0, file.getName().length() - 6);
                try
                {
                    classes.add(Class.forName(className));
                }
                catch (final ClassNotFoundException cnf)
                {
                    BaseModule.getInstance().logWarning("Cannot load class " + className);
                }
            }
        }
        return classes;
    }
}