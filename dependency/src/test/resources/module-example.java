import org.mydependency;

module org.peass.example
{
    exports org.peass.example;
    exports org.peass.example.subpackage;

    requires org.slf4j;
}
