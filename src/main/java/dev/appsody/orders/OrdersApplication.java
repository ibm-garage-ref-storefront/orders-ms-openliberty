package dev.appsody.orders;

import javax.annotation.security.DeclareRoles;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.auth.LoginConfig;

@LoginConfig(authMethod = "MP-JWT")
@DeclareRoles({"admin", "user"})
@ApplicationPath("/micro")
public class OrdersApplication extends Application {

}
