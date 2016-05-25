package api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import connection.Database;
import utilities.Constants;

public class Api implements HttpHandler {

	private Database database;

	public Api(Database database) {
		this.database = database;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {

		System.out.println("[HANDLER] Detected an https request");

		String method = exchange.getRequestMethod();
		URI uri = exchange.getRequestURI();

		String[] paths = uri.getPath().replaceFirst("^/", "").split("/");
		String query = uri.getQuery();

		if (query == null)
			query = "";

		Map<String, String> filtered = filter(query);

		try {
			process(exchange, method, paths, filtered);
		} catch (Exception e) {
			e.printStackTrace();
			response(exchange, "[ERROR] Unknown error");
		}

	}

	private void process(HttpExchange exchange, String method, String[] paths, Map<String, String> filtered) {

		Headers headers = exchange.getResponseHeaders();
		headers.add("Content-Type", "application/json");

		String body = getBody(exchange);

		if (paths[1].equals("user"))
			processEventUser(exchange, method, body, paths, filtered);
		else
			response(exchange, "[EVENT] Not an event");

	}

	/**
	 * Process user account requests
	 * 
	 * @param exchange
	 * @param method
	 * @param body
	 * @param paths
	 * @param filtered
	 */
	private void processEventUser(HttpExchange exchange, String method, String body, String[] paths,
			Map<String, String> filtered) {

		System.out.println("[USER EVENT] Processing event");

		String email = filtered.get("email");
		String password = null;
		String name = null;
		String username = null;
		String birthdate = null;
		String country = null;

		if (filtered.containsKey("password"))
			password = filtered.get("password");
		if (filtered.containsKey("name"))
			name = filtered.get("name");
		if (filtered.containsKey("username"))
			username = filtered.get("username");
		if (filtered.containsKey("birthdate"))
			birthdate = filtered.get("birthdate");
		if (filtered.containsKey("country"))
			country = filtered.get("country");

		switch (method) {
		case "GET":
			System.out.println("[USER EVENT] Processing GET request");
			if (email == null)
				response(exchange, "Invalid email!");
			else if (password == null)
				response(exchange, "Invalid password!");
			else
				handleUserGET(exchange, email, password);
			break;
		case "POST":
			System.out.println("[USER EVENT] Processing POST request");
			if (username == null)
				response(exchange, "Null Username!");
			else
				handleUserPOST(exchange, username, password, name, email, birthdate, country);
			break;
		case "PUT":
			System.out.println("[USER EVENT] Processing PUT request");
			if (username == null)
				response(exchange, "Null Username!");
			else if (password == null)
				response(exchange, "Null Password!");
			else if (name == null)
				response(exchange, "Null Name!");
			else if (email == null)
				response(exchange, "Null Email!");
			else if (birthdate == null)
				response(exchange, "Invalid Birthdate!");
			else
				handleUserPUT(exchange, username, password, name, email, birthdate, country);
			break;
		case "DELETE":
			System.out.println("[USER EVENT] Processing DELETE request");
			handleUserDELETE(exchange, username, password);
			break;
		default:
			System.out.println("[USER EVENT] Unknow request");
			break;
		}
	}

	/**
	 * Handle a GET request
	 * 
	 * @param exchange
	 * @param username
	 * @param password
	 */
	private void handleUserGET(HttpExchange exchange, String email, String password) {

		User user = new User(database, email, password);

		JSONObject json;
		ResultSet result;

		String response_code = user.UserGET();
		result = user.getPlayerInfo(email);

		try {
			if (result.next()) {

				if (response_code.equals(Constants.ERROR_USER_EMAIL)) {
					json = buildJson(Constants.ERROR, "Invalid email!", null, null, null, null, null);
					response(exchange, json.toString());
				} else if (response_code.equals(Constants.ERROR_USER_PASSWORD)) {
					json = buildJson(Constants.ERROR, "Invalid password!", null, null, null, null, null);
					response(exchange, json.toString());
				} else if (response_code.equals(Constants.OK)) {
					json = buildJson(Constants.OK, null, result.getString("username"), result.getString("name"),
							result.getString("birthdate"), result.getString("country"), result.getString("points"));
					response(exchange, json.toString());
				} else {
					json = buildJson(Constants.ERROR, "Unknown Error!", null, null, null, null, null);
					response(exchange, json.toString());
				}

			} 
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Handle a POST request
	 * 
	 * @param exchange
	 * @param username
	 * @param password
	 * @param name
	 * @param email
	 * @param birthdate
	 * @param country
	 */
	private void handleUserPOST(HttpExchange exchange, String username, String password, String name, String email,
			String birthdate, String country) {

		User user = new User(database, email, password);

		if (name != null)
			user.setName(name);
		if (username != null)
			user.setUsername(username);
		if (birthdate != null)
			user.setBirthdate(birthdate);
		if (country != null)
			user.setCountry(country);

		int response_code = user.UserPOST();

		if (response_code == 200)
			response(exchange, "POST request successful!");
		else
			response(exchange, "Not Found!");

	}

	/**
	 * Handle a PUT request
	 * 
	 * @param exchange
	 * @param username
	 * @param password
	 * @param name
	 * @param email
	 * @param birthdate
	 * @param country
	 */
	private void handleUserPUT(HttpExchange exchange, String username, String password, String name, String email,
			String birthdate, String country) {

		User user = new User(database, email, password);

		user.setName(name);
		user.setUsername(username);
		user.setBirthdate(birthdate);
		user.setCountry(country);

		int response_code = user.UserPUT();

		if (response_code == 200)
			response(exchange, "PUT request successful!");
		else
			response(exchange, "Not Found!");

	}

	/**
	 * Handle a DELETE request
	 * 
	 * @param username
	 */
	private void handleUserDELETE(HttpExchange exchange, String username, String password) {

		User user = new User(database, username, password);

		int response_code = user.UserDELETE();

		if (response_code == 200)
			response(exchange, "DELETE request successful!");
		else
			response(exchange, "Not Found!");

	}

	public JSONObject buildJson(String status, String reason, String username, String name, String birthdate,
			String country, String points) {

		JSONObject json = new JSONObject();

		if (status.equals(Constants.ERROR)) {
			try {
				json.put("status", status);
				json.put("reason", reason);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		} else {

			try {
				json.put("status", status);
				json.put("username", username);
				json.put("name", name);
				json.put("birthdate", birthdate);
				json.put("country", country);
				json.put("points", points);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}

		return json;
	}

	/**
	 * Get the message's body
	 * 
	 * @param exchange
	 * @return body
	 */
	private String getBody(HttpExchange exchange) {

		String body;

		InputStream input = exchange.getRequestBody();
		Scanner scanner = new Scanner(input);

		scanner.useDelimiter("\\A");

		body = scanner.hasNext() ? scanner.next() : "";

		try {
			scanner.close();
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return body;

	}

	private void response(HttpExchange exchange, String message) {

		try {
			exchange.sendResponseHeaders(200, message.getBytes().length);
		} catch (IOException exception) {
			exception.printStackTrace();
		}

		OutputStream output = exchange.getResponseBody();

		try {
			output.write(message.getBytes());
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Transform a string query into an hashmap
	 * 
	 * @param query
	 * @return hashmap
	 */
	public Map<String, String> filter(String query) {

		Map<String, String> map = new HashMap<String, String>();

		for (String keyValue : query.split("&")) {

			String[] pairs = keyValue.split("=");
			map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);

		}
		return map;

	}

}
