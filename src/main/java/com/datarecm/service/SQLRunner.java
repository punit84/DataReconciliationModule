package com.datarecm.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.datarecm.service.config.ConfigService;

/**
 * @author Punit Jain
 *
 */
@Component
public class SQLRunner {

	@Autowired
	public ConfigService config;
	static Map<Integer, Map<String, List<Object>>> sqlResutset= new HashMap<>();

	@Autowired
	public DBConnection sourceDB;

	String RULE1="select count(*) from dms_sample.\"order\";";
	String RULE2= "select ordinal_position as \"colum_position\",column_name,\ncase \nwhen data_type= 'timestamp without time zone' then 'timestamp' \nwhen data_type= 'double precision' then 'float8' \nwhen data_type= 'character varying' then 'varchar' \nelse data_type \n   END \n   FROM information_schema.columns \n    WHERE table_name = 'order' \n    ORDER BY ordinal_position;\n;";

	public Map<Integer, Map<String, List<Object>>> execuleAllRules() throws SQLException, ClassNotFoundException{
		List<String> rules = config.source().getRules();

		for (int index = 0; index < rules.size(); index++) {
			System.out.println("*******************Executing Source Query :"+ index+" *************");
			Map<String, List<Object>> result = executeSQL(rules.get(index));
			sqlResutset.put(index, result);
			
			System.out.println("*******************Execution successfull *************");

		}
		return sqlResutset;


	}

	public Map<String, List<Object>> executeSQL(String sqlRule) {
		PreparedStatement ruleStatement=null;

		try {
			if(null !=sourceDB && null != sourceDB.getConnection()){


				ruleStatement = sourceDB.getConnection().prepareStatement(sqlRule);
				ResultSet resultSet = ruleStatement.executeQuery();	

				return printSQLRespoinse(resultSet);
				//return resultSetToArrayList(resultSet);  

			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (ruleStatement!=null) {
				try {
					ruleStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
		return null;
	}

	public Map<String, List<Object>> printSQLRespoinse(ResultSet resultSet ) {

		try {
			ResultSetMetaData rsmd = resultSet.getMetaData();

			int columnsNumber = rsmd.getColumnCount();
			Map<String, List<Object>> map = new HashMap<>(columnsNumber);
			for (int i = 1; i <= columnsNumber; ++i) {
				map.put(rsmd.getColumnName(i), new ArrayList<>());
			}

			while (resultSet.next()) {
				for (int i = 1; i <= columnsNumber; i++) {
					if (i > 1) System.out.print(",  ");
					String columnValue = resultSet.getString(i);
					System.out.println(resultSet.getArray(i));

					System.out.print( rsmd.getColumnName(i) + ":" +columnValue);
					map.get(rsmd.getColumnName(i)).add(resultSet.getArray(i));

				}
				System.out.println("");
			}
			return map;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}