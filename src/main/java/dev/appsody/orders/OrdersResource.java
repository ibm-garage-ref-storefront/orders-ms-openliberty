package dev.appsody.orders;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/resource")
public class OrdersResource {

    @GET
    public String getRequest() {
        return "OrdersResource response";
    }
}
