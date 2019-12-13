import org.apache.hadoop.security.http.CrossOriginFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Created by Husna Sayedi in 2019
 */
public class SatelliteDataServer {


    public static void main(String[] args) throws Exception {
        Server server = new Server(12229);

        FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
        filterHolder.setInitParameter("allowedOrigins", "*");
        filterHolder.setInitParameter("allowedMethods", "GET, POST");

        ServletContextHandler servletContextHandler;
        servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        servletContextHandler.addServlet(ProcessServer.class, "/data/*");
        servletContextHandler.addFilter(filterHolder, "/*", null);


        server.setHandler(servletContextHandler);

        server.start();
        server.join();
    }
}