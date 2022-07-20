public class JavaLib {
    public static void main(String[] args) {
        System.out.println(String.join(System.lineSeparator(), System.getProperty("java.library.path").split(":")));
    }
}
