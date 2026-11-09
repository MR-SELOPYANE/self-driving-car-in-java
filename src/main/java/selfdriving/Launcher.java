package selfdriving;

/**
 * Plain entry point that does not extend Application. Running a JavaFX Application subclass
 * directly from the classpath (as NetBeans/IntelliJ/jar launchers do) fails with
 * "JavaFX runtime components are missing"; going through this class avoids that.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
