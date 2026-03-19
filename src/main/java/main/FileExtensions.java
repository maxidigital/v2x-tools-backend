package main;

/**
 *
 * @author bott_ma
 */
public class FileExtensions
{

    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }

    public static boolean isImage(String path) {
        if (getFileExtension(path).equals("png"))
            return true;
        if (getFileExtension(path).equals("ico"))
            return true;
        if (getFileExtension(path).equals("jpg"))
            return true;
        if (getFileExtension(path).equals("jpeg"))
            return true;
        if (getFileExtension(path).equals("svg"))
            return true;
        return false;
    }
}
