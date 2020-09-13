package com.datarecm.service.config;

import java.util.List;

public class ConfigProperties {
	private String region;

	private String hostname;
	private int port;
	private String username;
	private String password;
	private String dbname;
	private String dbtype;

	private String url;
	private List<String> queries;
	private String rule1;
	private String rule2;
	private String rule3;
	private String rule4;
	private String rule5;

	private String output;
	private int timeout;
	
	public List<String> getQueries() {
		return queries;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setQueries(List<String> queries) {
		this.queries = queries;
	}

	public String getDbname() {
		return dbname;
	}

	public void setDbname(String dbname) {
		this.dbname = dbname;
	}

	public String getDbtype() {
		return dbtype;
	}

	public void setDbtype(String dbtype) {
		this.dbtype = dbtype;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setRule1(String rule1) {
		this.rule1 = rule1;

	}

	public String getRule1() {
		return rule1;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}


	
	/*
	public void loadSourceConfig() throws IOException {
		try {
			Properties prop = new Properties();
			String propFileName = "application.properties";

			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}

			Date time = new Date(System.currentTimeMillis());
			//System.out.println(prop.toString());
			sourceConfig = new SourceConfig();
			// get the property value and print it out
			sourceConfig.setHostname(prop.getProperty("source.hostname"));
			sourceConfig.setUrl(prop.getProperty("source.url"));
			sourceConfig.setPort(Integer.parseInt(prop.getProperty("source.port")));
			sourceConfig.setPassword(prop.getProperty("source.password"));
			sourceConfig.setDbname(prop.getProperty("source.dbname"));
			sourceConfig.setDbtype(prop.getProperty("source.dbtype"));
			sourceConfig.setUsername(prop.getProperty("source.username"));
			sourceConfig.setRule1(prop.getProperty("source.rule1"));

			System.out.println( "\nProgram Ran on " + time + " by user=" +sourceConfig.getUsername() );
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			inputStream.close();
		}
	}
	 */

}