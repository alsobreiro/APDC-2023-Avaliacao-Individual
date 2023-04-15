package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.apache.commons.codec.digest.DigestUtils;

import com.google.protobuf.Timestamp;

import org.apache.http.client.entity.EntityBuilder;
import org.checkerframework.checker.units.qual.A;
import pt.unl.fct.di.apdc.firstwebapp.util.*;

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
	@Path("/listUsers")
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response listUsers(){
		List<String> users = new ArrayList();
		//Definir a query
		Query<Entity> q = Query.newEntityQueryBuilder().setKind("User").build();
		//Correr a query na datastore
		QueryResults<Entity> qR = datastore.run(q);
		//Percorrer os resultados da query
		while(qR.hasNext()){
			Entity current = qR.next();
			if(current.getString("user_status").equals("active")){
				users.add(current.getString("user_name"));
			}
		}
	return Response.ok(users).build();
	}
	@POST
	@Path("/deleteUser")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteUser(LoginData data){
		LOG.fine("Attempt to delete: " + data.username);
		Transaction txn = datastore.newTransaction();
		try{
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(data.username);
			Entity user = txn.get(userKey);
			Entity token = txn.get(tokenKey);
			if(token == null || user == null){
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).build();
			}
			if(isTokenValid(token)){
				txn.delete(tokenKey);
				txn.commit();
				return Response.status(Status.REQUEST_TIMEOUT).build();
			}
			String hashedPWD = (String) user.getString("user_pwd");
			if(hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
				if(token == null){
					txn.delete(userKey);
					txn.commit();
				}
				else {
					txn.delete(userKey, tokenKey);
					txn.commit();
				}
				return Response.ok().build();
			}else{
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
		}finally {
			if(txn.isActive()){
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	@POST
	@Path("/registerSU")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerSU(RegisterData data){
		LOG.fine("Attempt to register SU: " + data.username);
		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("SU").newKey(data.username);
			Entity user = txn.get(userKey);
			if (user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
			} else {
				//.set("user_name", data.username)
				user = Entity.newBuilder(userKey)
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
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	@POST
	@Path("/register")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response register(RegisterData data) {
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
				return Response.status(Status.FORBIDDEN).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey).set("user_name", data.username)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", data.email)
						.set("user_creation_time", g.toJson(fmt.format(new Date())))
						.set("user_profileType", data.profileType)
						.set("user_phone", String.valueOf(data.phone))
						.set("user_occupation", data.occupation)
						.set("user_workPlace", data.workPlace)
						.set("user_nif", data.nif)
						.set("user_status", data.status)
						.build();
				txn.add(user);
				LOG.info("User registered " + data.username);
				txn.commit();
				return Response.ok("{}").build();
			}
		} finally {
			if (txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}


	@POST
	@Path("/login")
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
				return Response.status(Status.BAD_REQUEST).build();
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


				AuthToken token = new AuthToken(data.username);
				Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(token.username);
				Entity utoken = Entity.newBuilder(tokenKey)
								.set("username", token.username)
								.set("tokenID",token.tokenID)
								.set("creationData",token.creationData)
								.set("expirationTime",token.expirationTime).build();
				txn.put(log,ustats,utoken);
				txn.commit();
				LOG.info("User "+ data.username+ " logged in successfully");
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

	//Working
	@POST
	@Path("/updatePassword")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePassword(UpdatePassword data){
		Transaction txn = datastore.newTransaction();
		try{
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(data.username);
			Entity user = txn.get(userKey);
			Entity token = txn.get(tokenKey);
			if(token == null || user == null){
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).build();
			}
			if(isTokenValid(token)){
				txn.delete(tokenKey);
				txn.commit();
				return Response.status(Status.REQUEST_TIMEOUT).build();
			}
			 String hashedPWD = (String) user.getString("user_pwd");
			 if(hashedPWD.equals(DigestUtils.sha512Hex(data.oldPassword))){
				 Entity updatedUser = Entity.newBuilder(userKey).set("user_name", data.username)
						 .set("user_pwd", DigestUtils.sha512Hex(data.newPassword))
						 .set("user_email", user.getString("user_email"))
						 .set("user_creation_time", user.getString("user_creation_time"))
						 .set("user_profileType", user.getString("user_profileType"))
						 .set("user_phone", user.getString("user_phone"))
						 .set("user_occupation", user.getString("user_occupation"))
						 .set("user_workPlace", user.getString("user_workPlace"))
						 .set("user_nif", user.getString("user_nif"))
						 .set("user_status", user.getString("user_status")).build();

				txn.put(updatedUser);
				txn.commit();
				return Response.ok().build();
			 }
			 else{
				 txn.rollback();
				 return Response.status(Status.FORBIDDEN).build();
			 }
		}finally {
			if(txn.isActive()){
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	//Working
	@POST
	@Path("/logout")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response logout(Logout data){
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Key tokenKey = datastore.newKeyFactory().setKind("token").newKey(data.username);
			Entity user = txn.get(userKey);
			Entity token = txn.get(tokenKey);
			if(token == null || user == null){
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).build();
			}
			if(isTokenValid(token)){
				txn.delete(tokenKey);
				txn.commit();
				return Response.status(Status.REQUEST_TIMEOUT).build();
			}
			txn.delete(tokenKey);
			txn.commit();
			return Response.ok().build();
		}finally{
			if(txn.isActive()){
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
	//Working
	@POST
	@Path("/activate")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response activateUser(LoginData data){
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try{
			Entity user = txn.get(userKey);
			if(user == null){
				LOG.warning("User does not exist: " + data.username);
				return Response.status(Status.BAD_REQUEST).build();
			}
			String hashedPWD = (String) user.getString("user_pwd");
			if(hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
				Entity activeUser = Entity.newBuilder(userKey).set("user_name", data.username)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_email", user.getString("user_email"))
						.set("user_creation_time", user.getString("user_creation_time"))
						.set("user_profileType", user.getString("user_profileType"))
						.set("user_phone", user.getString("user_phone"))
						.set("user_occupation", user.getString("user_occupation"))
						.set("user_workPlace", user.getString("user_workPlace"))
						.set("user_nif", user.getString("user_nif"))
						.set("user_status", "active")
						.build();
				txn.put(activeUser);
				txn.commit();
				return Response.ok().build();
			}
			else{
				txn.rollback();
				return Response.status(Status.FORBIDDEN).build();
			}
		}finally{
			if(txn.isActive()){
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	//Has error in atribute variables
	@POST
	@Path("/changeSelf")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changeSelf(SelfChange data){
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try{
			Entity user = txn.get(userKey);
			if(user == null){
				LOG.warning("User does not exist: " + data.username);
				return Response.status(Status.BAD_REQUEST).build();
			}
			String profileType;
			if(data.profileType == null){
				profileType = user.getString("user_profileType");
			}else{ profileType = data.profileType;}
			String phone;
			if(data.phone == null){
				phone= user.getString("user_phone");
			}else{ phone = data.phone;}
			String occupation;
			if(data.occupation == null){
				occupation= user.getString("user_occupation");
			}else{occupation = data.occupation;}
			String workPlace;
			if(data.workPlace == null){
				workPlace= user.getString("user_workPlace");
			}else{workPlace = data.workPlace;}
			String nif;
			if(data.nif == null){
				nif= user.getString("user_nif");
			}else{nif = data.nif;}
			String status;
			if(data.status == null){
				status = user.getString("user_status");
			}else{status = data.status;}

			Entity activeUser = Entity.newBuilder(userKey).set("user_name", data.username)
					.set("user_pwd", user.getString("user_pwd"))
					.set("user_email", user.getString("user_email"))
					.set("user_creation_time", user.getString("user_creation_time"))
					.set("user_profileType", profileType)
					.set("user_phone", phone)
					.set("user_occupation", occupation)
					.set("user_workPlace", workPlace)
					.set("user_nif", nif)
					.set("user_status", status)
					.build();
		}finally {
			if(txn.isActive()){
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		return Response.ok().build();
	}
//Return true valido, false invalido
	private boolean isTokenValid(Entity token){
		long currentTime = System.currentTimeMillis();
		long validation = token.getLong("expirationTime");
		return validation<currentTime;
	}
}