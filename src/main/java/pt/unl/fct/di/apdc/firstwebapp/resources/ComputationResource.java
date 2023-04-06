package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.*;
import org.apache.commons.codec.cli.Digest;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.protobuf.Timestamp;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

import com.google.cloud.tasks.v2.*;
import com.google.gson.Gson;



@Path("/utils")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
public class ComputationResource {

	private static final Logger LOG = Logger.getLogger(ComputationResource.class.getName()); 
	private final Gson g = new Gson();
	
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private static final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	public ComputationResource() {} //nothing to be done here @GET

	@GET
	@Path("/compute")
	public Response triggerExecuteComputeTask() throws IOException {
	String projectId = "alien-container-379310";
	String queueName = "Default";
	String location = "europe-west6";
	LOG.log(Level.INFO, projectId + " :: " + queueName + " :: " + location );
	try (CloudTasksClient client = CloudTasksClient.create()) {
		String queuePath = QueueName.of(projectId, location,
				queueName).toString();
		Task.Builder taskBuilder =Task.newBuilder().setAppEngineHttpRequest(AppEngineHttpRequest.newBuilder() 
				.setRelativeUri("/rest/utils/compute").setHttpMethod(HttpMethod.POST)
				.build());
		taskBuilder.setScheduleTime(Timestamp.newBuilder().setSeconds(Instant.now(Clock.systemUTC()).getEpochSecond()));
		client.createTask(queuePath, taskBuilder.build());
	}

	return Response.ok().build();
	}
	
	@POST
	@Path("/compute")
	public Response executeComputeTask() {
	LOG.fine("Starting to execute computation taks");
	try { 
		Thread.sleep(60*1000*10);//10 min...	
	} catch (Exception e) {
		LOG.logp(Level.SEVERE, this.getClass().getCanonicalName(), "executeComputeTask", "An exception has ocurred", e);
		return Response.serverError().build();
	}//Simulates 60s execution
	return Response.ok().build();
	}

	@POST
	@Path("/register/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response register(RegisterData data) {
		LOG.fine("Atempt to register user:" + data.username);
		//Checks if data is valid
		if(!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = Entity.newBuilder(userKey).set("user_pwd", DigestUtils.sha512Hex(data.password))
				.set("user_creation_time", g.toJson(fmt.format(new Date()))).build();
		
		datastore.put(user);
		LOG.info("User " + data.username + " added");
			
		return Response.ok().build();
	}
	@POST
	@Path("/register/v4")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response register4(RegisterData data) {
		LOG.fine("Attempt to register user: " + data.username);
		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);
			if (user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey).set("user_name", data.username)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", data.email)
						.set("user_creation_time", g.toJson(fmt.format(new Date())))
						.build();
				txn.add(user);
				LOG.info("User registered " + data.username);
				txn.commit();
				return Response.ok("{}").build();
			}
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
	@POST
	@Path("/login2")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response doLogin(LoginData data,
							@Context HttpServletRequest request,
							@Context HttpHeaders headers){
		LOG.fine("Attempt to login user: "+ data.username);

		//Keys should be generated outside transactions
		//Construct the key from the username
		//Key userKey = datastor.newKeyFactory().newKey(data.username);
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key ctrskey = datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.username))
				.setKind("UserStats").newKey("counters");
		//Generate automatically a key
		Key logKey = datastore.allocateId(
				datastore.newKeyFactory()
						.addAncestors(PathElement.of("User", data.username))
						.setKind("UserLog").newKey());
		Transaction txn = datastore.newTransaction();
		try{
			Entity user = txn.get(userKey);
			if(user == null){
				LOG.warning("Failed login attempt for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
			Entity stats = txn.get(ctrskey);
			if(stats == null){
				stats = Entity.newBuilder(ctrskey)
						.set("user_stats_logins", 0L)
						.set("user_stats_failed", 0L)
						.set("user_first_login",com.google.cloud.Timestamp.now())
						.set("user_last_login", com.google.cloud.Timestamp.now())
						.build();
			}
			String hashedPWD = (String) user.getString("user_pwd");
			if(hashedPWD.equals(DigestUtils.sha512Hex(data.password))){
				Entity log = Entity.newBuilder(logKey)
						.set("user_login_ip", request.getRemoteAddr())
						.set("user_login_host", request.getRemoteHost())
						.set("user_login_latlon",
								StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong")).setExcludeFromIndexes(true).build())
						.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
						.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
						.build();
				Entity ustats = Entity.newBuilder(ctrskey)
						.set("user_stats_logins",1L + stats.getLong("user_stats_logins"))
						.set("user_stats_failed", 0L)
						.set("user_first_login", stats.getTimestamp("user_first_login"))
						.set("user_last_login", com.google.cloud.Timestamp.now())
						.build();
				txn.put(log,ustats);
				txn.commit();

				AuthToken token = new AuthToken(data.username);
				LOG.info("User "+ data.username+ " logged in sucessufully");
				return Response.ok(g.toJson(token)).build();
			}else{
				Entity ustats = Entity.newBuilder(ctrskey)
						.set("user_stats_logins",stats.getLong("user_stats_logins"))
						.set("user_stats_failed", 1L + stats.getLong("user_stats_failed"))
						.set("user_first_login", stats.getTimestamp("user_first_login"))
						.set("user_last_login", stats.getTimestamp("user_last_login"))
						.set("user_last_attemp", com.google.cloud.Timestamp.now())
						.build();
				txn.put(ustats);
				txn.commit();
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
		}finally{
			if(txn.isActive()){
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
}