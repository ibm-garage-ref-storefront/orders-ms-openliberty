package dev.appsody.orders;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.opentracing.Traced;

import dev.appsody.orders.model.Order;
import dev.appsody.orders.utils.OrderDAOImpl;
import io.opentracing.Tracer;

@RequestScoped
@Path("/orders")
public class OrderService {

    @Inject
    private JsonWebToken jwt;
    
    @Inject Tracer tracer;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Traced(value = true, operationName = "getAllOrders")
    public Response getOrders() throws Exception {
        try {
            if (jwt == null) {
                // distinguishing lack of jwt from a poorly generated one
                return Response.status(Response.Status.BAD_REQUEST).entity("Missing JWT").build();
            }
            else {
            	System.out.println("MP JWT config message: " + jwt.getName() );
                System.out.println("MP JWT getIssuedAtTime " + jwt.getIssuedAtTime() );
            }
            final String customerId = jwt.getName();
            if (customerId == null) {
                // if no user passed in, this is a bad request
                // return "Invalid Bearer Token: Missing customer ID";
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID from jwt: " + jwt.getRawToken()).build();
            }

            System.out.println("caller: " + customerId);

            OrderDAOImpl ordersRepo = new OrderDAOImpl();

            final List<Order> orders = ordersRepo.findByCustomerIdOrderByDateDesc(customerId);

            return Response.ok(orders).build();

        } catch (Exception e) {
            System.err.println(e.getMessage() + "" + e);
            System.err.println("Entering the Fallback Method from getOrders().");
            throw new Exception(e.toString());
        }

    }
    

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @Traced(value = true, operationName = "getOrdersByID")
    public Response getOrdersById(@PathParam("id") String id) throws Exception {
	    try {
	    	if (jwt == null) {
	    		// distinguishing lack of jwt from a poorly generated one
	    		return Response.status(Response.Status.BAD_REQUEST).entity("Missing JWT").build();
	    	}
	    	else {
	    		System.out.println("MP JWT config message: " + jwt.getName() );
	    		System.out.println("MP JWT getIssuedAtTime " + jwt.getIssuedAtTime() );
	    	}
	          
	    	final String customerId = jwt.getName();
	          
	    	if (customerId == null) {
	              // if no user passed in, this is a bad request
	              // return "Invalid Bearer Token: Missing customer ID";
	              return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID from jwt: " + jwt.getRawToken()).build();
	        }
	
	    	System.out.println("caller: " + customerId);
	    	OrderDAOImpl ordersRepo = new OrderDAOImpl();
	    	final List<Order> orders = ordersRepo.findByOrderId(id);
	          
	    	return Response.ok(orders).build();
	      
	    } 
	    catch (Exception e) { 
	    	System.err.println(e.getMessage() + "" + e);
	    	throw new Exception(e.toString());
	    }

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Traced(value = true, operationName = "createOrders")
    public Response create(
        Order payload, @Context UriInfo uriInfo) throws IOException, TimeoutException {
        try {
            if (jwt == null) {
                // distinguishing lack of jwt from a poorly generated one
                return Response.status(Response.Status.BAD_REQUEST).entity("Missing JWT").build();
            }
            final String customerId = jwt.getName();
            if (customerId == null) {
                // if no user passed in, this is a bad request
                //return "Invalid Bearer Token: Missing customer ID";
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID from jwt: " + jwt.getRawToken()).build();
            }

            payload.setDate(Calendar.getInstance().getTime());
            payload.setCustomerId(customerId);

            String id = UUID.randomUUID().toString();

            payload.setId(id);

            System.out.println("New order: " + payload.toString());

            OrderDAOImpl ordersRepo = new OrderDAOImpl();
            ordersRepo.putOrderDetails(payload);

            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(payload.getId());
           
            System.out.println(builder.build().toString());
            
            return Response.created(builder.build()).entity(payload).build();

        } catch (Exception ex) {
            System.err.println("Error creating order: " + ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating order: " + ex.toString()).build();
        }

    }

}

