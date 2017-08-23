package Utility;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * Created by HoseinGhahremanzadeh on 8/23/2017.
 */
public class StringToJavaSource  extends SimpleJavaFileObject {
    final String code;

    public StringToJavaSource(String name, String code) {
        super(URI.create("string:///" + name.replace('.','/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
