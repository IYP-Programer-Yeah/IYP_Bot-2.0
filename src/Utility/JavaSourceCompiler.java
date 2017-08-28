package Utility;

import javafx.util.Pair;

import javax.tools.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by HoseinGhahremanzadeh on 8/23/2017.
 */
public class JavaSourceCompiler {
    public static HashMap<String, Class> loadedClasses = new HashMap<>();

    public static Pair<String, Boolean> compileString (String code, String name) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.print(code);
        out.close();
        JavaFileObject file = new StringToJavaSource(name, writer.toString());

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);

        boolean success = task.call();
        String errors = "";
        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            errors = errors + "\n" + diagnostic.getCode();
            errors = errors + "\n" + diagnostic.getKind();
            errors = errors + "\n" + diagnostic.getPosition();
            errors = errors + "\n" + diagnostic.getStartPosition();
            errors = errors + "\n" + diagnostic.getEndPosition();
            errors = errors + "\n" + diagnostic.getSource();
            errors = errors + "\n" + diagnostic.getMessage(null);
        }

        if (success) {
            ClassLoader classLoader = null;
            try {
                classLoader = URLClassLoader.newInstance(new URL[]{new File("").toURI().toURL()});
            } catch (MalformedURLException e) {
                success = false;
            }
            try {
                loadedClasses.put(name, Class.forName(name, true, classLoader));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                success = false;
            }
        }

        return new Pair<String, Boolean>(errors, success);
    }
}
